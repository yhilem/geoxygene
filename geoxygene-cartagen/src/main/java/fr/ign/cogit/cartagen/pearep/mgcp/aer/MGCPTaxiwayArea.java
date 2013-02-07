package fr.ign.cogit.cartagen.pearep.mgcp.aer;

import java.util.HashMap;

import fr.ign.cogit.cartagen.core.genericschema.airport.ITaxiwayArea;
import fr.ign.cogit.cartagen.pearep.mgcp.MGCPFeature;
import fr.ign.cogit.cartagen.pearep.vmap.PeaRepDbType;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;

public class MGCPTaxiwayArea extends MGCPFeature implements ITaxiwayArea {

  private TaxiwayType type;

  /**
   * @param type
   */
  public MGCPTaxiwayArea(IPolygon geom, HashMap<String, Object> attributes,
      PeaRepDbType type) {
    super();
    this.setInitialGeom(geom);
    this.setEliminated(false);
    this.setGeom(geom);
    this.setAttributeMap(attributes);
    if (attributes.containsKey("len_"))
      this.type = TaxiwayType.TAXIWAY;
    else
      this.type = TaxiwayType.APRON;
  }

  /**
   * @param type
   */
  public MGCPTaxiwayArea(IPolygon geom, TaxiwayType type) {
    super();
    this.setInitialGeom(geom);
    this.setEliminated(false);
    this.setGeom(geom);
    this.type = type;

  }

  @Override
  public TaxiwayType getType() {
    return type;
  }

  @Override
  public IPolygon getGeom() {
    return (IPolygon) this.geom;
  }

}
