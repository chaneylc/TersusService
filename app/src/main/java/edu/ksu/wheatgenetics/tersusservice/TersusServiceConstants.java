package edu.ksu.wheatgenetics.tersusservice;

/**
 * Created by chaneylc on 5/18/2017.
 */

interface TersusServiceConstants {

    int MESSAGE_READ = 100;
    int MESSAGE_WRITE = 101;
    int MESSAGE_DISCONNECT = 102;
    int MESSAGE_AVG_BASE = 103;
    int REQUEST_COARSE_LOCATION = 200;
    int REQUEST_ENABLE_BT = 201;

    //actions
    String TERSUS_COMMAND = "edu.ksu.wheatgenetics.tersusservice.TERSUS_COMMAND";

    //broadcasts
    String BROADCAST_TERSUS_OUTPUT = "edu.ksu.wheatgenetics.tersusservice.BROADCAST_TERSUS_OUTPUT";
    String BROADCAST_TERSUS_CONNECTION = "edu.ksu.wheatgenetics.tersusservice.BROADCAST_TERSUS_CONNECTION";
    String BROADCAST_TERSUS_DISCOVERY = "edu.ksu.wheatgenetics.tersusservice.BROADCAST_TERSUS_DISCOVERY";

    //extras
    String TERSUS_COMMAND_STRING = "edu.ksu.wheatgenetics.tersusservice.TERSUS_COMMAND_STRING";
    String TERSUS_CONNECTION = "edu.ksu.wheatgenetics.tersusservice.TERSUS_CONNECTION";
    String TERSUS_OUTPUT = "edu.ksu.wheatgenetics.tersusservice.TERSUS_OUTPUT";
    String TERSUS_DISCOVERY = "edu.ksu.wheatgenetics.terssuservice.TERSUS_DISCOVERY";
}
