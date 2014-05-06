package fr.ign.cogit.appli.commun.schema.shp.bdcarto.routier;

import fr.ign.cogit.appli.commun.schema.shp.bdcarto.ElementBDCarto;
import fr.ign.cogit.geoxygene.spatial.geomprim.GM_Point;

public abstract class EquipementRoutier extends ElementBDCarto {

	/** Renvoie la g�om�trie de self */
	public GM_Point getGeometrie() {return (GM_Point)geom;}
	/** D�finit la g�om�trie de self */
	public void setGeometrie(GM_Point geometrie) {this.geom = geometrie;}

	protected String nature;
	public String getNature() {return this.nature; }
	public void setNature (String Nature) {nature = Nature; }

	protected String toponyme;
	public String getToponyme() {return this.toponyme; }
	public void setToponyme (String Toponyme) {toponyme = Toponyme; }

}