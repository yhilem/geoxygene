/*******************************************************************************
 * This software is released under the licence CeCILL
 * 
 * see Licence_CeCILL-C_fr.html see Licence_CeCILL-C_en.html
 * 
 * see <a href="http://www.cecill.info/">http://www.cecill.info/a>
 * 
 * @copyright IGN
 ******************************************************************************/
package fr.ign.cogit.geoxygene.osm.importexport.postgis;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ign.cogit.geoxygene.osm.importexport.OSMNode;
import fr.ign.cogit.geoxygene.osm.importexport.OSMRelation;
import fr.ign.cogit.geoxygene.osm.importexport.OSMRelation.RoleMembre;
import fr.ign.cogit.geoxygene.osm.importexport.OSMRelation.TypeRelation;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.OSMWay;
import fr.ign.cogit.geoxygene.osm.importexport.OsmRelationMember;
import fr.ign.cogit.geoxygene.osm.importexport.PrimitiveGeomOSM;
import twitter4j.JSONException;
import twitter4j.JSONObject;

public class LoadFromPostGIS {
	public String host;
	public String port;
	public String dbName;
	public String dbUser;
	public String dbPwd;
	public Set<OSMResource> myJavaObjects;
	// public HashMap<Long, OSMContributor> myContributors;
	public Map<OsmRelationMember, Long> OsmRelMbList;
	public Set<OSMResource> myJavaRelations;

	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		// loader.relationEvolution(bbox, timespan);
		// loader.selectRelations(bbox, timespan);

