package com.talend.tac.cases.commandline;

import java.util.LinkedHashMap;
import java.util.List;
import org.testng.Assert;

import com.talend.tac.base.AntAction;

public class CommandlineImpls extends CommandlineAction {
	
	String ss = "";
	String otherVerionJobs = "";
	public void commandlineInitLocalImpl(String commandResult) {
		
		String initLocalResult = this.initLocal();
		System.err.println("initLocalResult>>>"+initLocalResult);
		int commandInitLocalId = this.getCommandId(initLocalResult);
		Assert.assertTrue(initLocalResult.contains(commandResult+" "+commandInitLocalId));
		this.commandlineGetCommandStatusImpl(commandInitLocalId, WAIT_TIME);
		
	}
	
	public void commandlineGetCommandStatusImpl(int commandId, int checkTimes) {
		
		Assert.assertTrue(this.commandStatusCheck(commandId, 1000, checkTimes));
		
	}
	
	public void commandlineInitRemoteImpl(String commandResult
			, String url, String root) {
		
		String initRemoteResult = this.initRemote(url+root);
		System.err.println(url+root);
		System.err.println("initRemoteResult>>>"+initRemoteResult);
		int commandInitRemoteId = this.getCommandId(initRemoteResult);
		System.out.println(">>>>>>>>>>"+initRemoteResult);
		System.out.println(">>>>>>>>>>"+commandResult+" "+commandInitRemoteId);
		Assert.assertTrue(initRemoteResult.contains(commandResult+" "+commandInitRemoteId));
		this.commandlineGetCommandStatusImpl(commandInitRemoteId, WAIT_TIME);
		
	}	
	
	public void commandlineLogonProjectImpl(String commandResult
			, String commPro, String userName, String userPassword, String url, String root) {
		
		this.commandlineInitRemoteImpl(commandResult, url, root);
		System.err.println("commPro>>>"+commPro);
		String logonProject = this.logonProject(commPro, userName, userPassword);
		System.err.println("LogonPtoject>>>"+logonProject);
		int commandLogonProjectId = this.getCommandId(logonProject);
		Assert.assertTrue(logonProject.contains(commandResult+" "+commandLogonProjectId));
		this.commandlineGetCommandStatusImpl(commandLogonProjectId, WAIT_TIME);
		
	}	
	
	public void commandlineCreatRemoteProjectImpl(String commandResult
			, String project, String description, String userName, String userPassword
			, String url, String root, String commPro) {
		
		this.commandlineInitRemoteImpl(commandResult, url, root);
		
		String creatRemoteProjectResult = this.createProject(project, description, userName, userPassword);
		System.err.println("creatRemoteProjectResult>>>"+creatRemoteProjectResult);		
		int commandCreatRemoteProjectResultId = this.getCommandId(creatRemoteProjectResult);
		Assert.assertTrue(creatRemoteProjectResult.contains(commandResult+" "+commandCreatRemoteProjectResultId));
		this.commandlineGetCommandStatusImpl(commandCreatRemoteProjectResultId, WAIT_TIME);
		
		this.commandlineListProjectImpl(commandResult, url, root, commPro, userName, userPassword);
		
	}
	
	public void commandlineExportAllJobImpl(String commandResult
			, String exportPath, String commPro, String userName, String userPassword, String url, String root) {
        
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
		
		System.err.println(this.getAbsolutePath(exportPath));
		String exportAllJob = this.exportAllJob(this.getAbsolutePath(exportPath));
		System.err.println("exportAllJob>>>"+exportAllJob);
	
		int commandExportAllJobId = this.getCommandId(exportAllJob);
		Assert.assertTrue(exportAllJob.contains(commandResult+" "+commandExportAllJobId));
		this.commandlineGetCommandStatusImpl(commandExportAllJobId, WAIT_TIME*2);
		
		LinkedHashMap map = this.getFileNameList(this.getAbsolutePath(exportPath));
		map.containsValue("generateBigLogs.zip");
		map.containsValue("refJobByMaintRunJobRun.zip");
		map.containsValue("tjava.zip");
		map.containsValue("tjavaWithMulripleCheckPoint.zip");
		map.containsValue("tRunJob.zip");
		map.containsValue("trunjobWithCheckpoint.zip");
		properties.put("file.path", this.getAbsolutePath(exportPath)+"/generateBigLogs.zip");
		new AntAction().runTarget("File.xml", "delete", properties);	
		properties.put("file.path", this.getAbsolutePath(exportPath)+"/refJobByMaintRunJobRun.zip");
		new AntAction().runTarget("File.xml", "delete", properties);	
		properties.put("file.path", this.getAbsolutePath(exportPath)+"/tjava.zip");
		new AntAction().runTarget("File.xml", "delete", properties);	
		properties.put("file.path", this.getAbsolutePath(exportPath)+"/tjavaWithMulripleCheckPoint.zip");
		new AntAction().runTarget("File.xml", "delete", properties);	
		properties.put("file.path", this.getAbsolutePath(exportPath)+"/tRunJob.zip");
		new AntAction().runTarget("File.xml", "delete", properties);	
		properties.put("file.path", this.getAbsolutePath(exportPath)+"/trunjobWithCheckpoint.zip");
		new AntAction().runTarget("File.xml", "delete", properties);	
		System.err.println("***********testCommandlineExportAllJob finished***********");
		
	}
	
