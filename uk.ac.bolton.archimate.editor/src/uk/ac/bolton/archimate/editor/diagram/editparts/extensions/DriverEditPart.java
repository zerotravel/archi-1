/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package uk.ac.bolton.archimate.editor.diagram.editparts.extensions;

import org.eclipse.draw2d.IFigure;

import uk.ac.bolton.archimate.editor.diagram.editparts.AbstractArchimateEditableTextFlowEditPart;
import uk.ac.bolton.archimate.editor.diagram.figures.extensions.DriverFigure;

/**
 * Driver Edit Part
 * 
 * @author Phillip Beauvoir
 */
public class DriverEditPart
extends AbstractArchimateEditableTextFlowEditPart {            
    
    @Override
    protected IFigure createFigure() {
        return new DriverFigure(getModel());
    }
 
}