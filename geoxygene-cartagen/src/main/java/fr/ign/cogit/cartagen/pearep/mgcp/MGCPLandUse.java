/*******************************************************************************
 * This software is released under the licence CeCILL
 * 
 * see Licence_CeCILL-C_fr.html see Licence_CeCILL-C_en.html
 * 
 * see <a href="http://www.cecill.info/">http://www.cecill.info/a>
 * 
 * @copyright IGN
 ******************************************************************************/
package fr.ign.cogit.cartagen.pearep.mgcp;

import java.util.HashMap;

import fr.ign.cogit.cartagen.core.genericschema.land.ISimpleLandUseArea;
import fr.ign.cogit.cartagen.pearep.vmap.PeaRepDbType;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;

public class MGCPLandUse extends MGCPFeature implements ISimpleLandUseArea {

  // MGCP attributes
  private String ace, ale, cpyrt_note, dmb, dmt, hgt, lbv, na2, nam, nfi, nfn,
      pfh, rbv, src_date, src_info, tier_note, txt, uid, upd_date, upd_info,
      wid;
  private long acc, ace_eval, ale_eval, bac, cda, csp, ffp, fmm, fuc, fun, hyp,
      irg, iss, ord, ppo, scc, shl, shr, smc, src_name, tid, tre, upd_name,
      veg, vsp, wcc, wle, wst, zval_type;
  private double area;

  private MGCPLandUseType landUseType;

  public MGCPLandUse(IPolygon poly) {
    super();
    this.setGeom(poly);
    this.setInitialGeom(poly);
    this.setEliminated(false);
    this.setArea(poly.area());
  }

  /**
   * @param type
   */
  public MGCPLandUse(IPolygon poly, HashMap<String, Object> attributes,
      PeaRepDbType type) {
    this(poly);

    this.acc = (Long) attributes.get("acc");
    this.ace = (String) attributes.get("ace");
    this.ace_eval = (Long) attributes.get("ace_eval");
    this.ale = (String) attributes.get("ale");
    this.ale_eval = (Long) attributes.get("ale_eval");
    this.nfi = (String) attributes.get("nfi");
    this.cpyrt_note = (String) attributes.get("cpyrt_note");
    this.nfn = (String) attributes.get("nfn");
    this.nam = (String) attributes.get("nam");
    this.src_date = (String) attributes.get("src_date");
    this.src_info = (String) attributes.get("src_info");
    this.src_name = (Long) attributes.get("src_name");
    this.txt = (String) attributes.get("txt");
    this.tier_note = (String) attributes.get("tier_note");
    this.uid = (String) attributes.get("uid");
    this.upd_date = (String) attributes.get("upd_date");
    this.upd_name = (Long) attributes.get("upd_name");
    this.upd_info = (String) attributes.get("upd_info");
    this.zval_type = (Long) attributes.get("zval_type");

    if (attributes.containsKey("bac"))
      this.bac = (Long) attributes.get("bac");
    ;
    if (attributes.containsKey("cda"))
      this.cda = (Long) attributes.get("cda");
    ;
    if (attributes.containsKey("csp"))
      this.csp = (Long) attributes.get("csp");
    ;
    if (attributes.containsKey("dmb"))
      this.dmb = (String) attributes.get("dmb");
    ;
    if (attributes.containsKey("dmt"))
      this.dmt = (String) attributes.get("dmt");
    ;
    if (attributes.containsKey("ffp"))
      this.ffp = (Long) attributes.get("ffp");
    ;
    if (attributes.containsKey("fmm"))
      this.fmm = (Long) attributes.get("fmm");
    ;
    if (attributes.containsKey("fuc"))
      this.fuc = (Long) attributes.get("fuc");
    ;
    if (attributes.containsKey("fun"))
      this.fun = (Long) attributes.get("fun");
    ;
    if (attributes.containsKey("hgt"))
      this.hgt = (String) attributes.get("hgt");
    ;
    if (attributes.containsKey("hyp"))
      this.hyp = (Long) attributes.get("hyp");
    ;
    if (attributes.containsKey("irg"))
      this.irg = (Long) attributes.get("irg");
    ;
    if (attributes.containsKey("iss"))
      this.iss = (Long) attributes.get("iss");
    ;
    if (attributes.containsKey("lbv"))
      this.lbv = (String) attributes.get("lbv");
    ;
    if (attributes.containsKey("na2"))
      this.na2 = (String) attributes.get("na2");
    ;
    if (attributes.containsKey("ord"))
      this.ord = (Long) attributes.get("ord");
    ;
    if (attributes.containsKey("pfh"))
      this.pfh = (String) attributes.get("pfh");
    ;
    if (attributes.containsKey("ppo"))
      this.ppo = (Long) attributes.get("ppo");
    ;
    if (attributes.containsKey("rbv"))
      this.rbv = (String) attributes.get("rbv");
    ;
    if (attributes.containsKey("scc"))
      this.scc = (Long) attributes.get("scc");
    ;
    if (attributes.containsKey("shl"))
      this.shl = (Long) attributes.get("shl");
    ;
    if (attributes.containsKey("shr"))
      this.shr = (Long) attributes.get("shr");
    ;
    if (attributes.containsKey("smc"))
      this.smc = (Long) attributes.get("smc");
    ;
    if (attributes.containsKey("tid"))
      this.tid = (Long) attributes.get("tid");
    ;
    if (attributes.containsKey("tre"))
      this.tre = (Long) attributes.get("tre");

    if (attributes.containsKey("veg"))
      this.veg = (Long) attributes.get("veg");

    if (attributes.containsKey("vsp"))
      this.vsp = (Long) attributes.get("vsp");

    if (attributes.containsKey("wcc"))
      this.wcc = (Long) attributes.get("wcc");

    if (attributes.containsKey("wid"))
      this.wid = (String) attributes.get("wid");

    if (attributes.containsKey("wle"))
      this.wle = (Long) attributes.get("wle");

    if (attributes.containsKey("wst"))
      this.wst = (Long) attributes.get("wst");

  }

