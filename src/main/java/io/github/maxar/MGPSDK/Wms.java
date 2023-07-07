package io.github.maxar.MGPSDK;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import okhttp3.Response;

/**
 * The Wms class supports making API calls that conform to OGC WMS spec. All methods in this
 * class are protected and cannot be called on the user side. Their functionality must be called
 * indireclty using {@link Ogc} methods
 */
class Wms {

    private final Auth AUTH;
    private final String BASE_URL;
    private final String VERSION;
    private final HashMap<String, String> QUERYSTRING;
    private final Ogc OGC;
    private final String ENDPOINT;
    private String srsname;

    /**
     * Constructor for <code>Wms</code>
     * @param ogc Instance of the OGC child class
     */
    Wms(Ogc ogc) {

        this.OGC = ogc;
        this.AUTH = ogc.getAUTH();
        this.ENDPOINT = this.OGC.getENDPOINT();
        String API_VERSION = this.AUTH.getAPI_VERSION();
        switch (this.ENDPOINT) {
            case "streaming" -> this.BASE_URL = String.format("%s/streaming/%s/ogc/wms",
                this.AUTH.getAPI_BASE_URL(), API_VERSION);
            case "basemaps" -> this.BASE_URL = String.format("%s/basemaps/%s/seamlines/ows",
                this.AUTH.getAPI_BASE_URL(), API_VERSION);
            case "analytics" -> this.BASE_URL = String.format("%s/analytics/%s/vector/"
                + "change-detection/Maxar/ows", this.AUTH.getAPI_BASE_URL(), API_VERSION);
            default -> this.BASE_URL = "";
        }
        this.VERSION = AUTH.getVERSION();
        this.srsname = this.OGC.getSrsname() == null ?  "EPSG:4326" : this.OGC.getSrsname();
        this.QUERYSTRING = this.initQueryString();
    }

    /**
     * <p>Performs a WMS search using the parameters set by the
     * {@link Ogc.Builder} class</p>
     * <p>Optional <code>StreamingBuilder parameters:</code></p>
     * <ul>
     *     <li>{@link Ogc.Builder#bbox(String)}</li>
     *     <li>{@link Ogc.Builder#srsname(String)}</li>
     *     <li>{@link Ogc.Builder#filter(String)}</li>
     *     <li>{@link Ogc.Builder#height(int)}</li>
     *     <li>{@link Ogc.Builder#width(int)}</li>
     *     <li>{@link Ogc.Builder#imageFormat(String)}</li>
     *     <li>{@link Ogc.Builder#downloadPath(String)}</li>
     *     <li>{@link Ogc.Builder#fileName(String)}</li>
     *     <li>{@link Ogc.Builder#typeName(String)}</li>
     * </ul>
     * @return String containing the response body (blob)
     */
    String returnImage() {

        String bbox = OGC.getBbox();
        String filter = OGC.getFilter();
        OgcUtils.validateImageFormat(OGC);
        this.QUERYSTRING.put("format", OGC.getParamsImageFormat());

        if (bbox != null) {
            OgcUtils.validateBbox(OGC);
            if (this.OGC.getSrsname() == null) {
                throw new IllegalArgumentException("Must provide the projection with .srsname()");
            } else {
                this.srsname = this.OGC.getSrsname();
            }
            this.QUERYSTRING.put("srsname", srsname);
            String bboxList = OgcUtils.processBbox(OGC);
            this.QUERYSTRING.put("bbox", bboxList);
        } else {
            throw new IllegalArgumentException("Search function must have a BBOX.");
        }
        if (filter != null) {
            OgcUtils.cqlChecker(filter);
            this.QUERYSTRING.put("cql_filter", filter);
        }
        if (OGC.getFEATURE_ID() != null) {
            this.QUERYSTRING.remove("cql_filter");
            this.QUERYSTRING.put("coverage_cql_filter", String.format("featureId='%s'", OGC.getFEATURE_ID()));
        }
        if (((Number) OGC.getHEIGHT()).doubleValue() != 0) {
            this.QUERYSTRING.put("height", String.valueOf(OGC.getHEIGHT()));
        }
        if (((Number) OGC.getWIDTH()).doubleValue() != 0) {
            this.QUERYSTRING.put("width", String.valueOf(OGC.getWIDTH()));
        }
        Response response = OgcUtils.handleRequest(this.AUTH, this.BASE_URL, this.QUERYSTRING);
        if (OGC.isDownload() || OGC.isDISPLAY()) {
            return OgcUtils.handleImageReturn(OGC, response);
        } else {
            String result = null;
            try {
                assert response.body() != null;
                result = response.body().string();
            } catch (IOException ioe) {
                System.out.println("okHttp3 exception: " + ioe + " please try again");
                System.exit(-1);
            }
            return result;
        }
    }

