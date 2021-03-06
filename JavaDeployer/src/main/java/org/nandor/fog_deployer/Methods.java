package org.nandor.fog_deployer;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;


public class Methods {

	public static DataGatherer dataG = new DataGatherer();
	
	public static void DisplayData(Fog f) {
		/*for (Integer i : f.getApps().keySet()) {
			System.out.println(f.getApps().get(i).getInfo());
		}
		System.out.println(f.getApps().get(1).getAppLoad((float) 1) + " "
				+ f.getApps().get(1).getProcDelay((float) 56.0, (float) 1.0));
		*/
		System.out.println("----Displaying Fog----");
		System.out.println(f.toString());
		System.out.println("Apps: " + f.getApps());
		System.out.println("Apps of  1: "+f.getApps().get(1).getTotDelay());
		System.out.println("Gw1 1: "+f.getGateways().get(1).getInfo());
		System.out.println("Resource: " + f.getResources().toString());
		System.out.println("Gateways: " + f.getGateways().toString());
		System.out.println("Clusters: " + f.getClusters().toString());
		for (Integer i : f.getClusters().keySet()) {
			System.out.println("Cluster "+i+" Load: "+f.getClusters().get(i).getClusterLoad());
		}
		for (Integer i : f.getGateways().keySet()) {
			System.out.println("Gateway " + f.getGateways().get(i).getInfo());
			for (Integer j: f.getGateways().get(i).getCluster().keySet()){
				System.out.println("Gw "+i+" Cluster "+j+" Share: "+f.getGateways().get(i).getClusterShare(j));
			}
		}
		
		System.out.println("Fog Utility: "+f.getFogCompoundUtility());
		System.out.println("Fog Delay: "+f.getFogCompoundDelay());
		System.out.println("Fog Reliability: "+f.getFogCompoundReliability());
	}
	
	static TimerTask timerTask = new TimerTask() {
	    @Override
	    public void run() {
	    	Runtime runtime = Runtime.getRuntime();

	    	long maxMemory = runtime.maxMemory();
	    	long allocatedMemory = runtime.totalMemory();
	    	long freeMemory = runtime.freeMemory();
	    	System.out.println("-----Memory Use-----");
	    	System.out.println("free memory: " + (freeMemory / 1024.0/1024.0));
	    	System.out.println("allocated memory: " + (allocatedMemory / 1024.0/1024.0));
	    	System.out.println("max memory: " + (maxMemory / 1024.0/1024.0));
	    	System.out.println("total free memory: " + (freeMemory + (maxMemory - allocatedMemory))/1024.0/1024.0);
	    	System.out.println("-----End Memory Use-----");
	    }
	};
	
	public static Fog InitFog(int ClsCount, int cloudGw){
		//Initialization and Generation
		Fog f = new Fog("Main Fog");
		float[] lat = {(float)8.97,(float)30.897};
		float[] lat2 = {(float)37.37,(float)87.89};
		float[] lat3 = {(float)2.37,(float)6.89};
		f.generateNewFog(ClsCount,(float)30,(float)60,(float)0.1,(float)0.05,lat,cloudGw,lat2,lat3,7,1);
		//Analysis part, of distributing Gw's to clusters		
		return f;
	}
	


