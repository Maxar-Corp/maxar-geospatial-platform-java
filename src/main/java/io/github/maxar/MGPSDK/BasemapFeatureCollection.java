package io.github.maxar.MGPSDK;

import org.locationtech.jts.geom.Geometry;

/**
 * Record to hold the results of an Analytics WFS call so that all parts of the result are
 * queryable
 *
 * @param type String for type of collection. Always FeatureCollection
 * @param features Array of the contained Features
 * @param totalFeatures int containing the number of features available in that AOI
 * @param numberMatched int containing the number of features matched in the call
 * @param numberReturned int containing the number of features returned by the call
 * @param timestamp String containing timestamp of the call
 * @param crs Crs object
 */
public record BasemapFeatureCollection(String type, Features[] features, int totalFeatures,
                                       int numberMatched, int numberReturned, String timestamp,
                                       Crs crs) {

    /**
     * Record for features information
     *
     * @param type String containing the type of feature. Usually Feature
     * @param id String of the feature ID
     * @param geometry Geometry from the Locationtech JTS Geometry class
     * @param geometry_name String containing the geometry name
     * @param properties Properties object containing the metadata for the feature
     */
    public record Features(String type, String id, Geometry geometry, String geometry_name,
                           Properties properties) {}

    /**
     * Record for displaying metadata information for each feature
     */
    public record Properties(String createdDate, String gsd, String accuracy,
                             String acq_time_earliest, String acq_time_latest, String acq_time,
                             String sensor, String ona, String ona_avg, String sunel_avg,
                             String sun_az_avg, String target_az_avg, String vehicle_name,
                             String product_name, String product_create_time, String catid,
                             String block_name, boolean active){}

    /**
     * CRS object containing the fully qualified projection name
     *
     * @param type String of the parameter in propertied
     * @param properties Object containing the properties for the projection
     */
    public record Crs(String type, Object properties){}
}



