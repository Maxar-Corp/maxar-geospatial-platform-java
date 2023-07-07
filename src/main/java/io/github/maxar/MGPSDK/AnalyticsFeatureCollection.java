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
 * @param bbox double[] bbox of the AOI
 */
public record AnalyticsFeatureCollection(String type, Features[] features, int totalFeatures,
                                         int numberMatched, int numberReturned, String timestamp,
                                         Crs crs, double[] bbox) {


        /**
         * Record for features information
         *
         * @param type String containing the type of feature. Usually Feature
         * @param id String of the feature ID
         * @param geometry Geometry from the Locationtech JTS Geometry class
         * @param geometry_name String containing the geometry name
         * @param properties Properties object containing the metadata for the feature
         * @param bbox double[] containing the bbox
         */
        public record Features(String type, String id, Geometry geometry, String geometry_name,
                               AnalyticsFeatureCollection.Features.Properties properties,
                               double[] bbox) {


                        /**
                         * Record for displaying metadata information for each feature
                         */
                        public record Properties(String uuid, String sensor,
                                                 String change_area_size_sqm, String geohash_6,
                                                 String change_timestamp, String cam,
                                                 String change_type, String context,
                                                 String subregion, String geocell, String product,
                                                 String version_timestamp){}
        }

        /**
         * CRS object containing the fully qualified projection name
         *
         * @param type String of the parameter in propertied
         * @param properties Object containing the properties for the projection
         */
        public record Crs(String type, Object properties) {}

}