	public static Map<String,Float> ExhaustiveGlobal(Fog f) {
		Map<String,Float> data = new HashMap<>();
		System.out.println("----- Exhaustive Global -----");
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		f.setDeplpyment(g.ExhaustiveAlloc());
		f.deployFog();
		data.put("Utility",f.getFogCompoundUtility());
		data.put("Time",(System.currentTimeMillis()-start)/(float)1000);
		System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());	
		System.out.println("Fog Utility: "+f.getFogCompoundUtility());
		System.out.println("Time Elapsed: "+(System.currentTimeMillis()-start)/(float)1000);
		return data;
	}
	
	public static Map<String,Float> GAGlobalStuff(Fog f,int size,int cnt,boolean safe){
		Map<String,Float> data = new HashMap<>();
		System.out.println("----- GA Stuff -----");
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		f.setDeplpyment(g.GAGlobal(size,cnt,safe));
		f.deployFog();
		data.put("Utility",f.getFogCompoundUtility());
		data.put("Time",(System.currentTimeMillis()-start)/(float)1000);
		System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());	
		System.out.println("Fog Utility: "+f.getFogCompoundUtility());
		System.out.println("Time Elapsed: "+(System.currentTimeMillis()-start)/(float)1000);
		return data;
	}
	
	public static List<Map<Integer, Integer>> GAGlobal(Fog f){
		Map<String,Float> data = new HashMap<>();
		System.out.println("---------------------------");
		System.out.println("----- A. Global GA Stuff -----");
		System.out.println("---------------------------");
		long start=System.currentTimeMillis();

		dataG.setBestCluster(f.retreiveCluster());

		Genetic g = new Genetic(f);
		int size = getMinPtsSize(f.getApps().size(),f.getScenario());
		int cnt = getMinPtsCount(f.getApps().size(),f.getScenario());
		f.setDeplpyment(g.GAGlobal(size,cnt,true,10000,dataG));
		System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());
		f.deployFog();
		System.out.println("Fog Utility: "+f.getFogCompoundUtility());
		System.out.println("Time Elapsed: "+(System.currentTimeMillis()-start)/(float)1000);
		if (f.checkIfAppsAllocated().size()!=0 || f.getFogCompoundUtility().isNaN()){
			return null;
		}else{
			dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
			dataG.addUtility(f.getFogCompoundUtility());
			return g.getBestGens();
		}
		/*System.out.println("Bests:");
		for ( Map<Integer,Integer> gen: g.getBestGens()){
			f.AssignAppsToGws(gen);
			float tmp = f.getFogCompoundUtility();
			//System.out.println(tmp);
			if (f.verifyIndValidity()){
				System.out.println("Util: "+tmp+" With Gen: "+gen);
			}
		}*/
	}
	
	public static float GAGlobal(Fog f,int size, int generations){
		Map<String,Float> data = new HashMap<>();
		System.out.println("------------------------");
		System.out.println("----- GA Reference -----");
		System.out.println("------------------------");
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		f.setDeplpyment(g.GAGlobal(size,generations,true,10000,dataG));
		f.deployFog();
		if (f.checkIfAppsAllocated().size()!=0 || f.getFogCompoundUtility().isNaN() || f.verifyIndValidity()!=0){
			System.out.println("GA Failed with FailAppCnt:"+f.checkIfAppsAllocated().size()+" Utility:"+f.getFogCompoundUtility()+ " Valid:"+f.verifyIndValidity());
			return (float)0.0;
		}else{
			return f.getFogCompoundUtility();
		}
	}
	
	public static Map<Integer, Float> GAGlobalEndCnd(Fog f,int size){
		Map<String,Float> data = new HashMap<>();
		System.out.println("------------------------");
		System.out.println("----- GA End Condition -----");
		System.out.println("------------------------");
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		Map<Integer, Float> best = g.GAGlobalEndCnd(size,5000);
		if (best==null){
			return null;
		}else{
			return best;
		}
	}
	
	public static Float GAGlobalSize(Fog f,int size){
		Map<String,Float> data = new HashMap<>();
		System.out.println("------------------------");
		System.out.println("----- GA Size -----");
		System.out.println("------------------------");
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		Float best = g.GAGlobalSize(size,5000);
		if (best==null){
			return null;
		}else{
			return best;
		}
	}
	
	public static Float GAGlobal(Fog f,int size,double d){
		Map<String,Float> data = new HashMap<>();
		float proc = (float) 0.9995;
		System.out.println("---------------------");
		System.out.println("----- GA Follow -----");
		System.out.println("---------------------");
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		Float time = g.GAGlobal(size,d*proc);
		f.deployFog();
		if (f.checkIfAppsAllocated().size()!=0 || f.getFogCompoundUtility().isNaN()){
			return Float.MAX_VALUE;
		}else{
			return time;
		}
	}
	

	public static Float getUtility(Fog f,Map<Integer, Integer> pop){
		f.clearAppToGws();
		f.AssignAppsToGws(pop);
		float ret = f.getFogCompoundUtility();
		f.clearAppToGws();
		return ret;
	}
	
	public static Float getPartialUtility(Fog f,Map<Integer, Integer> pop){
		f.clearAppToGws();
		f.AssignAppsToGws(pop);
		float ret = f.getPartialFogUtility();
		f.clearAppToGws();
		return ret;
	}
	

	public static Map<String,String> GAClusStuff(Fog f,int size,int cnt,boolean safe){
		System.out.println("----- Clust GA Stuff -----");
		Map<String,String> data = new HashMap<>();
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		//DisplayData(f);
		for (Integer i: f.getClusters().keySet()){
			f.getClusters().get(i).setDeployment(g.GACluster(size,cnt, f.getClusters().get(i),safe));
		}
		f.deployClusters();
		if (f.checkIfAppsAllocated().size()==0){
			data.put("1.Utility",f.getFogCompoundUtility().toString());
			data.put("2.Time",String.format("%.2f",(System.currentTimeMillis()-start)/(float)1000));
			data.put("3.ClusterSize",""+f.getClusters().size());
			//data.put("3.Cluster",f.getClusters().toString());
			//data.put("4.BestPop",f.getDeployment().toString());
			System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());
			System.out.println("Fog Utility: "+f.getFogCompoundUtility());
			System.out.println("Time Elapsed: "+(System.currentTimeMillis()-start)/(float)1000);
			//System.out.println("Cluster: "+f.getClusters());
			//System.out.println("BestPop: "+f.getDeployment());
			return data;}
		else{
				System.out.println(f.checkIfAppsAllocated());
				return null;
			}
	}
	
	public static List<Map<Integer, Integer>> GAClus(Fog f,boolean safe,DataGatherer dg){
		//System.out.println("----- Clust GA Stuff -----");
		Map<String,String> data = new HashMap<>();
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		//DisplayData(f);
		for (Integer i: f.getClusters().keySet()){
			int clsSize = f.getClusters().get(i).getApps().size();
			int size = getMinPtsSize(clsSize,f.getScenario());
			int count = getMinPtsCount(clsSize,f.getScenario());
			Map<Integer, Integer> tmp = g.GACluster(size,count, f.getClusters().get(i),safe,5000);
			if (tmp!=null){
				f.getClusters().get(i).setDeployment(tmp);
			}else{
				return null;
			}
		}
		f.deployClusters();
		if (f.checkIfAppsAllocated().size()==0){
			//data.put("1.Utility",f.getFogCompoundUtility().toString());
			//data.put("2.Time",String.format("%.2f",(System.currentTimeMillis()-start)/(float)1000));
			//data.put("3.ClusterSize",""+f.getClusters().size());
			//data.put("3.Cluster",f.getClusters().toString());
			//data.put("4.BestPop",f.getDeployment().toString());
			//System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());
			System.out.println("Fog Utility: "+f.getFogCompoundUtility());
			//System.out.println("Time Elapsed: "+(System.currentTimeMillis()-start)/(float)1000);
			//System.out.println("Cluster: "+f.getClusters());
			//System.out.println("BestPop: "+f.getDeployment());
			List<Map<Integer,Integer>> ret = new LinkedList<>();
			ret.add(f.getDeployment());
			ret.addAll(g.getBestClsGens());
			return ret;
			}
		else{
				System.out.println("Failed with Unallocated: "+f.checkIfAppsAllocated());
				return null;
			}
	}
	
	public static List<Map<Integer, Integer>> sampGAClus(Fog f,boolean safe){
		//System.out.println("----- Clust GA Stuff -----");
		Map<String,String> data = new HashMap<>();
		long start=System.currentTimeMillis();
		Genetic g = new Genetic(f);
		//DisplayData(f);
		for (Integer i: f.getClusters().keySet()){
			int clsSize = f.getClusters().get(i).getApps().size();
			int size = getMinPtsSize(clsSize,f.getScenario());
			int count = getMinPtsCount(clsSize,f.getScenario())/4;
			Map<Integer, Integer> tmp = g.GACluster(size,count, f.getClusters().get(i),safe,5000);
			if (tmp!=null){
				f.getClusters().get(i).setDeployment(tmp);
				f.deployClusters();
				return g.getBestClsGens();
			}else{
				return null;
			}
		}
		return null;
	}
	
	public static List<Map<Integer, Integer>> DistanceClusteringDeployment(Fog f){
		System.out.println("------------------------------------------");
		System.out.println("----- B. Distance Clustering Deployment -----");
		System.out.println("------------------------------------------");
		Genetic g = new Genetic(f);
		int eps = 1;
		//int minPts = getMinPts(f);
		int minPts = 10;
		int size = getMinPtsSize(minPts,f.getScenario());
		int count = getMinPtsCount(minPts,f.getScenario());
		long start=System.currentTimeMillis();
		
		dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
		dataG.addUtility((float)0.0);
		
		while(!Clustering(f,eps,minPts)){
			minPts = minPts-1;
			if (minPts<3){
				return null;
			}
		};
		dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
		dataG.addUtility((float)0.0);
		
		ResourceAllocation(f);
		dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
		dataG.addUtility((float)0.0);
		List<Map<Integer, Integer>> ret = Methods.GAClus(f, true,dataG);
		if (ret!=null){
			dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);	
			f.setDeplpyment(ret.get(0));
			f.deployFog();
			dataG.addUtility(f.getFogCompoundUtility());	
			dataG.setBestCluster(f.retreiveCluster());
			float tot_sec=(System.currentTimeMillis()-start)/(float)1000;
			System.out.println("Finished Clusering Part in:"+tot_sec);
			System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());
			System.out.println("Total Elapsed Time:"+ (System.currentTimeMillis()-start)/1000.0);
			return ret;
		}else{
			System.out.println("Distance Clustering Failed");
			return null;
		}
	}
	
	public static int getMinPtsCount(int minPts,String scenario) {
		int ret =0;
		switch (scenario) {
		case "Multi":
			ret =  (int)(694.57+0.2922*(double)minPts);
			break;
		case "Capab":			
			ret =  (int)(200+40.45*(double)minPts);
			break;
		case "Delay":
			ret = (int)(376.88+2.168*(double)minPts);
			break;
		default:
			System.out.println("Default:");
			ret =  (int)(694.57+0.2922*(double)minPts);
			break;
		}
		if (ret>20000){
			System.out.println("Limited from: "+ret);
			ret =20000;
		}
		return ret;
	}

	public static int getMinPtsSize(int minPts,String scenario) {
		int ret = 0;
		switch (scenario) {
		case "Multi":
			ret =  (int)(40.31+0.22*(double)minPts);
			break;
		case "Capab":			
			ret =  (int)(36.43+0.76*(double)minPts);
			break;
		case "Delay":
			ret =  (int)(47.69+0.079*(double)minPts);
			break;
		default:
			System.out.println("Default:");
			ret =  (int)(40.31+0.22*(double)minPts);
			break;
		}
		if (ret>400){
			System.out.println("Limited from: "+ret);
			ret = 400;
		}
		return ret;
	}



	public static int getMinPts(Fog f) {
		//System.out.println(f.getScenario());
		switch (f.getScenario()) {
		case "Delay":
			return 18;
		case "Multi":
			return 15;
		case "Capab":
			return 5;
		default:
			System.out.println("Default:");
			return 12;
		}
	}



	public static void ExhaustiveClusStuff(Fog f){
		System.out.println("----- Clust GA Stuff -----");
		Genetic g = new Genetic(f);
		//DisplayData(f);
		for (Integer i: f.getClusters().keySet()){
			f.getClusters().get(i).setDeployment(g.ExhaustiveCluster(f.getClusters().get(i)));
		}
		f.deployClusters();
		System.out.println("Fog Utility: "+f.getFogCompoundUtility());
		System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());
	}
	
	
	public static boolean Clustering(Fog f,float eps, int minPts){
		System.out.println("----- Clustering -----");
		f.clearGwClustConns();
		f.removeClusters();
		f.clearAppToGws();
		Clustering cls = new Clustering(f);
		List<Set<Integer>> tmp = cls.DBScan(eps,minPts);//eps, minPts
		if (tmp.size()<1){
			System.out.println("Clustering Failed - No CLusters");
			return false;
		}else{
			f.createClusters(tmp);
		}
		for (Integer i : f.getClusters().keySet()) {
			System.out.println("Cluster "+i+" Apps: "+f.getClusters().get(i).getApps().keySet());
		}
		if(f.checkIfAppsAllocated().size()!=0 || (f.getClusters().size()<=1 && f.getApps().size()>40)){
			System.out.println("Clustering Failed - Alloc ");
			return false;
		}else{
			return true;
		}
		
	}
	
	public static boolean AdvClustering(Fog f,float eps, int minPts){
		//System.out.println("----- Clustering -----");
		f.clearGwClustConns();
		f.removeClusters();
		AdvancedCls cls = new AdvancedCls(f);
		f.createClusters(cls.DBScan(eps,minPts));//eps, minPts
		for (Integer i : f.getClusters().keySet()) {
			System.out.println("Cluster "+i+" Apps: "+f.getClusters().get(i).getApps().keySet());
		}
		//System.out.println(f.checkIfAppsAllocated());
		//System.out.println(f.getClusters().size());
		if(f.checkIfAppsAllocated().size()!=0 || f.getClusters().size()<=1){
			return false;
		}else{
			return true;
		}
		
	}
	

	public static boolean WeightedClustering(Fog f, WeightedCls cls,float eps, int minPts,int maxPts) {
		System.out.println("----- Clustering -----");
		f.clearGwClustConns();
		f.removeClusters();
		f.createClusters(cls.DBScan(eps,minPts,maxPts));//eps, minPts
		for (Integer i : f.getClusters().keySet()) {
			System.out.println("Cluster "+i+" Apps: "+f.getClusters().get(i).getApps().keySet());
		}
		System.out.println("Unallocated Apps: "+f.checkIfAppsAllocated());
		if(f.checkIfAppsAllocated().size()!=0 || f.getClusters().size()<=1){
			System.out.println("Clustering Failed!");
			return false;
		}else{
			return true;
		}
		
	}
	
	public static boolean WeightedClustering(Fog f, WeightedCls cls, int minPts) {
		System.out.println("-> Clustering");
		f.clearGwClustConns();
		f.removeClusters();
		f.clearAppToGws();
		f.createClusters(cls.DBScan(minPts));//eps, minPts
		for (Integer i : f.getClusters().keySet()) {
			System.out.println("Cluster "+i+" Size:"+f.getClusters().get(i).getApps().size()+" Apps: "+f.getClusters().get(i).getApps().keySet());
		}
		System.out.println("Unallocated Apps: "+f.checkIfAppsAllocated());
		if(f.checkIfAppsAllocated().size()!=0 || f.getClusters().size()==0 || (f.getClusters().size()<=1 && f.getApps().size()>getMinPts(f)*4)){
			System.out.println("Clustering Failed!");
			return false;
		}else{
			return true;
		}
		
	}
	
	public static boolean RandomClustering(Fog f, WeightedCls cls, int minPts,int maxPts) {
		System.out.println("-> Clustering");
		f.clearGwClustConns();
		f.removeClusters();
		f.clearAppToGws();
		f.createClusters(cls.DBScan((float)0.0,minPts,maxPts));//eps, minPts
		for (Integer i : f.getClusters().keySet()) {
			System.out.println("Cluster "+i+" Apps: "+f.getClusters().get(i).getApps().keySet());
		}
		System.out.println("Unallocated Apps: "+f.checkIfAppsAllocated());
		if(f.checkIfAppsAllocated().size()!=0 || f.getClusters().size()==0){
			System.out.println("Clustering Failed!");
			return false;
		}else{
			return true;
		}
		
	}
	
	public static List<Map<Integer, Integer>> RandomDeployment(Fog f){
		System.out.println("-----------------------------");
		System.out.println("----- B(0). Random Deployment -----");
		System.out.println("-----------------------------");
		Genetic g = new Genetic(f);
		int minPts = getMinPts(f);
		//Res Share
		int maxShare = 2;
		double shareThreshold = (float) 0.3;
		//GA
		int size = getMinPtsSize(minPts,f.getScenario());
		int count = getMinPtsCount(minPts,f.getScenario());
		
		long start=System.currentTimeMillis();
		dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
		dataG.addUtility((float)0.0);
		f.clearGwClustConns();
		f.removeClusters();
		WeightedCls cls = new WeightedCls(f);
		
		RandomClustering(f, cls,minPts,(int)((int)minPts*1.5));
		dataG.setBestCluster(f.retreiveCluster());
		dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
		dataG.addUtility((float)0.0);
		
		weightedResourceAlloc(f, cls, maxShare, shareThreshold);
		dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
		dataG.addUtility((float)0.0);
		List<Map<Integer, Integer>> ret = Methods.GAClus(f, true,dataG);
		if (ret!=null){
			dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);	
			f.setDeplpyment(ret.get(0));
			f.deployFog();
			dataG.addUtility(f.getFogCompoundUtility());
			float tot_sec=(System.currentTimeMillis()-start)/(float)1000;
			System.out.println("Finished Clusering Part in:"+tot_sec);
			System.out.println("Unalocated Apps: "+f.checkIfAppsAllocated());
			System.out.println("Total Elapsed Time:"+ (System.currentTimeMillis()-start)/1000.0);
			return ret;
		}else{
			System.out.println("Distance Clustering Failed");
			return null;
		}
	}
	
	/* Section for the weighted/CorrelationBased/Clustering GA
	 * This hold all the methods we need
	 */
	public static void weightedDistanceClusteringOptimization(Fog f){
		
		//Init
		WeightedCls cls = new WeightedCls(f);
		Map<Integer,Integer> bestSolution = new HashMap<>();
		Double bestUtil = 0.0;
		cls.initTrain(10,2);
		Map<String,Float> prog = new HashMap<String,Float>();
		boolean nextStep = true;
		//Random Population Initialization using Initial Weights
		List<Map<Integer, Integer>> bests = randomClustGA(f,cls,60,30);
		//List<Map<Integer, Integer>> bests = GAGlobal(f, 60, 50, true);
		Map<String,Double> corrApp = cls.Correlation("Deployment",cls.allAppSimilarities(bests));
		Map<String,Double> corrGw = cls.Correlation("Deployment",cls.allGwSimilarities(bests));
		if (bests == null){ System.out.println("Initial Random Clustering Failed!");return;}
		prog.put("Init",getUtility(f,bests.get(0)));
		bestUtil = getUtility(f,bests.get(0)).doubleValue();
		bestSolution = bests.get(0);
		cls.getWeight().attemptResult(getUtility(f,bests.get(0)));
		//Loop here while Weighting Algorithm knows what to do next
		while (cls.getWeight().getNextStep()){
			long start = System.currentTimeMillis();
			//Put values to the new weights Calculation
			weightsCorrBasedTraining(cls,bests);
			System.out.println("---------- New iter for Opt Started with: "+cls.getWeight().getChar()+" ----------");
			//Try Clustering based on given weights If all eps failes then weights fail
			if (WeightedClustering(f, cls,5)) {	
				weightedResourceAlloc(f, cls, 2, 0.3);
				displayClsAndRes(f);
				//Do weighted Resource Allocation based algorithm
				//Do Local GA, if Any fail then the method fails 
				List<Map<Integer, Integer>> tmpbests = Methods.GAClus(f, true,dataG);
				if (tmpbests == null){
						System.out.println("Direction Clustering Failed!");
						cls.getWeight().setGwFailed();
					}else{
						System.out.println("Direction Clustering Done in :"+((System.currentTimeMillis()-start)/1000.0));
						bests=tmpbests;
						prog.put("Clust["+cls.getWeight().getChar()+"]",getUtility(f,bests.get(0)));
						if (getUtility(f,bests.get(0))>bestUtil){
							bestUtil = getUtility(f,bests.get(0)).doubleValue();
							bestSolution = bests.get(0);
						}
						cls.getWeight().attemptResult(getUtility(f,bests.get(0)));
					}
			}else{
				System.out.println("Direction Clustering Failed!");
				cls.getWeight().setAppFailed();
			}
		}
		System.out.println("Results: ");
		for (String name: prog.keySet()){
			System.out.println(name+ " = "+ prog.get(name));
		}
		//ExtraTODO: If anything failes give me full info, have a function for that 
		
	}
	
	public static List<Map<Integer, Integer>> SampleWeDiCOptimization(Fog f) {
		//Init
		System.out.println("------------------------------------------------------------");
		System.out.println("----- C. Sample Weighted Distance Clustering Optimization -----");
		System.out.println("------------------------------------------------------------");
		f.clearAppToGws();
		//Sampling
		float sampleProc = (float)0.2;
		int minSampleSize = getMinPts(f)*3;
		//Clustering 
		int minPts = getMinPts(f);
		//Res Share
		int maxShare = 2;
		double shareThreshold = (float) 0.3;
		//GA
		int size = getMinPtsSize(minPts,f.getScenario());
		int count = getMinPtsCount(minPts,f.getScenario());
		//Time start
		long startIni = System.currentTimeMillis();
		dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
		dataG.addUtility((float)0.0);
		dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
		dataG.addUtility((float)0.0);
		WeightedCls cls = new WeightedCls(f);
		Map<Integer,Integer> bestSolution = new HashMap<>();
		Double bestUtil = 0.0;
		cls.initTrain(10,2);
		//Random Population Initialization using Initial Weights
		//Create Random Cluster and Optimize that, it needs to have a certain size 
		//List<Map<Integer, Integer>> bests = sampleClustGA(f,cls,60,50,0.1);
		List<Map<Integer, Integer>> bests = iterSampleClustGA(f,cls,getMinPtsCount((int)sampleProc*f.getApps().size(),f.getScenario()),getMinPtsSize((int)sampleProc*f.getApps().size(),f.getScenario()),sampleProc,minSampleSize);
		//List<Map<Integer, Integer>> bests = randomClustGA(f,cls,60,30);
		//List<Map<Integer, Integer>> bests = GAGlobal(f, 60, 50, true);
		dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
		dataG.addUtility((float)0.0);
		if (bests == null){ System.out.println("Initial Random Clustering Failed!");return null;}
		//prog.put("Init",getUtility(f,bests.get(0)));
		bestUtil = getPartialUtility(f,bests.get(0)).doubleValue();
		bestSolution = bests.get(0);
		System.out.println("Sampling Best Util:"+bestUtil+" with solution: "+bestSolution);
		cls.getWeight().attemptResult(bestUtil.floatValue());
		System.out.println("Sampling Finished in :"+((System.currentTimeMillis()-startIni)/1000.0));
		
		dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
		dataG.addUtility((float)0.0);
		//Iterative Solution
		Map<Integer, Integer> best = IterWeDiCompOptimization(f, cls, bests, minPts, maxShare, shareThreshold, bestUtil, bestSolution,startIni);
		//Final Write Out
		System.out.println("Method Finished in :"+((System.currentTimeMillis()-startIni)/1000.0));
		if (best==null){
			return null;
		}
		List<Map<Integer, Integer>> ret = new LinkedList<>();
		ret.add(best);
		return ret;
	}

	public static List<Map<Integer, Integer>> InitWeDiCOptimization(Fog f) {
		//TODO Part
		System.out.println("------------------------------------------------------------------------");
		System.out.println("----- D. Initial Weights Weighted Distance Clustering Optimization -----");
		System.out.println("------------------------------------------------------------------------");
		f.clearAppToGws();
		//Sampling
		float sampleProc = (float)0.2;
		int minSampleSize = 10;
		//Clustering 
		int minPts = getMinPts(f);
		//Res Share
		int maxShare = 2;
		double shareThreshold = (float) 0.3;
		//GA
		int size = getMinPtsSize(minPts,f.getScenario());
		int count = getMinPtsCount(minPts,f.getScenario());
		//Time start
		long startIni = System.currentTimeMillis();
		WeightedCls cls = new WeightedCls(f);
		cls.resetGwWeights(1.0);
		cls.resetAppWeights(1.0);
		cls.initTrain(10,2);
		//Map<String,Double> corrApp = new HashMap<>();
		//Map<String,Double> corrGw = new HashMap<>();
		//corrApp.put("Constraints",0.1);corrApp.put("RequirementSim",0.1);corrApp.put("ResourceShare",0.1);corrApp.put("MessageRate",0.1);corrApp.put("UtilityWeights",0.1);corrApp.put("UnitLoad",0.1);corrApp.put("Distance",0.1);
		//corrGw.put("Capabilities",0.1);corrGw.put("SharedRes",0.1);corrGw.put("PerfToULoad",0.1);corrGw.put("BaseLoad",0.1);corrGw.put("CapToULoad",0.1);
		//cls.getWeight().correlationResults(corrApp,corrGw);
		Map<Integer,Integer> bestSolution = new HashMap<>();
		Double bestUtil = 0.0;
		dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
		dataG.addUtility((float)0.0);
		//Iterative Solution
		Map<Integer, Integer> best = IterWeDiCompOptimization(f, cls, null, minPts, maxShare, shareThreshold, bestUtil, bestSolution,startIni);
		
		//Final Write Out
		System.out.println("Method Finished in :"+((System.currentTimeMillis()-startIni)/1000.0));
		if (best==null){
			return null;
		}
		List<Map<Integer, Integer>> ret = new LinkedList<>();
		ret.add(best);
		return ret;
		
	}
	
	public static Map<Integer, Integer> IterWeDiCompOptimization(Fog f, WeightedCls cls,
			List<Map<Integer, Integer>> bests_init, int minPts, int maxShare, double shareThreshold,
			Double bestUtil, Map<Integer, Integer> bestSolution,long startIni) {
		Map<String, Float> prog = new HashMap<String, Float>();
		boolean nextStep = true;
		List<Map<Integer, Integer>> bests = null;
		//cls.getWeight().showData();
		// Loop here while Weighting Algorithm knows what to do next
		while (cls.getWeight().getNextStep()) {
			long start = System.currentTimeMillis();
			dataG.addUtility(bestUtil.floatValue());
			dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);

			// Put values to the new weights Calculation
			System.out.println("-------------------------------------------");
			System.out.println("New Iteration of Training Algorithm Started");
			System.out.println("-------------------------------------------");
			System.out.println("With Fog:"+f.toString());
			if (bests == null){
				weightsCorrBasedTraining(cls, bests_init);

			}else{
				weightsCorrBasedTraining(cls, bests);
			}
			dataG.addWeightApp(cls.getWeight().appWeights());
			dataG.addWeightGw(cls.getWeight().gwWeights());
			System.out.println("Clustering Parameters: " + cls.getWeight().getChar() + " ----------");
			cls.getWeight().showWeights();
			// Try Clustering based on given weights If all eps failes then
			// weights fail
			if (WeightedClustering(f, cls, minPts)) {
				dataG.addUtility(bestUtil.floatValue());
				dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
				weightedResourceAlloc(f, cls, maxShare, shareThreshold);
				dataG.addUtility(bestUtil.floatValue());
				dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
				// displayClsAndRes(f);
				// Do weighted Resource Allocation based algorithm
				// Do Local GA, if Any fail then the method fails
				List<Map<Integer, Integer>> tmpbests = Methods.GAClus(f, true,dataG);
				if (tmpbests == null) {
					System.out.println("Direction Clustering Failed!");
					cls.getWeight().setGwFailed();
					dataG.addUtility(bestUtil.floatValue());
					dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
				} else {
					System.out.println(
							"Direction Clustering Done in :" + ((System.currentTimeMillis() - start) / 1000.0));
					if (bests == null){
						bests = tmpbests;
					}
					prog.put(
							"Clust[" + cls.getWeight().getChar() + " Time: "
									+ ((System.currentTimeMillis() - start) / 1000.0) + "]",
							getUtility(f, bests.get(0)));
					if (getUtility(f, bests.get(0)) > bestUtil) {
						bestUtil = getUtility(f, bests.get(0)).doubleValue();
						bestSolution = bests.get(0);
						bests = tmpbests;
						dataG.setBestWeight(dataG.getCurrent());
						dataG.setBestCluster(f.retreiveCluster());
					}
					dataG.addUtility(bestUtil.floatValue());
					dataG.addTime((float)(System.currentTimeMillis()-startIni)/(float)1000.0);
					cls.getWeight().attemptResult(getUtility(f, bests.get(0)));
				}
			} else {
				System.out.println("Direction Clustering Failed!");
				cls.getWeight().setAppFailed();
			}
			//cls.getWeight().showData();
		}
		System.out.println("Results: ");
		SortedSet<String> intKeys = new TreeSet<>(prog.keySet());
		for (String name : intKeys) {
			System.out.println(name + " = " + prog.get(name));
		}
		if (bests !=null)
			{return bests.get(0);}
		else
			{return null;}
	}


	private static void weightsCorrBasedTraining(WeightedCls cls,List<Map<Integer, Integer>> bests) {
		//TODO: Fix to actually have a training alogrithm
		//Parameter Correlation Calculation based on Distance Clustering
		if (bests==null){
			cls.getWeight().correlationResults(new HashMap<>(),new HashMap<>());
		}else{
			Map<String,Double> corrApp = cls.Correlation("Deployment",cls.allAppSimilarities(bests));
			Map<String,Double> corrGw = cls.Correlation("Deployment",cls.allGwSimilarities(bests));
			//SetCorrelation
			System.out.println("Apps Correlations: "+corrApp);
			System.out.println("Gws Correlations: "+corrGw);
			cls.getWeight().correlationResults(corrApp,corrGw);			
		}
		cls.setCorrelation(cls.getWeight().appWeights(),cls.getWeight().gwWeights());
		//cls.setCorrelation(corrApp,corrGw,0.3);
	}



	private static List<Map<Integer, Integer>> randomClustGA(Fog f, WeightedCls cls,int size, int cnt) {
		long start = System.currentTimeMillis();
		if (RandomClustering(f, cls,5,15)) {	
			weightedResourceAlloc(f, cls, 3, 0.05);
			displayClsAndRes(f);
			List<Map<Integer, Integer>> tmp = Methods.GAClus(f, true,dataG);
			System.out.println("Random Clust finished in :"+((System.currentTimeMillis()-start)/1000.0));
			return tmp;
		}else{
			return null;
		}
	}
	
	private static List<Map<Integer, Integer>> sampleClustGA(Fog f, WeightedCls cls, int count, int size, double proc) {
		// TODO Auto-generated method stub
		//Gnerate new Cluster Randomply Random select Apps, the size will be of 
		long start = System.currentTimeMillis();
		System.out.println("-> SampleClustering");
		List<Integer> apps = new ArrayList<>();
		List<Integer> totApps = new ArrayList<Integer>(f.getApps().keySet());
		while (apps.size()<proc*totApps.size()){
			//Get Random app add it to list
			Collections.shuffle(totApps);
				apps.add(totApps.get(0));
			
		}
		f.clearGwClustConns();
		f.removeClusters();
		List<Set<Integer>> tmpList= new ArrayList<>();
		tmpList.add(new HashSet<Integer>(apps));
		f.createClusters(tmpList);//eps, minPts
		cls.resetGwWeights(1.0);
		cls.resetAppWeights(1.0);
		sampleResourceAlloc(f,cls);
		//displayClsAndRes(f);
		List<Map<Integer, Integer>> tmp = Methods.sampGAClus(f, true);
		cls.clearWeights();
		System.out.println("Sample Clustering finished in :"+((System.currentTimeMillis()-start)/1000.0));
		return tmp;
	}
	
	private static List<Map<Integer, Integer>> iterSampleClustGA(Fog f, WeightedCls cls, int count, int size, double proc,int minSize) {
		// TODO Auto-generated method stub
		//Gnerate new Cluster Randomply Random select Apps, the size will be of 
		long start = System.currentTimeMillis();
		System.out.println("-> SampleClustering");
		WayFinder wf = new WayFinder(f,cls,proc,minSize);
		//List<Map<Integer, Integer>> tmp = wf.singleShot();
		List<Map<Integer, Integer>> tmp = wf.sampleFogAttempt();
		System.out.println("Sample Clustering finished in :"+((System.currentTimeMillis()-start)/1000.0));
		return tmp;
	}

	//End of Weighted Part

	public static void IterativeCorrelationClustering(Fog f,Integer iter) {
		long start = System.currentTimeMillis();
		WeightedCls cls = new WeightedCls(f);
		Map<String,Float> prog = new HashMap<String,Float>();
		List<Map<Integer, Integer>> bests = Methods.GAGlobal(f);
		prog.put("Global",getUtility(f,bests.get(0)));
		if (bests==null){		
			//displayClsAndRes(f);
			System.out.println("Failed Global GA");
		}
		System.out.println("GlobalGA Time Elapsed: " + (System.currentTimeMillis() - start) / (float) 1000);
		Map<String,Double> corrApp = cls.Correlation("Deployment",cls.allAppSimilarities(bests));
		Map<String,Double> corrGw = cls.Correlation("Deployment",cls.allGwSimilarities(bests));
		System.out.println("Apps Correlations: "+corrApp);
		System.out.println("Gws Correlations: "+corrGw);
		start = System.currentTimeMillis();
		//Map<String,Double> corrApp = new HashMap<String,Double>();
		//Map<String,Double> corrGw = new HashMap<String,Double>();
		//List<Map<Integer, Integer>> bests = new ArrayList<>();
		if (RandomClustering(f, cls,5,20)) {	
			weightedResourceAlloc(f, cls, 3, 0.05);
			// displayClsAndRes(f);
			bests = Methods.GAClus(f, true,dataG);
			System.out.println("Rand Clust Elapsed: " + (System.currentTimeMillis() - start) / (float) 1000);
			corrApp = cls.Correlation("Deployment", cls.allAppSimilarities(bests));
			corrGw = cls.Correlation("Deployment", cls.allGwSimilarities(bests));
			System.out.println("Apps Correlations: "+corrApp);
			System.out.println("Gws Correlations: "+corrGw);
			cls.setCorrelation(corrApp,corrGw,0.3);
		}else{
			System.out.println("Not even Ranomd FAIL!");
		}
		for (int i = 0; i < iter; i++) {
			long start2 = System.currentTimeMillis();
			System.out.println("----- Training Iteration nr. "+i+" -----");
			cls.setCorrelation(corrApp,corrGw,0.3);
			if (WeightedClustering(f, cls,5)) {	
				weightedResourceAlloc(f, cls, 3, 0.05);
				// displayClsAndRes(f);
				bests = Methods.GAClus(f, true,dataG);
				System.out.println("Clustering Time: " + (System.currentTimeMillis() - start2) / (float) 1000);
				if (bests == null) {
					System.out.println("Failed Clustering GA");
				} else {
					prog.put("Clust["+i+"]",getUtility(f,bests.get(0)));
					corrApp = cls.Correlation("Deployment", cls.allAppSimilarities(bests));
					corrGw = cls.Correlation("Deployment", cls.allGwSimilarities(bests));
					System.out.println("Apps Correlations: "+corrApp);
					System.out.println("Gws Correlations: "+corrGw);
				}
			} else{
					System.out.println("Failed Everything");
				}
		}
		System.out.println("Clustering Time Elapsed: " + (System.currentTimeMillis() - start) / (float) 1000);
		System.out.println("Props: "+prog);
	}
	
	public static boolean CorrelationClusterin(Fog f){
		long start = System.currentTimeMillis();
		WeightedCls cls = new WeightedCls(f);
		List<Map<Integer, Integer>> bests = Methods.GAGlobal(f);
		/*if (bests==null){		
			displayClsAndRes(f);
			System.out.println("Failed Global GA");
			return false;
		}
		System.out.println("GlobalGA Time Elapsed: " + (System.currentTimeMillis() - start) / (float) 1000);
		Map<String,Double> corrApp = cls.Correlation("Deployment",cls.allAppSimilarities(bests));
		Map<String,Double> corrGw = cls.Correlation("Deployment",cls.allGwSimilarities(bests));
		System.out.println("Apps Correlations: "+corrApp);
		System.out.println("Gws Correlations: "+corrGw);
		cls.setCorrelation(corrApp,corrGw,0.05,0.1);*/
		if (WeightedClustering(f, cls,(float)0.9, 6,12)){
			weightedResourceAlloc(f,cls,3,0.1);
			//displayClsAndRes(f);
			System.out.println("Clustering and Alloc Time: " + (System.currentTimeMillis() - start) / (float) 1000);
			if (Methods.GAClus(f, true,dataG)==null){
				System.out.println("Failed Clustering GA");
				return false;
			}
		}else{
			System.out.println("Failed Clustering");
			return false;
		}
		System.out.println("Clustering Time Elapsed: " + (System.currentTimeMillis() - start) / (float) 1000);
		return true;
	}
	
	public static void ResourceAllocation(Fog f){
		f.clearAppToGws();
		f.clearGwClustConns();
		f.distributeGw2Cluster();	
	}
	
	public static void nandorsAlphaResourceAlloc(Fog f){
		f.clearAppToGws();
		AdvancedCls cls = new AdvancedCls(f);
		cls.distributeGw2Cluster();
		//displayClsAndRes(f);
		cls.resolveAnomalies();
		displayClsAndRes(f);
	}
	
	public static void weightedResourceAlloc(Fog f, WeightedCls cls,int maxShare,double shareThreshold) {
		f.clearGwClustConns();
		f.clearAppToGws();
		cls.distributeGw2Cluster(maxShare,shareThreshold);
		//displayClsAndRes(f);
		cls.resolveApptoGwNoise();
		//displayClsAndRes(f);
	}
	
	public static void sampleResourceAlloc(Fog f, WeightedCls cls) {
		f.clearAppToGws();
		cls.sampleGw2Cluster();
	}
	
	public static void displayClsAndRes(Fog f){
		System.out.println("-> Cluster Share Apps and Load-----");
		System.out.println("Total Load on System: " + f.getTotalLoad());
		System.out.println("Total Free Space on System: " + f.getTotalFreeCapacity());
		for (Integer i : f.getClusters().keySet()) {
			System.out.print("Cluster "+i+" Load: "+f.getClusters().get(i).getClusterLoad()+" AllocatedRes: "+f.getClusters().get(i).getTotResShare()+" Rate: "+f.getClusters().get(i).ShareRate());
			System.out.println(" Apps: "+f.getClusters().get(i).getApps().keySet()+" Gateways: "+f.getClusters().get(i).getGateways().keySet());
			System.out.print("Gw Shares: ");
			for (Integer g : f.getClusters().get(i).getGateways().keySet()){
				System.out.print(" | " + String.format("%.1f",f.getGateways().get(g).getClusterShare(i)));
			}
			System.out.println();
		}
		for (Integer g:f.getGateways().keySet()){
			System.out.print("Gateway "+g+" Shares: ");
			Float tot = (float)0.0;
			tot+=f.getGateways().get(g).getGwBaseLoad();
			System.out.print( String.format("%.1f",f.getGateways().get(g).getGwBaseLoad())+" |");
			for (Integer c:f.getGateways().get(g).getCluster().keySet()){
				System.out.print("| " + String.format("%.1f",f.getGateways().get(g).getClusterShare(c))+" ");
				tot+=f.getGateways().get(g).getClusterShare(c);
			}
			System.out.println(" || "+tot);
		}
		System.out.println("Unallocated Apps:"+f.checkIfAppsAllocated());
		System.out.println("OverAllocatedApps:"+f.checkIfAppsOverAlloc());
		System.out.println("Cumulative share Rate:"+f.cumClustShare());
	}



	public static Fog InitDelayFog(int appCnt) {
		Fog f = new Fog("Delay Fog");
		f.resetCounts();
		f.setScenario("Delay");
		float cloudGWRatio = (float) 0.1;
		float[] lat = {(float)8.97,(float)30.897};
		float[] lat2 = {(float)37.37,(float)87.89};
		float[] lat3 = {(float)2.37,(float)6.89};
		f.generateNewFogV2(appCnt,(float)30,(float)60,(float)0.2,(float)0.08,lat,cloudGWRatio,lat2,lat3,7,1,"Delay");
		//Analysis part, of distributing Gw's to clusters		
		return f;
	}



	public static Fog InitMultiFog(int appCnt) {
		Fog f = new Fog("Multi Fog");
		f.resetCounts();
		f.setScenario("Multi");
		float cloudGWRatio = (float) 0.1;
		float[] lat = {(float)8.97,(float)30.897};
		float[] lat2 = {(float)37.37,(float)87.89};
		float[] lat3 = {(float)2.37,(float)6.89};
		f.generateNewFogV2(appCnt,(float)30,(float)60,(float)0.2,(float)0.08,lat,cloudGWRatio,lat2,lat3,7,1,"Multi");
		//Analysis part, of distributing Gw's to clusters		
		return f;
	}



	public static Fog InitReqFog(int appCnt) {
		Fog f = new Fog("Requirement Fog");
		f.resetCounts();
		f.setScenario("Capab");
		float cloudGWRatio = (float) 0.1;
		float[] lat = {(float)8.97,(float)30.897};
		float[] lat2 = {(float)37.37,(float)87.89};
		float[] lat3 = {(float)2.37,(float)6.89};
		f.generateNewFogV2(appCnt,(float)30,(float)60,(float)0.2,(float)0.08,lat,cloudGWRatio,lat2,lat3,4,1,"Capab");
		//Analysis part, of distributing Gw's to clusters		
		return f;
	}

	public static void GAPopSizeEvaluation(int testType){
		//Generation Size
		int size = 5;
		int iStep = 16;
		
		//Run Count
		int kSize = 4;
		
		//Fog Size
		int testSize = 6;
		int startSize = 10;
		int endSize = 60;
		if (testType==3){
			startSize = 5;
			endSize = 25;			
		}
		int multi = (endSize-startSize)/testSize;
		Map<Integer,Map<Integer,Float>> results = new HashMap<>();
		System.out.println("Starting Tests, Total Nr of Tests: "+(size*kSize+1)*(testSize+1));
		//Test
		for (int j = 0;j<=testSize;j++){
			//Innit
			Map<Integer,Float> res = new HashMap<>();
			for (int i=1;i<=size;i++){
				res.put(i*iStep,(float)0.0);
			}
			//Generate Fog
			//Fog f = Methods.InitDelayFog(startSize+j*multi);
			Fog f = new Fog("Test");
			float best =(float) 0.0;
			while (best<1.0){
				switch (testType) {
				case 1:
					f = Methods.InitDelayFog(startSize+j*multi);
					break;
				case 2:
					f = Methods.InitMultiFog(startSize+j*multi);
					break;
				case 3:
					f = Methods.InitReqFog(startSize+j*multi);
					break;
				default:
					System.out.println("Default Init");
					f = Methods.InitDelayFog(startSize+j*multi);
					break;
				}
				best = Methods.GAGlobal(f,100,1000);
			}
			int progress = j*(size*kSize+1)+1;
			System.out.println("Progress: "+progress+"/"+(size*kSize+1)*(testSize+1));
			for (int k=0;k<kSize;k++){
				for (int i=1; i<=size; i++){
					int fail = 0;
					float put = (float)0.0;
					while (put<1.0  && fail<=5){
						put = Methods.GAGlobal(f,i*iStep,best)/(float)kSize;
						fail++;
					}
					if (put<1.0){
						System.out.println("GA Max Failed!");
						System.out.println("Progress: "+progress+"/"+(size*kSize+1)*(testSize+1));
					}else{
						progress = j*(size*kSize+1)+k*size+i+1;
						System.out.println("Progress: "+progress+"/"+(size*kSize+1)*(testSize+1));
						put+=res.get(i*iStep);
						res.put(i*iStep,put);
					}
				}
			}
			System.out.print("------------->Results:");
			System.out.println(res);
			results.put(startSize+j*multi, res);
		}
		System.out.print("------------->Final Results:");
		System.out.println(results);
		matlabPrint(results);
	}
	
	public static void GAStopCondEvaluation(int testType) {
		//Generation Size
		int size = 2;
		int iStep = 16;
		
		//Run Count
		int kSize = 4;
		
		//Fog Size
		int testSize = 4;
		int startSize = 10;
		int endSize = 80;
		if (testType==3){
			startSize = 5;
			endSize = 23;			
		}
		int multi = (endSize-startSize)/testSize;
		Map<Integer,Map<Integer,Float>> results = new HashMap<>();
		System.out.println("Starting Tests, Total Nr of Tests: "+(testSize+1));
		//Test
		for (int j = 0;j<=testSize;j++){
			//Generate Fog
			//Fog f = Methods.InitDelayFog(startSize+j*multi);
			Fog f = new Fog("Test");
			Map<Integer, Float> bests = null;
			while (bests==null){
				switch (testType) {
				case 1:
					f = Methods.InitDelayFog(startSize+j*multi);
					break;
				case 2:
					f = Methods.InitMultiFog(startSize+j*multi);
					break;
				case 3:
					f = Methods.InitReqFog(startSize+j*multi);
					break;
				default:
					System.out.println("Default Init");
					f = Methods.InitDelayFog(startSize+j*multi);
					break;
				}
				int GAsize = getMinPtsSize(startSize+j*multi,f.getScenario());
				bests = Methods.GAGlobalEndCnd(f,GAsize);
			}
			System.out.println("Bests: "+bests);
			results.put(startSize+j*multi, bests);
			System.out.println("Progress: "+(j+1)+"/"+(testSize+1));
		}
		matlabPrintV2(results);
		
	}

	public static void ClustSizeEvaluation(int testType) {
				
				//Run Count
				int kSize = 4;
				
				//Fog Size
				int testSize = 8;
				int startSize = 5;
				int endSize = 80;
				if (testType==3){
					startSize = 5;
					endSize = 30;			
				}
				int multi = (endSize-startSize)/testSize;
				Map<Integer,Float> results = new HashMap<>();
				System.out.println("Starting Tests, Total Nr of Tests: "+(testSize+1)*kSize);
				//Test
				for (int j = 0;j<=testSize;j++){
					//Generate Fog
					//Fog f = Methods.InitDelayFog(startSize+j*multi);
					Fog f = new Fog("Test");
					Float bests = (float)0.0;
					for (int i =0; i<kSize;i++){
						Float bUtil = null;
						int fails = 0;
						while (bUtil==null && fails < 5){
							fails++;
							switch (testType) {
							case 1:
								f = Methods.InitDelayFog(startSize+j*multi);
								break;
							case 2:
								f = Methods.InitMultiFog(startSize+j*multi);
								break;
							case 3:
								f = Methods.InitReqFog(startSize+j*multi);
								break;
							default:
								System.out.println("Default Init");
								f = Methods.InitDelayFog(startSize+j*multi);
								break;
							}
							int GAsize = getMinPtsSize(startSize+j*multi,f.getScenario());
							bUtil= Methods.GAGlobalSize(f,GAsize);
							System.out.println("Progress: "+(kSize*j+i+1)+"/"+(testSize+1)*kSize);
						}
						if (bUtil==null){
							System.out.println("Butil NULL!");
							bests+=(float)0.0;
						}else{
							System.out.println("Butil: "+bUtil/kSize);
							bests+=bUtil/kSize;
						}
					}
					System.out.println("Bests: "+bests);
					results.put(startSize+j*multi, bests);
				}
				matlabPrintV3(results);
		
	}
	

	public static void ComponentAnalysis(int caseType,int clustType,int allocType) {
		// TODO Auto-generated method stub
		System.out.println("Component Analysis.");
		Fog f = new Fog("Test");
		List<Map<Integer, Integer>> bests = null;
		Integer sizes1 = 320;
		Integer sizes2 = 80;
		int maxShare = 2;
		double shareThreshold = (float) 0.3;
		WeightedCls cls = new WeightedCls(f);
		int minPts =0;
		String text = "";
		switch (caseType) {
		case 1:
			f = Methods.InitDelayFog(sizes1);
			break;
		case 2:
			f = Methods.InitMultiFog(sizes1);
			break;
		case 3:
			f = Methods.InitReqFog(sizes2);
			break;
		default:
			System.out.println("Default Init");
			f = Methods.InitDelayFog(sizes1);
			break;
		}
		
		//Find weights if needed
		Map<String, Double> appW = new HashMap<>();
		Map<String, Double> gwW = new HashMap<>();
		if (clustType==4 || allocType==4){
			dataG.newDataSet("Init");
			bests = Methods.SampleWeDiCOptimization(f);
		}
		if (bests==null){
			System.out.println("Attempt Failed!");
			return;
		}
		bests = null; //Reset after test
		appW = dataG.getBestWeApps();
		gwW = dataG.getBestWeGws();
		System.out.println("Used Weights:");
		System.out.println(appW);
		System.out.println(gwW);
		double start = System.currentTimeMillis();
		//Select clust Type
		for (int i = 1; i <= 4; i++) {
			start = System.currentTimeMillis();
			if (clustType != 0) {i = clustType;} // Override just in case you need it :))
			switch (i) {
			case 1:
				//if (clustType != 0) {dataG.newDataSet("RandomC");}
				text = "RandomC";
				//Random
				System.out.println("--->Rand Clustering");
				minPts = getMinPts(f);
				cls = new WeightedCls(f);
				RandomClustering(f, cls,minPts,(int)((int)minPts*1.5));
				break;
			case 2:
				text = "DistC";
				System.out.println("--->Distance Clustering");
				int eps = 1;
				//int minPts = getMinPts(f);
				minPts = 8;
				while(!Clustering(f,eps,minPts)){
					minPts = minPts-1;
					if (minPts<3){
						break;
					}
				};
				break;
			case 3:
				System.out.println("--->AllWeights Clustering");
				text = "AllWeightsC";
				cls = new WeightedCls(f);
				cls.resetGwWeights(1.0);
				cls.resetAppWeights(1.0);
				cls.initTrain(10,2);
				// weights fail
				minPts = getMinPts(f);
				WeightedClustering(f, cls, minPts);
				break;
			case 4:
				text = "TrainedWeightsC";
				System.out.println("--->Trained Weights Clustering");
				cls = new WeightedCls(f);
				cls.setAppWeights(appW);
				cls.setGwWeights(gwW);
				cls.initTrain(10,2);
				minPts = getMinPts(f);
				WeightedClustering(f, cls, minPts);
				break;
			default:
				//if (clustType != 0) {dataG.newDataSet("DefaultsC");}
				//Add Clusteringbreak;
			}
		//Check if failed and if not excellent
		float middle = (float) ((System.currentTimeMillis()-start)/(float)1000.0);
		//Select clust Type
		for (int j = 1; j <= 4; j++) {
			if (allocType!= 0) {j = allocType;} // Override just in case you need it :))
			switch (j) {
			case 1:
				System.out.println("--->Rand Alloc");
				dataG.newDataSet(text+"RandomA");
				cls = new WeightedCls(f);
				weightedResourceAlloc(f, cls, maxShare, shareThreshold);
				break;
			case 2:
				dataG.newDataSet(text+"DistA");
				ResourceAllocation(f);
				System.out.println("--->Dist Alloc");
				break;
			case 3:
				dataG.newDataSet(text+"AllWeightsA");
				System.out.println("--->All Weights Alloc");
				cls = new WeightedCls(f);
				cls.resetGwWeights(1.0);
				cls.resetAppWeights(1.0);
				cls.initTrain(10,2);
				weightedResourceAlloc(f, cls, maxShare, shareThreshold);
				break;
			case 4:
				dataG.newDataSet(text+"TrainedWeightsA");
				System.out.println("--->Training Alloc");
				cls = new WeightedCls(f);
				cls.setAppWeights(appW);
				cls.setGwWeights(gwW);
				cls.initTrain(10,2);
				weightedResourceAlloc(f, cls, maxShare, shareThreshold);
				break;
			default:
				dataG.newDataSet(text+"DefaultsA");
				//Add Clusteringbreak;
			}
			//GA Part of optimization
			dataG.addTime(middle);
			dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);
			System.out.println("--->Clustering GAs");
			List<Map<Integer, Integer>> ret = Methods.GAClus(f, true,dataG);
			if (ret!=null){
				dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);	
				f.setDeplpyment(ret.get(0));
				f.deployFog();
				dataG.addUtility(f.getFogCompoundUtility());
			}else{
				dataG.addTime((float)(System.currentTimeMillis()-start)/(float)1000.0);	
				dataG.addUtility((float)0.0);
			}
			if (allocType != 0) {break;}
		}	
		if (clustType != 0) {break;}
	}	
		dataG.getAllData();
		if (clustType==0){
			dataG.printComponent(0);
		}else{
			dataG.printComponent(1);
		}
	}
	
	public static void WeightsAnalysis(int caseType,int choice) {
		System.out.println("Weights Analysis.");
		Fog f = new Fog("Test");
		List<Map<Integer, Integer>> bests = null;
		Integer sizes1 = 320;//320;
		Integer sizes2 = 120;//120;
		int maxShare = 2;
		double shareThreshold = (float) 0.3;
		WeightedCls cls = new WeightedCls(f);
		int minPts =0;
		String text = "";
		switch (caseType) {
		case 1:
			f = Methods.InitDelayFog(sizes1);
			break;
		case 2:
			f = Methods.InitMultiFog(sizes1);
			break;
		case 3:
			f = Methods.InitReqFog(sizes2);
			break;
		default:
			System.out.println("Default Init");
			f = Methods.InitDelayFog(sizes1);
			break;
		}
		if (choice == 2 || choice == 0){
			dataG.newDataSet("WeightsFull");
			Methods.InitWeDiCOptimization(f);
		}
		if (choice == 1 || choice == 0){
			dataG.newDataSet("Sample");
			Methods.SampleWeDiCOptimization(f);
		}
		dataG.getAllData();
		dataG.getWeights(f,choice);
	}
	
	
	public static JSONObject PerformanceAnalysis(int size, int testType, int single) {

		System.out.println("Starting Performance Test.");
		Fog f = new Fog("Test");
		List<Map<Integer, Integer>> bests = null;
		// dataG.setTestType("Perf");
		int fail = 0;
		List<Integer> sizes1 = new LinkedList<>();
		sizes1.add(20);
		sizes1.add(80);
		sizes1.add(320);
		sizes1.add(640);
		List<Integer> sizes2 = new LinkedList<>();
		sizes2.add(10);
		sizes2.add(20);
		sizes2.add(60);
		String type = "Not Specified";
		dataG.reset();
		fail = 0;
		int success = 0;
		while (success == 0) {
			switch (testType) {
			case 1:
				f = Methods.InitDelayFog(sizes1.get(size));
				break;
			case 2:
				f = Methods.InitMultiFog(sizes1.get(size));
				break;
			case 3:
				f = Methods.InitReqFog(sizes2.get(size));
				break;
			default:
				System.out.println("Default Init");
				f = Methods.InitDelayFog(size);
				break;
			}
			for (int i = 1; i <= 5; i++) {
				if (single != 0) {
					i = single;
				} // Override just in case you need it :))
				bests = null;
				switch (i) {
				case 1:
					fail = 0;
					while (bests == null & fail < 3) {
						fail++;
						dataG.newDataSet("GA");
						bests = Methods.GAGlobal(f);
					}
					break;
				case 2:
					fail = 0;
					while (bests == null & fail < 3) {
						fail++;
						dataG.newDataSet("Dist");
						bests = Methods.DistanceClusteringDeployment(f);
					}
					break;
				case 3:
					fail = 0;
					while (bests == null & fail < 3) {
						fail++;
						dataG.newDataSet("Sample");
						bests = Methods.SampleWeDiCOptimization(f);
					}
					break;
				case 4:
					fail = 0;
					while (bests == null & fail < 3) {
						fail++;
						dataG.newDataSet("Init");
						bests = Methods.InitWeDiCOptimization(f);
					}
					break;
				case 5:
					fail = 0;
					while (bests == null & fail < 3) {
						fail++;
						dataG.newDataSet("Random");
						bests = Methods.RandomDeployment(f);
					}
					break;
				default:
					dataG.newDataSet("GA");
					//bests = Methods.GAGlobal(f);
					break;
				}

				if (bests != null) {
					success++;
				}
				if (single != 0) {
					dataG.getSinglePerfResults(size, testType);
					return dataG.getSinglePerfResults_json(size, testType);
				} // Override just in case you need it :))
			}
		}
		if (single == 0) {
			dataG.getBestUtils();
			dataG.getPerfResults(size, testType);
			return dataG.getPerfResults_json(size, testType);

		}
		return null;
	}
	

	public static JSONObject ScalabilityAnalysis(int size, int count , int sceType, int meType) {
		
		List<Integer> sizes1 = new LinkedList<>();
		sizes1.add(20);sizes1.add(40);sizes1.add(80);sizes1.add(160);sizes1.add(320);sizes1.add(480);
		List<Integer> sizes2 = new LinkedList<>();
		sizes2.add(10);sizes2.add(20);sizes2.add(40);sizes2.add(80);sizes2.add(120);sizes2.add(180);
		
		System.out.println("Starting Scalability Test.");
		Fog f = new Fog("Test");
		for (int i = 1; i <= count; i++) {
			switch (sceType) {
			case 1:
				f = Methods.InitDelayFog(sizes1.get(size));
				break;
			case 2:
				f = Methods.InitMultiFog(sizes1.get(size));
				break;
			case 3:
				f = Methods.InitReqFog(sizes2.get(size));
				break;
			default:
				System.out.println("Default Init");
				f = Methods.InitDelayFog(size);
				break;
			}
			
			for (int j = 1; j <= 5; j++) {
				if (meType != 0) {j = meType;} // Override just in case you need it :))
				switch (j) {
				case 1:
					dataG.newDataSet("1-"+i);
					Methods.GAGlobal(f);
					break;
				case 2:
					dataG.newDataSet("2-"+i);
					Methods.DistanceClusteringDeployment(f);
					break;
				case 3:
					dataG.newDataSet("3-"+i);
					Methods.SampleWeDiCOptimization(f);
					break;
				case 4:
					dataG.newDataSet("4-"+i);
					Methods.InitWeDiCOptimization(f);
					break;
				case 5:
					dataG.newDataSet("5-"+i);
					Methods.RandomDeployment(f);
					break;
				default:
					dataG.newDataSet("1-"+i);
					Methods.GAGlobal(f);
					break;
				}
				if (meType != 0) {break;} // Override just in case you need it :))
			}
			System.out.println("Intermediate Results from iter "+i+"/"+count+":");
			dataG.getScaleResults(size,sceType);
			return dataG.getScaleResults_json(size,sceType);
		}
		System.out.println("Final Results:");
		dataG.getScaleResults(size,sceType);
		return dataG.getScaleResults_json(size,sceType);
	}
	
	public static void timeDistributionAnalysis() {
		List<Integer> sizes = new LinkedList<>();
		sizes.add(30);sizes.add(60);sizes.add(120);sizes.add(240);
		int size = 4;int i = 0;
		System.out.println("Starting Scalability Test.");
		Fog f = new Fog("Test");
		List<Map<Integer, Integer>> bests = null;
		while (bests==null || i<size) {
			bests=null;
			f = Methods.InitReqFog(sizes.get(i));
			dataG.newDataSet("K"+sizes.get(i));
			bests = Methods.SampleWeDiCOptimization(f);	
			if (bests!=null){
				i++;
			}
			System.out.println("Solution "+i+"/"+size+" Results: "+(bests!=null));
		}
		dataG.getDistResults();
	}

	public static void matlabPrint(Map<Integer,Map<Integer,Float>> results){
		SortedSet<Integer> keys = new TreeSet<>(results.keySet());
		
		//Fog Sizes
		System.out.print("rows = [ ");
		for (Integer key:keys){
			System.out.print(key+" ");
		}
		System.out.println("];");
		
		//Other Parameter
		System.out.print("columns = [ ");
		for (Integer key:keys){
			SortedSet<Integer> intKeys = new TreeSet<>(results.get(key).keySet());
			for (Integer iKey:intKeys){
				System.out.print(iKey+" ");
			}
			break;
		}
		System.out.println("];");
		
		System.out.print("data = [ ");
		for (Integer key:keys){
			SortedSet<Integer> intKeys = new TreeSet<>(results.get(key).keySet());
			for (Integer iKey:intKeys){
				System.out.print(results.get(key).get(iKey)+" ");
			}
			System.out.print("; ");
		}
		System.out.println("];");
	}

	public static void matlabPrintV2(Map<Integer,Map<Integer,Float>> results){
		SortedSet<Integer> keys = new TreeSet<>(results.keySet());
		System.out.print("labels = [ ");
		for (Integer key:keys){
			System.out.print(key+" ");
		}
		System.out.println("];");
		//Fog Sizes
		System.out.print("X = [ ");
		for (Integer key:keys){
			SortedSet<Integer> intKeys = new TreeSet<>(results.get(key).keySet());
			for (Integer ikey:intKeys){
				System.out.print(ikey+" ");
			}
			System.out.print("; ");
		}
		System.out.println("];");
		

		System.out.print("Y = [ ");
		for (Integer key:keys){
			SortedSet<Integer> intKeys = new TreeSet<>(results.get(key).keySet());
			for (Integer ikey:intKeys){
				System.out.print(results.get(key).get(ikey)+" ");
			}
			System.out.print("; ");
		}
		System.out.println("];");
	}
	
	public static void matlabPrintV3(Map<Integer,Float> results){
		SortedSet<Integer> keys = new TreeSet<>(results.keySet());

		System.out.print("X = [ ");
		for (Integer key:keys){
			System.out.print(key+" ");
		}
		System.out.println("];");
		
		System.out.print("Y = [ ");
		for (Integer key:keys){
			System.out.print(results.get(key)+" ");
		}
		System.out.println("];");
		
	}
	
	
	}
