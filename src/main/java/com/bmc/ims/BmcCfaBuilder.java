package com.bmc.ims;

import groovy.lang.Binding;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import hudson.Util;


@Extension // annotation is required when writing a pipeline compatible plugin
public class BmcCfaBuilder extends Builder implements SimpleBuildStep, Serializable {
	private String server, port, user,  jclContent, jobCard ,acctno, goodRC;
	private String db2log, db2bsds, limit,skip,maxlogs,prilog,thresh,sortby,maxlogsRc,maxlogsAbend,
	jobInclude,jobExclude,psbInclude,psbExclude, chkfreqval,planInclude,planExclude,chkfreq;
	private boolean bmcSlds,bmcDb2log, bmcDb2bsds,bmcLimit,bmcSkip,bmcActiveOlds,bmcImsid,bmcDlilog,bmcMaxlogs,bmcJobname,bmcRecon
	,bmcTimezone,bmcStartInterval,bmcStopInterval,bmcJobInc,bmcJobExc,bmcPsbInc,bmcPsbExc,bmcThresh,bmcChkfreq,bmcAppcheck,bmcAll,bmcLsec,	
	bmcPlanInc,	bmcPlanExc,	bmcSortby,bmcCsv,bmcFullreport,bmcPrilog;
	
	private  List<CfaLoadLib> cfaLoadLibs = new ArrayList<CfaLoadLib>();
	private  List<CfaSldsLib> cfaSldsLibs = new ArrayList<CfaSldsLib>();
	private  List<CfaDliLib> cfaDliLibs = new ArrayList<CfaDliLib>();
	private  List<CfaImsid>   cfaImsids =   new ArrayList<CfaImsid>();
	private  List<CfaJobname>   cfaJobnames =   new ArrayList<CfaJobname>();
	private  List<CfaReconSet> cfaReconSets = new ArrayList<CfaReconSet>();
		
	private  Start start = new Start("start","","");
	private  Stop stop= new Stop("stop","","") ;
	private  Timezone tz=new Timezone("local","");
			
	private String groovyScript;
    private SecureGroovyScript script;
    
    private Secret pswd;
    
	//private static final long serialVersionUID = 1;



	// to avoid compilation error: annotated classes must have a public no-argument
	// // constructor
	public BmcCfaBuilder() {

	}
	//To customize serialization and deserialization, define readObject() and writeObject() methods in this class.
	// Throws exception while saving configuration 
	/*
	private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException {
	    throw new java.io.NotSerializableException( getClass().getName() );
	}

	private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
	    throw new java.io.NotSerializableException( getClass().getName() );
	}
	*/
	/*
	 * Bind the Java attributes to the Jelly properties by: annotating our public
	 * constructor with @DataBoundConstructor and adding them to the constructor and
	 * providing a public getter method for each of them (be careful to name them
	 * accordingly- Fields in config.jelly must match the parameter names in the
	 * "DataBoundConstructor")
	 */
	@DataBoundConstructor
	public BmcCfaBuilder(String server, String port, String user,  String jclContent, String chkfreq,
			 String jobCard, String acctno, String thresh,String sortby,String pswd,String prilog,
			 List<CfaLoadLib> cfaLoadLibs, List<CfaSldsLib> cfaSldsLibs, List<CfaDliLib> cfaDliLibs,List<CfaJobname> cfaJobnames, List <CfaReconSet> cfaReconSets,
			String goodRC, boolean bmcSlds, boolean bmcDb2log, String db2log, String db2bsds, String limit, String skip, String maxlogs,
			boolean bmcDb2bsds, boolean bmcLimit, boolean bmcSkip,boolean bmcActiveOlds, boolean bmcImsid,boolean bmcDlilog,boolean bmcMaxlogs,boolean bmcJobname, 
			List<CfaImsid> cfaImsids,boolean bmcRecon,boolean bmcTimezone,boolean bmcStartInterval, boolean bmcStopInterval,
			Start start, Stop stop, Timezone tz, 
			String jobInclude,String jobExclude,String psbInclude, String psbExclude,String maxlogsRc, String maxlogsAbend,
			boolean bmcJobInc, boolean bmcJobExc,boolean bmcPsbInc, boolean bmcPsbExc,boolean bmcChkfreq, boolean bmcThresh, boolean bmcPrilog,
			boolean bmcAppcheck, boolean bmcAll,boolean bmcLsec, String chkfreqval ,boolean bmcPlanInc,String planInclude,
			boolean bmcPlanExc,	String planExclude,	boolean bmcSortby,	boolean bmcCsv,	boolean bmcFullreport
			) {
		
		this.pswd = Secret.fromString(pswd);
		this.bmcSlds=bmcSlds;		
		this.bmcDb2log=bmcDb2log;
		this.cfaLoadLibs = cfaLoadLibs;
		this.cfaSldsLibs = cfaSldsLibs;
		this.cfaReconSets=cfaReconSets;
		this.cfaDliLibs=cfaDliLibs;
		this.cfaImsids = cfaImsids;
		this.cfaJobnames=cfaJobnames;
		this.bmcJobname=bmcJobname;
		this.bmcRecon=bmcRecon;
		this.bmcImsid=bmcImsid;
		this.server = server;
		this.port = port;
		this.user = user;
//		this.pswd = pswd;
		this.jclContent = jclContent;		
		this.jobCard = jobCard;
		this.acctno=acctno;		
		this.goodRC = goodRC;

		this.db2bsds=db2bsds;
		this.limit=limit;
		this.skip=skip;
		this.bmcSkip=bmcSkip;
		this.maxlogs=maxlogs;
		this.db2log=db2log;
		this.bmcLimit=bmcLimit;
		this.bmcDb2bsds=bmcDb2bsds;
		this.bmcMaxlogs=bmcMaxlogs;		
		this.bmcDlilog=bmcDlilog;
		this.bmcActiveOlds=bmcActiveOlds;		
		
		this.prilog=prilog;
		this.bmcPrilog=bmcPrilog;
		this.thresh=thresh;
		this.bmcThresh=bmcThresh;
		this.sortby=sortby;		
		
		this.bmcTimezone=bmcTimezone;
		this.tz=tz;
		
		this.bmcStartInterval=bmcStartInterval;
		this.bmcStopInterval=bmcStopInterval;		
		this.start=start;
		this.stop=stop;
		
		this.bmcAppcheck=bmcAppcheck;
		this.bmcAll=bmcAll;
		this.jobExclude=jobExclude;		
		this.jobInclude=jobInclude;
		this.psbExclude=psbExclude;
		this.psbInclude=psbInclude;
		this.bmcJobExc=bmcJobExc;
		this.bmcJobInc=bmcJobInc;
		this.bmcPsbExc=bmcPsbExc;
		this.bmcPsbInc=bmcPsbInc;
		
		this.chkfreqval=chkfreqval;
		this.bmcChkfreq=bmcChkfreq;
		this.chkfreq=chkfreq;
		this.bmcLsec=bmcLsec;
		this.bmcPlanInc=bmcPlanInc;
		this.planInclude=planInclude;
		this.bmcPlanExc=bmcPlanExc;
		this.planExclude=planExclude;
		this.bmcSortby=bmcSortby;
		this.bmcCsv=bmcCsv;
		this.bmcFullreport=bmcFullreport;
		
		this.maxlogsRc=maxlogsRc;
		this.maxlogsAbend=maxlogsAbend;
	}
	
	

