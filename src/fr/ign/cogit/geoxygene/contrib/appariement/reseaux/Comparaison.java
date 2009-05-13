/*
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO specifications for
 * the development and deployment of geographic (GIS) applications. It is a open source
 * contribution of the COGIT laboratory at the Institut G�ographique National (the French
 * National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut G�ographique National
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

package fr.ign.cogit.geoxygene.contrib.appariement.reseaux;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.geometrie.Distances;
import fr.ign.cogit.geoxygene.contrib.geometrie.Operateurs;
import fr.ign.cogit.geoxygene.contrib.geometrie.Vecteur;
import fr.ign.cogit.geoxygene.feature.FT_Feature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPositionList;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.util.index.Tiling;

/**
 * Classe supportant les m�thodes de comparaison globale de r�seaux.
 * 
 * @author Mustiere - IGN / Laboratoire COGIT
 * @version 1.0
 * 
 */

public class Comparaison {

	/** Approximation de l'�cart moyen entre deux r�seaux.
	 * Cette approximation grossi�re �value pour tout point de r�seau1 son �cart � r�seau2
	 * (point le plus proche quelconque), sans r�aliser d'appariement de r�seau.
	 * 
	 * @param reseau1
	 * Un r�seau, typiquement un r�seau peu pr�cis dont on veut estimer la qualit� par rapport � reseau2.
	 * 
	 * @param reseau2
	 * Un autre r�seau, typiquement un r�seau de bonne qualit� qui sert de r�f�rence.
	 * 
	 * @param distanceMax
	 * Sert � �liminer les aberrations dans le calcul de la moyenne:
	 * les arcs de reseau1 situ�s au moins en un point � plus de distanceMax de reseau2
	 * ne sont pas pris en compte dans le calcul.
	 * 
	 * @return
	 * L'�cart moyen. Il est calcul� comme la moyenne des distances entre les points
	 * des arcs de reseau1 et reseau2. Cette moyenne est pond�r�e par la longueur
	 * des segments entourant les points en question.
	 */
	public static double approximationEcartPlaniMoyen(CarteTopo reseau1, CarteTopo reseau2, double distanceMax) {

		double dtot = 0, ltot = 0, dmax = 0;
		double ltotArc, dtotArc, dmaxArc, l1, l2, l;
		int n = 0;
		Iterator<?> itArcsRef, itArcsComp;
		FT_FeatureCollection<?> arcsCompProches;
		FT_FeatureCollection<?> arcsRef = reseau1.getPopArcs();
		FT_FeatureCollection<?> arcsComp = reseau2.getPopArcs();

		itArcsRef = arcsRef.getElements().iterator();
		while (itArcsRef.hasNext()) { //pour chaque arc de this
			FT_Feature objetRef = (FT_Feature) itArcsRef.next();
			GM_LineString geomRef = (GM_LineString)objetRef.getGeom();
			DirectPositionList dp = geomRef.coord();
			arcsCompProches=arcsComp.select(geomRef,distanceMax);

			dtotArc = 0;
			ltotArc = 0;
			dmaxArc = 0;
			for(int i=0;i<dp.size();i++) { //pour chaque point d'un arc de this
				DirectPosition ptRef = dp.get(i);
				if ( i == 0 ) l1 = 0;
				else l1 = Distances.distance(dp.get(i),dp.get(i-1));
				if ( i == dp.size()-1 ) l2 = 0;
				else l2 = Distances.distance(dp.get(i),dp.get(i+1));
				l = l1+l2;
				double dmin = Double.MAX_VALUE;
				itArcsComp = arcsCompProches.getElements().iterator();
				while (itArcsComp.hasNext()) {
					FT_Feature objetComp = (FT_Feature) itArcsComp.next();
					GM_LineString geomComp = (GM_LineString)objetComp.getGeom();
					DirectPosition ptProjete = Operateurs.projection(ptRef, geomComp);
					double d = Distances.distance(ptRef,ptProjete);
					if (d<dmin) dmin = d;
				}
				if ( dmin > dmaxArc ) dmaxArc = dmin;
				dtotArc = dtotArc +dmin*l;
				ltotArc = ltotArc + l;
			}
			if ( dmaxArc > distanceMax ) continue;
			if ( dmaxArc > dmax) dmax = dmaxArc;
			dtot = dtot + dtotArc;
			ltot = ltot + ltotArc;
			n++;
		}

		System.out.println("distance moyenne entre les arcs"+dtot/ltot);
		System.out.println("distance max entre les arcs "+dmax);
		System.out.println("Nb d'arcs de this pris en compte dans le calcul "+n);
		System.out.println("Nb d'arcs de this non pris en compte dans le calcul "+(arcsRef.size()-n));
		return dtot/ltot;
	}


