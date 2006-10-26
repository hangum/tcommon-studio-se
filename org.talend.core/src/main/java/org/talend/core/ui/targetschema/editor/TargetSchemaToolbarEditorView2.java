// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.core.ui.targetschema.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.talend.core.model.action.IAction;
import org.talend.core.model.metadata.MetadataColumn;
import org.talend.core.model.metadata.MetadataSchema;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.SchemaTarget;
import org.talend.core.model.metadata.editor.MetadataEditorActionFactory;
import org.talend.core.model.metadata.editor.MetadataEditorEvent;
import org.talend.core.model.metadata.editor.MetadataTableEditor;
import org.talend.core.model.targetschema.editor.TargetSchemaEditor2;
import org.talend.core.model.targetschema.editor.TargetSchemaEditorActionFactory2;
import org.talend.core.model.targetschema.editor.TargetSchemaEditorEvent;
import org.talend.core.ui.ImageProvider;
import org.talend.core.ui.ImageProvider.EImage;
import org.xml.sax.SAXException;

/**
 * DOC cantoine class global comment. Detailled comment <br/>
 * 
 * TGU same purpose as TargetSchemaToolbarEditorView2 but uses EMF model directly $Id: TargetSchemaToolbarEditorView2.java,v 1.1
 * 2006/08/02 19:43:45 cantoine Exp $
 * 
 */
public class TargetSchemaToolbarEditorView2 {

    private Composite toolbar;

    private Button addButton;

    private Button removeButton;

    private Button copyButton;

    // private Button cutButton;

    private Button pasteButton;

    private Button moveUpButton;

    private Button moveDownButton;

    private Button loadButton;

    private Button exportButton;

    private TargetSchemaTableEditorView2 targetSchemaEditorView;

    private String defaultLabel = "newColumn";

    private static int indexNewColumn;

    /**
     * DOC amaumont MatadataToolbarEditor constructor comment.
     * 
     * @param parent
     * @param style
     * @param targetSchemaEditorView
     */
    public TargetSchemaToolbarEditorView2(Group parent, int style, TargetSchemaTableEditorView2 targetSchemaEditorView) {
        toolbar = new Composite(parent, style);
        toolbar.setLayout(new RowLayout(SWT.HORIZONTAL));
        // Force the height of the toolbars
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.minimumHeight = 40;
        gridData.heightHint = 40;
        toolbar.setLayoutData(gridData);
        this.targetSchemaEditorView = targetSchemaEditorView;
        createComponents();
        addListeners();
    }

