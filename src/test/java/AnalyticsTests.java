import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.maxar.MGPSDK.Analytics;
import io.github.maxar.MGPSDK.AnalyticsFeatureCollection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AnalyticsTests {

    private final String BBOX = "24.678218,54.773712,25.725684,56.115417";
    private final String BBOX_3857 = "4851060.1776836105,-11712956.404248431,4870628.056924616,"
        + "-11693388.525007427";
    private final String FILTER_1 = "sensor='Landsat'";
    private final String FILTER_2 = "sensor='Sentinel-2'";
    private final String LAYER = "Maxar:layer_pcm_eo_2020";
    private long bytes;

    @Test
    @DisplayName("WFS Search with a bbox")
    void TestWfsSearchWithBbox() {
        Analytics analyticsTest = Analytics.builder()
            .bbox(BBOX)
            .typeName(LAYER)
            .srsname("EPSG:4326")
            .build();
        AnalyticsFeatureCollection results = analyticsTest.search();
        assertEquals(results.numberReturned(), results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS Search with 3857 Bbox")
    void TestWfsSearchWith3857Bbox() {
        Analytics analyticsTest = Analytics.builder()
            .bbox(BBOX_3857)
            .srsname("EPSG:3857")
            .typeName("Maxar:layer_pcm_eo_2020")
            .build();
        AnalyticsFeatureCollection results = analyticsTest.search();
        assertEquals(results.numberReturned(), results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS Search with 3857 bbox and CQL filter")
    void TestWfsSearchWith3857BboxAndFilter() {
        Analytics analyticsTest = Analytics.builder()
            .bbox(BBOX_3857)
            .srsname("EPSG:3857")
            .typeName("Maxar:layer_pcm_eo_2020")
            .filter(FILTER_1)
            .build();
        AnalyticsFeatureCollection results = analyticsTest.search();
        assertEquals(results.numberReturned(), results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS Search with bbox and CQL filter")
    void TestWfsSearchWithBboxAndFilter() {
        Analytics analyticsTest = Analytics.builder()
            .filter(FILTER_2)
            .bbox(BBOX)
            .typeName("Maxar:layer_pcm_eo_2020")
            .srsname("EPSG:4326")
            .build();
        AnalyticsFeatureCollection results = analyticsTest.search();
        assertEquals(results.numberReturned(), results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("Test bad bbox")
    void TestWfsSearchWithMalformedBbox() {
        String badBbox = "99,-105.05608,39.95133,-104.94827";
        Analytics analyticsTest = Analytics.builder()
            .bbox(badBbox)
            .srsname("EPSG:4326")
            .typeName("Maxar:layer_pcm_eo_2020")
            .build();
        assertThrows(IllegalArgumentException.class, analyticsTest::search);
    }

    @Test
    @DisplayName("Test bad CQL filter")
    void TestWfsSearchWithBadCqlFilter() {
        String badFilter = "(acquisitionDate>='2022-01-01'cloudCover<0.20)";
        Analytics analyticsTest = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(badFilter)
            .typeName("Maxar:layer_pcm_eo_2020")
            .build();
        assertThrows(IllegalArgumentException.class, analyticsTest::search);
    }

    @Test
    @DisplayName("WFS Search for shapefile")
    void TestShapefileDownload() {
        Analytics testAnalytics = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .typeName("Maxar:layer_pcm_eo_2020")
            .filter(FILTER_1)
            .fileName("test")
            .build();

        testAnalytics.downloadShapeFile();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.zip");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WFS Search for CSV")
    void TestCsvDownload() {
        Analytics testAnalytics = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(FILTER_1)
            .fileName("test")
            .typeName("Maxar:layer_pcm_eo_2020")
            .build();

        testAnalytics.downloadCsv();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.csv");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS 4326 bbox with filter JPEG")
    void testImageDownload() {
        Analytics testAnalytics = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(FILTER_1)
            .layer("Maxar:layer_pcm_eo_2020")
            .height(500)
            .width(500)
            .imageFormat("jpeg")
            .fileName("test")
            .download()
            .build();

        testAnalytics.downloadImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.jpeg");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS 3857 Bbox with filter PNG")
    void test3857Download() {
        String bbox3857 = "4828455.4171,-11686562.3554,4830614.7631,-11684030.3789";
        Analytics testAnalytics = Analytics.builder()
            .bbox(bbox3857)
            .srsname("EPSG:3857")
            .height(500)
            .width(500)
            .layer("Maxar:layer_pcm_eo_2020")
            .imageFormat("png")
            .fileName("test")
            .download()
            .build();

        testAnalytics.downloadImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.png");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS download to home directory GeoTiff")
    void testCustomDirectoryDownload() {
        Analytics testAnalytics = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .layer("Maxar:layer_pcm_eo_2020")
            .height(500)
            .width(500)
            .imageFormat("geotiff")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .download()
            .build();

        testAnalytics.downloadImage();
        File file = new File(System.getProperty("user.home") + "/test.geotiff");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS malformed bbox (Out of limits) JPEG")
    void testBadBboxDownload() {
        String badBbox = "99,-105.05608,39.95133,-104.94827";
        Analytics testAnalytics = Analytics.builder()
            .bbox(badBbox)
            .srsname("EPSG:4326")
            .layer("Maxar:layer_pcm_eo_2020")
            .height(500)
            .width(500)
            .imageFormat("jpeg")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .build();

        assertThrows(IllegalArgumentException.class, testAnalytics::downloadImage);
    }

    @Test
    @DisplayName("WMS bbox out of order PNG")
    void testUnorderedBboxDownload() {
        String unorderedBbox = "-105.05608,39.95133,-104.94827,39.84387";
        Analytics testAnalytics = Analytics.builder()
            .bbox(unorderedBbox)
            .srsname("EPSG:4326")
            .layer("Maxar:layer_pcm_eo_2020")
            .height(500)
            .width(500)
            .imageFormat("png")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .build();

        assertThrows(IllegalArgumentException.class, testAnalytics::downloadImage);
    }

    @Test
    @DisplayName("WMS incorrect image dimensions")
    void testBadImageSize() {
        Analytics testAnalytics = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .layer("Maxar:layer_pcm_eo_2020")
            .height(0)
            .width(500)
            .imageFormat("png")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .build();

        assertThrows(IllegalArgumentException.class, testAnalytics::downloadImage);
    }

    @Test
    @DisplayName("WMTS 4326 bbox zoom level 11")
    void testWmtsList() {
        Analytics wmtsListTest = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .layer("Maxar:layer_pcm_eo_2020")
            .zoomLevel(11)
            .build();

        HashMap<String, String> results = wmtsListTest.getTileList();
        assertTrue(results.size() > 0);
    }

    @Test
    @DisplayName("WMTS 4326 improper zoom level")
    void testWmtsListBadZoom() {
        Analytics wmtsListTest = Analytics.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .layer("Maxar:layer_pcm_eo_2020")
            .zoomLevel(35)
            .build();

        assertThrows(IllegalArgumentException.class, wmtsListTest::getTileList);
    }

    @Test
    @DisplayName("WMTS 3857 bbox zoom level 11")
    void testWmtsList3857() {
        String bbox3857 = "4828455.4171,-11686562.3554,4830614.7631,-11684030.3789";
        Analytics wmtsListTest = Analytics.builder()
            .bbox(bbox3857)
            .srsname("EPSG:3857")
            .layer("Maxar:layer_pcm_eo_2020")
            .zoomLevel(11)
            .build();

        HashMap<String, String> results = wmtsListTest.getTileList();
        assertTrue(results.size() > 0);
    }
}
