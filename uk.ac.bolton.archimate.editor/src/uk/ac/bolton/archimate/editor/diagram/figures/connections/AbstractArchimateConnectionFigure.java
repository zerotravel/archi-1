/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package uk.ac.bolton.archimate.editor.diagram.figures.connections;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;

import uk.ac.bolton.archimate.editor.diagram.figures.ToolTipFigure;
import uk.ac.bolton.archimate.editor.ui.ArchimateLabelProvider;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateConnection;
import uk.ac.bolton.archimate.model.IRelationship;


/**
 * Abstract implementation of an Archimate connection figure.  Subclasses should decide how to draw the line
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractArchimateConnectionFigure
extends AbstractDiagramConnectionFigure implements IArchimateConnectionFigure {

    public AbstractArchimateConnectionFigure(IDiagramModelArchimateConnection connection) {
        super(connection);
    }
    
    @Override
    public IDiagramModelArchimateConnection getModelConnection() {
        return (IDiagramModelArchimateConnection)super.getModelConnection();
    }

    @Override
    public void highlight(boolean set) {
        if(set) {
            setForegroundColor(ColorConstants.red);
            fLineColor = ColorConstants.red;
            setLineWidth(2);
        }
        else {
            setLineColor();
            setLineWidth();
        }
    }

    @Override
    public IFigure getToolTip() {
        ToolTipFigure toolTipFigure = (ToolTipFigure)super.getToolTip();
        
        if(toolTipFigure == null) {
            return null;
        }
        
        IRelationship relation = getModelConnection().getRelationship();
        
        String text = ArchimateLabelProvider.INSTANCE.getLabel(relation);
        toolTipFigure.setText(text);

        String type = ArchimateLabelProvider.INSTANCE.getDefaultName(relation.eClass());
        toolTipFigure.setType(Messages.AbstractArchimateConnectionFigure_0 + " " + type); //$NON-NLS-1$

        String rubric = ArchimateLabelProvider.INSTANCE.getRelationshipSentence(relation);
        toolTipFigure.setRubric(rubric);

        return toolTipFigure;
    }
    
}
