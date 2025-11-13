package com.bmc.ims;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableModel;
//import org.json.JSONArray;
//import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the dynamic model for the details table that shows the cfa report.
 *
 * <p>
 * This model consists of the following columns:
 * </p>
 * <ul>
 * <li>file name</li>
 * <li>total number of different authors</li>
 * <li>total number of commits</li>
 * <li>time of last commit</li>
 * <li>time of first commit</li>
 * </ul>
 *
 * @author Marit cohen
 */

public class ReportTableModel extends TableModel {

    private List<JsonObject> ja;
    String rptType;


    public ReportTableModel(List<JsonObject> ja, String rptType) {
        super();
        this.ja = ja;
        this.rptType=rptType;
    }

    @Override
    public String getId() {
        //return ForensicsJobAction.FORENSICS_ID;
        return "cfa";
    }

    @Override
    public List<TableColumn> getColumns() {
        List<TableColumn> columns = new ArrayList<>();

        columns.add(new TableColumn("Job name", "jobName"));
        if(this.rptType.equals("IMS"))
            columns.add(new TableColumn("PSB Name", "psbName"));
        else
            columns.add(new TableColumn("Plan Name", "planName"));
  //      columns.add(new TableColumn("Start time", "startTime").setHeaderClass(ColumnCss.DATE));
        columns.add(new TableColumn("Start time", "startTime"));
        if(this.rptType.equals("IMS"))
            columns.add(new TableColumn("#Checkpoints", "chkpt#"));
        else
            columns.add(new TableColumn("#Commits", "commits#"));
        if(this.rptType.equals("IMS"))
            columns.add(new TableColumn("Checkpoint Type", "chkptType"));
        columns.add(new TableColumn("Job Duration", "jobDuration"));
        columns.add(new TableColumn("Commit per min", "freqPerMin"));
        columns.add(new TableColumn("Commit per sec", "freqPerSec"));
        columns.add(new TableColumn("Exceptions", "exceptions"));

        return columns;
    }


    @Override
    public List<Object> getRows() {

        List<Object> rows = new ArrayList<>();

        //skip the header row
        //for (int i = 1; i < ja.length(); i++)
            for (JsonObject obj : this.ja) 
        {

            //JsonObject obj=  ja.get(i);
            Map<String, String> rowsMap = new HashMap<>();
            rowsMap.put("jobName",obj.get("jobName").toString());
            if(this.rptType.equals("IMS"))
                rowsMap.put("psbName",obj.get("psbName").toString());
            else
                rowsMap.put("planName",obj.get("planName").toString());
            rowsMap.put("startTime",obj.get("startTime").toString());
            if(this.rptType.equals("IMS")) {
                rowsMap.put("chkpt#", obj.get("chkpt#").toString());
                rowsMap.put("chkptType", obj.get("chkptType").toString());
            }
            else
                rowsMap.put("commits#", obj.get("commits#").toString());

            rowsMap.put("jobDuration",obj.get("jobDuration").toString());
            rowsMap.put("freqPerMin",obj.get("freqPerMin").toString());
            rowsMap.put("freqPerSec",obj.get("freqPerSec").toString());
            rowsMap.put("exceptions",obj.get("exceptions").toString());
            rows.add(rowsMap);

        }


        //return rowsMap.entrySet().stream().filter(e -> e.getValue().matches(".*")).map(Map.Entry::getKey).collect(Collectors.toList());
        return rows;
    }
}
