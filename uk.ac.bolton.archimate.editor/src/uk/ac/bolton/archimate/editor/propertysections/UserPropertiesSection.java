/*******************************************************************************
 * Copyright (c) 2011 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.propertysections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;

import uk.ac.bolton.archimate.editor.diagram.editparts.DiagramPart;
import uk.ac.bolton.archimate.editor.model.commands.EObjectFeatureCommand;
import uk.ac.bolton.archimate.editor.model.commands.EObjectNonNotifyingCompoundCommand;
import uk.ac.bolton.archimate.editor.ui.ExtendedTitleAreaDialog;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.editor.ui.ImageFactory;
import uk.ac.bolton.archimate.editor.ui.StringComboBoxCellEditor;
import uk.ac.bolton.archimate.editor.utils.HTMLUtils;
import uk.ac.bolton.archimate.editor.utils.StringUtils;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IArchimateFactory;
import uk.ac.bolton.archimate.model.IArchimateModel;
import uk.ac.bolton.archimate.model.IArchimateModelElement;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IDiagramModel;
import uk.ac.bolton.archimate.model.IProperties;
import uk.ac.bolton.archimate.model.IProperty;


/**
 * User Properties Section for an Archimate Element, Archimate Model or Diagram Model
 * 
 * @author Phillip Beauvoir
 */
public class UserPropertiesSection extends AbstractArchimatePropertySection {

    private static final String HELP_ID = "uk.ac.bolton.archimate.help.userProperties";

    /**
     * Filter to show or reject this section depending on input value
     */
    public static class Filter implements IFilter {
        @Override
        public boolean select(Object object) {
            if(object instanceof IProperties) {
                return true;
            }
            if(object instanceof DiagramPart) {
                return true;
            }
            if(object instanceof IAdaptable) {
                return ((IAdaptable)object).getAdapter(IArchimateElement.class) != null;
            }
            return false;
        }
    }

    /*
     * Adapter to listen to changes made elsewhere (including Undo/Redo commands)
     * This one is a EContentAdapter to listen to child IProperty changes
     */
    private Adapter eAdapter = new EContentAdapter() {
        private boolean ignoreMessages;

        @Override
        public void notifyChanged(Notification msg) {
            super.notifyChanged(msg);

            if(msg.getEventType() == EObjectNonNotifyingCompoundCommand.START) {
                ignoreMessages = true;
                return;
            }
            else if(msg.getEventType() == EObjectNonNotifyingCompoundCommand.END) {
                ignoreMessages = false;
                fTableViewer.refresh();
                fTableLayout.doRelayout();
            }

            if(!ignoreMessages) {
                Object feature = msg.getFeature();
                if(feature == IArchimatePackage.Literals.PROPERTIES__PROPERTIES) {
                    fTableViewer.refresh();
                    fTableLayout.doRelayout();
                }
                if(feature == IArchimatePackage.Literals.PROPERTY__KEY
                        || feature == IArchimatePackage.Literals.PROPERTY__VALUE) {
                    fTableViewer.update(msg.getNotifier(), null);
                }
            }
        }
    };

    private IProperties fPropertiesElement;

    private TableViewer fTableViewer;
    private UpdatingTableColumnLayout fTableLayout;
    private IAction fActionNewProperty, fActionNewMultipleProperty, fActionRemoveProperty, fActionShowKeyEditor;

    @Override
    protected void createControls(Composite parent) {
        createTableControl(parent);
    }

    @Override
    protected void setElement(Object element) {
        // IProperties element
        if(element instanceof IProperties) {
            fPropertiesElement = (IProperties)element;
        }
        // Diagram
        else if(element instanceof DiagramPart) {
            fPropertiesElement = (IProperties)((IAdaptable)element).getAdapter(IDiagramModel.class);;
        }
        // IArchimateElement in a GEF Edit Part
        else if(element instanceof IAdaptable) {
            fPropertiesElement = (IArchimateElement)((IAdaptable)element).getAdapter(IArchimateElement.class);
        }
        else {
            System.err.println(getClass() + " wants to display for " + element);
        }

        if(!(fPropertiesElement instanceof IArchimateModelElement)) {
            System.err.println(getClass() + " wants to display for wrong type: " + fPropertiesElement);
        }
    }

