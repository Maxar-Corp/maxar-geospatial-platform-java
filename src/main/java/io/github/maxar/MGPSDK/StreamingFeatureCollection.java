package io.github.maxar.MGPSDK;
import org.locationtech.jts.geom.Geometry;

/**
 * Record to hold the results of a Streaming WFS call so that all parts of the result are
 * queryable
 * @param type String for type of collection. Always FeatureCollection
 * @param features Array of the contained Features
 * @param totalFeatures int containing the number of features available in that AOI
 * @param numberReturned int containing the number of features returned by the call
 * @param timestamp String containing timestamp of the call
 * @param crs Crs object
 */
public record StreamingFeatureCollection(String type, Features[] features, String totalFeatures,
                                         int numberReturned, String timestamp, Crs crs) {

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
                               Properties properties) {


                        /**
                         * record for displaying metadata information for each feature
                         */
                        public record Properties(String featureId, String cloudCover, String sunAzimuth,
                                                 String sunElevation, String offNadirAngle,
                                                 String groundSampleDistance, String groundSampleDistanceUnit,
                                                 String source, String bandDescription,
                                                 String isEnvelopeGeometry,
                                                 Features.Properties.Centroid centroid, String dataLayer,
                                                 String legacyDescription, String bandConfiguration,
                                                 String fullResolutionInitiatedOrder, String legacyIdentifier,
                                                 String crs, String acquisitionDate, String resolutionX,
                                                 String resolutionY, String createdDate, String processingLevel,
                                                 String earliestAcquisitionTime, String latestAcquisitionTime,
                                                 String companyName, String orbitDirection, String beamMode,
                                                 String polarisationMode, String polarisationChannel,
                                                 String antennaLookDirection, String minimumIncidenceAngle,
                                                 String maximumIncidenceAngle, String incidenceAngleVariation,
                                                 String md5Hash, String licenseType, String isMultiPart,
                                                 String ceCategory, String niirs, String lastModifiedDate,
                                                 String hasCloudlessGeometry, String deletedDate,
                                                 String deletedReason, String productName,
                                                 String usageProductId, String ce90Accuracy, String bucketName,
                                                 String path, String sensorType) {


                            /**
                             * Record for Centroid class
                             * @param type String of type. Usually Point
                             * @param coordinates double[] of the coordinates for the centroid
                             */
                            public record Centroid(String type, double[] coordinates) {}
                        }
        }

        /**
         * CRS object containing the fully qualified projection name
         * @param type String of the parameter in propertied
         * @param properties Object containing the properties for the projection
         */
        public record Crs(String type, Object properties) {}

}