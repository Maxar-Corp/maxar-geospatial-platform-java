# Maxar Geospatial Platform SDK

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.maxar-corp/maxar-portal-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.maxar-corp/maxar-portal-java)
[![javadoc](https://javadoc.io/badge2/io.github.maxar-corp/maxar-portal-java/javadoc.svg)](https://javadoc.io/doc/io.github.maxar-corp/maxar-portal-java)

## Table of Contents
- [Overview](#overview)
- [Installation Instructions](#installation-instructions)
- [Authentication](#authentication)
- [Workflow](#workflow)
    - [Example](#example)
    - [Search](#search)
    - [Download Image](#download-image)
    - [Get Tile List](#get-tile-list)
    - [Download Tiles](#download-tiles)
    - [Get Full Resolution Image](#get-full-resolution-image)
- [Streaming, Basemap, Analytics](#streaming-basemap-analytics)
- [Builder Definitions](#username)

## Overview

---

The Java MPS package uses a builder pattern to build and send specific API calls. The SDK
supports the following Open Geospatial Consortium standard protocols: WFS, WMS, WMTS.

### View the Javadoc [Here](https://www.javadoc.io/doc/io.github.maxar-corp/maxar-portal-java/latest/com/maxar/MPSSDK/package-summary.html)


## Installation Instructions

---

### Maven (recommended)


```xml
<dependency>
  <groupId>io.github.maxar-corp</groupId>
  <artifactId>maxar-portal-java</artifactId>
  <version>{maxar-portal-java.version}</version>
</dependency>
```

### Gradle
```groovy
implementation group: 'io.github.maxar-corp', name: 'maxar-portal-java', version: '2.0.0'
```

### Ivy
```html
<dependency org="io.github.maxar-corp" name="maxar-portal-java" rev="2.0.0"/>
```

### Grape
```
@Grapes(
      @Grab(group='io.github.maxar-corp', module='maxar-portal-java', version='2.0.0')
  )
```

### How To Import

```
import io.github.maxar.corp.MaxarPortal;
```

## Authentication

---

Access to Maxar API resources requires valid credentials. The package looks for a credentials file 
located in the users home directory. Users should have a file here titled .MGP-config and should 
resemble the following exactly:
```
[mgp]
user_name=<myuser@maxar.com>
user_pasword=<mySuperSecretPassword>
client_id=<my-client-id>
```
Replace these values with the credentials provided to you by Maxar. <br />

Optionally, credentials can also be supplied by passing them in as builder arguments when the call is 
instantiated using .username() .password() and .clientID()

## Workflow

---
### Example
The following is an example workflow to make a simple wfs call with a bounding box and a few filters 
using the .searchToString() function

```java
import io.github.maxar.MGPSDK.Streaming;

public class Main {

  public static void main(String[] args) {
    Streaming wfsCall = Streaming.builder()
            .bbox("39.84387,-105.05608,39.95133,-104.94827")
            .bbox("EPSG:4326")
            .filter("acquisitionDate>='2022-01-01'")
            .filter("cloudCover<0.20")
            .build();

    System.out.println(wfsCall.searchToString());
  }

}
```

The search function performs a WFS request that returns a string containing a GeoJSON formatted list
of features that match the parameters sent in the call. 

Example response received from the server for the above call
```
{
   "type":"FeatureCollection",
   "features":[
      {
         "type":"Feature",
         "id":"7dea6ffc-e4b3-a507-f7e7-af315d32da29",
         "geometry":{
            "type":"Polygon",
            "coordinates":[...]           ]
         },
         "geometry_name":"featureGeometry",
         "properties":{
            "featureId":"7dea6ffc-e4b3-a507-f7e7-af315d32da29",
            "cloudCover":0,
            "sunAzimuth":159.64929,
            "sunElevation":24.48628,
            "offNadirAngle":26.880003,
            "groundSampleDistance":0.38,
            etc...
         }
      },
      {...}
   ],
   "totalFeatures":"unknown",
   "numberReturned":4,
   "timeStamp":"2023-01-18T16:51:58.818Z",
   "crs":{
      "type":"name",
      "properties":{
         "name":"urn:ogc:def:crs:EPSG::4326"
      }
   }
}
```

## Making a call
Because the sdk utilizes a builder pattern, the parameters you want to add to the call can be 
chained on the child class (Streaming, Basemap, Analytics) instantiation. Once the Portal is built, 
it contains methods that correspond to the different available functionality for making and 
returning OGC calls.

## Records for WFS Results

A FeatureCollection is built and provided for each child to view and efficiently parse results from
WFS results returned from the .search() method. StreamingFeatureCollection,
BasemapFeatureCollection, AnalyticsFeatureCollection

Below are a few examples of accessing metadata from returned calls:

```java
public class Main {

  public static void main(String[] args) {
    Streaming wfsCall = Streaming.builder()
            .bbox("39.84387,-105.05608,39.95133,-104.94827")
            .filter("acquisitionDate>='2022-01-01'")
            .filter("cloudCover<0.20")
            .build();

    StreamingFeatureCollection results = wfsCall.search();

    //Get number of images returned
    int numberReturned = results.numberReturned();

    //return the feature ID of every feature returned
    for (StreamingFeatures feature : results.features()) {
      System.out.println(feature.getId());
    }

    //print the coordinates of the centroid of the first returned feature
    Centroid firstCentroid = results.features()[0].properties.centroid();
    System.out.println(Arrays.toString(firstCentroid.coordinates()));
  }

}
```

Furthermore, the geometry returned within the FeatureCollection is an instance of the
JTS Geometry class providing all the methods returned from that class.
[Locationtech JTS Geometry](https://locationtech.github.io/jts/javadoc/org/locationtech/jts/geom/Geometry.html)

```java
import io.github.maxar.MGPSDK.StreamingFeatureCollection;
import java.util.Arrays;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class Main {

  public static void main(String[] args) {
    Streaming wfsCall = Streaming.builder()
            .bbox("39.84387,-105.05608,39.95133,-104.94827")
            .filter("acquisitionDate>='2022-01-01'")
            .filter("cloudCover<0.20")
            .build();

    StreamingFeatureCollection results = wfsCall.search();

    //Collect the first feature's geometry
    Geometry firstFeatureGeometry = results.features()[0].geometry;

    //Get the geometry type
    System.out.println("Geometry Type: " + firstFeatureGeometry.getGeometryType());

    //Get the coordinates
    System.out.println("Geometry Coordinates: " +
            Arrays.toString(firstFeatureGeometry.getCoordinates()));

    //Create an envelope
    Envelope envelope = firstFeatureGeometry.getEnvelope();

    //Get the area
    Double area = firstFeatureGeometry.getArea();
  }

}
```

If for any reason you require the raw string return from a WFS call, utilize the
<code>Ogc.searchToString()</code> method.

```java
public class Main {

  public static void main(String[] args) {
    Streaming wfsCall = Streaming.builder()
            .bbox("39.84387,-105.05608,39.95133,-104.94827")
            .filter("acquisitionDate>='2022-01-01'")
            .filter("cloudCover<0.20")
            .build();

    String results = wfsCall.searchToString();
    System.out.println(results);
  }
}
```


## Search
**Ogc.search()** <br/>
Performs a WFS search.<br/>
Return WFS results as a FeatureCollection<br/>
Builder parameters: <br/>
[.bbox()](#bounding-box) <br/>
[.filter()](#filter) <br/>
[.rawFilter()](#raw-filter)<br>
[.srsname()](#srsname) <br/>
[.featureId()](#featureid) <br/>
[.requestType()](#request-type) <br/>
[.typeName()](#type-name) <br/>


Example Call

```java
import io.github.maxar.MGPSDK.Streaming;
import io.github.maxar.MGPSDK.StreamingFeatureCollection;

public class Main {

  public static void main(String[] args) {

    //Build the call
    Streaming wfsCall = Streaming.builder()
            .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
            .filter("(acquisitionDate>='2022-01-01')AND(cloudCover<0.20)")
            .srsname("ESPG:3857")
            .build();

    //Make the call
    StreamingFeatureCollection wfsResults = wfsCall.search();

  }

}
```

## Download CSV
**Ogc.downloadCsv()** <br>
Performs a WFS request in the same manner as <code>.search()</code>, downloading a CSV of the 
results. CSV will be downloaded to the defined path, or defaulted to the user's downloads directory.
Filename can be set with <code>.filename()</code>
[.bbox()](#bounding-box) <br/>
[.filter()](#filter) <br/>
[.rawFilter()](#raw-filter)<br>
[.srsname()](#srsname) <br/>
[.downloadPath()](#download-path) <br/>
[.fileName()](#file-name) <br/>
[.typeName()](#type-name) <br/>

Example call:

```java
import io.github.maxar.MGPSDK.Streaming;

public class Main {

  public static void main(String[] args) {

    //Build the call
    Streaming csvCall = Streaming.builder()
            .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
            .rawFilter("(acquisitionDate>='2022-01-01')AND(cloudCover<0.20)")
            .srsname("EPSG:3857")
            .fileName("test")
            .downloadPath("C:/Users/user/Desktop/Images")
            .build();

    //Make the call
    csvCall.downloadCsv();

  }

}
```

## Download Shapefile
**Ogc.downloadShapeFile()** <br>
Performs a WFS request in the same manner as <code>.search()</code>, downloading a Shapefile of the
results. Shapefile will be downloaded to the defined path, or defaulted to the user's downloads directory.
Filename can be set with <code>.filename()</code>
[.bbox()](#bounding-box) <br/>
[.filter()](#filter) <br/>
[.rawFilter()](#raw-filter)<br>
[.srsname()](#srsname) <br/>
[.downloadPath()](#download-path) <br/>
[.fileName()](#file-name) <br/>
[.typeName()](#type-name) <br/>

Example call:

```java
import io.github.maxar.MGPSDK.Streaming;

public class Main {

  public static void main(String[] args) {

    //Build the call
    Streaming shapefile = Streaming.builder()
            .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
            .rawFilter("(acquisitionDate>='2022-01-01')AND(cloudCover<0.20)")
            .srsname("EPSG:3857")
            .fileName("test")
            .downloadPath("C:/Users/user/Desktop/Images")
            .build();

    //Make the call
    shapefile.downloadShapeFile();

  }

}
```

## Download Image
**Ogc.downloadImage()** <br/>
Get the requested image using WMS. If .download() is provided, image will be downloaded to the 
defined path, or defaulted to the user's downloads directory. If not, the function returns the blob 
from the API which can then be converted to an instance of a java.io.InputStream<br/>
Returns location the image was downloaded to <br/>
Builder parameters: <br/>
[.bbox()](#bounding-box) <br/>
[.filter()](#filter) <br/>
[.rawFilter()](#raw-filter)<br>
[.srsname()](#srsname) <br/>
[.height()](#height) <br/>
[.width()](#width) <br/>
[.imageFormat()](#image-format) <br/>
[.downloadPath()](#download-path) <br/>
[.fileName()](#file-name) <br/>
[.download()](#file-name) <br/>
[.legacyId()](#legacy-id) <br/>
[.typeName()](#type-name) <br/>

Example Call
```java
public class Main {

    public static void main(String[] args) {
        
        //Build the call
        Streaming wmsCall = Streaming.builder()
            .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
            .filter("(acquisitionDate>='2022-01-01')AND(cloudCover<0.20)")
            .srsname("ESPG:3857")
            .height(1000)
            .width(1000)
            .imageFormat("png")
            .fileName("test")
            .downloadPath("C:/Users/user/Desktop/Images")
            .build();

        //Make the call
        String downloadLocation = wmsCall.downloadImage();

        //View the response
        System.out.println(downloadLocation);
    }

}
```

## Get Tile List
**Ogc.getTileList()** <br/>
Returns a Hashmap<String, String> of WMTS calls that can be used to return all of the tiles in a 
given AOI. The key is a String list containing a tiles row, column, and zoom level. The value is the 
associated api call. If you want to download all tiles and do not care about the individual calls, 
use the .downloadTiles() method instead <br/>
Builder Parameters: <br/>
[.bbox()](#bounding-box) <br/>
[.srsname()](#srsname) <br/>
[.zoomLevel()](#zoom-level) <br/>

Example Call
```java
public class Main {

    public static void main(String[] args) {
        
        //Build the call
        Streaming wmtsCall = Streaming.builder()
                .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
                .srsname("EPSG:4326")
                .zoomLevel(12)
                .build();

        //Make the call
        Hashmap<String, String> wmtsApiCallList = wmtsCall.getTileList();

        //View the response
        wmtsApiCallList.entrySet().forEach(entry -> 
            System.out.println(entry.getKey() + " " + entry.getValue())
        );
    }
}
```

## Download Tiles
**streaming.downloadTiles()** <br/>
Downloads all the tiles in a given bounding box at a given zoom level. Returns a message indicating 
success or failures and the location of the downloaded tiles. A base file name can be added with the 
.fileName() method. This file name will be appended with a tile's row column and zoom level <br />
Builder Parameters: <br/>
[.bbox()](#bounding-box) <br/>
[.srsname()](#srsname) <br/>
[.zoomLevel()](#zoom-level) <br/>
[.imageFormat()](#image-format) <br/>
[.fileName()](#file-name) <br/>
[.downloadPath()](#download-path) <br/>
[.typeName()](#type-name) <br/>

Example Call
```java
public class Main {

    public static void main(String[] args) {
        
        //Build the call
        Streaming wmtsCall = Streaming.builder()
            .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
            .zoomLevel(12)
            .imageFormat("geotiff")
            .downloadPath("C:\\Users\\user\\Desktop\\Seattle")
            .fileName("seattle_tile")
            .build();

        //Make the call
        String results = wmtsCall.downloadTiles();

        //View the response
        System.out.println(results);
    }
}
```

## Get Full Resolution Image
**Streaming.getFullResImage()** <br/>
This method downloads the full scale resolution of a desired AOI. Utilizes multithreading to try to 
speed up the download process. Full resolution images are broken into tiles and downloaded as separate 
full resolution images. The number of threads must be set by the user. 50 is a good starting point. 
The more threads in use the quicker the download process however the more unstable. If you are getting
a number of failed returns, terminate the process, lower the number of threads and try again. 
Only available on the Streaming class<br/>
Builder Parameters: <br/>
[.featureId()](#featureid)<br/>
[.threadNumber()](#thread-number)<br/>
[.bbox()](#bounding-box) <br/>
[.srsname()](#srsname) <br/>
[.imageFormat()](#image-format) <br/>
[.downloadPath()](#download-path) <br/>
[.fileName()](#file-name) <br/>

Example Call
```java
public class Main {

    public static void main(String[] args) {
        
        //Build the call
        Streaming fullResDownload = Streaming.builder()
            .featureId("7dea6ffce4b3a507f7e7af315d32da29")
            .imageFormat("geotiff")
            .filename("test")
            .downloadPath("C:/Users/user/Desktop/FullRes")
            .build();

        //Make the call
        fullResDownload.getFullResImage();

    }

}
```

## Streaming, Basemap, Analytics

Streaming, Basemap, and Analytics all inherit from the Ogc class and therefore share all properties 
and methods. If an Ogc method is not specifically supported by the child, an 
UnsupportedOperationException will be thrown. All calls listed in the workflow theoretically
 can be used by all children. For example:<br>

Analytics WFS search:<br>

```java
import io.github.maxar.MGPSDK.Analytics;
import io.github.maxar.MGPSDK.AnalyticsFeatureCollection;

public class Main {

  public static void main(String[] args) {
    Analytics wfsCall = Analytics.builder()
            .bbox("40.407222,-74.363708,41.271614,-73.449097")
            .bbox("EPSG:4326")
            .filter("sensor='Landsat'")
            .typeName("Maxar:layer_pcm_eo_2020")
            .build();

    AnalyticsFeatureCollection pcmFeatures = wfsCall.search();
  }

}
```

Basemap WMS download Image:
```java
public class Main {

    public static void main(String[] args) {
        
        //Build the call
        Basemap wmsCall = Basemap.builder()
            .bbox("4828455.4171,-11686562.3554,4830614.7631,-11684030.3789")
            .filter("product_name='VIVID_STANDARD_30'")
            .srsname("ESPG:3857")
            .height(1000)
            .width(1000)
            .imageFormat("png")
            .filename("test")
            .downloadPath("C:/Users/user/Desktop/Images")
            .build();

        //Make the call
        String downloadLocation = wmsCall.downloadImage();

        //View the response
        System.out.println(downloadLocation);
    }

}
```


### Username
`.username(String)` <br/>
Used if not using a .MPS-config file.

### Password
`.password(String)` <br/>
Used if not using a .MPS-config file.

### ClientId
`.clientId(String)` <br/>
Used if not using a .MPS-config file.

### Bounding box 
`.bbox(String)` <br/>
Accepts a string containing the bbox in yx order. All calls default to an EPSG:4326 projection. To 
use a different one utilize the .srsname() method. Example EPSG:4326 call "39.84387,-105.05608,39.95133,-104.94827"

### Filter
`.filter(String)` <br/>
Accepts a String containing a cql filter. filters can be chained together by repeatedly 
calling .filter() FIlters will be combined and separated by an AND. If more control is 
needed over the filter, user .rawFilter()

### Raw Filter
`.rawFilter(String)` <br>
Accepts a complete filter to be passed to the API. When combining individual filters, 
filters must be surrounded by parenthesis and separated by an AND or OR

### SRSName
`.srsname(String)` <br/>
Accepts a string containing the desired projection. Must be set if utilizing a bbox

### FeatureID
`.featureId(String)` <br/>
Accepts a string containing the specific feature ID you are looking for. The featureId overrides the
.filter method. It will not throw an error if a filter is passed, however the filter will be ignored. 

### Request Type
`.requestType(String)` <br/>
Accepts a string containing the type of request you want to make. Defaults to "GetFeature" in WFS 
calls, GetMap in WMS calls and GetTile in WMTS calls

### Height
`.height(int)` <br/>
Accepts an integer representing the desired height of the image in pixels. Max 8000. NOTE: 8000px 
calls may not work due to many factors including server load, user machine capabilities, and latency. 
For large calls it is recommended the user break the calls up. 

### Width
`.width(int)` <br/>
Accepts an integer representing the desired width of the image in pixels. Max 8000. NOTE: 8000px 
calls may not work due to many factors including server load, user machine capabilities, and latency. 
For large calls it is recommended the user break the calls up. 

### Image Format
`.imageFormat(String)` <br/>
Accepts a string of the desired format. Supported formats jpeg, geotiff, png.

### Download Path
`.downloadPath(String)` <br/>
Accepts a string of the full path of the location to download responses to. Defaults to the users 
Downloads directory. If a folder that does not exist is declared, then the folder will be created. 

### Zoom Level
`.zoomLevel(int)` <br/>
Accepts an int representation of the desired zoom level. Supported zoom levels: <br/>
- EPSG:4326
  - 0 - 21
- EPSG:3857
  - 0 - 31

### File Name
`.fileName(String)` <br>
Accepts a string to represent the desired filename. Defaults to Download.Extension

### Legacy ID
`.legacyId(String)` <br/>
Accepts a string representing the legacy identifier you are searching for. Downloads the full image 
therefore height and width are ignore if they are passed in. Image searches using legacy ID point to
api.discover.digitalglobe.com

### Band Combination
`.bandCombination(ArrayList<String>)` <br/>
Accepts an ArrayList of Strings containing between 1 - 4 band options. 

### Thread Number
`.threadNumber(int)`<br/>
Accepts an integer value representing the number of threads to be used for full res download 
multithreading.

### Type Name
`.typeName(String)`