package entity;

import java.util.Objects;

public class SeekerImage {
    private String creator; // The email of the user that created the image
    private String name; // The actual name of the image, needs to set to a UUID
    private String url; // The URL at which it's located in the bucket
    private int timestamp; // The Unix timestamp of when the photo was taken
    private Double lat; // The latitude at which this image was taken
    private Double lon; // The longitude ...
    private String hash; // The MD5 hash of the image
    private boolean hasBunny; // Whether the picture contains a bunny or not
    private boolean hasSquirrel; // Whether the picture contains a squirrel

    public SeekerImage() {}

    public SeekerImage(String creator, int timestamp, Double lat, Double lon) {
        this.creator = creator;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
    }

    public SeekerImage(String creator, String name, String url, int timestamp, Double lat, Double lon,
                       String hash, boolean hasBunny, boolean hasSquirrel) {
        this.creator = creator;
        this.name = name;
        this.url = url;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
        this.hash = hash;
        this.hasBunny = hasBunny;
        this.hasSquirrel = hasSquirrel;
    }

    public String toString() {
        return creator + ":" + name + ":" + url + ":" + timestamp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof SeekerImage) {
            // TODO very advanced, run a perceptual hashing algorithm on the two images
            // https://kandepet.com/detecting-similar-and-identical-images-using-perseptual-hashes/

            return Objects.equals(this.creator, ((SeekerImage) o).creator) &&
                    (this.timestamp == ((SeekerImage) o).timestamp);
        }
        return false;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean isHasBunny() {
        return hasBunny;
    }

    public void setHasBunny(boolean hasBunny) {
        this.hasBunny = hasBunny;
    }

    public boolean isHasSquirrel() {
        return hasSquirrel;
    }

    public void setHasSquirrel(boolean hasSquirrel) {
        this.hasSquirrel = hasSquirrel;
    }
}
