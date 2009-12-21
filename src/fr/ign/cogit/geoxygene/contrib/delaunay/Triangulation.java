/*
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO specifications for
 * the development and deployment of geographic (GIS) applications. It is a open source
 * contribution of the COGIT laboratory at the Institut Géographique National (the French
 * National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut Géographique National
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this library (see file LICENSE if present); if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */

package fr.ign.cogit.geoxygene.contrib.delaunay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Noeud;
import fr.ign.cogit.geoxygene.feature.FT_Feature;
import fr.ign.cogit.geoxygene.feature.Population;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPositionList;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Envelope;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_Point;
import fr.ign.cogit.geoxygene.spatial.geomroot.GM_Object;
import fr.ign.cogit.geoxygene.util.index.Tiling;

/**
 * Classe mère de la triangulation construite sur la biblioth�que Triangle de Jonathan Richard Shewchuk.
 * Triangulation class used on top of Jonathan Richard Shewchuk's Triangle library.
 * @author Bonin
 * @author Julien Perret
 * @version 1.1
 */

public class Triangulation extends CarteTopo{
    static Logger logger=Logger.getLogger(Triangulation.class.getName());

    /**
     * 
     */
    public Triangulation() {
	this.ojbConcreteClass = this.getClass().getName(); // nécessaire pour ojb
	this.setPersistant(false);
	Population<ArcDelaunay> arcs = new Population<ArcDelaunay>(false, "Arc", ArcDelaunay.class,true);
	this.addPopulation(arcs);
	Population<NoeudDelaunay> noeuds = new Population<NoeudDelaunay>(false, "Noeud", NoeudDelaunay.class,true);
	this.addPopulation(noeuds);
	Population<TriangleDelaunay> faces = new Population<TriangleDelaunay>(false, "Face", TriangleDelaunay.class,true);
	this.addPopulation(faces);
    }

    /**
     * @param nom_logique
     */
    public Triangulation(String nom_logique) {
	this.ojbConcreteClass = this.getClass().getName(); // nécessaire pour ojb
	this.setNom(nom_logique);
	this.setPersistant(false);
	Population<ArcDelaunay> arcs = new Population<ArcDelaunay>(false, "Arc", ArcDelaunay.class,true);
	this.addPopulation(arcs);
	Population<NoeudDelaunay> noeuds = new Population<NoeudDelaunay>(false, "Noeud", NoeudDelaunay.class,true);
	this.addPopulation(noeuds);
	Population<TriangleDelaunay> faces = new Population<TriangleDelaunay>(false, "Face", TriangleDelaunay.class,true);
	this.addPopulation(faces);
    }

    private Triangulateio jin = new Triangulateio();
    private Triangulateio jout = new Triangulateio();
    private Triangulateio jvorout = new Triangulateio();
    private String options = null;

    Population<Arc> voronoiEdges = new Population<Arc>();
	/** Population des arcs de voronoi de la triangulation. */
	public Population<Arc> getPopVoronoiEdges() {return this.voronoiEdges;}
    Population<Noeud> voronoiVertices = new Population<Noeud>();
	/** Population des noeuds de voronoi de la triangulation. */
	public Population<Noeud> getPopVoronoiVertices() {return this.voronoiVertices;}

    /**
     * Convert the node collection into an array
     */
    private void convertJin() {
    	int i;
    	NoeudDelaunay node;
    	DirectPosition coord;
    	List<Noeud> noeuds = new ArrayList<Noeud>(this.getListeNoeuds());
    	this.jin.numberofpoints = noeuds.size();
    	this.jin.pointlist = new double[2*this.jin.numberofpoints];
    	for (i=0; i<noeuds.size(); i++) {
    		node = (NoeudDelaunay) noeuds.get(i);
    		coord = node.getGeometrie().getPosition();
    		this.jin.pointlist[2*i]=coord.getX();
    		this.jin.pointlist[2*i+1]=coord.getY();
    	}
    }

    /**
     * Convert the edges into an array
     */
    private void convertJinSegments() {
    	int i;
    	ArrayList<FT_Feature> noeuds = new ArrayList<FT_Feature>(this.getListeNoeuds());
    	ArrayList<FT_Feature> aretes = new ArrayList<FT_Feature>(this.getListeArcs());
    	this.jin.numberofsegments = aretes.size();
    	this.jin.segmentlist = new int[2*this.jin.numberofsegments];
    	for (i=0; i<this.jin.numberofsegments; i++) {
    		this.jin.segmentlist[2*i]=noeuds.indexOf(((ArcDelaunay)aretes.get(i)).getNoeudIni());
    		this.jin.segmentlist[2*i+1]=noeuds.indexOf(((ArcDelaunay)aretes.get(i)).getNoeudFin());
    	}
    }

