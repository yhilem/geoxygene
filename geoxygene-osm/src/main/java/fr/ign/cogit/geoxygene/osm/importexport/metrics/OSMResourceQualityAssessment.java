package fr.ign.cogit.geoxygene.osm.importexport.metrics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.osm.importexport.OSMNode;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMRelation;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.OSMWay;
import fr.ign.cogit.geoxygene.osm.importexport.OsmRelationMember;
import fr.ign.cogit.geoxygene.osm.schema.OSMDefaultFeature;
import fr.ign.cogit.geoxygene.osm.schema.OsmGeometryConversion;

public class OSMResourceQualityAssessment {
	/**
	 * Contributions de la fenêtre chargées depuis PostGIS
	 */
	Set<OSMResource> myJavaObjects;
	/**
	 * Contributions rangées par OSMObject : map référencée par le type de
	 * primitive OSM.
	 * 
	 */
	HashMap<String, HashMap<Long, OSMObject>> myOSMObjects;

	public OSMResourceQualityAssessment(Set<OSMResource> myJavaObjects) {
		this.myJavaObjects = myJavaObjects;
		HashMap<Long, OSMObject> myNodeOSMObjects = groupByOSMObject(myJavaObjects, "OSMNode");
		HashMap<Long, OSMObject> myWayOSMObjects = groupByOSMObject(myJavaObjects, "OSMWay");
		HashMap<Long, OSMObject> myRelOSMObjects = groupByOSMObject(myJavaObjects, "OSMRelation");
		this.myOSMObjects.put("OSMNode", myNodeOSMObjects);
		this.myOSMObjects.put("OSMWay", myWayOSMObjects);
		this.myOSMObjects.put("OSMRelation", myRelOSMObjects);
	}

	/**
	 * Group OSM contributions by object
	 * 
	 * @param myJavaObjects
	 * @param nameGeomOSM
	 *            equals "OSMNode", "OSMWay" or "OSMRelation"
	 * @return a map of OSMobjects identified by their OSM ID
	 */
	public static HashMap<Long, OSMObject> groupByOSMObject(Set<OSMResource> myJavaObjects, String nameGeomOSM) {
		HashMap<Long, OSMObject> myOSMObjects = new HashMap<Long, OSMObject>();
		Iterator<OSMResource> it = myJavaObjects.iterator();
		while (it.hasNext()) {
			OSMResource contribution = it.next();
			if (!contribution.getGeom().getClass().getSimpleName().equals(nameGeomOSM))
				continue;
			if (!myOSMObjects.containsKey(contribution.getId())) {
				// If OSMObject doesn't exist yet, create a new object
				OSMObject objet = new OSMObject(contribution.getId());
				objet.nbVersions = 1;
				objet.addContributor((long) contribution.getUid());
				myOSMObjects.put(contribution.getId(), objet);
				objet.addcontribution(contribution);
				// Indicate if OSM primitive is an OSMNode, OSMWay or
				// OSMRelation
				objet.setPrimitiveGeomOSM(nameGeomOSM);
			} else {
				// If OSMObject already exists : increments the number of
				// version and stores the contribution
				myOSMObjects.get(contribution.getId()).nbVersions += 1;
				myOSMObjects.get(contribution.getId()).addcontribution(contribution);
				// Refresh the list of unique contributors of the OSMobject
				if (!myOSMObjects.get(contribution.getId()).getContributorList().contains(contribution.getUid()))
					myOSMObjects.get(contribution.getId()).addContributor((long) contribution.getUid());
			}
		}
		// Chronologically order every list of contributions in each OSMObject
		for (OSMObject o : myOSMObjects.values()) {
			Collections.sort(o.getContributions(), new OSMResourceComparator());
		}
		return myOSMObjects;
	}

	/**
	 * Count the number of tags (key/value pair) in an OSMResource
	 * 
	 * @param resource
	 * @return number tags
	 */
	public Integer countTags(OSMResource resource) {
		return resource.getTags().size();
	}

	/**
	 * Get the previous contribution version of an OSMResource
	 * 
	 * @param resource
	 * @return the former version of the resource if exists
	 */
	public OSMResource getFormerVersion(OSMResource resource) {
		if (resource.getVersion() == 1)
			return null;
		else {
			// Get the set of OSMObject which primitive is the same as resource
			HashMap<Long, OSMObject> osmObj = this.myOSMObjects.get(resource.getGeom().getClass().getSimpleName());
			// List of all the versions of the current contribution
			List<OSMResource> allVersions = osmObj.get(resource.getId()).getContributions();
			Iterator<OSMResource> it = allVersions.iterator();
			OSMResource former = null;
			int i = 0;
			while (it.hasNext()) {
				OSMResource r = it.next();
				if (r.getVersion() == resource.getVersion())
					break;
				i++;
			}
			if (i > 1)
				former = allVersions.get(i - 1);
			return former;
		}
	}

