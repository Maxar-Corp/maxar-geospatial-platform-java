package io.github.maxar.MGPSDK;

import java.util.HashMap;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * The Wfs class supports making API calls that conform to OGC WFS spec. All methods in this
 * class are protected and cannot be called on the user side. Their functionality must be called
 * indireclty using {@link Streaming} methods
 */
class Wfs {

    private final Auth AUTH;
    private final String BASE_URL;
    private final String VERSION;
    private final HashMap<String, String> QUERYSTRING;
    private final Ogc OGC;
    private final String ENDPOINT;

    /**
     * Constructor for <code>Wfs</code>
     * @param ogc Instance of the {@link Streaming} class
     */
    Wfs(Ogc ogc) {
        this.OGC = ogc;
        this.AUTH = this.OGC.getAUTH();
        this.ENDPOINT = ogc.getENDPOINT();
        switch (this.ENDPOINT) {
            case "streaming" -> this.BASE_URL = String.format("%s/streaming/%s/ogc/wfs",
                this.AUTH.getAPI_BASE_URL(), this.AUTH.getAPI_VERSION());
            case "basemaps" -> this.BASE_URL = String.format("%s/basemaps/%s/seamlines/wfs",
                this.AUTH.getAPI_BASE_URL(), this.AUTH.getAPI_VERSION());
            case "analytics" ->
                this.BASE_URL = String.format("%s/analytics/%s/vector/change-detection/Maxar/ows",
                    this.AUTH.getAPI_BASE_URL(), this.AUTH.getAPI_VERSION());
            default -> this.BASE_URL = "";
        }

        this.VERSION = this.AUTH.getVERSION();
        this.QUERYSTRING = this.initQueryString();
    }

    /**
     * <p>Performs a WFS search using the parameters provided by the <code>OGC</code> child classes</p>
     * <p>Optional <code>OgcBuilder</code> parameters:</p>
     * <ul>
     *     <li>{@link Ogc.Builder#bbox(String)}</li>
     *     <li>{@link Ogc.Builder#filter(String)}</li>
     *     <li>{@link Ogc.Builder#featureId(String)}</li>
     *     <li>{@link Ogc.Builder#srsname(String)}</li>
     *     <li>{@link Ogc.Builder#shapefile()}</li>
     *     <li>{@link Ogc.Builder#csv()}</li>
     *     <li>{@link Ogc.Builder#requestType(String)}</li>
     *     <li>{@link Ogc.Builder#typeName(String)}</li>
     * </ul>
     * @return <code>Response</code> object containing response from API
     */
    Response search() {

        Ogc ogc = this.OGC;
        String bbox = ogc.getBbox();
        String filter = ogc.getFilter();
        String featureID = ogc.getFEATURE_ID();

        if (featureID != null) {
            filter = String.format("featureId='%s'", featureID);
        }

        String srsname;


        if (bbox != null) {
            OgcUtils.validateBbox(ogc);
            if (this.OGC.getSrsname() == null) {
                throw new IllegalArgumentException("Must provide the projection with .srsname()");
            } else {
                srsname = this.OGC.getSrsname();
            }
            this.QUERYSTRING.put("srsname", srsname);
            String bboxList = OgcUtils.processBbox(ogc);
            if (filter != null) {
                this.combineBboxAndFilter(bboxList, filter, srsname);
            } else {
                this.QUERYSTRING.put("bbox", bboxList);
            }
        } else if (filter != null) {
            this.QUERYSTRING.put("cql_filter", filter);
        } else {
            throw new IllegalArgumentException("Search function must have a BBOX or a Filter");
        }
        if (ogc.getREQUEST_TYPE() != null) {
            this.QUERYSTRING.put("request", ogc.getREQUEST_TYPE());
            this.QUERYSTRING.remove("outputFormat");
        }

        if (ogc.isShapeFile()) {
            this.QUERYSTRING.put("outputFormat", "shape-zip");
        } else if (ogc.isCsv()) {
            this.QUERYSTRING.put("outputFormat", "csv");
        }

        return OgcUtils.handleRequest(this.AUTH, this.BASE_URL, this.QUERYSTRING);

    }

    /**
     * Initializes a default querystring which may then be amended based on
     * <code>StreamingBuilder</code> parameters provided to <code>search</code>
     * @return HashMap&lt;String, String&gt; key value pairs for API parameters
     */
    private HashMap<String, String> initQueryString() {
        HashMap<String, String> queryString = new HashMap<>();
        if (this.OGC.getTypeName() == null) {
            switch (this.ENDPOINT) {
                case "streaming" -> this.OGC.setTypename("Maxar:FinishedFeature");
                case "basemaps" -> this.OGC.setTypename("seamline");
                case "analytics" -> throw new IllegalArgumentException("Analytics calls must have "
                    + "a typename set with .typename()");
            }
        }
        queryString.put("service", "WFS");
        queryString.put("request", "GetFeature");
        queryString.put("typename", this.OGC.getTypeName());
        queryString.put("outputFormat", "application/json");
        queryString.put("version", "2.0.0");
        queryString.put("SDKversion", this.VERSION);

        return queryString;
    }

    /**
     * Combines the bbox and filter into a single query if both are provided. Adds combined
     * query to a cql_filter parameter
     * @param bbox String containing the bbox
     * @param filter String containing the filter
     * @param srsname String containing the projection
     */
    private void combineBboxAndFilter(String bbox, String filter, String srsname) {
        String geometry = "";
        switch (this.ENDPOINT) {
            case "streaming" -> geometry = "featureGeometry";
            case "basemaps" -> geometry = "seamline_geometry";
            case "analytics" -> {
                geometry = "change_area_polygon_3857";
            }
        }
        String[] bboxList = bbox.split(",");
        bboxList[4] = String.format("'%s'", srsname);
        bbox = StringUtils.join(bboxList, ",");
        String bboxGeometry = String.format("BBOX(%s,%s)",geometry, bbox);
        String combinedFilter = bboxGeometry + "AND(" + filter + ")";
        this.QUERYSTRING.put("cql_filter", combinedFilter);
    }

}