	/*
	 * Getters and Setters !!! important for Configure to be able to read from
	 * config.xml
	 */
	public Secret getPswd() {
        return pswd;
    }
	
	public String getMaxlogsAbend() {
		return maxlogsAbend;
	}
	public void setMaxlogsAbend(String maxlogsAbend) {
		this.maxlogsAbend = maxlogsAbend;
	}
	
	public String getMaxlogsRc() {
		return maxlogsRc;
	}
	
	public void setMaxlogsRc(String maxlogsRc) {
		this.maxlogsRc = maxlogsRc;
	}
	public String getChkfreq() {
		return chkfreq;
	}
	public void setChkfreq(String chkfreq) {
		this.chkfreq = chkfreq;
	}
	public boolean isBmcActiveOlds() {
		return bmcActiveOlds;
	}
	public void setBmcActiveOlds(boolean bmcActiveOlds) {
		this.bmcActiveOlds = bmcActiveOlds;
	}
	
	
	public String getPlanInclude() {
		return planInclude;
	}
	public void setPlanInclude(String planInclude) {
		this.planInclude = planInclude;
	}
	public boolean isBmcPlanInc() {
		return bmcPlanInc;
	}
	public void setBmcPlanInc(boolean bmcPlanInc) {
		this.bmcPlanInc = bmcPlanInc;
	}
	public String getPlanExclude() {
		return planExclude;
	}
	public boolean isBmcPlanExc() {
		return bmcPlanExc;
	}
	public void setBmcPlanExc(boolean bmcPlanExc) {
		this.bmcPlanExc = bmcPlanExc;
	}
	public void setPlanExclude(String planExclude) {
		this.planExclude = planExclude;
	}
	public boolean isBmcSortby() {
		return bmcSortby;
	}
	public void setBmcSortby(boolean bmcSortby) {
		this.bmcSortby = bmcSortby;
	}
	public boolean isBmcCsv() {
		return bmcCsv;
	}
	public void setBmcCsv(boolean bmcCsv) {
		this.bmcCsv = bmcCsv;
	}
	public boolean isBmcFullreport() {
		return bmcFullreport;
	}
	public void setBmcFullreport(boolean bmcFullreport) {
		this.bmcFullreport = bmcFullreport;
	}
	public boolean isBmcLsec() {
		return bmcLsec;
	}
	public void setBmcLsec(boolean bmcLsec) {
		this.bmcLsec = bmcLsec;
	}
	public boolean isBmcAll() {
		return bmcAll;
	}
	public void setBmcAll(boolean bmcAll) {
		this.bmcAll = bmcAll;
	}
	public boolean isBmcAppcheck() {
		return bmcAppcheck;
	}
	public void setBmcAppcheck(boolean bmcAppcheck) {
		this.bmcAppcheck = bmcAppcheck;
	}
	public String getChkfreqval() {
		return chkfreqval;
	}
	public void setChkfreqval(String chkfreqval) {
		this.chkfreqval = chkfreqval;
	}
	public boolean isBmcChkfreq() {
		return bmcChkfreq;
	}
	public void setBmcChkfreq(boolean bmcChkfreq) {
		this.bmcChkfreq = bmcChkfreq;
	}
	public String getPsbExclude() {
		return psbExclude;
	}
	public void setPsbExclude(String psbExclude) {
		this.psbExclude = psbExclude;
	}
	
	public boolean isBmcPsbExc() {
		return bmcPsbExc;
	}
	public void setBmcPsbExc(boolean bmcPsbExc) {
		this.bmcPsbExc = bmcPsbExc;
	}
	
	public String getPsbInclude() {
		return psbInclude;
	}
	public void setPsbInclude(String psbInclude) {
		this.psbInclude = psbInclude;
	}
	
	public void setBmcPsbInc(boolean bmcPsbInc) {
		this.bmcPsbInc = bmcPsbInc;
	}
	public boolean isBmcPsbInc() {
		return bmcPsbInc;
	}
	
	public String getJobInclude() {
		return jobInclude;
	}
	public void setJobInclude(String jobInclude) {
		this.jobInclude = jobInclude;
	}
	public boolean isBmcJobInc() {
		return bmcJobInc;
	}
	public void setBmcJobInc(boolean bmcJobInc) {
		this.bmcJobInc = bmcJobInc;
	}
	
