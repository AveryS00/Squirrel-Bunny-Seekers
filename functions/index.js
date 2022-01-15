exports.animalImageIdentifier = (req, res) => {
    // Imports the Google Cloud client library
    /*
    const vision = require('@google-cloud/vision');
  
    // Creates a client
    const client = new vision.ImageAnnotatorClient();

    const imgURI = `https://storage.googleapis.com/msu4xohkt-sjiuw-z4/squirrel.jpg`;
  
    // Performs label detection on the image file
    const [result] = await client.objectLocalization(imgURI);
    const objects = result.localizedObjectAnnotations;
    console.log('Objects:');
    objects.forEach(object => console.log(`Name: ${object.name} Confidence: ${object.score}`));

    const isBunny = objects.filter( object => object.name === 'Rabbit'); 

    const isSquirrel = objects.filter( object => object.name === 'Squirrel');

    if (isBunny.length) {
        console.log(`It's a bunny!`);
        res.send(`It's a bunny!`);
    }

    if (isSquirrel.length) {
        console.log(`It's a squirrel!`);
        res.send(`It's a squirrel!`);
    }
    */

    res.send(req.query.imgURI || req.body.imgURI || 'Hello World');

  }