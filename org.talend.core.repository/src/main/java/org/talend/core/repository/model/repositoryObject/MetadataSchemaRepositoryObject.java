// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.repository.model.repositoryObject;

import java.util.Date;
import java.util.List;

import org.talend.core.model.metadata.MetadataSchema;
import org.talend.core.model.metadata.builder.connection.AbstractMetadataObject;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.User;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ISubRepositoryObject;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IRepositoryNode;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.resource.relational.Schema;

/**
 * DOC klliu class global comment. Detailled comment
 */
public class MetadataSchemaRepositoryObject extends MetadataSchema implements ISubRepositoryObject {

    private final IRepositoryViewObject viewObject;

    private final Schema schema;

    private IRepositoryNode repositoryNode;

    public IRepositoryViewObject getViewObject() {
        return this.viewObject;
    }

    public Schema getSchema() {
        return this.schema;
    }

    /**
     * DOC klliu MetadataSchemaRepositoryObject constructor comment.
     * 
     * @param repositoryViewObject
     * @param schema
     */
    public MetadataSchemaRepositoryObject(IRepositoryViewObject repositoryViewObject, Schema schema) {
        this.viewObject = repositoryViewObject;
        this.schema = schema;
    }

    @Override
    public Property getProperty() {
        Property property = viewObject.getProperty();
        // update table
        updateCatalog(property);
        return property;
    }

    // @Override
    @Override
    public String getVersion() {
        return viewObject.getVersion();
    }

    @Override
    public String getLabel() {
        return getSchema().getName();
    }

    @Override
    public String getId() {
        return getSchema().getName();
    }

    @Override
    public AbstractMetadataObject getAbstractMetadataObject() {
        return null;
    }

    @Override
    public void removeFromParent() {
    }

    private void updateCatalog(Property property) {
    }

    @Override
    public User getAuthor() {
        return viewObject.getAuthor();
    }

    @Override
    public List<IRepositoryViewObject> getChildren() {
        return this.viewObject.getChildren();
    }

    @Override
    public Date getCreationDate() {
        return viewObject.getCreationDate();
    }

    @Override
    public String getDescription() {
        return viewObject.getDescription();
    }

    @Override
    public ERepositoryStatus getInformationStatus() {
        return viewObject.getInformationStatus();
    }

    @Override
    public Date getModificationDate() {
        return viewObject.getModificationDate();
    }

    @Override
    public String getPath() {
        return viewObject.getPath();
    }

    @Override
    public String getProjectLabel() {
        return viewObject.getProjectLabel();
    }

    @Override
    public String getPurpose() {
        return viewObject.getPurpose();
    }

    @Override
    public IRepositoryNode getRepositoryNode() {
        return this.repositoryNode;
    }

    @Override
    public ERepositoryStatus getRepositoryStatus() {
        return viewObject.getRepositoryStatus();
    }

    @Override
    public String getStatusCode() {
        return viewObject.getStatusCode();
    }

    @Override
    public ERepositoryObjectType getRepositoryObjectType() {
        return ERepositoryObjectType.METADATA_CON_SCHEMA;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public void setRepositoryNode(IRepositoryNode node) {
        this.repositoryNode = node;
    }

    @Override
    public ModelElement getModelElement() {
        return this.schema;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.model.repository.IRepositoryViewObject#isModified()
     */
    @Override
    public boolean isModified() {
        return viewObject.isModified();
    }

}