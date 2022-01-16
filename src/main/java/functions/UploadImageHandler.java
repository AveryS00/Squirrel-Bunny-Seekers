package functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.*;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import entity.SeekerImage;

public class UploadImageHandler implements HttpFunction {
    private static final String projectId = "still-tensor-338300";
    private static final String bucketName = "msu4xohkt-sjiuw-z4";
    private static final String storage_api_url = "https://storage.googleapis.com/";
    private static final Logger logger = LoggerFactory.getLogger(UploadImageHandler.class);
    private static final Gson gson = new Gson();
    private Storage storage;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException, InterruptedException{
        logger.info("Upload Request received");
        logger.info(request.toString());
        SeekerImage sImage;
        JsonObject body;
        String url;
        String data;
        byte[] img_data;
        int isBunny;
        int isSquirrel;
        String contentType = request.getContentType().orElse("");
        // Can likely be taken to a higher level. IE, once initialized, can be used everywhere.
        storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        // Cors
        response.appendHeader("Access-Control-Allow-Origin", "*");
        response.appendHeader("Access-Control-Allow-Methods", "POST");
        if ("OPTIONS".equals(request.getMethod())) {
            response.appendHeader("Access-Control-Allow-Headers", "Content-Type");
            response.appendHeader("Access-Control-Max-Age", "3600");
            response.setStatusCode(HttpURLConnection.HTTP_NO_CONTENT);
            return;
        }
        logger.info("Cors headers set?");


        // Get values out of json request
        if (contentType.equals("application/json")) {
            logger.info("Reading JSON");
            body = gson.fromJson(request.getReader(), JsonObject.class);

            sImage = createImageObject(body);
            if (sImage == null) {
                logger.error("Required json entry missing");
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }

            if (body.has("base64encoded")){
                data = body.get("base64encoded").getAsString();
                img_data = Base64.getDecoder().decode(data);

                try {
                    // Create a hash of the image and assign it to the
                    logger.info("Attempting to hash the image");
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digest = md.digest(img_data);
                    sImage.setHash(BaseEncoding.base16().lowerCase().encode(digest));
                } catch (NoSuchAlgorithmException e) {
                    logger.error("Hashing error");
                    response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                    return;
                }
                logger.info("Hash successful: " + sImage.getHash());
            }
            else {
                logger.error("No encoded value in JSON");
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }
        } else {
            logger.error("Not JSON");
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            return;
        }

        logger.info("JSON read");
        logger.info(sImage.toString());

        // TODO remove metadata from image
        logger.info("Metadata removed (not implemented)");


        // add to bucket
        url = uploadToBucket(sImage, img_data);
        sImage.setUrl(url);
        logger.info("Uploaded to bucket");
        logger.info("URL: " + sImage.getUrl());

        // Verify it contains a bunny or squirrel
        isBunny = verifyBunny(url);
        isSquirrel = verifySquirrel(url);
        logger.info("Contains bunny? " + isBunny);
        logger.info("Contains squirrel? " + isSquirrel);
    
        // Remove from bucket if not a bunny or squirrel
        if (isBunny == 0 && isSquirrel == 0) {
            logger.error("No bunny or squirrel seen, deleting from bucket");
            deleteFromBucket(sImage);
        } else {
            logger.info("Adding to database");
            if (isBunny > 0) {
                sImage.setHasBunny(true);
            }
            if (isSquirrel > 0) {
                sImage.setHasSquirrel(true);
            }

            if (uploadToDatabase(sImage, isBunny + isSquirrel)) {
                logger.info("Added to database");
            } else {
                response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                logger.error("Unable to add to database");
            }
        }
    }

    private int verifyBunny(String url) throws IOException, InterruptedException {
        return verifyAnimal(url, "bunnyIdentifier");
    }

    private int verifySquirrel(String url) throws IOException, InterruptedException {
        return verifyAnimal(url, "squirrelIdentifier");
    }

