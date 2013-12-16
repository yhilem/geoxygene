package fr.ign.cogit.geoxygene.appli.layer;

import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;

public abstract class LayerViewGLCanvas extends AWTGLCanvas implements ComponentListener, MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1095977885262623231L; // Serializable UID
    protected LayerViewGLPanel parentPanel = null;
    private final boolean doPaintOverlay = false;
    protected static Logger logger = Logger.getLogger(LayerViewGL1Canvas.class.getName());

    /**
     * Constructor
     * 
     * @param parentPanel
     * @throws LWJGLException
     */
    public LayerViewGLCanvas(LayerViewGLPanel parentPanel) throws LWJGLException {
        super();
        if (parentPanel == null) {
            throw new IllegalArgumentException("invalid null parent Panel for " + this.getClass().getSimpleName());
        }
        this.setParentPanel(parentPanel);
        this.addComponentListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

    }

    /**
     * Set the parent panel
     */
    protected final void setParentPanel(final LayerViewGLPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    /**
     * @return the parentPanel
     */
    public LayerViewGLPanel getParentPanel() {
        return this.parentPanel;
    }

    /**
     * @return true if overlays have to be painted on rendering media
     */
    protected boolean doPaintOverlay() {
        return this.doPaintOverlay;
    }

    public final void doPaint() {
        this.paintGL();
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        if (this.getParentPanel() != null) {
            this.getParentPanel().dispatchEvent(e);
        }
    }

}