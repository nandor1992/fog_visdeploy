package org.nandor.fog_deployer;

import java.util.*;

public class WeighTrainer {
		//Maybe Use Hooke-Jeeves Algorithm
		//https://www.siam.org/books/textbooks/fr18_book.pdf
		private int maxStep;
		private int failMax;
		private boolean exit = false;
		private boolean appFailed = false;
		private boolean gwFailed = false;
		private int failCnt = 0;
		private double procLim = 0.2; //Delete Later 
		private double appProcLim = 0.2;
		private double gwProcLim = 0.05;
		private double diffLim = 0.00001;
		private int bestIter = 0;
		private int prevBestIter = 0;
		private  List<Float>utils = new ArrayList<>();
		private Map<String,Double> appPenalties = new HashMap<>();
		private Map<String,Double> gwPenalties = new HashMap<>();
		private Double refPenalty = 0.2;
		private List<Map<String, Double>> appWeights = new ArrayList<>();
		private List<Map<String, Double>> gwWeights = new ArrayList<>();
		private List<Map<String, Double>> corrApp = new ArrayList<>();
		private List<Map<String, Double>> corrGw = new ArrayList<>();

	public  WeighTrainer(int maxStep,int failSteps){
		//Constructor for Class, might be empty, or some constants
		this.maxStep = maxStep;
		this.failMax=failSteps;
		appWeights.add(new HashMap<>());
		gwWeights.add(new HashMap<>());
		corrApp.add(new HashMap<>());
		corrGw.add(new HashMap<>());
	}
	
	public void correlationResults(Map<String, Double> corrApp, Map<String, Double> corrGw) {
		//Increase iter
		System.out.println("CorrResults: CorrApp:"+corrApp+" CorrGW:"+corrGw);	
		System.out.println("Corrs: CorrApp:"+this.corrApp+" CorrGW:"+this.corrGw);
		System.out.println(bestIter+1);
		//Add Data
		System.out.print(" -> Sorting Correlation Results");
		if (fullStopCriterion()){
			//System.out.print(" - Not Full Stop - ");
			//Everything Fine, keep Probing to see how deep this goes 
			this.corrApp.add(corrApp);
			this.corrGw.add(corrGw);
			if (dirStopCriterion()){
				System.out.println(" - Not Dir Stop - ");
				probe(corrApp, corrGw);
				appFailed=false;
				gwFailed=false;
			}else{
				System.out.print(" - Dir Stop - ");
				failCnt++;
				//utils.get(utils.size()-1)<(double)utils.get(bestIter) ||
				//Check if any the any of the clust or alloc Failed 
				if (appFailed==true){
					//Make App changes
					System.out.print(" - App Failed ");
					makeAppChanges(this.corrApp.get(bestIter+1));
					probe(this.corrApp.get(bestIter+1), this.corrGw.get(bestIter+1));
				}else if (gwFailed==true){
					//Make Gw Changes
					System.out.print(" - Gw Failed");
					makeGwChanges(this.corrGw.get(bestIter+1));
					probe(this.corrApp.get(bestIter+1), this.corrGw.get(bestIter+1));
				}else if ( bestIter==utils.size()-1){
					System.out.println(" - Better Util");
					//makeChanges(corrApp, corrGw);
					makeAppChanges(corrApp);
					makeGwChanges(corrGw);
					probe(corrApp, corrGw);
				}else{
					System.out.println(" - Worse Util");
					//makeChanges(this.corrApp.get(bestIter), this.corrGw.get(bestIter));
					makeAppChanges(this.corrApp.get(bestIter+1));
					makeGwChanges(this.corrGw.get(bestIter+1));
					probe(this.corrApp.get(bestIter+1), this.corrGw.get(bestIter+1));
				}
				appFailed=false;
				gwFailed=false;
			}
		}else{
			System.out.println();
			exit = true;
		}
	}
	