	/**
	 * Get the set of tags of a contribution's former version
	 * 
	 * @param resource
	 * @return former set of tags
	 */
	public Map<String, String> getFormerTags(OSMResource resource) {
		OSMResource former = getFormerVersion(resource);
		if (former != null)
			return former.getTags();
		else
			return null;
	}

	/**
	 * Get the geometry of a contribution's former version
	 * 
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	public OSMDefaultFeature getFormerGeom(OSMResource resource) throws Exception {
		if (resource.getVersion() == 1)
			return null;
		return getOSMFeature(getFormerVersion(resource));
	}

	/**
	 * Identify the edition type relatively to previous version
	 * 
	 * @param resource
	 * @return creation, modification, delete or revert
	 */
	public EditionType getEditionType(OSMResource resource) {
		if (resource.getVersion() == 1)
			return EditionType.creation;

		if (!resource.isVisible())
			return EditionType.delete;
		// Get former version v-1
		OSMResource versionMinus1 = this.getFormerVersion(resource);
		if (versionMinus1 != null) {
			if (!versionMinus1.isVisible()) {
				// Get former version v-2
				OSMResource versionMinus2 = this.getFormerVersion(versionMinus1);
				if (versionMinus2.equals(resource))
					return EditionType.revert;
			} else
				return EditionType.modification;
		}
		return null;
	}

	public Date getFormerEditionDate(OSMResource resource) {
		return this.getFormerVersion(resource).getDate();
	}

	/**
	 * If the contribution in parameter is a revert, this method returns the
	 * elapsed time between former version.
	 * 
	 * @param resource
	 * @return elapsed time in seconds or null if resource given in parameter is
	 *         not a revert
	 */
	public Double getTimeFrameBeforeRevert(OSMResource resource) {
		if (getEditionType(resource).equals(EditionType.revert)) {
			Double diff = (double) (resource.getDate().getTime() - this.getFormerVersion(resource).getDate().getTime())
					/ 1000;
			return diff;
		}
		return null;

	}

	/**
	 * If contribution is made by night
	 * 
	 * @return true if contribution hour is between 6 PM and 6 AM
	 */
	public boolean isNightTimeContribution(OSMResource resource) {
		Date contributionDate = resource.getDate();
		Calendar c = new GregorianCalendar();
		c.setTime(contributionDate);
		Calendar nineOClock = (Calendar) c.clone();
		nineOClock.set(Calendar.HOUR_OF_DAY, 9);
		nineOClock.set(Calendar.MINUTE, 0);
		Calendar sixOClock = (Calendar) c.clone();
		sixOClock.set(Calendar.HOUR_OF_DAY, 18);
		sixOClock.set(Calendar.MINUTE, 0);
		if (c.before(nineOClock) || c.after(sixOClock))
			return true;
		else
			return false;
	}

	/**
	 * If contribution is made during the weekend
	 * 
	 * @return true if contribution day is Saturday or Sunday
	 */
	public boolean isWeekendContribution(OSMResource resource) {
		Date contributionDate = resource.getDate();
		Calendar c = new GregorianCalendar();
		c.setTime(contributionDate);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
			return true;
		else
			return false;
	}

	/**
	 * Compare with the tags of the previous version and determines whether a
	 * new tag is created in the current tag set.
	 * 
	 * @param resource
	 * @return true if there is a new tag key in the new set of tags
	 */
	public boolean isTagCreation(OSMResource resource) {
		System.out.println("Edition type " + getEditionType(resource));
		if (getEditionType(resource) == null)
			return false;
		if (getEditionType(resource).equals(EditionType.modification)) {
			// Compares the two key sets: if the former key set does not contain
			// the key set of the current version then there is some tag
			// creation in the current version
			if (!this.getFormerTags(resource).keySet().containsAll(resource.getTags().keySet()))
				return true;
		}
		return false;
	}

