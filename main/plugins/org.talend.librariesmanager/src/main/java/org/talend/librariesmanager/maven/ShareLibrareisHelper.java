// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.librariesmanager.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ISVNProviderServiceInCoreRuntime;
import org.talend.core.PluginChecker;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.designer.maven.talendlib.TalendLibsServerManager;
import org.talend.librariesmanager.i18n.Messages;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * created by Talend on 2015年7月31日 Detailled comment
 *
 */
public abstract class ShareLibrareisHelper {

    private final String TYPE_NEXUS = "nexus";

    private final String TYPE_SVN = "svn";

    public IStatus shareLibs(Job job, IProgressMonitor monitor) {
        ArtifactsDeployer deployer = new ArtifactsDeployer();
        Map<ModuleNeeded, File> filesToShare = null;
        IStatus status = Status.OK_STATUS;
        try {
            if (PluginChecker.isSVNProviderPluginLoaded()) {
                IProxyRepositoryFactory factory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
                if (!factory.isLocalConnectionProvider() && factory.getRepositoryContext() != null
                        && !factory.getRepositoryContext().isOffline()) {
                    ISVNProviderServiceInCoreRuntime service = (ISVNProviderServiceInCoreRuntime) GlobalServiceRegister
                            .getDefault().getService(ISVNProviderServiceInCoreRuntime.class);
                    if (service != null && service.isSvnLibSetupOnTAC()) {
                        setJobName(job, Messages.getString("ShareLibsJob.message", TYPE_SVN));
                        int shareLimit = 5;
                        // share to svn lib
                        int limit = shareLimit;
                        filesToShare = getFilesToShare(monitor);
                        if (filesToShare == null) {
                            return Status.CANCEL_STATUS;
                        }
                        SubMonitor mainSubMonitor = SubMonitor.convert(monitor, filesToShare.size());
                        Iterator<ModuleNeeded> iterator = filesToShare.keySet().iterator();
                        int index = 0;
                        while (iterator.hasNext()) {
                            if (monitor.isCanceled()) {
                                return Status.CANCEL_STATUS;
                            }
                            List<String> jars = new ArrayList<String>();
                            String jarName = "";
                            while (index < limit && index < filesToShare.size()) {
                                ModuleNeeded next = iterator.next();
                                File file = filesToShare.get(next);
                                String installLocation = getStorageDirectory().getAbsolutePath();
                                File target = new File(installLocation, next.getModuleName());
                                // if already eixst in lib svn , don't replace it .
                                if (!target.exists()) {
                                    FilesUtils.copyFile(file, target);
                                    jars.add(target.getAbsolutePath());
                                    deployToLocalMaven(deployer, file, next);
                                    jarName += next.getModuleName();
                                    if (index < limit - 1) {
                                        jarName += ",";
                                    }
                                }
                                index++;
                            }
                            limit += shareLimit;
                            try {
                                if (jars.size() > 0) {
                                    mainSubMonitor.setTaskName(Messages.getString("ShareLibsJob.sharingLibraries", jarName)); //$NON-NLS-1$
                                    service.deployNewJar(jars);
                                }
                                mainSubMonitor.worked(limit);
                            } catch (Exception e) {
                                ExceptionHandler.process(new Exception("Share libraries :" + jarName + " failed !", e));
                                status = new Status(IStatus.ERROR, "unknown", IStatus.ERROR, "Share libraries :" + jarName
                                        + " failed !", e);
                                continue;
                            }
                        }
                    } else {
                        // share to custom nexus
                        int searchLimit = 50;
                        setJobName(job, Messages.getString("ShareLibsJob.message", TYPE_NEXUS));
                        TalendLibsServerManager instance = TalendLibsServerManager.getInstance();
                        NexusServerBean customServer = instance.getCustomNexusServer();
                        if (customServer != null) {
                            filesToShare = getFilesToShare(monitor);
                            if (filesToShare == null) {
                                return Status.CANCEL_STATUS;
                            }
                            SubMonitor mainSubMonitor = SubMonitor.convert(monitor, filesToShare.size());
                            Iterator<ModuleNeeded> iterator = filesToShare.keySet().iterator();

                            Iterator<ModuleNeeded> searchIter = filesToShare.keySet().iterator();

                            int index = 0;
                            int limit = searchLimit;
                            int shareIndex = 0;
                            while (iterator.hasNext()) {
                                if (monitor.isCanceled()) {
                                    return Status.CANCEL_STATUS;
                                }
                                String jarsToCheck = "";
                                while (shareIndex < limit && index < limit && index < filesToShare.size()) {
                                    ModuleNeeded toCheck = searchIter.next();
                                    String jarName = toCheck.getModuleName();
                                    String artifactId = jarName;
                                    int lastIndexOf = jarName.lastIndexOf(".");
                                    if (lastIndexOf != -1) {
                                        artifactId = jarName.substring(0, lastIndexOf);
                                    }
                                    jarsToCheck += artifactId;
                                    index++;
                                    if (index < limit) {
                                        jarsToCheck += ",";
                                    }
                                }
                                shareIndex++;
                                if (shareIndex == limit) {
                                    limit += searchLimit;
                                }

                                final List<MavenArtifact> searchResults = instance.search(customServer.getServer(),
                                        customServer.getUserName(), customServer.getPassword(), customServer.getRepositoryId(),
                                        MavenConstants.DEFAULT_LIB_GROUP_ID, jarsToCheck, null);
                                ModuleNeeded next = iterator.next();
                                File file = filesToShare.get(next);
                                String pomPath = file.getParent();
                                String name = file.getName();
                                mainSubMonitor.setTaskName(Messages.getString("ShareLibsJob.sharingLibraries", name));
                                MavenArtifact artifact = MavenUrlHelper.parseMvnUrl(next.getMavenUriSnapshot());
                                if (artifact == null) {
                                    continue;
                                }
                                boolean eixst = false;
                                for (MavenArtifact remoteAtifact : searchResults) {
                                    String groupId = artifact.getGroupId();
                                    String artifactId = artifact.getArtifactId();
                                    String version = artifact.getVersion();
                                    String rGroup = remoteAtifact.getGroupId();
                                    String rArtifact = remoteAtifact.getArtifactId();
                                    String rVersion = remoteAtifact.getVersion();
                                    if (groupId != null && artifactId != null && version != null && groupId.equals(rGroup)
                                            && artifactId.equals(rArtifact) && version.equals(rVersion)) {
                                        eixst = true;
                                        break;
                                    }
                                }
                                if (eixst) {
                                    continue;
                                }
                                int indexOf = name.lastIndexOf(".");
                                if (indexOf != -1) {
                                    pomPath = pomPath + "/" + name.substring(0, indexOf) + "." + MavenConstants.PACKAGING_POM;
                                }
                                try {
                                    deployToLocalMaven(deployer, file, next);
                                    deployer.installToRemote(file, artifact, artifact.getType());
                                    File pomFile = new File(pomPath);
                                    if (pomFile.exists()) {
                                        deployer.installToRemote(pomFile, artifact, MavenConstants.PACKAGING_POM);
                                    }
                                    mainSubMonitor.worked(1);
                                } catch (Exception e) {
                                    ExceptionHandler.process(new Exception("Share libraries :" + name + " failed !", e));
                                    status = new Status(IStatus.ERROR, "unknown", IStatus.ERROR, "Share libraries :" + name
                                            + " failed !", e);
                                    continue;
                                }
                            }
                        }

                    }
                }
            }
            // for local projects and projects without setup library server on TAC
            if (filesToShare == null) {
                filesToShare = getFilesToShare(monitor);
                Iterator<ModuleNeeded> iterator = filesToShare.keySet().iterator();
                while (iterator.hasNext()) {
                    ModuleNeeded next = iterator.next();
                    File file = filesToShare.get(next);
                    deployToLocalMaven(deployer, file, next);
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
            status = new Status(IStatus.ERROR, "unknown", IStatus.ERROR, e.getMessage(), e);
        }
        return status;

    }

    private void setJobName(Job job, String jobName) {
        if (job != null) {
            job.setName(jobName);
        }
    }

    private File getStorageDirectory() {
        String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
        File storageDir = new File(librariesPath);
        return storageDir;
    }

    public abstract Map<ModuleNeeded, File> getFilesToShare(IProgressMonitor monitor);

    public abstract void deployToLocalMaven(ArtifactsDeployer deployer, File jarFile, ModuleNeeded module) throws Exception;
}