	//Make Changes
	private void makeChanges(Map<String, Double> corrApp, Map<String, Double> corrGw) {
		//System.out.println("Weights:");
		//System.out.println(appWeights);
		//System.out.println(gwWeights);
		if (verifyUnderfitting(appWeights.get(bestIter),gwWeights.get(bestIter),corrApp.size(),corrGw.size())){
			//Increase procLim by *1+(1*failRate)
			//System.out.println("Underfitted Solution, Solving...");
			procLim=procLim*(1+(0.2*(double)failCnt));
		}else if (verifyOverFitiing(appWeights.get(bestIter),gwWeights.get(bestIter))){
			//Decrease procLim buy *1-(1*failRate)
			//System.out.println("Overfitted Solution, Solving...");
			procLim=procLim*(1-(0.2*(double)failCnt));
			//Add penalty for top weights of refPenalty*failRate
			modifyPenalties(appWeights.get(bestIter),gwWeights.get(bestIter),0.2*(double)failCnt,0.2*(double)failCnt);
		}else{
			//Consider the solution to stagnation as something that can be used generically
			//System.out.println("General Stagnation, Solving...");
			procLim=procLim*(1+(0.1*(double)failCnt));
			modifyRandPenalties(0.2*(double)failCnt,1.0*(double)failCnt);
		}
		//System.out.println("proc/Penalties:");
		//System.out.println(appPenalties+" : "+gwPenalties);
		//System.out.println(procLim);
	}

	private void makeAppChanges(Map<String, Double> corrApp) {
		//System.out.println("Weights:");
		//System.out.println(appWeights);
		//System.out.println(gwWeights);
		if (verifyAppUnderfitting(appWeights.get(bestIter),corrApp.size())){
			//Increase procLim by *1+(1*failRate)
			System.out.println("Underfitted App Solution, Solving...");
			appProcLim=appProcLim*(1+(0.2*(double)failCnt));
		}else if (verifyAppOverFitiing(appWeights.get(bestIter))){
			//Decrease procLim buy *1-(1*failRate)
			System.out.println("Overfitted App Solution, Solving...");
			appProcLim=appProcLim*(1-(0.2*(double)failCnt));
			//Add penalty for top weights of refPenalty*failRate
			modifyAppPenalties(appWeights.get(bestIter),0.2*(double)failCnt);
		}else{
			//Consider the solution to stagnation as something that can be used generically
			System.out.println("General App Stagnation, Solving...");
			appProcLim=appProcLim*(1+(0.1*(double)failCnt));
			modifyAppRandPenalties(0.2*(double)failCnt,1.0*(double)failCnt);
		}
		//System.out.println("proc/Penalties:");
		//System.out.println(appPenalties+" : "+gwPenalties);
		//System.out.println(procLim);
	}
	
	private void makeGwChanges(Map<String, Double> corrGw) {
		//System.out.println("Weights:");
		//System.out.println(appWeights);
		//System.out.println(gwWeights);
		if (verifyGwUnderfitting(gwWeights.get(bestIter),corrGw.size())){
			//Increase procLim by *1+(1*failRate)
			System.out.println("Underfitted Gw Solution, Solving...");
			gwProcLim=gwProcLim*(1+(0.2*(double)failCnt));
		}else if (verifyGwOverFitiing(gwWeights.get(bestIter))){
			//Decrease procLim buy *1-(1*failRate)
			System.out.println("Overfitted Gw Solution, Solving...");
			gwProcLim=gwProcLim*(1-(0.2*(double)failCnt));
			//Add penalty for top weights of refPenalty*failRate
			modifyGwPenalties(gwWeights.get(bestIter),0.5*(double)failCnt);
		}else{
			//Consider the solution to stagnation as something that can be used generically
			System.out.println("General Gw Stagnation, Solving...");
			gwProcLim=gwProcLim*(1+(0.1*(double)failCnt));
			modifyGwRandPenalties(0.2*(double)failCnt,1.0*(double)failCnt);
		}
		//System.out.println("proc/Penalties:");
		//System.out.println(appPenalties+" : "+gwPenalties);
		//System.out.println(procLim);
	}

	
	//Modify Penalties
	private void modifyPenalties(Map<String, Double> appWeights, Map<String, Double> gwWeights,Double appRem,Double gwRem) {
		Double appAvg = 0.0;
		Double gwAvg = 0.0;
		//App
		for (String name: appWeights.keySet()){
			appAvg+=appWeights.get(name);
		}
		appAvg=appAvg/(double)appWeights.size();
		//Gw
		for (String name: gwWeights.keySet()){
			gwAvg+=gwWeights.get(name);
		}
		gwAvg=gwAvg/(double)gwWeights.size();
		//System.out.println("----->Averages:");
		//System.out.println(appAvg);
		//System.out.println(gwAvg);
		//Penalties for those that exist
		for (String name: corrApp.get(corrApp.size()-1).keySet()){
			if (appWeights.containsKey(name)){
				if (appWeights.get(name)>appAvg){
					appPenalties.put(name,1-appRem);
				}else{
					appPenalties.put(name, 1+appRem);
				}
			}else{
				appPenalties.put(name, 1+appRem);
			}
		}
		for (String name: corrGw.get(corrGw.size()-1).keySet()){
			if (gwWeights.containsKey(name)){
				if (gwWeights.get(name)>=gwAvg){
					gwPenalties.put(name, 1-gwRem);
				}else{
					gwPenalties.put(name, 1+gwRem);
				}
			}else{
				gwPenalties.put(name, 1+gwRem);
			}
		}
	}
	
