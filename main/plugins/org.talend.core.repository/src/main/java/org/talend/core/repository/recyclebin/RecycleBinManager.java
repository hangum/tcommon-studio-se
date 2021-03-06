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
package org.talend.core.repository.recyclebin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.runtime.model.emf.EmfHelper;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProjectRepositoryNode;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.utils.URIHelper;
import org.talend.model.recyclebin.RecycleBin;
import org.talend.model.recyclebin.RecycleBinFactory;
import org.talend.model.recyclebin.RecycleBinPackage;
import org.talend.model.recyclebin.TalendItem;
import org.talend.repository.ProjectManager;

/**
 * created by nrousseau on Jun 15, 2015 Detailled comment
 *
 */
public class RecycleBinManager {

    private static Map<String, RecycleBin> projectRecyclebins;

    public static final String TALEND_RECYCLE_BIN_INDEX = "recycle_bin.index"; //$NON-NLS-1$

    private static RecycleBinManager manager;

    private RecycleBinManager() {

    }

    public static RecycleBinManager getInstance() {
        if (manager == null) {
            manager = new RecycleBinManager();
            projectRecyclebins = new HashMap<String, RecycleBin>();
        }
        return manager;
    }

    public List<String> getDeletedFolders(Project project) {
        return new ArrayList<String>(project.getEmfProject().getDeletedFolders());
    }

    public void clearCache() {
        projectRecyclebins.clear();
    }

    public void clearIndex(Project project) {
        loadRecycleBin(project);
        projectRecyclebins.get(project.getTechnicalLabel()).getDeletedItems().clear();
        saveRecycleBin(project);
    }

    public List<IRepositoryViewObject> getDeletedObjects(Project project) {
        loadRecycleBin(project);
        List<IRepositoryViewObject> deletedObjects = new ArrayList<IRepositoryViewObject>();
        final EList<TalendItem> deletedItems = projectRecyclebins.get(project.getTechnicalLabel()).getDeletedItems();
        List<TalendItem> notDeletedItems = new ArrayList<TalendItem>();
        for (TalendItem deletedItem : deletedItems) {
            try {
                IRepositoryViewObject object = ProxyRepositoryFactory.getInstance().getLastVersion(project, deletedItem.getId(),
                        deletedItem.getPath(), ERepositoryObjectType.getType(deletedItem.getType()));
                if (object == null) {
                    object = ProxyRepositoryFactory.getInstance().getLastVersion(project, deletedItem.getId());
                }
                if (object != null) {
                    Item item = object.getProperty().getItem();
                    boolean hasSubItem = false;
                    if (item instanceof ConnectionItem) {
                        hasSubItem = ProjectRepositoryNode.getInstance().hasDeletedSubItem((ConnectionItem) item);
                    }
                    if (object.isDeleted() || hasSubItem) {
                        deletedObjects.add(object);
                    } else {
                        // need remove it.
                        notDeletedItems.add(deletedItem);
                    }
                } else {
                    // need remove it.
                    notDeletedItems.add(deletedItem);
                }
            } catch (PersistenceException e) {
                ExceptionHandler.process(e);
            }
        }
        // clean
        deletedItems.removeAll(notDeletedItems);
        return deletedObjects;
    }

    public void addToRecycleBin(Project project, Item item) {
        addToRecycleBin(project, item, false);
    }

    public void addToRecycleBin(Project project, Item item, boolean skipAutoSave) {
        loadRecycleBin(project);
        boolean contains = false;
        for (TalendItem deletedItem : projectRecyclebins.get(project.getTechnicalLabel()).getDeletedItems()) {
            if (item.getProperty().getId().equals(deletedItem.getId())) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            TalendItem recBinItem = RecycleBinFactory.eINSTANCE.createTalendItem();
            recBinItem.setId(item.getProperty().getId());
            recBinItem.setPath(item.getState().getPath());
            recBinItem.setType(ERepositoryObjectType.getItemType(item).getType());
            projectRecyclebins.get(project.getTechnicalLabel()).getDeletedItems().add(recBinItem);
        }
        if (!skipAutoSave) {
            saveRecycleBin(project);
        }
    }

