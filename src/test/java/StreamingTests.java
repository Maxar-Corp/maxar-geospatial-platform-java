import static org.junit.jupiter.api.Assertions.*;
import io.github.maxar.MGPSDK.Streaming;
import io.github.maxar.MGPSDK.StreamingFeatureCollection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StreamingTests {

    private final String BBOX = "39.84387,-105.05608,39.95133,-104.94827";
    private final String BBOX_3857 = "4828455.4171,-11686562.3554,4830614.7631,-11684030.3789";
    private final String FEATURE_ID = "8faebe9b-da34-74f5-27e6-feb05ab7a82a";
    private final String FILTER_1 = "acquisitionDate>='2022-01-01'";
    private final String FILTER_2 = "cloudCover<0.20";
    private final String RAW_FILTER = "(acquisitionDate>='2022-01-01')AND(cloudCover<0.20)";
    private long bytes;

    @Test
    @DisplayName("WFS Search with a bbox")
    void TestWfsSearchWithBbox() {
        Streaming streamingTest = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .build();
        StreamingFeatureCollection results = streamingTest.search();

        assertEquals(results.numberReturned(),results.features().length);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS Search with 3857 Bbox")
    void TestWfsSearchWith3857Bbox() {
        Streaming streamingTest = Streaming.builder()
            .bbox(BBOX_3857)
            .srsname("EPSG:3857")
            .build();
        StreamingFeatureCollection results = streamingTest.search();;
        assertEquals(results.numberReturned(),results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS Search with 3857 bbox and CQL filter")
    void TestWfsSearchWith3857BboxAndFilter() {
        Streaming streamingTest = Streaming.builder()
            .bbox(BBOX_3857)
            .srsname("EPSG:3857")
            .filter(FILTER_1)
            .filter(FILTER_2)
            .build();
        StreamingFeatureCollection results = streamingTest.search();;
        assertEquals(results.numberReturned(),results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS search with feature ID")
    void TestWfsSearchWithFeatureID() {
        String featureID = "57d0e26239dde11463d31ff0893ce9ca";
        Streaming streamingTest = Streaming.builder()
            .featureId(featureID)
            .build();
        StreamingFeatureCollection results = streamingTest.search();;
        assertEquals(results.numberReturned(),results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("WFS Search with bbox and CQL filter")
    void TestWfsSearchWithBboxAndFilter() {
        Streaming streamingTest = Streaming.builder()
            .filter(FILTER_1)
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .build();
        StreamingFeatureCollection results = streamingTest.search();;
        assertEquals(results.numberReturned(),results.features().length);
        assertTrue(results.numberReturned() > 0);
        System.out.println("Number of results: " + results.features().length);
    }

    @Test
    @DisplayName("Test bad bbox")
    void TestWfsSearchWithMalformedBbox() {
        String badBbox = "99,-105.05608,39.95133,-104.94827";
        Streaming streamingTest = Streaming.builder()
            .bbox(badBbox)
            .build();
        assertThrows(IllegalArgumentException.class, streamingTest::search);
    }

    @Test
    @DisplayName("Test bad CQL filter")
    void TestWfsSearchWithBadCqlFilter() {
        String badFilter = "(acquisitionDate>='2022-01-01'cloudCover<0.20)";
        Streaming streamingTest = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(badFilter)
            .build();
        assertThrows(IllegalArgumentException.class, streamingTest::search);
    }

    @Test
    @DisplayName("WFS Search for shapefile")
    void TestShapefileDownload() {
        Streaming testStreaming = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(FILTER_2)
            .filter(FILTER_1)
            .fileName("test")
            .build();

        testStreaming.downloadShapeFile();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.zip");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WFS Search for CSV")
    void TestCsvDownload() {
        Streaming testStreaming = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(FILTER_1)
            .fileName("test")
            .build();

        testStreaming.downloadCsv();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.csv");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS 4326 bbox with rraw filter JPEG")
    void testImageDownload() {
        Streaming testStreaming = Streaming.builder()
            .bbox(BBOX_3857)
            .srsname("EPSG:3857")
            .filter(RAW_FILTER)
            .height(500)
            .width(500)
            .imageFormat("jpeg")
            .fileName("test")
            .download()
            .build();

        testStreaming.downloadImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.jpeg");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists() && bytes > 5500);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS 3857 Bbox with filter PNG")
    void test3857Download() {
        String bbox3857 = "4828455.4171,-11686562.3554,4830614.7631,-11684030.3789";
        Streaming testStreaming = Streaming.builder()
            .bbox(bbox3857)
            .srsname("EPSG:3857")
            .filter(FILTER_1)
            .height(500)
            .width(500)
            .imageFormat("png")
            .fileName("test")
            .download()
            .build();

        testStreaming.downloadImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/test.png");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists() && bytes > 5500);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS download to home directory GeoTiff")
    void testCustomDirectoryDownload() {
        Streaming testStreaming = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .filter(FILTER_1)
            .height(500)
            .width(500)
            .imageFormat("geotiff")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .download()
            .build();

        testStreaming.downloadImage();
        File file = new File(System.getProperty("user.home") + "/test.geotiff");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists() && bytes > 5500);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS Legacy ID GeoTiff")
    void testLegacyIdIdSearch() {
        Streaming testStreaming = Streaming.builder()
            .legacyId("10200100D323B800")
            .imageFormat("geotiff")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .download()
            .build();

        testStreaming.downloadImage();
        File file = new File(System.getProperty("user.home") + "/test.geotiff");
        try {
            bytes = Files.size(file.toPath());
        } catch (IOException e) {
            System.out.println("File read error");
        }
        assertTrue(file.exists() && bytes > 5500);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("WMS malformed bbox (Out of limits) JPEG")
    void testBadBboxDownload() {
        String badBbox = "99,-105.05608,39.95133,-104.94827";
        Streaming testStreaming = Streaming.builder()
            .bbox(badBbox)
            .srsname("EPSG:4326")
            .height(500)
            .width(500)
            .imageFormat("jpeg")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .build();

        assertThrows(IllegalArgumentException.class, testStreaming::downloadImage);
    }

    @Test
    @DisplayName("WMS bbox out of order PNG")
    void testUnorderedBboxDownload() {
        String unorderedBbox = "-105.05608,39.95133,-104.94827,39.84387";
        Streaming testStreaming = Streaming.builder()
            .bbox(unorderedBbox)
            .srsname("EPSG:4326")
            .height(500)
            .width(500)
            .imageFormat("png")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .build();

        assertThrows(IllegalArgumentException.class, testStreaming::downloadImage);
    }

    @Test
    @DisplayName("WMS incorrect image dimensions")
    void testBadImageSize() {
        Streaming testStreaming = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .height(0)
            .width(500)
            .imageFormat("png")
            .downloadPath(System.getProperty("user.home"))
            .fileName("test")
            .build();

        assertThrows(IllegalArgumentException.class, testStreaming::downloadImage);
    }

    @Test
    @DisplayName("WMTS 4326 bbox zoom level 11")
    void testWmtsList() {
        Streaming wmtsListTest = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .featureId(FEATURE_ID)
            .zoomLevel(11)
            .build();

        HashMap<String, String> results = wmtsListTest.getTileList();
        assertTrue(results.size() > 0);
    }

    @Test
    @DisplayName("WMTS 4326 improper zoom level")
    void testWmtsListBadZoom() {
        Streaming wmtsListTest = Streaming.builder()
            .bbox(BBOX)
            .srsname("EPSG:4326")
            .zoomLevel(35)
            .build();

        assertThrows(IllegalArgumentException.class, wmtsListTest::getTileList);
    }

    @Test
    @DisplayName("WMTS 3857 bbox zoom level 11")
    void testWmtsList3857() {
        String bbox3857 = "4828455.4171,-11686562.3554,4830614.7631,-11684030.3789";
        Streaming wmtsListTest = Streaming.builder()
            .bbox(bbox3857)
            .srsname("EPSG:3857")
            .zoomLevel(11)
            .build();

        HashMap<String, String> results = wmtsListTest.getTileList();
        assertTrue(results.size() > 0);
    }

    @Test
    @DisplayName("Full res download jpeg")
    void testFullResDownload() {
        Streaming fullResTest = Streaming.builder()
            .featureId(FEATURE_ID)
            .imageFormat("jpeg")
            .downloadPath(System.getProperty("user.home") + "/Downloads/TestFullRes")
            .threadNumber(100)
            .build();

        fullResTest.getFullResImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/TestFullRes");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Full res download with bbox jpeg")
    void testFullResDownloadWithBbox() {
        String bbox = "39.84387,-105.05608,39.95133,-104.94827";
        Streaming fullResTest = Streaming.builder()
            .featureId(FEATURE_ID)
            .bbox(bbox)
            .srsname("EPSG:4326")
            .imageFormat("jpeg")
            .downloadPath(System.getProperty("user.home") + "/Downloads/TestFullRes")
            .threadNumber(75)
            .build();

        fullResTest.getFullResImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/TestFullRes");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Full res download with bbox geotiff")
    void testFullResDownloadWithBboxGeotiff() {
        String bbox = "39.84387,-105.05608,39.95133,-104.94827";
        Streaming fullResTest = Streaming.builder()
            .featureId(FEATURE_ID)
            .bbox(bbox)
            .srsname("EPSG:4326")
            .imageFormat("geotiff")
            .downloadPath(System.getProperty("user.home") + "/Downloads/TestFullRes")
            .threadNumber(75)
            .build();

        fullResTest.getFullResImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/TestFullRes");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Full res download with 3857 bbox")
    void testFullResDownloadWith3857Bbox() {
        String bbox3857 = "4828455.4171,-11686562.3554,4830614.7631,-11684030.3789";
        Streaming fullResTest = Streaming.builder()
            .featureId(FEATURE_ID)
            .bbox(bbox3857)
            .srsname("EPSG:3857")
            .imageFormat("jpeg")
            .downloadPath(System.getProperty("user.home") + "/Downloads/TestFullRes")
            .threadNumber(75)
            .build();

        fullResTest.getFullResImage();
        File file = new File(System.getProperty("user.home") + "/Downloads/TestFullRes");
        assertTrue(file.exists());
        if (file.exists()) {
            file.delete();
        }
    }


}