	private void modifyAppPenalties(Map<String, Double> appWeights,Double appRem) {
		Double appAvg = 0.0;
		//App
		for (String name: appWeights.keySet()){
			appAvg+=appWeights.get(name);
		}
		appAvg=appAvg/(double)appWeights.size();
		//System.out.println("----->Averages:");
		//System.out.println(appAvg);
		//System.out.println(gwAvg);
		//Penalties for those that exist
		for (String name: corrApp.get(corrApp.size()-1).keySet()){
			if (appWeights.containsKey(name)){
				if (appWeights.get(name)>appAvg){
					appPenalties.put(name,1-appRem);
				}else{
					appPenalties.put(name, 1+appRem);
				}
			}else{
				appPenalties.put(name, 1+appRem);
			}
		}
	}
	
	private void modifyGwPenalties(Map<String, Double> gwWeights,Double gwRem) {
		Double gwAvg = 0.0;
		//Gw
		for (String name: gwWeights.keySet()){
			gwAvg+=gwWeights.get(name);
		}
		gwAvg=gwAvg/(double)gwWeights.size();
		//System.out.println("----->Averages:");
		//System.out.println(appAvg);
		//System.out.println(gwAvg);
		//Penalties for those that exist
		for (String name: corrGw.get(corrGw.size()-1).keySet()){
			if (gwWeights.containsKey(name)){
				if (gwWeights.get(name)>=gwAvg){
					gwPenalties.put(name, 1-gwRem);
				}else{
					gwPenalties.put(name, 1+gwRem);
				}
			}else{
				gwPenalties.put(name, 1+gwRem);
			}
		}
	}
	
	//Rand Modify
	private void modifyRandPenalties(double min, double max) {
		Random rand = new Random();
		for (String name:this.corrApp.get(bestIter).keySet()){
			appPenalties.put(name, rand.nextDouble()*(max-min)*min);
		}
		for (String name:this.corrGw.get(bestIter).keySet()){
			gwPenalties.put(name, rand.nextDouble()*(max-min)*min);
		}
		
	}
	
	private void modifyAppRandPenalties(double min, double max) {
		Random rand = new Random();
		for (String name:this.corrApp.get(bestIter).keySet()){
			appPenalties.put(name, rand.nextDouble()*(max-min)+min);
		}
	}
	
	private void modifyGwRandPenalties(double min, double max) {
		Random rand = new Random();
		for (String name:this.corrApp.get(bestIter).keySet()){
			appPenalties.put(name, rand.nextDouble()*(max-min)+min);
		}
	}
	
	
	
	//Overfitting
	private boolean verifyOverFitiing(Map<String,Double> appWeights,Map<String,Double> gwWeights) {
		// if any component is at 0.8 or higher
		double overfitThresh = 0.8;
		for (String value: appWeights.keySet()){
			if (appWeights.get(value)>=overfitThresh){
				return true;
			}
		}
		for (String value: gwWeights.keySet()){
			if (gwWeights.get(value)>=overfitThresh){
				return true;
			}
		}
		return false;
	}
	
	private boolean verifyAppOverFitiing(Map<String,Double> appWeights) {
		// if any component is at 0.8 or higher
		double overfitThresh = 0.8;
		for (String value: appWeights.keySet()){
			if (appWeights.get(value)>=overfitThresh){
				return true;
			}
		}
		return false;
	}
	
	private boolean verifyGwOverFitiing(Map<String,Double> gwWeights) {
		// if any component is at 0.8 or higher
		double overfitThresh = 0.8;
		for (String value: gwWeights.keySet()){
			if (gwWeights.get(value)>=overfitThresh){
				return true;
			}
		}
		return false;
	}
	
