package edu.ksu.wheatgenetics.tersusservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chaneylc on 5/19/2017.
 */

class TersusString implements Parcelable {

    private String satSystem, UTC, latitude, latIndicator, longitude,
            longIndicator, qualityIndicator, numSat, HDOP, antennaAltitude,
            antennaAltUnits, geoidalSeparation, geoidalSeparationUnits,
            ageOfDiffCorrection, diffRefStationID, checksum;


    TersusString(byte[] bytes) {

        this.satSystem = this.UTC = this.latitude = this.latIndicator =
                this.longitude = this.longIndicator = this.qualityIndicator =
                        this.numSat = this.HDOP = this.antennaAltitude = this.antennaAltUnits =
                                this.geoidalSeparationUnits = this.geoidalSeparation = this.ageOfDiffCorrection =
                                        this.diffRefStationID = "";

        String nmeaChunk = new String(bytes);

        //sometimes the transmission does not contain the $
        if (nmeaChunk.startsWith("$")) {
            nmeaChunk = nmeaChunk.substring(1);
        }

        final String[] chunks = nmeaChunk.split("\\r");
        if (chunks.length >= 1) {
            nmeaChunk = chunks[0];
            final String[] tokens = nmeaChunk.split("\\*");
            if (tokens.length == 2) {
                nmeaChunk = tokens[0].substring(0, tokens[0].length());//.subSequence(0, tokens[0].length() - 1).toString();
                final int checksum = Integer.valueOf(tokens[1]);
                //the final token should be the checksum starting with a '*'
                final char[] characters = nmeaChunk.toCharArray();
                //verify the checksum
                int actualChecksum = '$';
                for (char c : characters) {
                    actualChecksum ^= c;
                }
                //Tersus GNSS documentation is incorrect! xor '$' finally to sum the correct checksum
                //actualChecksum ^= '$';
                if (actualChecksum == checksum) {
                    parseNmea(nmeaChunk);
                }
            }
        }
    }

    private TersusString(Parcel source) {

        this.satSystem = source.readString();
        this.UTC =  source.readString();
        this.latitude =  source.readString();
        this.latIndicator =  source.readString();
        this.longitude =  source.readString();
        this.longIndicator =  source.readString();
        this.qualityIndicator =  source.readString();
        this.numSat =  source.readString();
        this.HDOP =  source.readString();
        this.antennaAltitude =  source.readString();
        this.antennaAltUnits =  source.readString();
        this.geoidalSeparation =  source.readString();
        this.geoidalSeparationUnits =  source.readString();
        this.ageOfDiffCorrection = source.readString();
        this.diffRefStationID =  source.readString();
        this.checksum =  source.readString();
    }

    private void parseNmea(String rawString) {

        if (rawString.contains(",")) {
            String[] tokens = rawString.split(",", -1);
            if (tokens.length >= 15) {
                this.satSystem = tokens[0] == null ? "" : tokens[0];
                this.UTC = tokens[1] == null ? "" : tokens[1];
                this.latitude = tokens[2] == null ? "" : tokens[2];
                this.latIndicator = tokens[3] == null ? "" : tokens[3];
                this.longitude = tokens[4] == null ? "" : tokens[4];
                this.longIndicator = tokens[5] == null ? "" : tokens[5];
                this.qualityIndicator = tokens[6] == null ? "" : tokens[6];
                this.numSat = tokens[7] == null ? "" : tokens[7];
                this.HDOP = tokens[8] == null ? "" : tokens[8];
                this.antennaAltitude = tokens[9] == null ? "" : tokens[9];
                this.antennaAltUnits = tokens[10] == null ? "" : tokens[10];
                this.geoidalSeparation = tokens[11] == null ? "" : tokens[11];
                this.geoidalSeparationUnits = tokens[12] == null ? "" : tokens[12];
                this.ageOfDiffCorrection = tokens[13] == null ? "" : tokens[13];
                this.diffRefStationID = tokens[14] == null ? "" : tokens[14];
                //this.checksum = tokens[15];
            }
        }
    }

