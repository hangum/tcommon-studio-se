// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.maven.ui.setting.preference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.eclipse.core.internal.net.ProxyType;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.osgi.util.tracker.ServiceTracker;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.designer.maven.DesignerMavenPlugin;
import org.talend.designer.maven.repository.DefaultMavenRepositoryProvider;
import org.talend.designer.maven.talendlib.TalendLibsServerManager;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.ui.DesignerMavenUiPlugin;
import org.talend.login.AbstractLoginTask;
import org.talend.utils.io.FilesUtils;

/**
 * DOC ggu class global comment. Detailled comment
 * 
 * set the preference for MavenSettingsPreferencePage.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class M2eUserSettingForTalendLoginTask extends AbstractLoginTask {

    public static final String MAVEN_REPO_CONFIG = "maven.repository"; //$NON-NLS-1$

    private static final String MAVEN_SETTING_HAVE_SET = "Maven_Setting_Have_Set"; //$NON-NLS-1$

    private ServiceTracker proxyTracker;

    private IEclipsePreferences prefSetting = ConfigurationScope.INSTANCE.getNode(DesignerMavenPlugin.PLUGIN_ID);

    @Override
    public boolean isCommandlineTask() {
        return true; // also enable support for commandline, so set true.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.login.ILoginTask#getOrder()
     */
    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2015, 6, 17, 12, 0, 0);
        return gc.getTime();
    }

    /**
     * local by default
     */
    private boolean isLocalRepository() {
        String configSetting = System.getProperty(MAVEN_REPO_CONFIG, "local"); //$NON-NLS-1$
        return !"global".equalsIgnoreCase(configSetting); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.login.ILoginTask#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (monitor.isCanceled()) {
            return;
        }
        final boolean isSet = prefSetting.getBoolean(MAVEN_SETTING_HAVE_SET, false);
        final boolean isLocalRepository = isLocalRepository();

        final Path configPath = new Path(Platform.getConfigurationLocation().getURL().getPath());
        final File studioUserSettingsFile = configPath.append(
                IProjectSettingTemplateConstants.MAVEN_USER_SETTING_TEMPLATE_FILE_NAME).toFile();
        final String studioUserSettingsPath = studioUserSettingsFile.getAbsolutePath();

        try {
            checkMavenUserSetting(monitor, studioUserSettingsFile, isSet, isLocalRepository);

            final IMaven maven = MavenPlugin.getMaven();
            maven.reloadSettings();

            /*
             * FIXME, only deal with for special settings in studio.
             * 
             * If use other user setting, nothing to do.
             * 
             * Just keep the problem for user, because we should change the setting for Studio in configuration folder
             * only.
             */
            final String userSettingsFile = MavenPlugin.getMavenConfiguration().getUserSettingsFile();
            boolean defaultUserSetting = (userSettingsFile == null || MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath()
                    .equals(userSettingsFile));
            boolean isStudioUserSetting = studioUserSettingsPath.equals(userSettingsFile);

            // check the user setting when set for studio, don't existed or have no rights.
            if (isStudioUserSetting && (!studioUserSettingsFile.exists() || !studioUserSettingsFile.canRead())) {
                try {
                    // try use system default one.
                    MavenPlugin.getMavenConfiguration().setUserSettingsFile(null);
                    maven.reloadSettings(); // reload again

                    defaultUserSetting = true;
                } catch (CoreException e) {
                    //
                }
            }

            // if can't access m2 repository, and for studio setting or set default only.
            if (!enableAccessM2Repository(monitor, maven) && (isStudioUserSetting || defaultUserSetting)) {
                if (studioUserSettingsFile.exists()) {// try to use studio one directly.
                    MavenPlugin.getMavenConfiguration().setUserSettingsFile(studioUserSettingsPath);
                } else { // if not existed, try to force creating studio user setting and use it.
                    checkMavenUserSetting(monitor, studioUserSettingsFile, isSet, true);
                }
                maven.reloadSettings(); // reload again
                // get new value
                isStudioUserSetting = studioUserSettingsPath.equals(MavenPlugin.getMavenConfiguration().getUserSettingsFile());
            }

            final Settings settings = maven.getSettings();
            // studio settings
            if (isStudioUserSetting && studioUserSettingsFile.canWrite()) {
                boolean modified = false;

                // update the local repository
                if (checkLocalRepository(monitor, maven, settings, configPath, studioUserSettingsFile)) {
                    modified = true;
                }
                // update profile
                if (updateProfileSettings(monitor, maven, settings, configPath, studioUserSettingsFile)) {
                    modified = true;
                }
                if (modified) { // save changes
                    maven.writeSettings(settings, new FileOutputStream(studioUserSettingsFile));
                    // after update reload
                    maven.reloadSettings();
                }
            }

            // update the proxies
            updateProxiesPreference(monitor, maven, settings);

            // add one marker to check to sync or not.
            File repoFolder = new File(maven.getLocalRepositoryPath());
            File markerFile = new File(repoFolder, ".syncMarker"); //$NON-NLS-1$
            if (!markerFile.exists()) {
                if (!repoFolder.exists()) {
                    repoFolder.mkdirs();
                }
                markerFile.createNewFile();
                DefaultMavenRepositoryProvider.sync(repoFolder.getParentFile());
            }

            // apply the user settings to MavenResolver
            Dictionary<String, String> props = new Hashtable<String, String>();
            // get the setting file same as M2E preference in M2eUserSettingForTalendLoginTask.
            props.put("org.ops4j.pax.url.mvn.settings", studioUserSettingsFile.getPath());
            TalendLibsServerManager.getInstance().updateMavenResolver(props);

        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

    }

    private IProxyService getProxyService() {
        if (proxyTracker == null) {
            proxyTracker = new ServiceTracker(DesignerMavenUiPlugin.getDefault().getBundle().getBundleContext(),
                    IProxyService.class.getName(), null);
            proxyTracker.open();
        }
        return (IProxyService) proxyTracker.getService();
    }

    private void checkMavenUserSetting(IProgressMonitor monitor, File studioUserSettingsFile, boolean isSet, boolean force) {
        if (monitor.isCanceled()) {
            return;
        }
        try {

            if (!isSet || force) {// first time to set or force
                if (!studioUserSettingsFile.exists()) {
                    InputStream inputStream = MavenTemplateManager.getBundleTemplateStream(DesignerMavenPlugin.PLUGIN_ID,
                            IProjectSettingTemplateConstants.PATH_RESOURCES_TEMPLATES + '/'
                                    + IProjectSettingTemplateConstants.MAVEN_USER_SETTING_TEMPLATE_FILE_NAME);
                    if (inputStream != null) {
                        FilesUtils.copyFile(inputStream, studioUserSettingsFile);
                    }
                }
                if (monitor.isCanceled()) {
                    return;
                }
                // create failure?
                if (studioUserSettingsFile.exists()) {
                    String path = studioUserSettingsFile.getAbsolutePath();
                    if (!path.equals(MavenPlugin.getMavenConfiguration().getUserSettingsFile())) {
                        MavenPlugin.getMavenConfiguration().setUserSettingsFile(path);
                    }
                    if (!isSet) {
                        prefSetting.putBoolean(MAVEN_SETTING_HAVE_SET, true);
                        prefSetting.flush();
                    }
                } else { // set the default one.
                    MavenPlugin.getMavenConfiguration().setUserSettingsFile(null);
                }
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    @SuppressWarnings("restriction")
    private boolean checkLocalRepository(IProgressMonitor monitor, IMaven maven, Settings settings, Path configPath,
            File userSettingsFile) throws Exception {
        if (monitor.isCanceled()) {
            return false;
        }
        // default one
        String localRepoPath = "${user.home}/.m2/repository"; //$NON-NLS-1$
        // if local, always use config one
        if (isLocalRepository() || !enableAccessM2Repository(monitor, maven)) {
            // need change the repo setting
            IPath m2RepoPath = configPath.append(".m2/repository"); //$NON-NLS-1$
            localRepoPath = m2RepoPath.toString();
            File studioDefaultRepoFolder = m2RepoPath.toFile();
            if (!studioDefaultRepoFolder.exists()) {
                studioDefaultRepoFolder.mkdirs();
            }

        }
        // make sure the setting file can be changed.
        if (userSettingsFile.exists() && userSettingsFile.canRead() && userSettingsFile.canWrite()
                && !localRepoPath.equals(settings.getLocalRepository())) {
            // modify the setting file for "localRepository"
            settings.setLocalRepository(localRepoPath);
            maven.reloadSettings();
            // should same as MavenSettingsPreferencePage.updateSettings update index?
            try {
                MavenPlugin.getIndexManager().getWorkspaceIndex().updateIndex(true, monitor);
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }

            return true;
        }
        return false;
    }

    private boolean enableAccessM2Repository(IProgressMonitor monitor, IMaven maven) {
        File oldRepoFolder = new File(maven.getLocalRepositoryPath());
        try {
            if (!oldRepoFolder.exists()) { // TUP-3301
                oldRepoFolder.mkdirs(); // try to create the root folder first.
            }
        } catch (Throwable e) {
            // if can't create, continue.
        }
        if (!oldRepoFolder.exists() || !oldRepoFolder.canRead() || !oldRepoFolder.canWrite()) {
            return false;
        }
        return true;
    }

    private void updateProxiesPreference(IProgressMonitor monitor, IMaven maven, Settings settings) throws CoreException {
        if (monitor.isCanceled()) {
            return;
        }
        List<Proxy> proxiesList = settings.getProxies();
        if (proxiesList != null && !proxiesList.isEmpty()) {
            IProxyService proxyService = getProxyService();
            if (proxyService != null) {
                proxyService.setSystemProxiesEnabled(false); // not NATIVE_PROVIDER
                if (!proxyService.isProxiesEnabled()) { // is DIRECT_PROVIDER
                    proxyService.setProxiesEnabled(true);// try to change to ECLIPSE_PROVIDER
                }

                // set proxy
                for (Proxy p : proxiesList) {
                    if (p.isActive()) {
                        setProxy(monitor, proxyService, p);
                        break; // only set for first active proxy.
                    }
                }
            }
        }
    }

    @SuppressWarnings("restriction")
    private void setProxy(IProgressMonitor monitor, IProxyService proxyService, Proxy p) throws CoreException {
        if (monitor.isCanceled()) {
            return;
        }
        String protocol = p.getProtocol();
        if (protocol == null) {
            return;
        }
        String host = p.getHost();
        // must set host and port
        if (host == null || host.trim().length() == 0 || p.getPort() <= 0) {
            return;
        }

        IProxyData proxyData = proxyService.getProxyData(protocol.toUpperCase());
        if (proxyData == null) {
            return;
        }
        // have set another one in proxy preference specially, nothing to do.
        if (proxyData.getHost() != null && !host.equals(proxyData.getHost())) {
            return;
        }

        proxyData.setHost(p.getHost());
        proxyData.setPort(p.getPort());
        // if need authenticate for the proxy.
        String username = p.getUsername();
        if (username != null && username.trim().length() > 0) {
            proxyData.setUserid(username);
            String password = p.getPassword();
            if (password != null && password.trim().length() > 0) {
                proxyData.setPassword(password);
            }
        }
        proxyService.setProxyData(new IProxyData[] { proxyData });

        String nonProxyHosts = p.getNonProxyHosts();
        if (nonProxyHosts != null && nonProxyHosts.trim().length() > 0) {
            List<String> bypassHosts = new ArrayList<String>();
            String[] nonProxiedHosts = proxyService.getNonProxiedHosts();
            if (nonProxiedHosts != null) {
                bypassHosts.addAll(Arrays.asList(nonProxiedHosts));
            }
            boolean addNew = false;
            String[] convertPropertyStringToHosts = ProxyType.convertPropertyStringToHosts(nonProxyHosts);
            for (String nonProxy : convertPropertyStringToHosts) {
                if (!bypassHosts.contains(nonProxy)) { // if not existed
                    addNew = true;
                    bypassHosts.add(nonProxy);
                }
            }
            if (addNew) {
                proxyService.setNonProxiedHosts(bypassHosts.toArray(new String[bypassHosts.size()]));
            }
        }
    }

    private boolean updateProfileSettings(IProgressMonitor monitor, IMaven maven, Settings settings, Path configPath,
            File userSettingsFile) {
        if (monitor.isCanceled()) {
            return false;
        }
        boolean modified = false;
        Profile profile = settings.getProfilesAsMap().get("provider-repository"); //$NON-NLS-1$
        // remove old one, because it's never used.
        if (profile != null) {
            settings.removeProfile(profile);
            modified = true;
        }

        return modified;
    }
}