	//Verify Underfitting
	private boolean verifyUnderfitting(Map<String,Double> appWeights,Map<String,Double> gwWeights,int appSize,int gwSize) {
		Double appAvg = 0.0;
		Double gwAvg = 0.0;
		double avgThresh = 0.35;
		for (String value: appWeights.keySet()){
			appAvg+=appWeights.get(value);
		}
		for (String value: gwWeights.keySet()){
			gwAvg+=gwWeights.get(value);
		}
		if (gwWeights.size()==0 || appWeights.size()==0){
			return true;
		}
		if (gwAvg/(double)gwWeights.size()<avgThresh || appAvg/(double)appWeights.size()<avgThresh){
			return true;
		}
		return false;
	}
	
	private boolean verifyAppUnderfitting(Map<String,Double> appWeights,int appSize) {
		Double appAvg = 0.0;
		double avgThresh = 0.35;
		for (String value: appWeights.keySet()){
			appAvg+=appWeights.get(value);
		}
		if (appWeights.size()==0){
			return true;
		}
		if ( appAvg/(double)appWeights.size()<avgThresh){
			return true;
		}
		return false;
	}
	
	private boolean verifyGwUnderfitting(Map<String,Double> gwWeights,int gwSize) {
		Double appAvg = 0.0;
		Double gwAvg = 0.0;
		double avgThresh = 0.35;
		for (String value: gwWeights.keySet()){
			gwAvg+=gwWeights.get(value);
		}
		if (gwWeights.size()==0 ){
			return true;
		}
		if (gwAvg/(double)gwWeights.size()<avgThresh){
			return true;
		}
		return false;
	}
	
	private Map<String,Double> getWeights(Map<String, Double> corr){
		//slimmed down to just one type 
		Double Max = 0.0;
		for (String name: corr.keySet()){
			if (Math.abs(corr.get(name))>Max){
				Max = Math.abs(corr.get(name));
			}
		}		
		//
		Map<String,Double> Weights = new HashMap<>();
		for (String name : corr.keySet()) {
			if (appPenalties.containsKey(name)) {
				if (Math.abs(corr.get(name)) > Max * appProcLim) {
					Weights.put(name, Math.abs(corr.get(name)) * appPenalties.get(name));
				}
			} else if (gwPenalties.containsKey(name)) {
				if (Math.abs(corr.get(name)) > Max * gwProcLim) {
					Weights.put(name, Math.abs(corr.get(name)) * gwPenalties.get(name));
				}
			} else {
				if (Math.abs(corr.get(name)) > Max * procLim) { //Seems like error but its not Just leave it be
					Weights.put(name, Math.abs(corr.get(name)));
				}
			}
		}
		
		//Adjust so that their sum is 1 (why dunno, seems to make sense to me)
		double sum1=0.0;
		for (String name: Weights.keySet()){
			sum1+=Weights.get(name);
		}
		for (String name: Weights.keySet()){
			Weights.put(name,Weights.get(name)/sum1);
		}
		return Weights;
	}