  public MGCPLandUseType getLandUseType() {
    return landUseType;
  }

  @Override
  public int getType() {
    return landUseType.ordinal();
  }

  @Override
  public void setType(int type) {
    this.landUseType = MGCPLandUseType.values()[type];
  }

  @Override
  public IPolygon getGeom() {
    return (IPolygon) super.getGeom();
  }

  @Override
  public void setGeom(IGeometry geom) {
    super.setGeom(geom);
    this.setArea(geom.area());
  }

  public Long getSmc() {
    return this.smc;
  }

  public void setSmc(Long smc) {
    this.smc = smc;
  }

  public String getNam() {
    return this.nam;
  }

  public void setNam(String nam) {
    this.nam = nam;
  }

  public Long getBac() {
    return this.bac;
  }

  public void setBac(Long bac) {
    this.bac = bac;
  }

  public Long getFuc() {
    return this.fuc;
  }

  public void setFuc(Long fuc) {
    this.fuc = fuc;
  }

  public String getNfi() {
    return this.nfi;
  }

  public void setNfi(String nfi) {
    this.nfi = nfi;
  }

  public String getNfn() {
    return this.nfn;
  }

  public void setNfn(String nfn) {
    this.nfn = nfn;
  }

  public double getArea() {
    return this.area;
  }

  public void setArea(double area) {
    this.area = area;
  }

  public String getHgt() {
    return this.hgt;
  }

  public void setHgt(String hgt) {
    this.hgt = hgt;
  }

  public Long getAcc() {
    return this.acc;
  }

  public void setAcc(Long acc) {
    this.acc = acc;
  }

  public Long getFun() {
    return this.fun;
  }

  public void setFun(Long fun) {
    this.fun = fun;
  }

  public Long getOrd() {
    return this.ord;
  }

  public void setOrd(Long ord) {
    this.ord = ord;
  }

  public void setLandUseType(MGCPLandUseType landUseType) {
    this.landUseType = landUseType;
  }

  public String getAce() {
    return ace;
  }

  public void setAce(String ace) {
    this.ace = ace;
  }

  public String getAle() {
    return ale;
  }

  public void setAle(String ale) {
    this.ale = ale;
  }

  public String getCpyrt_note() {
    return cpyrt_note;
  }

  public void setCpyrt_note(String cpyrt_note) {
    this.cpyrt_note = cpyrt_note;
  }

  public String getDmb() {
    return dmb;
  }

  public void setDmb(String dmb) {
    this.dmb = dmb;
  }

  public String getDmt() {
    return dmt;
  }