	public void commandlineHelpFiltrImpl(String helpFilterResult) {
		
		List<String> helpFiltrs = this.helpFiltr();
		System.err.println("helpFiltr>>>"+helpFiltrs);	
		
		String ss = "";
		for(int i=0; i<helpFiltrs.size(); i++) {
			
			ss = ss+helpFiltrs.get(i);
            System.err.println("i>>>>>>>>"+helpFiltrs.get(i));			
			
		}
		System.err.println("ss>>>"+ss);
		Assert.assertTrue(ss.contains(helpFilterResult));
		System.err.println("***********testCommandlineHelpFiltr finished***********");
		
	}

	public void commandlineHelpImpl(String helpResult, String helpResult1) {
		
		List<String> help = this.help();
		System.err.println("help>>>"+help);	
		
		String ss = "";
		for(int i=0; i<help.size(); i++) {
			
			ss = ss+help.get(i);
            System.err.println("i>>>>>>>>"+help.get(i));			
			
		}
		System.err.println("ss>>>"+ss);
		Assert.assertTrue(ss.contains(helpResult));
		Assert.assertTrue(ss.contains(helpResult1));
		System.err.println("***********testCommandlineHelp finished***********");
		
	}
	
    public void commandlineLogoffProjectImpl(String commandResult, String project, String user, String passwd
    		, String url, String root) {
		
		this.commandlineLogonProjectImpl(commandResult, project, user, passwd, url, root);	
		
		String logoffProjectResult = this.logOffProject();
		System.err.println("logoffProjectResult>>>"+logoffProjectResult);
		int commandLogoffProjectId = this.getCommandId(logoffProjectResult);
		Assert.assertTrue(logoffProjectResult.contains(commandResult+" "+commandLogoffProjectId));
		this.commandlineGetCommandStatusImpl(commandLogoffProjectId, WAIT_TIME);
		System.err.println("***********testCommandlineHelpFiltr finished***********");
		
	}