    /**
     * Convert back the result into vertices, edges and triangles.
     */
    private void convertJout() {
    	if (logger.isDebugEnabled()) logger.debug("début de l'export des données");
    	try {
    		if (logger.isDebugEnabled()) logger.debug("début de l'export des noeuds");
    		for (int i=this.jin.numberofpoints; i<this.jout.numberofpoints; i++) {
    			this.getPopNoeuds().nouvelElement().setCoord(new DirectPosition(this.jout.pointlist[2*i],this.jout.pointlist[2*i+1]));
    		}

    		ArrayList<FT_Feature> noeuds = new ArrayList<FT_Feature>(this.getListeNoeuds());

    		Class<?>[] signaturea = {this.getPopNoeuds().getClasse(),this.getPopNoeuds().getClasse()};
    		Object[] parama = new Object[2];

    		if (logger.isDebugEnabled()) logger.debug("début de l'export des arcs");	
    		for (int i=0; i<this.jout.numberofedges; i++) {
    			parama[0] = noeuds.get(this.jout.edgelist[2*i]);
    			parama[1] = noeuds.get(this.jout.edgelist[2*i+1]);
    			this.getPopArcs().nouvelElement(signaturea,parama);
    		}

    		Class<?> [] signaturef = {this.getPopNoeuds().getClasse(),this.getPopNoeuds().getClasse(),this.getPopNoeuds().getClasse()};
    		Object[] paramf = new Object[3];

    		if (logger.isDebugEnabled()) logger.debug("début de l'export des triangles");
    		for (int i=0; i<this.jout.numberoftriangles; i++) {
    			paramf[0] = noeuds.get(this.jout.trianglelist[3*i]);
    			paramf[1] = noeuds.get(this.jout.trianglelist[3*i+1]);
    			paramf[2] = noeuds.get(this.jout.trianglelist[3*i+2]);
    			this.getPopFaces().nouvelElement(signaturef,paramf).setId(i);
    		}
    		if (this.getOptions().indexOf('v') != -1) {
        		if (logger.isDebugEnabled()) logger.debug("début de l'export du diagramme de Voronoi");

        		GM_Envelope envelope = this.getPopNoeuds().envelope();
            		envelope.expandBy(100);
            		this.voronoiVertices.initSpatialIndex(Tiling.class, true, envelope, 10);

    			// l'export du diagramme de voronoi
				for (int i=0; i<this.jvorout.numberofpoints; i++) {
					this.voronoiVertices.add(new Noeud(new GM_Point(new DirectPosition(jvorout.pointlist[2*i],jvorout.pointlist[2*i+1]))));
				}
				for (int i=0; i<this.jvorout.numberofedges; i++) {
					int indexIni = this.jvorout.edgelist[2*i];
					int indexFin = this.jvorout.edgelist[2*i+1];
					if (indexFin==-1) {
						// infinite edge
						double vx = this.jvorout.normlist[2*i];
						double vy = this.jvorout.normlist[2*i+1];
						Noeud c1 = this.voronoiVertices.getElements().get(indexIni);
						Noeud c2 = new Noeud();
						double vectorSize = 10000000;
						c2.setGeometrie(new GM_Point(new DirectPosition(c1.getGeometrie().getPosition().getX()+vectorSize*vx,c1.getGeometrie().getPosition().getY()+vectorSize*vy)));
						GM_LineString line = new GM_LineString(new DirectPositionList(Arrays.asList(c1.getGeometrie().getPosition(),c2.getGeometrie().getPosition())));
						GM_Object intersection = line.intersection(envelope.getGeom());
						DirectPositionList list = intersection.coord();
						if (list.size()>1) c2.setGeometrie(list.get(1).toGM_Point());
						indexFin = this.voronoiVertices.size();
						this.voronoiVertices.add(c2);
					}
					this.voronoiEdges.add(new Arc(this.voronoiVertices.getElements().get(indexIni),this.voronoiVertices.getElements().get(indexFin)));
				}
    		}
    	}
    	catch (Exception e) {e.printStackTrace();}
    	if (logger.isDebugEnabled()) logger.debug("Fin de l'export des données");
    }
    
    ///méthode de triangulation proprment dite en C - va chercher la biblioth�que C (dll/so)
    private native void trianguleC(String trianguleOptions, Triangulateio trianguleJin, Triangulateio trianguleJout, Triangulateio trianguleJvorout);
    static {System.loadLibrary("trianguledll");} //$NON-NLS-1$

