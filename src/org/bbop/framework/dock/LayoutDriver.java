package org.bbop.framework.dock;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import org.bbop.framework.GUIComponent;
import org.bbop.framework.GUIComponentFactory;

public interface LayoutDriver {
	public void init();

	public List<Perspective> getPerspectives();

	public void setPerspective(Perspective p);
	
	public Perspective getCurrentPerspective();

	public Perspective savePerspectiveAs(Perspective p, String name);
	
	/** Saves current list of perspectives, and default, to perspective.xml*/
	public void savePerspectives();

	public Perspective getPerspective(String name);
	
	public boolean deletePerspective(Perspective p);

	public boolean importPerspective(File file);

	public String showComponent(GUIComponentFactory factory, GUIComponent target,
			String name, String label, boolean preferFloat, Rectangle floatRect);
	
	public void setComponentLabel(GUIComponent target, String label);
	
	public void setComponentTitlebarTooltip(GUIComponent target, String label);
	
	public void setComponentTitlebarColor(GUIComponent c, Color color);
	
	public void cleanup();
	
	public GUIComponent createMainPanel(String id);

	public String getComponentLabel(GUIComponent c);
	
	public String getComponentTitlebarTooltip(GUIComponent c);
	
	public Color getComponentTitlebarColor(GUIComponent c);
	
	public void addLayoutListener(LayoutListener listener);

	public void removeLayoutListener(LayoutListener listener);
	
	public boolean isFloating(GUIComponent c);
	
	public void setFloating(GUIComponent c, boolean floating);
	
	/**
	 * Restores focus to the last focused child component or, if no child component has had focus, the first focusable component inside the GUIComponent.
	 */
	public void focusComponent(GUIComponent c);
	
	/**
	 * Restore this component to its location before it was minimized or maximized.
	 */
	public void restoreComponent(GUIComponent c);
	
	//Layout lock
  public void lockDockingPanels();
  
  public void unlockDockingPanels(); 
  
  /**
   * lockOTE - view/scroll lock
   * Locks the OTE view around a specific path.
   * */
  public void lockOTE();
  
  public void unlockOTE();
  
  /** Whether to save layout on exit - default true */
  public void setSaveLayoutOnExit(boolean saveLayout);
}
