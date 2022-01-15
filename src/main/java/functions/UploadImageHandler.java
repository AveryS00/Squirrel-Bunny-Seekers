package functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.*;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.security.MessageDigest;

import database.ImageDAO;
import entity.SeekerImage;

public class UploadImageHandler implements HttpFunction {
    // TODO might want to move these to a "higher" place
    private static final String projectId = "still-tensor-338300";
    private static final String bucketName = "msu4xohkt-sjiuw-z4 ";
    private static final String storage_api_url = "https://storage.googleapis.com/";
    private static final Gson gson = new Gson();
    private Storage storage;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        // TODO implement logging
        SeekerImage sImage;
        JsonObject body;
        String url;
        String data;
        String contentType = request.getContentType().orElse("");
        // Can likely be taken to a higher level. IE, once initialized, can be used everywhere.
        storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        // Get values out of json request
        if (contentType.equals("application/json")) {
            body = gson.fromJson(request.getReader(), JsonObject.class);

            sImage = createImageObject(body);
            if (sImage == null) {
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }

            // TODO fix this data format
            if (body.has("base64encoded")){
                data = body.get("base64encoded").getAsString();

                try {
                    // Create a hash of the image and assign it to the
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digest = md.digest(data.getBytes());
                    sImage.setHash(BaseEncoding.base16().lowerCase().encode(digest));
                } catch (NoSuchAlgorithmException e) {
                    response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                    return;
                }
            }
            else {
                response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }
        } else {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            return;
        }

        // TODO remove metadata from image


        // add to bucket
        url = uploadToBucket(sImage, data.getBytes());
        sImage.setUrl(url);

        // TODO Verify it contains a bunny or squirrel

        // TODO remove from bucket if not a bunny or squirrel
        if (false)
            if (deleteFromBucket(sImage))
                System.out.println("Deleted image");

        // TODO if bunny or squirrel, add to database
        if (uploadToDatabase(sImage))
            System.out.println("Success");
    }

    private boolean verifyImageContents() {
        // TODO Char add algorithm
        return false;
    }


    /**
     * Uploads the byte array to the website bucket and gives it a GUID name. Returns the url where it is accessible.
     * @param data A raw byte array containing the data to be uploaded
     * @return The url where the data is stored or null if unable
     */
    private String uploadToBucket(SeekerImage seekerImage, byte[] data) {
        // Create a uuid
        String uuid = UUID.randomUUID().toString();
        // Replace the name of the image with the uuid, while keeping the extension
        String extension = seekerImage.getName().substring(seekerImage.getName().lastIndexOf('.'));
        seekerImage.setName(uuid + extension);

        // TODO apply compression?

        // Add the image to the bucket named after the recently created name
        BlobId blobId = BlobId.of(bucketName, seekerImage.getName());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try {
            storage.create(blobInfo, data);
        } catch (StorageException se) {
            return null;
        }

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
            return false;
        }
        return true;
    }


    /**
     * Uploads the Image object to the database
     * @param seekerImage The image object
     * @return True if uploaded
     */
    private boolean uploadToDatabase(SeekerImage seekerImage) {
        ImageDAO dao;
        try {
            dao = new ImageDAO();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return dao.addImage(seekerImage);
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
        else
            return null;

        if (json.has("name"))
            seekerImage.setName(json.get("name").getAsString());
        else
            return null;

        if (json.has("timestamp"))
            seekerImage.setTimestamp(json.get("timestamp").getAsInt());

        if (json.has("lat"))
            seekerImage.setLat(json.get("lat").getAsDouble());

        if (json.has("lon"))
            seekerImage.setLon(json.get("lon").getAsDouble());

        return seekerImage;
    }
}