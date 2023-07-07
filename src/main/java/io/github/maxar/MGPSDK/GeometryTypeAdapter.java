package io.github.maxar.MGPSDK;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

//This class is needed in order to show the GSON parser how to write to the JTS Geometry library
class GeometryTypeAdapter extends TypeAdapter<Geometry> {

    @Override
    public void write(JsonWriter out, Geometry geometry) throws IOException {
        out.beginObject();
        out.name("type").value(geometry.getGeometryType());
        out.name("coordinates");
        writeCoordinates(out, geometry.getCoordinates());
        out.endObject();
    }

    private void writeCoordinates(JsonWriter out, Coordinate[] coordinates) throws IOException {
        if (coordinates.length == 1) {
            writeCoordinate(out, coordinates[0]);
        } else {
            out.beginArray();
            for (Coordinate coordinate : coordinates) {
                writeCoordinate(out, coordinate);
            }
            out.endArray();
        }
    }

    private void writeCoordinate(JsonWriter out, Coordinate coordinate) throws IOException {
        out.beginArray();
        out.value(coordinate.x);
        out.value(coordinate.y);
        if (!Double.isNaN(coordinate.getZ())) {
            out.value(coordinate.getZ());
        }
        out.endArray();
    }

    @Override
    public Geometry read(JsonReader in) {
        GeometryFactory geometryFactory = new GeometryFactory();
        JsonObject jsonObject = JsonParser.parseReader(in).getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        switch (type) {
            case "Point" -> {
                JsonArray pointCoordinates = jsonObject.getAsJsonArray("coordinates");
                double x = pointCoordinates.get(0).getAsDouble();
                double y = pointCoordinates.get(1).getAsDouble();
                return geometryFactory.createPoint(new Coordinate(x, y));
            }
            case "Polygon" -> {
                JsonArray polygonCoordinates = jsonObject.getAsJsonArray("coordinates");
                JsonArray exteriorRingCoordinates = polygonCoordinates.get(0).getAsJsonArray();
                Coordinate[] coordinates = new Coordinate[exteriorRingCoordinates.size()];
                for (int i = 0; i < exteriorRingCoordinates.size(); i++) {
                    JsonArray coordinateArray = exteriorRingCoordinates.get(i).getAsJsonArray();
                    double lon = coordinateArray.get(0).getAsDouble();
                    double lat = coordinateArray.get(1).getAsDouble();
                    coordinates[i] = new Coordinate(lon, lat);
                }
                LinearRing shell = geometryFactory.createLinearRing(coordinates);
                return geometryFactory.createPolygon(shell);
            }
            case "MultiPolygon" -> {
                JsonArray multiPolygonCoordinates = jsonObject.getAsJsonArray("coordinates");
                Polygon[] polygons = new Polygon[multiPolygonCoordinates.size()];
                for (int i = 0; i < multiPolygonCoordinates.size(); i++) {
                    JsonArray polygonCoordinates = multiPolygonCoordinates.get(i).getAsJsonArray();
                    JsonArray exteriorRingCoordinates = polygonCoordinates.get(0).getAsJsonArray();
                    Coordinate[] coordinates = new Coordinate[exteriorRingCoordinates.size()];
                    for (int j = 0; j < exteriorRingCoordinates.size(); j++) {
                        JsonArray coordinateArray = exteriorRingCoordinates.get(j).getAsJsonArray();
                        double lon = coordinateArray.get(0).getAsDouble();
                        double lat = coordinateArray.get(1).getAsDouble();
                        coordinates[j] = new Coordinate(lon, lat);
                    }
                    LinearRing shell = geometryFactory.createLinearRing(coordinates);
                    polygons[i] = geometryFactory.createPolygon(shell);
                }
                return geometryFactory.createMultiPolygon(polygons);
            }

            default -> throw new JsonParseException("Unknown geometry type: " + type);
        }
    }
}
