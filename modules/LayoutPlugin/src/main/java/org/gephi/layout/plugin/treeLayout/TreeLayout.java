/*
Copyright 2013 MFG Labs
Authors : Julian Bilcke, Joachim de Lezardière
Website : http://www.mfglabs.com

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
package org.gephi.layout.plugin.treeLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeData;
import org.gephi.layout.plugin.AbstractLayout;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.openide.util.NbBundle;

/**
 * Algorithm's paper:
 * "Improving Walker's Algorithm to Run in Linear Time"
 * by C. Buchheim, M. Jünger, S. Leipert
 * 
 * Gephi implementation:
 * @author Juilan Bilcke
 */
public class TreeLayout extends AbstractLayout implements Layout {

    //Graph
    protected HierarchicalGraph graph;
    
    //Properties
    private int rootId = 1;
        
    // current state
    private boolean converged;
    private Node root = null;
    
    // computed stats
    private int levels = 0;
   
    private Comparator<Node> nodeComparator = new Comparator<Node>() {
                @Override public int compare(Node one, Node two) {
                    return new Integer(one.getId()).compareTo(new Integer(two.getId()));
                }
    };
    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
    public TreeLayout(LayoutBuilder layoutBuilder) {
        super(layoutBuilder);
    }

    public void resetPropertiesValues() {
        rootId = 1;
    }