	private void probe(Map<String, Double> corrApp, Map<String, Double> corrGw){
		Double appMax = 0.0;
		Double gwMax = 0.0;
		//System.out.println("Probe:"+corrApp+" CorrGW:"+corrGw);
		for (String name: corrApp.keySet()){
			if (Math.abs(corrApp.get(name))>appMax){
				appMax = Math.abs(corrApp.get(name));
			}
		}
		for (String name: corrGw.keySet()){
			if (Math.abs(corrGw.get(name))>gwMax){
				gwMax = Math.abs(corrGw.get(name));
			}
		}		
		//
		//System.out.println("Info:");
		//System.out.println(appPenalties);
		//System.out.println(gwPenalties);
		this.appWeights.add(new HashMap<>());
		this.gwWeights.add(new HashMap<>());
		int i = this.gwWeights.size()-1;
		//Apps
		for (String name: corrApp.keySet()){
			//System.out.println("Name:"+name);
			if (appPenalties.get(name)!=null){
				if (Math.abs(corrApp.get(name))>appMax*appProcLim){
					appWeights.get(i).put(name, corrApp.get(name)*appPenalties.get(name));
				}
			}else{
				if (Math.abs(corrApp.get(name))>appMax*appProcLim){
					appWeights.get(i).put(name,corrApp.get(name));
				}
			}
		}
		//Gateways
		for (String name: corrGw.keySet()){
			if (gwPenalties.get(name)!=null){
				if (Math.abs(corrGw.get(name))>gwMax*gwProcLim){
					gwWeights.get(i).put(name, corrGw.get(name)*gwPenalties.get(name));
				}
			}else{
				if (Math.abs(corrGw.get(name))>gwMax*gwProcLim){
					gwWeights.get(i).put(name, corrGw.get(name));
				}
			}
		}
		
		//Adjust so that their sum is 1 (why dunno, seems to make sense to me)
		double sum1=0.0;
		for (String name: appWeights.get(i).keySet()){
			sum1+=Math.abs(appWeights.get(i).get(name));
		}
		for (String name: appWeights.get(i).keySet()){
			appWeights.get(i).put(name,appWeights.get(i).get(name)/sum1);
		}
		double sum2=0.0;
		for (String name: gwWeights.get(i).keySet()){
			sum2+=Math.abs(gwWeights.get(i).get(name));
		}
		for (String name: gwWeights.get(i).keySet()){
			gwWeights.get(i).put(name,gwWeights.get(i).get(name)/sum2);
		}
		//System.out.println("GW Weights Probe:"+gwWeights.get(i)+" App Weights:"+appWeights.get(i));
		//If all else fails set weights to 1 :-??
		if (appWeights.get(i).size()==0){
		appWeights.get(i).put("Constraints",0.1);appWeights.get(i).put("RequirementSim",0.1);appWeights.get(i).put("ResourceShare",0.1);appWeights.get(i).put("MessageRate",0.1);appWeights.get(i).put("UtilityWeights",0.1);appWeights.get(i).put("UnitLoad",0.1);appWeights.get(i).put("Distance",0.1);
		}
		if (gwWeights.get(i).size()==0){
			gwWeights.get(i).put("Capabilities",0.1);gwWeights.get(i).put("SharedRes",0.1);gwWeights.get(i).put("PerfToULoad",0.1);gwWeights.get(i).put("BaseLoad",0.1);gwWeights.get(i).put("CapToULoad",0.1);
		}
	}
	
	private boolean dirStopCriterion(){
		if (appFailed==true|| gwFailed==true){
			return false;
		}
		if (utils.size()<=1){
			return true;
		}
		if (relDiff((double)utils.get(prevBestIter),(double)utils.get(bestIter))>diffLim && bestIter==utils.size()-1){
			return true;
		}
		return false;
	}
	
	private boolean fullStopCriterion() {
		if (!dirStopCriterion()){
			if (utils.size()>maxStep || failMax<=failCnt){
				return false;
			}
		}
		return true;
	}
	
	public void attemptResult(Float utility) {
		//Insert the value of the solution just attempted
		utils.add(utility);
		if (utility>utils.get(bestIter)){
			prevBestIter = bestIter;
			bestIter = utils.size()-1;
			//Reset only when new best point is found
			failCnt = 0;
		}
		if (utils.size()>2){
			//Not First Iteration
			if (!fullStopCriterion()){
				//Everything Fine, keep Probing to see how deep this goes 
				exit=true;
			}
		}
	}
	

	public boolean getNextStep(){
		//Empty request for the next step, will be reimplemented
		return !exit;
	}

	public void setAppFailed() {
		//Triggered when an attemt failed for one reason or another
		appFailed = true;
	}
	
	public void setGwFailed() {
		//Triggered when an attemt failed for one reason or another
		gwFailed = true;
	}
	
	public String getChar() {
		////Return Characteristics as a string
		return "Count:"+utils.size()+" FailSteps:"+failCnt+" ProcLim[app/gw]:"+appProcLim+"/"+gwProcLim+" App-Penalties:"+appPenalties+" Gw-Penalties:"+gwPenalties;
	}
	
	public void showData(){
		System.out.println("Data:");
		System.out.println("Utils:"+utils);
		System.out.println("Best Iter: "+bestIter);
		System.out.println("Dir:"+dirStopCriterion()+" Exit:"+fullStopCriterion());
		System.out.println("CorrApp:"+corrApp);
		System.out.println("CorrGw:"+corrGw);
		System.out.println("Weight Apps:"+appWeights);
		System.out.println("Weight Gws:"+gwWeights);
		System.out.println("Penalty Apps:"+appPenalties);
		System.out.println("Penalty Gws:"+gwPenalties);
	}
	