    public void removeFromRecycleBin(Project project, Item item) {
        removeFromRecycleBin(project, item, false);
    }

    public void removeFromRecycleBin(Project project, Item item, boolean skipAutoSave) {
        loadRecycleBin(project);
        TalendItem itemToDelete = null;
        for (TalendItem deletedItem : projectRecyclebins.get(project.getTechnicalLabel()).getDeletedItems()) {
            if (item.getProperty().getId().equals(deletedItem.getId())) {
                itemToDelete = deletedItem;
                break;
            }
        }
        if (itemToDelete != null) {
            projectRecyclebins.get(project.getTechnicalLabel()).getDeletedItems().remove(itemToDelete);
            if (!skipAutoSave) {
                saveRecycleBin(project);
            }
        }
    }

    private void loadRecycleBin(Project project) {
        if (projectRecyclebins.get(project.getTechnicalLabel()) != null) {
            // already loaded, nothing to do. Don't do any force reload
            return;
        }
        Resource resource = getResource(project);
        try {
            if (resource != null) {
                resource.load(null);
                RecycleBin rbin = (RecycleBin) EcoreUtil.getObjectByType(resource.getContents(),
                        RecycleBinPackage.eINSTANCE.getRecycleBin());
                projectRecyclebins.put(project.getTechnicalLabel(), rbin);
            } else {
                projectRecyclebins.put(project.getTechnicalLabel(), RecycleBinFactory.eINSTANCE.createRecycleBin());
            }
        } catch (IOException e) {
            ExceptionHandler.process(e);
            // if there is any exception, just set a new resource
            projectRecyclebins.put(project.getTechnicalLabel(), RecycleBinFactory.eINSTANCE.createRecycleBin());
        }
    }

    public void saveRecycleBin(Project project) {
        if (projectRecyclebins.get(project.getTechnicalLabel()) == null) {
            loadRecycleBin(project);
        }
        try {
            Resource resource = getResource(project);
            if (resource == null) {
                resource = createRecycleBinResource(project);
            }
            resource.getContents().clear();
            projectRecyclebins.get(project.getTechnicalLabel()).setLastUpdate(new Date());
            resource.getContents().add(projectRecyclebins.get(project.getTechnicalLabel()));
            EmfHelper.saveResource(resource);
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        }
    }

    private Resource getResource(Project project) {
        if (projectRecyclebins.get(project.getTechnicalLabel()) == null
                || projectRecyclebins.get(project.getTechnicalLabel()).eResource() == null) {

            IProject eclipseProject = ProjectManager.getInstance().getResourceProject(project.getEmfProject());
            if (eclipseProject.getFile(TALEND_RECYCLE_BIN_INDEX).exists()) {
                return createRecycleBinResource(project);
            }
            return null;
        }
        return projectRecyclebins.get(project.getTechnicalLabel()).eResource();
    }

    private Resource createRecycleBinResource(Project project) {
        IProject eclipseProject = ProjectManager.getInstance().getResourceProject(project.getEmfProject());
        URI uri = URIHelper.convert(eclipseProject.getFullPath().append(TALEND_RECYCLE_BIN_INDEX));

        XMLResourceFactoryImpl resourceFact = new XMLResourceFactoryImpl();
        XMLResource resource = (XMLResource) resourceFact.createResource(uri);
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
        resource.getDefaultLoadOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);

        resource.getDefaultSaveOptions().put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);

        resource.getDefaultLoadOptions().put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);

        resource.getDefaultLoadOptions().put(XMLResource.OPTION_USE_LEXICAL_HANDLER, Boolean.TRUE);
        return resource;
    }

}
