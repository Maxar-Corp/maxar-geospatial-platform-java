package io.github.maxar.MGPSDK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * The Wmts class handles API calls that conform to the OGC WMTS spec. All methods in this
 *  class are protected and cannot be called on the user side. Their functionality must be called
 *  indireclty using {@link Streaming} methods
 */
class Wmts {

    private final Auth AUTH;
    private final String BASE_URL;
    private final String VERSION;
    private final String ENDPOINT;
    private final HashMap<String, String> QUERYSTRING;
    private final Ogc OGC;
    private String srsname;

    /**
     * Constructor for this Wmts class
     * @param ogc Instance of the {@link Streaming} class
     */
    public Wmts(Ogc ogc) {
        this.OGC = ogc;
        this.AUTH = ogc.getAUTH();
        this.ENDPOINT = this.OGC.getENDPOINT();
        switch (this.ENDPOINT) {
            case "streaming" -> this.BASE_URL = String.format("%s/ogc/%s/ogc/gwc/service/wmts",
                this.AUTH.getAPI_BASE_URL(), this.AUTH.getAPI_VERSION());
            case "basemaps" -> this.BASE_URL = String.format("%s/basemaps/%s/seamlines/gwc/service/"
                + "wmts", this.AUTH.getAPI_BASE_URL(), this.AUTH.getAPI_VERSION());
            case "analytics" -> this.BASE_URL = String.format("%s/analytics/%s/vector/"
                + "change-detection/Maxar/gwc/service/wmts", this.AUTH.getAPI_BASE_URL(),
                this.AUTH.getAPI_VERSION());
            default -> this.BASE_URL = "";
        }

        this.VERSION = AUTH.getVERSION();
        this.srsname = this.OGC.getSrsname() == null ? "EPSG:4326" : this.OGC.getSrsname();
        this.QUERYSTRING = this.initQueryString();
    }

    /**
     * Executes a WMTS GetTile request based on a row column and zoom level set with
     * {@link Ogc.Builder} parameters
     * @param tilerow String containing the tile row
     * @param tilecol String containing the tile column
     * @param zoomLevel String containing the zoom level
     * @return <code>Response</code> object containing response from the WMTS call
     */
    public Response wmtsGetTile(String tilerow, String tilecol, String zoomLevel) {


        this.QUERYSTRING.put("TileMatrix", this.QUERYSTRING.get("TileMatrixSet") + ":" +
            zoomLevel);
        this.QUERYSTRING.put("tilerow", tilerow);
        this.QUERYSTRING.put("tilecol", tilecol);
        this.QUERYSTRING.put("format", OGC.getParamsImageFormat());
        this.QUERYSTRING.put("request", "GetTile");
        return OgcUtils.handleRequest(this.AUTH, this.BASE_URL, this.QUERYSTRING);
    }