	public String getJobExclude() {
		return jobExclude;
	}
	public void setJobExclude(String jobExclude) {
		this.jobExclude = jobExclude;
	}
	
	public boolean isBmcJobExc() {
		return bmcJobExc;
	}
	
	public void setBmcJobExc(boolean bmcJobExc) {
		this.bmcJobExc = bmcJobExc;
	}
	
	public void setBmcStartInterval(boolean bmcStartInterval) {
		this.bmcStartInterval = bmcStartInterval;
	}
	public void setBmcStopInterval(boolean bmcStopInterval) {
		this.bmcStopInterval = bmcStopInterval;
	}
	
	public boolean isBmcStartInterval() {
		return bmcStartInterval;
	}
	public boolean isBmcStopInterval() {
		return bmcStopInterval;
	}
	
	public Start getStart() {
		return start;
	}
	public void setStart(Start start) {
		this.start = start;
	}
	public void setStop(Stop stop) {
		this.stop = stop;
	}
	public Stop getStop() {
		return stop;
	}
	
	public void setSortby(String sortby) {
		this.sortby = sortby;
	}
	public String getSortby() {
		return sortby;
	}
	
	public String getPrilog() {
		return prilog;
	}
	public void setPrilog(String prilog) {
		this.prilog = prilog;
	}
	
	public boolean isBmcPrilog() {
		return bmcPrilog;
	}
	public void setBmcPrilog(boolean bmcPrilog) {
		this.bmcPrilog = bmcPrilog;
	}
	public void setThresh(String thresh) {
		this.thresh = thresh;
	}
	public String getThresh() {
		return thresh;
	}
	public boolean isBmcThresh() {
		return bmcThresh;
	}
	public void setBmcThresh(boolean bmcThresh) {
		this.bmcThresh = bmcThresh;
	}
	public void setBmcRecon(boolean bmcRecon) {
		this.bmcRecon = bmcRecon;
	}
	public boolean isBmcRecon() {
		return bmcRecon;
	}
	public void setBmcJobname(boolean bmcJobname) {
		this.bmcJobname = bmcJobname;
	}
	
	public boolean isBmcJobname() {
		return bmcJobname;
	}
	public void setBmcDlilog(boolean bmcDlilog) {
		this.bmcDlilog = bmcDlilog;
	}
	public boolean isBmcDlilog() {
		return bmcDlilog;
	}
	public void setBmcImsid(boolean bmcImsid) {
		this.bmcImsid = bmcImsid;
	}
	public boolean isBmcImsid() {
		return bmcImsid;
	}
	public List<CfaJobname> getCfaJobnames() {
		return cfaJobnames;
	}
	public void setCfaJobnames(List<CfaJobname> cfaJobnames) {
		this.cfaJobnames = cfaJobnames;
	}
	public String getMaxlogs() {
		return maxlogs;
	}
	public void setMaxlogs(String maxlogs) {
		this.maxlogs = maxlogs;
	}
	public void setBmcMaxlogs(boolean bmcMaxlogs) {
		this.bmcMaxlogs = bmcMaxlogs;
	}
	public boolean isBmcMaxlogs() {
		return bmcMaxlogs;
	}
	public void setBmcTimezone(boolean bmcTimezone) {
		this.bmcTimezone = bmcTimezone;
	}
	public boolean isBmcTimezone() {
		return bmcTimezone;
	}
	public void setBmcDb2bsds(boolean bmcDb2bsds) {
		this.bmcDb2bsds = bmcDb2bsds;
	}
	public boolean isBmcDb2bsds() {
		return bmcDb2bsds;
	}
	
	public void setBmcLimit(boolean bmcLimit) {
		this.bmcLimit = bmcLimit;
	}
	public boolean isBmcLimit() {
		return bmcLimit;
	}
	
	public Timezone getTz() {
		return tz;
	}
	public void setTz(Timezone tz) {
		this.tz = tz;
	}
	public void setBmcSkip(boolean bmcSkip) {
		this.bmcSkip = bmcSkip;
	}
	
	public boolean isBmcSkip() {
		return bmcSkip;
	}
	public String getDb2log() {
		return db2log;
	}
	
	public void setDb2log(String db2log) {
		this.db2log = db2log;
	}
	
	public String getDb2bsds() {
		return db2bsds;
	}
	public void setDb2bsds(String db2bsds) {
		this.db2bsds = db2bsds;
	}
	
	public String getLimit() {
		return limit;
	}
	public void setLimit(String limit) {
		this.limit = limit;
	}
	
	public String getSkip() {
		return skip;
	}
	public void setSkip(String skip) {
		this.skip = skip;
	}
	public List<CfaLoadLib> getCfaLoadLibs() {
		return cfaLoadLibs;
	}
	public void setCfaLoadLibs(List<CfaLoadLib> cfaLoadLibs) {
		this.cfaLoadLibs = cfaLoadLibs;
	}
		
	public List<CfaSldsLib> getCfaSldsLibs() {
		return cfaSldsLibs;
	}	
	public void setCfaSldsLibs(List<CfaSldsLib> cfaSldsLibs) {
		this.cfaSldsLibs = cfaSldsLibs;
	}
	public List<CfaReconSet> getCfaReconSets() {
		return cfaReconSets;
	}
	public void setCfaReconSets(List<CfaReconSet> cfaReconSets) {
		this.cfaReconSets = cfaReconSets;
	}
	public List<CfaDliLib> getCfaDliLibs() {
		return cfaDliLibs;
	}
	public void setCfaDliLibs(List<CfaDliLib> cfaDliLibs) {
		this.cfaDliLibs = cfaDliLibs;
	}
	
	public List<CfaImsid> getCfaImsids() {
		return cfaImsids;
	}
	