	public void commandlineCreateLocalProjectImpl(String commandResult
			, String project, String description, String userName, String userPassword) {
		
		this.commandlineInitLocalImpl(commandResult);
		
		String createLocalProjectResult = this.createProject(project, description, userName, userPassword);
		System.err.println("createLocalProjectResult>>>"+createLocalProjectResult);	
		int commandCreateLocalProjectResultId = this.getCommandId(createLocalProjectResult);
		Assert.assertTrue(createLocalProjectResult.contains(commandResult+" "+commandCreateLocalProjectResultId));
		this.commandlineGetCommandStatusImpl(commandCreateLocalProjectResultId,WAIT_TIME);		
		
		List<String> listCommandListProjectResult = this.listProject();
		System.err.println("listCommandResult>>>>>>>>"+listCommandListProjectResult);
		
		for(int i=0; i<listCommandListProjectResult.size(); i++) {
			
			ss = ss+listCommandListProjectResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandListProjectResult.get(i));			
			
		}
		System.err.println("ss>>>"+ss);	
		Assert.assertTrue(ss.contains(project));
	}
	
	public void commandlineImportItemsImpl(String commandResult
			, String project, String description, String userName, String userPassword
			, String url, String root, String importPath) {		
		
		
		this.commandlineLogonProjectImpl(commandResult, project, userName, userPassword, url, root);
		
		String importItemsResult = this.importItems(this.getAbsolutePath(importPath+"/imporJobs.zip"));
		
		int commandImportItemsResultId = this.getCommandId(importItemsResult);
		Assert.assertTrue(importItemsResult.contains(commandResult+" "+commandImportItemsResultId));
	    this.commandlineGetCommandStatusImpl(commandImportItemsResultId, WAIT_TIME);
		
	}

	public void commandlineExportItemsImpl(String commandResult
			, String project, String userName, String userPassword
			, String url, String root, String exportPath) {		
		
		this.commandlineLogonProjectImpl(commandResult, project, userName, userPassword, url, root);
		
		String exportItemsResult = this.exportItems(this.getAbsolutePath(exportPath));
		
		int commandExportItemsResultId = this.getCommandId(exportItemsResult);
		Assert.assertTrue(exportItemsResult.contains(commandResult+" "+commandExportItemsResultId));
	    this.commandlineGetCommandStatusImpl(commandExportItemsResultId, WAIT_TIME);
	    LinkedHashMap map = this.getFileNameList(this.getAbsolutePath(exportPath));
		map.containsValue("REFERENCEPRO");
		this.delFolder(this.getAbsolutePath(exportPath+"/REFERENCEPRO"));		
	
	}
	
	public void commandlineListCommandImpl(String commandResult, String commandResult1) {
		
		List<String> listCommandResult = this.listCommand();
		System.err.println("listCommandResult>>>>>>>>"+listCommandResult);
		String ss = "";
		for(int i=0; i<listCommandResult.size(); i++) {
			
			ss = ss+listCommandResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandResult.get(i));			
			
		}
		System.err.println("ss>>>"+ss);
		Assert.assertTrue(ss.contains(commandResult));
		Assert.assertTrue(ss.contains(commandResult1));
		
	}
	
	public void commandlineListExecutionServerImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword,
			 String listExecutionServer, String listExecutionServer1) {
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
		
		List<String> listCommandResult = this.listExecutionServer();
		System.err.println("listCommandResult>>>>>>>>"+listCommandResult);
		String ss = "";
		for(int i=0; i<listCommandResult.size(); i++) {
			
			ss = ss+listCommandResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandResult.get(i));			
			
		}
		System.err.println("ss>>>"+ss);
		Assert.assertTrue(ss.contains(listExecutionServer));
		Assert.assertTrue(ss.contains(listExecutionServer1));
	}

	public void commandlineListJobImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword) {
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String> listCommandListJobResult = this.listJob();
		System.err.println("listCommandResult>>>>>>>>"+listCommandListJobResult);
		
		for(int i=0; i<listCommandListJobResult.size(); i++) {
			
			ss = ss+listCommandListJobResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandListJobResult.get(i));			
			
		}
		System.err.println("ss>>>"+ss);
				
	}	

	public void commandlineListProjectImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword) {
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
		
		List<String> listCommandListProjectResult = this.listProject();
		System.err.println("listCommandResult>>>>>>>>"+listCommandListProjectResult);
		
		for(int i=0; i<listCommandListProjectResult.size(); i++) {
			
			ss = ss+listCommandListProjectResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandListProjectResult.get(i));			
			
		}
		System.err.println("ss>>>"+ss);	
		
	}

	public void commandlineListItemImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword) {
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
		
		List<String> listCommandListItemResult = this.listItem();
		System.err.println("listCommandResult>>>>>>>>"+listCommandListItemResult);
		
		for(int i=0; i<listCommandListItemResult.size(); i++) {
			
			ss = ss+listCommandListItemResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandListItemResult.get(i));			
			
		}
		System.err.println("ss>>>"+ss);	
		
	}

	public void commandlineChangeVersionImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword) {
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
		
		String listCommandChangeJobVersionResult = this.changeJobVersion();
		System.err.println("changeJobVersionResult>>>>>>>>"+listCommandChangeJobVersionResult);
		
		int commandChangeJobVersionId = this.getCommandId(listCommandChangeJobVersionResult);
		this.commandlineGetCommandStatusImpl(commandChangeJobVersionId , WAIT_TIME);
		
		List<String> listCommandlistOtherVersionResult = this.listOtherVersionJob();
		System.err.println("listCommandResult>>>>>>>>"+listCommandlistOtherVersionResult);
		
		for(int i=0; i<listCommandlistOtherVersionResult.size(); i++) {			
			
			otherVerionJobs = otherVerionJobs+listCommandlistOtherVersionResult.get(i);
            System.err.println("i>>>>>>>>"+listCommandlistOtherVersionResult.get(i));			
			
		}
		System.err.println("otherVerionJobs>>>"+otherVerionJobs);	
		
	}

	public void commandlineShowVersionImpl(String commandResult
			, String url, String root) {
		
		this.commandlineInitRemoteImpl(commandResult, url, root);
		
		List<String> showVerionResult = this.showVersion();
		System.err.println("changeJobVersionResult>>>>>>>>"+showVerionResult);		
	    
		String showVersionActual = "";
		for(int i=0; i<showVerionResult.size(); i++) {			
			
			showVersionActual = showVersionActual+showVerionResult.get(i);
            System.err.println("i>>>>>>>>"+showVerionResult.get(i));			
			
		}
		System.err.println("otherVerionJobs>>>"+otherVerionJobs);	
		
		Assert.assertTrue(!showVersionActual.equals(null));
		
	}

	public void commandlineExecuteJobImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword
			, String jdkPath) {
		
		
		this.commandlineListJobImpl(commandResult, url, root, commPro, userName, userPassword);		
		
		Assert.assertTrue(ss.contains("generateBigLogs"));
		Assert.assertTrue(ss.contains("refJobByMaintRunJobRun"));
		Assert.assertTrue(ss.contains("tRunJob"));
		Assert.assertTrue(ss.contains("tjavaWithMulripleCheckPoint"));
		Assert.assertTrue(ss.contains("tjava"));
		Assert.assertTrue(ss.contains("trunjobWithCheckpoint"));		
		System.err.println("ss>>>"+ss);	

		String commandExecuteJobResult = this.executeJob("tjavaWithMulripleCheckPoint", jdkPath);
		System.err.println("commandExecuteJobResult>>>>>>>>"+commandExecuteJobResult);
		int commandExecuteJobId = this.getCommandId(commandExecuteJobResult);
		Assert.assertTrue(commandExecuteJobResult.contains(commandResult+" "+commandExecuteJobId));
		this.commandlineGetCommandStatusImpl(commandExecuteJobId, WAIT_TIME);
		List<String> executeJobResult = this.getCommandStatusAllInfo(commandExecuteJobId);
		
		String executeJobResultInfo = "";
		for(int i=0; i<executeJobResult.size(); i++) {			
			
			executeJobResultInfo = executeJobResultInfo+executeJobResult.get(i);
            System.err.println("i>>>>>>>>"+executeJobResult.get(i));			
			
		}
		System.err.println("executeJobResultInfo>>>"+executeJobResultInfo);	
		
		Assert.assertTrue(executeJobResultInfo.contains("the first checkpoint"));
		Assert.assertTrue(executeJobResultInfo.contains("the second checkpoint"));
		Assert.assertTrue(executeJobResultInfo.contains("the third checkpoint"));
		Assert.assertTrue(executeJobResultInfo.contains("the fourth checkpoint"));
		
	}

	public void commandlineImportDatabaseMetadataImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword
			, String databaseMetadataPath) {		
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
        
		String importDatebaseMetadataResult = this.importDatabaseMetadata(databaseMetadataPath);		
		System.err.println("importDatebaseMetadataResult>>>>>>>>"+importDatebaseMetadataResult);
		int commandImportDatebaseMetadataId = this.getCommandId(importDatebaseMetadataResult);
		Assert.assertTrue(importDatebaseMetadataResult.contains(commandResult+" "+commandImportDatebaseMetadataId));
		this.commandlineGetCommandStatusImpl(commandImportDatebaseMetadataId, WAIT_TIME);
		
		
	}

	public void commandlineImportDelimitedMetadataImpl(String commandResult
			, String url, String root, String commPro, String userName, String userPassword
			, String delimitedMetadataPath) {		
		
		this.commandlineLogonProjectImpl(commandResult, commPro, userName, userPassword, url, root);
        
		String importDelimitedMetadataPathResult = this.importDelimitedMetadata(delimitedMetadataPath);		
		System.err.println("importDelimitedMetadataPathResult>>>>>>>>"+importDelimitedMetadataPathResult);
		int commandImportDelimitedMetadataPathId = this.getCommandId(importDelimitedMetadataPathResult);
		Assert.assertTrue(importDelimitedMetadataPathResult.contains(commandResult+" "+commandImportDelimitedMetadataPathId));
		this.commandlineGetCommandStatusImpl(commandImportDelimitedMetadataPathId, WAIT_TIME);
		
		
	}

}