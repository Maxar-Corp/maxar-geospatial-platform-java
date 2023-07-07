import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.maxar.MGPSDK.Streaming;
import io.github.maxar.MGPSDK.Streaming.Builder;
import io.github.maxar.MGPSDK.StreamingFeatureCollection;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthTest {

    @Test
    @DisplayName("Test for properly formatted .MGP-CONFIG")
    void testGoodConfigFile() {
        Streaming authTest = Streaming.builder()
            .bbox("39.84387,-105.05608,39.95133,-104.94827")
            .srsname("EPSG:4326")
            .build();

        StreamingFeatureCollection results = authTest.search();;
        assertEquals(results.numberReturned(),results.features().length);
        assertTrue(results.numberReturned() > 0);
    }

    @Test
    @DisplayName("Test good passed in credentials")
    void testGoodBuilderCredentials() {
        String username;
        String password;
        String clientId;
        String homeDirectory = System.getProperty("user.home");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(homeDirectory + "/.MGP-config"));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(".MGP-Config file not found in user home directory");
        }
        try {
            reader.readLine();
            username = reader.readLine().split("=")[1].trim();
            password = reader.readLine().split("=")[1].trim();
            clientId = reader.readLine().split("=")[1].trim();
        } catch (IOException e) {
            throw new IllegalArgumentException(".MGP-Config file not formatted correctly");
        }
        Streaming authTest = Streaming.builder()
            .bbox("39.84387,-105.05608,39.95133,-104.94827")
            .srsname("EPSG:4326")
            .username(username)
            .password(password)
            .clientId(clientId)
            .build();

        StreamingFeatureCollection results = authTest.search();;
        assertEquals(results.numberReturned(),results.features().length);
        assertTrue(results.numberReturned() > 0);

    }

    @Test
    @DisplayName("Test no password")
    void testBadAuthNoPassword() {
        Builder ogcTest = Streaming.builder()
            .username("no username")
            .clientId("no client")
            .bbox("39.84387,-105.05608,39.95133,-104.94827");

        assertThrows(IllegalArgumentException.class, ogcTest::build);
    }

    @Test
    @DisplayName("Test no username")
    void testBadAuthNoUsername() {
        Builder ogcTest = Streaming.builder()
            .password("no password")
            .clientId("no client")
            .bbox("39.84387,-105.05608,39.95133,-104.94827");

        assertThrows(IllegalArgumentException.class, ogcTest::build);
    }

    @Test
    @DisplayName("Test no client ID")
    void testBadAuthNoClientId() {
        Builder ogcTest = Streaming.builder()
            .username("no username")
            .password("no password")
            .bbox("39.84387,-105.05608,39.95133,-104.94827");

        assertThrows(IllegalArgumentException.class, ogcTest::build);
    }

    @Test
    @DisplayName("Test bad passed credentials")
    void testBadCredentials() {
        Builder ogcTest = Streaming.builder()
            .password("no password")
            .username("no username")
            .clientId("no client")
            .bbox("39.84387,-105.05608,39.95133,-104.94827");

        assertThrows(IllegalArgumentException.class, ogcTest::build);
    }

}
