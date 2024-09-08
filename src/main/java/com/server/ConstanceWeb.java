package com.server;

public class ConstanceWeb {

    public static final String pathProject = System.getProperty("user.dir").concat("\\");;

    public final static String pathFolderCertBS = pathProject.concat("certBS\\");
    public final static String pathFolderSslTrial = pathProject.concat("ssltrial\\");

    public final static String pathConfig = pathProject.concat("configWeb.json");
    
    public static final String Db_Uri = "mongodb://localhost:27017";
    public static final String Db_Name = "InfoUserVH";

    public static final String Collections_User = "InfoAllUserQickWeb";
    public static final String Collections_Admin = "InfoAdminQickWeb";
    
    public static final String resultFind = "resultFind";
    public static final String valueJWT = "valueJWT";
    
    public final static String mapConfigWeb = "mapConfigWeb";
    
//    public static final String path

    static {

    }
}
