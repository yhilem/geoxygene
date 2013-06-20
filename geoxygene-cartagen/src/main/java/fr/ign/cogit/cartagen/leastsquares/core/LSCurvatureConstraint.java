/*
 * Cr�� le 29 avr. 2008
 * 
 * Pour changer le mod�le de ce fichier g�n�r�, allez � :
 * Fen�tre&gt;Pr�f�rences&gt;Java&gt;G�n�ration de code&gt;Code et commentaires
 */
package fr.ign.cogit.cartagen.leastsquares.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.LineDensification;

/**
 * @author G. Touya
 * 
 *         Curvature constraints like the one presented in (Harrie, 1999)
 */
public class LSCurvatureConstraint extends LSInternalConstraint {

  /**
   * True if the constraint is applicable on point.
   * @param point
   * @return
   */
  public static boolean appliesTo(LSPoint point) {
    if (!point.isPointIniFin()) {
      return true;
    }
    return false;
  }

  public LSCurvatureConstraint(LSPoint pt, LSScheduler scheduler) {
    super(pt, scheduler);
  }

  /*
   * (non-Javadoc)
   * 
   * @seefr.ign.gothic.cogit.guillaume.moindresCarres.ContrainteInterneMC#
   * calculeSystemeEquations(gothic.main.GothicObject,
   * fr.ign.gothic.cogit.guillaume.moindresCarres.MCPoint)
   */
  @Override
  public EquationsSystem calculeSystemeEquations(IFeature obj, LSPoint point) {

    EquationsSystem systeme = this.sched.initSystemeLocal();
    // on commence par récupérer le point précédent et le suivant
    IDirectPosition coordPrec = null;
    IDirectPosition coordSuiv = null;
    // on commence par récupérer la géométrie
    IGeometry geom = obj.getGeom();
    ILineString ligne;
    if (geom instanceof ILineString) {
      ligne = LineDensification.densification((ILineString) geom, 50.0);
    } else {
      ligne = LineDensification.densification(
          ((IPolygon) geom).exteriorLineString(), 50.0);
    }

    for (int i = 0; i < ligne.numPoints(); i++) {
      IDirectPosition coord = ligne.coord().get(i);
      if (!coord.equals(point.getIniPt())) {
        continue;
      }

      // si on est l�, c'est le bon vertex
      // on marque le vertex pr�c�dent
      int prevIndex, nextIndex;
      if (i == 0) {
        prevIndex = ligne.numPoints() - 2;
      } else {
        prevIndex = i - 1;
      }
      // on marque le vertex suivant
      if (i + 1 == ligne.numPoints()) {
        nextIndex = 0;
      } else {
        nextIndex = i + 1;
      }
      // on r�cup�re les coordonnées précédentes
      coordPrec = ligne.coord().get(prevIndex);
      // on r�cup�re les coordonnées suivantes
      coordSuiv = ligne.coord().get(nextIndex);
      break;
    }

    // on récupère maintenant les MCPoints correspondant à ces coordonnées
    ArrayList<LSPoint> setPoints = this.sched.getMapObjPts().get(obj);
    LSPoint pointPrec = null, pointSuiv = null;
    Iterator<LSPoint> iter = setPoints.iterator();
    while (iter.hasNext()) {
      LSPoint pt = iter.next();
      if (pt.getIniPt().equals(coordPrec)) {
        pointPrec = pt;
      }
      if (pt.getIniPt().equals(coordSuiv)) {
        pointSuiv = pt;
      }
    }// while boucle sur setPoints

    // construction du vecteur des inconnues
    systeme.setUnknowns(new Vector<LSPoint>());
    if (!pointPrec.isFixed()) {
      systeme.getUnknowns().addElement(pointPrec);
      systeme.getUnknowns().addElement(pointPrec);
    }
    systeme.getUnknowns().addElement(point);
    systeme.getUnknowns().addElement(point);
    if (!pointSuiv.isFixed()) {
      systeme.getUnknowns().addElement(pointSuiv);
      systeme.getUnknowns().addElement(pointSuiv);
    }

    // construction du vecteur des contraintes
    systeme.setConstraints(new Vector<LSConstraint>());
    for (int i = 0; i < 3; i++) {
      systeme.getConstraints().add(this);
    }

    // construction de la matrice des observations
    // c'est une matrice (4,1) contenant deux 0
    systeme.initObservations(3);

    // calcul des facteurs de l'équation sur les angles
    double a = 0.0, b = 0.0, c = 0.0, d = 0.0, e = 0.0, f = 0.0;
    double normeU = Math.sqrt((point.getIniPt().getX() - pointPrec.getIniPt()
        .getX())
        * (point.getIniPt().getX() - pointPrec.getIniPt().getX())
        + (point.getIniPt().getY() - pointPrec.getIniPt().getY())
        * (point.getIniPt().getY() - pointPrec.getIniPt().getY()));
    double normeW = Math.sqrt((pointSuiv.getIniPt().getX() - point.getIniPt()
        .getX())
        * (pointSuiv.getIniPt().getX() - point.getIniPt().getX())
        + (pointSuiv.getIniPt().getY() - point.getIniPt().getY())
        * (pointSuiv.getIniPt().getY() - point.getIniPt().getY()));
    a = (point.getIniPt().getY() - pointSuiv.getIniPt().getY())
        / (normeU * normeW);
    b = (-point.getIniPt().getX() + pointSuiv.getIniPt().getX())
        / (normeU * normeW);
    c = (-pointPrec.getIniPt().getY() + pointSuiv.getIniPt().getY())
        / (normeU * normeW);
    d = (pointPrec.getIniPt().getX() - pointSuiv.getIniPt().getX())
        / (normeU * normeW);
    e = (pointPrec.getIniPt().getY() - point.getIniPt().getY())
        / (normeU * normeW);
    f = (-pointPrec.getIniPt().getX() + point.getIniPt().getX())
        / (normeU * normeW);

    // calcul des facteurs pour les équations sur les longueurs
    double a1 = 0.0, b1 = 0.0, c1 = 0.0, d1 = 0.0;
    double a2 = 0.0, b2 = 0.0, c2 = 0.0, d2 = 0.0;
    a1 = (pointPrec.getIniPt().getX() - point.getIniPt().getX()) / normeU;
    b1 = (pointPrec.getIniPt().getY() - point.getIniPt().getY()) / normeU;
    c1 = (point.getIniPt().getX() - pointPrec.getIniPt().getX()) / normeU;
    d1 = (point.getIniPt().getY() - pointPrec.getIniPt().getY()) / normeU;
    a2 = (pointSuiv.getIniPt().getX() - point.getIniPt().getX()) / normeW;
    b2 = (pointSuiv.getIniPt().getY() - point.getIniPt().getY()) / normeW;
    c2 = (point.getIniPt().getX() - pointSuiv.getIniPt().getX()) / normeW;
    d2 = (point.getIniPt().getY() - pointSuiv.getIniPt().getY()) / normeW;

    // construction de la matrice A
    if (pointPrec.isFixed() && pointSuiv.isFixed()) {
      systeme.initMatriceA(3, 2);
      systeme.setA(0, 0, c);
      systeme.setA(0, 1, d);
      systeme.setA(1, 0, c1);
      systeme.setA(1, 1, d1);
      systeme.setA(2, 0, c2);
      systeme.setA(2, 1, d2);
      systeme.setNonNullValues(6);
    } else if (pointPrec.isFixed()) {
      systeme.initMatriceA(3, 4);
      systeme.setA(0, 0, c);
      systeme.setA(0, 1, d);
      systeme.setA(0, 2, e);
      systeme.setA(0, 3, f);
      systeme.setA(1, 0, c1);
      systeme.setA(1, 1, d1);
      systeme.setA(2, 0, c2);
      systeme.setA(2, 1, d2);
      systeme.setA(2, 2, a2);
      systeme.setA(2, 3, b2);
      systeme.setNonNullValues(10);
    } else if (pointSuiv.isFixed()) {
      systeme.initMatriceA(3, 4);
      systeme.setA(0, 0, a);
      systeme.setA(0, 1, b);
      systeme.setA(0, 2, c);
      systeme.setA(0, 3, d);
      systeme.setA(1, 0, a1);
      systeme.setA(1, 1, b1);
      systeme.setA(1, 2, c1);
      systeme.setA(1, 3, d1);
      systeme.setA(2, 2, c2);
      systeme.setA(2, 3, d2);
      systeme.setNonNullValues(10);
    } else {
      systeme.initMatriceA(3, 6);
      systeme.setA(0, 0, a);
      systeme.setA(0, 1, b);
      systeme.setA(0, 2, c);
      systeme.setA(0, 3, d);
      systeme.setA(0, 4, e);
      systeme.setA(0, 5, f);
      systeme.setA(1, 0, a1);
      systeme.setA(1, 1, b1);
      systeme.setA(1, 2, c1);
      systeme.setA(1, 3, d1);
      systeme.setA(2, 4, a2);
      systeme.setA(2, 5, b2);
      systeme.setA(2, 2, c2);
      systeme.setA(2, 3, d2);
      systeme.setNonNullValues(14);
    }

    return systeme;
  }

}
