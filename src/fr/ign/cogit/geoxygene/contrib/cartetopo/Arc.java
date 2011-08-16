/*
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO
 * specifications for the development and deployment of geographic (GIS)
 * applications. It is a open source contribution of the COGIT laboratory at the
 * Institut Géographique National (the French National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut Géographique National
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library (see file LICENSE if present); if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package fr.ign.cogit.geoxygene.contrib.cartetopo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import fr.ign.cogit.geoxygene.api.feature.IPopulation;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPositionList;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IPoint;
import fr.ign.cogit.geoxygene.contrib.geometrie.Distances;
import fr.ign.cogit.geoxygene.contrib.geometrie.Operateurs;
import fr.ign.cogit.geoxygene.contrib.geometrie.Rectangle;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPositionList;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_Point;

/**
 * Classe des arcs de la carte topo. Les arcs ont pour géométrie une
 * GM_LineString, et peuvent être orientés. Des méthodes sont prévues pour une
 * gestion de réseau, de graphe, et de carte topologique.
 * 
 * English: arcs of a topological map
 * @author Sébastien Mustière
 * @author Olivier Bonin
 * @author Julien Perret
 * @author Jean-François Girres
 */

public class Arc extends ElementCarteTopo {
  static Logger logger = Logger.getLogger(Arc.class.getName());

  public Arc() {
    super();
  }

