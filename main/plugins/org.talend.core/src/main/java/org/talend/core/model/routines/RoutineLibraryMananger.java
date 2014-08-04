// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.model.routines;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.talend.core.CorePlugin;
import org.talend.core.ILibraryManagerService;
import org.talend.core.model.general.LibraryInfo;
import org.talend.core.repository.constants.FileConstants;

/**
 * wchen class global comment. Detailled comment
 */
public class RoutineLibraryMananger {

    private static final String EXTENSION_POINT_ID = "org.talend.core.systemRoutineLibrary"; //$NON-NLS-1$

    private static final String LIB_FOLDER = "/lib"; //$NON-NLS-1$

    private static final String LIBRARY_ELE = "library"; //$NON-NLS-1$

    private static final String NAME_ATTR = "name"; //$NON-NLS-1$

    private static final String BUNDLE_ID = "bundleId"; //$NON-NLS-1$

    protected static Logger log = Logger.getLogger(RoutineLibraryMananger.class.getName());

    private boolean initialized = false;

    private Map<String, List<LibraryInfo>> routineAndJars = null;

    private static IConfigurationElement[] configurationElements = null;

    private static RoutineLibraryMananger manager = new RoutineLibraryMananger();

    public static RoutineLibraryMananger getInstance() {
        return manager;
    }

    static {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        configurationElements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID);
    }

    /**
     * DOC ycbai Comment method "initializeSystemLibs".
     */
    public void initializeSystemLibs() {
        if (!initialized) {
            ILibraryManagerService libraryManagerService = CorePlugin.getDefault().getRepositoryBundleService();
            for (IConfigurationElement current : configurationElements) {
                String bundleName = current.getContributor().getName();
                Bundle bundle = Platform.getBundle(bundleName);
                Enumeration entryPaths = bundle.getEntryPaths(LIB_FOLDER);
                if (entryPaths == null) {
                    continue;
                }
                while (entryPaths.hasMoreElements()) {
                    Object entryPath = entryPaths.nextElement();
                    if (entryPath != null && entryPath instanceof String) {
                        String path = (String) entryPath;
                        if (path.endsWith(FileConstants.JAR_FILE_SUFFIX)) {
                            URL entry = bundle.getEntry(path);
                            if (entry != null) {
                                try {
                                    URL fileUrl = FileLocator.toFileURL(entry);
                                    libraryManagerService.deploy(fileUrl.toURI());
                                } catch (Exception e) {
                                    log.warn("Cannot deploy: " + bundleName + path);
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
            initialized = true;
        }
    }

    public Map<String, List<LibraryInfo>> getRoutineAndJars() {
        if (routineAndJars == null) {
            routineAndJars = new HashMap<String, List<LibraryInfo>>();
            for (IConfigurationElement current : configurationElements) {
                String routine = current.getAttribute(NAME_ATTR);
                List<LibraryInfo> jarList = routineAndJars.get(routine);
                if (jarList == null) {
                    jarList = new ArrayList<LibraryInfo>();
                    routineAndJars.put(routine, jarList);
                }
                IConfigurationElement[] children = current.getChildren(LIBRARY_ELE);
                for (IConfigurationElement child : children) {
                    LibraryInfo libraryInfo = new LibraryInfo();
                    String library = child.getAttribute(NAME_ATTR);
                    libraryInfo.setLibName(library);
                    IConfigurationElement[] bundleIdChildren = child.getChildren(BUNDLE_ID);
                    if (bundleIdChildren == null || bundleIdChildren.length == 0) {
                        libraryInfo.setBundleId(null);
                    } else {
                        for (IConfigurationElement bundleIdChild : bundleIdChildren) {
                            String bundleId = bundleIdChild.getAttribute(BUNDLE_ID);
                            libraryInfo.setBundleId(StringUtils.trimToEmpty(bundleId));
                        }
                    }
                    if (!jarList.contains(libraryInfo)) {
                        jarList.add(libraryInfo);
                    }
                }
            }
        }

        return routineAndJars;
    }

}