    /**
     * Generates a list of WMTS calls that can be used to call all tiles contained in a
     * particular AOI based on a bbox and zoom level provided with
     * {@link Ogc.Builder} parameters
     * @param ogc Instance of the {@link Streaming} class
     * @return HashMap&lt;String, String&gt; containing URL, row_col_zoom
     */
    public HashMap<String, String> wmtsBboxGetTileList(Ogc ogc) {

        if (((Number) ogc.getZOOM_LEVEL()).doubleValue() == 0 && ogc.getZOOM_LEVEL() != 0) {
            throw new IllegalArgumentException("Must provide a zoom level");
        }
        OgcUtils.validateBbox(ogc);
        String[] bboxList = ogc.getBbox().split(",");
        double minX = Double.parseDouble(bboxList[1]);
        double minY = Double.parseDouble(bboxList[0]);
        double maxX = Double.parseDouble(bboxList[3]);
        double maxY = Double.parseDouble(bboxList[2]);

        HashMap<String, Integer> results;
        results = this.wmtsConvert(minY, minX, ogc.getZOOM_LEVEL(), ogc.getSrsname());
        long minTileRow = results.get("tileRow");
        long minTileCol = results.get("tileCol");
        results = this.wmtsConvert(maxY, maxX, ogc.getZOOM_LEVEL(), ogc.getSrsname());
        long maxTileRow = results.get("tileRow");
        long maxTileCol = results.get("tileCol");

        if (maxTileRow < minTileRow) {
            //swap variable assignments
            maxTileRow = maxTileRow ^ minTileRow ^ (minTileRow = maxTileRow);
        }
        if (maxTileCol < minTileCol) {
            //swap variable assignments
            maxTileCol = maxTileCol ^ minTileCol ^ (minTileCol = maxTileCol);
        }

        HashMap<String, String> wmtsCallList = new HashMap<>();
        for (long i = minTileCol; i < maxTileCol + 1; i++) {
            for (long j = minTileRow; j < maxTileRow + 1; j++) {
                this.QUERYSTRING.put("TileMatrixSet", this.srsname);
                this.QUERYSTRING.put("TileMatrix", this.QUERYSTRING.get("TileMatrixSet") + ":" +
                    ogc.getZOOM_LEVEL());
                this.QUERYSTRING.put("tileRow", String.valueOf(i));
                this.QUERYSTRING.put("tileCol", String.valueOf(j));
                HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(this.BASE_URL)).newBuilder();
                //Build params from querystring
                for (Map.Entry<String, String> set : this.QUERYSTRING.entrySet()) {
                    urlBuilder.addQueryParameter(set.getKey(), set.getValue());
                }
                String call = urlBuilder.build().toString();
                ArrayList<String> rowCol = new ArrayList<>();
                rowCol.add(this.QUERYSTRING.get("tileRow"));
                rowCol.add(this.QUERYSTRING.get("tileCol"));
                rowCol.add(String.valueOf(ogc.getZOOM_LEVEL()));
                wmtsCallList.put(String.valueOf(rowCol), call);
            }
        }
        return wmtsCallList;
    }

    /**
     * Converts a lat long position to the tile column and row needed to return WMTS
     * imagery over the area
     * @param latY double containing a latitude
     * @param longX double containing a longitude
     * @param zoomLevel int containing a zoom level
     * @param crs String containing the desired projection
     * @return HashMap&lt;String, Integer&gt; containing converted tile row and column values
     */
    public HashMap<String, Integer> wmtsConvert(double latY, double longX, int zoomLevel, String crs) {

        if (crs.equals("EPSG:4326")) {
            /*
                GetCapablities call structure changed from SW2 to SW3, hardcoded TileMatrixSets
                instead of restructuring the XML parser
                fill tileMatrixSet 0 - 21 ->
                first value: {0: {"MatrixWidth": 2, "MatrixHeight": 1}}
                final value: {21: {"MatrixWidth": 4194304, "MatrixHeight": 2097152}}
            */
            HashMap<Integer, HashMap<String, Integer>> tileMatrixSet = new HashMap<>();
            int value1 = 1;
            for (int i = 0; i <= 21; i++) {
                int finalValue1 = value1;
                tileMatrixSet.put(i, new HashMap<String, Integer>() {{
                    put("MatrixWidth", finalValue1 * 2);
                    put("MatrixHeight", finalValue1);
                }});
                value1 = value1 * 2;
            }

            Integer matrixWidth;
            Integer matrixHeight;
            try {
                matrixWidth = tileMatrixSet.get(zoomLevel).get("MatrixWidth");
                matrixHeight = tileMatrixSet.get(zoomLevel).get("MatrixHeight");
            } catch (NullPointerException npe) {
                throw new IllegalArgumentException("Unable to determine Matrix dimensions from "
                    + "input coordinates. Zoom levels for EPSG:4326 should be between - 21");
            }
            HashMap<String, Integer> results = new HashMap<>();
            results.put("tileRow" , (int) Math.round((longX + 180) * (matrixWidth / 360.0)));
            results.put("tileCol" , (int) Math.round((90 - latY) * (matrixHeight / 180.0)));
            return results;
        }
        else {
            CoordinateReferenceSystem sourceCRS;
            CoordinateReferenceSystem targetCRS;
            double transformedLat;
            double transformedLon;
            MathTransform transform;
            try {
                sourceCRS = CRS.decode(OGC.getSrsname());
                targetCRS = CRS.decode("EPSG:4326");
                Coordinate originalCoord = new Coordinate(longX, latY);

                transform = CRS.findMathTransform(sourceCRS, targetCRS);
                Coordinate transformedCoord = JTS.transform(originalCoord, null, transform);
                transformedLat = transformedCoord.y;
                transformedLon = transformedCoord.x;
            } catch (FactoryException | TransformException e) {
                throw new RuntimeException(e);
            }
            double latRads = Math.toRadians(transformedLat);
            double n = Math.pow(2, OGC.getZOOM_LEVEL());
            int xTile = (int) ((transformedLon + 180.0 ) / 360.0 * n);
            double x = Math.tan(latRads);
            int yTile = (int) ((1.0 - (Math.log(x + Math.sqrt(Math.pow(x, 2) + 1))) / Math.PI) /
                2.0 * n);
            HashMap<String, Integer> results = new HashMap<>();
            results.put("tileRow" , xTile);
            results.put("tileCol" , yTile);
            return results;
        }
    }

    /**
     * Initializes a default querystring which may then be amended based on
     * <code>StreamingBuilder</code> parameters provided to <code>Wmts</code>
     * @return HashMap&lt;String, String&gt; key value pairs for API parameters
     */
    private HashMap<String, String> initQueryString() {
        HashMap<String, String> queryString = new HashMap<>();
        String layer = "";
        if (this.OGC.getLayer() == null) {
            switch (this.ENDPOINT) {
                case "streaming" -> layer = "Maxar:Imagery";
                case "basemaps" -> layer = "Maxar:seamline";
                case "analytics" -> throw new IllegalArgumentException("Calls to analytics must"
                    + "have a layer set with .layer()");
            }
        } else {
            layer = this.OGC.getLayer();
        }

        queryString.put("service", "WMTS");
        queryString.put("request", "GetTile");
        queryString.put("TileMatrixSet", "EPSG:4326");
        queryString.put("Layer", layer);
        queryString.put("version", "1.1.0");
        queryString.put("SDKversion", this.VERSION);

        return queryString;
    }

}
