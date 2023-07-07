package io.github.maxar.MGPSDK;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Collection of static helper functions for {@link Streaming} to perform arithmetic or repetitive functions
 */
class OgcUtils {

    /**
     * <code>OgcUtils</code> can not be instantiated
     * @throws UnsupportedOperationException always
     */
    OgcUtils() {
        throw new UnsupportedOperationException("OgcUtils can not be instantiated");
    }

    /**
     * Sends completed API calls for any endpoint and handles any API errors for the user
     * @param auth instance of the <code>Auth</code> class for authentication
     * @param url URL built from the base URL and sub endpoint
     * @param params parameters that get passed and added to the URL
     * @return Response object containing status code and body
     */
    static Response handleRequest(Auth auth, String url, HashMap<String, String> params) {

        String token = auth.refreshToken();
        //HTTP request
        OkHttpClient client = new OkHttpClient()
            .newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        //Build params from querystring
        if (!params.isEmpty()) {
            for (Map.Entry<String, String> set : params.entrySet()) {
                urlBuilder.addQueryParameter(set.getKey(), set.getValue());
            }
        }
        String URL = urlBuilder.build().toString();
//        System.out.println(URL);
        Request getRequest = new Request.Builder()
            .header("Authorization", "Bearer " + token)
            .url(URL)
            .build();
        Response response = null;
        try {
            response = client.newCall(getRequest).execute();
        } catch (IOException ioe) {
            System.out.println("okHttp3 error. Please try again" + ioe);
            System.exit(-1);
        }
        if (response.code() != 200) {
            System.out.printf("error: %s %s", response.code(), response.body());
        }
        return response;
    }


    /**
     * Validates the image format provided and prepends with image/. API parameter requirement
     * @param ogc instance of the <code>Streaming</code> class
     * @throws IllegalArgumentException if provided format is not valid
     */
    static void validateImageFormat(Ogc ogc) throws IllegalArgumentException {

        String[] acceptableFormats = {"jpeg", "png", "geotiff"};
        if (Arrays.asList(acceptableFormats).contains(ogc.getIMAGE_FORMAT())) {
            ogc.setParamsImageFormat("image/" + ogc.getIMAGE_FORMAT());
        } else if (ogc.getIMAGE_FORMAT() == null) {
            ogc.setParamsImageFormat("image/jpeg");
        }
        else {
            throw new IllegalArgumentException("Format not recognized, please use acceptable format"
                + " for downloading image. Format provided: " + ogc.getIMAGE_FORMAT());
        }
    }

    /**
     * Combines the filters passed into .filter() with AND separators
     * @param filterList Arraylist of Strings containing filters
     * @return String containing combined filter
     */
    static String combineFilterList(ArrayList<String> filterList) {

        StringBuilder finalFilter = new StringBuilder();
        for (int i = 0; i < filterList.size(); i++) {
            if (i == filterList.size() - 1) {
                finalFilter.append(String.format("(%s)", filterList.get(i)));
            } else {
                finalFilter.append(String.format("(%s)AND", filterList.get(i)));
            }
        }

        return finalFilter.toString();
    }

