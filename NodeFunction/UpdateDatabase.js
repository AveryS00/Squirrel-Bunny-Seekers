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
exports.updateDatabase = (req, res) => {
    const image_data = req.body.image_data;
    const points = req.body.points;
    console.log('Image data');
    console.log(image_data);
    console.log('Points');
    console.log(points);

    uploadToDatabase(image_data, (err) => {
        if (err) {
            console.log('Error with first database attempt');
            console.log(err);
            res.status(500).send(false);
        } else {
            updateUserPoints(points, image_data['email'], (err) => {
                if (err) {
                    console.log('Error with second database attempt');
                    console.log(err);
                    res.status(500).send(false);
                } else {
                    console.log('Success');
                    res.status(200).send(true);
                }
            });
        }
    });
};

function uploadToDatabase(image_data, callback) {
    console.log('Querying image database')
    pool.query('SELECT * FROM image WHERE hash = "' + image_data['hash'] + '"', (error, data) => {
        console.log('First query complete');
        if (!error && data && data.length === 0) {
            pool.query("INSERT INTO image (email, name, hash, url, timestamp, lat, lon, hasBunny, hasSquirrel) " +
                "VALUES ('" + image_data['email'] + "', '" + image_data['name'] + "', '" + image_data['hash'] +
                "', '" + image_data['url'] + "', " + image_data['timestamp'] +
                ", " + image_data['lat'] + ", " + image_data['lon'] + ", " + image_data['hasBunny'] +
                ", " + image_data['hasSquirrel'] + ")",

                (error, data) => {
                    console.log('second query complete');
                    if (error) {
                        console.log(`failed to put image information into database: ${error}`);
                        callback(error);
                    } else {
                        console.log('Image in database');
                        console.log(data);
                        callback();
                    }
                });
        } else {
            console.log(`Error encountered: ${error}`);
            callback(error);
        }
    });
}

function updateUserPoints(points, email, callback) {
    pool.query(`UPDATE user SET points = points + ${points} WHERE email = '${email}'`,

        (error, data) => {
            if (error) {
                console.log(`Error encountered updating user points: ${error}`);
                callback(err);
            } else {
                console.log(`Updated user points: ${data}`);
                callback();
            }
        });
}