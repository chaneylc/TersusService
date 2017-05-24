package edu.ksu.wheatgenetics.tersusservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by chaneylc on 5/19/2017.
 */

class TersusString implements Parcelable {

    final String newline = System.getProperty("line.separator");

    private String talkerIdentifier, sentenceIdentifier;

    private String nmeaSentence, satSystem, UTC, latitude, latIndicator, longitude,
            longIndicator, qualityIndicator, numSat, HDOP, antennaAltitude,
            antennaAltUnits, geoidalSeparation, geoidalSeparationUnits,
            ageOfDiffCorrection, diffRefStationID, speedOverGround,
            trackMadeGood, status, date, magneticVariation, magneticVariationIndicator,
            T, M, N, K, trackDegreesT, trackDegreesM, speedKnots, speedKph, numMessages,
            messageNum, satellitesInView, satelliteNum, elevationInDeg, azimuthInDegreesT,
            SNR, selectionMode, mode, PDOP, VDOP, prn1, prn2, prn3, prn4, prn5, prn6,
            prn7, prn8, prn9, prn10, prn11, prn12;


    TersusString(byte[] bytes) {

        talkerIdentifier = sentenceIdentifier = "";

        nmeaSentence = satSystem = UTC
                = latitude = latIndicator = longitude
                = longIndicator = qualityIndicator = numSat
                = HDOP = antennaAltitude = antennaAltUnits
                = geoidalSeparationUnits = geoidalSeparation = ageOfDiffCorrection
                = diffRefStationID = speedOverGround = trackMadeGood
                = status = date = magneticVariation = magneticVariationIndicator
                = T = M = N = K = trackDegreesT = trackDegreesM = speedKnots = speedKph
                = numMessages = messageNum = satellitesInView = satelliteNum
                = elevationInDeg = azimuthInDegreesT = SNR = selectionMode = mode
                = PDOP = VDOP = prn1 = prn2 = prn3 = prn4 = prn5 = prn6 = prn7 = prn8
                = prn9 = prn10 = prn11 = prn12 = "";

        String nmeaChunk = new String(bytes);

        final String[] chunks = nmeaChunk.split("\\r\\n");

        if (chunks.length >= 1) {

            //iterate through chunks and find a logged nmea sentence (begins with $)
            for(String chunk : chunks) {
                nmeaChunk = chunk;
                if (nmeaChunk.startsWith("$"))
                    break;
            }

            //split nmea sentence between the checksum and the tokens
            final String[] tokens = nmeaChunk.split("\\*");
            if (tokens.length == 2) {
                final int checksum = Integer.valueOf(tokens[1]);
                //the final token should be the checksum starting with a '*'
                final char[] characters = tokens[0].toCharArray();
                //verify the checksum
                //Tersus GNSS documentation is incorrect! begin checksum with '$'
                int actualChecksum = 0;
                for (char c : characters) {
                    actualChecksum ^= c;
                }
                if (actualChecksum == checksum) {
                    parseNmea(tokens[0].substring(1));
                }
            }
        }
    }