	/** Ensemble d'indicateurs �valuant globalement l'�cart de position entre le r�seau � �tudier r�seau11
	 * et un r�seau de r�f�rence r�seau2.
	 * Cette �valuation se fait en s'appuyant sur le calcul des �carts entre chaque point
	 * de reseau1 et sa projection au plus pr�s sur le r�seau reseau2 (et non sur un r�el appariement d'arcs).
	 * Les moyennes sont pond�r�es par la longueur des segments entourant les points,
	 * pour gommer les effets dus aux pas de d�coupage variables des lignes.
	 * 
	 * @param reseau1
	 * R�seau �tudi�.
	 * 
	 * @param reseau2
	 * R�seau servant de r�f�rence.
	 * 
	 * @param affichage
	 * Si TRUE alors les r�sultats sont affich�s.
	 * 
	 * @param distanceMax
	 * Sert � �liminer les aberrations dans les calculs.
	 * - Les arcs de reseau1 situ�s en au moins un point � plus de distanceMax
	 *   de reseau2 ne sont pas pris en compte dans le calcul des indicateurs sur les arcs.
	 * - Les noeuds de reseau1 situ�s � plus de distanceMax d'un noeud
	 *   de reseau2 ne sont pas pris en compte dans le calcul des indicateurs sur les noeuds
	 * 
	 * @return
	 * Liste (de 'double') contenant un ensemble d'indicateurs sur l'�cart entre les r�seaux :
	 * 
	 * ESTIMATEURS SUR LES ARCS
	 * liste(0): longueur des arcs du r�seau "this" total
	 * liste(1): longueur des arcs du r�seau "this" pris en compte dans les calculs d'�valuation de l'�cart
	 * liste(2): longueur des arcs du r�seau "reseau"
	 * liste(3): nombre d'arcs du r�seau "this" total
	 * liste(4): nombre d'arcs du r�seau "this" pris en compte dans les calculs d'�valuation de l'�cart
	 * liste(5): nombre d'arcs du r�seau "reseau"
	 * liste(6): estimation du biais syst�matique en X sur les arcs
	 * 			(valeur en X de la moyenne des vecteurs d'�cart entre un point de "this" et son projet� sur "reseau")
	 * liste(7): estimation du biais syst�matique en Y sur les arcs
	 * 			(valeur en Y de la moyenne des vecteurs d'�cart entre un point de "this" et son projet� sur "reseau")
	 * liste(8): estimation de l'�cart moyen sur les arcs
	 * 			(moyenne des longueurs des vecteurs d'�cart entre un point de "this" et son projet� sur "reseau")
	 * liste(9): estimation de l'�cart moyen quadratique sur les arcs
	 * 			(moyenne quadratique des longueurs des vecteurs d'�cart entre un point de "this" et son projet� sur "reseau")
	 * liste(10): estimation de l'�cart type sur les arcs, i.e. pr�cision une fois le biais corrig�
	 * 			( racine(ecart moyen quadratique^2 - biais^2)
	 * liste(11): histogramme de r�partition des �carts sur tous les points (en nb de points interm�diaires sur les arcs)
	 * 
	 * ESTIMATEURS SUR LES NOEUDS (si ils existent)
	 * liste(12): nombre de noeuds du r�seau "this" total
	 * liste(13): nombre de noeuds du r�seau "this" pris en compte dans les calculs d'�valuation de l'�cart
	 * liste(14): nombre de noeuds du r�seau "reseau"
	 * liste(15): estimation du biais syst�matique en X sur les noeuds
	 * 			(valeur en X de la moyenne des vecteurs d'�cart entre un noeud de "this" et le noeud le plus proche de "reseau")
	 * liste(16): estimation du biais syst�matique en Y sur les noeuds
	 * 			(valeur en Y de la moyenne des vecteurs d'�cart entre un noeud de "this" et le noeud le plus proche de "reseau")
	 * liste(17): estimation de l'�cart moyen sur les noeuds
	 * 			(moyenne des longueurs des vecteurs d'�cart entre un noeud de "this" et le noeud le plus proche de "reseau")
	 * liste(18): estimation de l'�cart moyen quadratique sur les arcs
	 * 			(moyenne quadratique des longueurs des vecteurs d'�cart entre un noeud de "this" et le noeud le plus proche de "reseau")
	 * liste(19): estimation de l'�cart type sur les noeuds, i.e. pr�cision une fois le biais corrig�
	 * 			( racine(ecart moyen quadratique^2 - biais^2)
	 * liste(20): histogramme de r�partition des �carts sur tous les noeuds (en nb de noeuds)
	 * 
	 */
	public static List<?> evaluationEcartPosition(CarteTopo reseau1, CarteTopo reseau2, double distanceMax, boolean affichage) {

		List<Double> resultats = new ArrayList<Double>();
		FT_FeatureCollection<?> arcs1 = reseau1.getPopArcs();
		FT_FeatureCollection<?> arcs2 = reseau2.getPopArcs();
		Iterator<?> itArcs1, itArcs2;
		Arc arc1, arc2;
		GM_LineString geom1, geom2;
		Vecteur v12, vmin, vPourUnArc1, vTotal;
		DirectPosition pt1, projete;
		FT_FeatureCollection<?> arcs2proches;
		double longArc1, poids, d12, ecartQuadratiquePourUnArc1, l1, l2,
		poidsPourUnArc1, ecartPourUnArc1, ecartMaxArc1, poidsTotal;

		// indicateurs finaux
		double longTotal1, longPrisEnCompte1, longTotal2;
		double ecartTotal , ecartQuadratiqueTotal, ecartTypeArcs;
		int nbArcsTotal1, nbArcsPrisEnCompte1, nbArcsTotal2;


		///////////////// EVALUATION SUR LES ARCS ////////////////////
		// indexation des arcs du r�seau 2
		if ( !reseau2.getPopArcs().hasSpatialIndex()) {
			reseau2.getPopArcs().initSpatialIndex(Tiling.class, false, 20 );
		}
		// parcours du r�seau 2 juste pour calculer sa longueur
		itArcs2 = arcs2.getElements().iterator();
		longTotal2 = 0;
		while (itArcs2.hasNext()) {
			arc2 = (Arc) itArcs2.next();
			longTotal2 = longTotal2+((GM_LineString)arc2.getGeom()).length();
		}
		nbArcsTotal2 = arcs2.getElements().size();
		// parcours du r�seau 1 pour �valuer les �carts sur les arcs
		nbArcsTotal1 = arcs1.getElements().size();
		nbArcsPrisEnCompte1 = 0;
		poidsTotal = 0;
		ecartTotal = 0;
		ecartQuadratiqueTotal = 0;
		vTotal = new Vecteur(new DirectPosition(0,0));
		longTotal1 = 0;
		longPrisEnCompte1 = 0;
		itArcs1 = arcs1.getElements().iterator();
		while (itArcs1.hasNext()) { //pour chaque arc du r�seau 1
			arc1 = (Arc) itArcs1.next();
			geom1 = (GM_LineString)arc1.getGeom();
			longArc1 = geom1.length();
			longTotal1 = longTotal1+geom1.length();

			arcs2proches=arcs2.select(geom1,distanceMax);
			if (arcs2proches.size()==0) continue;
			poidsPourUnArc1 = 0;
			ecartPourUnArc1 = 0;
			ecartQuadratiquePourUnArc1 = 0;
			vmin = new Vecteur();
			vPourUnArc1 = new Vecteur(new DirectPosition(0,0));
			ecartMaxArc1 = 0;
			DirectPositionList dp = geom1.coord();
			for(int i=0; i<dp.size(); i++) { //pour chaque point de arc1
				pt1 = dp.get(i);
				// calcul du poids de ce point
				if ( i == 0 ) l1 = 0;
				else l1 = Distances.distance(dp.get(i),dp.get(i-1));
				if ( i == dp.size()-1 ) l2 = 0;
				else l2 = Distances.distance(dp.get(i),dp.get(i+1));
				poids = l1+l2;
				// projection du point sur le r�seau2
				double dmin = Double.MAX_VALUE;
				itArcs2 = arcs2proches.getElements().iterator();
				while (itArcs2.hasNext()) { // pour chaque arc du r�seau 2
					arc2 = (Arc) itArcs2.next();
					geom2 = (GM_LineString)arc2.getGeom();
					projete = Operateurs.projection(pt1, geom2);
					v12 = new Vecteur(pt1,projete);
					d12 = Distances.distance(pt1,projete);
					if (d12<dmin) {
						dmin = d12;
						vmin = v12;
					}
				}
				// calcul des indicateurs
				if (dmin > ecartMaxArc1 ) ecartMaxArc1 = dmin;
				ecartPourUnArc1 = ecartPourUnArc1+dmin*poids;
				ecartQuadratiquePourUnArc1 = ecartPourUnArc1+poids*Math.pow(dmin,2);
				vPourUnArc1 = vPourUnArc1.ajoute(vmin.multConstante(poids));
				poidsPourUnArc1 = poidsPourUnArc1 + poids;
			}
			if ( ecartMaxArc1 > distanceMax ) continue; // on ne prend pas l'arc en compte
			longPrisEnCompte1 = longPrisEnCompte1 + longArc1;
			nbArcsPrisEnCompte1++;
			poidsTotal = poidsTotal + poidsPourUnArc1;
			ecartTotal = ecartTotal + ecartPourUnArc1;
			ecartQuadratiqueTotal = ecartQuadratiqueTotal + ecartQuadratiquePourUnArc1;
			vTotal = vTotal.ajoute(vPourUnArc1);
		}
		vTotal = vTotal.multConstante(1/poidsTotal);
		ecartTotal = ecartTotal/poidsTotal;
		ecartTypeArcs = Math.sqrt((ecartQuadratiqueTotal/poidsTotal)-(Math.pow(vTotal.getX(),2)+Math.pow(vTotal.getY(),2)));
		ecartQuadratiqueTotal = Math.sqrt(ecartQuadratiqueTotal/poidsTotal);
		// resultats sur les arcs
		if (affichage) System.out.println("******************* BILAN SUR LES ARCS *******************");
		if (affichage) System.out.println("** Comparaison globale des r�seaux, en nombre et longueur d'arcs :");
		resultats.add(new Double(longTotal1));
		if (affichage) System.out.println("** Longueur total des arcs du r�seau 1 (km) : "+Math.round(longTotal1/1000));
		resultats.add(new Double(longPrisEnCompte1));
		if (affichage) System.out.println("** Longueur total des arcs du r�seau 1 pris en compte pour le calcul (km) : "+Math.round(longPrisEnCompte1/1000));
		resultats.add(new Double(longTotal2));
		if (affichage) System.out.println("** Longueur total des arcs du r�seau 2(km) : "+Math.round(longTotal2/1000));
		resultats.add(new Double(nbArcsTotal1));
		if (affichage) System.out.println("** Nombre d'arcs du r�seau 1 : "+nbArcsTotal1);
		resultats.add(new Double(nbArcsPrisEnCompte1));
		if (affichage) System.out.println("** Nombre d'arcs du r�seau 1 pris en compte pour le calcul : "+nbArcsPrisEnCompte1);
		resultats.add(new Double(nbArcsTotal2));
		if (affichage) System.out.println("** Nombre d'arcs du r�seau 2 : "+nbArcsTotal2);

		if (affichage) System.out.println("");
		if (affichage) System.out.println("** Estimateurs d'�cart ");
		resultats.add(new Double(vTotal.getX()));
		if (affichage) System.out.println("** Biais syst�matique en X  : "+vTotal.getX());
		resultats.add(new Double(vTotal.getY()));
		if (affichage) System.out.println("** Biais syst�matique en Y  : "+vTotal.getY());
		resultats.add(new Double(ecartTotal));
		if (affichage) System.out.println("** Ecart moyen : "+ecartTotal);
		resultats.add(new Double(ecartQuadratiqueTotal));
		if (affichage) System.out.println("** Ecart moyen quadratique : "+ecartQuadratiqueTotal);
		resultats.add(new Double(ecartTypeArcs));
		if (affichage) System.out.println("** Ecart type : "+ecartTypeArcs);
		if (affichage) System.out.println("*********************************************************");

		return resultats;
	}

}