    @Override
    public String toString() {

        final String newline = System.getProperty("line.separator");
        final StringBuilder sb = new StringBuilder();

        sb.append("SatelliteSystem: ");
        sb.append(this.satSystem.isEmpty() ? "NONE" : this.satSystem);
        sb.append(newline);

        sb.append("UTC: ");
        sb.append(this.UTC.isEmpty() ? "NONE": this.UTC);
        sb.append(newline);

        sb.append("Latitude: ");
        if (!latIndicator.isEmpty() && !latitude.isEmpty()) {
            if (latIndicator.equals("S")) {
                sb.append("-");
                sb.append(this.latitude);
            } else sb.append(this.latitude);
        } else sb.append("NONE");
        sb.append(newline);

        sb.append("Longitude: ");
        if (!longIndicator.isEmpty() && !longitude.isEmpty()) {
            if (longIndicator.equals("W")) {
                sb.append("-");
                sb.append(this.longitude);
            } else sb.append(this.longitude);
        } else sb.append("NONE");
        sb.append(newline);

        sb.append("Quality: ");
        if (!qualityIndicator.isEmpty()) {
            int indicator = Integer.valueOf(this.qualityIndicator);
            if (indicator == 0) {
                sb.append("NO POSITION");
            } else if (indicator == 1) {
                sb.append("AUTONOMOUS");
            } else if (indicator == 2) {
                sb.append("DGPS");
            } else if (indicator == 4) {
                sb.append("RTK fixed");
            } else if (indicator == 5) {
                sb.append("RTK float");
            }
        } else sb.append("NONE");
        sb.append(newline);

        sb.append("Number of satellites: ");
        if (!numSat.isEmpty()) {
            sb.append(numSat);
        } else sb.append("NONE");
        sb.append(newline);

        sb.append("HDOP: ");
        if (!HDOP.isEmpty()) sb.append(HDOP);
        else sb.append("NONE");
        sb.append(newline);

        sb.append("Antenna altitude ");
        if (!antennaAltUnits.isEmpty()) {
            sb.append("(");
            sb.append(antennaAltUnits);
            sb.append(")");
            sb.append(":");
            if (!antennaAltitude.isEmpty()) sb.append(antennaAltitude);
            else sb.append("NONE");
        } else sb.append("NONE");
        sb.append(newline);

        sb.append("Geoidal separation ");
        if (!geoidalSeparationUnits.isEmpty()) {
            sb.append("(");
            sb.append(geoidalSeparationUnits);
            sb.append(")");
            sb.append(":");
            if (!geoidalSeparation.isEmpty()) sb.append(geoidalSeparation);
            else sb.append("NONE");
        } else sb.append("NONE");
        sb.append(newline);

        sb.append("Diff Correction Age (s): ");
        if (!ageOfDiffCorrection.isEmpty()) sb.append(ageOfDiffCorrection);
        else sb.append("NONE");
        sb.append(newline);

        sb.append("Diff ref station ID: ");
        if (!diffRefStationID.isEmpty()) sb.append(diffRefStationID);
        else sb.append("NONE");
        sb.append(newline);

        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
                this.satSystem, this.UTC, this.latitude, this.latIndicator,
                this.longitude, this.longIndicator, this.qualityIndicator,
                this.numSat, this.HDOP, this.antennaAltitude, this.antennaAltUnits,
                this.geoidalSeparation, this.geoidalSeparationUnits, this.ageOfDiffCorrection,
                this.diffRefStationID, this.checksum
        });
    }

    public static final Parcelable.Creator<TersusString> CREATOR
            = new Parcelable.Creator<TersusString>() {

        @Override
        public TersusString createFromParcel(Parcel source) {
            return new TersusString(source);
        }

        @Override
        public TersusString[] newArray(int size) {
            return new TersusString[size];
        }
    };

    public String getAgeOfDiffCorrection() {
        return ageOfDiffCorrection;
    }

    public String getDiffRefStationID() {
        return diffRefStationID;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAntennaAltUnits() {
        return antennaAltUnits;
    }

    public String getGeoidalSeparation() {
        return geoidalSeparation;
    }

    public String getGeoidalSeparationUnits() {
        return geoidalSeparationUnits;
    }

    public String getAntennaAltitude() {
        return antennaAltitude;
    }

    public String getHDOP() {
        return HDOP;
    }

    public String getNumSat() {
        return numSat;
    }

    public String getQualityIndicator() {
        return qualityIndicator;
    }

    public String getLongIndicator() {
        return longIndicator;
    }

    public String getUTC() {
        return UTC;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLatIndicator() {
        return latIndicator;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getSatSystem() {
        return satSystem;
    }
}