	public void setCfaImsids(List<CfaImsid> cfaImsids) {
		this.cfaImsids = cfaImsids;
	}
	
	public void setBmcDb2log(boolean bmcDb2log) {
		this.bmcDb2log = bmcDb2log;
	}
	
	public boolean isBmcDb2log() {
		return bmcDb2log;
	}
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
/*
	public void setPswd(String pswd) {
		this.pswd = pswd;
	}

	public String getPswd() {
		return pswd;
	}
*/
	public void setJclContent(String jclContent) {
		this.jclContent = jclContent;
	}

	public String getJclContent() {
		return jclContent;
	}
	
	
	public String getJobCard() {
		return jobCard;
	}

	public void setJobCard(String jobCard) {
		this.jobCard = jobCard;
	}
	public String getAcctno() {
		return acctno;
	}
	public void setAcctno(String acctno) {
		this.acctno = acctno;
	}
	
	public String getGoodRC() {
		return goodRC;
	}

	public void setGoodRC(String goodRC) {
		this.goodRC = goodRC;
	}
	
	public boolean isBmcSlds() {
		return bmcSlds;
	}
	
	public void setBmcSlds(boolean bmcSlds) {
		this.bmcSlds = bmcSlds;
	}

	private String inspectFailureInLogs(String log)
	{	
		//System.out.println(log);
		// Set the job status according to RC
		// $HASP395 [job_name] ENDED - ABEND=S202			
		// $HASP395 [job_name] ENDED - RC=0000
		
		if (log.contains("$HASP395")) 
		{			
			if (log.indexOf("RC") != -1) 
			{
				String actRC=log.substring(log.indexOf("RC") + 3, log.indexOf("RC") + 7);
				//if RC!=0
				if (!actRC.equals("0000")) 
				{						
					int goodRc =Integer.parseInt(this.goodRC);  
					int actualRc=Integer.parseInt(actRC);
					if(actualRc>goodRc)
						return "RC=" + actRC;						
				}
			} 
			else if (log.indexOf("ABEND") != -1) 				
				return "ABEND=" + log.substring(log.indexOf("ABEND") + 6, log.indexOf("ABEND") + 10);				
		
		}
		// IEF453I [job_name] - JOB FAILED - JCL ERROR - TIME=02.12.30
		else if (log.contains("IEF453I")) 
		{
			return "JOB FAILED - JCL ERROR";
			
		}
		// IEFC452I [job_name] - JOB NOT RUN - JCL ERROR
		else if (log.contains("IEFC452I"))
		{
			return "JOB NOT RUN - JCL ERROR";
			
		}
		//$HASP106 JOB DELETED BY JES2 OR CANCELLED BY OPERATOR BEFORE EXECUTION 
		else if (log.contains("HASP106"))
		{
			return "JOB DELETED BY JES2 OR CANCELLED BY OPERATOR BEFORE EXECUTION ";
			
		}
		return null;
	}
	
	private String adjustBodyTo72Chars(String body)
	{
		String processedBody="";
		
		while(body.length()>0)
		{
			if(body.indexOf("\n")>73)
			{
				//break the line at the last " "
				for(int charsPerLine=72 ; charsPerLine>0 ; charsPerLine--)
				{
					String delimiter=body.substring(charsPerLine,charsPerLine+1);
					if(delimiter.equals(" ") || delimiter.equals(","))
					{
						processedBody=processedBody.concat(body.substring(0,charsPerLine)+"\n");					
						body=body.substring(charsPerLine);
						break;
					}
				}//end for loop				
			}
			// last line
			else if(body.indexOf("\n")==-1)
			{
				processedBody=processedBody.concat(body);
				body=body.substring(0,0); //sets length to 0
			}
			else if(body.indexOf("\n")<=73)
			{				
				//String[] temp=body.split("\\n");				
				processedBody=processedBody.concat(body.substring(0,body.indexOf("\n")+1));				
				body=body.substring(body.indexOf("\n")+1);			
			}

		}
		
		return processedBody;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath,
	 * hudson.Launcher, hudson.model.TaskListener) For pipeline-compatible plugin
	 */

