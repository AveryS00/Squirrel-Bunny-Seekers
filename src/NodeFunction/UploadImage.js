const {randomUUID, createHash} = require('crypto');
const fs = require('fs');


// Imports the Google Cloud client library
const gcs = require('@google-cloud/storage');
const mysql = require('mysql');

const pool = mysql.createPool({
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    socketPath: `/cloudsql/${process.env.INSTANCE_CONNECTION_NAME}`,
});

/**
 * Responds to any HTTP request.
 *
 * @param {!express:Request} req HTTP request context.
 * @param {!express:Response} res HTTP response context.
 */

exports.uploadImage = async (req, res) => {
    console.log(`Received request: ${req.toString()}`);

    const stream = require('stream');
    const bufferStream = new stream.PassThrough();
    bufferStream.end(Buffer.from(req.body.base64encoded, 'base64'));

    const image = Buffer.from(req.body.base64encoded, 'base64');
    const new_name = randomUUID() + req.body.name.split('.').pop()
    const image_data = {'email': req.body.creator,
        'name': new_name,
        'hash': createHash('md5').update(image).digest('hex'),
        'url': process.env.BASE_BUCKET_URL + new_name,
        'timestamp': req.body.timestamp,
        'lat': req.body.lat,
        'lon': req.body.lon,
        'hasBunny': false,
        'hasSquirrel': false
    };
    const storage = new gcs.Storage();
    const myBucket = storage.bucket('msu4xohkt-sjiuw-z4');
    const file = myBucket.file(image_data['name']);

    // fs.writeFile(new_name, req.body.base64encoded, {encoding: 'base64'},
    //     function(err) {
    //         console.log('File created');
    //     });

    console.log(`Image Data: ${image_data.toString()}`);

    bufferStream.pipe(file.createWriteStream({
        metadata: {
            contentType: 'image/jpeg',
            metadata: {
                custom: 'metadata'
            }
        },
        public: true,
        validation: "md5"
    })).on('error', (err) => {
        console.log('unable to upload image' + err);
    }).on('finish', () => {
        console.log('Image uploaded');

        const squirrel_points = verifySquirrel(image_data['url']);
        const bunny_points = verifyBunny(image_data['url']);

        if (squirrel_points !== 0 && bunny_points !== 0) {
            deleteFromBucket(storage, image_data['name']);
        } else {
            if (squirrel_points >= 0) {
                image_data['hasSquirrel'] = true;
            }
            if (bunny_points >= 0) {
                image_data['hasBunny'] = true;
            }
            uploadToDatabase(image_data);
            updateUserPoints(squirrel_points + bunny_points, image_data['email']);
        }
    });

    // uploadToBucket(storage, image_data)
    //     .then(res.status(200).send("Image uploaded"))
    //     .catch(err => res.status(400).send(err));
}

// async function uploadToBucket(storage, image_data) {
//     console.log('Trying to upload to bucket');
//     await storage.bucket('msu4xohkt-sjiuw-z4').upload(image_data['new_name'], {
//         destination: image_data['new_name'],
//     });
//
//     //const myBucket = storage.bucket('msu4xohkt-sjiuw-z4');
//     //const file = myBucket.file(image_data['name']);
//     //fs.createReadStream(image_data['name']).pipe(file.createWriteStream());
//
//     console.log(`Uploaded to ${process.env.BUCKET_NAME}`);
//
//
// }

async function verifySquirrel(uri) {
    console.log('Checking for squirrel');
    return await verifyAnimal(uri, 'squirrelIdentifier');
}

async function verifyBunny(uri) {
    console.log('Checking for bunny');
    return await verifyAnimal(uri,'bunnyIdentifier');
}

function verifyAnimal(uri, api_endpoint) {
    return new Promise(function(resolve, reject) {
        const xhr = new XMLHttpRequest();
        xhr.open('POST',
            'https://us-east4-still-tensor-338300.cloudfunctions.net' + api_endpoint, true);
        xhr.onloadend = function() {
            if (xhr.status === 200) {
                console.log('Response from verification');
                console.log(xhr.response.toString());
                console.log(xhr.response.body.toString());
                resolve(xhr.response.body);
            } else {
                console.log(`Error in verification: ${xhr.status}`);
                reject(xhr.status);
            }
        };
        xhr.send(JSON.stringify({'imgURI':uri}));
    });
}

function uploadToDatabase(image_data) {
    console.log('Querying image database')
    pool.query('SELECT * FROM user WHERE hash = "' + image_data['hash'] + '"', (error, data) => {
        if (!error && data && data.length === 0) {
            pool.query("INSERT INTO image (email, name, hash, url, timestamp, lat, lon, hasBunny, hasSquirrel) " +
                "VALUES ('" + image_data['email'] + "', '" + image_data['new_name'] + "', '" + image_data['hash'] +
                "', '" + image_data['url'] + "', " + image_data['timestamp'] +
                ", " + image_data['lat'] + ", " + image_data['lon'] + ", " + image_data['hasBunny'] +
                ", " + image_data['hasSquirrel'] + ")",

                (error, data) => {
                if (error) {
                    console.log(`failed to put image information into database: ${error}`);
                } else {
                    console.log(`Image in database: ${data}`);
                }
            });
        } else {
            console.log(`Error encountered: ${error}`);
        }
    });
}

function updateUserPoints(points, email) {
    pool.query('UPDATE user SET points = points + {points} WHERE email = {email}',

        (error, data) => {
            if (error) {
                console.log(`Error encountered updating user points: ${error}`);
            } else {
                console.log(`Updated user points: ${data}`);
            }
    });
}

function deleteFromBucket(storage, image_name) {
    storage.bucket(process.env.BUCKET_NAME).file(image_name).delete().catch(err => {
        console.log(`Error in deleting image from bucket: ${err}`);
    });
}