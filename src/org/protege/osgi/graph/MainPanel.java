package org.protege.osgi.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.collections15.Transformer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;

public class MainPanel extends JPanel {
    private static final int CLASS = 0;
    private static final int PACKAGE = 1;
    
    private BundleContext context;
    private PackageAdmin packages;
    
    private JComboBox classOrPackageBox;
    private JTextField classOrPackageText;
    private VisualizationViewer<Bundle,Edge> graphView;
    private JComboBox layoutComboBox;

    public MainPanel(BundleContext context, PackageAdmin packages) {
        this.context = context;
        this.packages = packages;
        
        setLayout(new BorderLayout());
        
        add(createHeader(), BorderLayout.NORTH);
        add(createFooter(), BorderLayout.SOUTH);
        add(createMainDocument(), BorderLayout.CENTER);

    }
    
    private JComponent createHeader() {
        JPanel panel = new JPanel();
        
        panel.setLayout(new FlowLayout());
        String[] choices = { "Class", "Package" };
        classOrPackageBox = new JComboBox(choices);
        panel.add(classOrPackageBox);
        
        classOrPackageText = new JTextField();
        JTextField sample = new JTextField("org.protege.osgi.debug.graph.MainPanel");
        classOrPackageText.setPreferredSize(sample.getPreferredSize());
        panel.add(classOrPackageText);
        
        JButton draw = new JButton("Refresh");
        draw.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               refresh();
            } 
        });
        panel.add(draw);
        
        JButton clear = new JButton("Clear");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                classOrPackageText.setText("");
                refresh();
            }
        });
        panel.add(clear);
        return panel;
    }
    
    private JComponent createMainDocument() {
        drawGraph();
        return graphView;
    }
    
    private JComponent createFooter() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        layoutComboBox = new JComboBox(LayoutEnum.getNames());
        layoutComboBox.setSelectedIndex(LayoutEnum.DIRECTED_ACYCLIC_GRAPH_LAYOUT.ordinal());
        layoutComboBox.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               refresh();
            } 
        });
        panel.add(layoutComboBox);
        
        return panel;
    }
    
    private void drawGraph() {
        Layout<Bundle, Edge> layout = buildLayout();
        graphView = new VisualizationViewer<Bundle,Edge>(layout); 
        graphView.setPreferredSize(new Dimension(950, 650));
        graphView.getRenderContext().setVertexLabelTransformer(new OSGiVertexLabelRenderer());
        graphView.getRenderContext().setVertexFillPaintTransformer(new OSGiVertexPaintTransformer());
        graphView.getRenderContext().setEdgeDrawPaintTransformer(new OSGiEdgeTransformer());
        AbstractModalGraphMouse gm = new DefaultModalGraphMouse<Bundle, Edge>();
        graphView.setGraphMouse(gm);
    }
    
    private Layout<Bundle, Edge> buildLayout() {
        int layoutIndex = layoutComboBox.getSelectedIndex();
        LayoutEnum le = LayoutEnum.values()[layoutIndex];
        GraphBuilder builder = new GraphBuilder(context, packages);
        DirectedGraph<Bundle, Edge> graph = builder.getGraph();
        Layout<Bundle, Edge> layout = le.buildLayout(graph);
        layout.setSize(new Dimension(900,600));
        return layout;
    }
    
    private void refresh() {
        graphView.setGraphLayout(buildLayout());
        graphView.repaint();
    }
    
    private Bundle getOwningBundleFromClassInBundle(Bundle b) {
        String className = classOrPackageText.getText();
        if (className == null || className.equals("")) {
            return null;
        }
        try {
            Class c = b.loadClass(className);
            Bundle owner = packages.getBundle(c);
            if (owner == null) {
                return context.getBundle(0);
            }
            else return owner;
        }
        catch (Throwable t) {
            return null;
        }
    }
    
    private static class OSGiVertexLabelRenderer implements Transformer<Bundle, String> {
        
        @SuppressWarnings("unchecked")
        public String transform(Bundle vertex) {
            if (vertex instanceof Bundle) {
                Dictionary<String, String> headers = vertex.getHeaders();
                String name = headers.get(Constants.BUNDLE_NAME);
                return name == null ? vertex.getSymbolicName() : name;
            }
            return null;
        }

    }
    
    private class OSGiVertexPaintTransformer implements Transformer<Bundle, Paint> {
        public Paint transform(Bundle b) {
            Bundle owningBundle;
            if (classOrPackageBox.getSelectedIndex() == PACKAGE || 
            		(owningBundle = getOwningBundleFromClassInBundle(b)) == null) {
                return Color.RED;
            }
            else {
                if (b.equals(owningBundle)) {
                    return Color.BLUE;
                }
                else {
                    return Color.GREEN;
                }
            }
        }
    }
    
    private class OSGiEdgeTransformer implements Transformer<Edge, Paint> {
    	public Paint transform(Edge edge) {
    		String name = classOrPackageText.getText();
    		if (classOrPackageBox.getSelectedIndex() == CLASS) {
    			int index = name.lastIndexOf('.');
    			if (index < 0) {
    				name = "";
    			}
    			else  {
    				name = name.substring(0, index);
    			}
    		}
			for (ExportedPackage export : edge.getPackages()) {
				if (export.getName().equals(name)) {
					return Color.GREEN;
				}
			}
			return Color.RED;
    	}
    }
    
    
}
