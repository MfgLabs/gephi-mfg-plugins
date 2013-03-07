/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2011 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

Portions Copyrighted 2011 Gephi Consortium.
*/
package org.gephi.visualization.selection;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import org.gephi.graph.api.Renderable;
import org.gephi.visualization.VizController;
import org.gephi.visualization.VizModel;
import org.gephi.visualization.apiimpl.GraphDrawable;
import org.gephi.visualization.apiimpl.GraphIO;
import org.gephi.visualization.apiimpl.ModelImpl;
import org.gephi.visualization.api.selection.SelectionArea;
import org.gephi.visualization.api.selection.SelectionManager;
import org.gephi.lib.gleem.linalg.Vecf;

/**
 *
 * @author Mathieu Bastian
 */
public class Cylinder implements SelectionArea {

    //Architecture
    private GraphIO graphIO;
    private GraphDrawable drawable;
    private SelectionManager selectionManager;
    private VizModel vizModel;

    //Variables
    private static final float[] rectPoint = {1, 1};
    private float[] rectangle = new float[2];

    public Cylinder() {
        graphIO = VizController.getInstance().getGraphIO();
        drawable = VizController.getInstance().getDrawable();
        selectionManager = VizController.getInstance().getSelectionManager();
        vizModel = VizController.getInstance().getVizModel();
    }

    public float[] getSelectionAreaRectancle() {
        float diameter = selectionManager.getMouseSelectionDiameter();
        if (diameter == 1) {
            //Point
            return rectPoint;
        } else {
            float size;
            if (selectionManager.isMouseSelectionZoomProportionnal()) {
                size = diameter * (float) Math.abs(drawable.getDraggingMarkerX());
            } else {
                size = diameter;
            }
            rectangle[0] = size;
            rectangle[1] = size;
            return rectangle;
        }
    }

    public float[] getSelectionAreaCenter() {
        return null;
    }

    public boolean mouseTest(Vecf distanceFromMouse, ModelImpl object) {
        float diameter = selectionManager.getMouseSelectionDiameter();
        if (diameter == 1) {
            //Point
            return object.selectionTest(distanceFromMouse, 0);
        } else {
            if (selectionManager.isMouseSelectionZoomProportionnal()) {
                return object.selectionTest(distanceFromMouse, diameter * (float) Math.abs(drawable.getDraggingMarkerX()));
            } else {
                return object.selectionTest(distanceFromMouse, diameter);
            }
        }
    }

    public boolean select(Renderable object) {
        return true;
    }

    public boolean unselect(Renderable object) {
        return true;
    }

    public void drawArea(GL2 gl, GLU glu) {
        float diameter = selectionManager.getMouseSelectionDiameter();
        if (diameter == 1) {
            //Point
        } else {
            //Cylinder
            float radius;
            boolean lighting = vizModel.isLighting();
            if (selectionManager.isMouseSelectionZoomProportionnal()) {
                radius = (float) (diameter * Math.abs(drawable.getDraggingMarkerX()));      //Proportionnal
            } else {
                radius = diameter;      //Constant
            }
            float[] mousePosition = graphIO.getMousePosition();
            float vectorX, vectorY, vectorX1 = mousePosition[0], vectorY1 = mousePosition[1];
            double angle;

            if (lighting) {
                gl.glDisable(GL2.GL_LIGHTING);
            }
            gl.glColor4f(0f, 0f, 0f, 0.2f);
            gl.glBegin(GL2.GL_TRIANGLES);
            for (int i = 0; i <= 360; i++) {
                angle = i / 57.29577957795135f;
                vectorX = mousePosition[0] + (radius * (float) Math.sin(angle));
                vectorY = mousePosition[1] + (radius * (float) Math.cos(angle));
                gl.glVertex2f(mousePosition[0], mousePosition[1]);
                gl.glVertex2f(vectorX1, vectorY1);
                gl.glVertex2f(vectorX, vectorY);
                vectorY1 = vectorY;
                vectorX1 = vectorX;
            }
            gl.glEnd();
            if (lighting) {
                gl.glEnable(GL2.GL_LIGHTING);
            }
        }
    }

    public boolean isEnabled() {
        return selectionManager.isSelectionEnabled();
    }

    public boolean blockSelection() {
        return false;
    }
}
