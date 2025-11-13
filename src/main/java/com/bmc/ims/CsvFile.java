package com.bmc.ims;

//import org.json.JSONArray;
//import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CsvFile {

    public static List<JsonObject> readCsvFile(String csvFilePath) {

        BufferedReader br = null;
        File file = new File(csvFilePath);
        //List<ReportView> report = new Vector<ReportView>();

        List<JsonObject> jsonObjArr = new ArrayList<JsonObject>();

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()));
           // br = new BufferedReader(new FileReader(file));
            String line = null;

            int i=0;
            while ((line = br.readLine()) != null) {
                if(i>0) {
                    String values[] = line.split(",");
                    jsonObjArr.add(createJsonModel(values, file.getName()));
                }
                i++;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return jsonObjArr;

    }
    //JOB,PLANNAME,START TIME,#COMMITS,,JOB DURATION,FREQ/MIN,FREQ/SEC,EXCEPTIONS
    private static JsonObject createJsonModel(String values[], String rptType)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("jobName",values[0]);

        if(rptType.contains("DB2"))
        {
            obj.addProperty("planName", values[1]);
            obj.addProperty("startTime", values[2]);
            obj.addProperty("commits#", values[3]);
            //#4 is blank
            obj.addProperty("jobDuration", values[5]);
            obj.addProperty("freqPerMin", values[6]);
            obj.addProperty("freqPerSec",values[7]);
            obj.addProperty("exceptions", values[8]);
        }
        else if(rptType.contains("IMS") || rptType.contains("DLI") )
        {
            obj.addProperty("psbName", values[1]);
            obj.addProperty("startTime", values[2]);
            obj.addProperty("chkpt#", values[3]);
            obj.addProperty("chkptType", values[4]);
            obj.addProperty("jobDuration", values[5]);
            obj.addProperty("freqPerMin", values[6]);
            obj.addProperty("freqPerSec",values[7]);
            obj.addProperty("exceptions", values[8]);

        }


        return obj;
    }
}