  public Arc(Noeud ini, Noeud fin) {
    super();
    this.setNoeudIni(ini);
    this.setNoeudFin(fin);
    this.setGeometrie(new GM_LineString(new DirectPositionList(Arrays.asList(
        ini.getGeometrie().getPosition(), fin.getGeometrie().getPosition()))));
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // géométrie
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  /** Renvoie le ILineString qui définit la géométrie de self */
  public ILineString getGeometrie() {
    return (ILineString) this.geom;
  }

  /** définit le ILineString qui définit la géométrie de self */
  public void setGeometrie(ILineString geometrie) {
    this.setGeom(geometrie);
  }

  /** Renvoie la liste de IDirectPosition qui d�finit les coordonn�es de self */
  public IDirectPositionList getCoord() {
    return this.geom.coord();
  }

  /** définit la liste de IDirectPosition qui définit les coordonnées de self */
  public void setCoord(IDirectPositionList dpl) {
    this.geom = new GM_LineString(dpl);
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Gestion de la topologie arc / noeuds
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  private Noeud noeudIni;

  /** Renvoie le noeud initial de self */
  public Noeud getNoeudIni() {
    return this.noeudIni;
  }

  /**
   * définit le noeud initial de self. NB: met à jour la relation inverse
   * "sortants" de noeud
   */
  public void setNoeudIni(Noeud noeud) {
    if (this.getNoeudIni() != null) {
      this.getNoeudIni().getSortants().remove(this);
    }
    if (noeud != null) {
      this.noeudIni = noeud;
      if (!noeud.getSortants().contains(this)) {
        noeud.addSortant(this);
      }
    } else {
      this.noeudIni = null;
    }
  }

  private Noeud noeudFin;

  /** Renvoie le noeud final de self */
  public Noeud getNoeudFin() {
    return this.noeudFin;
  }

  /**
   * définit le noeud final de self. NB: met à jour la relation inverse
   * "entrants" de noeud
   */
  public void setNoeudFin(Noeud noeud) {
    if (this.getNoeudFin() != null) {
      this.getNoeudFin().getEntrants().remove(this);
    }
    if (noeud != null) {
      this.noeudFin = noeud;
      if (!noeud.getEntrants().contains(this)) {
        noeud.addEntrant(this);
      }
    } else {
      this.noeudFin = null;
    }
  }

  /** Renvoie le noeud initial et final de self */
  public List<Noeud> noeuds() {
    List<Noeud> noeuds = new ArrayList<Noeud>();
    if (!(this.noeudIni == null)) {
      noeuds.add(this.noeudIni);
    }
    if (!(this.noeudFin == null)) {
      noeuds.add(this.noeudFin);
    }
    return noeuds;
  }

  /**
   * Projete le point P sur l'arc et découpe l'arc en 2 avec ce point projeté.
   * NB: si la projection tombe sur une extrémité de l'arc : ne fait rien.
   * @param P point projeté sur l'arc this afin de le découper
   * @return la liste des arcs créés
   */
  public void projeteEtDecoupe(IPoint P) {
    IDirectPositionList listePoints = this.getGeometrie().coord();
    IDirectPositionList ptsAvant, ptsApres;
    Arc arcAvant, arcApres;
    double d, dmin;
    IDirectPosition pt, ptmin;
    int positionMin = 0;
    Noeud nouveauNoeud;

    if (this.getCarteTopo() == null) {
      return;
    }

    IPopulation<Noeud> popNoeuds = this.getCarteTopo().getPopNoeuds();
    IPopulation<Arc> popArcs = this.getCarteTopo().getPopArcs();
    int i;

    if (listePoints.size() < 2) {
      return;
    }
    ptmin = Operateurs.projection(P.getPosition(), listePoints.get(0),
        listePoints.get(1));
    dmin = Distances.distance(P.getPosition(), ptmin);
    for (i = 1; i < listePoints.size() - 1; i++) {
      pt = Operateurs.projection(P.getPosition(), listePoints.get(i),
          listePoints.get(i + 1));
      d = Distances.distance(P.getPosition(), pt);
      if (d < dmin) {
        ptmin = pt;
        dmin = d;
        positionMin = i;
      }
    }
    if (Distances.distance(ptmin, listePoints.get(0)) == 0) {
      return;
    }
    if (Distances.distance(ptmin, listePoints.get(listePoints.size() - 1)) == 0) {
      return;
    }

    // cr�ation du nouveau noeud
    nouveauNoeud = popNoeuds.nouvelElement();
    nouveauNoeud.setGeometrie(new GM_Point(ptmin));

    // cr�ation des nouveaux arcs
    ptsAvant = new DirectPositionList();
    ptsApres = new DirectPositionList();

    for (i = 0; i <= positionMin; i++) {
      ptsAvant.add(listePoints.get(i));
    }
    ptsAvant.add(ptmin);
    arcAvant = popArcs.nouvelElement();
    arcAvant.setGeometrie(new GM_LineString(ptsAvant));

    if (Distances.distance(ptmin, listePoints.get(positionMin + 1)) != 0) {
      ptsApres.add(ptmin);
    }
    for (i = positionMin + 1; i < listePoints.size(); i++) {
      ptsApres.add(listePoints.get(i));
    }
    arcApres = popArcs.nouvelElement();
    arcApres.setGeometrie(new GM_LineString(ptsApres));

    // instanciation de la topologie et des attributs
    this.getNoeudIni().getSortants().remove(this);
    arcAvant.setNoeudIni(this.getNoeudIni());
    arcAvant.setNoeudFin(nouveauNoeud);
    arcAvant.setCorrespondants(this.getCorrespondants());
    arcAvant.setOrientation(this.getOrientation());

    arcApres.setNoeudIni(nouveauNoeud);
    this.getNoeudFin().getEntrants().remove(this);
    arcApres.setNoeudFin(this.getNoeudFin());
    arcApres.setCorrespondants(this.getCorrespondants());
    arcApres.setOrientation(this.getOrientation());

    // destruction de l'ancien arc
    this.setNoeudIni(null);
    this.setNoeudFin(null);
    popArcs.enleveElement(this);
  }

  /**
   * Projete le point P sur l'arc et découpe l'arc en 2 avec ce noeud projet�.
   * NB: si la projection tombe sur une extrémité de l'arc : ne fait rien. TODO
   * ATTENTION : il reste du nettoyage à faire !!!
   * @param n noeud projeté sur l'arc this afin de le d�couper
   */
  public void projeteEtDecoupe(Noeud n) {
    IDirectPositionList listePoints = this.getGeometrie().coord();
    IDirectPositionList ptsAvant, ptsApres;
    Arc arcAvant, arcApres;
    double d, dmin;
    IDirectPosition pt, ptmin;
    int positionMin = 0;

    if (this.getCarteTopo() == null) {
      return;
    }

    IPopulation<Arc> popArcs = this.getCarteTopo().getPopArcs();
    int i;

    if (listePoints.size() < 2) {
      return;
    }
    ptmin = Operateurs.projection(n.getGeometrie().getPosition(), listePoints
        .get(0), listePoints.get(1));
    dmin = Distances.distance(n.getGeometrie().getPosition(), ptmin);
    for (i = 1; i < listePoints.size() - 1; i++) {
      pt = Operateurs.projection(n.getGeometrie().getPosition(), listePoints
          .get(i), listePoints.get(i + 1));
      d = Distances.distance(n.getGeometrie().getPosition(), pt);
      if (d < dmin) {
        ptmin = pt;
        dmin = d;
        positionMin = i;
      }
    }
    if (Distances.distance(ptmin, listePoints.get(0)) == 0) {
      return;
    }
    if (Distances.distance(ptmin, listePoints.get(listePoints.size() - 1)) == 0) {
      return;
    }

    // modification de la géométrie du noeud et de ses arcs
    for (Arc arc : n.arcs()) {
      if (arc.getGeometrie().getControlPoint(0).equals(
          n.getGeometrie().getPosition())) {
        arc.getGeometrie().setControlPoint(0, ptmin);
      } else {
        arc.getGeometrie().setControlPoint(
            arc.getGeometrie().sizeControlPoint() - 1, ptmin);
      }
    }
    n.setGeometrie(new GM_Point(ptmin));

    // cr�ation des nouveaux arcs
    ptsAvant = new DirectPositionList();
    ptsApres = new DirectPositionList();

    for (i = 0; i <= positionMin; i++) {
      ptsAvant.add(listePoints.get(i));
    }
    ptsAvant.add(ptmin);
    arcAvant = popArcs.nouvelElement();
    arcAvant.setGeometrie(new GM_LineString(ptsAvant));

    if (Distances.distance(ptmin, listePoints.get(positionMin + 1)) != 0) {
      ptsApres.add(ptmin);
    }
    for (i = positionMin + 1; i < listePoints.size(); i++) {
      ptsApres.add(listePoints.get(i));
    }
    arcApres = popArcs.nouvelElement();
    arcApres.setGeometrie(new GM_LineString(ptsApres));

    // instanciation de la topologie et des attributs
    this.getNoeudIni().getSortants().remove(this);
    arcAvant.setNoeudIni(this.getNoeudIni());
    arcAvant.setNoeudFin(n);
    arcAvant.setCorrespondants(this.getCorrespondants());
    arcAvant.setOrientation(this.getOrientation());

    arcApres.setNoeudIni(n);
    this.getNoeudFin().getEntrants().remove(this);
    arcApres.setNoeudFin(this.getNoeudFin());
    arcApres.setCorrespondants(this.getCorrespondants());
    arcApres.setOrientation(this.getOrientation());

    // destruction de l'ancien arc
    this.setNoeudIni(null);
    this.setNoeudFin(null);
    popArcs.enleveElement(this);
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Gestion de la topologie arc / faces
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  /** Renvoie la face à gauche et à droite de self */
  public List<Face> faces() {
    List<Face> faces = new ArrayList<Face>();
    if (this.getFaceGauche() != null) {
      faces.add(this.getFaceGauche());
    }
    if (this.getFaceDroite() != null) {
      faces.add(this.getFaceDroite());
    }
    return faces;
  }

  private Face faceGauche;

  /** Renvoie la face à gauche de self */
  public Face getFaceGauche() {
    return this.faceGauche;
  }

  /**
   * définit la face à gauche de self. NB: met à jour la relation inverse
   * "arcs directs" de face
   */
  public void setFaceGauche(Face face) {
    if (face != null) {
      this.faceGauche = face;
      if (!face.getArcsDirects().contains(this)) {
        face.addArcDirect(this);
      }
    } else {
      if (this.getFaceGauche() != null) {
        this.getFaceGauche().getArcsDirects().remove(face);
      }
      this.faceGauche = null;
    }
  }

  private Face faceDroite;

  /** Renvoie la face à droite de self */
  public Face getFaceDroite() {
    return this.faceDroite;
  }

  /**
   * définit la face à droite de self. NB: met à jour la relation inverse
   * "arcs indirects" de face
   */
  public void setFaceDroite(Face face) {
    if (face != null) {
      this.faceDroite = face;
      if (!face.getArcsIndirects().contains(this)) {
        face.addArcIndirect(this);
      }
    } else {
      if (this.getFaceDroite() != null) {
        this.getFaceDroite().getArcsIndirects().remove(face);
      }
      this.faceDroite = null;
    }
  }

  /**
   * Recherche du cycle du réseau à droite de l'arc en se basant sur la
   * topologie de RESEAU uniquement. NB: la liste retournée est égale à null si
   * on n'a pas trouvé de cycle (cas pouvant arriver si la topologie arcs/noeuds
   * n'est pas complète. NB: ne nécessite PAS d'avoir une topologie arcs/faces
   * instanciée. NB: nécessite d'avoir une topologie arcs/noeuds instanciée. NB:
   * un cycle passe 2 fois (une fois dans chaque sens) par les cul-de-sac si il
   * y en a.
   * @return un cycle du réseau. Un cycle contient :
   *         <ul>
   *         <li>la liste des arcs dans l'ordre de parcours du cycle. Cette
   *         Liste est classée dans le sens anti-trigonometrique (sauf pour la
   *         face exterieure). (liste de type "ArrayList", contenant elle-même
   *         des Arcs).
   *         <li>la liste des orientations des arc : true si l'arc à sa face à
   *         gauche, false sinon. (liste de type "ArrayList", contenant
   *         elle-même des objets Booleans).
   *         <li>la géométrie du polygone faisant le tour du cycle (de type
   *         IPolygon).
   *         </ul>
   */
  public Cycle cycleADroite() {
    Arc arcEnCours;
    boolean sensEnCours;
    List<Object> arcOriente;
    GM_LineString contour = new GM_LineString();
    ;
    List<Arc> arcsDuCycle = new ArrayList<Arc>();
    List<Boolean> orientationsDuCycle = new ArrayList<Boolean>();
    int i;

    // intialisation avec le premier arc du cycle (this) qui est par d�finition
    // dans le bon sens
    arcEnCours = this;
    sensEnCours = true;

    // on parcours le cycle dans le sens anti-trigonometrique,
    // jusqu'à revenir sur this en le parcourant dans le bon sens
    // (pr�cision utile � la gestion des cul-de-sac).

    while (true) {
      // ajout de l'arc en cours au cycle...
      arcsDuCycle.add(arcEnCours);
      orientationsDuCycle.add(new Boolean(sensEnCours));

      if (sensEnCours) { // arc dans le bon sens
        for (i = 0; i < arcEnCours.getGeometrie().sizeControlPoint() - 1; i++) {
          contour.addControlPoint(arcEnCours.getGeometrie().getControlPoint(i));
        }
        arcOriente = arcEnCours.arcSuivantFin();
      } else { // arc dans le sens inverse
        for (i = arcEnCours.getGeometrie().sizeControlPoint() - 1; i > 0; i--) {
          contour.addControlPoint(arcEnCours.getGeometrie().getControlPoint(i));
        }
        arcOriente = arcEnCours.arcSuivantDebut();
      }
      if (arcOriente == null) {
        return null;
      }

      // au suivant...

      arcEnCours = (Arc) arcOriente.get(0); // l'arc
      sensEnCours = !((Boolean) arcOriente.get(1)).booleanValue(); // le sens de
                                                                   // l'arc par
                                                                   // rapport au
                                                                   // cycle

      // c'est fini ?
      if (arcEnCours == this && sensEnCours) {
        break;
      }
    }

    // ajout du dernier point pour finir la boucle du polygone
    contour.addControlPoint(contour.startPoint());
    return new Cycle(arcsDuCycle, orientationsDuCycle, contour, false);
  }

  /**
   * Recherche du cycle du réseau à gauche de l'arc en se basant sur la
   * topologie de RESEAU uniquement. NB: la liste retournée est égale à null si
   * on n'a pas trouvé de cycle (cas pouvant arriver si la topologie arcs/noeuds
   * n'est pas complète. NB: ne nécessite PAS d'avoir une topologie arcs/faces
   * instanciée. NB: nécessite d'avoir une topologie arcs/noeuds instanciée. NB:
   * un cycle passe 2 fois (une fois dans chaque sens) par les cul-de-sac si il
   * y en a.
   * @return un cycle du réseau. Un cycle contient :
   *         <ul>
   *         <li>la liste des arcs dans l'ordre de parcours du cycle. Cette
   *         Liste est classée dans le sens trigonometrique (sauf pour la face
   *         exterieure). (liste de type "ArrayList", contenant elle-même des
   *         Arcs).
   *         <li>la liste des orientations des arc : true si l'arc à sa face à
   *         gauche, false sinon. (liste de type "ArrayList", contenant
   *         elle-même des objets Booleans).
   *         <li>la géométrie du polygone faisant le tour du cycle (de type
   *         IPolygon).
   *         </ul>
   */
  public Cycle cycleAGauche() {
    Arc arcEnCours;
    boolean sensEnCours;
    List<Object> arcOriente;
    GM_LineString contour = new GM_LineString();
    ;
    List<Arc> arcsDuCycle = new ArrayList<Arc>();
    List<Boolean> orientationsDuCycle = new ArrayList<Boolean>();
    int i;
    // intialisation avec le premier arc du cycle (this) qui est par d�finition
    // dans le bon sens
    arcEnCours = this;
    sensEnCours = true;

    // on parcours le cycle dans le sens anti-trigonometrique,
    // jusqu'à revenir sur this en le parcourant dans le bon sens
    // (précision utile à la gestion des cul-de-sac).
    while (true) {
      // ajout de l'arc en cours au cycle...
      arcsDuCycle.add(arcEnCours);
      orientationsDuCycle.add(new Boolean(sensEnCours));
      if (sensEnCours) { // arc dans le bon sens
        for (i = 0; i < arcEnCours.getGeometrie().sizeControlPoint() - 1; i++) {
          contour.addControlPoint(arcEnCours.getGeometrie().getControlPoint(i));
        }
        arcOriente = arcEnCours.arcPrecedentFin();
      } else { // arc dans le sens inverse
        for (i = arcEnCours.getGeometrie().sizeControlPoint() - 1; i > 0; i--) {
          contour.addControlPoint(arcEnCours.getGeometrie().getControlPoint(i));
        }
        arcOriente = arcEnCours.arcPrecedentDebut();
      }
      if (arcOriente == null) {
        return null;
      }

      // au suivant...
      arcEnCours = (Arc) arcOriente.get(0); // l'arc
      sensEnCours = !((Boolean) arcOriente.get(1)).booleanValue(); // le sens de
                                                                   // l'arc par
                                                                   // rapport au
                                                                   // cycle

      // c'est fini ?
      if (arcEnCours == this && sensEnCours) {
        break;
      }
    }

    // ajout du dernier point pour finir la boucle du polygone
    contour.addControlPoint(contour.startPoint());
    return new Cycle(arcsDuCycle, orientationsDuCycle, contour, true);
  }

  /**
   * Recherche du cycle du réseau à gauche de l'arc en se basant sur la
   * topologie de RESEAU uniquement. NB: le résultat est null si on n'a pas
   * trouvé de cycle (cas pouvant arriver si la topologie arcs/noeuds n'est pas
   * complète. NB: ne nécessite PAS d'avoir une topologie arcs/faces instanciée.
   * NB: nécessite d'avoir une topologie arcs/noeuds instanciée. NB: un cycle
   * passe 2 fois (une fois dans chaque sens) par les cul-de-sac si il y en a.
   * @param aGauche si vrai, on parcours l'arc par la gauche. Sinon, on le
   *          parcours par la droite.
   * @return un cycle du réseau
   */
  public Cycle cycle(boolean aGauche) {
    List<Object> arcOriente;
    GM_LineString contour = new GM_LineString();
    List<Arc> arcsDuCycle = new ArrayList<Arc>();
    List<Boolean> orientationsDuCycle = new ArrayList<Boolean>();
    // intialisation avec le premier arc du cycle (this) qui est par d�finition
    // dans le bon sens
    Arc arcEnCours = this;
    boolean sensEnCours = true;

    // on parcours le cycle dans le sens anti-trigonometrique,
    // jusqu'à revenir sur this en le parcourant dans le bon sens
    // (précision utile à la gestion des cul-de-sac).
    while (true) {
      // ajout de l'arc en cours au cycle...
      arcsDuCycle.add(arcEnCours);
      orientationsDuCycle.add(new Boolean(sensEnCours));
      if (sensEnCours) { // arc dans le bon sens
        for (int i = 0; i < arcEnCours.getGeometrie().sizeControlPoint() - 1; i++) {
          contour.addControlPoint(arcEnCours.getGeometrie().getControlPoint(i));
        }
        arcOriente = (aGauche) ? arcEnCours.arcPrecedentFin() : arcEnCours
            .arcSuivantFin();
      } else { // arc dans le sens inverse
        for (int i = arcEnCours.getGeometrie().sizeControlPoint() - 1; i > 0; i--) {
          contour.addControlPoint(arcEnCours.getGeometrie().getControlPoint(i));
        }
        arcOriente = (aGauche) ? arcEnCours.arcPrecedentDebut() : arcEnCours
            .arcSuivantDebut();
      }
      if (arcOriente == null) {
        return null;
      }

      // au suivant...
      arcEnCours = (Arc) arcOriente.get(0); // l'arc
      sensEnCours = !((Boolean) arcOriente.get(1)).booleanValue(); // le sens de
                                                                   // l'arc par
                                                                   // rapport au
                                                                   // cycle

      // c'est fini ?
      if (arcEnCours == this && sensEnCours) {
        break;
      }
    }

    // ajout du dernier point pour finir la boucle du polygone
    contour.addControlPoint(contour.startPoint());
    return new Cycle(arcsDuCycle, orientationsDuCycle, contour, aGauche);
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Gestion de type carte topopolgique
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Les arcs sont class�s autour d'un noeud en fonction de leur g�om�trie.
  // Ceci permet en particulier de parcourir facilement les cycles d'un graphe.
  // ///////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Arc suivant self à son noeud final, au sens des cartes topologiques. L'arc
   * suivant est l'arc incident au noeud final de self, et suivant self dans
   * l'ordre trigonométrique autour de ce noeud final.
   * 
   * NB: renvoie une liste de 2 éléments : element 1, liste.get(0) = l'arc
   * element 2, liste.get(1) = Boolean, true si entrant, false si sortant NB:
   * calcul réalisé pour chaque appel de la méthode. NB : l'arcSuivant peut être
   * self, en cas de cul de sac sur le noeud final.
   */
  public List<Object> arcSuivantFin() {
    if (this.getNoeudFin() == null) {
      return null;
    }
    List<?> arcs = (List<?>) this.getNoeudFin().arcsClasses().get(0);
    List<?> arcsOrientation = (List<?>) this.getNoeudFin().arcsClasses().get(1);
    Iterator<?> itArcs = arcs.iterator();
    Iterator<?> itArcsOrientation = arcsOrientation.iterator();
    Boolean orientationEntrant;
    Arc arc;
    List<Object> resultat = new ArrayList<Object>();

    // On parcours la liste des arcs autour du noeud final
    // Quand on y rencontre this en tant qu'entrant, on renvoie le suivant dans
    // la liste
    // NB: cette notion d'entrant est n�cesaire pour bien g�rer les boucles
    while (itArcs.hasNext()) {
      arc = (Arc) itArcs.next();
      orientationEntrant = (Boolean) itArcsOrientation.next();
      if ((arc == this) && orientationEntrant.booleanValue()) {
        if (itArcs.hasNext()) {
          resultat.add(itArcs.next());
          resultat.add(itArcsOrientation.next());
        } else {
          resultat.add(arcs.get(0));
          resultat.add(arcsOrientation.get(0));
        }
        return resultat;
      }
    }
    return null;
  }

  /**
   * Arc suivant self à son noeud initial, au sens des cartes topologiques.
   * L'arc suivant est l'arc incident au noeud initial de self, et suivant self
   * dans l'ordre trigonométrique autour de ce noeud initial.
   * 
   * NB: renvoie une liste de 2 éléments : element 1, liste.get(0) = l'arc
   * element 2, liste.get(1) = Boolean, true si entrant, false si sortant NB:
   * calcul réalisé pour chaque appel de la méthode. NB : l'arcSuivant peut être
   * self, en cas de cul de sac sur le noeud initial.
   */
  public List<Object> arcSuivantDebut() {
    if (this.getNoeudIni() == null) {
      return null;
    }
    List<?> arcs = (List<?>) this.getNoeudIni().arcsClasses().get(0);
    List<?> arcsOrientation = (List<?>) this.getNoeudIni().arcsClasses().get(1);
    Iterator<?> itArcs = arcs.iterator();
    Iterator<?> itArcsOrientation = arcsOrientation.iterator();
    Boolean orientationEntrant;
    Arc arc;
    List<Object> resultat = new ArrayList<Object>();

    // On parcours la liste des arcs autour du noeud initial
    // Quand on y rencontre this en tant que sortant, on renvoie le suivant dans
    // la liste
    // NB: cette notion de sortant est n�cesaire pour bien g�rer les boucles.
    while (itArcs.hasNext()) {
      arc = (Arc) itArcs.next();
      orientationEntrant = (Boolean) itArcsOrientation.next();
      if ((arc == this) && !orientationEntrant.booleanValue()) {
        if (itArcs.hasNext()) {
          resultat.add(itArcs.next());
          resultat.add(itArcsOrientation.next());
        } else {
          resultat.add(arcs.get(0));
          resultat.add(arcsOrientation.get(0));
        }
        return resultat;
      }
    }
    return null;
  }

  /**
   * Arc précédant self à son noeud final, au sens des cartes topologiques.
   * L'arc précédent est l'arc incident au noeud final de self, et précédant
   * self dans l'ordre trigonométrique autour de ce noeud final.
   * 
   * NB: renvoie une liste de 2 éléments : element 1, liste.get(0) = l'arc
   * element 2, liste.get(1) = Boolean, true si entrant, false si sortant NB:
   * calcul réalisé pour chaque appel de la méthode. NB : l'arc précédent peut
   * être self, en cas de cul de sac sur le noeud final.
   */
  public List<Object> arcPrecedentFin() {
    if (this.getNoeudFin() == null) {
      return null;
    }
    List<?> arcs = (List<?>) this.getNoeudFin().arcsClasses().get(0);
    List<?> arcsOrientation = (List<?>) this.getNoeudFin().arcsClasses().get(1);
    Iterator<?> itArcs = arcs.iterator();
    Iterator<?> itArcsOrientation = arcsOrientation.iterator();
    Boolean orientationEntrant, orientationPrecedent;
    Arc arc, arcPrecedent;
    List<Object> resultat = new ArrayList<Object>();

    // On parcours la liste des arcs autour du noeud final
    // Quand on y rencontre this en tant qu'entrant, on renvoie le pr�c�dant
    // dans la liste
    // NB: cette notion de pr�c�dant est n�cesaire pour bien g�rer les boucles.
    arc = (Arc) itArcs.next();
    orientationEntrant = (Boolean) itArcsOrientation.next();
    if ((arc == this) && orientationEntrant.booleanValue()) {
      resultat.add(arcs.get(arcs.size() - 1));
      resultat.add(arcsOrientation.get(arcs.size() - 1));
      return resultat;
    }
    while (itArcs.hasNext()) {
      arcPrecedent = arc;
      orientationPrecedent = orientationEntrant;
      arc = (Arc) itArcs.next();
      orientationEntrant = (Boolean) itArcsOrientation.next();
      if ((arc == this) && orientationEntrant.booleanValue()) {
        resultat.add(arcPrecedent);
        resultat.add(orientationPrecedent);
        return resultat;
      }
    }
    return null;
  }

  /**
   * Arc précédent self à son noeud initial, au sens des cartes topologiques.
   * L'arc précédent est l'arc incident au noeud initial de self, et précédent
   * self dans l'ordre trigonométrique autour de ce noeud initial.
   * 
   * NB: renvoie une liste de 2 éléments : element 1, liste.get(0) = l'arc
   * element 2, liste.get(1) = Boolean, true si entrant, false si sortant NB:
   * calcul réalisé pour chaque appel de la méthode. NB : l'arc précédent peut
   * être self, en cas de cul de sac sur le noeud initial.
   */
  public List<Object> arcPrecedentDebut() {
    if (this.getNoeudIni() == null) {
      return null;
    }
    List<?> arcs = (List<?>) this.getNoeudIni().arcsClasses().get(0);
    List<?> arcsOrientation = (List<?>) this.getNoeudIni().arcsClasses().get(1);
    Iterator<?> itArcs = arcs.iterator();
    Iterator<?> itArcsOrientation = arcsOrientation.iterator();
    Boolean orientationEntrant, orientationPrecedent;
    Arc arc, arcPrecedent;
    List<Object> resultat = new ArrayList<Object>();

    // On parcours la liste des arcs autour du noeud initial
    // Quand on y rencontre this en tant que sortant, on renvoie le pr�c�dant
    // dans la liste
    // NB: cette notion de pr�c�dant est n�cesaire pour bien g�rer les boucles.
    arc = (Arc) itArcs.next();
    orientationEntrant = (Boolean) itArcsOrientation.next();
    if ((arc == this) && !orientationEntrant.booleanValue()) {
      resultat.add(arcs.get(arcs.size() - 1));
      resultat.add(arcsOrientation.get(arcs.size() - 1));
      return resultat;
    }
    while (itArcs.hasNext()) {
      arcPrecedent = arc;
      orientationPrecedent = orientationEntrant;
      arc = (Arc) itArcs.next();
      orientationEntrant = (Boolean) itArcsOrientation.next();
      if ((arc == this) && !orientationEntrant.booleanValue()) {
        resultat.add(arcPrecedent);
        resultat.add(orientationPrecedent);
        return resultat;
      }
    }
    return null;
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Gestion d'un réseau orienté
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // NB: ne pas confondre orientation définie par l'attribut "orientation"
  // (traité ici),
  // et l'orientation d�finie implicitement par le sens de stockage de la
  // géométrie

  private int orientation = 2;

  /**
   * Renvoie l'orientation. L'orientation vaut 2 dans les deux sens, -1 en sens
   * indirect et 1 en sens direct
   */
  public int getOrientation() {
    return this.orientation;
  }

  /**
   * Définit l'orientation. L'orientation vaut 2 dans les deux sens, -1 en sens
   * indirect et 1 en sens direct
   */
  public void setOrientation(int orientation) {
    this.orientation = orientation;
  }

  /*
   * Noeuds initiaux de l'arc, au sens de son orientation. Si l'arc est en
   * double sens (orientation = 2), les deux noeuds extrémités sont renvoyés.
   * Sinon, un seul noeud est renvoyé. NB: à ne pas confondre avec getNoeudIni,
   * qui renvoie le noeud initial au sens du stockage
   */
  public List<Noeud> inisOrientes() {
    List<Noeud> noeuds = new ArrayList<Noeud>();
    if ((this.orientation == 2 || this.orientation == 1)
        && !(this.noeudIni == null)) {
      noeuds.add(this.noeudIni);
    }
    if ((this.orientation == 2 || this.orientation == -1)
        && !(this.noeudFin == null)) {
      noeuds.add(this.noeudFin);
    }
    return noeuds;
  }

  /*
   * Noeuds finaux de l'arc, au sens de son orientation. Si l'arc est en double
   * sens (orientation = 2), les deux noeuds extrémités sont renvoyés. Sinon, un
   * seul noeud est renvoyé. NB: à ne pas confondre avec getNoeudFin, qui
   * renvoie le noeud final au sens du stockage
   */
  public List<Noeud> finsOrientes() {
    List<Noeud> noeuds = new ArrayList<Noeud>();
    if ((this.orientation == 2 || this.orientation == -1)
        && !(this.noeudIni == null)) {
      noeuds.add(this.noeudIni);
    }
    if ((this.orientation == 2 || this.orientation == 1)
        && !(this.noeudFin == null)) {
      noeuds.add(this.noeudFin);
    }
    return noeuds;
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Gestion des poids pour les plus courts chemins
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  private double poids = 0;

  /** Renvoie le poids de l'arc, pour les calculs de plus court chemin */
  public double getPoids() {
    return this.poids;
  }

  /** Définit le poids de l'arc, pour les calculs de plus court chemin */
  public void setPoids(double d) {
    this.poids = d;
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Gestion des groupes
  // ///////////////////////////////////////////////////////////////////////////////////////////////

  /* Groupes auquels self appartient */
  private Collection<Groupe> listeGroupes = new ArrayList<Groupe>();

  /** Renvoie la liste des groupes de self */
  public Collection<Groupe> getListeGroupes() {
    return this.listeGroupes;
  }

  /** Définit la liste des groupes de self */
  public void setListegroupes(Collection<Groupe> liste) {
    this.listeGroupes = liste;
  }

  /** Ajoute un groupe à self */
  public void addGroupe(Groupe groupe) {
    if (groupe != null && !this.listeGroupes.contains(groupe)) {
      this.listeGroupes.add(groupe);
      if (!groupe.getListeArcs().contains(this)) {
        groupe.addArc(this);
      }
    }
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Pour la gestion des requetes spatiales
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  private Rectangle rectangleEnglobant = null;

  /**
   * Rectangle englobant de l'arc, orienté le long des axes des x,y. NB: le
   * rectangle est calculé au premier appel de cette fonction. Si l'arc est
   * modifié, la valeur n'est pas mise à jour : il faut le faire explicitement
   * au besoin avec calculeRectangleEnglobant.
   */
  public Rectangle getRectangleEnglobant() {
    if (this.rectangleEnglobant == null) {
      this.calculeRectangleEnglobant();
    }
    return this.rectangleEnglobant;
  }

  /** Calcule le rectangle englobant x,y en fonction de la g�om�trie */
  public void calculeRectangleEnglobant() {
    this.rectangleEnglobant = Rectangle.rectangleEnglobant(this.getGeometrie());
  }

  protected boolean proche(Arc arc, double distance) {
    return arc.getRectangleEnglobant().intersecte(
        this.getRectangleEnglobant().dilate(distance));
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Opérateurs de calculs sur les arcs
  // ///////////////////////////////////////////////////////////////////////////////////////////////
  /** Distance euclidienne entre le noeud et self */
  public double distance(Noeud noeud) {
    return (Distances.distance(noeud.getCoord(), this.getGeometrie()));
  }

  /**
   * Première composante de la distance de Hausdorff de self vers l'arc. Elle
   * est calculee comme le maximum des distances d'un point intermediaire de
   * self à l'arc. Cette approximation peut différer sensiblement de la
   * definition theorique. NB : défini en théorie à 3D, mais non vérifié en
   * profondeur
   */
  public double premiereComposanteHausdorff(Arc arc) {
    return (Distances.premiereComposanteHausdorff(this.getGeometrie(), arc
        .getGeometrie()));
  }

  /**
   * Distance de Hausdorff entre self et l'arc. Elle est calculee comme le
   * maximum des distances d'un point intermediaire d'une des lignes a l'autre
   * ligne. Dans certains cas cette definition differe de la definition
   * theorique car la distance de Hausdorff ne se realise pas necessairement sur
   * un point intermediaire. Mais cela est rare sur des données réel. Cette
   * implementation est un bon compromis entre simplicité et précision. NB :
   * défini en théorie à 3D, mais non vérifié en profondeur
   */
  public double hausdorff(Arc arc) {
    return (Distances.hausdorff(this.getGeometrie(), arc.getGeometrie()));
  }

  /**
   * Longueur euclidienne de l'arc. Est calcul� en 3D si la géométrie est
   * définie en 3D
   */
  public double longueur() {
    return this.getGeometrie().length();
  }

  protected boolean pendant = false;

  /**
   * @return vrai si l'arc est pendant, i.e. si sa face droite est la m�me que
   *         sa face gauche. En d'autres mots, c'est une impasse.
   */
  public boolean isPendant() {
    return this.pendant;
  }

  /**
   * Affecte la valeur de l'attribut pendant de l'arc :
   * @param pendant vrai si l'arc est pendant, i.e. si sa face droite est la
   *          même que sa face gauche. En d'autres mots, c'est une impasse.
   */
  public void setPendant(boolean pendant) {
    this.pendant = pendant;
    if (pendant && !this.getFaceDroite().getArcsPendants().contains(this)) {
      this.getFaceDroite().getArcsPendants().add(this);
    }
  }

  @Override
  public String toString() {
    return "Arc " + this.getId() + " - " + this.getOrientation() + " - "
        + ((this.getNoeudIni() == null) ? "null" : this.getNoeudIni().getId())
        + " - "
        + ((this.getNoeudFin() == null) ? "null" : this.getNoeudFin().getId())
        + " - " + this.getGeometrie();
  }
  
    /**
   * Renvoie le noeud de l'autre coté de l'arc.
   * @param n a node
   * @return the node on the other side of the edge
   */
  public Noeud getOtherSide(Noeud n) {
    if (n == this.noeudFin) {
      return this.noeudIni;
    }
    if (n == this.noeudIni) {
      return this.noeudFin;
    }
    return null;
  }

public void construireGeom() {
	IDirectPositionList list = new DirectPositionList();
	list.add(this.noeudIni.getCoord());
	list.add(this.noeudFin.getCoord());
	this.setGeom(new GM_LineString(list));
}

public Arc copy() {
  Arc arc = new Arc();
  arc.setNoeudIni(this.getNoeudIni());
  arc.setNoeudFin(this.getNoeudFin());
  arc.setGeometrie(new GM_LineString(new DirectPositionList(Arrays.asList(
      this.getNoeudIni().getGeometrie().getPosition(), 
      this.getNoeudFin().getGeometrie().getPosition()))));
  return arc;
}


}
