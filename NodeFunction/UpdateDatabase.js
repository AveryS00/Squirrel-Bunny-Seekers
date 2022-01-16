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

    uploadToDatabase(image_data, (err) => {
        if (err) {
            res.status(500).send(err);
        } else {
            updateUserPoints(points, image_data['email'], (err) => {
                if (err) {
                    res.status(500).send(err);
                } else {
                    res.status(200).send('Uploaded to database');
                }
            });
        }
    });
};

function uploadToDatabase(image_data, callback) {
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
                        callback(error);
                    } else {
                        console.log(`Image in database: ${data}`);
                        callback();
                    }
                });
        } else {
            console.log(`Error encountered: ${error}`);
            callback(error);
        }
    });
}

function updateUserPoints(points, email) {
    pool.query(`UPDATE user SET points = points + ${points} WHERE email = ${email}`,

        (error, data) => {
            if (error) {
                console.log(`Error encountered updating user points: ${error}`);
            } else {
                console.log(`Updated user points: ${data}`);
            }
        });
}