	/**
	 * Determines whether the is tag modification in the current tag set,
	 * relatively to the previous set of tags.
	 * 
	 * @param resource
	 * @return true if the values of the former tags have been changed
	 */
	public boolean isTagModification(OSMResource resource) {
		if (getEditionType(resource) == null)
			return false;
		if (getEditionType(resource).equals(EditionType.modification)) {
			Iterator<String> it = this.getFormerTags(resource).keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				String formerValue = this.getFormerTags(resource).get(key);
				String currentValue = resource.getTags().get(key);
				if (!formerValue.equals(currentValue))
					return true;
			}
		}
		return false;
	}

	/**
	 * Compare with the tags of the previous version and determines whether a
	 * tag is deleted in the current tag set.
	 * 
	 * @param resource
	 * @return true if a tag key is missing in the new set of tags
	 */
	public boolean isTagDelete(OSMResource resource) {
		if (getEditionType(resource) == null)
			return false;
		if (getEditionType(resource).equals(EditionType.modification)) {
			if (!resource.getTags().keySet().containsAll(this.getFormerTags(resource).keySet()))
				return true;
		}
		return false;
	}

	public OSMDefaultFeature getOSMFeature(OSMResource resource) throws Exception {
		OsmGeometryConversion convertor = new OsmGeometryConversion("4326");
		IGeometry igeom = null;
		if (resource.getGeom().getClass().getSimpleName().equals("OSMNode")) {
			igeom = convertor.convertOsmPoint((OSMNode) resource.getGeom());
		}
		if (resource.getGeom().getClass().getSimpleName().equals("OSMWay")) {
			List<OSMResource> myNodes = this.getWayComposition(resource);
			igeom = convertor.convertOSMLine((OSMWay) resource.getGeom(), myNodes);
		}
		if (resource.getGeom().getClass().getSimpleName().equals("OSMRelation")) {
			// TODO: voir comment fabriquer des géométries complexes avec
			// Geoxygene
		}
		OSMDefaultFeature feature = new OSMDefaultFeature(resource.getContributeur(), igeom, (int) resource.getId(),
				resource.getChangeSet(), resource.getVersion(), resource.getUid(), resource.getDate(),
				resource.getTags());
		return feature;
	}

	public List<OSMResource> getWayComposition(OSMResource way) {
		List<OSMResource> myNodeList = new ArrayList<OSMResource>();
		// A way is composed of a list of nodes
		List<Long> composition = ((OSMWay) way.getGeom()).getVertices();
		for (Long nodeID : composition) {
			myNodeList.add(this.getLatestElement("OSMNode", nodeID, way.getDate()));
		}
		return myNodeList;
	}

	public List<OSMResource> getRelationComposition(OSMResource relation) {
		List<OSMResource> myMemberList = new ArrayList<OSMResource>();
		List<OsmRelationMember> members = ((OSMRelation) relation.getGeom()).getMembers();
		for (OsmRelationMember m : members) {
			if (m.isNode())
				myMemberList.add(this.getLatestElement("OSMNode", m.getRef(), relation.getDate()));
			else if (m.isWay())
				myMemberList.add(this.getLatestElement("OSMWay", m.getRef(), relation.getDate()));
			else if (m.isRelation())
				myMemberList.add(this.getLatestElement("OSMRelation", m.getRef(), relation.getDate()));
		}
		return myMemberList;
	}

	/**
	 * Get the latest OSMResource that composes a complex contribution (Way or
	 * Relation)
	 * 
	 * @param eltID
	 *            ID of the OSMResource to fetch
	 * @param r
	 *            a complex OSMResource (way or relation)
	 * @return the latest contribution regarding the date r
	 */
	public OSMResource getLatestElement(String primitive, Long eltID, Date d) {
		// Get the set of OSMObject which primitive is the same as resource
		HashMap<Long, OSMObject> osmObj = this.myOSMObjects.get(primitive);
		Iterator<OSMResource> it = osmObj.get(eltID).getContributions().iterator();
		OSMResource toDate = null;
		while (it.hasNext()) {
			OSMResource next = it.next();
			// Break the loop as soon as node date is after way date
			if (next.getDate().after(d))
				break;
			toDate = next;
		}
		return toDate;
	}

	/**
	 * 
	 * @param resource
	 * @return the number of key tags which are different from the previous
	 *         version
	 */
	public Integer getNbChangedTags(OSMResource resource) {
		int nbChanges = 0;
		if (isTagModification(resource)) {
			OSMResource former = this.getFormerVersion(resource);
			// Compares the values of the tags
			for (String k : resource.getTags().keySet())
				if (!resource.getTags().get(k).equals(former.getTags().get(k)))
					nbChanges++;
		}
		return nbChanges;
	}

	/**
	 * 
	 * @param resource
	 * @return the number of key tags that are missing in the current version
	 */
	public Integer getNbDeletedTags(OSMResource resource) {
		int nbDeletes = 0;
		if (this.isTagDelete(resource)) {
			OSMResource former = this.getFormerVersion(resource);
			for (String k : former.getTags().keySet())
				if (!resource.getTags().containsKey(k))
					nbDeletes++;
		}
		return nbDeletes;
	}

	/**
	 * 
	 * @param resource
	 * @return The number of new key tags in the current version
	 */
	public Integer getNbAddedTags(OSMResource resource) {
		int nbAdditions = 0;
		if (this.isTagCreation(resource)) {
			OSMResource former = this.getFormerVersion(resource);
			for (String k : resource.getTags().keySet())
				if (!former.getTags().containsKey(k))
					nbAdditions++;
		}
		return nbAdditions;
	}

}
