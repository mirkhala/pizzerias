package org.openshift.pizzerias.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.openshift.pizzerias.domain.Coordinates;
import org.openshift.pizzerias.domain.Pizzerias;
import org.openshift.pizzerias.mongo.DBConnection;

import com.mongodb.BasicDBObject;

@RequestScoped
@Path("/pizzerias")
public class PizzeriasResource {

	@Inject
	private DBConnection dbConnection;

	private Pizzerias populatePizzeriasInformation(Document dataValue) {
		Pizzerias newObject = new Pizzerias();
		newObject.setId((ObjectId) dataValue.get("_id"));
		newObject.setName(dataValue.get("name"));
		Coordinates cord = new Coordinates((List) dataValue.get("pos"));
		newObject.setPosition(cord);
		newObject.setLatitude(cord.getLatitude());
		newObject.setLongitude(cord.getLongitude());

		return newObject;
	}

	// get all the mlb Pizzerias
	@GET()
	@Produces("application/json")
	public List<Pizzerias> getAllPizzerias() {
		ArrayList<Pizzerias> allPizzeriasList = new ArrayList<Pizzerias>();
		System.out.println("Hi..");
		MongoCollection pizzerias = dbConnection.getCollection();
		MongoCursor<Document> cursor = pizzerias.find().iterator();
		try {
			while (cursor.hasNext()) {
				allPizzeriasList.add(this.populatePizzeriasInformation(cursor.next()));
			}
		} finally {
			cursor.close();
		}

		return allPizzeriasList;
	}

	@GET
	@Produces("application/json")
	@Path("within")
	public List<Pizzerias> findPizzeriasWithin(@QueryParam("lat1") float lat1,
                                       @QueryParam("lon1") float lon1, @QueryParam("lat2") float lat2,
                                       @QueryParam("lon2") float lon2) {

	    // The first thing we want to do is make sure the DB contains data
        dbConnection.checkDatabase();
		ArrayList<Pizzerias> allPizzeriasList = new ArrayList<Pizzerias>();
		MongoCollection pizzerias = dbConnection.getCollection();

		// make the query object
		BasicDBObject spatialQuery = new BasicDBObject();

		ArrayList<double[]> boxList = new ArrayList<double[]>();
		boxList.add(new double[] { new Float(lat2), new Float(lon2) });
		boxList.add(new double[] { new Float(lat1), new Float(lon1) });

		BasicDBObject boxQuery = new BasicDBObject();
		boxQuery.put("$box", boxList);

		spatialQuery.put("pos", new BasicDBObject("$within", boxQuery));
		System.out.println("Using spatial query: " + spatialQuery.toString());

		MongoCursor<Document> cursor = pizzerias.find(spatialQuery).iterator();
		try {
			while (cursor.hasNext()) {
				allPizzeriasList.add(this.populatePizzeriasInformation(cursor.next()));
			}
		} finally {
			cursor.close();
		}
		System.out.println("Return " + allPizzeriasList.size() + " pizzerias");

		return allPizzeriasList;
	}
}