    private int verifyAnimal(String url, String animalIdentifier) throws IOException, InterruptedException {
        logger.info("Trying to verify animal");
        int isAnimal;
        String postBody = "{ \"imgURI\":\"" + url + "\" }";
        logger.info(postBody);

        var request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create("https://us-east4-still-tensor-338300.cloudfunctions.net/" + animalIdentifier))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(postBody))
            .build();
        logger.info(request.toString());

        var client = HttpClient.newHttpClient();

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        logger.info(response.body());
        isAnimal = Integer.parseInt(response.body());

        return isAnimal;
    }


    /**
     * Uploads the byte array to the website bucket and gives it a GUID name. Returns the url where it is accessible.
     * @param data A raw byte array containing the data to be uploaded
     * @return The url where the data is stored or null if unable
     */
    private String uploadToBucket(SeekerImage seekerImage, byte[] data) {
        Blob blob;
        logger.info("Changing name of image");
        // Create a uuid
        String uuid = UUID.randomUUID().toString();
        // Replace the name of the image with the uuid, while keeping the extension
        String extension = seekerImage.getName().substring(seekerImage.getName().lastIndexOf('.'));
        seekerImage.setName(uuid + extension);
        logger.info("New name: " + seekerImage.getName());

        // TODO apply compression?

        // Add the image to the bucket named after the recently created name
        BlobId blobId = BlobId.of(bucketName, seekerImage.getName());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try {
            blob = storage.create(blobInfo, data);
        } catch (StorageException se) {
            logger.error("Exception adding to bucket");
            logger.error(se.getMessage());
            se.printStackTrace();
            return null;
        }
        Map<String, String> newMetadata = new HashMap<>();
        newMetadata.put("Content-Type", "image/" + extension.substring(1));
        blob.toBuilder().setMetadata(newMetadata).build().update();


        logger.info("successfully added to bucket");
        return storage_api_url + bucketName + "/" + seekerImage.getName();
    }


    /**
     * Deletes the image object from the bucket
     * @param seekerImage The object containing the name of the image
     * @return True if deleted
     */
    private boolean deleteFromBucket(SeekerImage seekerImage) {
        try {
            storage.delete(bucketName, seekerImage.getName());
        } catch (StorageException se) {
            logger.error("Unable to delete from bucket");
            logger.error(se.getMessage());
            se.printStackTrace();
            return false;
        }
        logger.info("Deleted from bucket");
        return true;
    }


    /**
     * Uploads the Image object to the database
     * @param seekerImage The image object
     * @return True if uploaded
     */
    private boolean uploadToDatabase(SeekerImage seekerImage, int points) throws IOException, InterruptedException {
        String postBody = "{\"image_data\": {\"email\":\"" + seekerImage.getCreator() + "\", \"name\":\""
        + seekerImage.getName() + "\",\"hash\": \""+ seekerImage.getHash() +"\",\"url\": \""
                + seekerImage.getUrl() + "\",\"timestamp\":" + seekerImage.getTimestamp() + ",\"lat\": "
                + seekerImage.getLat() +",\"lon\":" + seekerImage.getLon() + ",\"hasBunny\": "
                + seekerImage.isHasBunny() + ",\"hasSquirrel\": "
                + seekerImage.isHasSquirrel() + "},\"points\": " + points + "}";
        logger.info(postBody);

        var request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://us-east4-still-tensor-338300.cloudfunctions.net/updateDatabase"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(postBody))
                .build();
        logger.info(request.toString());

        var client = HttpClient.newHttpClient();

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        logger.info(response.body());
        return Boolean.parseBoolean(response.body());
    }


    /**
     * Constructs an image object based on the json request. Returns null if required values are missing
     * @param json The json body of the HTTP request
     * @return An Image object based on the data
     */
    private SeekerImage createImageObject(JsonObject json) {
        SeekerImage seekerImage = new SeekerImage();

        if (json.has("creator"))
            seekerImage.setCreator(json.get("creator").getAsString());
        else {
            logger.error("No creator string");
            return null;
        }


        if (json.has("name"))
            seekerImage.setName(json.get("name").getAsString());
        else {
            logger.error("No name string");
            return null;
        }


        if (json.has("timestamp"))
            seekerImage.setTimestamp(json.get("timestamp").getAsInt());

        if (json.has("lat"))
            seekerImage.setLat(json.get("lat").getAsDouble());

        if (json.has("lon"))
            seekerImage.setLon(json.get("lon").getAsDouble());

        logger.info("");
        return seekerImage;
    }
}