    /**
     * Run the triangulation with the given parameters.
     * Lance la triangulation avec les paramètres donnés
     * @param trianguleOptions paramètres de la triangulation :
     * <ul> 
	 * <li> <b>z Zero:</b> points are numbered from zero
	 * <li> <b>e Edges:</b> export edges
     * <li> <b>c Convex Hull:</b> Creates segments on the convex hull of the triangulation. 
     * If you are triangulating a vertex set, this switch causes the creation 
     * of all edges in the convex hull.  If you are
     * triangulating a PSLG, this switch specifies that the whole convex
     * hull of the PSLG should be triangulated, regardless of what
     * segments the PSLG has.  If you do not use this switch when
     * triangulating a PSLG, it is assumed that you have identified the
     * region to be triangulated by surrounding it with segments of the
     * input PSLG.  Beware:  if you are not careful, this switch can cause
     * the introduction of an extremely thin angle between a PSLG segment
     * and a convex hull segment, which can cause overrefinement (and
     * possibly failure if Triangle runs out of precision).  If you are
     * refining a mesh, the -c switch works differently; it generates the
     * set of boundary edges of the mesh.
     * <li> <b>B Boundary:</b> No boundary markers in the output.
     * <li> <b>Q Quiet:</b> Suppresses all explanation of what Triangle is doing,
     * unless an error occurs. 
     * <li> v for exporting a Voronoi diagram. 
     * This implementation does not use exact arithmetic to compute the Voronoi
     * vertices, and does not check whether neighboring vertices are identical.
     * Be forewarned that if the Delaunay triangulation is degenerate or
     * near-degenerate, the Voronoi diagram may have duplicate vertices,
     * crossing edges, or infinite rays whose direction vector is zero.
     * The result is a valid Voronoi diagram only if Triangle's output is a true
     * Delaunay triangulation.  The Voronoi output is usually meaningless (and
     * may contain crossing edges and other pathology) if the output is a CDT or
     * CCDT, or if it has holes or concavities.  If the triangulation is convex
     * and has no holes, this can be fixed by using the -L switch to ensure a
     * conforming Delaunay triangulation is constructed.
     * <li> p for reading a Planar Straight Line Graph
     * <li> r for refining a previously generated mesh
     * <li> q for Quality mesh generation by my variant of Jim Ruppert's 
     * Delaunay refinement algorithm.  Adds vertices to the mesh to ensure that no
     * angles smaller than 20 degrees occur.  An alternative minimum angle 
     * may be specified after the `q'.  If the minimum angle is 20.7
     * degrees or smaller, the triangulation algorithm is mathematically 
     * guaranteed to terminate (assuming infinite precision arithmetic--
     * Triangle may fail to terminate if you run out of precision).  
     * In practice, the algorithm often succeeds for minimum angles up to
     * 33.8 degrees.  For some meshes, however, it may be necessary to 
     * reduce the minimum angle to avoid problems associated with
     * insufficient floating-point precision.  The specified angle may 
     * include a decimal point.
     * <li> V Verbose: Gives detailed information about what Triangle is doing.
     * Add more `V's for increasing amount of detail.  `-V' gives
     * information on algorithmic progress and more detailed statistics.
     * `-VV' gives vertex-by-vertex details, and prints so much that
     * Triangle runs much more slowly. `-VVVV' gives information only
     * a debugger could love.
     * </ul>
     * @throws Exception
     */
    public void triangule(String trianguleOptions) throws Exception {
    	if (this.getPopNoeuds().size()<3) {
    		logger.error("Triangulation annul�e : "+this.getPopNoeuds().size()+" points (3 points au moins)");
    		return;
    	}
    	if (logger.isDebugEnabled()) logger.debug("Triangulation commenc�e avec les options "+trianguleOptions);
    	this.setOptions(trianguleOptions);
    	this.convertJin();
    	if (trianguleOptions.indexOf('p') != -1) {
    		this.convertJinSegments();
    		this.getPopArcs().setElements(new ArrayList<Arc>());
    	}
    	if (this.getOptions().indexOf('v') != -1) {
    		trianguleC(trianguleOptions, this.jin, this.jout, this.jvorout);
    	} else {
    		trianguleC(trianguleOptions, this.jin, this.jout, null);
    	}
    	convertJout();
    	if (logger.isDebugEnabled()) logger.debug("Triangulation termin�e");
    }

    /**
     * Run the triangulation with default parameters:
	 * <ul> 
	 * <li> <b>z Zero:</b> points are numbered from zero
	 * <li> <b>e Edges:</b> export edges
     * <li> <b>c Convex Hull:</b> Creates segments on the convex hull of the triangulation. 
     * If you are triangulating a vertex set, this switch causes the creation 
     * of all edges in the convex hull.  If you are
     * triangulating a PSLG, this switch specifies that the whole convex
     * hull of the PSLG should be triangulated, regardless of what
     * segments the PSLG has.  If you do not use this switch when
     * triangulating a PSLG, it is assumed that you have identified the
     * region to be triangulated by surrounding it with segments of the
     * input PSLG.  Beware:  if you are not careful, this switch can cause
     * the introduction of an extremely thin angle between a PSLG segment
     * and a convex hull segment, which can cause overrefinement (and
     * possibly failure if Triangle runs out of precision).  If you are
     * refining a mesh, the -c switch works differently; it generates the
     * set of boundary edges of the mesh.
     * <li> <b>B Boundary:</b> No boundary markers in the output.
     * <li> <b>Q Quiet:</b> Suppresses all explanation of what Triangle is doing,
     * unless an error occurs. 
	 * </ul>
     * @throws Exception
     */
    public void triangule() throws Exception{this.triangule("czeBQ");} //$NON-NLS-1$

    /**
	 * Set the triangulation options
	 * @param options triangulation options.
	 * @see #triangule(String)
     */
    public void setOptions(String options) {this.options = options;}

    /**
     * @return the options
     */
    public String getOptions() {return this.options;}
}