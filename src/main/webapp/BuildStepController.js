 	/* ************************************************************************************************************	*/
    /*																											   	*/
	/* The script is being triggered by the following cases:  													   	*/	
	/* 																												*/
	/* 1. New Item																									*/
	/* 2. Configure - the script runs several times, per each BMC build step that is included in the configuration  */
	/* 3. Add build step (BMC CFA plugin)																				*/
	/* 																												*/
	/* It always gets the last stepid and not the current, thus a hidden entry named "visited" is being used		*/
	/* in order to determine the current build step.																*/
	/*																											   	*/
	/* ************************************************************************************************************	*/
	/* Functionality:																								*/
	/* ************************************************************************************************************	*/ 
	/*																												*/
	/* Handle mutually exclusive fields.	            															*/
	/*																												*/
	/* Update the generated JCL according to the selected fields.													*/
	/*																												*/
	/* It detects elements by ID, which are suffixed with the current build step id. 								*/
	/* For repeatable blocks the elements are identified by the current chunk and depends on the HTML structure.	*/
	/*  																											   	*/
	/* ************************************************************************************************************	*/
	/* Notes:																										*/
	/* ************************************************************************************************************	*/  
	/*																											   	*/
	/* document.getElementsByName can not be used for detecting elements, because multiple build steps are allowed. */	
	/*																												*/
	/* ************************************************************************************************************	*/
    
		
    var createJclJobcardPart="${JOB_CARD}\n" +
					 "//DLPYLIST EXEC PGM=DLPYLIST,REGION=4M\n";
    var JclLoadlibsPart="";	 	
	
	var analyze ="";	
	var actOlds="";
	var DB2LOG="";
	var DB2BSDS="";
	var limit="";
	var skip="";	
	var maxlogs="";
	var prilog="";
	var timezone="";	
	
	var startInterval="";
	var stopInterval="";
	
	var appcheckExcJobs="";
	var appcheckIncJobs="";
	var appcheckIncPsbs="";
	var appcheckExcPsbs="";
	var appcheckIncPlans="";
	var appcheckExcPlans="";
	
	var appcheckAll="";
	var appcheckCsvrpt="";
	var appcheckSortby="";	
	var appcheckThresh="";
	var appcheckLsec="";
	var appcheckFreq="";
	
	var appcheck="";
	var fullreport="";
    var loadLibs=new Array();
    var SLDS=new Array();   
    var DLI=new Array();
    var reconSet=new Array();
    var JOBNAMEs=new Array();
	var IMSIDs=new Array();	
   	
    var updateOptionalBlockCounter=0;
    var totalNumOfoperands=29;    //should match the total number of optional keywords

    var sectionClassName;
    /* ************************************************************************ */
    /*			 																*/
	/* Override onAdd in														*/
	/* jenkins/core/src/main/resources/lib/form/repeatable/repeatable.js		*/
	/*			 																*/
	/* ************************************************************************ */   	 
   	Behaviour.specify("INPUT.repeatable-add", 'repeatable', 0, function(e) 
   		{
   			makeButton(e,function(e) 
   			{        			
   				repeatableSupport.onAdd(e.target);// original code
   				addOrRemoveChunk(e);// new code
   			});
   			e = null; // avoid memory leak
    	});
    	
   	/* ************************************************************************ */
   	/*			 																*/
	/* Override onDelete in														*/
	/* jenkins/core/src/main/resources/lib/form/repeatable/repeatable.js		*/
   	/*			 																*/
	/* ************************************************************************ */  
  /* 	Behaviour.specify("INPUT.repeatable-delete", 'repeatable', 0, function(e)
   		{
   			makeButton(e,function(e)
   			{        			
   				repeatableSupport.onDelete(e.target);// original code
   				addOrRemoveChunk(e);// new code
        	});
   			e = null; // avoid memory leak
    	});
   	*/
   		Behaviour.specify(
          "BUTTON.repeatable-delete, INPUT.repeatable-delete",
          "repeatable",
          1,
          function (e) {
            e.addEventListener("click", function () {
              repeatableSupport.onDelete(e);
              addOrRemoveChunk(e);// new code
            });
          }
        );
    // validate required form values
    //Behaviour.specify("INPUT.required", "input-required", 1000, function(e) { registerRegexpValidator(e,/./,"Field is required"); });
   	   	
   	/* **************************************************************************************************************** */
   	/*			 																										*/
   	/* Override updateOptionalBlock 																					*/
   	/* jenkins/war/src/main/webapp/scripts/hudson-behavior.js															*/
   	/*			 																										*/
   	/* c - checkbox on the optionalBlock																				*/
   	/*																													*/
   	/* The function is being invoked per each optionalBlock when the page is loaded.									*/
   	/* At this point we're not interested in populating the JCL, but reading it from config.xml instead.				*/
   	/* Thus, we keep a counter that counts the number of times it was invoked,											*/
   	/* and when the counter reaches the total number of optionalBlocks- 												*/
   	/* this is an indication that the function was triggered by the user, and now we can update the JCL.				*/
   	/* When a new build is added the counter is being reset to 0, and the JCL will be populated with the default value. */ 
   	/*				
   	/* <!--div class="rowSet-container"--!>                                                                             */
      	/*  <div class="jenkins-section">   <ZENG-260139> since Jenkins version >2.332.3 class name was changed to "jenkins-section"
   	/*			|			                                                                                            */
   	/*			⛛	                                                                                                    */
   	/*		<div class="optionalBlock-container">                                                                       */
   	/*				|                                                                                                   */
   	/*				⛛                                                                                                  */
      	/*			<input type="checkbox">																				*/
   	/* **************************************************************************************************************** */
   	
   	
   	var origUpdateOptionalBlock = window.updateOptionalBlock;
   	window.updateOptionalBlock = function(c) {

   	 // find the start TR
      var s = c;
      while (!s.classList.contains("optional-block-start")) {
        s = s.parentNode;
      }

      // find the beginning of the rowvg
      var vg = s;
      while (!vg.classList.contains("rowvg-start")) {
        vg = vg.nextElementSibling;
      }

      var checked = xor(c.checked, c.classList.contains("negative"));

      vg.rowVisibilityGroup.makeInnerVisible(checked);

      if (c.name == "hudson-tools-InstallSourceProperty") {
        // Hack to hide tool home when "Install automatically" is checked.
        var homeField = findPreviousFormItem(c, "home");
        if (homeField != null && homeField.value == "") {
          const formItem = homeField.closest(".jenkins-form-item");
          if (formItem != null) {
            formItem.style.display = c.checked ? "none" : "";
            layoutUpdateCallback.call();
          }
        }
      }
   		//new code applies only to optionalBlocks that were added by BMC plugin   	    
   		if(c.name.substring(0,3)=="bmc")
   		{  		
   			if(updateOptionalBlockCounter>=totalNumOfoperands)
   			{
   							
   				refreshAll(c,null);		
   			}
   			updateOptionalBlockCounter++;   		
   		} 	
   	}
	
   	function oninputMaxlogsAbend(input,stepid)
   	{
   		if (input.value!='')
   			document.getElementById('maxlogsRc-'+stepid).disabled=true;
   		else
   			document.getElementById('maxlogsRc-'+stepid).disabled=false;
   		chk=input.parentNode.parentNode.parentNode.parentNode.firstChild.firstChild.firstChild;
   		
   		refreshAll(chk,null);		
   	}
   	
	function oninputMaxlogsRc(input,stepid)
   	{
   		if (input.value!='')
   			document.getElementById('maxlogsAbend-'+stepid).disabled=true;
   		else
   			document.getElementById('maxlogsAbend-'+stepid).disabled=false;
   		chk=input.parentNode.parentNode.parentNode.parentNode.firstChild.firstChild.firstChild;
   		
   		refreshAll(chk,null);		
   	}
   	/*
	 * Allowing running code onLoad of the page - triggered by "Configure"
	 */	
	// creator=function(){
	// alert("creator");
	// Behaviour.apply();
	// onBuildInit(); }
	// Behaviour.addLoadEvent(creator);
	 	 
  
   	
   	/*
	 * Because a new script is being activated per each build step, we lose the data stored in the variables, thus a refresh is required.	
	 */
   	
   	function refreshAll(check,event)
   	{
   			prev=findParentDiv(check);	   		    
			prev=findParentRowSetContainer(prev);
			connectionRowSetContainer=findConRowSetContainer(prev);
			curr_stepid=isolateStepidFromPswd(connectionRowSetContainer); 
			section=connectionRowSetContainer;
			section=section.nextSibling;

			while(section!=null)
			{
				if (sectionClassName=="jenkins-section")
				    sectionTitle=section.firstChild.innerText;
				else if(sectionClassName=="rowSet-container")
				    sectionTitle=section.firstChild.firstChild.firstChild.innerHTML;
				
				if(sectionTitle=="ANALYZE Keywords" || sectionTitle== "INTERVAL Keywords" || sectionTitle== "REPORTS Keywords" || sectionTitle=="BMC AMI DevOps for Application Checkpoint Analysis Options")
					for(optBlck of section.children)
		   			{
		   				if(optBlck.className.includes("optionalBlock-container"))
		   				{
		   				    if (sectionClassName=="rowSet-container")
		   				        cbox=optBlck.firstChild.firstChild.firstChild;
		   				    else if (sectionClassName=="jenkins-section")
		   					    cbox=isolateCboxFromSpan(optBlck);

		   					toggleOptionalOperands(cbox,curr_stepid,event);

		   					if(cbox.getAttribute("name")=="bmcAppcheck")
		   					{
		   						for( appcheckOpt of optBlck.children[3].children)
		   						{
		   						    if (sectionClassName=="rowSet-container")
   		   						        appcbox=appcheckOpt.firstChild.firstChild.firstChild;
   		   						     else if (sectionClassName=="jenkins-section")
                                        appcbox=isolateCboxFromSpan(appcheckOpt);
   	   							    toggleOptionalOperands(appcbox,curr_stepid,event);
		   						}
		   					
		   					}
		   				}
		   			}
				if(sectionTitle=="REPORTS Keywords" )
					break;

    			section=section.nextSibling;
			}//end while   	
			populateJcl(curr_stepid);	  		
   	}

   	function isolateCboxFromSpan(optBlck)
   	{
   	    xmlString=optBlck.firstChild.firstChild.firstChild.innerHTML;
        tmpArr=xmlString.split("id=\"");
        arr2=tmpArr[1].split("\"");
        id=arr2[0];
        //cbox = (new DOMParser().parseFromString(xmlString, "text/xml")).firstChild;
        return cbox = document.getElementById(id);
   	}
   	/**
   	 * The function calculates load libraries on a single build step.
   	 * @param container - Load library container
   	 * @returns
   	 */
   	function refreshLoadLibs(container)
   	{  		
   		// Reset load libraries
   		i=0;
  		while (loadLibs.length > 0)
				loadLibs.pop();
  		// Count the number of load libs in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{	
  				if(i==0)		  									
  					loadLibs.push("//STEPLIB   DD DISP=SHR,DSN=${CFA_LOAD0}\n");
  				else
  					loadLibs.push("//          DD DISP=SHR,DSN=${CFA_LOAD"+i+"}\n");	
  					
				i++;		 
			} 	
   	}
   	//----------------------------------------------
   	/**
   	 * 
   	 * @param container -  container
   	 * @returns
   	 */
   	function refreshSLDS(container)
   	{  		
   		// Reset load libraries
   		i=0;
  		while (SLDS.length > 0)
				SLDS.pop();
  		// Count the number of load libs in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{  					  									
  				singleSLDS="   SLDS=(${SLDS_NAME"+i+"},${SLDS_VER"+i+"},${SLDS_UNIT"+i+"},${SLDS_VOLSER"+i+"})\n";  				       	
  			    SLDS.push(singleSLDS);		
				i++;		 
			} 	
   	}
  //----------------------------------------------
	function refreshRECON(container)
   	{  		
   		// Reset recon libraries
   		i=0;
  		while (reconSet.length > 0)
  			reconSet.pop();
  		// Count the number of RECON sets in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{  	
  				singleReconSet="   RECON=(${RECON1"+i+"},${RECON2"+i+"},${RECON3"+i+"})\n";  				
  				reconSet.push(singleReconSet);
  				i++;
			} 	
   	}
	//----------------------------------------------
	function refreshDLI(container)
   	{  		
   		// Reset dli libraries
   		i=0;
  		while (DLI.length > 0)
			DLI.pop();
  		// Count the number of dli libs in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{  					  									
  				singleDLI="   DLILOG=(${DLI_NAME"+i+"},${DLI_VERSION"+i+"},${DLI_UNIT"+i+"},${DLI_VOLSER"+i+"})\n";  					
  			    DLI.push(singleDLI);		
				i++;		 
			} 	
   	}
	//----------------------------------------------
   	function refreshIMSIDs(container)
   	{  		
   		// Reset IMSIDs 
   		i=0;
   		while (IMSIDs.length > 0)
   			IMSIDs.pop();   		
  		// Count the number of IMSIDs in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{  					  									
  				IMSIDs.push('${IMSID'+i+'}');
				i++;		 
			}   		
   	}
   	//----------------------------------------------
   	function refreshJobnames(container)
   	{  		
   		// Reset load libraries
   		i=0;  		
   		while (JOBNAMEs.length > 0)
   			JOBNAMEs.pop();     		
  		// Count the number of jobs in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{  	
  				JOBNAMEs.push('${JOBNAME'+i+'}');
				i++;
			}  	
   	}
   	//----------------------------------------------
   	function findAnalyzeRowSetContainer(but)
   	{
   		previous=but;
  		while(previous.tagName!="DIV")
  			previous=previous.parentNode;
  		while(previous.tagName=="DIV")
		{	
			previous=previous.parentNode;
			if(previous.className.includes("rowSet-container"))
			{
			    sectionClassName="rowSet-container";
					break;
			}
			else if(previous.className.includes("jenkins-section"))
			{
                sectionClassName="jenkins-section";
            		break;
            }
			
		}
  		return previous;
   		
   	}
   	//----------------------------------------------
   	function findParentDiv(el)
   	{
   		previous=el;
  		while(previous.tagName!="DIV")
  			previous=previous.parentNode;
  		return previous;
   	}
   	//----------------------------------------------
	function findRepeatContFromAddDelButton(but)
	{			
		previous=but;
  		while(previous.tagName!="DIV")
  			previous=previous.parentNode;
  		
  		if(previous.className=="repeated-container")
  			return previous;
  		else
  		{  			
	  		while(previous.tagName=="DIV")
			{	
				previous=previous.parentNode;
				if(previous.className=="repeated-container")
						break;
				
			}
  		}
  		return previous;
	}
	//----------------------------------------------
	function findParentRowSetContainer(el)
	{
		while(el.tagName=="DIV")   					
		{
 			el=el.parentNode;
 			if(el.className.includes("rowSet-container") )
 			{
 			    sectionClassName="rowSet-container";
 				break;
 			}
 			else if(el.className.includes("jenkins-section"))
            {
                sectionClassName="jenkins-section";
            	break;
            }
		}
		return el;
	}
	//----------------------------------------------
	function findConRowSetContainer(cont)
	{
	    if(sectionClassName=="jenkins-section")
	        sectionTitle=cont.firstChild.innerHTML;
	    else if(sectionClassName=="rowSet-container")
	        sectionTitle=cont.firstChild.firstChild.firstChild.innerHTML;
		while(sectionTitle!=="Connection Options")
		{
			cont=cont.previousSibling;
			if(sectionClassName=="jenkins-section")
            	sectionTitle=cont.firstChild.innerText;
            else if(sectionClassName=="rowSet-container")
                sectionTitle=cont.firstChild.firstChild.firstChild.innerHTML;
		}
		connContainer=cont;
		return connContainer;
	}
	//----------------------------------------------
	function isolateStepidFromPswd(connContainer)
	{	
		
		curr_stepid=null;
		// id="pswd-3"	 
		for (var inp of connContainer.getElementsByTagName("input"))	  			
			if(inp.id.includes("pswd"))
			{
				curr_stepid=inp.id.substring(5);
				break;
			}
		return curr_stepid;
	}
		
	/*
	 * Function that is being triggered by Add/Delete repeatable objects and refreshes all repeatable objects
	 */	
	function addOrRemoveChunk(event)
  	{		
		c=null;

		if(event.outerHTML!=null)
            eventButton=event;
        else if(event.target!==null)
           eventButton=event.target;
		
		//The STEPLIB section doesn't contain an optionalBlock, so provide any arbitrary checkbox 
		if(event.target.innerHTML.includes("library"))
		{			
			el=eventButton;
			while(el.tagName!="DIV")
				el=el.parentNode;
			while(el.tagName=="DIV")
			{
				el=el.parentNode;
				if(el.className.includes("rowSet-container") )
				{
				    sectionClassName="rowSet-container";
					break;
				}
				else if(el.className.includes("jenkins-section"))
				{
				    sectionClassName="jenkins-section";
                	break;
				}

			}
			c=el.nextSibling.children[1].firstChild.firstChild.firstChild; //send SLDS checkbox to refresh ALL
			
		}
		else
		{			
			temp=findOptBlockContFromInnerElement(eventButton);
			c=temp.firstChild.firstChild.firstChild.firstChild;
		}
		refreshAll(c,eventButton);
	
 		}// end of function
	 
		
	  	/* ****************************************************************************************************
	  	 * 	populateJcl 	  	
	  	 * ****************************************************************************************************/	  	
		function populateJcl(stepid)		
		{			
				JclLoadlibsPart="";//reset		
		
				for(var loadLib of loadLibs)
		     		JclLoadlibsPart+=loadLib ;				
				 
				analyze="";//reset
				for(singleSLDS of SLDS)
					analyze+=singleSLDS;				
				for(singleRecon of reconSet)
					analyze+=singleRecon;
				for(singleDLI of DLI)
					analyze+=singleDLI;
				
				tmp="";
				let j=0;
				for(singleJOBNAME of JOBNAMEs)
				{
					if(j==0)
						tmp+=singleJOBNAME;
					else
						tmp+=","+singleJOBNAME;
					j++;
				}
				if(JOBNAMEs.length>0)
					analyze+="   JOBNAME=("+tmp+")\n"	
				
				tmp="";
				j=0;
				for(singleIMSID of IMSIDs)
				{
					if(j==0)
						tmp+=singleIMSID;
					else
						tmp+=","+singleIMSID;
					j++;
				}
				if(IMSIDs.length>0)
					analyze+="   IMSID=("+tmp+")\n"				
				analyze+=DB2LOG;
				analyze+=DB2BSDS;
				analyze+=limit;
				analyze+=skip;
				analyze+=maxlogs;
				analyze+=actOlds;
				analyze+=prilog;
				analyze+=timezone;			
				
				tempAppcheck=appcheckAll+ appcheckIncJobs+appcheckExcJobs+ appcheckIncPsbs+appcheckExcPsbs+ appcheckIncPlans+appcheckExcPlans+appcheckCsvrpt+appcheckSortby+appcheckFreq+appcheckThresh+appcheckLsec;
				tempAppcheck=tempAppcheck.trim(); //remove extra spaces to avoid syntax error
				if(tempAppcheck!='')
					appcheck="   APPCHECK=("+tempAppcheck+")\n";
				else
					appcheck="";
				
				document.getElementById('jclContent-'+stepid).value=
				    "${JOB_CARD}\n" +
				   	"//CFRMAIN   EXEC PGM=CFRMAIN,REGION=4M\n"+										
					 JclLoadlibsPart+ 
					"//SYSOUT   DD SYSOUT=* \n" +				
					"//SYSIN     DD *\n" +           
					" ANALYZE\n"+
					analyze+ 
					" INTERVAL\n" +
					startInterval+
					stopInterval+
					" REPORTS\n"   +
					appcheck+
					fullreport+
					" END\n"+
					"/*\n" +           
					"//\n";				
		}// end of function
		
		
		
				
		/* ****************************************************************************************************
	  	 * 	findSLDSContainerFromCheckbox 	  	
	  	 * ****************************************************************************************************/	
		function findSLDSContainerFromCheckbox(sldsOptionalBlock)
        {
            if (sectionClassName=="jenkins-section")
                previous=sldsOptionalBlock.parentNode.parentNode; //<div><span><cbox></span></div>
           else if(sectionClassName=="rowSet-container")
            	previous=sldsOptionalBlock.parentNode;

			while(previous.tagName=="DIV")
			{
				previous=previous.parentNode;
				if(previous.className.includes("optionalBlock-container"))	  					
					break;	  				
			}
			for(tmp of previous.children)
				if(tmp.className.includes("form-container"))
						break;
			while(!tmp.className.includes("repeated-container"))
				tmp=tmp.firstChild;				
				
			return tmp;										
		}
		/* ****************************************************************************************************
	  	 * 	findOptBlockCont 	  	
	  	 * ****************************************************************************************************/	
		function findOptBlockContFromInnerElement(el)
		{
			while(el.tagName!="DIV")
				el=el.parentNode;
			while(el.tagName=="DIV")
			{
				el=el.parentNode;
				if(el.className.includes("optionalBlock-container"))
					break;
			}
			return el;
			
		}
		/* ****************************************************************************************************
	  	 * 	toggleOptionalOperands 	  	
	  	 * ****************************************************************************************************/			
		function toggleOptionalOperands(chkbox,stepid,event)
		{			
	  			switch(chkbox.getAttribute("name"))
	  			{
	  				case "bmcSlds":
	  					toggleSlds(chkbox,event);
	  					break;
	  				case "bmcDlilog":
	  					toggleDli(chkbox,event);
	  					break;
	  				case "bmcRecon":
	  					toggleRecon(chkbox,event);
	  					break;
			  		case "bmcDb2log":
			  		    toggleDb2log(chkbox);
			  		    break;
			  		case "bmcDb2bsds":			  			
			  			toggleDb2bsds(chkbox);
			  			break;
			  		case "bmcLimit":
			  			limit=(chkbox.checked==true)? "   LIMIT=${LIMIT}\n" : "";			
			  			break;
			  		case "bmcSkip":			  			
			  			skip=(chkbox.checked==true)? "   SKIP=${SKIP}\n" : "";			  			
			  			break;
			  		case "bmcMaxlogs":
			  			if(chkbox.checked==true)
			  				if(document.getElementById('maxlogsRc-'+stepid).value!=="")
			  					maxlogs="   MAXLOGS=(${MAXLOGS},R${RC})\n";
			  				else if(document.getElementById('maxlogsAbend-'+stepid).value!=="")
			  					maxlogs="   MAXLOGS=(${MAXLOGS},${ABEND})\n";
			  				else
			  					maxlogs="   MAXLOGS=${MAXLOGS}\n";
			  			break;
			  		case "bmcTimezone":
			  			timezone=(chkbox.checked==true)? "   TIMEZONE=${TIMEZONE}\n" : "";			
			  			break;
			  		case "bmcPrilog":
			  			prilog=(chkbox.checked==true)? "   PRILOG=${PRILOG}\n" : "";			
			  			break;
			  		case "bmcJobname":
			  			toggleJobname(chkbox,event);
			  			break;
			  		case "bmcImsid":
			  			toggleImsid(chkbox,event);
			  			break;	
			  		case "bmcStartInterval":
			  			startInterval=(chkbox.checked==true)? "   START=${START}\n" : "";
			  			break;
			  		case "bmcStopInterval":
			  			stopInterval=(chkbox.checked==true)? "   STOP=${STOP}\n" : "";
			  			break;			  		
			  		case "bmcAppcheck":
			  			if(chkbox.checked==false)
			  			{
			  				//reset all appcheck keywords
			  				appcheckAll="";
			  				appcheckIncJobs="";
			  				appcheckExcJobs="";
			  				appcheckIncPsbs="";
			  				appcheckExcPsbs="";
			  				appcheckIncPlans="";
			  				appcheckExcPlans="";
			  				appcheckCsvrpt="";
			  				appcheckSortby="";
			  				appcheckFreq="";
			  				appcheckThresh="";
			  				appcheckLsec="";
			  				uncheckAppcheckSubKeywords(chkbox);
			  			}			  			   
			  			break;	
			  		case "bmcAll":
			  			toggleAll(chkbox,stepid);
			  			break;			  		
			  		case "bmcLsec":
			  			toggleLsec(chkbox);
			  			break;
			  		case "bmcJobInc":
			  			appcheckIncJobs=toggleAppcheckOptions(chkbox,"bmcJobInc",stepid);
			  			break;
			  		case "bmcJobExc":
			  			appcheckExcJobs=toggleAppcheckOptions(chkbox,"bmcJobExc",stepid);
			  			break;
			  		case "bmcPsbInc":
			  			appcheckIncPsbs=toggleAppcheckOptions(chkbox,"bmcPsbInc",stepid);
			  			break;
			  		case "bmcPsbExc":
			  			appcheckExcPsbs=toggleAppcheckOptions(chkbox,"bmcPsbExc",stepid);
			  			break;
			  		case "bmcPlanInc":
			  			appcheckIncPlans=toggleAppcheckOptions(chkbox,"bmcPlanInc",stepid);
			  			break;
			  		case "bmcPlanExc":
			  			appcheckExcPlans=toggleAppcheckOptions(chkbox,"bmcPlanExc",stepid);
			  			break;
			  		case "bmcThresh":
			  			appcheckThresh=(chkbox.checked==true)? "THRESH = ${THRESH} " : "";
			  			break;
			  		case "bmcSortby":
			  			appcheckSortby=(chkbox.checked==true)? "SORTBY = ${SORTBY} " : "";
			  			break;
			  		case "bmcCsv":
			  			toggleCsvrpt(chkbox);
			  			break;
			  		case "bmcFullreport":
			  			fullreport=(chkbox.checked==true)? "   FULLREPORT=YES\n" : "";
			  			break;
			  		case "bmcChkfreq":
			  			toggleChkfreq(chkbox,stepid);
			  			break;
			  		case "bmcActiveOlds":
			  			actOlds=(chkbox.checked==true)? "   ACTIVEOLDS=YES\n" : "";		
			  			break;			  			
			  		default:
			  		    break;
			  	}
	  			
	  			tmp=document.getElementById('pswd-'+stepid).parentNode;
	  			while(tmp.tagName=="DIV")
	  				{
	  					tmp=tmp.parentNode;
	  					if(tmp.className.includes("rowSet-container") )
	  					{
	  					    sectionClassName="rowSet-container";
	  						break;
	  					}
	  					else if(tmp.className.includes("jenkins-section"))
	  					{
	  					    sectionClassName="jenkins-section";
   	  						break;
	  					}
	  				}
	  			tmp=tmp.nextSibling.children[1].children[1].firstChild;
	  			refreshLoadLibs(tmp);
	  			if(event!=null)
	  			{
	  				//if(event.target.title=="Delete library")
	  				if(event.className.includes('repeatable-delete'))
	  					loadLibs.pop();
	  			}	  			
		}	
		//------------------------------------
		function uncheckAppcheckSubKeywords(chkbox)
		{
			
			for(el of findOptBlockContFromInnerElement(chkbox).children)
			{
				if (el.className.includes("form-container"))
				{
					for(opt of el.children)
					{
						  opt.getElementsByTagName("INPUT")[0].checked=false;	
					}
					break;
				}
					
			}
		}
		//------------------------------------
		function toggleChkfreq(chkbox,stepid)
		{
			if(chkbox.checked==true)
			{
				if(document.getElementById('less-'+stepid).checked==true)
					appcheckFreq= "CHKFREQ < ${CHKFREQ} ";
				else if(document.getElementById('lessEqual-'+stepid).checked==true)
					appcheckFreq= "CHKFREQ <= ${CHKFREQ} ";
				else if(document.getElementById('great-'+stepid).checked==true)
					appcheckFreq= "CHKFREQ > ${CHKFREQ} ";
				else if(document.getElementById('greatEqu-'+stepid).checked==true)
					appcheckFreq= "CHKFREQ >= ${CHKFREQ} ";
				else if(document.getElementById('equ-'+stepid).checked==true)
					appcheckFreq= "CHKFREQ = ${CHKFREQ} ";
				
			}
			else
				appcheckFreq="";
		}
		//---------INVOKED VIA RADIO BUTTONS--
		function toggleChkfreqRadio(rad,stepid)
		{
			tmp=findOptBlockContFromInnerElement(rad).getElementsByTagName("INPUT")[0];
			if(tmp.name=="bmcChkfreq" && tmp.checked==true)			
			{
				if(rad.value=="Less")
					appcheckFreq= "CHKFREQ < ${CHKFREQ} ";
				else if(rad.value=="LessEqual")
					appcheckFreq= "CHKFREQ <= ${CHKFREQ} ";
				else if(rad.value=="Greater")
					appcheckFreq= "CHKFREQ > ${CHKFREQ} ";
				else if(rad.value=="GreaterEqual")
					appcheckFreq= "CHKFREQ >= ${CHKFREQ} ";
				else if(rad.value=="Equ")
					appcheckFreq= "CHKFREQ = ${CHKFREQ} ";
			}
			else
				appcheckFreq="";
			
			populateJcl(stepid);
		}
		//------------------------------------		
		function toggleAll(chkbox,stepid)
		{
			if(chkbox.checked==true)
			{
				appcheckAll="ALL";	
				
			}
			else
			{
				appcheckAll="";				
			}
			
			//mutually exclusive
			allMutExcArray=new Array("bmcJobInc","bmcJobExc","bmcPsbInc","bmcPsbExc","bmcChkfreq","bmcThresh","bmcLsec","bmcPlanInc","bmcPlanExc","bmcSortby","bmcCsv");
			
			appCheckmutExc(chkbox,allMutExcArray,stepid);
		}				
	
		//------------------------------------
		function appCheckmutExc(chkbox,mutual,stepid)
		{
			form=findOptBlockContFromInnerElement(chkbox).parentNode;
			if(form.className.includes("form-container"))
				for(el of form.children)
				{
					chk=el.getElementsByTagName("INPUT")[0];
					for(mut of mutual)
					{
						if(chk.name==mut)
						{
							if(chkbox.checked==true)
							{
								chk.disabled=true; //disable the mutual exclusive checkbox								
								chk.checked=false; //uncheck the mutual exclusive checkbox	
								
								switch(mut)
								{
								case "bmcJobInc":
									appcheckIncJobs=""; //clear the value from JCL
									el=document.getElementById('jobInclude-'+stepid);
									el.value=""; //clear the value from textbox 
									el.parentNode.style.display="none"; //hide textbox
									break;
								case "bmcJobExc":
									appcheckExcJobs="";
									el=document.getElementById('jobExclude-'+stepid);
									el.value="";
									el.parentNode.style.display="none";
									break;
								case "bmcPsbInc":
									appcheckIncPsbs="";
									el=document.getElementById('psbInclude-'+stepid);
									el.value="";
									el.parentNode.style.display="none";
									break;
								case "bmcPsbExc":
									appcheckExcPsbs="";
									el=document.getElementById('psbExclude-'+stepid);
									el.value="";
									el.parentNode.style.display="none";
									break;
								case "bmcChkfreq":
									appcheckFreq="";
									el=document.getElementById('chkFreq-'+stepid);
									el.value="";
									el.parentNode.style.display="none";
									break;
								case "bmcThresh":
									appcheckThresh="";
									el=document.getElementById('thresh-'+stepid);									
									el.parentNode.parentNode.parentNode.style.display="none";//hide select
									break;
								case "bmcLsec":
									appcheckLsec="";
									break;
								case "bmcPlanInc":
									appcheckIncPlans=""; //clear the value from JCL
									el=document.getElementById('planInc-'+stepid);
									el.value=""; //clear the value from textbox 
									el.parentNode.style.display="none"; //hide textbox
									break;
								case "bmcPlanExc":
									appcheckExcPlans=""; //clear the value from JCL
									el=document.getElementById('planExc-'+stepid);
									el.value=""; //clear the value from textbox 
									el.parentNode.style.display="none"; //hide textbox
									break;
								case "bmcSortby":
									appcheckSortby=""; //clear the value from JCL
									el=document.getElementById('sortby-'+stepid);									
									el.parentNode.parentNode.parentNode.style.display="none"; //hide select
									break;
								case "bmcCsv":
									appcheckCsvrpt="";
									break;
								default:
									break;										
									
								}
							}
							else if(!chkbox.disabled)
								chk.disabled=false;
						}
					}
				}
		}
		//------------------------------------
		function toggleLsec(chkbox)
		{
			if(chkbox.checked==true)
				appcheckLsec="LSEC = YES ";
			else
				appcheckLsec="";
			
		}
	
		//------------------------------------
		function toggleCsvrpt(radio,stepid)
		{
			if(radio.checked==true)
				appcheckCsvrpt="CSVRPT = YES ";
			else
				appcheckCsvrpt="";
			
		}
		//------------------------------------
		function toggleAppcheckOptions(chkbox,name,stepid)
		{				
					tmp="";
					switch(name)
					{
					case "bmcPsbInc":
						tmp=(chkbox.checked==true)? "PSB = ${INCLUDE_PSBS} " : "";		
						psbIncMutExcArray=new Array("bmcPsbExc");
						appCheckmutExc(chkbox,psbIncMutExcArray,stepid);
						break;
					case "bmcPsbExc":
						tmp=(chkbox.checked==true)? "PSB NOT= ${EXCLUDE_PSBS} " : "";
						psbExcMutExcArray=new Array("bmcPsbInc");
						appCheckmutExc(chkbox,psbExcMutExcArray,stepid);
						break;
					case "bmcJobInc":
						tmp=(chkbox.checked==true)? "JOB = ${INCLUDE_JOBS} " : "";		
						jobIncMutExcArray=new Array("bmcJobExc");
						appCheckmutExc(chkbox,jobIncMutExcArray,stepid);
						break;
					case "bmcJobExc":
						tmp=(chkbox.checked==true)? "JOB NOT= ${EXCLUDE_JOBS} ": "";
						jobExcMutExcArray=new Array("bmcJobInc");
						appCheckmutExc(chkbox,jobExcMutExcArray,stepid);
						break;
					case "bmcPlanInc":
						tmp=(chkbox.checked==true)? "PLAN = ${INCLUDE_PLANS} " : "";	
						planIncMutExcArray=new Array("bmcPlanExc");
						appCheckmutExc(chkbox,planIncMutExcArray,stepid);
						break;
					case "bmcPlanExc":
						tmp=(chkbox.checked==true)? "PLAN NOT= ${EXCLUDE_PLANS} " : "";
						planExcMutExcArray=new Array("bmcPlanInc");
						appCheckmutExc(chkbox,planExcMutExcArray,stepid);
						break;
					
					default:
						break;
					}
					return tmp;
		}
		/* ********************************************************************************************** */
		function toggleSlds(chkbox,event)
		{
			if(chkbox.checked==true)	
			{
				refreshSLDS(findSLDSContainerFromCheckbox(chkbox));
				if(event!=null)
					//if(event.target.title=="Delete SLDS")
					if(event.className.includes('repeatable-delete') && event.parentNode.parentNode.parentNode.getAttribute("name").includes('builder.cfaSldsLibs'))
						SLDS.pop();
			}
			else
				while (SLDS.length > 0)
					SLDS.pop();
		}
		/* ********************************************************************************************** */
		function toggleRecon(chkbox,event)
		{
			if(chkbox.checked==true)	
			{
				refreshRECON(findSLDSContainerFromCheckbox(chkbox));
				if(event!=null)
					//if(event.target.title=="Delete RECON SET")
					if(event.className.includes('repeatable-delete') && event.parentNode.parentNode.parentNode.getAttribute("name").includes('cfaReconSets'))
						reconSet.pop();
			}
			else
				while (reconSet.length > 0)
					reconSet.pop();
		}
		/* ********************************************************************************************** */
		function toggleDli(chkbox,event)
		{
			if(chkbox.checked==true)
			{
				refreshDLI(findSLDSContainerFromCheckbox(chkbox));
				if(event!=null)
					//if(event.target.title=="Delete DLILOG")
					if(event.className.includes('repeatable-delete') && event.parentNode.parentNode.parentNode.getAttribute("name").includes('builder.cfaDliLibs'))
						DLI.pop();
			}
			else
				while (DLI.length > 0)
					DLI.pop();
		}
		/* ********************************************************************************************** */
		function toggleImsid(chkbox,event)
		{
			if(chkbox.checked==true)
			{
				refreshIMSIDs(findSLDSContainerFromCheckbox(chkbox));
				if(event!=null)
					//if(event.target.title=="Delete IMSID")
					if(event.className.includes('repeatable-delete')&& event.parentNode.parentNode.parentNode.getAttribute("name").includes('cfaImsids'))
						IMSIDs.pop();
			}
			else				
				while (IMSIDs.length>0)
					IMSIDs.pop();
		}
		
		/* ********************************************************************************************** */
		function toggleJobname(chkbox,event)
		{
			if(chkbox.checked==true)	
			{
				refreshJobnames(findSLDSContainerFromCheckbox(chkbox));
				if(event!=null)
					//if(event.target.title=="Delete JOBNAME")
					if(event.className.includes('repeatable-delete') && event.parentNode.parentNode.parentNode.getAttribute("name").includes('cfaJobnames'))
						JOBNAMEs.pop();
			}
			else	
				while (JOBNAMEs.length>0)
					JOBNAMEs.pop();
		}
		/* ********************************************************************************************** */
		function toggleDb2log(chkbox)
		{
			if(chkbox.checked==true)					
			{
				DB2LOG= "   DB2LOG=${DB2LOG_DATA_SET}\n"
			}
			else
				DB2LOG="";
			
		}
		/* ********************************************************************************************** */
		function toggleDb2bsds(chkbox)
		{
			if(chkbox.checked==true)					
			{
				DB2BSDS ="   DB2BSDS=${DB2BSDS_DATA_SET}\n" 
			}
			else
				DB2BSDS="";
			
		}
	
	
	