	@Override
	@SuppressWarnings({"NP_NULL_ON_SOME_PATH_EXCEPTION", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
	/*
	 * Deprecated
	 * https://javadoc.jenkins-ci.org/jenkins/tasks/SimpleBuildStep.html
	 */
	//public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
	//		throws InterruptedException, IOException {

	  public void perform(Run<?,?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)	
			  throws InterruptedException, IOException {

		ResponseObject resp = null;
		String url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs";
		Properties headers = new Properties();
		String body = null;
		String jc=null; //jobcard
		String jobname = null;
		String jobid = null;
		String jobowner = null;
		String jobstatus = null;
		String jobtype = null;
		String jobretcode = null;
		long starttime = 0;
		long endtime = 0;
		long waittime = 0;
		String jobCompletion = null;
		String compareRC = "0001";

		/**************************************************************************/
		/* Login via zosmf */
		/**************************************************************************/
		listener.getLogger().println("user: " + user);
		listener.getLogger().println("server: " + server);
		listener.getLogger().println("port: " + port);	
		
			
		JCLService zosmf = new JCLService(true);

	//	String pswd = ((BmcCfaBuilder.DescriptorImpl)getDescriptor()).getPswd().getPlainText(); 
		zosmf.login(server, port, user, pswd.getPlainText(), listener);

		/**************************************************************************/
		/* Submit jobs with z/OS jobs REST interface */
		/**************************************************************************/
		// ZOSMF job related manual
		// https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.1.0/com.ibm.zos.v2r1.izua700/IZUHPINFO_API_PutSubmitJob.htm

		/***************************/
		/* set request's body */
		/***************************/

		// Using Groovy script to do string interpolation and resolve placeholders
		// marked with ${}
		// share data between the java application and the Groovy script using binding
		Binding binding = new Binding();		
		
		binding.setVariable("ACCTNO", this.acctno.toUpperCase());
		
		//Apply Groovy script security		
		ClassLoader cl = getClass().getClassLoader();	    
	    
	    try 
	    {
	        groovyScript="\"\"\"" + this.jobCard + "\"\"\"";
	        script = new SecureGroovyScript(groovyScript, false, null).configuringWithKeyItem();
	        jc=script.evaluate(cl, binding).toString();
			binding.setVariable("JOB_CARD", jc.toUpperCase());
		}
	    catch (Exception e)
	    {	            
			e.printStackTrace(listener.error("Failed to evaluate groovy script."));				
		}
	     

		
		List<CfaLoadLib> LoadList = getCfaLoadLibs();
		int indx1 = 0;
		for (CfaLoadLib i : LoadList) {
			binding.setVariable("CFA_LOAD" + String.valueOf(indx1), i.getLib().toUpperCase());
			indx1++;
		}

		List<CfaSldsLib> SldsList = getCfaSldsLibs();
		indx1 = 0;
		for (CfaSldsLib i : SldsList) {
			binding.setVariable("SLDS_NAME" + String.valueOf(indx1), i.getSldsname().toUpperCase());
			binding.setVariable("SLDS_VER" + String.valueOf(indx1), i.getSldsver().toUpperCase());
			binding.setVariable("SLDS_UNIT" + String.valueOf(indx1), i.getSldsunit().toUpperCase());
			binding.setVariable("SLDS_VOLSER" + String.valueOf(indx1), i.getSldsvolser().toUpperCase());
			indx1++;
		}		
		
		List<CfaReconSet> ReconList  = getCfaReconSets();
		indx1 = 0;
		for (CfaReconSet i : ReconList) {
			binding.setVariable("RECON1" + String.valueOf(indx1), i.getRecon1().toUpperCase());
			binding.setVariable("RECON2" + String.valueOf(indx1), i.getRecon2().toUpperCase());
			binding.setVariable("RECON3" + String.valueOf(indx1), i.getRecon3().toUpperCase());			
			indx1++;	
			}
		List<CfaDliLib> DliList = getCfaDliLibs();
		indx1 = 0;
		for (CfaDliLib i : DliList) {
			binding.setVariable("DLI_NAME" + String.valueOf(indx1), i.getDliname().toUpperCase());
			binding.setVariable("DLI_VERSION" + String.valueOf(indx1), i.getDliver().toUpperCase());
			binding.setVariable("DLI_UNIT" + String.valueOf(indx1), i.getDliunit().toUpperCase());
			binding.setVariable("DLI_VOLSER" + String.valueOf(indx1), i.getDlivolser().toUpperCase());
			indx1++;
		}
		
		List<CfaImsid> ImsidsList = getCfaImsids();
		indx1=0;
		for(CfaImsid i: ImsidsList)
		{
			binding.setVariable("IMSID" + String.valueOf(indx1), i.getImsid().toUpperCase());
		}
		
		List<CfaJobname> JobnamesList = getCfaJobnames();
		indx1=0;
		for(CfaJobname i: JobnamesList)
		{
			binding.setVariable("JOBNAME" + String.valueOf(indx1), i.getJobname().toUpperCase());
		}
		
		
		binding.setVariable("LIMIT", this.limit.toUpperCase());
		binding.setVariable("SKIP", this.skip.toUpperCase());
		binding.setVariable("DB2BSDS_DATA_SET", this.db2bsds.toUpperCase());
		binding.setVariable("DB2LOG_DATA_SET", this.db2log.toUpperCase());
		//binding.setVariable("ACTIVEOLDS", this.bmcActiveOlds);
		binding.setVariable("MAXLOGS", this.maxlogs);
		binding.setVariable("ABEND", this.maxlogsAbend);
		binding.setVariable("RC", this.maxlogsRc);
		
		binding.setVariable("PRILOG", this.prilog);
		
		if(this.tz!=null)
		{
			if(this.tz.getValue().equals("offset"))
			{
				binding.setVariable("TIMEZONE", this.tz.getTimezoneoffset());				
			}
			else
				binding.setVariable("TIMEZONE", this.tz.getValue().toUpperCase());
		}
		
		if(this.start!=null)
		{
			if(this.start.getValue().equals("logseq"))
			{
				binding.setVariable("START", this.start.getStartlogSeq());
				
			}
			else if(this.start.getValue().equals("date"))
				binding.setVariable("START", this.start.getStartDate());
			else
				binding.setVariable("START", "FIRST");
		}
		
		if(this.stop!=null)
		{
			if(this.stop.getValue().equals("logseq"))
				binding.setVariable("STOP", this.stop.getStoplogSeq());
			else if(this.stop.getValue().equals("date"))
				binding.setVariable("STOP", this.stop.getStopDate());
			else
				binding.setVariable("STOP", "LAST");
		}	
		binding.setVariable("INCLUDE_JOBS", this.jobInclude.toUpperCase());
		binding.setVariable("EXCLUDE_JOBS",this.jobExclude.toUpperCase());
		binding.setVariable("EXCLUDE_PSBS",this.psbExclude.toUpperCase());
		binding.setVariable("INCLUDE_PSBS",this.psbInclude.toUpperCase());
		binding.setVariable("EXCLUDE_PLANS",this.planExclude.toUpperCase());
		binding.setVariable("INCLUDE_PLANS",this.planInclude.toUpperCase());
		binding.setVariable("THRESH",this.thresh.toUpperCase());
		binding.setVariable("CHKFREQ",this.chkfreqval.toUpperCase());
		binding.setVariable("SORTBY",this.sortby.toUpperCase());

		// enclosing between triple quotes/double-quotes to initialize the value of a
		// string with multiple lines
		//body = shell.evaluate("\"\"\"" + this.jclContent + "\"\"\"").toString().replace(",,","");
		
		 try 
		 {
			 groovyScript="\"\"\"" + this.jclContent + "\"\"\"";
		     script = new SecureGroovyScript(groovyScript, false, null).configuring(ApprovalContext.create());
		     body=script.evaluate(cl, binding).toString().replace(",,","");
			 body=body.replace("(,", "(");
			 body=body.replace(",)", ")");
			 //This will replace every 72 characters with the same 80 characters and add a new line at the end
			 //body=body.replaceAll("(.{72})", "$1\n");

			 body=adjustBodyTo72Chars(body);

			 listener.getLogger().println("body:\n" + body);
		 }
		 catch (Exception e)
		 {
				e.printStackTrace(listener.error("Failed to evaluate groovy script."));				
		}
		 

	


		/***************************/
		/* Set headers */
		/***************************/
		headers.put("Content-Type", "text/plain");
		headers.put("X-IBM-Intrdr-Class", "A");
		headers.put("X-IBM-Intrdr-Recfm", "F");
		headers.put("X-IBM-Intrdr-Lrecl", "80");
		headers.put("X-IBM-Intrdr-Mode", "TEXT");

		// submit
		resp = zosmf.doRequest(url, "PUT", body, headers, listener);
		listener.getLogger().println("Server returned response code: " + resp.status + " " + resp.jobId);

		if (resp.status >= 200 && resp.status <= 299) {

			jobname = resp.jobName;
			jobid = resp.jobId;
			listener.getLogger().println("Job " + jobid + " submitted successfully to " + server);
		}

		else {
			listener.getLogger().println("Error during job submission");
			run.setResult(Result.FAILURE);
			return;
		}

		/**************************************************************************/
		/* Obtain the job status after job submission */
		/**************************************************************************/
		boolean jobCompleted = false;
		int retcount = 1;

		try {
			url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs/" + jobname + "/" + jobid;

			listener.getLogger().println("Waiting to retrieve job status...");

			headers = new Properties();
			headers.put("Content-Type", "application/json");
			body = "Obtain Job Status";
			
			while (!jobCompleted) {

				resp = zosmf.doRequest(url, "GET", body, headers, listener);
				//Bad request when the jobname includes # DLP#LIST
				//{"rc":4,"reason":7,"stack":"JesException: CATEGORY_SERVICE rc=4 reason=7 message=No match for method GET and pathInfo='\/DLP'\n\tat com.ibm.zoszmf.restjobs.util.JesException.serviceException(JesException.java:183)
				if(resp.status==400)	
				{
					run.setResult(Result.FAILURE);
					return;
				}	
				if (resp.jobStatus != null) {
					listener.getLogger()
							.println("Job Output Retrieval Attempt No= " + retcount + " status: " + resp.jobStatus);
					if (resp.jobStatus.equals("OUTPUT") || resp.jobStatus.equals("PRINT")) {
						jobCompleted = true;
					}
				}
				retcount++;

			}
		} catch (Exception ex) {
			ex.printStackTrace();
			listener.getLogger().println("Job status could not be retrieved");
			listener.getLogger().println(ex);

		}
		/**************************************************************************/
		/* List the job spool files of submitted job */
		/**************************************************************************/
		/*
		 * Sample response: [
		 * 
		 * {"jobid":�JOB00023�,"jobname":"TESTJOB1",�subsystem�:null,"id":1,
		 * "stepname":"JESE",�procstep�:null,"class":"H",
		 * "ddname":"JESMSGLG",�record-count�:14,"byte-count":1200, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/1/records"},
		 * {"jobid":�JOB00023�,"jobname":"TESTJOB1",�subsystem�:null,"id":2,
		 * "stepname":"JESE",�procstep�:null,"class":"H",
		 * "ddname":"JESJCL",�record-count�:10,"byte-count":526, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/2/records"},
		 * {"jobid":�JOB00023�,"jobname":"TESTJOB1",�subsystem�:null,"id":3,
		 * "stepname":"JESE",�procstep�:null,"class":"H",
		 * "ddname":"JESYSMSG",�record-count�:14,"byte-count":1255, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/3/records"},
		 * {"jobid":�JOB00023�,"jobname":"TESTJOB1",�subsystem�:null,"id":4,
		 * "stepname":"STEP57","procstep":"COMPILE","class":"H",
		 * "ddname":"SYSUT1","record-count":6,"byte-count":741, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/4/records"},
		 * {"jobid":�JOB00023�,"jobname":"TESTJOB1",�subsystem�:null,"id":5,
		 * "stepname":"STEP57","procstep":"COMPILE","class":"A",
		 * "ddname":"SYSPRINT","record-count":3,"byte-count":209, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/5/records"}
		 * ]
		 */

		try {
			url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs/" + jobname + "/" + jobid + "/" + "files";

			// if (debug) {
			listener.getLogger().println("HTTPS URL path to list the spool files: " + url);
			// }

			headers = new Properties();
			headers.put("Content-Type", "application/json");
			// if (debug) {
			listener.getLogger().println("Waiting to retrieve list of job spool files...");
			// }
			body = "List spool files";
			resp = zosmf.doRequest(url, "GET", body, headers, listener);

			listener.getLogger().println("Server returned response code: " + resp.status);
			String message = "Additional diagnostic response messages:\n" + resp.statAndHeaders.toString();
			// if (debug) {
			listener.getLogger().println(message);
			// }

		} catch (Exception ex) {
			listener.getLogger().println("List of job spool files could not be retrieved");
			ex.printStackTrace();

			listener.getLogger().println(ex);

		}

		/**************************************************************************/
		/* Retrieve Log from spool files. Get the content of Job spool files */
		/**************************************************************************/

		StringBuffer append_data = new StringBuffer();
		String ACM_Security_temp = "";
		
		Writer w=null;
		PrintWriter pw=null;		
		String logfilename = "";
		String logfileFolderPath="";
		
		try 
		{
			int size = resp.idvalarr.size();
			ArrayList<String> idvalarr = new ArrayList<String>();
			ArrayList<String> ddnamevalarr = new ArrayList<String>();
			for (int i = 0; i < size; i++) 
			{
				// if (debug) {
				listener.getLogger().println("ID number of the job spool files= " + resp.idvalarr.get(i));
				// }
				idvalarr.add(resp.idvalarr.get(i));
				ddnamevalarr.add(resp.ddnamevalarr.get(i));
			}
			// if (debug) {
			listener.getLogger().println("Before Job Log retrieval...");
			// }
			
			// issue requests per # of spool files
			for (int i = 0; i < size; i++) 
				{
				url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs/" + jobname + "/" + jobid + "/files"
						+ "/" + idvalarr.get(i) + "/records";

				// if (debug) {
				listener.getLogger().println("HTTPS URL path to retrieve content of spool files: " + url);
				// }

				headers = new Properties();
				headers.put("Content-Type", "plain/text");
				body = "Retrieve spool files content";

				resp = zosmf.doRequest(url, "GET", body, headers, listener);

				listener.getLogger().println(
						"Server returned response code for job spool file-" + idvalarr.get(i) + ": " + resp.status);
				String message = "Additional diagnostic response messages:\n" + resp.statAndHeaders.toString();
				// if (debug) {
				listener.getLogger().println(message);
				// }
				if (resp.ret_code == 8) {
					listener.getLogger().println(resp.resp_details);
					throw new Exception();
				}
				if (resp.status >= 200 && resp.status <= 299)
				{
					
					
					// write log file
					/*
					 * To avoid DM_DEFAULT_ENCODING error: use OutputStreamWriter instead of FileWriter
					 */
					/*
					FileWriter fw = null;
					BufferedWriter bw = null;
					*/					
					// if (debug) {

					listener.getLogger().println("Writing the Job Log to workspace");
					// }
					//logfilename = jobname + "-" + jobid + "-" +i;
					logfilename =ddnamevalarr.get(i);
					//logfileFolderPath=jobname + "-" + jobid;
					logfileFolderPath=String.valueOf(run.getNumber());
					/*
					fw = new FileWriter(workspace + File.separator + logfilename);
					bw = new BufferedWriter(fw);
					bw.write(append_data.toString());					
					File file = new File(someFilePath);					  
					*/
					File logfileFolder = new File(workspace + File.separator +logfileFolderPath+ File.separator+jobid);
					if (!logfileFolder.exists())
					{
						if (logfileFolder.mkdirs()) {
							System.out.println("directory " + logfileFolder+ " created");
						}

					}
					w = new OutputStreamWriter(new FileOutputStream(workspace + File.separator + logfileFolderPath + File.separator+jobid+File.separator+ logfilename), "UTF-8");
					pw = new PrintWriter(w);						
					String inputLine;
					BufferedReader in = null;
					/*
					 * Since the data to be processed, that is sent over the network might be very large, say more than a few hundred MB,
					 * switching to stream processing instead of StringBuffer usage, which loads all into memory 
					 * and might cause OutOfMemoryError: Java heap space exceptions
					 */						
					in = new BufferedReader(new InputStreamReader(resp.istream, "utf-8"));
					while ((inputLine = in.readLine()) != null) 
					{
						pw.println(inputLine);
						String errormsg=inspectFailureInLogs(inputLine);
						if(errormsg!=null)
						{
							listener.getLogger().println(errormsg);
							run.setResult(Result.FAILURE);							
							break;
						}

					}
					in.close();
					resp.istream.close();	
					pw.close();

					// bw.close();
					// fw.close();

					listener.getLogger().println("Spool file #"+i+" was successfully written to workspace");
					listener.getLogger().println("Job Output Path= " + workspace + File.separator + logfilename);	
					
				} else if (resp.status == 401) {
					// error_var1 = "Incorrect user ID or password, or both, or the client did not
					// authenticate to z/OSMF";
					// listener.getLogger().println(ACMConst.BMCAMA00082E);
					throw new Exception();
				} else if (resp.status == 503) {
					// listener.getLogger().println("Server error. Server is not available");
					// listener.getLogger().println(ACMConst.BMCAMA00083E);
					throw new Exception();
				} else {
					// listener.getLogger().println("Other error. Please check the response code");
					// listener.getLogger().println(ACMConst.BMCAMA00084E);
					throw new Exception();
				}
			}//end for
			// if (debug) {
			//listener.getLogger().println("Job log successfully placed in buffer...");
			// }
			for( String i: ddnamevalarr) {
				if (i.contains("CSV"))
					run.addAction(new BmcCfaAction(run, run.getNumber(), workspace.getRemote(), resp, jobid,i));
			}
		} //end of try

		
		 catch (IOException ioex) {
			listener.getLogger().println("Job log STARTS here...");
			listener.getLogger().println();
			listener.getLogger().println(append_data.toString());
			listener.getLogger().println();
			listener.getLogger().println("Retrieved job log ends here...");
			listener.getLogger().println("Job Output Path= " + workspace + File.separator + logfilename);
			// if (debug)
			listener.getLogger().println(ioex);
			// if (ioex.getMessage() != null)
			// listener.getLogger().println(ioex.getMessage());
			ioex.printStackTrace();

		}  
		catch (Exception ex) {
			listener.getLogger().println("Error while retrieving the job log");
			// if (debug)
			listener.getLogger().println(ex);
			// if (ex.getMessage() != null)
			// listener.getLogger().println(ex.getMessage());
			ex.printStackTrace();
			run.setResult(Result.FAILURE);	
		}
		
		// if (append_data.toString().contains("BMC4568") ||
		// append_data.toString().contains("BMC56388"))
		// listener.getLogger().println("ACM Security return codes: " +
		// ACM_Security_temp);
		/*
		 * if(jobCompletion.equals("bad")){ ACMGetCredential acmgetcr = new
		 * ACMGetCredential(debug); acmgetcr.delIntFile(listener, intFileName); //throw
		 * new AbortException("Job Return Code= " + jobRC); throw new
		 * AbortException(ACMConst.BMCAMA00076E + jobRC); }
		 */
		// }



	}// end of perform

	@Extension
	/*
	 * This class is basically used for handling configuration of your Plugin. When
	 * you click on �Configure� link on Jenkins it basically calls this method and
	 * loads the configured data
	 */
	// To make for a more attractive and mnemonic usage style, you can depend on
	// org.jenkins-ci.plugins:structs and add a @Symbol to your Descriptor, uniquely
	// identifying it among extensions of its kind
	@Symbol("BMC DevOps for CFA Plugin")
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private int lastEditorId = 0;		
		/*
		  @Override
	        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
	            req.bindJSON(this, json);
	            save();
	            return true;
	        }
		*/
		/**
		 * The default constructor.
		 */
		
		public DescriptorImpl() {
			super(BmcCfaBuilder.class);
			load();
			

		}

		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return "BMC AMI DevOps for Application Checkpoint Analysis";

		}

		@JavaScriptMethod
		public synchronized String createUniqueId() {
			
			return String.valueOf(lastEditorId++);

		}
		
		/* For debugging purposes*/
		/*
		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			// TODO Auto-generated method stub
			System.out.println("json:\n"+formData);
			
			return super.newInstance(req, formData);
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			// TODO Auto-generated method stub
			System.out.println("json:\n"+json);
			return super.configure(req, json);
		}
		*/
		
/*
		@JavaScriptMethod
		public synchronized boolean reloadJobConfig() throws Exception {

			// System.out.println(jn);
			// jn.doConfigSubmit(Stapler.getCurrentRequest(),Stapler.getCurrentResponse());
			// jn.reload();

			// XmlPage page = getRssAllAtomPage();
			// NodeList allLinks = page.getXmlDocument().getElementsByTagName("link");
			// System.out.println(allLinks);

			return true;

		}
*/
		/*
		 * private XmlPage getRssAllAtomPage() throws Exception { return (XmlPage)
		 * createWebClient().getPage(getConfigPage()); // descriptor. //
		 * submit(createWebClient().getPage(view, //
		 * "configure").getFormByName("viewConfig")); }
		 * 
		 * public WebClient createWebClient() { return new WebClient(); }
		 */
		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return super.getId();
		}