  public void setDmt(String dmt) {
    this.dmt = dmt;
  }

  public String getLbv() {
    return lbv;
  }

  public void setLbv(String lbv) {
    this.lbv = lbv;
  }

  public String getNa2() {
    return na2;
  }

  public void setNa2(String na2) {
    this.na2 = na2;
  }

  public String getPfh() {
    return pfh;
  }

  public void setPfh(String pfh) {
    this.pfh = pfh;
  }

  public String getRbv() {
    return rbv;
  }

  public void setRbv(String rbv) {
    this.rbv = rbv;
  }

  public String getSrc_date() {
    return src_date;
  }

  public void setSrc_date(String src_date) {
    this.src_date = src_date;
  }

  public String getSrc_info() {
    return src_info;
  }

  public void setSrc_info(String src_info) {
    this.src_info = src_info;
  }

  public String getTier_note() {
    return tier_note;
  }

  public void setTier_note(String tier_note) {
    this.tier_note = tier_note;
  }

  public long getAce_eval() {
    return ace_eval;
  }

  public void setAce_eval(long ace_eval) {
    this.ace_eval = ace_eval;
  }

  public long getAle_eval() {
    return ale_eval;
  }

  public void setAle_eval(long ale_eval) {
    this.ale_eval = ale_eval;
  }

  public long getCda() {
    return cda;
  }

  public void setCda(long cda) {
    this.cda = cda;
  }

  public long getCsp() {
    return csp;
  }

  public void setCsp(long csp) {
    this.csp = csp;
  }

  public long getFfp() {
    return ffp;
  }

  public void setFfp(long ffp) {
    this.ffp = ffp;
  }

  public long getFmm() {
    return fmm;
  }

  public void setFmm(long fmm) {
    this.fmm = fmm;
  }

  public long getHyp() {
    return hyp;
  }

  public void setHyp(long hyp) {
    this.hyp = hyp;
  }

  public String getTxt() {
    return txt;
  }

  public void setTxt(String txt) {
    this.txt = txt;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getUpd_info() {
    return upd_info;
  }

  public void setUpd_info(String upd_info) {
    this.upd_info = upd_info;
  }

  public String getWid() {
    return wid;
  }

  public void setWid(String wid) {
    this.wid = wid;
  }

  public long getIrg() {
    return irg;
  }

  public void setIrg(long irg) {
    this.irg = irg;
  }

  public long getIss() {
    return iss;
  }

  public void setIss(long iss) {
    this.iss = iss;
  }

  public long getPpo() {
    return ppo;
  }

  public void setPpo(long ppo) {
    this.ppo = ppo;
  }

  public long getScc() {
    return scc;
  }

  public void setScc(long scc) {
    this.scc = scc;
  }

  public long getShl() {
    return shl;
  }

  public void setShl(long shl) {
    this.shl = shl;
  }

  public long getShr() {
    return shr;
  }

  public void setShr(long shr) {
    this.shr = shr;
  }

  public long getSrc_name() {
    return src_name;
  }

  public void setSrc_name(long src_name) {
    this.src_name = src_name;
  }

  public long getTid() {
    return tid;
  }

  public void setTid(long tid) {
    this.tid = tid;
  }

  public long getTre() {
    return tre;
  }

  public void setTre(long tre) {
    this.tre = tre;
  }

  public long getUpd_name() {
    return upd_name;
  }

  public void setUpd_name(long upd_name) {
    this.upd_name = upd_name;
  }

  public long getVeg() {
    return veg;
  }

  public void setVeg(long veg) {
    this.veg = veg;
  }

  public long getVsp() {
    return vsp;
  }

  public void setVsp(long vsp) {
    this.vsp = vsp;
  }

  public long getWcc() {
    return wcc;
  }

  public void setWcc(long wcc) {
    this.wcc = wcc;
  }

  public long getWle() {
    return wle;
  }

  public void setWle(long wle) {
    this.wle = wle;
  }

  public long getWst() {
    return wst;
  }

  public void setWst(long wst) {
    this.wst = wst;
  }

  public long getZval_type() {
    return zval_type;
  }

  public void setZval_type(long zval_type) {
    this.zval_type = zval_type;
  }

  public String getUpd_date() {
    return upd_date;
  }

  public void setUpd_date(String upd_date) {
    this.upd_date = upd_date;
  }

}