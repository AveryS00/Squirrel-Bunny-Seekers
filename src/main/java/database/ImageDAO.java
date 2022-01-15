package database;

import entity.SeekerImage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ImageDAO {
    Connection conn;
    PreparedStatement stmt;
    ResultSet set;


    /**
     * Constructs a DAO object for VideoSegments
     * @throws Exception Either ClassNotFound or SQL exception
     */
    public ImageDAO() throws Exception {
        this.conn = DatabaseConnection.connect();
    }


    /**
     * Gets a specific image by the hash
     * @param hash The MD5 hash of the image
     * @return A SeekerImage object
     */
    public SeekerImage getImageObject(String hash) {
        try {
            stmt = conn.prepareStatement("SELECT * FROM image WHERE hash = ?");
            stmt.setString(1, hash);
            set = stmt.executeQuery();
            if (set.next()) {
                return generateImage();
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.closeStmt(stmt, set);
        }
    }


    /**
     * Gets all images by a user
     * @param email The email designating the user
     * @return An array of SeekerImageObjects
     */
    public ArrayList<SeekerImage> getUserImages(String email) {
        return null;
    }


    /**
     * Gets all images and returns them in order with most recent first
     * @return A list of images with most recent first
     */
    public ArrayList<SeekerImage> getMostRecentImages() {
        return getMostRecentImages(Integer.MAX_VALUE);
    }


    /**
     * Gets x most recently taken images and returns them in order with most recent first
     * @param number The maximum number of images to get
     * @return A list of images with most recent first
     */
    public ArrayList<SeekerImage> getMostRecentImages(int number) {
        return null;
    }


    /**
     *Gets all images of a specific animal
     * @param type 0 for bunny, 1 for squirrel, none for any other value
     * @return A list of images of the specified animal sorted by most recent first
     */
    public ArrayList<SeekerImage> getAnimal(int type) {
        return getAnimal(type, Integer.MAX_VALUE);
    }


    /**
     * Gets all images of a specific animal
     * @param type 0 for bunny, 1 for squirrel, none for any other value
     * @param max The maximum number of images to get
     * @return A list of images of the specified animal sorted by most recent first
     */
    public ArrayList<SeekerImage> getAnimal(int type, int max) {
        String column;

        if (type == 1) {
            column = "hasBunny";
        } else if (type == 2) {
            column = "hasSquirrel";
        } else {
            return null;
        }

        return null;
    }


    /**
     * Adds the SeekerImage object to the relational database
     * @param seekerImage The object to add to the database
     * @return True if added, false otherwise
     */
    public boolean addImage(SeekerImage seekerImage) {
        try {
            if (isInDatabase(seekerImage)) {
                return false;
            }
            stmt = conn.prepareStatement("INSERT INTO image " +
                    "(email, name, hash, url, timestamp, lat, lon, hasBunny, hasSquirrel) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, seekerImage.getCreator());
            stmt.setString(2, seekerImage.getName());
            stmt.setString(3, seekerImage.getHash());
            stmt.setString(4, seekerImage.getUrl());
            stmt.setInt(5, seekerImage.getTimestamp());
            stmt.setDouble(6, seekerImage.getLat());
            stmt.setDouble(7, seekerImage.getLon());
            stmt.setBoolean(8, seekerImage.isHasBunny());
            stmt.setBoolean(9, seekerImage.isHasSquirrel());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.closeStmt(stmt, set);
        }
    }


    /**
     * Helper method to create a VideoSegment object based on the current tuple of the result set.
     * @return A VideoSegment object based on the current tuple of set.
     * @throws SQLException If one of the columns is not in the tuple
     */
    private SeekerImage generateImage() throws SQLException {
        return new SeekerImage(set.getString("email"), set.getString("name"),
                set.getString("url"), set.getInt("timestamp"),
                set.getDouble("lat"), set.getDouble("lon"),
                set.getString("hash"), set.getBoolean("hasBunny"),
                set.getBoolean("hasSquirrel"));
    }

    /**
     * Private helper method to check if an image object is currently in the database by checking the primary key.
     * @param seekerImage The image object to check for
     * @return True if the image hash is in the database.
     * @throws SQLException If sql errors
     */
    private boolean isInDatabase (SeekerImage seekerImage) throws SQLException {
        try {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM image WHERE hash = ?")) {
                stmt.setString(1, seekerImage.getHash());

                try (ResultSet set = stmt.executeQuery()) {
                    return set.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