		//Form validation
		@POST
		public FormValidation doCheckBmcRecon(@QueryParameter boolean value,@QueryParameter boolean bmcStartInterval, @QueryParameter boolean bmcStopInterval) {
			
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			
			if(value==true)
			{
				if(bmcStartInterval==false &&  bmcStopInterval==false ) 
					result=FormValidation.warning("INTERVAL START and STOP keywords are required for RECON");
				else if(bmcStopInterval==false ) 
					result=FormValidation.warning("INTERVAL STOP keyword is required for RECON");
				else if(bmcStartInterval==false ) 
					result=FormValidation.warning("INTERVAL START keyword is required for RECON");
				
			}				
			
			return result;
		}
		
		@POST
		public FormValidation doCheckBmcImsid(@QueryParameter boolean value,@QueryParameter boolean bmcSlds) {
			
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			
			if(value==true && bmcSlds==true)				
				result=FormValidation.warning("IMSID and SLDS are mutually exclusive");			
			
			return result;
		}

		@POST
		public FormValidation doCheckJobname(@QueryParameter String value) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value.length()>8)
				result=FormValidation.warning("JOBNAME should be 1-8 characters");

			return result;
		}


		@POST
		public FormValidation doCheckBmcSlds(@QueryParameter boolean value,@QueryParameter boolean bmcImsid) {
			
			FormValidation result = null;

			if(value==true && bmcImsid)				
				result=FormValidation.warning("SLDS and IMSID are mutually exclusive");			
			
			return result;
		}



		
		@POST	
		public FormValidation doCheckPort(@QueryParameter String value) 
		{					
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			
			String tempValue = Util.fixNull(value).trim();
			if (tempValue.isEmpty())			
				result = FormValidation.error("z/OSMF Port number is required!");				
			
			return result;
		}
		
		@POST
		public FormValidation doCheckUser(@QueryParameter String value) 
		{					
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			
			String tempValue = Util.fixNull(value).trim();
			if (tempValue.isEmpty())			
				result = FormValidation.error("User name is required!");				
			
			return result;
		}
		
		@POST
		public FormValidation doCheckPswd(@QueryParameter String value) 
		{					
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			
			String tempValue = Util.fixNull(value).trim();
			if (tempValue.isEmpty())			
				result = FormValidation.error("Password is required!");				
			
			return result;
		}
		
		@POST
		public FormValidation doCheckLib(@QueryParameter String value) 
		{					
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			
			String tempValue = Util.fixNull(value).trim();
			if (tempValue.isEmpty())			
				result = FormValidation.error("STEPLIB is required!");				
			
			return result;
		}
		
		//doFill{fieldname}Items		
		
		public ListBoxModel doFillPrilogItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "SLDS", "SLDS" );		    		
		    items.add( "DLI", "DLI" );
		    items.add( "BOTH", "BOTH" );
		    return items;
		}  
	
		public ListBoxModel doFillThreshItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "MIN", "MIN" );		    		
		    items.add( "SEC", "SEC" );
		    return items;
		}  
		
		public ListBoxModel doFillSortbyItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "FREQUENCY", "FREQ" );		    		
		    items.add( "START TIME", "STIME" );
		    return items;
		}  
		
	
	}

}