    @Override
    public void refresh() {
        // Populate fields...
        fTableViewer.setInput(fPropertiesElement);
        fTableLayout.doRelayout();
    }

    @Override
    protected Adapter getECoreAdapter() {
        return eAdapter;
    }

    @Override
    protected EObject getEObject() {
        return fPropertiesElement;
    }

    @Override
    public boolean shouldUseExtraSpace() {
        return true;
    }

    private void createTableControl(Composite parent) {
        // Table
        Composite tableComp = new Composite(parent, SWT.NULL);
        fTableLayout = new UpdatingTableColumnLayout(tableComp);
        tableComp.setLayout(fTableLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        // This ensures a minumum and equal size and no horizontal size creep
        gd.widthHint = 100;
        gd.heightHint = 50;
        tableComp.setLayoutData(gd);
        Table table = createTable(tableComp, SWT.MULTI);
        fTableViewer = new TableViewer(table);

        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addDragSupport();
        addDropSupport();

        // Help ID on table
        PlatformUI.getWorkbench().getHelpSystem().setHelp(table, HELP_ID);

        // Columns
        TableViewerColumn columnBlank = new TableViewerColumn(fTableViewer, SWT.NONE, 0);
        fTableLayout.setColumnData(columnBlank.getColumn(), new ColumnPixelData(24, false));

        TableViewerColumn columnKey = new TableViewerColumn(fTableViewer, SWT.NONE, 1);
        columnKey.getColumn().setText("Name");
        fTableLayout.setColumnData(columnKey.getColumn(), new ColumnWeightData(20, true));
        columnKey.setEditingSupport(new KeyEditingSupport(fTableViewer));

        // Click on Key Table Header
        columnKey.getColumn().addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                sortKeys();
            }
        });

        TableViewerColumn columnValue = new TableViewerColumn(fTableViewer, SWT.NONE, 2);
        columnValue.getColumn().setText("Value");
        fTableLayout.setColumnData(columnValue.getColumn(), new ColumnWeightData(80, true));
        columnValue.setEditingSupport(new ValueEditingSupport(fTableViewer));

        // Content Provider
        fTableViewer.setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            public void dispose() {
            }

            public Object[] getElements(Object inputElement) {
                return ((IProperties)inputElement).getProperties().toArray();
            }
        });

        // Label Provider
        fTableViewer.setLabelProvider(new LabelCellProvider());

        // Toolbar
        ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.VERTICAL);
        GridDataFactory.fillDefaults().align(SWT.END, SWT.TOP).applyTo(toolBar);

        ToolBarManager toolBarmanager = new ToolBarManager(toolBar);

        // New Property
        fActionNewProperty = new Action("New") {
            @Override
            public void run() {
                if(isAlive()) {
                    int index = -1;
                    IProperty selected = (IProperty)((IStructuredSelection)fTableViewer.getSelection()).getFirstElement();
                    if(selected != null) {
                        index = fPropertiesElement.getProperties().indexOf(selected) + 1;
                    }
                    IProperty property = IArchimateFactory.eINSTANCE.createProperty();
                    getCommandStack().execute(new NewPropertyCommand(fPropertiesElement.getProperties(), property, index));
                    fTableViewer.editElement(property, 1);
                }
            }

            @Override
            public String getToolTipText() {
                return getText();
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_PLUS);
            }
        };

        // New Multiple Properties
        fActionNewMultipleProperty = new Action("New Multiple...") {
            @Override
            public void run() {
                if(isAlive()) {
                    MultipleAddDialog dialog = new MultipleAddDialog(fPage.getSite().getShell());
                    if(dialog.open() == Window.OK) {
                        getCommandStack().execute(dialog.getCommand());
                    }
                }
            }

            @Override
            public String getToolTipText() {
                return getText();
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_MUTIPLE);
            }
        };

        // Remove Property
        fActionRemoveProperty = new Action("Remove") {
            @Override
            public void run() {
                if(isAlive()) {
                    CompoundCommand compoundCmd = new EObjectNonNotifyingCompoundCommand(fPropertiesElement) {
                        @Override
                        public String getLabel() {
                            return getCommands().size() > 1 ? "Remove Properties" : "Remove Property";
                        }
                    };
                    for(Object o : ((IStructuredSelection)fTableViewer.getSelection()).toList()) {
                        Command cmd = new RemovePropertyCommand(fPropertiesElement.getProperties(), (IProperty)o);
                        compoundCmd.add(cmd);
                    }
                    getCommandStack().execute(compoundCmd);
                }
            }

            @Override
            public String getToolTipText() {
                return getText();
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_SMALL_X);
            }
        };
        fActionRemoveProperty.setEnabled(false);

        // Manage
        fActionShowKeyEditor = new Action("Manage") {
            @Override
            public void run() {
                if(isAlive()) {
                    UserPropertiesManagerDialog dialog = new UserPropertiesManagerDialog(fPage.getSite().getShell(),
                            ((IArchimateModelElement)fPropertiesElement).getArchimateModel());
                    dialog.open();
                }
            }

            @Override
            public String getToolTipText() {
                return getText();
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_COG);
            }
        };

        toolBarmanager.add(fActionNewProperty);
        toolBarmanager.add(fActionNewMultipleProperty);
        toolBarmanager.add(fActionRemoveProperty);
        toolBarmanager.add(fActionShowKeyEditor);
        toolBarmanager.update(true);

        /*
         * Selection Listener
         */
        fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                fActionRemoveProperty.setEnabled(!event.getSelection().isEmpty());
            }
        });

        fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                IProperty selected = (IProperty)((IStructuredSelection)event.getSelection()).getFirstElement();
                if(selected != null) {
                    handleDoubleClick(selected);
                }
            }
        });

        hookContextMenu();
    }

    /**
     * Hook into a right-click menu
     */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PropertiesPopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);

        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });

        Menu menu = menuMgr.createContextMenu(fTableViewer.getControl());
        fTableViewer.getControl().setMenu(menu);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(fActionNewProperty);
        manager.add(fActionNewMultipleProperty);
        manager.add(new Separator());
        manager.add(fActionRemoveProperty);
        manager.add(new Separator());
        manager.add(fActionShowKeyEditor);
    }

    /**
     * Sort Keys
     */
    private void sortKeys() {
        if(isAlive()) {
            getCommandStack().execute(new SortPropertiesCommand(fPropertiesElement.getProperties()));
        }
    }

    /**
     * If it's a URL open in Browser
     */
    private void handleDoubleClick(IProperty selected) {
        Matcher matcher = HTMLUtils.HTML_LINK_PATTERN.matcher(selected.getValue());
        if(matcher.find()) {
            String href = matcher.group();
            HTMLUtils.openLinkInBrowser(href);
        }
    }

    /**
     * @return All unique Property Keys for an entire model (sorted)
     */
    private String[] getAllUniquePropertyKeysForModel() {
        IArchimateModel model = ((IArchimateModelElement)fPropertiesElement).getArchimateModel();

        List<String> list = new ArrayList<String>();

        for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();) {
            EObject element = iter.next();
            if(element instanceof IProperty) {
                String key = ((IProperty)element).getKey();
                if(StringUtils.isSetAfterTrim(key) && !list.contains(key)) {
                    list.add(key);
                }
            }
        }

        String[] items = list.toArray(new String[list.size()]);
        Arrays.sort(items, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        return items;
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // Drag & Drop
    //
    // -----------------------------------------------------------------------------------------------------------------

    private boolean fDragSourceValid;

    /*
     * Drag Source support
     */
    private void addDragSupport() {
        int operations = DND.DROP_MOVE;
        Transfer[] transferTypes = new Transfer[] { LocalSelectionTransfer.getTransfer() };
        fTableViewer.addDragSupport(operations, transferTypes, new DragSourceListener() {
            public void dragFinished(DragSourceEvent event) {
                LocalSelectionTransfer.getTransfer().setSelection(null);
                fDragSourceValid = false;
            }

            public void dragSetData(DragSourceEvent event) {
                // For consistency set the data to the selection even though
                // the selection is provided by the LocalSelectionTransfer
                // to the drop target adapter.
                event.data = LocalSelectionTransfer.getTransfer().getSelection();
            }

            public void dragStart(DragSourceEvent event) {
                if(!isAlive()) {
                    return;
                }
                IStructuredSelection selection = (IStructuredSelection)fTableViewer.getSelection();
                LocalSelectionTransfer.getTransfer().setSelection(selection);
                event.doit = true;
                fDragSourceValid = true;
            }
        });
    }

    private void addDropSupport() {
        int operations = DND.DROP_MOVE;
        Transfer[] transferTypes = new Transfer[] { LocalSelectionTransfer.getTransfer() };
        fTableViewer.addDropSupport(operations, transferTypes, new DropTargetListener() {
            int operations = DND.DROP_NONE;

            public void dragEnter(DropTargetEvent event) {
                operations = fDragSourceValid ? event.detail : DND.DROP_NONE;
            }

            public void dragLeave(DropTargetEvent event) {
            }

            public void dragOperationChanged(DropTargetEvent event) {
                operations = fDragSourceValid ? event.detail : DND.DROP_NONE;
            }

            public void dragOver(DropTargetEvent event) {
                event.detail = fDragSourceValid ? operations : DND.DROP_NONE;

                if(operations == DND.DROP_NONE) {
                    event.feedback = DND.FEEDBACK_NONE;
                }
                else {
                    event.feedback = getDragFeedbackType(event);
                    event.feedback |= DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND;
                }
            }

            public void drop(DropTargetEvent event) {
                if(event.detail == DND.DROP_COPY || event.detail == DND.DROP_MOVE) {
                    doDropOperation(event);
                }
            }

            public void dropAccept(DropTargetEvent event) {
            }

        });
    }

    @SuppressWarnings("unchecked")
    private void doDropOperation(DropTargetEvent event) {
        if(!LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
            return;
        }

        ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
        if(selection == null || selection.isEmpty()) {
            return;
        }

        // Determine the index position of the drop
        int index = getDropTargetPosition(event);

        // Valid position?
        List<?> list = ((IStructuredSelection)selection).toList();
        for(Object o : list) {
            IProperty property = (IProperty)o;
            int movedIndex = fPropertiesElement.getProperties().indexOf(property);
            if(movedIndex == index || (movedIndex + 1) == index) {
                return;
            }
        }

        movePropertiesToIndex((List<IProperty>)list, index);
    }

    private void movePropertiesToIndex(List<IProperty> propertiesToMove, int index) {
        EList<IProperty> properties = fPropertiesElement.getProperties();

        // Sanity check
        if(index < 0) {
            index = 0;
        }
        if(index > properties.size()) {
            index = properties.size();
        }

        CompoundCommand compoundCmd = new CompoundCommand("Move Properties");

        for(IProperty property : propertiesToMove) {
            int oldIndex = properties.indexOf(property);

            if(index > oldIndex) {
                index--;
            }

            if(index == oldIndex) {
                break;
            }

            compoundCmd.add(new MovePropertyCommand(properties, property, index));

            index++;
        }

        getCommandStack().execute(compoundCmd.unwrap());
    }

    private int getDropTargetPosition(DropTargetEvent event) {
        int index = -1;

        Point pt = fTableViewer.getControl().toControl(event.x, event.y);

        if(pt.y < 5) {
            index = 0;
        }
        else if(event.item != null) {
            IProperty property = (IProperty)event.item.getData();
            index = fPropertiesElement.getProperties().indexOf(property);
        }
        else {
            index = fPropertiesElement.getProperties().size();
        }

        // Dropped in after position
        int feedback = getDragFeedbackType(event);
        if(feedback == DND.FEEDBACK_INSERT_AFTER) {
            index += 1;
        }

        return index;
    }

    /**
     * Determine the feedback type for DND
     */
    private int getDragFeedbackType(DropTargetEvent event) {
        if(event.item == null) {
            return DND.FEEDBACK_NONE;
        }

        Rectangle rect = ((TableItem)event.item).getBounds();
        Point pt = fTableViewer.getControl().toControl(event.x, event.y);
        if(pt.y < rect.y + 3) {
            return DND.FEEDBACK_INSERT_BEFORE;
        }
        if(pt.y > rect.y + rect.height - 3) {
            return DND.FEEDBACK_INSERT_AFTER;
        }

        return DND.FEEDBACK_NONE; // <----- This is important otherwise we get unwanted selection cheese on XP
    }


    // -----------------------------------------------------------------------------------------------------------------
    //
    // Table functions
    //
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Label Provider
     */
    private static class LabelCellProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            switch(columnIndex) {
                case 1:
                    String key = ((IProperty)element).getKey();
                    if(!StringUtils.isSetAfterTrim(key)) {
                        key = "(blank)";
                    }
                    return key;

                case 2:
                    return ((IProperty)element).getValue();

                default:
                    return null;
            }
        }

        @Override
        public Color getForeground(Object element, int columnIndex) {
            if(columnIndex == 2) {
                Matcher matcher = HTMLUtils.HTML_LINK_PATTERN.matcher(((IProperty)element).getValue());
                return matcher.find() ? ColorConstants.blue : null;
            }
            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex) {
            return null;
        }
    }

    /**
     * Key Editor
     */
    private class KeyEditingSupport extends EditingSupport {
        StringComboBoxCellEditor cellEditor;

        public KeyEditingSupport(ColumnViewer viewer) {
            super(viewer);
            cellEditor = new StringComboBoxCellEditor((Composite)viewer.getControl(), new String[0], true);
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            String[] items = new String[0];

            if(isAlive()) {
                items = getAllUniquePropertyKeysForModel();
            }

            cellEditor.setItems(items);
            return cellEditor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        @Override
        protected Object getValue(Object element) {
            return ((IProperty)element).getKey();
        }

        @Override
        protected void setValue(Object element, Object value) {
            if(isAlive()) {
                getCommandStack().execute(new EObjectFeatureCommand("Set Property Name", (IProperty)element,
                        IArchimatePackage.Literals.PROPERTY__KEY, value) {
                    @Override
                    public boolean canExecute() {
                        return (fNewValue != null) ? !fNewValue.equals(fOldValue)
                                : (fOldValue != null) ? !fOldValue.equals(fNewValue)
                                        : false;
                    }
                });
            }
        }
    }

    /**
     * Value Editor
     */
    private class ValueEditingSupport extends EditingSupport {
        TextCellEditor cellEditor;

        public ValueEditingSupport(ColumnViewer viewer) {
            super(viewer);
            cellEditor = new TextCellEditor((Composite)viewer.getControl());
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return cellEditor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        @Override
        protected Object getValue(Object element) {
            return ((IProperty)element).getValue();
        }

        @Override
        protected void setValue(Object element, Object value) {
            if(isAlive()) {
                getCommandStack().execute(new EObjectFeatureCommand("Set Property Value", (IProperty)element,
                        IArchimatePackage.Literals.PROPERTY__VALUE, value) {
                    @Override
                    public boolean canExecute() {
                        return (fNewValue != null) ? !fNewValue.equals(fOldValue)
                                : (fOldValue != null) ? !fOldValue.equals(fNewValue)
                                        : false;
                    }
                });
            }
        }
    }

    /**
     * TableColumnLayout with public method so we can re-layout when the host table adds/removes vertical scroll bar
     * It's a kludge to stop a bogus horizontal scroll bar being shown.
     */
    private static class UpdatingTableColumnLayout extends TableColumnLayout {
        private Composite fParent;

        public UpdatingTableColumnLayout(Composite parent) {
            fParent = parent;
        }

        public void doRelayout() {
            layout(fParent, true);
        }
    }


    // -----------------------------------------------------------------------------------------------------------------
    //
    // Commands
    //
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * New Property Command
     */
    private static class NewPropertyCommand extends Command {
        private EList<IProperty> properties;
        private IProperty property;
        private int index;

        NewPropertyCommand(EList<IProperty> properties, IProperty property, int index) {
            this.properties = properties;
            this.property = property;
            this.index = index;
            setLabel("New Property");
        }

        @Override
        public void execute() {
            if(index < 0 || index > properties.size()) {
                properties.add(property);
            }
            else {
                properties.add(index, property);
            }
        }

        @Override
        public void undo() {
            properties.remove(property);
        }

        @Override
        public void dispose() {
            properties = null;
            property = null;
        }
    }

    /**
     * Remove Property Command
     */
    private static class RemovePropertyCommand extends Command {
        private EList<IProperty> properties;
        private IProperty property;
        private int index;

        RemovePropertyCommand(EList<IProperty> properties, IProperty property) {
            this.properties = properties;
            this.property = property;
        }

        @Override
        public void execute() {
            // Ensure index is stored just before execute because if this is part of a composite action
            // then the index positions will have changed
            index = properties.indexOf(property); 
            if(index != -1) {
                properties.remove(property);
            }
        }

        @Override
        public void undo() {
            if(index != -1) {
                properties.add(index, property);
            }
        }

        @Override
        public void dispose() {
            properties = null;
            property = null;
        }
    }

    /**
     * Move Property Command
     */
    private static class MovePropertyCommand extends Command {
        private EList<IProperty> properties;
        private IProperty property;
        private int oldIndex;
        private int newIndex;

        MovePropertyCommand(EList<IProperty> properties, IProperty property, int newIndex) {
            this.properties = properties;
            this.property = property;
            this.newIndex = newIndex;
            setLabel("Move Property");
        }

        @Override
        public void execute() {
            oldIndex = properties.indexOf(property);
            properties.move(newIndex, property);
        }

        @Override
        public void undo() {
            properties.move(oldIndex, property);
        }

        @Override
        public void dispose() {
            properties = null;
            property = null;
        }
    }

    /**
     * Sort Properties Command
     */
    private static class SortPropertiesCommand extends Command implements Comparator<IProperty>  {
        private EList<IProperty> properties;
        private List<IProperty> original;

        public SortPropertiesCommand(EList<IProperty> properties) {
            setLabel("Sort Properties");
            this.properties = properties;

            // Keep a copy of the original order
            original = new ArrayList<IProperty>(properties);
        }

        @Override
        public boolean canExecute() {
            if(properties.size() < 2) {
                return false;
            }

            EList<IProperty> temp = new BasicEList<IProperty>(properties);
            ECollections.sort(temp, this);

            for(int i = 0; i < temp.size(); i++) {
                if(temp.get(i) != properties.get(i)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void execute() {
            ECollections.sort(properties, this);
        }

        @Override
        public void undo() {
            properties.clear();
            properties.addAll(original);
        }

        @Override
        public int compare(IProperty p1, IProperty p2) {
            String key1 = StringUtils.safeString(p1.getKey());
            String key2 = StringUtils.safeString(p2.getKey());
            return key1.compareToIgnoreCase(key2);
        }

        @Override
        public void dispose() {
            properties = null;
            original = null;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //
    // Multiple Add Dialog
    //
    // -----------------------------------------------------------------------------------------------------------------

    private class MultipleAddDialog extends ExtendedTitleAreaDialog {
        private CheckboxTableViewer tableViewer;
        private Button buttonSelectAll, buttonDeselectAll;

        private CompoundCommand compoundCmd;
        private String[] keys;

        public MultipleAddDialog(Shell parentShell) {
            super(parentShell, "ArchimatePropertiesMultipleAddDialog");
            setTitleImage(IArchimateImages.ImageFactory.getImage(ImageFactory.ECLIPSE_IMAGE_IMPORT_PREF_WIZARD));
            setShellStyle(getShellStyle() | SWT.RESIZE);

            keys = getAllUniquePropertyKeysForModel();
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Properties");
        }

        @Override
        protected Control createButtonBar(Composite parent) {
            Control c = super.createButtonBar(parent);
            if(keys.length == 0) {
                getButton(IDialogConstants.OK_ID).setEnabled(false);
                buttonSelectAll.setEnabled(false);
                buttonDeselectAll.setEnabled(false);
            }
            return c;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            // Help
            PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);

            setTitle("Add Multiple Properties");
            setMessage("Select the Properties to add to this Element");

            Composite composite = (Composite)super.createDialogArea(parent);

            Composite client = new Composite(composite, SWT.NULL);
            GridLayout layout = new GridLayout(2, false);
            client.setLayout(layout);
            client.setLayoutData(new GridData(GridData.FILL_BOTH));

            createTableControl(client);
            createButtonPanel(client);

            return composite;
        }

        private void createTableControl(Composite parent) {
            Composite tableComp = new Composite(parent, SWT.BORDER);
            TableColumnLayout tableLayout = new TableColumnLayout();
            tableComp.setLayout(tableLayout);
            tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));

            Table table = new Table(tableComp, SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
            tableViewer = new CheckboxTableViewer(table);
            tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

            tableViewer.getTable().setLinesVisible(true);

            tableViewer.setSorter(new ViewerSorter());

            // Column
            TableViewerColumn columnKey = new TableViewerColumn(tableViewer, SWT.NONE, 0);
            tableLayout.setColumnData(columnKey.getColumn(), new ColumnWeightData(100, true));

            // Content Provider
            tableViewer.setContentProvider(new IStructuredContentProvider() {
                public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                }

                public void dispose() {
                }

                public Object[] getElements(Object inputElement) {
                    return keys;
                }
            });

            // Label Provider
            tableViewer.setLabelProvider(new LabelProvider());
            tableViewer.setInput(""); // anything will do
        }

        private void createButtonPanel(Composite parent) {
            Composite client = new Composite(parent, SWT.NULL);

            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            client.setLayout(layout);

            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            client.setLayoutData(gd);

            buttonSelectAll = new Button(client, SWT.PUSH);
            buttonSelectAll.setText("Select All");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            buttonSelectAll.setLayoutData(gd);
            buttonSelectAll.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    tableViewer.setCheckedElements(keys);
                }
            });

            buttonDeselectAll = new Button(client, SWT.PUSH);
            buttonDeselectAll.setText("Deselect All");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            buttonDeselectAll.setLayoutData(gd);
            buttonDeselectAll.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    tableViewer.setCheckedElements(new Object[] {});
                }
            });
        }

        @Override
        protected void okPressed() {
            compoundCmd = new EObjectNonNotifyingCompoundCommand(fPropertiesElement, "Add Properties");

            for(Object o : tableViewer.getCheckedElements()) {
                IProperty property = IArchimateFactory.eINSTANCE.createProperty();
                property.setKey((String)o);
                compoundCmd.add(new NewPropertyCommand(fPropertiesElement.getProperties(), property, -1));
            }

            super.okPressed();
        }

        Command getCommand() {
            return compoundCmd;
        }

        @Override
        protected Point getDefaultDialogSize() {
            return new Point(400, 250);
        }
    }

}