    public HierarchicalTreeNodeLayoutData getOrSetLayout(Node n, HierarchicalTreeNodeLayoutData data) {
            if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof HierarchicalTreeNodeLayoutData)) {
                n.getNodeData().setLayoutData(data);
            }
            return n.getNodeData().getLayoutData();
    }
    public void initAlgo() {
       converged = false;
                
        this.graph = graphModel.getHierarchicalGraphVisible();
        graph.readLock();

        for (Node n : graph.getNodes()) {
            getOrSetLayout(n, new HierarchicalTreeNodeLayoutData());
            if (n.getId() == rootId) {
                root = n;
            }
         }
                
        // we first initialize modifiers, threads, and ancestrors
        Edge[] edges = graph.getEdgesAndMetaEdges().toArray();
 
        System.out.println("STEP 1. INITIALIZING.");
        for (Edge E : edges) {
            Node source = E.getSource();
            Node target = E.getTarget();
            System.out.println(" - source: " + source.getId());
            System.out.println(" - target: " + target.getId());
            System.out.println("");
            HierarchicalTreeNodeLayoutData sourceLayoutData = getOrSetLayout(source, new HierarchicalTreeNodeLayoutData());
            HierarchicalTreeNodeLayoutData targetLayoutData = getOrSetLayout(target, new HierarchicalTreeNodeLayoutData());
            
 
            // detect the direction using timestamps
            //System.out.println("source timestamp:  " + source.getNodeData().getAttributes().getValue("timestamp"));
            //System.out.println("target timestamp:  " + source.getNodeData().getAttributes().getValue("timestamp"));
           // Long ts = (Long) source.getAttributes().getValue("timestamp");
            
                targetLayoutData.parent = source;
                targetLayoutData.modifier = 0;
                targetLayoutData.ancestror = target;
                targetLayoutData.thread = root;
                Node[] newChild = { target };
                sourceLayoutData.children = concat(sourceLayoutData.children, newChild); 

                Arrays.sort(sourceLayoutData.children, nodeComparator);

         }

        System.out.println("root: " + root.getId());
        System.out.println("Setting depth for each nodes starting from root..");
        levels = setDepth(root, 0);
        System.out.println("max depth: " + levels);
        graph.readUnlock();
    }

    public int setDepth(Node n, int depth) {
         HierarchicalTreeNodeLayoutData data = n.getNodeData().getLayoutData();
         data.depth = depth;
         int max = depth;
         for (Node child : data.children) {
             int d = setDepth(child, depth + 1);
             if (d > max) max = d;
         }
         return max;
    }
    
    public void goAlgo() {
        this.graph = graphModel.getHierarchicalGraphVisible();
        graph.readLock();
        Node[] nodes = graph.getNodes().toArray();
        Edge[] edges = graph.getEdgesAndMetaEdges().toArray();

        HierarchicalTreeNodeLayoutData rootLayoutData = root.getNodeData().getLayoutData();
        System.out.println("STEP 2. CALLING FIRST WALK");
        firstWalk(root);
        
        System.out.println("STEP 3. CALLING SECOND WALK");
        secondWalk(root, - rootLayoutData.prelim);
        
        System.out.println("STEP 4. copying layout coordinates into nodes coordinates");
        for (Node n : nodes) {
            HierarchicalTreeNodeLayoutData layoutData = n.getNodeData().getLayoutData();
            layoutData.x = 0;
            layoutData.y = 0;
            
            NodeData nodeData = n.getNodeData();
            nodeData.setX(layoutData.x);
            nodeData.setY(layoutData.y);
        }

        graph.readUnlock();
         converged = true;
    }

    private void firstWalk(Node v) {
        System.out.println("calling firstWalk on " + v.getId());
        HierarchicalTreeNodeLayoutData data = v.getNodeData().getLayoutData();
        if (data.children.length == 0) {
            System.out.println("is a leaf");
            data.prelim = 0;
        } else {
            Node defaultAncestror = data.children[0];
            for (Node w : data.children) {
                firstWalk(w);
                apportion(w, defaultAncestror);
            }
            executeShifts(v);
            HierarchicalTreeNodeLayoutData leftMostd = getLeftMost(data.children).getNodeData().getLayoutData();
            HierarchicalTreeNodeLayoutData rightMostd = getRightMost(data.children).getNodeData().getLayoutData();
            float midpoint = 0.5f * ( leftMostd.prelim + rightMostd.prelim );
            
            Node leftSibling = getLeftSibling(v);
            if (leftSibling != null) {
                HierarchicalTreeNodeLayoutData leftSiblingLayoutData = v.getNodeData().getLayoutData();
                data.prelim = leftSiblingLayoutData.prelim + 10.0f;
                data.modifier = data.prelim - midpoint;
            } else {
                data.prelim = midpoint;
            }
        }
    }
     private void secondWalk(Node v, float m) {
         System.out.println(" -> calling secondWalk on " + v.getId() + ", m is: " + m);
        HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
        vd.x = vd.prelim + m;
        vd.y = vd.depth;
        for (Node child : vd.children) {
            secondWalk(child, m + vd.modifier);
        }
        
    }
     
     private Node getLeftSibling(Node v) {
         HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
         if (vd.parent != null) {
            HierarchicalTreeNodeLayoutData pd = vd.parent.getNodeData().getLayoutData();
            Node leftSibling = null;
            for (Node w : pd.children) {
                if (w.getId() == v.getId()) {
                    if (leftSibling != null) {
                        return leftSibling;
                    } else {
                        return null;
                    }
                }
                leftSibling = w;
            }
         }
         return null;
     }
     
     private Node rightMostDescendant(Node v) {
         HierarchicalTreeNodeLayoutData data = v.getNodeData().getLayoutData();

         return null;
     }
     
     private boolean isLeaf(Node v) {
         HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
         return (vd.children.length == 0);
     }
     
    private Node getLeftMost(Node[] nodes) {
        return (nodes.length > 0) ? nodes[0] : null;
    }
    private Node getRightMost(Node[] nodes) {
        return (nodes.length > 0) ? nodes[nodes.length - 1] : null;
    }
   private Node getLeftMost(Node n) {
        HierarchicalTreeNodeLayoutData layoutData = n.getNodeData().getLayoutData();
        return getLeftMost(layoutData.children);
    }
    private Node getRightMost(Node n) {
        HierarchicalTreeNodeLayoutData layoutData = n.getNodeData().getLayoutData();
        return getRightMost(layoutData.children);
    }
    private Node getLeftMostSibling(Node n) {
        HierarchicalTreeNodeLayoutData layoutData = n.getNodeData().getLayoutData();
        if (layoutData.parent == null) return n;
        return getLeftMost(layoutData.parent);
    }
    private Node getRightMostSibling(Node n) {
        HierarchicalTreeNodeLayoutData layoutData = n.getNodeData().getLayoutData();
        if (layoutData.parent == null) return n;
        return getRightMost(layoutData.parent);
    }

    private Node getNodeThread(Node v) {
      HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
      return vd.thread;
    }
    private Node getNextLeft(Node v) {
        System.out.println("getNextLeft("+v.getId()+")");
        Node leftMostChild = getLeftMost(v);
        return (leftMostChild != null) ? leftMostChild : getNodeThread(v);
    }

    private Node getNextRight(Node v) {
        System.out.println("getNextRight("+v.getId()+")");
       Node rightMostChild = getRightMost(v);
        return (rightMostChild != null) ? rightMostChild : getNodeThread(v);
    }

    private void moveSubtree(Node wm, Node wp, float shift) {
        HierarchicalTreeNodeLayoutData wmd = wm.getNodeData().getLayoutData();
        HierarchicalTreeNodeLayoutData wpd = wp.getNodeData().getLayoutData();
        System.out.println("moveSubtree("+wm.getId()+", "+wp.getId()+", "+shift+")");
        /*
        Params wmp = getParams(wm);
        Params wpp = getParams(wp);
        double subtrees = wpp.number - wmp.number;
        wpp.change -= shift/subtrees;
        wpp.shift += shift;
        wmp.change += shift/subtrees;
        wpp.prelim += shift;
        wpp.mod += shift;
        */
    }

    private void executeShifts(Node n) {
        HierarchicalTreeNodeLayoutData nd = n.getNodeData().getLayoutData();
        /*
        double shift = 0, change = 0;
        for ( Node c = (Node)n.getLastChild();
              c != null; c = (Node)c.getPreviousSibling() )
        {
            Params cp = getParams(c);
            cp.prelim += shift;
            cp.mod += shift;
            change += cp.change;
            shift += cp.shift + change;
        }
        */
    }

    private boolean hasChild(Node v, Node candidate) {
        if (v == null) {
            return false;
        }
        HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
        Node candidateParent = getParent(candidate);
        if (candidateParent == null) {
            return false;
        }
        return (v.getId() == candidateParent.getId());
    }
    private Node getParent(Node v) {
        HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
        return vd.parent;
    }
    
    private Node getAncestor(Node vim, Node v, Node defaultAncestror) {
        if (hasChild(getParent(v), getParent(vim))) {
            return getParent(vim);
        } else {
            return defaultAncestror;
        }
    }
 

    private Node apportion(Node v, Node defaultAncestror) {
        HierarchicalTreeNodeLayoutData vd = v.getNodeData().getLayoutData();
        HierarchicalTreeNodeLayoutData defaultAncestrord = defaultAncestror.getNodeData().getLayoutData();
        System.out.println("apportion("+v.getId()+")");

        Node w = getLeftSibling(v);
        if (w != null) { 

            Node     vip, vim, vop, vom;
            float    sip, sim, sop, som;

            vip = vop = v;
            vim = w;
            vom = getLeftMostSibling(vip);

            System.out.println(vom.getId() + " = getLeftSibling("+v.getId()+")");
                                
            HierarchicalTreeNodeLayoutData vipd = vip.getNodeData().getLayoutData(),
                                           vopd = vop.getNodeData().getLayoutData(),
                                           vimd = vim.getNodeData().getLayoutData(),
                                           vomd = vom.getNodeData().getLayoutData();
       
            sip = vipd.modifier;
            sop = vopd.modifier;
            sim = vimd.modifier;
            som = vomd.modifier;

            Node nr = getNextRight(vim);
            Node nl = getNextLeft(vip);
            while ( nr != null && nl != null ) {
                vim = nr;
                vip = nl;
                vom = getNextLeft(vom);
                vop = getNextRight(vop);
                vopd.ancestror = v;
                /*
                float shift = (vimd.prelim + sim) -
                        (vipd.prelim + sip) + spacing(vim,vip,false);
                if ( shift > 0 ) {
                    moveSubtree(getAncestor(vim,v,defaultAncestror), v, shift);
                    sip += shift;
                    sop += shift;
                }
                * */
                sim += vimd.modifier;
                sip += vipd.modifier;
                som += vomd.modifier;
                sop += vopd.modifier;

                nr = getNextRight(vim);
                nl = getNextLeft(vip);
            }
        
            if ( nr != null && getNextRight(vop) == null ) {
                    vopd.thread = nr;
                    vopd.modifier += sim - sop;
             }
             if ( nl != null && getNextLeft(vom) == null ) {
                    vomd.thread = nl;
                    vomd.modifier += sip - som;
                    defaultAncestror = v;
             }
        }

        defaultAncestror = v;
        
        return defaultAncestror;
    }

    public void endAlgo() {
        for (Node n : graph.getNodes()) {
            n.getNodeData().setLayoutData(null);
        }
    }

    @Override
    public boolean canAlgo() {
        return !converged;
    }

    public LayoutProperty[] getProperties() {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        final String TREE_LAYOUT = "Tree Layout";

        try {
            properties.add(LayoutProperty.createProperty(
                    this, Integer.class,
                    NbBundle.getMessage(TreeLayout.class, "treeLayout.root.name"),
                    TREE_LAYOUT,
                    "treeLayout.root.name",
                    NbBundle.getMessage(TreeLayout.class, "treeLayout.root.desc"),
                    "getRoot", "setRoot"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return properties.toArray(new LayoutProperty[0]);
    }


    public Integer getRoot() {
        return rootId;
    }
    public void setRoot(Integer rootId) {
        this.rootId = rootId;
    }

}
