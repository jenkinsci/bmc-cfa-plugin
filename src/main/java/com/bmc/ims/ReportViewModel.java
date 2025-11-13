package com.bmc.ims;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.echarts.Palette;
import edu.hm.hafner.echarts.PieChartModel;
import edu.hm.hafner.echarts.PieData;
import hudson.model.ModelObject;
import hudson.model.Run;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableModel;
import org.apache.commons.lang.ObjectUtils;
//import org.json.JSONArray;
//import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReportViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    List<JsonObject> ja;
    //JSONArray ja;
    Run owner;
    String rptType;
    ArrayList<ExceptionType> exceptions = new ArrayList<ExceptionType>();

    public ReportViewModel(Run owner, List<JsonObject> ja, String reportType) {
        super();
        this.ja = ja;
        this.owner=owner;
        this.rptType=reportType;
    }
    @Override
    public TableModel getTableModel(String s) {
        return new ReportTableModel(this.ja,this.rptType);
    }

    @Override
    public String getDisplayName() {
        if (this.rptType.equals("IMS"))
            return "Application Checkpoint";
        else
            return "Commit";
    }

    public String getCommitModel() {

        return create();

    }
    public Run getOwner(){
        return this.owner;
    }

    public String getRptType() {
        return rptType;
    }

   
    // Creates a pie chart based on data that resides on csv file  

    public String create() {
        int moreThen5=0, lessThen60=0,others=0;
        //JSONArray ja=CsvFile.readCsvFile(file.getPath());
        //skip the headers and start from 1

        //for(int i=1;i<this.ja.length();i++)
        for (JsonObject obj : this.ja) {

            String exceptionText=obj.get("exceptions").toString(); //*** More than 1 chkp / sec   ,
            String[] tmpArr=exceptionText.split("than");
            String start=tmpArr[0].replace("***","") + " than"; //*** More
            String end;
            int num ;
            if(tmpArr.length>1)
            {
                String[] arr2;
                if (rptType.equals("IMS") || rptType.equals("DLI"))
                    arr2 = tmpArr[1].split("chkp"); //than 1
                else //DB2
                    arr2 = tmpArr[1].split("commits");
                //System.out.println("arr2=" + arr2);
                num = Integer.parseInt(arr2[0].trim());
                // System.out.println("num=" + num);

                if (rptType.equals("IMS") || rptType.equals("DLI"))
                    end = "chkp " + arr2[1];
                else
                    end = "commits " + arr2[1];
            }
            else//no exceptions
            {
                num=0;
                end="Others";
            }


            if (exceptions.size() == 0)
                exceptions.add(new ExceptionType(start, num, end));
            for (int j = 0; j < exceptions.size(); j++) {
                ExceptionType ex = exceptions.get(j);
                if (ex.start.equals(start) && ex.end.equals(end) && ex.num == num)
                    ex.count++;
                else {
                    ExceptionType ex1 = new ExceptionType(start, num, end);
                    exceptions.add(ex1);
                    }
            }
        }
        PieChartModel model = new PieChartModel("Commit Dist");

        for(int j=0;j<exceptions.size();j++)
        {
            ExceptionType ex =exceptions.get(j);
            if(ex.end=="Others")
                model.add(new PieData("Others",ex.count), Palette.color(j));
            else
                model.add(new PieData(ex.start+" "+ex.num+" "+ex.end, ex.count), Palette.color(j));
        }

        String json = new JacksonFacade().toJson(model);

        return json;
    }
}

class ExceptionType
{
    String start;
    int num;
    String end;
    int count=0;

    ExceptionType(String start, int num, String end)
    {

        this.end=end;
        this.start=start;
        this.num=num;
    }
}