    /**
     * Analyzes provided bbox to determine if provided format conforms to API spec
     * @param ogc Instance of the <code>Streaming</code> class
     * @throws IllegalArgumentException If bbox is an incorrect format
     */
    static void validateBbox(Ogc ogc) throws IllegalArgumentException {

        String[] bboxList = ogc.getBbox().split(",");
        if (bboxList.length != 4) {
            throw new IllegalArgumentException("Projection must be exactly 4 coordinates");
        }
        //build map to parse and hold values from bbox
        HashMap<String, Float> bboxData = new HashMap<>();
        try {
            bboxData.put("minY", Float.parseFloat(bboxList[0]));
            bboxData.put("minX", Float.parseFloat(bboxList[1]));
            bboxData.put("maxY", Float.parseFloat(bboxList[2]));
            bboxData.put("maxX", Float.parseFloat(bboxList[3]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bbox coordinates must be numeric.");
        }
        if (bboxData.get("minY") >= bboxData.get("maxY")) {
            throw new IllegalArgumentException("Improper order of bbox: minY is greater than maxY.");
        }
        if (bboxData.get("minX") >= bboxData.get("maxX")) {
            throw new IllegalArgumentException("Improper order of bbox: minX is greater than maxX.");
        }
        if (ogc.getSrsname() == null) {
            if (180 < Math.abs(bboxData.get("minX")) || 180 < Math.abs(bboxData.get("maxX"))) {
                throw new IllegalArgumentException("X coordinates out of range -180 - "
                    + "180. Did you mean to set a srsName?");
            }
            if (90 < Math.abs(bboxData.get("minY")) || 90 < Math.abs(bboxData.get("maxY"))) {
                throw new IllegalArgumentException("Y coordinates out of range -90 - 90. Did "
                    + "you mean to set a srsName?");
            }
        } else {
            if (20048966.1 < Math.abs(bboxData.get("minX")) || 20048966.1 < Math.abs(bboxData
                .get("maxX"))) {
                throw new IllegalArgumentException("X coordinates out of range -20048966.1 - 20048966.1");
            }
            if (20037508.34 < Math.abs(bboxData.get("minY")) || 20037508.34 < Math.abs(bboxData
                .get("maxY"))) {
                throw new IllegalArgumentException("Y coordinates out of range -20037508.34 - 20037508.34");
            }
        }
    }

    /**
     * Processes any bbox and appends the projection if not EPSG:4326
     * @param ogc Instance of the {@link Streaming} class
     * @return String containing the altered bbox
     */
    static String processBbox(Ogc ogc) {

        if (ogc.getSrsname() != null) {
            String[] bboxList = ogc.getBbox().split(",");
            return String.join(",", bboxList[1],  bboxList[0], bboxList[3],  bboxList[2],
                ogc.getSrsname());
        }
        if (ogc.getENDPOINT().equals("analytics")) {
            String[] bboxList = ogc.getBbox().split(",");
            return String.join(",", bboxList[1],  bboxList[0], bboxList[3],  bboxList[2],
                "EPSG:4326");
        }
        return ogc.getBbox();

    }

    /**
     * Handles image return for WMS and WMTS calls. If <code>download</code> or
     * <code>display</code> are specified, calls respective functions
     * @param ogc Instance of the {@link Streaming} class
     * @param response Response containing the API response
     * @return String containing download / display success message for API blob
     * @throws IllegalArgumentException if builder parameters are not set correctly
     */
    static String handleImageReturn(Ogc ogc, Response response) throws IllegalArgumentException {

        assert response.body() != null;
        InputStream stream = response.body().byteStream();
        if (ogc.isDISPLAY()) {
            BufferedImage image = null;
            try {
                image = ImageIO.read(stream);
            } catch (IOException ioe) {
                System.out.println("ImageIO exception: " + ioe + " please try again");
                System.exit(-1);
            }
            ImageIcon icon = new ImageIcon(image);
            JLabel label = new JLabel();
            label.setIcon(icon);
            JPanel panel = new JPanel();
            panel.add(label);
            JFrame frame = new JFrame();
            frame.add(panel);
            frame.pack();
            frame.setVisible(true);
            return "Image displayed";
        }
        if (ogc.isDownload()) {
            String filename = ogc.getFileName() != null ? ogc.getFileName() : "Download";
            String format;
            if (ogc.isCsv()) {
                format = "csv";
            } else if (ogc.isShapeFile()) {
                format = "zip";
            } else {
                format = ogc.getIMAGE_FORMAT();
            }
            if (ogc.getDownloadPath() != null) {
                try {
                    Paths.get(ogc.getDownloadPath());
                } catch (InvalidPathException ipe) {
                    throw new IllegalArgumentException(String.format("Path %s not valid.",
                        ogc.getDownloadPath()));
                }
                try {
                    FileUtils.copyInputStreamToFile(stream, new File(String.format("%s/%s.%s",
                        ogc.getDownloadPath(), filename, format)));
                } catch (IOException ioe) {
                    System.out.println("FileUtils exception: " + ioe + " please try again");
                    System.exit(-1);
                }
                Path path = Paths.get(String.format("%s/%s",
                    ogc.getDownloadPath(), filename));
                return String.format("File downloaded to: " + path);
            } else {
                try {
                    FileUtils.copyInputStreamToFile(stream, new File(String.format("%s/Downloads/%s.%s",
                        System.getProperty("user.home"), filename, format)));
                } catch (IOException ioe) {
                    System.out.println("FileUtils exception: " + ioe + " please try again");
                    System.exit(-1);
                }
                Path path = Paths.get(String.format("%s/Downloads/%s",
                    System.getProperty("user.home"), filename));
                return String.format("File downloaded to: " + path);
            }
        } else {
            throw new IllegalArgumentException("Must indicate whether to display or download with "
                + ".display() or .download()");
        }

    }

    /**
     * Handles multithreaded WMS calls for {@link Streaming#getFullResImage()}
     * @param multiThreadingMap HashMap&lt;String,String&gt; containing API URL, col row pairs
     * @param ogc Instance of the {@link Streaming} class
     * @return int containing the number of failed WMS calls
     * @throws IllegalArgumentException if builder parameters are not set properly
     */
    static int handleMultithreadDownload(HashMap<String, String> multiThreadingMap, Ogc ogc)
    throws IllegalArgumentException {

        // Create a fixed thread pool with .getThreadNumber() threads
        ExecutorService executors = Executors.newFixedThreadPool(ogc.getTHREAD_NUMBER());
        HashMap<String, String> failedRequests = new HashMap<>();
        try {

            //Create a CountDownLatch to block the main thread until all API calls have finished
            CountDownLatch latch = new CountDownLatch(multiThreadingMap.size());

            //Get auth once for all calls
            String token = ogc.getAUTH().refreshToken();

            String downloadPath = ogc.getDownloadPath();

            //Performance measures for user
            double multiple = Math.floor(multiThreadingMap.size() * 0.25);
            final int[] COUNT = {0};
            AtomicInteger percentage = new AtomicInteger();

            int attempts = 0;
            //Set up the HTTP client
            OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .build();

            for (Entry<String, String> entry : multiThreadingMap.entrySet()) {
                executors.execute(() -> {
                    Request getRequest = new Request.Builder()
                        .header("Authorization", "Bearer " + token)
                        .url(entry.getValue())
                        .build();
                    try (Response response = client.newCall(getRequest).execute()){
                        if (response.code() == 200) {
                            COUNT[0]++;
                            if (COUNT[0] % multiple == 0) {
                                percentage.getAndIncrement();
                                System.out.println((percentage.get() * 25) + "% complete");
                            }
                            assert response.body() != null;
                            InputStream file = response.body().byteStream();
                            try {
                                FileUtils.copyInputStreamToFile(file, new File(String.format("%s/%s.%s",
                                    downloadPath, entry.getKey(), ogc.getIMAGE_FORMAT())));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            failedRequests.put(entry.getKey(), entry.getValue());
                        }
                    } catch (IOException e) {
                        failedRequests.put(entry.getKey(), entry.getValue());
                    } finally {
                        //Decrement the latch count to signal that this API call has finished
                        latch.countDown();
                    }
                });
            }



            // Block the main thread until the latch count reaches zero
            try {
                latch.await();
            } catch (InterruptedException ie) {
                System.out.println("Latch countdown failed: " + ie + " aborting download");
                System.exit(-1);
            }

            // Rerun failed images
            if (!failedRequests.isEmpty()) {
                while (attempts < 5) {
                    attempts++;
                    System.out.printf("Some image requests failed, retrying failed requests, "
                        + "retry attempt: %s%n", attempts);
                    int failed = handleMultithreadDownload(failedRequests, ogc);
                    if (failed == 0) {
                        failedRequests.clear();
                        break;
                    }
                }
            }
        } finally {
            executors.shutdown();
            try {
                // Wait for the executor to finish executing tasks
                boolean shutdown = executors.awaitTermination((long) 10.0, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Handle the InterruptedException if needed
                Thread.currentThread().interrupt();
            }
        }
        executors.shutdown();
        return failedRequests.size();
    }

    /**
     * Not yet implemented. Further research needed to determine feasibility. Do not use
     * @param path String containing the path of the tiles to be mosaiced
     * @throws UnsupportedOperationException always
     */
    static void createMosaic(String path) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not implemented. Do not use");
    }

    /**
     * Checks the provided CQL filter to determine API spec compatibility
     * @param cqlFilter String containing the CQL filter
     * @throws IllegalArgumentException if the filter is not properly formatted
     */
    static void cqlChecker(String cqlFilter) throws IllegalArgumentException {

        ArrayList<String> errorArray = new ArrayList<>();
        String[] stringArray = {"featureId", "groundSampleDistanceUnit", "source", "bandDescription",
            "dataLayer", "legacyDescription", "bandConfiguration", "fullResolutionInitiatedOrder",
            "legacyIdentifier", "crs", "processingLevel", "companyName", "orbitDirection",
            "beamMode", "polarisationMode", "polarisationChannel", "antennaLookDirection", "md5Hash",
            "licenseType", "ceCategory", "deletedReason", "productName", "bucketName", "path",
            "sensorType", "sensor", "vehicle_name", "product_name", "catid", "block_name", "uuid",
            "geohash_6", "cam", "change_type", "context", "subregion", "geocell", "product"};
        String[] stringDateArray = {"acquisitionDate", "createdDate", "earliestAcquisitionTime",
            "latestAcquisitionTime", "lastModifiedDate", "deletedDate", "create_date",
            "acq_time_earliest", "acq_time_latest", "acq_time", "change_timestamp",
            "version_timestamp"};
        String[] floatArray = {"groundSampleDistance", "resolutionX", "resolutionY", "niirs",
            "ce90Accuracy", "gsd", "accuracy"};
        String[] booleanArray = {"isEnvelopeGeometry", "isMultiPart", "hasCloudlessGeometry",
            "active"};
        String[] integerArray = {"usageProductId", "change_area_size_sqm"};
        String[] sourceArray = {"WV01", "WV02", "WV03_VNIR", "WV03", "WV04", "GE01", "QB02", "KS3",
            "KS3A", "WV03_SWIR", "KS5", "RS2", "IK02", "LG01", "LG02"};
        String[] zero360Array = {"sunAzimuth", "sunElevation", "offNadirAngle",
            "minimumIncidenceAngle", "maximumIncidenceAngle", "incidenceAngleVariation", "ona",
            "ona_avg", "sunel_avg", "sun_az_avg", "target_az_avg"};
        String[] zero1Array = {"cloudCover"};
        if (cqlFilter == null) {
            errorArray.add("Filter can not be null");
            throw new IllegalArgumentException("CQL filters Error:" + errorArray);
        }
        if (StringUtils.countMatches(cqlFilter, ")") != StringUtils.countMatches(cqlFilter,
            "(") ||
            cqlFilter.indexOf(")") < cqlFilter.indexOf("(")) {
            errorArray.add("Incorrect parenthesis");
        }
        String comparisons = "<=|>=|<|>|=";
        Pattern pattern = Pattern.compile(comparisons, Pattern.MULTILINE);
        Matcher m = pattern.matcher(cqlFilter);
        if (!m.find()) {
            errorArray.add("No comparison operator e.g. < > =");
        }
        String[] cqlArray = cqlFilter.split("\\)\\s*(AND|OR)\\s*\\(");
        for (String s : cqlArray) {
            String[] filter = s.replaceAll("[()]", "").split(comparisons);
            if (Objects.equals(filter[0], "source") && !Arrays.asList(sourceArray)
                .contains(filter[0])) {
                errorArray.add(filter[0] + " should be one of: " + Arrays.asList(sourceArray));
            } else if (Arrays.asList(floatArray).contains(filter[0])) {
                try {
                    Float.parseFloat(filter[1]);
                } catch (NumberFormatException e) {
                    errorArray.add(filter[1] + " is not a float");
                }
            } else if (Arrays.asList(booleanArray).contains(filter[0])) {
                if (!Objects.equals(filter[1], "FALSE") || !Objects.equals(filter[1], "TRUE")) {
                    errorArray.add(filter[1] + " should be TRUE or FALSE");
                }
            } else if (Arrays.asList(integerArray).contains(filter[0])) {
                try {
                    Integer.parseInt(filter[1]);
                } catch (NumberFormatException nfe) {
                    errorArray.add(filter[1] + " is not an integer");
                }
            } else if (Arrays.asList(stringDateArray).contains(filter[0])) {
                String date = filter[1].replaceAll("'", "");
                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd",
                    Locale.ENGLISH);
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd "
                    + "hh:mm:ss", Locale.ENGLISH);
                try {
                    LocalDate.parse(date, formatter1);
                } catch (java.time.DateTimeException dte) {
                    try {
                        LocalDate.parse(date, formatter2);
                    } catch (java.time.DateTimeException dte2) {
                        errorArray.add(filter[1] + " not a valid date");
                    }
                }
            } else if (Arrays.asList(zero1Array).contains(filter[0])) {
                try {
                    float flt = Float.parseFloat(filter[1]);
                    if (flt < 0 || flt > 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException nfe) {
                    errorArray.add(filter[1] + " must represent a number between 0 and 1");
                }
            } else if (Arrays.asList(zero360Array).contains(filter[0])) {
                try {
                    float flt = Float.parseFloat(filter[1]);
                    if (flt < 0 || flt > 360) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException nfe) {
                    errorArray.add(filter[1] + " must represent a number between 0 and 360");
                }
            } else if (Arrays.asList(stringArray).contains(filter[0])) {
                if (!filter[1].startsWith("'") || !filter[1].endsWith("'")) {
                    errorArray.add(filter[1] + " must be wrapped with single quotes. Eg: " + "'" +
                        filter[1] + "'");
                }
            } else {
                errorArray.add(String.format("%s %s is not a valid filter", filter[0], filter[1]));
            }
        }
        if (!errorArray.isEmpty()) {
            throw new IllegalArgumentException(String.valueOf(errorArray));
        }
    }
}