	public void showWeights(){
		System.out.print("Weight Apps:"+appWeights.get(appWeights.size()-1));
		System.out.println("Weight Gws:"+gwWeights.get(gwWeights.size()-1));
	}

	public Map<String, Double> appWeights() {
		return appWeights.get(appWeights.size()-1);
	}

	public Map<String, Double> gwWeights() {
		return gwWeights.get(gwWeights.size()-1);
	}
	//Math part
	public Double relDiff(Double x, Double y) {
		// Get RelativeDiffrence of two numbers
		// RelDiff = | x - y | / max(|x|,|y|)
		// |x| = sqrt(x^2)
		if (x.compareTo(y) == 0) {
			return 0.0;
		} else {
			return Math.sqrt((x - y) * (x - y)) / Math.max(Math.sqrt(x * x), Math.sqrt(y * y));
		}
	}
	
	public static void main(String[] args) {
		WeighTrainer w1 = new WeighTrainer(10,2);
		List<Map<String, Double>> corrApp = new ArrayList<Map<String,Double>>();
		List<Map<String, Double>> corrGw  = new ArrayList<Map<String,Double>>();
		List<Double> util = new ArrayList<Double>();
		//Iteration Nr. 1
		int iter =0;
		corrApp.add(new HashMap());
		corrGw.add(new HashMap());
		util.add(57.29681);
		//App
		corrApp.get(iter).put("Constraints",0.0349);
		corrApp.get(iter).put("RequirementSim",0.002361);
		corrApp.get(iter).put("ResourceShare",0.01075);
		corrApp.get(iter).put("MessageRate",-0.00127);
		corrApp.get(iter).put("UtilityWeights",0.0);
		corrApp.get(iter).put("UnitLoad",-0.0071);
		corrApp.get(iter).put("Distance",-0.07927);
		//GW
		corrGw.get(iter).put("Capabilities",0.0);
		corrGw.get(iter).put("SharedRes",0.15986);
		corrGw.get(iter).put("PerfToULoad",0.0);
		corrGw.get(iter).put("BaseLoad",0.007514);
		corrGw.get(iter).put("CapToULoad",0.003785);
		//Iteration Nr. 2
		iter = 1;
		corrApp.add(new HashMap());
		corrGw.add(new HashMap());
		util.add(57.65052);
		//App
		corrApp.get(iter).put("Constraints",0.01849);
		corrApp.get(iter).put("RequirementSim",0.0059);
		corrApp.get(iter).put("ResourceShare",0.3482);
		corrApp.get(iter).put("MessageRate",0.00137);
		corrApp.get(iter).put("UtilityWeights",0.0);
		corrApp.get(iter).put("UnitLoad",-0.02149);
		corrApp.get(iter).put("Distance",-0.08280);
		//GW
		corrGw.get(iter).put("Capabilities",0.0);
		corrGw.get(iter).put("SharedRes",0.53);
		corrGw.get(iter).put("PerfToULoad",0.0);
		corrGw.get(iter).put("BaseLoad",0.00737);
		corrGw.get(iter).put("CapToULoad",0.0048);
		//Iteration Nr. 3
		iter = 2;
		corrApp.add(new HashMap());
		corrGw.add(new HashMap());
		util.add(57.7019);
		//App
		corrApp.get(iter).put("Constraints",0.00957);
		corrApp.get(iter).put("RequirementSim",0.01175);
		corrApp.get(iter).put("ResourceShare",0.1392);
		corrApp.get(iter).put("MessageRate",0.00943);
		corrApp.get(iter).put("UtilityWeights",0.0);
		corrApp.get(iter).put("UnitLoad",-0.005983);
		corrApp.get(iter).put("Distance",-0.077426);
		//GW
		corrGw.get(iter).put("Capabilities",0.0);
		corrGw.get(iter).put("SharedRes",0.4290);
		corrGw.get(iter).put("PerfToULoad",0.0);
		corrGw.get(iter).put("BaseLoad",0.0);
		corrGw.get(iter).put("CapToULoad",0.00619);
		int i = 0;
		w1.attemptResult((float)51.05);
		while (w1.getNextStep()){
			System.out.println("-->Step: "+w1.getChar());
			w1.correlationResults(corrApp.get(i), corrGw.get(i));
			w1.attemptResult(util.get(i).floatValue());
			w1.showData();
			if (i<2){i++;}
		}
		System.out.println("Final:");
		w1.showData();
	}

}
