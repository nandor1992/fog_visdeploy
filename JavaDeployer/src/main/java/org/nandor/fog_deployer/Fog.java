package org.nandor.fog_deployer;

import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class Fog {
	private String name;
	private Map<Integer, Resource> resources;
	private Map<Integer, App> apps;
	private Map<Integer, Gateway> gateways;
	private Map<Integer, Cluster> clusters;
	private Map<String, float[][]> structure;
	private Map<String, Float> appTypes;
	private Map<String, Float> clustParam;
	private Map<Integer, Map<Integer, Float>> connections;
	private int appToClusterAvg[] = { 3, 22 };
	private Random random = new Random();
	private float gwLoadAdj = (float) 0.0;
	private String ScenarioType = "";
	private List<String> capabilityList = new ArrayList<String>() {{
	    add("Capability A");
	    add("Capability B");
	    add("Capability C");
	    add("Capability D");
	    add("Capability E");
	    add("Capability F");
	    add("Capability G");
	}};
	private Map<Integer, Integer> deployment;

	// Initialization
	public Fog(String name) {
		resources = new HashMap<>();
		apps = new HashMap<>();
		gateways = new HashMap<>();
		clusters = new HashMap<>();
		structure = new HashMap<>();
		appTypes = new HashMap<>();
		clustParam = new HashMap<>();
		connections = new HashMap<>();
		deployment = new HashMap<>();
		this.name = name;
		this.initCluster(null);
		this.InitApps(null);
		this.InitStructure(null, null, null, null, null);
	}

	public void generateNewFogV2(int appCnt, float maxAppLoad, float gwLoad,float constRate,float constImprove, float[] gwLatency,float cloudGWRatio,
			float[] gwExtLatency, float[] gwCloudIntLat,int capRate, int reqRate,String scenarioType) {
		// c.saveToFile("testing.json", c.addCloud(int(k/5), 42,
		// c.createGwSet(k-int(k/5), 30, 80, 19.53, 22.34)
		System.out.println("----------------------------");
		System.out.println("-----Generating new Fog-----");
		System.out.println("----------------------------");
		System.out.println("Parameters - AppCount: " + appCnt + " Gw Count's Ext: " + cloudGWRatio + " Load: "
				+ gwLoad + " Latency: " + gwLatency[0] + "," + gwLatency[1] + "Ext Lat: " + gwExtLatency[0] + ","
				+ gwExtLatency[1]+ " Fog Type: "+scenarioType);
		// Init Clusters and their Apps
		// Apps Parameters
		Map<Integer, Map<String, Map<String, Map<String, Float>>>> appMsges = new HashMap<>();
		float fogLoad = (float) 0.0;
		Map<Integer, Float> clsLoad = new HashMap<>();
		int i2=0;
		int totAppCnt = 0;
		while(appCnt>totAppCnt) {
			i2++;
			int c = this.addCluster("Cluster_" + i2);
			int app_cnt = generateRandomClustSize();
			if (totAppCnt+app_cnt>=appCnt){
				app_cnt = appCnt-totAppCnt;
				totAppCnt = appCnt;
			}else{
				totAppCnt+=app_cnt;
			}
			float totLoad = (float) 0.0;
			for (int j = 0; j < app_cnt; j++) {
				int app_type = this.random.nextInt(10) + 1;
				int a = this.addApp("TestAppC" + i2 + "_Nr" + (j+1), "Testing_App" + app_type);
				addRandReq(a,reqRate);
				this.addClustAppConn(c, a);
				float aLoad = (float) 99.0;
				Map<String, Map<String, Map<String, Float>>> tmp = null;
				while (aLoad > maxAppLoad) {
					tmp = this.getAppRandMsges(this.getApps().get(a).getUnitLoad());
					aLoad = this.apps.get(a).getEstAppResLoad((float) 1.0,
							tmp.get("Tot").get("Tot").get("Res"),
							tmp.get("Tot").get("Tot").get("App"));
				}
				appMsges.put(a,tmp); 
				totLoad += aLoad;
				//System.out.println("Added App to Cluster: "+i+"App: "+a+"Type: "+app_type+" MessageRates:"+appMsges.get(a).get("Tot").get("Tot").get("Res")+","+appMsges.get(a).get("Tot").get("Tot").get("App")+"Load:"+aLoad);
				//System.out.println();
			}
			clsLoad.put(c, totLoad);
			//System.out.println("Created Cluster Id: " + c + " AppCnt:" + app_cnt + " TotLoad:" + totLoad);
			fogLoad += totLoad;
		}
		// Total Gateway Count
		List<Float> perfC = new ArrayList<>();
		Float totPerfC = (float) 0.0;
		for (int i = 0; i < new Double(fogLoad / 1.1 / 100 * ((float) 100.0 / gwLoad)).intValue() + 1; i++) {
			float perf = random.nextFloat() * (float) (1.4 - 1.0) + (float)1.0;
			perfC.add(perf);
			totPerfC += perf;
		}
		// Calculate Diffrence
		float totPerfC2 = (float) 0.0;
		float diff = (fogLoad / (float) 100 * ((float) 100.0 / gwLoad) / -totPerfC) / (float) perfC.size();
		for (int i = 0; i < perfC.size(); i++) {
			perfC.set(i, perfC.get(i) + diff);
			totPerfC2 += perfC.get(i) + diff;
		}
		/*System.out.println("Tot Res Required Count: " + fogLoad + "  Gw Count: "
				+ (new Double(fogLoad / 1.1 / 100 * ((float) 100.0 / gwLoad)).intValue() + 1) + " ResProvided: "
				+ totPerfC2*100.0 + " App Cnt: " + this.getApps().size());*/

		// Create Actuall gateways with Capabilities
		for (int i = 0; i < perfC.size(); i++) {
			int g1 = this.addGateway("TestGw" + i, "Basic");
			this.gateways.get(g1).setPjCap(perfC.get(i));
			this.gateways.get(g1).setPjSpeed(perfC.get(i));
			addRandCap(g1,capRate);
			// System.out.println("Gw
			// ProcCoef:"+this.gateways.get(g1).getPerfCoef());
		}

		// Randomly Distribute Gateways to Clusters based on Requirements Of
		// courese....
		this.basicDistributeGw2Cluster(clsLoad);

		this.basicAppToGwDistribution();

		/// Make sure to Delete Return Later
		// if (true){return;}
		// Can't yet run this
		// System.out.println("Create Resources and Connection for Apps Based on
		/// Conns");
		for (Integer i : this.getApps().keySet()) {
			this.addAppConns(i, appMsges.get(i));
		}
		// System.out.println("Adding Gateway to Gateway Connections");
		this.addGwConns(gwLatency[0], gwLatency[1]);
		// Get Already Connected Gw's
		// System.out.println("Adding External Gateways and Connns");
		Set<Integer> currGws = this.getGateways().keySet();
		List<Integer> newGws = new ArrayList<>();
		for (int i = 1; i <= currGws.size()*cloudGWRatio; i++) {
			int g1 = this.addGateway("ExtGw" + i, "External");
			newGws.add(g1);
			this.gateways.get(g1).setPjCap(random.nextFloat() * (float) (0.7) + (float) 1.8);
			this.gateways.get(g1).setPjSpeed(random.nextFloat() * (float) (1.5) + (float) 2.8);
			addRandCap(g1,capRate);
		}
		// System.out.println(this.getGateways().keySet());
		//this.addRandomConstraints(constRate,constImprove);
		switch (scenarioType) {
		case "Delay":
			//Delay Improvement Scenario
			this.setConstraints(Float.MAX_VALUE, Float.MIN_VALUE);
			this.setWeights((float)1.0,(float)0.0);
			break;
		case "Multi":
			this.addRandomConstraints(constRate,constImprove);
			this.addRandomWeights();
			break;
		case "Capab":
			this.addRandomConstraints(constRate,constImprove);
			this.addRandomWeights();
			break;
		default:
			break;
		}
		//All Types Here
		//this.addRandomConstraints(constRate,constImprove);
		//this.setConstraints(Float.MAX_VALUE, Float.MIN_VALUE);
		//this.setWeights((float)1.0,(float)0.0,(float)0.0);
		//this.addRandomWeights();
		this.addExtraGwConns(newGws, currGws,gwExtLatency,gwCloudIntLat);
		// System.out.println("Clearing Connections for Empty Intit Phase");
		System.out.println("Tot Res Required Count: " + this.getTotalLoad() + "  Gw Count: "
				+ this.getGateways().size()+ " ResProvided: "
				+ this.getTotRes()+ " at Rate: "+this.getShareRate()+" App Cnt: " + this.getApps().size()+" Utility: "+this.getFogCompoundUtility());
	}
	
	public void generateNewFog(int clustCnt, float maxAppLoad, float gwLoad,float constRate,float constImprove, float[] gwLatency,int extGwCnt,
			float[] gwExtLatency, float[] gwCloudIntLat,int capRate, int reqRate) {
		// c.saveToFile("testing.json", c.addCloud(int(k/5), 42,
		// c.createGwSet(k-int(k/5), 30, 80, 19.53, 22.34)
		System.out.println("-----Generating new Fog-----");
		System.out.println("Parameters - ClusterCount: " + clustCnt + " Gw Count's Ext: " + extGwCnt + " Load: "
				+ gwLoad + " Latency: " + gwLatency[0] + "," + gwLatency[1] + "Ext Lat: " + gwExtLatency[0] + ","
				+ gwExtLatency[1]);
		// Init Clusters and their Apps
		// Apps Parameters
		Map<Integer, Map<String, Map<String, Map<String, Float>>>> appMsges = new HashMap<>();
		float fogLoad = (float) 0.0;
		Map<Integer, Float> clsLoad = new HashMap<>();
		for (int i = 1; i <= clustCnt; i++) {
			int c = this.addCluster("Cluster_" + i);
			int app_cnt = generateRandomClustSize();
			float totLoad = (float) 0.0;
			for (int j = 0; j < app_cnt; j++) {
				int app_type = this.random.nextInt(10) + 1;
				int a = this.addApp("TestAppC" + i + "_Nr" + (j+1), "Testing_App" + app_type);
				addRandReq(a,reqRate);
				this.addClustAppConn(c, a);
				float aLoad = (float) 99.0;
				Map<String, Map<String, Map<String, Float>>> tmp = null;
				while (aLoad > maxAppLoad) {
					tmp = this.getAppRandMsges(this.getApps().get(a).getUnitLoad());
					aLoad = this.apps.get(a).getEstAppResLoad((float) 1.0,
							tmp.get("Tot").get("Tot").get("Res"),
							tmp.get("Tot").get("Tot").get("App"));
				}
				appMsges.put(a,tmp); 
				totLoad += aLoad;
				//System.out.println("Added App to Cluster: "+i+"App: "+a+"Type: "+app_type+" MessageRates:"+appMsges.get(a).get("Tot").get("Tot").get("Res")+","+appMsges.get(a).get("Tot").get("Tot").get("App")+"Load:"+aLoad);
				//System.out.println();
			}
			clsLoad.put(c, totLoad);
			//System.out.println("Created Cluster Id: " + c + " AppCnt:" + app_cnt + " TotLoad:" + totLoad);
			fogLoad += totLoad;
		}
		// Total Gateway Count
		List<Float> perfC = new ArrayList<>();
		Float totPerfC = (float) 0.0;
		for (int i = 0; i < new Double(fogLoad / 1.1 / 100 * ((float) 100.0 / gwLoad)).intValue() + 1; i++) {
			float perf = random.nextFloat() * (float) (1.4 - 1.0) + (float)1.0;
			perfC.add(perf);
			totPerfC += perf;
		}
		// Calculate Diffrence
		float totPerfC2 = (float) 0.0;
		float diff = (fogLoad / (float) 100 * ((float) 100.0 / gwLoad) / -totPerfC) / (float) perfC.size();
		for (int i = 0; i < perfC.size(); i++) {
			perfC.set(i, perfC.get(i) + diff);
			totPerfC2 += perfC.get(i) + diff;
		}
		/*System.out.println("Tot Res Required Count: " + fogLoad + "  Gw Count: "
				+ (new Double(fogLoad / 1.1 / 100 * ((float) 100.0 / gwLoad)).intValue() + 1) + " ResProvided: "
				+ totPerfC2*100.0 + " App Cnt: " + this.getApps().size());*/

		// Create Actuall gateways with Capabilities
		for (int i = 0; i < perfC.size(); i++) {
			int g1 = this.addGateway("TestGw" + i, "Basic");
			this.gateways.get(g1).setPjCap(perfC.get(i));
			addRandCap(g1,capRate);
			// System.out.println("Gw
			// ProcCoef:"+this.gateways.get(g1).getPerfCoef());
		}

		// Randomly Distribute Gateways to Clusters based on Requirements Of
		// courese....
		this.basicDistributeGw2Cluster(clsLoad);

		this.basicAppToGwDistribution();

		/// Make sure to Delete Return Later
		// if (true){return;}
		// Can't yet run this
		// System.out.println("Create Resources and Connection for Apps Based on
		/// Conns");
		for (Integer i : this.getApps().keySet()) {
			this.addAppConns(i, appMsges.get(i));
		}
		// System.out.println("Adding Gateway to Gateway Connections");
		this.addGwConns(gwLatency[0], gwLatency[1]);
		// Get Already Connected Gw's
		// System.out.println("Adding External Gateways and Connns");
		Set<Integer> currGws = this.getGateways().keySet();
		List<Integer> newGws = new ArrayList<>();
		for (int i = 1; i <= extGwCnt; i++) {
			int g1 = this.addGateway("ExtGw" + i, "External");
			newGws.add(g1);
			this.gateways.get(g1).setPjCap(random.nextFloat() * (float) (1.6 - 1.1) + (float) 1.2);
			addRandCap(g1,capRate);
		}
		// System.out.println(this.getGateways().keySet());
		this.addRandomConstraints(constRate,constImprove);
		//this.addRandomWeights();
		this.addExtraGwConns(newGws, currGws,gwExtLatency,gwCloudIntLat);
		// System.out.println("Clearing Connections for Empty Intit Phase");
		System.out.println("Tot Res Required Count: " + this.getTotalLoad() + "  Gw Count: "
				+ this.getGateways().size()+ " ResProvided: "
				+ this.getTotRes()+ " at Rate: "+this.getShareRate()+" App Cnt: " + this.getApps().size()+" Utility: "+this.getFogCompoundUtility());
	}

	
	private void addRandReq(Integer a,int reqRate){
		Collections.shuffle(this.capabilityList);
		for (Integer i=0; i<reqRate;i++){
			this.getApps().get(a).addRequirements(this.capabilityList.get(i));
		}
	} 
	
	private void addRandCap(Integer g, int capRate){
		Collections.shuffle(this.capabilityList);
		for (Integer i=0; i<capRate;i++){
			this.getGateways().get(g).addCapabilities(this.capabilityList.get(i));
		}
	}
	
	private void addRandomWeights() {
		float[] vals = {(float)0.0,(float)0.33,(float)0.66,(float)1.0};
		float[] vals2 = {(float)0.33,(float)0.66,(float)1.0};
		for (Integer a:this.getApps().keySet())
		{
				float delay = vals[new Random().nextInt(vals.length)];
				float reliability = vals[new Random().nextInt(vals.length)];
				if (delay == 0.0){
					reliability = vals2[new Random().nextInt(vals2.length)];
				}
				this.getApps().get(a).setUtilityWeights(delay,reliability);
		}
		
	}
	
	private void setWeights(float delay, float reliability) {
		for (Integer a:this.getApps().keySet())
		{
				this.getApps().get(a).setUtilityWeights(delay,reliability);
		}
		
	}

	private void addRandomConstraints(float constRate,float constImprove) {
		for (Integer a:this.getApps().keySet())
		{
			if (random.nextFloat()<constRate){
				this.getApps().get(a).setConstraints(this.getApps().get(a).getTotDelay()*(float)(1.0-constImprove),this.getApps().get(a).getAppReliability()*(float)(1.0+constImprove));
				this.getApps().get(a).setWeightConstraint((float)1.0);
			}else{
				this.getApps().get(a).setWeightConstraint((float)0.0);
			}
		}
		
	}
	
	private void setConstraints(float delay,float reliability){
		for (Integer a:this.getApps().keySet())
		{
			this.getApps().get(a).setConstraints(delay, reliability);
		}
	}

	public int generateRandomClustSize() {
		int app_cnt = 0;
		while (app_cnt < 3) {
			app_cnt = (int) ((this.random.nextGaussian() + 3) / 4.5 * (22 - 3) - 7);
		}
		return app_cnt;
	}

	// Deploy Everything
	public void setDeplpyment(Map<Integer, Integer> deployment) {
		this.deployment = deployment;
	}

	public void deployFog() {
		clearAppToGws();
		AssignAppsToGws(this.deployment);
	}

	// Deploy CLusters
	public void deployClusters() {
		clearAppToGws();
		for (Integer c : getClusters().keySet()) {
			AssignAppsToGws(getClusters().get(c).getDeployment());
		}
	}
	
	public Map<Integer,Integer> getDeployment(){
		Map<Integer,Integer> pop = new HashMap();
			for (Integer a:this.getApps().keySet()){
				pop.put(a, this.getApps().get(a).getGateway().getId());
			}
		return pop;
	}
	
	// Print Out
	public String toString() {
		return "Name: " + name + "; " + "Clusters:" + clusters.keySet() + "; " + "Gateways:" + gateways.keySet() + "; "
				+ "Apps:" + apps.keySet() + "; " + "Resources:" + resources.keySet();
	}
	
	public Float cumClustShare() {
		Float tot = (float)0.0;
		for (Integer c: this.getClusters().keySet()){
			tot+=this.getClusters().get(c).ShareRate();
		}
		return tot/this.getClusters().size();
	}
	
	//Share Rate 
	public Float getShareRate() {
		return this.getTotRes()/this.getTotLoad();
	}
	
	public Float getTotRes() {
		Float tot = (float)0.0;
		for (Integer g: getGateways().keySet()){
			tot+=((float)100.0-getGateways().get(g).getGwBaseLoad())*getGateways().get(g).getPjCap();
		}
		return tot;
	}
	public Float getTotLoad(){
		Float totL=(float)0.0;
		for( Integer a : apps.keySet()){
			totL+=apps.get(a).getAppLoad((float)1.0);			
		}
		return totL;
	}
	
	// Get App Info
	public Float getFogCompoundUtility() {
		float util = (float) 0.0;
		if (checkIfAppsAllocated().size()==0)
		{
			for (Integer a : getApps().keySet()) {
				if (getApps().get(a).getAppUtility().isNaN()){
					System.out.println("App Utility Nan: "+a);
					System.out.print("Components: ");
					System.out.print("Reliablity:"+getApps().get(a).getAppReliability());
					//System.out.println("util-rel: "+utilityWeights.get("reliability")*this.getAppReliability());
					System.out.print("Const Viol: "+getApps().get(a).getConstraintViolations());
					//Get Reference Delay
					//System.out.println("util-constr: "+utilityWeights.get("constraint")*(1.0-this.getConstraintViolations()));
					System.out.print("Proc Del:"+getApps().get(a).getProcDelay((float)5.4, (float)1));
					System.out.print("Tot del: "+getApps().get(a).getTotDelay());
					System.out.print("Tot proc: "+getApps().get(a).getProcDelay());
					System.out.print("Tot Netw: "+getApps().get(a).getNetworkDelay());
					System.out.print("ResSize: "+getApps().get(a).getResources().size());
					System.out.print("AppSize: "+getApps().get(a).getApps().size());
					System.out.println("Tot Netw: "+getApps().get(a).getNetworkDelay());
					
				}
				util += getApps().get(a).getAppUtility();
			}
		}
		return util;
	}


	public Float getPartialFogUtility() {
		float util = (float) 0.0;
		for (Integer a : getApps().keySet()) {
			if (getApps().get(a).getGateway()!=null){
				if (!getApps().get(a).getAppUtility().isNaN()){
					util += getApps().get(a).getAppUtility();
				}
			}
		}
		return util;
	}

	public Float getFogCompoundDelay() {
		float del = (float) 0.0;
		for (Integer a : getApps().keySet()) {
			del += getApps().get(a).getTotDelay();
		}
		return del;
	}

	public Float getFogCompoundReliability() {
		float rel = (float) 0.0;
		for (Integer a : getApps().keySet()) {
			rel += getApps().get(a).getAppReliability();
		}
		return rel;
	}

	public int verifyIndValidity() {
		int viol = 0;
		//System.out.println("Testing");
		for (Integer g: this.getGateways().keySet()) {
			//System.out.println("GW "+g.toString()+" Load: "+this.getGateways().get(g).getGwLoad());
			if (this.getGateways().get(g).getGwLoad() > 99) {
				//System.out.println("False");
				viol++;
			}
		}
		return viol+this.verifyCapabilityVailidty();
	}
	
	public int  verifyValidityVerbose(){
		int viol = 0;
		System.out.println("Testing");
		for (Integer g: this.getGateways().keySet()) {
			System.out.println("GW "+g.toString()+" Load: "+this.getGateways().get(g).getGwLoad());
			if (this.getGateways().get(g).getGwLoad() > 99) {
				System.out.println("False");
				viol++;
			}
		}
		return viol+this.verifyCapabilityVailidty();
	}
	
	public int verifyCapabilityVailidty(){
		int viol = 0;
		for (Integer a: this.getApps().keySet()){
			if (!this.getApps().get(a).validateRequirements()){
				//System.out.println("App"+this.getApps().get(a).getRequirements());
				//System.out.println("GW"+this.getApps().get(a).getGateway().getCapabilities());
				//System.out.println("False");
				//System.exit(1);
				viol++;
			}
		}
		return viol;
	}
	
	// After we have Clust to Gw we need initial App to GW
	public void basicAppToGwDistribution() {
		// Randomly/Evenly, Based on Load
		// this.addGwAppConn(g1,a1)

		for (Integer c : this.getClusters().keySet()) {
			Float totShare = (float) 0.0;
			int totApps = 0;
			for (Float share : this.getClusters().get(c).getGatewayShare().values()) {
				totShare += share;
			}
			for (Integer a : this.getClusters().get(c).getApps().keySet()) {
				totApps++;
			}
			Float appShare = totApps / (totShare / (float) 100);
			// System.out.println("Per App Share:"+appShare.intValue());
			// System.out.println("Cluster with Share:"+totShare+" Apps:
			// "+totApps);
			Iterator<Integer> app_set = this.getClusters().get(c).getApps().keySet().iterator();
			// Allocate even value to all Gws
			for (Integer g : this.getClusters().get(c).getGateways().keySet()) {
				int i = 0;
				Float gwShare = this.getClusters().get(c).getGatewayShare().get(g) / (float) 100.0 * appShare;
				// System.out.println("Apps on Gw "+g+": "+gwShare.intValue());
				while (i < gwShare.intValue() && app_set.hasNext()) {
					i++;
					int next = app_set.next();
					this.gateways.get(g).addAppConn(this.getApps().get(next));
				}
			}
			List<Integer> gws = new ArrayList<Integer>(this.getClusters().get(c).getGateways().keySet());
			while (app_set.hasNext()) {
				Collections.shuffle(gws);
				// System.out.println(gws);
				int a = app_set.next();
				int gw = gws.get(0);
				this.gateways.get(gws.remove(0)).addAppConn(this.getApps().get(a));
				// System.out.println("Rand Apps on Gw "+gw+" :"+1);
			}
		}
	}

	// Initial Distribute Gw's to Clusters based on Nothing:
	public void basicDistributeGw2Cluster(Map<Integer, Float> clsLoad) {
		Float totFreeGw = (float) 0.0;
		Float totClustLoad = (float) 0.0;
		Float totLoad = (float) 0.0;
		Map<Integer, Float> gwLoad = new HashMap<>();
		Map<Integer, Float> gwCoef = new HashMap<>();
		Map<Integer, List<Integer>> gwClustLink = new HashMap<>();
		List<Integer> freeGateways = new LinkedList<>();
		Map<Integer, Map<Integer, Float>> gwShare = new HashMap<>();
		// Get Free Gw Load
		for (Integer g : this.getGateways().keySet()) {
			freeGateways.add(g);
			gwShare.put(g, new HashMap<>());
			totLoad += this.getGateways().get(g).getGwLoad();
			gwLoad.put(g, this.getGateways().get(g).getGwLoad());
			gwCoef.put(g, this.getGateways().get(g).getPjCap());
			totFreeGw += ((float) 100.0 - this.getGateways().get(g).getGwLoad())
					* this.getGateways().get(g).getPjCap();
		}
		for (Integer c : clsLoad.keySet()) {
			totClustLoad += clsLoad.get(c);
		}
		// System.out.println(clsLoad);
		Float adj = totFreeGw / totClustLoad;
		// System.out.println("Adj: "+adj+"Free: "+totFreeGw+"ClsL:
		// "+totClustLoad);
		for (Integer i : clsLoad.keySet()) {
			clsLoad.put(i, clsLoad.get(i) * adj);
		}
		// System.out.println(gwLoad);
		// System.out.println(clsLoad);
		// Distribute Free Resources to Cluster Randomly
		for (Integer g : gwLoad.keySet()) {
			Float tmp = gwLoad.get(g);
			while (tmp < (float) 99.9) {
				// System.out.println("Gateway:"+g+" Load: "+tmp);
				for (Integer c : clsLoad.keySet()) {
					if (clsLoad.get(c) > (float) 0.0) {
						// System.out.println("Gateway: "+g+" Gw Load: "+tmp+"
						// Cluster: "+c+" Cluster Load: "+clsLoad.get(c));
						// We have unallocated stuff, now eitehr allocate some
						// or all
						if (((float) 100.0 - tmp) * gwCoef.get(g) > clsLoad.get(c)) {
							// System.out.println("First");
							gwShare.get(g).put(c, clsLoad.get(c) / gwCoef.get(g));
							tmp = tmp + clsLoad.get(c) / gwCoef.get(g);
							clsLoad.put(c, (float) 0.0);
						} else {
							// System.out.println("Second");
							clsLoad.put(c, clsLoad.get(c) - ((float) 100.0 - tmp) * gwCoef.get(g));
							gwShare.get(g).put(c, ((float) 100.0 - tmp));
							tmp = (float) 100.0;
							// Gw Full Break Cluster Loop
							break;
						}
					}
				}
			}
			gwLoad.put(g, tmp);
		}

		// Save Data to the Outside
		for (Integer g : gwShare.keySet()) {
			for (Integer c : gwShare.get(g).keySet()) {
				getGateways().get(g).addCluster(getClusters().get(c), gwShare.get(g).get(c),
						(float) 100.0 / adj + (float) 10.0);
				getClusters().get(c).addGateway(getGateways().get(g), gwShare.get(g).get(c),
						(float) 100.0 / adj + (float) 10.0);
			}
		}

		// Print out stuff for debugging
		/*
		 * System.out.println("Clust Distribution"); for (Integer i :
		 * gwShare.keySet()) { Float tmp=(float)0.0; for (Integer j :
		 * gwShare.get(i).keySet()) { tmp+=gwShare.get(i).get(j);
		 * System.out.println("Assignment to gw:"+i+" clust:"+j+" of:" +
		 * gwShare.get(i).get(j)); }
		 * System.out.println("Load:"+(gwLoad.get(i))); } for (Integer i :
		 * clsLoad.keySet()) { System.out.println("Cluster Space Left:"
		 * +clsLoad.get(i)); }
		 */
	}

	// Distribute Gw's to Clusters
	public void distributeGw2Cluster() {
		this.clearGwClustConns();
		Float totFreeGw = (float) 0.0;
		Float totClustLoad = (float) 0.0;
		Float totLoad = (float) 0.0;
		Map<Integer, Float> gwLoad = new HashMap<>();
		Map<Integer, Float> gwCoef = new HashMap<>();
		Map<Integer, Float> clsLoad = new HashMap<>();
		Map<Integer, List<Integer>> gwClustLink = new HashMap<>();
		List<Integer> freeGateways = new LinkedList<>();
		Map<Integer, Map<Integer, Float>> gwShare = new HashMap<>();
		// Get Free Gw Load
		for (Integer g : this.getGateways().keySet()) {
			freeGateways.add(g);
			gwShare.put(g, new HashMap<>());
			totLoad += this.getGateways().get(g).getGwLoad();
			gwLoad.put(g, this.getGateways().get(g).getGwLoad());
			gwCoef.put(g, this.getGateways().get(g).getPjCap());
			totFreeGw += ((float) 100.0 - this.getGateways().get(g).getGwLoad())
					* this.getGateways().get(g).getPjCap();
		}
		for (Integer c : this.getClusters().keySet()) {
			clsLoad.put(c, this.getClusters().get(c).getClusterLoad());
			totClustLoad += this.getClusters().get(c).getClusterLoad();
			Map<Integer, Integer> gwRes = this.getClusters().get(c).getGwResourcesCount();
			for (Integer g : gwRes.keySet()) {
				freeGateways.remove(g);
				if (gwClustLink.get(g) == null) {
					List<Integer> tmp = new LinkedList<>();
					tmp.add(c);
					gwClustLink.put(g, tmp);
				} else {
					gwClustLink.get(g).add(c);
				}
			}
		}
		Float adj = totFreeGw / totClustLoad;
		for (Integer i : clsLoad.keySet()) {
			clsLoad.put(i, clsLoad.get(i) * adj);
		}
		// Take each Gateway check how many resources are on it then distribute
		// evenly\
		for (Integer g : gwClustLink.keySet()) {
			Float unit = ((float) 100.0 - gwLoad.get(g)) / (float) gwClustLink.get(g).size();
			for (Integer c : gwClustLink.get(g)) {
				if (clsLoad.get(c) >= (float) 0.0) {
					if (clsLoad.get(c) / gwCoef.get(g) >= unit) {
						gwShare.get(g).put(c, unit);
						gwLoad.put(g, gwLoad.get(g) + unit);
						clsLoad.put(c, clsLoad.get(c) - unit * gwCoef.get(g));
					} else {
						gwShare.get(g).put(c, clsLoad.get(c) / (float) gwCoef.get(g));
						gwLoad.put(g, gwLoad.get(g) + clsLoad.get(c) / (float) gwCoef.get(g));
						clsLoad.put(c, (float) 0.0);
					}
				}
			}
		}
		for (Integer g : gwLoad.keySet()) {
			Float tmp = gwLoad.get(g);
			while (tmp < (float) 99.9) {
				// System.out.println(g+" "+tmp);
				for (Integer c : clsLoad.keySet()) {
					if (clsLoad.get(c) > (float) 0.0) {
						// System.out.println(g+" "+tmp+" "+c+"
						// "+clsLoad.get(c));
						// We have unallocated stuff, now eitehr allocate some
						// or all
						if (((float) 100.0 - tmp) * gwCoef.get(g) > clsLoad.get(c)) {
							// System.out.println("First");
							gwShare.get(g).put(c, clsLoad.get(c) / gwCoef.get(g));
							tmp = tmp + clsLoad.get(c) / gwCoef.get(g);
							clsLoad.put(c, (float) 0.0);
						} else {
							// System.out.println("Second");
							clsLoad.put(c, clsLoad.get(c) - ((float) 100.0 - tmp) * gwCoef.get(g));
							gwShare.get(g).put(c, ((float) 100.0 - tmp));
							tmp = (float) 100.0;
							// Gw Full Break Cluster Loop
							break;
						}
					}
				}
			}
			gwLoad.put(g, tmp);
		}

		// Save Data to the Outside
		for (Integer g : gwShare.keySet()) {
			for (Integer c : gwShare.get(g).keySet()) {
				getGateways().get(g).addCluster(getClusters().get(c), gwShare.get(g).get(c),
						(float) 100.0 / adj + (float) 10.0);
				getClusters().get(c).addGateway(getGateways().get(g), gwShare.get(g).get(c),
						(float) 100.0 / adj + (float) 10.0);
			}
		}

		/*
		 * System.out.println("Clust Distribution"); for (Integer i :
		 * gwShare.keySet()) { Float tmp=(float)0.0; for (Integer j :
		 * gwShare.get(i).keySet()) { tmp+=gwShare.get(i).get(j);
		 * System.out.println("Assignment to gw:"+i+" clust:"+j+" of:" +
		 * gwShare.get(i).get(j)); }
		 * System.out.println("Load:"+(gwLoad.get(i))); } for (Integer i :
		 * clsLoad.keySet()) { System.out.println("Cluster Space Left:"
		 * +clsLoad.get(i)); }
		 */
	}

	// Assing Connections to Gws's

	public void addGwConns(Float minLat, Float maxLat) {
		for (Integer i : getGateways().keySet()) {
			for (Integer j : getGateways().keySet()) {
				if (i != j) {
					addGwGwConn(getGateways().get(i), getGateways().get(j),
							random.nextFloat() * (maxLat - minLat) + minLat);
				}
			}
		}
	}

	public void addExtraGwConns(List<Integer> newGws, Set<Integer> oldGws, float[] gwCloudLat, float[] cloudInt) {
		for (Integer i : oldGws) {
			for (Integer j : newGws) {
				if (i != j) {
				addGwGwConn(getGateways().get(i), getGateways().get(j),
						random.nextFloat() * (gwCloudLat[1]-gwCloudLat[0])+gwCloudLat[0]);
				}
			}
		}
	for (Integer i : newGws) {
		for (Integer j : newGws) {
			if (i != j) {
				addGwGwConn(getGateways().get(i), getGateways().get(j), random.nextFloat() * (cloudInt[1] - cloudInt[0]) + cloudInt[0]);
			}
		}
	}
	}
	// Assign Resource to Gw and Apps

	public void AssignAppsToGws(Map<Integer, Integer> con) {
		// Clear Gw's First
		// Assign New Ones
		//System.out.println("Assigment set");
		for (Integer a : con.keySet()) {
				try{
					this.addGwAppConn(con.get(a), a);
				} catch(NullPointerException e) {
					System.out.print("Add App to Gw Conns Exception Gateway: "+con.get(a)+" App:"+a);
					e.printStackTrace();
				}
		//	System.out.println("Assigning App"+a+"to gw:"+con.get(a));
		}
		/*for (Integer g: this.gateways.keySet()){
			System.out.print("Gateway: "+g);
			System.out.println(" Load: "+this.gateways.get(g).getGwLoad());
			System.out.println(" Apps: "+this.gateways.get(g).getApps().toString());
		}*/
	}

	public void clearAppToGws() {
		for (Integer g : this.getGateways().keySet()) {
			this.gateways.get(g).clearApps();
		}
		for (Integer a: this.getApps().keySet()){
			this.getApps().get(a).setGateway(null);
		}
	}

	public void addAppConns(Integer a, Map<String, Map<String, Map<String, Float>>> msg) {
		// Chose Gateway in Cluster
		Object[] values = apps.get(a).getCluster().getGateways().keySet().toArray();
		Integer gId = (Integer) values[random.nextInt(values.length)];
		// System.out.println(apps.get(a).getCluster().getGateways().keySet()+"
		// - "+randomValue);

		// Generate Resources
		// Local
		String[] types = { "Device", "Cloud", "Storage", "LocalAccess" };
		for (String type : types) {
			// all.get(type).get("Internal").put("Cnt", (float)count);
			// all.get(type).get("Internal").put("Rate", rate);
			Integer count = msg.get(type).get("Internal").get("Cnt").intValue();
			for (int i = 0; i < count; i++) {
				Integer r = this.addResource(type + "Loc_App" + a + "GW:" + gId + "_" + i, type);
				// System.out.println(type + " Gid " + gId + " Rid " + r);
				this.addGwResConn(gId, r);
				this.addAppResConn(a, r, msg.get(type).get("Internal").get("Rate"));
			}
			// Ext
			count = msg.get(type).get("External").get("Cnt").intValue();
			for (int i = 0; i < count; i++) {
				values = this.getGateways().keySet().toArray();
				Integer g2Id = gId;
				while (g2Id == gId) {
					g2Id = (Integer) values[random.nextInt(values.length)];
				}
				Integer r = this.addResource(type + "Ext_App" + a + "GW:" + g2Id + "_" + i, type);
				// System.out.println(type + " Ext-GId " + g2Id + " Rid " + r);
				this.addGwResConn(g2Id, r);
				this.addAppResConn(a, r, msg.get(type).get("External").get("Rate"));
			}
		}
		// Generate Apps
		// Local and CLuster (No Local)
		String type = "Apps";
		Integer count = msg.get(type).get("Internal").get("Cnt").intValue();
		Object[] app_list = apps.get(a).getCluster().getApps().keySet().toArray();
		for (int i = 0; i < count; i++) {
			Integer a2 = 0;
			app_list = ArrayUtils.removeElement(app_list, (Object) a);
			if (app_list.length > 0) {
				a2 = (Integer) app_list[random.nextInt(app_list.length)];
				app_list = ArrayUtils.removeElement(app_list, (Object) a2);
				// System.out.println(type + " AId " + a2);
				if (a2 != a) {
					this.addAppAppConnection(a, a2, msg.get(type).get("Internal").get("Rate"));
				}
			}
		}
		// External
		app_list = apps.get(a).getCluster().getApps().keySet().toArray();
		List<Integer> l1 = new ArrayList(Arrays.asList(app_list));
		List<Integer> l2 = new ArrayList(Arrays.asList(getApps().keySet().toArray()));
		l2.removeAll(l1);
		Object[] diff_list = l2.toArray();
		Integer a2 = a;
		count = msg.get(type).get("External").get("Cnt").intValue();
		for (int i = 0; i < count; i++) {
			if (diff_list.length > 0) {
				a2 = (Integer) diff_list[random.nextInt(diff_list.length)];
				diff_list = ArrayUtils.removeElement(diff_list, (Object) a2);
				// System.out.println(type + " AiD " + a2);
				if (a2 != a) {
					this.addAppAppConnection(a, a2, msg.get(type).get("External").get("Rate"));
				}
			}
		}
	}

	public Map<String, Map<String, Map<String, Float>>> getAppRandMsges(Float uLoad) {
		Float[] msg_range = { (float) 0.8, (float) 5 };
		if (uLoad >= 3) {
			msg_range[1] = msg_range[1] / (float) (uLoad / 3.0);
		}
		float totA = (float) 0.0;
		float totR = (float) 0.0;
		Map<String, Map<String, Map<String, Float>>> all = new HashMap<>();
		String[] types = { "Device", "Cloud", "Storage", "LocalAccess" };
		for (String type : types) {
			all.put(type, new HashMap<>());
			Integer count = getRandomDist((float) structure.get(type)[0][0], (float) structure.get(type)[0][1],
					(float) structure.get(type)[0][2]);
			float rate = random.nextFloat() * (msg_range[1] - msg_range[0]) + msg_range[0];
			totR += (float) rate * (float) count;
			all.get(type).put("Internal", new HashMap<>());
			all.get(type).get("Internal").put("Cnt", (float) count);
			all.get(type).get("Internal").put("Rate", rate);
			// External
			count = getRandomDist((float) structure.get(type)[1][0], (float) structure.get(type)[1][1],
					(float) structure.get(type)[1][2]);
			rate = random.nextFloat() * (msg_range[1] - msg_range[0]) + msg_range[0];
			totR += (float) rate * (float) count;
			all.get(type).put("External", new HashMap<>());
			all.get(type).get("External").put("Cnt", (float) count);
			all.get(type).get("External").put("Rate", rate);

		}
		String type = "Apps";
		all.put(type, new HashMap<>());
		Integer count = getRandomDist((float) structure.get(type)[0][0] + (float) structure.get(type)[1][0],
				(float) structure.get(type)[0][1] + (float) structure.get(type)[1][1],
				(float) structure.get(type)[0][2] + (float) structure.get(type)[1][2]);
		float rate = random.nextFloat() * (msg_range[1] - msg_range[0]) + msg_range[0];
		totA += rate * (float) count;
		all.get(type).put("Internal", new HashMap<>());
		all.get(type).get("Internal").put("Cnt", (float) count);
		all.get(type).get("Internal").put("Rate", rate);
		count = getRandomDist((float) structure.get(type)[2][0], (float) structure.get(type)[2][1],
				(float) structure.get(type)[2][2]);
		rate = random.nextFloat() * (msg_range[1] - msg_range[0]) + msg_range[0];
		totA += rate * (float) count;
		all.get(type).put("External", new HashMap<>());
		all.get(type).get("External").put("Cnt", (float) count);
		all.get(type).get("External").put("Rate", rate);
		all.put("Tot", new HashMap<>());
		all.get("Tot").put("Tot", new HashMap<>());
		all.get("Tot").get("Tot").put("Res", totR);
		all.get("Tot").get("Tot").put("App", totA);
		return all;
	}
	
	public List<Integer> checkIfAppsAllocated(){
		List<Integer> apps;
		apps = new ArrayList(this.getApps().keySet());
		for (Integer c: this.getClusters().keySet()){
			apps.removeAll(new ArrayList(this.getClusters().get(c).getApps().keySet()));
		}
		//Check for GW Allocations, totally useless piece of fking code
		/*for (Integer a: this.getApps().keySet()){
			if (this.getApps().get(a).getGateway()==null){
				apps.add(a);
			}
		}*/
		return apps;
	}
	
	public List<Integer> checkIfAppsOverAlloc() {
		List<Integer> apps= new ArrayList<>();
		for (Integer c1: this.getClusters().keySet()){
			for (Integer a: this.getClusters().get(c1).getApps().keySet()){
				for (Integer c2: this.getClusters().keySet()){
					if (c1 != c2){
						if (this.getClusters().get(c2).getApps().get(a)!=null){
							//System.out.println("Clusters: "+c1+" : "+c2+" On App:"+a);
							//System.out.println(this.getClusters().get(c1).getApps().keySet());
							//System.out.println(this.getClusters().get(c2).getApps().keySet());
							apps.add(a);
						}
					}
				}
			}
		}
		return apps;
	}
	
	// Random param between values
	public Integer getRandomDist(Float min, Float max, Float avg) {
		// ToDo Do F-Distribution
		Float gaus = (float) -1.0;
		if (avg < 0.5) {
			avg *= 2;
		}
		while ((gaus > max) || (gaus < min)) {
			gaus = ((float) random.nextGaussian() + 1) * avg + min;
		}
		// System.out.println(min + ":" + max + ":" + avg + ":" + gaus + ":" +
		// Math.round(gaus));
		return Math.round(gaus);

	}
	// Add Connections

	public float getTotalLoad() {
		Float load = (float) 0.0;
		for (Integer a: this.getApps().keySet()){
			load+=this.getApps().get(a).getAppLoad((float)1.0);
		}
		return load;
	}

	public float getTotalFreeCapacity() {
		Float free = (float) 0.0;
		for (Integer g: this.getGateways().keySet()){
			free = free + (float)(100.0-this.getGateways().get(g).getGwLoad())*this.getGateways().get(g).getPjCap();
		}
		return free;
	}
	
	public void setGwGwConnections(Float[][] conn) {
		for (Float[] a : conn) {
			gateways.get(a[0].intValue()).addGateway(gateways.get(a[1].intValue()), a[2]);
			gateways.get(a[1].intValue()).addGateway(gateways.get(a[0].intValue()), a[2]);
			if (connections.get(a[0]) == null) {
				connections.put(a[0].intValue(), new HashMap<>(a[1].intValue(), a[2]));
			} else {
				connections.get(a[0]).put(a[1].intValue(), a[2]);
			}
			if (connections.get(a[1]) == null) {
				connections.put(a[1].intValue(), new HashMap<>(a[0].intValue(), a[2]));
			} else {
				connections.get(a[1]).put(a[0].intValue(), a[2]);
			}
		}
	}

	public void addGwGwConn(Gateway g1, Gateway g2, Float latency) {
		if (connections.get(g1.getId()) == null) {
			connections.put(g1.getId(), new HashMap<>(g2.getId(), latency));
		} else {
			connections.get(g1.getId()).put(g2.getId(), latency);
		}
		if (connections.get(g2.getId()) == null) {
			connections.put(g2.getId(), new HashMap<>(g1.getId(), latency));
		} else {
			connections.get(g2.getId()).put(g1.getId(), latency);
		}
		g1.addGateway(g2, latency);
		g2.addGateway(g1, latency);
	}

	public void setGwResConnection(Integer[][] conn) {
		for (Integer[] a : conn) {
			gateways.get(a[0]).addResConn(resources.get(a[1]));
		}
	}

	public void addGwResConn(Integer gw, Integer res) {
		gateways.get(gw).addResConn(resources.get(res));
	}

	public void setAppGwConnection(Integer[][] conn) {
		for (Integer[] a : conn) {
			gateways.get(a[0]).addAppConn(apps.get(a[1]));
		}
	}

	public void addGwAppConn(Integer gw, Integer app) {
		gateways.get(gw).addAppConn(apps.get(app));
	}
	
	public void createClusters(List<Set<Integer>> clust) {
		//System.out.println("Clusters: "+clust);
		int i=0;
		for (Set<Integer> c:clust){
			int cls = this.addCluster("Cluster_" + i);
			for (Integer a:c){
				this.addClustAppConn(cls, a);
			}
			i++;
		}	
	}
	
	public void setAppClustConnection(Integer[][] conn) {
		for (Integer[] a : conn) {
			clusters.get(a[0]).addAppConn(apps.get(a[1]));
		}
	}

	public void addClustAppConn(Integer cls, Integer app) {
		clusters.get(cls).addAppConn(apps.get(app));
	}

	public void setClustGwConnection(Integer[][] conn) {
		for (Integer[] a : conn) {
			clusters.get(a[0]).addGateway(gateways.get(a[1]));
			gateways.get(a[1]).addCluster(clusters.get(a[0]));
		}
	}

	public void addClustGwConn(Integer cls, Integer gw, Float load) {
		clusters.get(cls).addGateway(gateways.get(gw));
		gateways.get(gw).addCluster(clusters.get(cls));
	}

	public void addClustGwConn(Integer cls, Integer gw, Float share, Float load) {
		clusters.get(cls).addGateway(gateways.get(gw), share, load);
		gateways.get(gw).addCluster(clusters.get(cls), share, load);
	}

	public void setAppResourceConnection(Integer[][] conn, Float msg_rate) {
		for (Integer[] a : conn) {
			apps.get(a[0]).addResource(resources.get(a[1]), msg_rate);
		}
	}

	public void addAppResConn(Integer app, Integer res, Float msg_rate) {
		apps.get(app).addResource(resources.get(res), msg_rate);
	}

	public void setAppAppConnection(Integer[][] conn, Float msg_rate) {
		for (Integer[] a : conn) {
			apps.get(a[0]).addApps(apps.get(a[1]), msg_rate);
			apps.get(a[1]).addApps(apps.get(a[0]), msg_rate);
		}
	}

	public void addAppAppConnection(Integer app1, Integer app2, Float msg_rate) {
		apps.get(app1).addApps(apps.get(app2), msg_rate);
	}
	// AddComponents

	public int addGateway(String name, String type) {
		Gateway g = new Gateway(name, type);
		this.gateways.put(g.getId(), g);
		return g.getId();
	}
	
	public void addGateway(int id,String name,String type,float Lidle,float PjCap, float PjSpeed) {
		Gateway g = new Gateway(id,name,type,Lidle, PjCap, PjSpeed);
		this.gateways.put(id, g);
	}

	public int addApp(String name, String type) {
		App a = new App(name, type, this.appTypes.get(type));
		this.apps.put(a.getId(), a);
		return a.getId();
	}
	
	public void addApp(int id, String name, String type,float unitLoad) {
		App a = new App(id, name, type, unitLoad);
		this.apps.put(id, a);
	}

	public int addResource(String name, String type) {
		Resource r = new Resource(name, type);
		this.resources.put(r.getId(), r);
		return r.getId();
	}
	
	public void addResource(int id, String name, String type) {
		Resource r = new Resource(id, name, type);
		this.resources.put(id, r);
	}
	
	public int addCluster(String name) {
		Cluster c = new Cluster(name);
		this.clusters.put(c.getId(), c);
		return c.getId();
	}
	
	public void addCluster(int id,String name) {
		Cluster c = new Cluster(id,name);
		this.clusters.put(id, c);
	}

	// Clear Gateway Clust
	public void clearGwClustConns() {
		for (Integer g : getGateways().keySet()) {
			this.getGateways().get(g).clearClusters();
		}
		for (Integer c : getClusters().keySet()) {
			this.getClusters().get(c).clearGateways();
		}
	}
	
	public void removeClusters(){
		Cluster.resetIndex();
		this.clusters.clear();
	}

	// Init Cluster

	public void initCluster(float[] clust) {
		if (clust == null) {
			this.clustParam.put("min", (float) 1);
			this.clustParam.put("max", (float) 22);
			this.clustParam.put("avg", (float) 7.42);
		} else {
			this.clustParam.put("min", clust[0]);
			this.clustParam.put("max", clust[1]);
			this.clustParam.put("avg", clust[2]);
		}
	}

	// Initial Structure
	public void InitStructure(float[][] dev, float[][] cloud, float[][] store, float[][] loc, float[][] apps) {
		if ((dev == null) || (cloud == null) || (store == null) || (loc == null) || (apps == null)) {
			float[][] a = { { 0, 8, (float) 2.3 }, { 0, 1, (float) 0.1 }, { 0, 8, (float) 2.4 } };
			float[][] b = { { 0, 1, (float) 0.1 }, { 0, 1, (float) 0.1 }, { 0, 1, (float) 0.2 } };
			float[][] c = { { 0, 1, (float) 0.15 }, { 0, 1, (float) 0.1 }, { 0, 1, (float) 0.25 } };
			float[][] d = { { 0, 2, (float) 0.26 }, { 0, 2, (float) 0.1 }, { 0, 2, (float) 0.36 } };
			float[][] e = { { 0, 6, (float) 1.64 }, { 0, 14, (float) 1.21 }, { 0, 3, (float) 0.32 },
					{ 1, 15, (float) 3.17 } };
			this.structure.put("Device", a);
			this.structure.put("Cloud", b);
			this.structure.put("Storage", c);
			this.structure.put("LocalAccess", d);
			this.structure.put("Apps", e);
		} else {
			this.structure.put("Device", dev);
			this.structure.put("Cloud", cloud);
			this.structure.put("Storage", store);
			this.structure.put("LocalAccess", loc);
			this.structure.put("Apps", apps);
		}
	}

	// Initial Apps

	public void InitApps(Map<String, Float> app) {
		if (app == null) {
			this.appTypes.put("Testing_App1", (float) 0.4);
			this.appTypes.put("Testing_App2", (float) 0.8);
			this.appTypes.put("Testing_App3", (float) 1.2);
			this.appTypes.put("Testing_App4", (float) 1.6);
			this.appTypes.put("Testing_App5", (float) 2.0);
			this.appTypes.put("Testing_App6", (float) 2.4);
			this.appTypes.put("Testing_App7", (float) 2.8);
			this.appTypes.put("Testing_App8", (float) 3.2);
			this.appTypes.put("Testing_App9", (float) 3.6);
			this.appTypes.put("Testing_App10", (float) 4.0);
			this.appTypes.put("Testing_App11", (float) 4.4);
		} else {
			this.appTypes = app;
		}
	}
	
	
	//Reset Stuff
	public void resetCounts() {
		App.resetCount();
		Gateway.resetCount();
		Resource.resetCount();
	}
	// Basic Setters and Getters for Gateway
	
	
	public Map<Integer, Gateway> getGateways() {
		return gateways;
	}

	public Map<Integer, Map<Integer, Float>> getConnections() {
		return connections;
	}

	public void setGateways(Map<Integer, Gateway> gateways) {
		this.gateways = gateways;
	}

	public Map<Integer, App> getApps() {
		return apps;
	}

	public void setApps(Map<Integer, App> apps) {
		this.apps = apps;
	}

	public Map<Integer, Resource> getResources() {
		return resources;
	}

	public void setResources(Map<Integer, Resource> resources) {
		this.resources = resources;
	}

	public Map<Integer, Cluster> getClusters() {
		return clusters;
	}

	public void setClusters(Map<Integer, Cluster> clusters) {
		this.clusters = clusters;
	}

	public String getScenario() {
		return ScenarioType;
	}

	public void setScenario(String scenarioType) {
		ScenarioType = scenarioType;
	}

	public List<Set<Integer>> retreiveCluster() {
		List<Set<Integer>> clust = new ArrayList<>();
		for( Integer clsI : this.getClusters().keySet()){
			Set<Integer> cls = this.getClusters().get(clsI).getApps().keySet();	
			clust.add(cls);
		}
		return clust;
	}




}
