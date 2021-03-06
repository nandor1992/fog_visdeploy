package org.nandor.fog_deployer;

import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Exporter {
		
	public static Fog readJsonFog(JSONObject json){			
		Fog f = new Fog((String)json.get("Name"));
		JSONObject gw = (JSONObject) json.get("Gateways");
		for (Object i: gw.keySet()){
			//System.out.println(gw.get(i));
			JSONObject g1 = (JSONObject) gw.get(i);
			f.addGateway(Integer.valueOf((String)i), (String)g1.get("Name"),(String)g1.get("Type"),((Double)g1.get("LIdle")).floatValue(), ((Double)g1.get("PerfCoef")).floatValue(),((Double)g1.get("PerfCoef")).floatValue());
			f.getGateways().get(Integer.valueOf((String)i)).setCapabilities((JSONArray) g1.get("Capabilities"));
			JSONObject res = (JSONObject) g1.get("Resources");
			for (Object r:res.keySet()){
				//System.out.println(res.get(r));
				JSONObject r1 = (JSONObject) res.get(r);
				f.addResource(Integer.valueOf((String)r),(String)r1.get("Name"),(String)r1.get("Type"));
				f.addGwResConn(Integer.valueOf((String)i), Integer.valueOf((String)r));
			}
			JSONObject apps = (JSONObject) g1.get("Apps");
			for (Object a:apps.keySet()){
				//System.out.println(apps.get(a));
				JSONObject a1 = (JSONObject) apps.get(a);
				f.addApp(Integer.valueOf((String)a),(String)a1.get("Name"),(String)a1.get("Type"),((Double)a1.get("UnitLoad")).floatValue());
				JSONObject constr = (JSONObject) a1.get("UWeights");
				f.getApps().get(Integer.valueOf((String)a)).setUtilityWeights(((Double)constr.get("Delay")).floatValue(), ((Double)constr.get("Reliability")).floatValue(), ((Double)constr.get("Constraints")).floatValue());
				constr = (JSONObject) a1.get("Constraints");
				f.getApps().get(Integer.valueOf((String)a)).setConstraints(((Double)constr.get("Delay")).floatValue(), ((Double)constr.get("Reliability")).floatValue());
				f.getApps().get(Integer.valueOf((String)a)).setRequirements((JSONArray) a1.get("Requirements"));
				f.addGwAppConn(Integer.valueOf((String)i), Integer.valueOf((String)a));
			}
		}
		JSONObject cls = (JSONObject) json.get("Clusters");
		for (Object c : cls.keySet()) {
			JSONObject c1 = (JSONObject) cls.get(c);
			f.addCluster(Integer.valueOf((String)c),(String)c1.get("Name"));
			JSONObject gws = (JSONObject) c1.get("Gateways");
			for (Object g : gws.keySet()) {
				//System.out.println(gws.get(g));
				JSONObject g1 = (JSONObject) gws.get(g);
				f.addClustGwConn(Integer.valueOf((String)c), Integer.valueOf((String)g), ((Double)g1.get("Share")).floatValue(), ((Double)g1.get("Load")).floatValue());
			}
			JSONObject apps = (JSONObject) c1.get("Apps");
			for (Object a : apps.keySet()) {
				//System.out.println(f.getApps().keySet());
				//System.out.println(f.getApps().get(Integer.valueOf((String)a)).getId());
				f.addClustAppConn(Integer.valueOf((String)c), Integer.valueOf((String)a));
			}
		}
		JSONObject conn = (JSONObject) json.get("Connections");
		for (Object c : conn.keySet()) {
			JSONObject c1 = (JSONObject) conn.get(c);
			JSONObject r1 = (JSONObject) c1.get("Resources");
			for (Object r : r1.keySet()) {
				f.addAppResConn(Integer.valueOf((String)c), Integer.valueOf((String)r), ((Double)r1.get(r)).floatValue());
			}
			JSONObject a1 = (JSONObject) c1.get("Apps");
			for (Object a : a1.keySet()) {
				f.addAppAppConnection(Integer.valueOf((String)c), Integer.valueOf((String)a), ((Double)a1.get(a)).floatValue());
			}
		}
		JSONObject lat = (JSONObject) json.get("Latencies");
		for (Object g : lat.keySet()) {
			JSONObject g1 = (JSONObject) lat.get(g);
			for (Object g2:g1.keySet()){
				f.addGwGwConn(f.getGateways().get(Integer.valueOf((String)g)),f.getGateways().get(Integer.valueOf((String)g2)), ((Double)g1.get(g2)).floatValue());
			}
		}
		return f;
	}

	public static JSONObject writeJsonFog(Fog f){
		JSONObject json = new JSONObject();
		//Gateways
		JSONObject gateways = new JSONObject();
		JSONObject resources = new JSONObject();
		JSONObject applications = new JSONObject();
		for (Integer gId:f.getGateways().keySet()){
			Gateway gw = f.getGateways().get(gId);
			JSONObject g = new JSONObject();
			g.put("Name", gw.getName());
			g.put("Type", gw.getType());
			g.put("PerfCoef", gw.getPjCap());
			g.put("LIdle", gw.getLidle());
			g.put("TotLoad", gw.getGwLoad());
			g.put("BaseLoad", gw.getGwBaseLoad());
			g.put("TotMsgRate", gw.getTotMsgRate());
			JSONArray caps = new JSONArray();
			for (String cap: gw.getCapabilities()){
				caps.add(cap);
			}
			g.put("Capabilities", caps);
			
			//Resources
			JSONObject r = new JSONObject();
			for (Integer rId:gw.getResources().keySet()){
				JSONObject res = new JSONObject();
				res.put("Name", gw.getResources().get(rId).getName());
				res.put("Type", gw.getResources().get(rId).getType());
				res.put("MsgRate", gw.getResources().get(rId).getTotMsgs());
				res.put("GatewayID",gw.getId());
				res.put("GatewayName",gw.getName());
				r.put(rId, res);
				resources.put(rId,res);
			}
			g.put("Resources",r);
			//Apps
			JSONObject a = new JSONObject();
			for (Integer aId:gw.getApps().keySet()){
				JSONObject app = new JSONObject();
				App a1 = gw.getApps().get(aId);
				app.put("Name", a1.getName());
				app.put("Type", a1.getType());
				app.put("Load", a1.getAppLoad());
				app.put("Utility", a1.getAppUtility());
				app.put("Delays", a1.getTotDelay());
				app.put("Reliability", a1.getAppReliability());
				app.put("ConstViolations", a1.getConstraintViolations());
				app.put("UnitLoad", a1.getUnitLoad());
				app.put("TotalMsgCount",a1.getTotalMsgRate());
				app.put("Type", a1.getType());
				JSONArray reqs = new JSONArray();
				for (String req: a1.getRequirements()){
					reqs.add(req);
				}
				app.put("Requirements", reqs);
				JSONObject weights = new JSONObject();
				weights.put("Delay", a1.getUtilityWeights().get("delay"));
				weights.put("Reliability", a1.getUtilityWeights().get("reliability"));
				weights.put("Constraints", a1.getUtilityWeights().get("constraint"));
				app.put("UWeights",weights);
				JSONObject constr = new JSONObject();
				constr.put("Delay", a1.getConstraints().get("delay"));
				constr.put("Reliability", a1.getConstraints().get("reliability"));
				app.put("Constraints",constr);
				a.put(aId,app);
				applications.put(aId, app);
			}
			g.put("Apps",a);
			//Write to Main
			gateways.put(gId, g);
		}
		json.put("Gateways", gateways);
		json.put("Resources", resources);
		json.put("Applications", applications);
		//Latencies
		JSONObject lats = new JSONObject();
		for (Integer gId:f.getConnections().keySet()){
			JSONObject g = new JSONObject();
			for(Integer g2Id:f.getConnections().get(gId).keySet()){
				g.put(g2Id, f.getConnections().get(gId).get(g2Id));
			}
			lats.put(gId, g);
		}
		json.put("Latencies", lats);
		//Connections
		JSONObject conn = new JSONObject();
		for (Integer aId:f.getApps().keySet()){
			JSONObject c = new JSONObject();
			JSONObject res = new JSONObject();
			for(Integer rId:f.getApps().get(aId).getResources().keySet()){
				res.put(rId,f.getApps().get(aId).getResources().get(rId).getTotMsgs());
			}
			c.put("Resources", res);
			JSONObject apps = new JSONObject();
			for(Integer a2Id:f.getApps().get(aId).getApps().keySet()){
				apps.put(a2Id,f.getApps().get(aId).getAmessages().get(a2Id));
			}
			c.put("Apps", apps);
			conn.put(aId, c);
		}
		json.put("Connections", conn);			
		//Clusters
		JSONObject cls = new JSONObject();
		for (Integer cId:f.getClusters().keySet()){
			JSONObject c = new JSONObject();
			Cluster clus = f.getClusters().get(cId);
			//Cluster Info
			c.put("Name",clus.getName());
			c.put("Delay",clus.getClusterCompoundDelay());
			c.put("Reliability",clus.getClusterCompoundReliability());
			c.put("Utility",clus.getClusterCompoundUtility());
			c.put("ConstViolations",clus.getClusterConstViolations());
			//Connected Gateways and their Share
			JSONObject gws = new JSONObject();
			for(Integer gId:clus.getGateways().keySet()){
				JSONObject gw = new JSONObject();
				gw.put("Share",clus.getGatewayShare().get(gId));
				gw.put("Load",clus.getShareLoad().get(gId));
				gw.put("Name",f.getGateways().get(gId).getName());
				gws.put(gId, gw);
			}
			c.put("Gateways",gws);
			//Connected Apps
			JSONObject apps = new JSONObject();
			for(Integer aId:clus.getApps().keySet()){
				apps.put(aId, f.getApps().get(aId).getName());
			}
			c.put("Apps",apps);
			//Final Put
			cls.put(cId, c);
		}
		json.put("Clusters", cls);
		// Addign Nodes to System for Display
		JSONArray nodes = new JSONArray();
		JSONArray edges = new JSONArray();
		//Gateways
		for (Integer g: f.getGateways().keySet()){
			JSONObject g1 = new JSONObject();
			g1.put("group", "server");
			g1.put("id", f.getGateways().get(g).getId()+60000);
			g1.put("label", f.getGateways().get(g).getName());
			g1.put("value",(int)(f.getGateways().get(g).getPjCap()*50));
			nodes.add(g1);
		}
		//Apps
		String[] colours = {"Brown","Blue","Chartreuse","Crimson","DarkBlue","DarkOrange","DarkOrchid","DarkRed","Purple","RebeccaPurple","Red","Tomato","Turquoise","Violet","AntiqueWhite","Aqua","Aquamarine","Bisque","BlanchedAlmond","BlueViolet","BurlyWood","CadetBlue","Chocolate","Coral","CornflowerBlue","Cornsilk","Cyan","DarkCyan","DarkGoldenRod","DarkGray","DarkGrey","DarkGreen","DarkKhaki","DarkMagenta","DarkOliveGreen","DarkSalmon","DarkSeaGreen","DarkSlateBlue","DarkSlateGray","DarkSlateGrey","DarkTurquoise","DarkViolet","DeepPink","DeepSkyBlue","DimGray","DimGrey","DodgerBlue","FireBrick","FloralWhite","ForestGreen","Fuchsia","Gainsboro","Gold","GoldenRod","Gray","Grey","Green","GreenYellow","HotPink","IndianRed","Indigo","Khaki","Lavender","LawnGreen","LightBlue","LightCoral","LightCyan","LightGoldenRodYellow","LightGray","LightGrey","LightGreen","LightPink","LightSalmon","LightSeaGreen","LightSkyBlue","LightSlateGray","LightSlateGrey","LightSteelBlue","Lime","LimeGreen","Magenta","Maroon","MediumAquaMarine","MediumBlue","MediumOrchid","MediumPurple","MediumSeaGreen","MediumSlateBlue","MediumSpringGreen","MediumTurquoise","MediumVioletRed","MidnightBlue","MistyRose","Moccasin","NavajoWhite","Navy","Olive","OliveDrab","Orange","OrangeRed","Orchid","PaleGoldenRod","PaleGreen","PaleTurquoise","PaleVioletRed","PeachPuff","Peru","Pink","Plum","PowderBlue","RosyBrown","RoyalBlue","SaddleBrown","Salmon","SandyBrown","SeaGreen","Sienna","Silver","SkyBlue","SlateBlue","SlateGray","SlateGrey","SpringGreen","SteelBlue","Tan","Teal","Thistle","Wheat","Yellow","YellowGreen"};//Black Is dummy
		for (Integer c : f.getClusters().keySet()) {
			for (Integer a : f.getClusters().get(c).getApps().keySet()) {
				JSONObject a1 = new JSONObject();
				JSONObject col = new JSONObject();
				col.put("border", "black");
				col.put("background", colours[c]);
				a1.put("color", col);
				a1.put("shape", "box");
				a1.put("value", f.getApps().get(a).getAppLoad().intValue());
				a1.put("label",f.getApps().get(a).getName());
				a1.put("id",f.getApps().get(a).getId());
				nodes.add(a1);
				//Add Gw Conn for App
				JSONObject eGw = new JSONObject();
				JSONObject col3 = new JSONObject();
				col3.put("color", colours[c]);
				eGw.put("from", f.getApps().get(a).getId());
				eGw.put("color", col3);
				eGw.put("to", 60000+f.getApps().get(a).getGateway().getId());
				edges.add(eGw);
				//Resources
				for (Integer r: f.getApps().get(a).getResources().keySet()){
					JSONObject r1 = new JSONObject();
					JSONObject e1 = new JSONObject();
					JSONObject e2 = new JSONObject();
					JSONObject col2 = new JSONObject();
					col2.put("color", colours[c]);
					e1.put("from", f.getApps().get(a).getId());
					e1.put("color", col2);
					e2.put("from",60000+f.getResources().get(r).getGateway().getId());
					e2.put("color", col2);
					switch (f.getResources().get(r).getType()) {
					case "Device": 
						r1.put("shape", "image");
						r1.put("image","computer-microprocessor.png");
						r1.put("label", f.getResources().get(r).getName());
						r1.put("value", 50);
						r1.put("id",10000+f.getResources().get(r).getId());
						e1.put("to", 10000+f.getResources().get(r).getId());
						e1.put("id", f.getApps().get(a).getId()+"-"+10000+f.getResources().get(r).getId());
						e2.put("to", 10000+f.getResources().get(r).getId());
						e2.put("id", 60000+f.getResources().get(r).getGateway().getId()+"-"+10000+f.getResources().get(r).getId());
						
						break;
					case "Cloud":
						r1.put("group", "cloud");
						r1.put("label", f.getResources().get(r).getName());
						r1.put("id",20000+f.getResources().get(r).getId());
						r1.put("value", 50);
						e1.put("to", 20000+f.getResources().get(r).getId());
						e1.put("id", f.getApps().get(a).getId()+"-"+20000+f.getResources().get(r).getId());
						e2.put("to", 20000+f.getResources().get(r).getId());
						e2.put("id", 60000+f.getResources().get(r).getGateway().getId()+"-"+20000+f.getResources().get(r).getId());
						break;
					case "Storage":
						r1.put("group", "database");
						r1.put("label", f.getResources().get(r).getName());
						r1.put("id",40000+f.getResources().get(r).getId());
						r1.put("value", 50);
						e1.put("to", 40000+f.getResources().get(r).getId());
						e1.put("id", f.getApps().get(a).getId()+"-"+40000+f.getResources().get(r).getId());
						e2.put("to", 40000+f.getResources().get(r).getId());
						e2.put("id", 60000+f.getResources().get(r).getGateway().getId()+"-"+40000+f.getResources().get(r).getId());
						break;
					case "LocalAccess":
						r1.put("group", "region");
						r1.put("label", f.getResources().get(r).getName());
						r1.put("id",30000+f.getResources().get(r).getId());
						r1.put("value", 50);
						e1.put("to", 30000+f.getResources().get(r).getId());
						e1.put("id", f.getApps().get(a).getId()+"-"+30000+f.getResources().get(r).getId());
						e2.put("to", 30000+f.getResources().get(r).getId());
						e2.put("id", 60000+f.getResources().get(r).getGateway().getId()+"-"+30000+f.getResources().get(r).getId());
						break;
					default:
						r1.put("label", f.getResources().get(r).getName());
						r1.put("id",50000+f.getResources().get(r).getId());
						r1.put("value", 50);
						e1.put("to", 50000+f.getResources().get(r).getId());
						e1.put("id", f.getApps().get(a).getId()+"-"+50000+f.getResources().get(r).getId());
						e2.put("to", 50000+f.getResources().get(r).getId());
						e2.put("id", 60000+f.getResources().get(r).getGateway().getId()+"-"+50000+f.getResources().get(r).getId());
						break;
					}
					nodes.add(r1);
					edges.add(e1);
					edges.add(e2);
				}
				//Other Apps Conns 
				for (Integer a2 : f.getApps().get(a).getApps().keySet()) {
					if (true) {
						JSONObject e1 = new JSONObject();
						JSONObject col2 = new JSONObject();
						if (f.getApps().get(a).getCluster().getId() == f.getApps().get(a2).getCluster().getId()) {
							col2.put("color", colours[c]);
						} else {
							col2.put("color", "black");
						}
						e1.put("from", a);
						e1.put("color", col2);
						e1.put("to", a2);
						e1.put("id", a + "-" + a2);
						edges.add(e1);
					}
				}
			}
		}
		//Append all Nodes
		json.put("nodes", nodes);
		//Append all Connections	
		json.put("edges", edges);
		//Extra info to be able to Show it
		json.put("type", "virtual");
		Date date = new Date();
		json.put("date", date.toString());
		json.put("Name","Test_java");
		//Put Parameters here
		JSONObject params = new JSONObject();
		params.put("Name", "Test_java");
		params.put("Info",f.toString());
		params.put("Utility", f.getFogCompoundUtility());
		params.put("Delay", f.getFogCompoundDelay());
		params.put("Reliability", f.getFogCompoundReliability());
		json.put("SystemData",params);
		return json;
	}
	
	public static String writeStringFog(Fog f){
		return writeJsonFog(f).toJSONString();
	}
	
	public static Fog readJsonFog(String jsonString){
		JSONParser parser = new JSONParser();
		JSONArray json2 = new JSONArray();
		try {
			json2 = (JSONArray) parser.parse(jsonString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		JSONObject json = (JSONObject)json2.get(0);
		return readJsonFog(json);
	}
}
