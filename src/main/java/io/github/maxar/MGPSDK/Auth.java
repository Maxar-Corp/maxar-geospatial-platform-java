package io.github.maxar.MGPSDK;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Auth class handles the authentication for all API calls. Auth methods contain the
 * functionality for generating storing and passing openid tokens
 */
class Auth {

    private final String BASE_URL;
    private final String API_BASE_URL;
    private final String USERNAME;
    private final String PASSWORD;
    private final String CLIENT_ID;
    private String access;
    private String refresh;
    private final String VERSION;
    private final String API_VERSION;

    /**
     * Constructor for <code>Auth</code>
     * @param credentials HashMap&lt;String, String&gt; containing user credentials
     * @throws IllegalArgumentException if credentials are not passed properly
     */
    Auth(HashMap<String, String> credentials) throws IllegalArgumentException {
        this.BASE_URL = "https://account.maxar.com";
        this.API_BASE_URL = "https://api.maxar.com";
        this.access = null;
        this.refresh = null;
        this.VERSION = "Java_0.1.0";
        this.API_VERSION = "v1";
        //XOR operator to make sure that not only username or password or clientid passed in
        if ((credentials.get("username") != null ^ credentials.get("password") != null) ||
            (credentials.get("clientId") != null ^ credentials.get("username") != null) ||
            (credentials.get("clientId") != null ^ credentials.get("password") != null)) {
            throw new IllegalArgumentException("Must pass in both a username and password");
        }
        //If credentials are passed
        else if (credentials.get("username") != null) {
            this.USERNAME = credentials.get("username");
            this.PASSWORD = credentials.get("password");
            this.CLIENT_ID = credentials.get(("clientId"));
        } else {
            //Read from MPS-config
            String homeDirectory = System.getProperty("user.home");
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(homeDirectory +
                    "/.MGP-config"));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(".MGP-Config file not found in user home directory");
            }
            //skip first line
            try {
                reader.readLine();
                this.USERNAME = reader.readLine().split("=")[1].trim();
                this.PASSWORD = reader.readLine().split("=")[1].trim();
                this.CLIENT_ID = reader.readLine().split("=")[1].trim();
            } catch (IOException e) {
                throw new IllegalArgumentException(".MGP-Config file not formatted correctly");
            }

        }
        this.refreshToken();
    }

    String getAPI_BASE_URL() {
        return API_BASE_URL;
    }

    String getVERSION() {
        return VERSION;
    }

    String getAPI_VERSION() {
        return API_VERSION;
    }

    /**
     * Takes the login credentials stored in this <code>Auth</code> and passes them to the openid
     * connect endpoint to generate an access token if no refresh token is present, otherwise
     * calls {@link #getAuth()} to generate a new access token
     * @return String containing the access token
     */
    String refreshToken() {

        if (this.refresh != null) {
            String URL = String.format("%s/auth/realms/mds/protocol/openid-connect/token", this.BASE_URL);
            String payload = String.format("grant_type=refresh_token&refresh_token=%s&client_id=%s",
                    this.refresh, this.CLIENT_ID);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest postRequest = HttpRequest.newBuilder(
                            URI.create(URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", String.format("Bearer %s", this.refresh))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> httpResponse;

            try {
                httpResponse = httpClient.send(postRequest, BodyHandlers.ofString());
            }
            catch (IOException | InterruptedException e) {
                return "API error please try again: " + e.toString();
            }
            JsonObject gsonResponse = new Gson().fromJson(httpResponse.body(), JsonObject.class);

            if (httpResponse.statusCode() == 400 && String.valueOf(
                gsonResponse.get("error_description")).equals("Token is not active")) {
                return this.getAuth();
            } else if (httpResponse.statusCode() != 200) {
                System.out.println("Error. Status code = " + httpResponse.statusCode() + " " +
                    httpResponse.body());
                return null;
            } else {
                this.access = String.valueOf(gsonResponse.get("access_token")).replaceAll(
                    "\"", "");
                this.refresh = String.valueOf(gsonResponse.get("refresh_token")).replaceAll(
                    "\"", "");
                return this.access;
            }
        } else {
            return this.getAuth();
        }
    }

    /**
     * Generates an access token and refresh token based on a username and password combination
     * @return String containing the access token
     */
    private String getAuth() {

        String URL = String.format("%s/auth/realms/mds/protocol/openid-connect/token", this.BASE_URL);
        String payload = String.format("client_id=%s&username=%s&password=%s&grant"
            + "_type=password&scope=openid", this.CLIENT_ID, this.USERNAME, this.PASSWORD);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest postRequest = HttpRequest.newBuilder(
                        URI.create(URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> httpResponse = null;
        try {
            httpResponse = httpClient.send(postRequest, BodyHandlers.ofString());
        }
        catch (IOException | InterruptedException e) {
            return "API error please try again: " + e.toString();
        }
        JsonObject gsonResponse = new Gson().fromJson(httpResponse.body(), JsonObject.class);
        if (httpResponse.statusCode() != 200) {
            if (httpResponse.statusCode() == 401 && httpResponse.body().contains("Invalid client "
                + "credentials")) {
                throw new IllegalArgumentException("Authentication Error: Invalid User Credentials");
            }
            else if (httpResponse.statusCode() == 400 && httpResponse.body().contains("Account "
                + "disabled")) {
                System.out.println("Authentication Error:Account Disabled");
            } else {
                System.out.println("Error: " + httpResponse.body());
            }
            System.exit(-1);
            return null;
        } else {
            this.access = String.valueOf(gsonResponse.get("access_token")).replaceAll("\"",
                "");
            this.refresh = String.valueOf(gsonResponse.get("refresh_token")).replaceAll("\"",
                "");
            return this.access;
        }
    }
}