		Double[] bbox = { 2.3322, 48.8489, 2.3634, 48.8627 };
		String[] timespan = { "2010-01-01", "2010-02-01" };
		loader.getDataFrombbox(bbox, timespan);

	}

	public LoadFromPostGIS(String host, String port, String dbName, String dbUser, String dbPwd) {
		this.host = host;
		this.port = port;
		this.dbName = dbName;
		this.dbUser = dbUser;
		this.dbPwd = dbPwd;
		this.myJavaObjects = new HashSet<OSMResource>();
		// this.myOSMObjects = new HashMap<Long, OSMObject>();
		// this.myContributors = new HashMap<Long, OSMContributor>();
		this.OsmRelMbList = new HashMap<OsmRelationMember, Long>();
		this.myJavaRelations = new HashSet<OSMResource>();
	}

	/**
	 * Récupère les coordonnées lon_min, lat_min, lon_max, lat_max de la commune
	 * Attention: il faut lancer le script
	 * 
	 * @param city
	 *            Nom de la commune
	 * @param timestamp
	 *            Date à laquelle on cherche la dernière version des frontières
	 * @return les coordonnées géographiques {xmin, ymin, xmax, ymax} de la
	 *         commune
	 * @throws Exception
	 */
	public Double[] getCityBoundary(String city, String timestamp) throws Exception {
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			String query = "SELECT idrel FROM  relation WHERE tags -> 'boundary' = 'administrative' ANd tags->'admin_level'='8' "
					+ "AND tags-> 'name'='" + city + "' AND datemodif <= '" + timestamp
					+ "' ORDER BY vrel DESC LIMIT 1;";
			ResultSet r = s.executeQuery(query);
			// Get relation members
			Long idrel = null;
			while (r.next()) {
				idrel = r.getLong("idrel");
			}
			System.out.println("idrel = " + idrel);

			query = "DROP TABLE IF EXISTS enveloppe ;"
					+ "CREATE TABLE enveloppe (lon_min numeric DEFAULT 180,lat_min numeric DEFAULT 90,lon_max numeric DEFAULT -180,lat_max numeric DEFAULT -90);"
					+ "INSERT INTO enveloppe VALUES (180, 90, -180 ,-90);";
			query += "SELECT relation_boundary(" + idrel + ", '" + timestamp + "');"; // run
																						// first
																						// pgScript
																						// "relation_boundary.sql"
			s.execute(query);
			query = "SELECT * FROM enveloppe LIMIT 1;";
			ResultSet r1 = s.executeQuery(query);
			Double xmin = null, ymin = null, xmax = null, ymax = null;
			while (r1.next()) {
				xmin = r1.getDouble("lon_min");
				ymin = r1.getDouble("lat_min");
				xmax = r1.getDouble("lon_max");
				ymax = r1.getDouble("lat_max");
				System.out.println(xmin);
				System.out.println(ymin);
				System.out.println(xmax);
				System.out.println(ymax);
			}

			Double[] borders = { xmin, ymin, xmax, ymax };
			s.close();
			conn.close();
			return borders;
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Loads the latest version of every building that was created inside the
	 * input borders. The buildings can be visible or not.
	 * 
	 * @param borders
	 *            {xmin, ymin, xmax, ymax}
	 * @param timestamp
	 *            date du snapshot
	 * @throws Exception
	 */
	public void getSnapshotBuilding(Double[] borders, String timestamp) throws Exception {
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

			String uniqueBuildingQuery = "SELECT DISTINCT ON (id) * FROM way WHERE tags->'building'='yes' "
					+ "AND lon_min >= " + borders[0] + "AND lat_min>= " + borders[1] + "AND lon_max<= " + borders[2]
					+ "AND lat_max<= " + borders[3] + "ORDER BY id, datemodif DESC";
			String infoBuildingQuery = "SELECT max(way.vway) as max, way.id FROM way, (" + uniqueBuildingQuery
					+ ") as unique_building WHERE way.id = unique_building.id AND way.datemodif <='" + timestamp
					+ "' GROUP BY way.id";
			String query = "SELECT way.idway, way.id, way.uid, way.vway, way.changeset, way.username, way.datemodif, hstore_to_json(way.tags), way.composedof, way.visible "
					+ "FROM way, (" + infoBuildingQuery
					+ ") AS info_building WHERE way.id=info_building.id AND way.vway=info_building.max;";

			ResultSet r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, "way");
			s.close();
			conn.close();
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Load the evolution of the buildings that are inside the input border,
	 * including : - the later versions of the buildings selected at begin date
	 * until end date - the versions of the buildings created during the input
	 * timespan until end date
	 * 
	 * @param borders
	 * @param timespan
	 * @throws Exception
	 */
	public void getEvolutionBuilding(Double[] borders, String[] timespan) throws Exception {
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			// Query the evolution of all buildings which where selected at
			// timespan[0] until timespan[1]
			String uniqueBuildingQuery = "SELECT DISTINCT ON (id) * FROM way WHERE tags->'building'='yes' "
					+ "AND lon_min >= " + borders[0] + "AND lat_min>= " + borders[1] + "AND lon_max<= " + borders[2]
					+ "AND lat_max<= " + borders[3] + "ORDER BY id, datemodif DESC";
			String infoBuildingQuery = "SELECT max(way.vway) as max, way.id FROM way, (" + uniqueBuildingQuery
					+ ") as unique_building WHERE way.id = unique_building.id AND way.datemodif <='" + timespan[0]
					+ "' GROUP BY way.id";
			String query = "SELECT way.idway, way.id, way.uid, way.vway, way.changeset, way.username, way.datemodif, hstore_to_json(way.tags), way.composedof, way.visible "
					+ "FROM way, (" + infoBuildingQuery
					+ ") AS info_building WHERE way.id = info_building.id AND way.vway > info_building.max AND way.datemodif <= '"
					+ timespan[1] + "' ORDER BY way.id;";
			ResultSet r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, "way");

			// Query the buildings that were created inside timespan and all the
			// versions which fall between this time interval
			String createdBuildings = "SELECT DISTINCT ON (id) * FROM way WHERE tags->'building'='yes' "
					+ "AND lon_min >= " + borders[0] + "AND lat_min>=" + borders[1] + " AND lon_max<=" + borders[2]
					+ " AND lat_max<=" + borders[3] + "AND vway = 1 AND datemodif > '" + timespan[0]
					+ "' AND datemodif <= '" + timespan[1] + "'";

			query = "SELECT way.idway, way.id, way.uid, way.vway, way.changeset, way.username, way.datemodif, hstore_to_json(way.tags), way.composedof, way.visible "
					+ "FROM way, (" + createdBuildings
					+ ") AS bati_cree WHERE way.id = bati_cree.id AND way.datemodif <='" + timespan[1] + "';";

			r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, "way");
			s.close();
			conn.close();
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Get the latest version of all nodes (visible or not) that are contained
	 * inside the borders
	 * 
	 * @param borders
	 * @param timestamp
	 * @throws Exception
	 */
	public void getSnapshotNodes(Double[] borders, String timestamp) throws Exception {
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

			String uniqueNodeQuery = "SELECT DISTINCT ON (id) * FROM node WHERE lon >= " + borders[0] + "AND lat>= "
					+ borders[1] + "AND lon<= " + borders[2] + "AND lat<= " + borders[3]
					+ " ORDER BY id, datemodif DESC";
			String infoNodeQuery = "SELECT max(node.vnode) as max, node.id FROM node, (" + uniqueNodeQuery
					+ ") AS unique_nodes WHERE node.id = unique_nodes.id AND node.datemodif <='" + timestamp
					+ "' GROUP BY node.id";
			String query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat "
					+ " FROM node, (" + infoNodeQuery
					+ ") AS info_node WHERE node.id=info_node.id AND node.vnode=info_node.max;";

			ResultSet r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, "node");
			s.close();
			conn.close();
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Retrieves OSM nodes from a PostGIS database according to spatiotemporal
	 * parameters: 1) select the later versions of all queried nodes a
	 * timespan[0] 2) select all created nodes inside timespan including their
	 * later versions until timespan[1]
	 * 
	 * @param borders
	 * @param timespan
	 * @throws Exception
	 */
	public void getEvolutionNode(Double[] borders, String[] timespan) throws Exception {
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			// Query the evolution of all buildings which where selected at
			// timespan[0] until timespan[1]
			String uniqueNodeQuery = "SELECT DISTINCT ON (id) * FROM node WHERE lon >= " + borders[0] + " AND lat>= "
					+ borders[1] + " AND lon<= " + borders[2] + " AND lat<= " + borders[3]
					+ " ORDER BY id, datemodif DESC";
			String infoNodeQuery = "SELECT max(node.vnode) as max, node.id FROM node, (" + uniqueNodeQuery
					+ ") as unique_node WHERE node.id = unique_node.id AND node.datemodif <='" + timespan[0]
					+ "' GROUP BY node.id";
			String query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat "
					+ " FROM node, (" + infoNodeQuery
					+ ") AS info_node WHERE node.id = info_node.id AND node.vnode > info_node.max AND node.datemodif <= '"
					+ timespan[1] + "' ORDER BY node.id;";
			ResultSet r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, "node");

			// Query the buildings that were created inside timespan and all the
			// versions which fall between this time interval
			String createdNodes = "SELECT DISTINCT ON (id) * FROM node WHERE lon >= " + borders[0] + " AND lat>="
					+ borders[1] + " AND lon<=" + borders[2] + " AND lat<=" + borders[3]
					+ "AND vnode = 1 AND datemodif > '" + timespan[0] + "' AND datemodif <= '" + timespan[1] + "'";

			query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat "
					+ "FROM node, (" + createdNodes
					+ ") AS node_cree WHERE node.id = node_cree.id AND node.datemodif <='" + timespan[1] + "';";

			r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, "node");
			s.close();
			conn.close();
		} catch (Exception e) {
			throw e;
		}

	}

	/**
	 * Récupère un snapshot des nodes & ways à t1 et leurs évolutions entre t1
	 * et t2
	 * 
	 * @param bbox
	 * @param timespan
	 * @throws Exception
	 */
	public void getDataFrombbox(Double[] bbox, String[] timespan) throws Exception {
		// Nodes at t1
		selectNodesInit(bbox, timespan[0].toString());
		// Nodes between t1 and t2
		selectNodes(bbox, timespan);

		// Ways at t1
		selectWaysInit(bbox, timespan[0].toString());
		// Ways between t1 and t2
		selectWays(bbox, timespan);
	}

	public ArrayList<OsmRelationMember> getRelationMemberList(int idrel) throws Exception {
		String query = "SELECT * FROM relationmember WHERE idrel=" + idrel;
		java.sql.Connection conn;
		ArrayList<OsmRelationMember> members = new ArrayList<OsmRelationMember>();
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			while (r.next()) {
				OsmRelationMember mb = writeRelationMember(r);
				members.add(mb);
			}
			s.close();
			conn.close();
		} catch (Exception e) {
			throw e;
		}
		return members;

	}

	public TypeRelation getRelationType(ResultSet r) throws SQLException {
		// Parses hstore_to_json in order to get relation type which is needed
		// to write an OSMRelation
		String value = " ";
		if (!r.getString("hstore_to_json").toString().equalsIgnoreCase("{}")) {
			try {
				JSONObject obj = new JSONObject(r.getString("hstore_to_json"));
				int i = 0;
				while (value == " " && i < obj.names().length()) {
					String key = obj.names().getString(i);
					if (key.toString().equalsIgnoreCase("type")) {
						value = obj.getString(key);
					}
					i++;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(" TypeRelation = " + value);
		return TypeRelation.valueOfTexte(value);

	}

	/**
	 * @deprecated
	 * @param bbox
	 * @param timespan
	 * @throws Exception
	 */
	public void relationEvolution(Double[] bbox, String[] timespan) throws Exception {
		String dropViewQuery = "DROP VIEW IF EXISTS "
				+ "relationmemberselected, numberselectedrelmb, allrelationmember, "
				+ "totalnumberselectedrelmb, idrelinbbox, idinbbox CASCADE;"
				+ "DROP VIEW IF EXISTS relmbcomposedofrel,nbrelmbcomposedofrel,allrelmbcomposedofrel,"
				+ "totalnbrelmbcomposedofrel,idrelofrelinbbox CASCADE;";

		String subqueryNode = "SELECT DISTINCT ON (id) id FROM node WHERE datemodif >\'" + timespan[0].toString()
				+ "\' AND datemodif <= \'" + timespan[1].toString() + "\' AND node.geom && ST_MakeEnvelope("
				+ bbox[0].toString() + "," + bbox[1].toString() + "," + bbox[2].toString() + "," + bbox[3].toString()
				+ ")";

		String subqueryWay = "SELECT DISTINCT ON (id) id FROM way WHERE datemodif >\'" + timespan[0].toString()
				+ "\' AND datemodif <= \'" + timespan[1].toString() + "\' AND lon_min >=" + bbox[0].toString()
				+ " AND lat_min >=" + bbox[1].toString() + " AND lon_max <=" + bbox[1].toString() + " AND lat_max <= "
				+ bbox[3].toString();

		String query = "CREATE VIEW relationmemberselected AS SELECT * FROM relationmember WHERE (idmb in ("
				+ subqueryWay + ") AND typemb = 'Way') OR (idmb in (" + subqueryNode
				+ ") AND typemb = 'Node') ORDER BY idrel;";

		// Compte le nombre de membres sélectionnés par relation
		query += "CREATE VIEW numberselectedrelmb AS "
				+ "SELECT idrel, COUNT(*) FROM relationmemberselected GROUP BY idrel;";

		// Selectionne la totalité des membres des relations précédemment
		// sélectionnées
		query += "CREATE VIEW allrelationmember AS "
				+ "SELECT * FROM relationmember WHERE idrel in (SELECT idrel FROM numberselectedrelmb);";

		// Compte le nombre total de membres pour chaque relation
		// précédemment
		// sélectionnée
		query += "CREATE VIEW totalnumberselectedrelmb AS "
				+ "SELECT idrel, COUNT(*) FROM allrelationmember GROUP BY idrel;";

		// Sélection des relations qui ont tous leurs membres dans la table
		// numberselectedrelmb
		query += "CREATE VIEW idrelinbbox AS " + "SELECT a.* FROM numberselectedrelmb a, totalnumberselectedrelmb b "
				+ "WHERE a.idrel = b.idrel AND a.count = b.count;";

		// Même raisonnement pour les relations composées des relations
		// contenues dans la fenêtre
		query += "CREATE VIEW relmbcomposedofrel AS SELECT * FROM relationmember WHERE idmb in ("
				+ "SELECT DISTINCT ON (id) id FROM relation WHERE datemodif >\'" + timespan[0].toString()
				+ "\' AND datemodif <=\'" + timespan[1].toString() + "\' AND idrel in (SELECT idrel FROM idrelinbbox)"
				+ ");";

		// Compte le nombre de membres sélectionnés par relation
		query += "CREATE VIEW nbrelmbcomposedofrel AS "
				+ "SELECT idrel, COUNT(*) FROM relmbcomposedofrel	GROUP BY idrel;";

		// Selectionne la totalité des membres des relations précédemment
		// sélectionnées
		query += "CREATE VIEW allrelmbcomposedofrel AS "
				+ "SELECT * FROM relationmember WHERE idrel in (SELECT idrel FROM nbrelmbcomposedofrel);";

		// Compte le nombre total de membres pour chaque relation
		// précédemment
		// sélectionnée
		query += "CREATE VIEW totalnbrelmbcomposedofrel AS "
				+ "SELECT idrel, COUNT(*)	FROM allrelationmember GROUP BY idrel;";

		// Sélection des relations qui ont tous leurs membres dans la table
		// nbrelmbcomposedofrel
		query += "CREATE VIEW idrelofrelinbbox AS "
				+ "SELECT a.idrel, a.count FROM nbrelmbcomposedofrel a, totalnbrelmbcomposedofrel b "
				+ "WHERE a.idrel = b.idrel AND a.count = b.count;";

		// Création d'un trigger pour mettre à jour la vue idrelinbbox
		// (attention, lancer un PgScript sur PgAdmin avant)
		query += "DROP TRIGGER IF EXISTS idrelinbbox_trig ON idrelinbbox;";
		query += "CREATE TRIGGER idrelinbbox_trig "
				+ "INSTEAD OF INSERT ON idrelinbbox FOR EACH ROW EXECUTE PROCEDURE miseajour_idrelinbbox();";
		query += "INSERT INTO idrelinbbox SELECT idrel, count FROM idrelofrelinbbox; ";
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			System.out.println(dropViewQuery + query);
			s.executeQuery(dropViewQuery + query);
			System.out.println("------- Query Executed -------");
			s.close();
			conn.close();
		} catch (Exception e) {
			// throw e;
		}

	}

	/**
	 * @deprecated
	 * @param bbox
	 * @param beginDate
	 * @throws Exception
	 */
	public void relationSnapshot(Double[] bbox, String beginDate) throws Exception {
		String dropViewQuery = "DROP VIEW IF EXISTS "
				+ "relationmemberselected, numberselectedrelmb, allrelationmember, "
				+ "totalnumberselectedrelmb, idrelinbbox, relationinbbox, idinbbox CASCADE; "
				+ "DROP VIEW IF EXISTS relmbcomposedofrel,nbrelmbcomposedofrel,allrelmbcomposedofrel,"
				+ "totalnbrelmbcomposedofrel,idrelofrelinbbox CASCADE;"
				+ "DROP TRIGGER IF EXISTS idrelinbbox_trig ON idrelinbbox;";
		// Recuperation des membres de relation de type node ou de type way
		String subqueryNode = "SELECT DISTINCT ON (id) id FROM node WHERE datemodif <= \'" + beginDate
				+ "\' AND node.geom && ST_MakeEnvelope(" + bbox[0].toString() + "," + bbox[1].toString() + ","
				+ bbox[2].toString() + "," + bbox[3].toString() + ") ORDER BY id, vnode DESC";
		String subqueryWay = "SELECT DISTINCT ON (id) id FROM way WHERE datemodif <= \'" + beginDate
				+ "\' AND lon_min >=" + bbox[0].toString() + " AND lat_min >=" + bbox[1].toString() + " AND lon_max <="
				+ bbox[1].toString() + " AND lat_max <= " + bbox[3].toString() + " ORDER BY id, datemodif DESC";

		String query = "CREATE VIEW relationmemberselected AS " + "SELECT * FROM relationmember WHERE (idmb in ("
				+ subqueryWay + ") AND typemb = \'Way\') " + "OR (idmb in (" + subqueryNode
				+ ") AND typemb = 'Node') ORDER BY idrel;";

		// Compte le nombre de membres sélectionnés par relation
		query += "CREATE VIEW numberselectedrelmb AS "
				+ "SELECT idrel, COUNT(*)	FROM relationmemberselected GROUP BY idrel;";

		// Selectionne la totalité des membres des relations précédemment
		// sélectionnées
		query += "CREATE VIEW allrelationmember AS "
				+ "SELECT * FROM relationmember WHERE idrel in (SELECT idrel FROM numberselectedrelmb);";

		// Compte le nombre total de membres pour chaque relation précédemment
		// sélectionnée
		query += "CREATE VIEW totalnumberselectedrelmb AS "
				+ "SELECT idrel, COUNT(*) FROM allrelationmember GROUP BY idrel;";

		// Sélection des relations qui ont tous leurs membres dans la table
		// numberselectedrelmb
		query += "CREATE VIEW idrelinbbox AS "
				+ "SELECT a.* FROM numberselectedrelmb a, totalnumberselectedrelmb b WHERE a.idrel = b.idrel AND a.count = b.count;";

		query += "CREATE VIEW idinbbox AS "
				+ "SELECT DISTINCT ON (id) id FROM relation WHERE idrel in (SELECT idrel FROM idrelinbbox);";

		// Même raisonnement pour les relations composées des relations
		// contenues dans la fenêtre
		query += "CREATE VIEW relmbcomposedofrel AS "
				+ "SELECT * FROM relationmember WHERE idmb in (SELECT id FROM idinbbox);";
		// Compte le nombre de membres sélectionnés par relation
		query += "CREATE VIEW nbrelmbcomposedofrel AS "
				+ "SELECT idrel, COUNT(*) FROM relmbcomposedofrel	GROUP BY idrel;";

		// Selectionne la totalité des membres des relations précédemment
		// sélectionnées
		query += "CREATE VIEW allrelmbcomposedofrel AS "
				+ "SELECT * FROM relationmember WHERE idrel in (SELECT idrel FROM nbrelmbcomposedofrel);";

		// Compte le nombre total de membres pour chaque relation précédemment
		// sélectionnée
		query += "CREATE VIEW totalnbrelmbcomposedofrel AS "
				+ "SELECT idrel, COUNT(*)	FROM allrelationmember GROUP BY idrel;";

		// Sélection des relations qui ont tous leurs membres dans la table
		// nbrelmbcomposedofrel
		query += "CREATE VIEW idrelofrelinbbox AS "
				+ "SELECT a.idrel, a.count FROM nbrelmbcomposedofrel a, totalnbrelmbcomposedofrel b "
				+ "WHERE a.idrel = b.idrel AND a.count = b.count;";

		// Création d'un trigger pour mettre à jour la vue idrelinbbox
		// (attention, lancer un PgScript sur PgAdmin avant)
		query += "CREATE TRIGGER idrelinbbox_trig "
				+ "INSTEAD OF INSERT ON idrelinbbox FOR EACH ROW EXECUTE PROCEDURE miseajour_idrelinbbox();";
		query += "INSERT INTO idrelinbbox SELECT idrel, count FROM idrelofrelinbbox; ";

		// Sélection de la dernière version de chaque relation à la date t1
		// (=2010-01-01 ici)
		query += "CREATE VIEW relationinbbox AS SELECT * FROM "
				+ "(SELECT DISTINCT ON (id) * FROM relation WHERE datemodif <=\'" + beginDate
				+ "\' ORDER BY id,datemodif DESC) as relationselected WHERE id in (SELECT * FROM idinbbox);";

		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println(dropViewQuery + query);
			s.executeQuery(dropViewQuery + query);
			System.out.println("------- Query Executed -------");
			s.close();
			conn.close();
		} catch (Exception e) {
			// throw e;
		}
	}

	public void selectFromDB(String query, String osmDataType) throws Exception {
		java.sql.Connection conn;
		try {
			String url = "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.dbName;
			conn = DriverManager.getConnection(url, this.dbUser, this.dbPwd);
			Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet r = s.executeQuery(query);
			System.out.println("------- Query Executed -------");
			writeOSMResource(r, osmDataType);
			s.close();
			conn.close();
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Gets the latest visible nodes at date beginDate
	 * 
	 * @param beginDate
	 *            : timespan lower boundary
	 * @throws SQLException
	 */
	public void selectNodesInit(Double[] bbox, String beginDate) throws SQLException {
		String uniqueNodesQuery = "SELECT DISTINCT ON (id) * FROM node	WHERE lon >=" + bbox[0] + " AND lat>= "
				+ bbox[1] + " AND lon<=" + bbox[2] + " AND lat<=" + bbox[3] + " ORDER BY id, datemodif DESC";
		String infoNodesQuery = "SELECT max(node.vnode) as max, node.id FROM node,(" + uniqueNodesQuery
				+ ") as unique_nodes WHERE node.id = unique_nodes.id AND node.datemodif <='" + beginDate
				+ "' GROUP BY node.id";
		// Query visible attribute
		String query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat FROM node, ("
				+ infoNodesQuery
				+ ") as info_node WHERE node.id=info_node.id AND node.vnode=info_node.max AND node.visible IS TRUE;";
		// Query database
		try {
			selectFromDB(query, "node");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Retrieves OSM nodes from a PostGIS database according to spatiotemporal
	 * parameters: 1) select the later versions of visible nodes at timespan[0]
	 * until timespan[1]; 2) select all created nodes inside timespan including
	 * their later versions until timespan[1]
	 * 
	 * @param bbox
	 *            contains the bounding box coordinates in the following order :
	 *            [minLon, minLat, maxLon, maxLat]
	 * 
	 * @param timespan
	 *            is composed of the begin date and end date written in
	 *            timestamp format
	 * @throws SQLException
	 */
	public void selectNodes(Double[] bbox, String[] timespan) throws SQLException {
		String uniqueNodesQuery = "SELECT DISTINCT ON (id) * FROM node	WHERE lon >=" + bbox[0] + " AND lat>= "
				+ bbox[1] + " AND lon<=" + bbox[2] + " AND lat<=" + bbox[3] + " ORDER BY id, datemodif DESC";
		String infoNodesQuery = "SELECT max(node.vnode) as max, node.id FROM node,(" + uniqueNodesQuery
				+ ") as unique_nodes WHERE node.id = unique_nodes.id AND node.datemodif <='" + timespan[0]
				+ "' GROUP BY node.id";
		String visibleNodeBeginDate = "SELECT node.* FROM node, (" + infoNodesQuery
				+ ") as info_node WHERE node.id=info_node.id AND node.vnode=info_node.max AND node.visible IS TRUE";
		String query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat FROM node, ("
				+ visibleNodeBeginDate + ") AS visible_node_t1 "
				+ " WHERE node.id = visible_node_t1.id AND node.vnode > visible_node_t1.vnode AND node.datemodif <= '"
				+ timespan[1] + "';";
		// Query database
		try {
			selectFromDB(query, "node");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(query);
		}
		// Query the nodes that were created inside timespan and all the
		// versions which fall between this time interval
		String createdNodes = "SELECT DISTINCT ON (id) * FROM node WHERE lon >= " + bbox[0] + " AND lat>=" + bbox[1]
				+ " AND lon<=" + bbox[2] + " AND lat<=" + bbox[3] + "AND vnode = 1 AND datemodif > '" + timespan[0]
				+ "' AND datemodif <= '" + timespan[1] + "'";

		query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat "
				+ "FROM node, (" + createdNodes + ") AS node_cree WHERE node.id = node_cree.id AND node.datemodif <='"
				+ timespan[1] + "';";
		// Query database
		try {
			selectFromDB(query, "node");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(query);
		}
	}

	/**
	 * Gets the latest visible ways at date beginDate
	 * 
	 * @param bbox
	 * @param beginDate
	 * @throws SQLException
	 */
	public void selectWaysInit(Double[] bbox, String beginDate) throws SQLException {
		/*
		 * String query =
		 * "SELECT idway, id, uid, vway, changeset, username, datemodif, hstore_to_json(tags), composedof, visible FROM ("
		 * + "SELECT DISTINCT ON (id) * FROM way WHERE datemodif <= \'" +
		 * beginDate + "\' AND lon_min >=" + bbox[0].toString() +
		 * " AND lat_min >=" + bbox[1].toString() + " AND lon_max <=" +
		 * bbox[2].toString() + "AND lat_max <=" + bbox[3].toString() +
		 * " ORDER BY id, datemodif DESC) AS way_selected;";
		 */

		String uniqueWaysQuery = "SELECT DISTINCT ON (id) * FROM way WHERE lon_min >=" + bbox[0] + " AND lat_min>= "
				+ bbox[1] + " AND lon_max<=" + bbox[2] + " AND lat_max<=" + bbox[3] + " ORDER BY id, datemodif DESC";
		String infoWaysQuery = "SELECT max(way.vway) as max, way.id FROM way,(" + uniqueWaysQuery
				+ ") as unique_ways WHERE way.id = unique_ways.id AND way.datemodif <='" + beginDate
				+ "' GROUP BY way.id";
		// Query visible attribute
		String query = "SELECT way.id,way.uid,way.vway, way.changeset, way.username, way.datemodif, hstore_to_json(way.tags), way.visible,way.composedof FROM way, ("
				+ infoWaysQuery
				+ ") as info_way WHERE way.id=info_way.id AND way.vway=info_way.max AND way.visible IS TRUE;";
		// Query database
		try {
			selectFromDB(query, "way");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Retrieves OSM ways from a PostGIS database according to spatiotemporal
	 * parameters
	 * 
	 * @param bbox
	 *            contains the bounding box coordinates in the following order :
	 *            [minLon, minLat, maxLon, maxLat]
	 * 
	 * @param timespan
	 *            is composed of the begin date and end date written in
	 *            timestamp format
	 * @throws Exception
	 */
	public void selectWays(Double[] bbox, String[] timespan) throws Exception {
		// Query later versions of visible ways at begin date
		String uniqueWaysQuery = "SELECT DISTINCT ON (id) * FROM way WHERE lon_min >=" + bbox[0] + " AND lat_min>= "
				+ bbox[1] + " AND lon_min<=" + bbox[2] + " AND lat_min<=" + bbox[3] + " ORDER BY id, datemodif DESC";
		String infoWaysQuery = "SELECT max(way.vway) as max, way.id FROM way,(" + uniqueWaysQuery
				+ ") as unique_ways WHERE way.id = unique_ways.id AND way.datemodif <='" + timespan[0]
				+ "' GROUP BY way.id";
		String visibleWayBeginDate = "SELECT way.* FROM way, (" + infoWaysQuery
				+ ") as info_way WHERE way.id=info_way.id AND way.vway=info_way.max AND way.visible IS TRUE";
		String query = "SELECT way.id,way.uid,way.vway, way.changeset, way.username, way.datemodif, hstore_to_json(way.tags), way.visible, way.composedof FROM way, ("
				+ visibleWayBeginDate + ") AS visible_way_t1 "
				+ " WHERE way.id = visible_way_t1.id AND way.vway > visible_way_t1.vway AND way.datemodif <= '"
				+ timespan[1] + "';";
		// Query database
		try {
			selectFromDB(query, "way");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(query);
			// throw e;
		}
		// Query the nodes that were created inside timespan and all the
		// versions which fall between this time interval
		String createdWays = "SELECT DISTINCT ON (id) * FROM way WHERE lon_min >= " + bbox[0] + " AND lat_min>="
				+ bbox[1] + " AND lon_min<=" + bbox[2] + " AND lat_min<=" + bbox[3] + "AND vway = 1 AND datemodif > '"
				+ timespan[0] + "' AND datemodif <= '" + timespan[1] + "'";

		query = "SELECT way.idway, way.id,way.uid,way.vway, way.changeset, way.username, way.datemodif, hstore_to_json(way.tags), way.visible, way.composedof "
				+ "FROM way, (" + createdWays + ") AS way_cree WHERE way.id = way_cree.id AND way.datemodif <='"
				+ timespan[1] + "';";
		// Query database
		try {
			selectFromDB(query, "way");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @deprecated
	 * @param bbox
	 * @param beginDate
	 * @throws Exception
	 */
	public void selectRelInit(Double[] bbox, String beginDate) throws Exception {

		relationSnapshot(bbox, beginDate);

		// Query database
		try {
			// Selectionne les membres de relation
			String query = "SELECT * FROM relationmember WHERE idrel in (SELECT idrel FROM relationinbbox);";
			// Recherche les membres de relations avant t1 et les écrit
			selectFromDB(query, "relationmember");
			// Recherche dans la table relation et écriture d'OSMRelation
			query = "SELECT idrel, id, uid, vrel, changeset, username, datemodif, hstore_to_json(tags), visible "
					+ "FROM (SELECT DISTINCT ON (id) * FROM relation WHERE datemodif <=\'" + beginDate + "\'"
					+ "ORDER BY id,datemodif DESC) as relationselected WHERE id in (SELECT * FROM idinbbox);";
			selectFromDB(query, "relation");
			// Creates Java object
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// throw e;
		}

		// Cherche si les relations qui viennent d'être créées sont également
		// des membres de relation qui sont dans la BBOX étudiée

	}

	/**
	 * @deprecated
	 * @param bbox
	 * @param timespan
	 * @throws Exception
	 */
	public void selectRelations(Double[] bbox, String[] timespan) throws Exception {
		relationEvolution(bbox, timespan);

		// Query database
		try {
			// Selectionne les membres de relation
			String query = "SELECT * FROM relationmember WHERE idrel in (SELECT idrel FROM idrelinbbox);";
			// Recherche les membres de relations avant t1 et les écrit
			selectFromDB(query, "relationmember");
			// Recherche dans la table relation et écriture d'OSMRelation
			query = "SELECT idrel, id, uid, vrel, changeset, username, datemodif, hstore_to_json(tags), visible FROM "
					+ "(SELECT DISTINCT ON (id) * FROM relation WHERE datemodif >\'" + timespan[0].toString()
					+ "\' AND datemodif <= \'" + timespan[1].toString() + "\') as relationselected"
					+ " WHERE idrel in (SELECT idrel FROM idrelinbbox);";
			selectFromDB(query, "relation");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw e;
		}

	}

	public void writeNode(ResultSet r) throws SQLException {
		System.out.println("Writing node...");
		DateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");
		Date date = null;
		try {
			date = formatDate.parse(r.getString("datemodif"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// System.out.println(r.getString("datemodif"));
		OSMResource myOsmResource = new OSMResource(r.getString("username"),
				new OSMNode(r.getDouble("lat"), r.getDouble("lon")), r.getLong("id"), r.getInt("changeset"),
				r.getInt("vnode"), r.getInt("uid"), date);
		// Visible : peut être null
		myOsmResource.setVisible(r.getBoolean("visible"));
		// Add tags if exist
		if (!r.getString("hstore_to_json").toString().equalsIgnoreCase("{}")) {
			try {
				JSONObject obj = new JSONObject(r.getString("hstore_to_json"));
				for (int i = 0; i < obj.names().length(); i++) {
					String key = obj.names().getString(i);
					String value = obj.getString(key);
					System.out.println(" Ajout du tag {" + key + ", " + value + "}");
					myOsmResource.addTag(key, value);
					if (key.equalsIgnoreCase("source")) {
						myOsmResource.setSource(value);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.myJavaObjects.add(myOsmResource);
		System.out.println("Java object created ! " + "\nid = " + myOsmResource.getId() + "\nusername = "
				+ myOsmResource.getContributeur() + "     uid = " + myOsmResource.getUid() + "\nvnode = "
				+ myOsmResource.getVersion() + "\ndate = " + myOsmResource.getDate() + "   Changeset = "
				+ myOsmResource.getChangeSet());
		System.out.println("-------------------------------------------");
	}

	public void writeOSMResource(ResultSet r, String osmDataType) throws Exception {

		while (r.next()) {
			System.out.println("writeOSMResource");
			if (osmDataType.equalsIgnoreCase("node")) {
				System.out.println("Writing resource with type node...");
				System.out.println("r.next is a node");
				writeNode(r);
			}
			if (osmDataType.equalsIgnoreCase("way")) {
				System.out.println("Writing resource with type way...");
				writeWay(r);
			}
			if (osmDataType.equalsIgnoreCase("relation")) {
				System.out.println("Writing resource with type relation...");
				// writeRelation(r);
				writeRelation(r);
			}
			if (osmDataType.equalsIgnoreCase("relationmember")) {
				// if (!idRelPrev.equals(r.getLong("idrel"))) {
				// String query = "SELECT idrel, id, uid, vrel, changeset,
				// username, datemodif, hstore_to_json(tags) FROM relation WHERE
				// idrel="
				// + idRelPrev;
				// selectFromDB(query, "relation");
				// }
				// Adds a new relation member in the list
				// this.OsmRelMbList.add(writeRelationMember(r));
				// idRelPrev = r.getLong("idrel");
				// System.out.println("idrel =" + idRelPrev);
				// System.out.println("OSMRelationMember added in
				// OsmRelMbList.");

				this.OsmRelMbList.put(writeRelationMember(r), r.getLong("idrel"));

			}
		}
	}

	public OsmRelationMember writeRelationMember(ResultSet r) throws Exception {
		boolean isNode = r.getString("typeMb").toLowerCase().equalsIgnoreCase("node");
		boolean isWay = r.getString("typeMb").toLowerCase().equalsIgnoreCase("way");
		boolean isRelation = r.getString("typeMb").toLowerCase().equalsIgnoreCase("relation");
		long idmb = r.getLong("idmb");
		if (r.getString("rolemb").toLowerCase().equalsIgnoreCase("outer")) {
			OsmRelationMember osmRelMb = new OsmRelationMember(RoleMembre.OUTER, isNode, isWay, isRelation, idmb);
			System.out.println("writeRelationMember(r)");
			return osmRelMb;

		} else if (r.getString("rolemb").toLowerCase().equalsIgnoreCase("inner")) {
			OsmRelationMember osmRelMb = new OsmRelationMember(RoleMembre.INNER, isNode, isWay, isRelation, idmb);
			System.out.println("writeRelationMember(r)");
			return osmRelMb;

		} else {
			OsmRelationMember osmRelMb = new OsmRelationMember(RoleMembre.NON_DEF, isNode, isWay, isRelation, idmb);
			System.out.println("writeRelationMember(r)");
			return osmRelMb;
		}
	}

	public void writeRelation(ResultSet r) throws Exception {
		// Get date
		DateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");
		Date date = null;
		try {
			date = formatDate.parse(r.getString("datemodif"));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// Crée une liste de membres de relation: parcourt OsmRelMbList
		ArrayList<OsmRelationMember> mbList = new ArrayList<OsmRelationMember>();
		for (OsmRelationMember mb : OsmRelMbList.keySet()) {
			if (OsmRelMbList.get(mb) == (r.getLong("id") + r.getLong("vrel")))
				mbList.add(mb);
		}
		// Récupère le type de relation (MULTIPOLYGON ou NON_DEF)
		TypeRelation relType = getRelationType(r);
		PrimitiveGeomOSM myRelation = new OSMRelation(relType, mbList);

		// Creates a new OSMResource
		OSMResource myOsmResource = new OSMResource(r.getString("username"), myRelation, r.getLong("id"),
				r.getInt("changeset"), r.getInt("vrel"), r.getInt("uid"), date);
		// Visible : peut être null
		myOsmResource.setVisible(r.getBoolean("visible"));
		// Add tags if exist
		if (!r.getString("hstore_to_json").toString().equalsIgnoreCase("{}")) {
			try {
				JSONObject obj = new JSONObject(r.getString("hstore_to_json"));
				for (int i = 0; i < obj.names().length(); i++) {
					String key = obj.names().getString(i);
					String value = obj.getString(key);
					System.out.println(" Ajout du tag {" + key + ", " + value + "}");
					myOsmResource.addTag(key, value);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Java object created ! " + "\nid = " + myOsmResource.getId() + "\nusername = "
				+ myOsmResource.getContributeur() + "     uid = " + myOsmResource.getUid() + "\nvrel = "
				+ myOsmResource.getVersion() + "\ndate = " + myOsmResource.getDate() + "   Changeset = "
				+ myOsmResource.getChangeSet());
		this.myJavaRelations.add(myOsmResource);

		System.out.println("-------------------------------------------");

	}

	public void writeWay(ResultSet r) throws SQLException {
		System.out.println("Writing way...");
		Long[] nodeWay = (Long[]) r.getArray("composedof").getArray();
		for (long i : nodeWay) {
			System.out.println("nodeWay[i] = " + i);
		}
		List<Long> composedof = Arrays.asList(nodeWay);

		DateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");
		Date date = null;
		try {
			date = formatDate.parse(r.getString("datemodif"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		OSMResource myOsmResource = new OSMResource(r.getString("username"), new OSMWay(composedof), r.getLong("id"),
				r.getInt("changeset"), r.getInt("vway"), r.getInt("uid"), date);
		// Visible : peut être null
		myOsmResource.setVisible(r.getBoolean("visible"));
		// Add tags if exist
		if (!r.getString("hstore_to_json").toString().equalsIgnoreCase("{}")) {
			try {
				JSONObject obj = new JSONObject(r.getString("hstore_to_json"));
				for (int i = 0; i < obj.names().length(); i++) {
					String key = obj.names().getString(i);
					String value = obj.getString(key);
					System.out.println(" Ajout du tag {" + key + ", " + value + "}");
					myOsmResource.addTag(key, value);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.myJavaObjects.add(myOsmResource);
		System.out.println("Java object created ! " + "\nid = " + myOsmResource.getId() + "\nusername = "
				+ myOsmResource.getContributeur() + "     uid = " + myOsmResource.getUid() + "\nvway = "
				+ myOsmResource.getVersion() + "\ndate = " + myOsmResource.getDate() + "   Changeset = "
				+ myOsmResource.getChangeSet());
		System.out.println("-------------------------------------------");
	}

}