    private TersusString(Parcel source) {

        final String[] data = source.createStringArray();
        if (data.length >= 2) {
            this.talkerIdentifier = data[0];
            this.sentenceIdentifier = data[1];

            switch (talkerIdentifier) {

                case "GP":
                    this.satSystem="GPS";
                    break;

                case "GN":
                    this.satSystem="GLONASS";
                    break;

                default:
                    throw new UnsupportedOperationException("Proprietary sentence detected.");
            }

            switch (sentenceIdentifier) {
                case "GGA": {
                    buildGGAString(data);
                    break;
                }
                case "RMC": {
                    buildRMCString(data);
                    break;
                }
                case "VTG": {
                    buildVTGString(data);
                    break;
                }
                case "GSA": {
                    buildGSAString(data);
                    break;
                }
                case "GSV": {
                    buildGSVString(data);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void parseNmea(String rawString) {

        if (rawString.contains(",")) {
            final String[] tokens = rawString.split(",", -1);
            talkerIdentifier = tokens[0].substring(0, 2);
            sentenceIdentifier = tokens[0].substring(2);

            switch (talkerIdentifier) {

                case "GP":
                    this.satSystem="GPS";
                    break;

                case "GN":
                    this.satSystem="GLONASS";
                    break;

                default:
                    throw new UnsupportedOperationException("Proprietary sentence detected.");
            }

            switch (sentenceIdentifier) {
                case "GGA": {
                    buildGGAString(tokens);
                    break;
                }
                case "RMC": {
                    buildRMCString(tokens);
                    break;
                }
                case "VTG": {
                    buildVTGString(tokens);
                    break;
                }
                case "GSA": {
                    buildGSAString(tokens);
                    break;
                }
                case "GSV": {
                    buildGSVString(tokens);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void buildGGAString(String[] tokens) {

        if (tokens.length >= 15) {
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
        }

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

        this.nmeaSentence = sb.toString();
    }

    private void buildRMCString(String[] tokens) {

        if (tokens.length >= 12) {
            this.UTC = tokens[1] == null ? "" : tokens[1];
            this.status = tokens[2] == null ? "" : tokens[2];
            this.latitude = tokens[3] == null ? "" : tokens[3];
            this.latIndicator = tokens[4] == null ? "" : tokens[4];
            this.longitude = tokens[5] == null ? "" : tokens[5];
            this.longIndicator = tokens[6] == null ? "" : tokens[6];
            this.speedOverGround = tokens[7] == null ? "" : tokens[7];
            this.trackMadeGood = tokens[8] == null ? "" : tokens[8];
            this.date = tokens[9] == null ? "" : tokens[9];
            this.magneticVariation = tokens[10] == null ? "" : tokens[10];
            this.magneticVariationIndicator = tokens[11] == null ? "" : tokens[11];
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("SatelliteSystem: ");
        sb.append(this.satSystem.isEmpty() ? "NONE" : this.satSystem);
        sb.append(newline);

        sb.append("UTC: ");
        sb.append(this.UTC.isEmpty() ? "NONE": this.UTC);
        sb.append(newline);

        sb.append("Status: ");
        sb.append(this.status.isEmpty() ? "NONE" : this.status);
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

        sb.append("Speed over ground (Knots): ");
        sb.append(speedOverGround.isEmpty() ? "NONE" : speedOverGround);
        sb.append(newline);

        sb.append("Track made good (degrees T): ")
                .append(trackMadeGood.isEmpty() ? "NONE" : trackMadeGood);
        sb.append(newline);

        sb.append("Date: ");
        sb.append(date.isEmpty() ? "NONE" : date);
        sb.append(newline);

        sb.append("Magnetic Variation: ");
        if (!magneticVariationIndicator.isEmpty() && !magneticVariation.isEmpty()) {
            if (magneticVariationIndicator.equals("W")) {
                sb.append("-");
                sb.append(this.magneticVariation);
            } else sb.append(this.magneticVariation);
        } else sb.append("NONE");
        sb.append(newline);

        this.nmeaSentence = sb.toString();
    }

    private void buildVTGString(String[] tokens) {

        if (tokens.length >= 9) {
            this.trackDegreesT = tokens[1] == null ? "" : tokens[1];
            this.T = tokens[2] == null ? "" : tokens[2];
            this.trackDegreesM = tokens[3] == null ? "" : tokens[3];
            this.M = tokens[4] == null ? "" : tokens[4];
            this.speedKnots = tokens[5] == null ? "" : tokens[5];
            this.N = tokens[6] == null ? "" : tokens[6];
            this.speedKph = tokens[7] == null ? "" : tokens[7];
            this.K = tokens[8] == null ? "" : tokens[8];
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("Satellite System: ");
        sb.append(satSystem.isEmpty() ? "NONE" : satSystem);
        sb.append(newline);

        sb.append("Track Degrees (True): ");
        sb.append(trackDegreesT.isEmpty() ? "NONE" : trackDegreesT);
        sb.append(newline);

        sb.append("T: ");
        sb.append(T.isEmpty() ? "NONE" : T);
        sb.append(newline);

        sb.append("Track Degrees (Magnetic): ");
        sb.append(trackDegreesM.isEmpty() ? "NONE" : trackDegreesM);
        sb.append(newline);

        sb.append("M: ");
        sb.append(M.isEmpty() ? "NONE" : M);
        sb.append(newline);

        sb.append("Speed Knots: ");
        sb.append(speedKnots.isEmpty() ? "NONE" : speedKnots);
        sb.append(newline);

        sb.append("N: ");
        sb.append(N.isEmpty() ? "NONE" : N);
        sb.append(newline);

        sb.append("Speed Km/h: ");
        sb.append(speedKph.isEmpty() ? "NONE" : speedKph);
        sb.append(newline);

        sb.append("K: ");
        sb.append(K.isEmpty() ? "NONE" : K);
        sb.append(newline);

        nmeaSentence = sb.toString();
    }

    private void buildGSVString(String[] tokens) {

        if (tokens.length >= 8) {
            this.numMessages = tokens[1] == null ? "" : tokens[1];
            this.messageNum = tokens[2] == null ? "" : tokens[2];
            this.satellitesInView = tokens[3] == null ? "" : tokens[3];
            this.satelliteNum = tokens[4] == null ? "" : tokens[4];
            this.elevationInDeg = tokens[5] == null ? "" : tokens[5];
            this.azimuthInDegreesT = tokens[6] == null ? "" : tokens[6];
            this.SNR = tokens[7] == null ? "" : tokens[7];
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("Satellite System: ");
        sb.append(satSystem.isEmpty() ? "NONE" : satSystem);
        sb.append(newline);

        sb.append("Total messages: ");
        sb.append(numMessages.isEmpty() ? "NONE" : numMessages);
        sb.append(newline);

        sb.append("Message number: ");
        sb.append(messageNum.isEmpty() ? "NONE" : messageNum);
        sb.append(newline);

        sb.append("Satellites in view: ");
        sb.append(satellitesInView.isEmpty() ? "NONE" : satellitesInView);
        sb.append(newline);

        sb.append("Satellite number: ");
        sb.append(satelliteNum.isEmpty() ? "NONE" : satelliteNum);
        sb.append(newline);

        sb.append("Elevation in degrees: ");
        sb.append(elevationInDeg.isEmpty() ? "NONE" : elevationInDeg);
        sb.append(newline);

        sb.append("Azimuth in degrees to True: ");
        sb.append(azimuthInDegreesT.isEmpty() ? "NONE" : azimuthInDegreesT);
        sb.append(newline);

        sb.append("SNR in dB: ");
        sb.append(SNR.isEmpty() ? "NONE" : SNR);
        sb.append(newline);

        nmeaSentence = sb.toString();
    }

    private void buildGSAString(String[] tokens) {

        if (tokens.length >= 18) {
            selectionMode = tokens[1] == null ? "" : tokens[1];
            mode = tokens[2] == null ? "" : tokens[2];
            prn1 = tokens[3] == null ? "" : tokens[3];
            prn2 = tokens[4] == null ? "" : tokens[4];
            prn3 = tokens[5] == null ? "" : tokens[5];
            prn4 = tokens[6] == null ? "" : tokens[6];
            prn5 = tokens[7] == null ? "" : tokens[7];
            prn6 = tokens[8] == null ? "" : tokens[8];
            prn7 = tokens[9] == null ? "" : tokens[9];
            prn8 = tokens[10] == null ? "" : tokens[10];
            prn9 = tokens[11] == null ? "" : tokens[11];
            prn10 = tokens[12] == null ? "" : tokens[12];
            prn11 = tokens[13] == null ? "" : tokens[13];
            prn12 = tokens[14] == null ? "" : tokens[14];
            PDOP = tokens[15] == null ? "" : tokens[15];
            HDOP = tokens[16] == null ? "" : tokens[16];
            VDOP = tokens[17] == null ? "" : tokens[17];
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("Satellite System: ");
        sb.append(satSystem.isEmpty() ? "" : satSystem);
        sb.append(newline);

        sb.append("Selection Mode: ");
        sb.append(selectionMode.isEmpty() ? "" : selectionMode);
        sb.append(newline);

        sb.append("Mode: ");
        sb.append(mode.isEmpty() ? "" : mode);
        sb.append(newline);

        sb.append("PRN1: ");
        sb.append(prn1.isEmpty() ? "" : prn1);
        sb.append(newline);

        sb.append("PRN2: ");
        sb.append(prn2.isEmpty() ? "" : prn2);
        sb.append(newline);

        sb.append("PRN3: ");
        sb.append(prn3.isEmpty() ? "" : prn3);
        sb.append(newline);

        sb.append("PRN4: ");
        sb.append(prn4.isEmpty() ? "" : prn4);
        sb.append(newline);

        sb.append("PRN5: ");
        sb.append(prn5.isEmpty() ? "" : prn5);
        sb.append(newline);

        sb.append("PRN6: ");
        sb.append(prn6.isEmpty() ? "" : prn6);
        sb.append(newline);

        sb.append("PRN7: ");
        sb.append(prn7.isEmpty() ? "" : prn7);
        sb.append(newline);

        sb.append("PRN8: ");
        sb.append(prn8.isEmpty() ? "" : prn8);
        sb.append(newline);

        sb.append("PRN9: ");
        sb.append(prn9.isEmpty() ? "" : prn9);
        sb.append(newline);

        sb.append("PRN10: ");
        sb.append(prn10.isEmpty() ? "" : prn10);
        sb.append(newline);

        sb.append("PRN11: ");
        sb.append(prn11.isEmpty() ? "" : prn11);
        sb.append(newline);

        sb.append("PRN12: ");
        sb.append(prn12.isEmpty() ? "" : prn12);
        sb.append(newline);

        sb.append("PDOP: ");
        sb.append(PDOP.isEmpty() ? "" : PDOP);
        sb.append(newline);

        sb.append("HDOP: ");
        sb.append(HDOP.isEmpty() ? "" : HDOP);
        sb.append(newline);

        sb.append("VDOP: ");
        sb.append(VDOP.isEmpty() ? "" : VDOP);
        sb.append(newline);

        nmeaSentence = sb.toString();
    }

    @Override
    public String toString() {

        return this.nmeaSentence;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        String[] data;
        switch (sentenceIdentifier) {
            case "GGA":
                data = new String[] { talkerIdentifier, sentenceIdentifier,
                        satSystem, UTC, latitude, latIndicator, longitude,
                        longIndicator, qualityIndicator, numSat, HDOP, antennaAltitude,
                        antennaAltUnits, geoidalSeparation, geoidalSeparationUnits,
                        ageOfDiffCorrection, diffRefStationID
                };
                break;
            case "RMC":
                data = new String[] { talkerIdentifier, sentenceIdentifier,
                        satSystem, UTC, status, latitude, latIndicator,
                    longitude, longIndicator, speedOverGround, trackMadeGood, date,
                    magneticVariation, magneticVariationIndicator};
                break;
            case "VTG":
                data = new String[] { talkerIdentifier, sentenceIdentifier,
                        satSystem, trackDegreesT, T, trackDegreesM, M,
                    speedKnots, N, speedKph, K };
                break;
            case "GSA":
                data = new String[] { talkerIdentifier, sentenceIdentifier,
                satSystem, selectionMode, mode, prn1, prn2, prn3, prn4, prn5,
                prn6, prn7, prn8, prn9, prn10, prn11, prn12, PDOP, HDOP, VDOP};
                break;
            case "GSV":
                data = new String[] { talkerIdentifier, sentenceIdentifier,
                        satSystem, numMessages, messageNum, satellitesInView,
                    satelliteNum, elevationInDeg, azimuthInDegreesT, SNR };
                break;
            default:
                data = null;
                break;
        }

        dest.writeStringArray(data);
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
}