    /**
     * Runs WMS requests from a collection of bboxes provided in a csv file
     */
    void rerunFailedImages() {

        if (((Number) OGC.getWIDTH()).doubleValue() == 0 ||
            ((Number) OGC.getHEIGHT()).doubleValue() == 0) {
            throw new IllegalArgumentException("Must provide a height and a width");
        }
        this.QUERYSTRING.put("width", String.valueOf(OGC.getWIDTH()));
        this.QUERYSTRING.put("height", String.valueOf(OGC.getHEIGHT()));
        if (OGC.getFilter() != null) {
            OgcUtils.cqlChecker(OGC.getFilter());
            this.QUERYSTRING.put("cql_filter", OGC.getFilter());
        }
        if (OGC.getFEATURE_ID() != null) {
            this.QUERYSTRING.remove("cql_filter");
            this.QUERYSTRING.put("coverage_cql_filter", String.format("featureId='%s'", OGC.getFEATURE_ID()));
        }
        OgcUtils.validateImageFormat(OGC);
        this.QUERYSTRING.put("format", OGC.getParamsImageFormat());
        String filename;
        if (OGC.getFileName() == null) {
            throw new IllegalArgumentException("Must provide a filename for the csv with .filename()");
        } else {
            filename = OGC.getFileName();
            if (!filename.endsWith(".csv")) {
                filename = filename + ".csv";
            }
        }
        ArrayList<String> failedRequests = new ArrayList<>();
        String homeDirectory = OGC.getDownloadPath() != null ? OGC.getDownloadPath() :
            System.getProperty("user.home");
        String csvFile = homeDirectory + "/" + filename;
        String line = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            while ((line = reader.readLine()) != null) {
                this.QUERYSTRING.put("bbox", line);
                Response response = OgcUtils.handleRequest(AUTH, this.BASE_URL, this.QUERYSTRING);
                if (response.code() != 200) {
                    System.out.println("Request failed, appended to failedRequests.csv");
                    failedRequests.add(line);
                }
            }
        } catch (FileNotFoundException fnf) {
            throw new IllegalArgumentException("csv file not found in directory specified: " + fnf);
        } catch (IOException ioe) {
            System.out.println("BufferedReader exception: " + ioe + " please try again");
            System.exit(-1);
        }
       if (failedRequests.size() > 0) {
           try (PrintWriter pw = new PrintWriter(new FileWriter("failedRequests.csv"))) {
               for (String failedRequest : failedRequests) {
                   pw.println(failedRequest);
               }
           } catch (IOException ioe) {
               System.out.println("FileWriter exception, failedRequests.csv could not be created: " + ioe);
           }
       }
    }

    /**
     * Initializes a default querystring which may then be amended based on
     * <code>StreamingBuilder</code> parameters provided to <code>returnImage</code>
     * @return HashMap&lt;String, String&gt; key value pairs for API parameters
     */
    HashMap<String, String> initQueryString() {
        HashMap<String, String> queryString = new HashMap<>();
        String layers = "";
        if (this.OGC.getLayer() == null) {
            switch (this.ENDPOINT) {
                case "streaming" -> layers = "Maxar:Imagery";
                case "basemaps" -> layers = "Maxar:seamline";
                case "analytics" -> throw new IllegalArgumentException("Calls to analytics must"
                    + "have a layer set with .layer()");
            }
        } else {
            layers = this.OGC.getLayer();
        }

        queryString.put("service", "WMS");
        queryString.put("request", "GetMap");
        queryString.put("version", "1.3.0");
        queryString.put("crs", this.srsname);
        queryString.put("height", "512");
        queryString.put("width", "512");
        queryString.put("layers", layers);
        queryString.put("SDKversion", this.VERSION);

        return queryString;
    }

}
