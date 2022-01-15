package functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.*;

import java.io.IOException;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;

import entity.SeekerImage;

public class UploadImageHandler implements HttpFunction {
    // TODO might want to move these to a "higher" place
    private static final String projectId = "still-tensor-338300";
    private static final String bucketName = "msu4xohkt-sjiuw-z4 ";
    private static final String storage_api_url = "https://storage.googleapis.com/";
    private Storage storage;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        // TODO implement logging

        // Can likely be taken to a higher level. IE, once initialized, can be used everywhere.
        storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        // TODO remove metadata from image

        // TODO construct Image object
        SeekerImage sImage = new SeekerImage();

        // TODO add to bucket
        byte[] data = {};
        String url = uploadToBucket(sImage, data);
        sImage.setUrl(url);

        // TODO Verify it contains a bunny or squirrel
        boolean isBunny = false;
        boolean isSquirrel = false;

        try {
            isBunny = verifyBunny(url);
            isSquirrel = verifyBunny(url);
        } catch(Exception e){
            System.out.println("Something went wrong :(");
            e.printStackTrace();
        }


        // TODO remove from bucket if not a bunny or squirrel
        if (false)
            deleteFromBucket(sImage);

        // TODO if bunny or squirrel, add to database
        if (uploadToDatabase())
            System.out.println("Success");
    }

    private boolean verifyBunny(String url) throws IOException, InterruptedException {
        boolean isBunny = false;

        String bunnyIdentifierEndpoint = "https://us-east4-still-tensor-338300.cloudfunctions.net/bunnyIdentifier";
        String postBody = "{ \"imgURI\":\"" + url + "\" }";

        var request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(bunnyIdentifierEndpoint))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(postBody))
            .build();

        var client = HttpClient.newHttpClient();
 
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        isBunny = Boolean.parseBoolean(response.body());

        return isBunny;
    }

    private boolean verifySquirrel(String url) throws IOException, InterruptedException {
        boolean isSquirrel = false;

        String squirrelIdentifierEndpoint = "https://us-east4-still-tensor-338300.cloudfunctions.net/squirrelIdentifier";
        String postBody = "{ \"imgURI\":\"" + url + "\" }";

        var request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(squirrelIdentifierEndpoint))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(postBody))
            .build();

        var client = HttpClient.newHttpClient();
 
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        isSquirrel = Boolean.parseBoolean(response.body());

        return isSquirrel;
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

    private boolean deleteFromBucket(SeekerImage seekerImage) {
        // TODO determine if success
        storage.delete(bucketName, seekerImage.getName());
        return true;
    }

    private boolean uploadToDatabase() {
        // TODO
        return false;
    }
}


//import http.UploadVideoSegmentRequest;
//import http.UploadVideoSegmentResponse;
//import java.io.ByteArrayInputStream;
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.LambdaLogger;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.CannedAccessControlList;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import database.VideoSegmentDAO;
//import entity.VideoSegment;
//
//public class UploadVideoSegmentHandler implements RequestHandler<UploadVideoSegmentRequest, UploadVideoSegmentResponse> {
//    public LambdaLogger logger;
//    private AmazonS3 s3 = null;
//    private final String URL = "https://cs3733-witch-of-endor.s3.us-east-2.amazonaws.com/VideoSegments/";
//
//    @Override
//    public UploadVideoSegmentResponse handleRequest(UploadVideoSegmentRequest req, Context context) {
//        logger = context.getLogger();
//        logger.log("Loading Java Lambda handler to upload video segment.");
//        String text = req.text.replaceAll("\\s+","");
//
//        UploadVideoSegmentResponse response = null;
//        logger.log(req.toString());
//
//        byte[] encoded = java.util.Base64.getDecoder().decode(req.base64encoded);
//        try {
//            VideoSegmentDAO dao = new VideoSegmentDAO();
//            if (addToBucket(encoded, text)) {
//                logger.log("attempting to add to database");
//                VideoSegment vs = new VideoSegment(URL + text + ".ogg", req.character, req.text, true);
//                if (dao.addVideoSegment(vs)) {
//                    logger.log("successfully added");
//                    dao.close();
//                    return new UploadVideoSegmentResponse(200, "Video segment uploaded: " + req.text);
//                }
//            }
//            dao.close();
//            response = new UploadVideoSegmentResponse(400, "Unable to upload video segment " + req.text);
//        } catch (Exception e) {
//            response = new UploadVideoSegmentResponse(500, "Unable to upload video segment " + req.text + " " + e.getMessage());
//            logger.log(e.getMessage());
//        }
//
//        return response;
//    }
//
//    public boolean addToBucket (byte[] contents, String name) throws Exception {
//        logger.log("trying to add to bucket");
//
//        if (s3 == null) {
//            logger.log("attach to S3 request");
//            s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
//            logger.log("attach to S3 succeed");
//        }
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(contents);
//        ObjectMetadata omd = new ObjectMetadata();
//        omd.setContentLength(contents.length);
//        s3.putObject(new PutObjectRequest("cs3733-witch-of-endor", "VideoSegments/" + name + ".ogg", bais, omd).withCannedAcl(CannedAccessControlList.PublicRead));
//
//        logger.log(" added to bucket ");
//        return true;
//    }
//}