    /**
     * DOC amaumont Comment method "addListeners".
     */
    private void addListeners() {
        addButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (targetSchemaEditorView.getTargetSchemaEditor() != null) {
                    targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                    TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.ADD;
                    SchemaTarget schemaTarget = ConnectionFactory.eINSTANCE.createSchemaTarget();
                    schemaTarget.setLabel(targetSchemaEditorView.getTargetSchemaEditor().getValidateColumnName(defaultLabel,
                            (++indexNewColumn)));
                    targetSchemaEditorEvent.entries.add(schemaTarget);
                    Table targetSchemaEditorTable = targetSchemaEditorView.getTableViewerCreator().getTable();
                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorTable.getSelectionIndices();
                    IAction action = TargetSchemaEditorActionFactory2.getInstance()
                            .getAction(targetSchemaEditorView, targetSchemaEditorEvent);
                    action.run(targetSchemaEditorEvent);
                }
            }
        });
        
        removeButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (targetSchemaEditorView.getTargetSchemaEditor() != null) {
                    int index = targetSchemaEditorView.getTableViewerCreator().getTable().getSelectionIndex();
                    targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                    TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.REMOVE;
                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = TargetSchemaEditorActionFactory2.getInstance()
                            .getAction(targetSchemaEditorView, targetSchemaEditorEvent);
                    action.run(targetSchemaEditorEvent);
                    if ((index) < targetSchemaEditorView.getTableViewerCreator().getTable().getItemCount()) {
                        targetSchemaEditorView.getTableViewerCreator().getTable().setSelection(index);
                    } else if (targetSchemaEditorView.getTableViewerCreator().getTable().getItemCount() != 0) {
                        targetSchemaEditorView.getTableViewerCreator().getTable().setSelection(
                                targetSchemaEditorView.getTableViewerCreator().getTable().getItemCount() - 1);
                    }
                    targetSchemaEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        copyButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (targetSchemaEditorView.getTargetSchemaEditor() != null) {
                    targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                    TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.COPY;
                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = TargetSchemaEditorActionFactory2.getInstance()
                            .getAction(targetSchemaEditorView, targetSchemaEditorEvent);
                    action.run(targetSchemaEditorEvent);
                    targetSchemaEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        pasteButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (targetSchemaEditorView.getTargetSchemaEditor() != null) {
                    targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                    TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.PASTE;
                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = TargetSchemaEditorActionFactory2.getInstance()
                            .getAction(targetSchemaEditorView, targetSchemaEditorEvent);
                    action.run(targetSchemaEditorEvent);
                    targetSchemaEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        moveUpButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (targetSchemaEditorView.getTargetSchemaEditor() != null) {
                    targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                    TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.MOVE_UP;
                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = TargetSchemaEditorActionFactory2.getInstance()
                            .getAction(targetSchemaEditorView, targetSchemaEditorEvent);
                    action.run(targetSchemaEditorEvent);
                    targetSchemaEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        moveDownButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (targetSchemaEditorView.getTargetSchemaEditor() != null) {
                    targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                    TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.MOVE_DOWN;
                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorView.getTableViewerCreator().getTable()
                            .getSelectionIndices();
                    IAction action = TargetSchemaEditorActionFactory2.getInstance()
                            .getAction(targetSchemaEditorView, targetSchemaEditorEvent);
                    action.run(targetSchemaEditorEvent);
                    targetSchemaEditorView.getTableViewerCreator().getTableViewer().refresh();
                }
            }
        });

        loadButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                FileDialog dial = new FileDialog(toolbar.getShell(), SWT.OPEN);
                dial.setFilterExtensions(new String[] { "*.xml" });
                String fileName = dial.open();
                if ((fileName != null) && (!fileName.equals(""))) {
                    File file = new File(fileName);
                    if (file != null) {
                        TargetSchemaEditor2 newEditor = targetSchemaEditorView.getTargetSchemaEditor();
                        if (newEditor != null) {
                            try {
                                // remove all the columns from the table
                                TargetSchemaEditorEvent targetSchemaEditorEvent = new TargetSchemaEditorEvent();
                                targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.REMOVE;
                                int[] index = new int[targetSchemaEditorView.getTableViewerCreator().getTable().getItemCount()];
                                for (int i = 0; i < targetSchemaEditorView.getTableViewerCreator().getTable().getItemCount(); i++) {
                                    index[i] = i;
                                }
                                targetSchemaEditorEvent.entriesIndices = index;
                                IAction action = TargetSchemaEditorActionFactory2.getInstance().getAction(targetSchemaEditorView,
                                        targetSchemaEditorEvent);
                                action.run(targetSchemaEditorEvent);

                                // load the schema
                                //PTODO CAN : XMLFILE ATTENTION AVEC SCHEMATARGET
                                List<SchemaTarget> loadSchemaTarget = MetadataSchema.loadTargetSchemaColumnFromFile(file);

                                // Add Columnsof Schema to the table
                                if (!loadSchemaTarget.isEmpty()) {
                                    targetSchemaEditorEvent.type = TargetSchemaEditorEvent.TYPE.ADD;
                                    List<SchemaTarget> newList = new ArrayList<SchemaTarget>();
                                    for (int i = 0; i < loadSchemaTarget.size(); i++) {
                                        // check the unicity of label
                                        String label = loadSchemaTarget.get(i).getLabel();
                                        label = targetSchemaEditorView.getTargetSchemaEditor().getValidateColumnName(label, i, newList);
                                        loadSchemaTarget.get(i).setLabel(label);
                                        newList.add(loadSchemaTarget.get(i));
                                        targetSchemaEditorEvent.entries.add(loadSchemaTarget.get(i));
                                    }
                                    Table targetSchemaEditorTable = targetSchemaEditorView.getTableViewerCreator().getTable();
                                    targetSchemaEditorEvent.entriesIndices = targetSchemaEditorTable.getSelectionIndices();
                                    action = TargetSchemaEditorActionFactory2.getInstance().getAction(targetSchemaEditorView,
                                            targetSchemaEditorEvent);
                                    action.run(targetSchemaEditorEvent);
                                }

                                // Refresh the table
                                targetSchemaEditorView.getTableViewerCreator().getTable().setFocus();
                                targetSchemaEditorView.getTableViewerCreator().getTable().deselectAll();
                                targetSchemaEditorView.getTableViewerCreator().getTableViewer().refresh();
                                targetSchemaEditorView.getTableViewerCreator().layout();

                            } catch (ParserConfigurationException e) {
                                openMessageError("Parsing XML Error : " + e.getMessage());
                            } catch (SAXException e) {
                                openMessageError("Parsing XML Error : " + e.getMessage());
                            } catch (IOException e) {
                                openMessageError("File Error : " + e.getMessage());
                            }
                        }
                    }
                }
            }

            private void openMessageError(String errorText) {
                MessageBox msgBox = new MessageBox(toolbar.getShell());
                msgBox.setText("Error Occurred");
                msgBox.setMessage(errorText);
                msgBox.open();
            }
        });

        exportButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                FileDialog dial = new FileDialog(toolbar.getShell(), SWT.SAVE);
                dial.setFilterExtensions(new String[] { "*.xml" });
                String fileName = dial.open();
                if ((fileName != null) && (!fileName.equals(""))) {
                    File file = new File(fileName);
                    try {
                        file.createNewFile();
                        if (file != null) {
                            TargetSchemaEditor2 newEditor = targetSchemaEditorView.getTargetSchemaEditor();
                            if (newEditor != null) {
                                // get all the columns from the table
                                //PTODO CAN : XMLFILE
                                org.talend.core.model.metadata.builder.connection.MetadataSchema oldTable = newEditor.getMetadataSchema();
                                MetadataSchema.saveSchemaTargetToFile(file, oldTable);
                            }
                        }
                    } catch (IOException e) {
                        openMessageError("File Error : " + e.getMessage());
                    } catch (ParserConfigurationException e) {
                        openMessageError("Parsing XML Error : " + e.getMessage());
                    }
                }
            }

            private void openMessageError(String errorText) {
                MessageBox msgBox = new MessageBox(toolbar.getShell());
                msgBox.setText("Error Occurred");
                msgBox.setMessage(errorText);
                msgBox.open();
            }
        });
    }

    /**
     * DOC amaumont Comment method "createComponents".
     */
    private void createComponents() {

        addButton = new Button(toolbar, SWT.PUSH);
        addButton.setToolTipText("Add");
        addButton.setImage(ImageProvider.getImage(EImage.ADD_ICON));

        removeButton = new Button(toolbar, SWT.PUSH);
        removeButton.setToolTipText("Remove");
        removeButton.setImage(ImageProvider.getImage(EImage.MINUS_ICON));

        copyButton = new Button(toolbar, SWT.PUSH);
        copyButton.setToolTipText("Copy");
        copyButton.setImage(ImageProvider.getImage(EImage.COPY_ICON));

        // cutButton = new Button(toolbar, SWT.PUSH);
        // cutButton.setText("Cut");
        // cutButton.setImage(ImageProvider.getImage(EImage.CUT).createImage());

        pasteButton = new Button(toolbar, SWT.PUSH);
        pasteButton.setToolTipText("Paste");
        pasteButton.setImage(ImageProvider.getImage(EImage.PASTE_ICON));

        moveUpButton = new Button(toolbar, SWT.PUSH);
        moveUpButton.setToolTipText("Move up");
        moveUpButton.setImage(ImageProvider.getImage(EImage.UP_ICON));

        moveDownButton = new Button(toolbar, SWT.PUSH);
        moveDownButton.setToolTipText("Move down");
        moveDownButton.setImage(ImageProvider.getImage(EImage.DOWN_ICON));

        loadButton = new Button(toolbar, SWT.PUSH);
        loadButton.setToolTipText("Import");
        loadButton.setImage(ImageProvider.getImage(EImage.IMPORT_ICON));

        exportButton = new Button(toolbar, SWT.PUSH);
        exportButton.setToolTipText("Export");
        exportButton.setImage(ImageProvider.getImage(EImage.EXPORT_ICON));
    }

    /**
     * DOC ocarbone Comment method "setReadOnly". setReadOnly(boolean) is equivalent to setEnabled(boolean) all the
     * buttons.
     * 
     * @param b
     */
    public void setReadOnly(boolean b) {
        addButton.setEnabled(!b);
        removeButton.setEnabled(!b);
        copyButton.setEnabled(!b);
        pasteButton.setEnabled(!b);
        moveUpButton.setEnabled(!b);
        moveDownButton.setEnabled(!b);
        loadButton.setEnabled(!b);
        exportButton.setEnabled(!b);
    }

    /**
     * DOC ocarbone Comment method "setDefaultLabel". setDefaultLabel determine the label to use when Add button is
     * used.
     * 
     * @param string
     */
    public void setDefaultLabel(String label) {
        this.defaultLabel = label;
    }
}
