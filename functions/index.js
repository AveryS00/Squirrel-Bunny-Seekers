/**
 * Responds to any HTTP request.
 *
 * @param {!express:Request} req HTTP request context.
 * @param {!express:Response} res HTTP response context.
 */
 exports.bunnyIdentifier = async (req, res) => {
    // Imports the Google Cloud client library
     const vision = require('@google-cloud/vision');
   
     // Creates a client
     const client = new vision.ImageAnnotatorClient();
 
     const imgURI = req.body.imgURI;
   
     // Performs label detection on the image file
     const [result] = await client.objectLocalization(imgURI);
     const objects = result.localizedObjectAnnotations;
     console.log('Objects:');
     objects.forEach(object => console.log(`Name: ${object.name} Confidence: ${object.score}`));
 
     const isBunny = objects.filter( object => object.name === 'Rabbit'); 
 
     res.status(200).send(isBunny.length > 0);
 };
 