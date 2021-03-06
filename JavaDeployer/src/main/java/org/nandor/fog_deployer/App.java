package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class App {
	private String name;
	private String type;
	private static final AtomicInteger count = new AtomicInteger(0);
	private int id; 
	private float k1=(float)38.409;
	private float k2=(float)0.1885;
	private float Lm = (float) 0.285;
	private float unitLoad;
	private float Ldk = (float)0.73;
	private Map<String,Float> utilityWeights;
	private Map<String,Float> constraints;
	private Map<Integer,Float> Rmessages;
	private Map<Integer,Float> Amessages;
	private List<String> requirements = new ArrayList<>();
	//Types of Resources: Cloud, Storage, LocalAccesPoint, Device
	private Gateway gateway;
	private Cluster cluster;
	private Map<Integer,Resource> resources;
	private Map<Integer,App> apps;
	
	//Initialization
	public App(String name,String type,float unitLoad){
		this.type=type;
		this.name=name;
		this.unitLoad=unitLoad;
		resources=new HashMap<>();
		apps=new HashMap<>();
		Rmessages=new HashMap<>();
		Amessages=new HashMap<>();
		utilityWeights = new HashMap<>();
		utilityWeights.put("reliability", (float)1.0);
		utilityWeights.put("delay", (float)1.0);
		utilityWeights.put("constraint", (float)1.0);
		constraints = new HashMap<>();
		constraints.put("reliability", Float.MIN_VALUE);
		constraints.put("delay", Float.MAX_VALUE);
		this.id=count.incrementAndGet();
	}
	
	public App(int id, String name,String type,float unitLoad){
		this.type=type;
		this.name=name;
		this.unitLoad=unitLoad;
		resources=new HashMap<>();
		apps=new HashMap<>();
		Rmessages=new HashMap<>();
		Amessages=new HashMap<>();
		utilityWeights = new HashMap<>();
		utilityWeights.put("reliability", (float)0.0);
		utilityWeights.put("delay", (float)1.0);
		utilityWeights.put("constraint", (float)0.0);
		constraints = new HashMap<>();
		constraints.put("reliability", Float.POSITIVE_INFINITY);
		constraints.put("delay", Float.POSITIVE_INFINITY);
		this.id=id;
		if (count.get()<id){
			count.set(id);
		}
	}
	//Estimated App Utility Stuff
	public Float getEstAppUtility(){
		//Need to normalize Values of Apps to 0-1 range...how the fk, Reliability, Const Viol easy ofcourse but others ?
		float gwAddedLoad = this.gateway.getAvgAddedLoad(this.cluster);		
		float ret= utilityWeights.get("reliability")*this.getEstAppReliability();
		//System.out.println("util-rel: "+utilityWeights.get("reliability")*this.getAppReliability());
		ret+=utilityWeights.get("constraint")*(1.0-this.getEstConstraintViolations(gwAddedLoad));
		//Get Reference Delay
		//System.out.println("util-constr: "+utilityWeights.get("constraint")*(1.0-this.getConstraintViolations()));
		float refDel=this.getProcDelay((float)5.4, (float)1);
		ret+=utilityWeights.get("delay")*(refDel/this.getEstTotDelay(gwAddedLoad));
		//System.out.println("util-delay: "+utilityWeights.get("delay")*(refDel/this.getTotDelay()));
		return ret;	
	}	
	//Get Constraint Violations
	
	public Float getEstConstraintViolations(Float gwAddedLoad){
		float totDel=this.getEstTotDelay(gwAddedLoad);
		float rel=this.getEstAppReliability();
		float proc=(float)0.0;
		if (constraints.get("delay")>totDel){
			proc+=1;
		}
		if (constraints.get("reliability")<rel){
			proc+=1;
		}
		return proc;
	}
	
	//Get App Reliability on System
	public Float getEstAppReliability() {
		Float relApp = (float) 1.0;
		Map<Integer,Float> GRel = new HashMap<>();
		for (Integer r : resources.keySet()) {
			if (resources.get(r).getGateway() != null) {
				int id = resources.get(r).getGateway().getId();
				if (id != this.getGateway().getId()) {
					GRel.put(id, resources.get(r).getGateway().getEstGwReliabiltiy());
				}
			}
		}
		for (Integer a : apps.keySet()) {
			if (apps.get(a).getGateway() != null) {
				int id = apps.get(a).getGateway().getId();
				if (id != this.getGateway().getId()) {
					GRel.put(id, apps.get(a).getGateway().getEstGwReliabiltiy());
				}
			}
		}
		for (Integer i: GRel.keySet()){
			if (relApp==(float)1.0){
				relApp=(1-GRel.get(i));
			}else{
				relApp*=(1-GRel.get(i));
			}
		}
		//System.out.println("Res n App Rel:" + (1 - relApp));
		//System.out.println("Gw Rel:" + this.getGateway().getGwReliabiltiy());
		return ((float)(1.0 - relApp) * this.getGateway().getGwReliabiltiy());
	}	
	
	public Float getEstTotDelay(Float addedLoad){
		return getProcDelay(addedLoad)+getNetworkDelay();
	}
	
	
	// Equations for Processing Delay and App Load and Tot msg rate
	//utility
	public Float getAppUtility(){
		//Need to normalize Values of Apps to 0-1 range...how the fk, Reliability, Const Viol easy ofcourse but others ?
		float ret= utilityWeights.get("reliability")*this.getAppReliability();
		//System.out.println("util-rel: "+utilityWeights.get("reliability")*this.getAppReliability());
		ret+=utilityWeights.get("constraint")*(2.0-this.getConstraintViolations());
		//Get Reference Delay
		//System.out.println("util-constr: "+utilityWeights.get("constraint")*(1.0-this.getConstraintViolations()));
		float refDel=this.getProcDelay((float)5.4, (float)1);
		ret+=utilityWeights.get("delay")*(refDel/this.getTotDelay());
		//System.out.println("util-delay: "+utilityWeights.get("delay")*(refDel/this.getTotDelay()));
		return ret;	
	}
	
	//Get Constraint Violations
	
	public Float getConstraintViolations(){
		float totDel=this.getTotDelay();
		float rel=this.getAppReliability();
		float proc=(float)0.0;
		if (constraints.get("delay")<totDel){
			proc+=1;
		}
		if (constraints.get("reliability")>rel){
			proc+=1;
		}
		return proc;
	}
	
	//Get App Reliability on System
	public Float getAppReliability() {
		Float relApp = (float) 1.0;
		Map<Integer,Float> GRel = new HashMap<>();
		for (Integer r : resources.keySet()) {
			if (resources.get(r).getGateway() != null && this.getGateway()!=null) {
				int id = resources.get(r).getGateway().getId();
				if (id != this.getGateway().getId()) {
					GRel.put(id, resources.get(r).getGateway().getGwReliabiltiy());
				}
			}
		}
		for (Integer a : apps.keySet()) {
			if (apps.get(a).getGateway() != null && this.getGateway()!=null) {
				int id = apps.get(a).getGateway().getId();
				if (id != this.getGateway().getId()) {
					GRel.put(id, apps.get(a).getGateway().getGwReliabiltiy());
				}
			}
		}
		for (Integer i: GRel.keySet()){
			if (relApp==(float)1.0){
				relApp=(1-GRel.get(i));
			}else{
				relApp*=(1-GRel.get(i));
			}
		}
		//System.out.println("Res n App Rel:" + (1 - relApp));
		//System.out.println("Gw Rel:" + this.getGateway().getGwReliabiltiy());
		if (this.getGateway()==null){
			return (float)0.0;
		}else{
			return ((float)(1.0 - relApp) * this.getGateway().getGwReliabiltiy());
		}
	}
	
	//Proc Delay
	public Float getProcDelay(){
		Float PCoef=this.gateway.getPjSpeed();
		Float LGw=this.gateway.getGwLoad();
		Float procD=this.unitLoad*PCoef*(this.k1+this.k2*LGw);
		return procD;
	}
	public Float getProcDelay(Float load){
		Float PCoef=this.gateway.getPjSpeed();
		Float LGw=this.gateway.getGwLoad()+load;
		Float procD=this.unitLoad*PCoef*(this.k1+this.k2*LGw);
		return procD;
	}
	public Float getProcDelay(Float load,Float perfCoef){
		Float PCoef=perfCoef;
		Float LGw=load;
		Float procD=this.unitLoad*PCoef*(this.k1+this.k2*LGw);
		return procD;
	}
	
	
	public Float getNetworkDelay(){
		Float netwDel = (float)0.0;
		for (Integer r : resources.keySet()){
			//System.out.println("Resource:"+r);
			//System.out.println("ResGw:"+resources.get(r).getGateway().getId());
			//System.out.println("This Gw:"+this.getGateway().getId());
			netwDel+=resources.get(r).getGateway().getGatewayLatency(this.getGateway().getId());
		}
		for (Integer a : apps.keySet()){
			//System.out.println("App:"+a);
			//System.out.println("ResGw:"+apps.get(a).getGateway().getId());
			//System.out.println("This Gw:"+this.getGateway().getId());
			if (apps.get(a).getGateway()!=null){
				netwDel+=apps.get(a).getGateway().getGatewayLatency(this.getGateway().getId());
			}
		}	
		if (resources.size()+apps.size()==0){
			return (float)0.0;
		}else{
			return netwDel/((float)(resources.size()+apps.size()));
		}
	}
	
	public Float getTotDelay(){
		return getProcDelay()+getNetworkDelay();
	}
	
	
	public Float getEstAppResLoad(Float perfCoef,Float msgRate,Float aRate){
		Float LoadA=(this.unitLoad+this.Lm)*(msgRate+aRate)/perfCoef;
		LoadA+= msgRate * Ldk/perfCoef;
		return LoadA;
	}
	public Float getAppLoad(){
		Float LoadA=(this.unitLoad+this.Lm)*this.getTotalMsgRate()/this.getGateway().getPjCap();
		return LoadA;
	}
	
	public Float getAppLoad(Float perfCoef)
	{
		Float LoadA=(this.unitLoad+this.Lm)*this.getTotalMsgRate()/perfCoef;
		return LoadA;
	}
	//Total Msg Rate
	
	public Float getTotalMsgRate(){
		Float totMsg=(float)0.0;
		for (Integer i: this.Rmessages.keySet()){
			totMsg+=this.Rmessages.get(i);
		}
		for (Integer i: this.Amessages.keySet()){
			totMsg+=this.Amessages.get(i);
		}
		return totMsg;
	}
	//Add Stuff to App 
	public void addResource(Resource r,Float msgCount){
		this.resources.put(r.getId(), r);
		this.Rmessages.put(r.getId(), msgCount);
		r.addApps(this,msgCount);
	}
	
	public void addApps(App a,Float msgCount){
		this.apps.put(a.getId(), a);
		this.Amessages.put(a.getId(), msgCount);
	}
	
	//Stringify
	
	public String toString(){
	    return name;
}
	
	public void setConstraints(float delay, float reliabiltiy){
		constraints.put("reliability", reliabiltiy);
		constraints.put("delay", delay);
	}
	
	public Map<String,Float> getConstraints(){
		return constraints;
	}
	
	public Boolean validateRequirements(List<String> requirements) {
		return requirements.containsAll(this.requirements);
	}
	
	public Boolean validateRequirements() {
		return this.getGateway().getCapabilities().containsAll(this.requirements);
	}
	
	
	public String getInfo(){
		String ret=name+"; "
	    		 +"Type: "+type+"; "
	    		 +"Unit_Load: "+this.unitLoad+"; "
				 +"TotMsgRate: "+this.getTotalMsgRate()+"; ";
		if (gateway!=null){
	    		 ret+="GatewayID: "+gateway.getId()+"; ";
		}
		if (cluster!=null){
	    		 ret+="ClusterID:"+cluster.getId()+"; ";
		}
	     		 ret+="Resources:"+resources.keySet();
	     		 ret+="Apps: "+apps.keySet();
	    return ret;
	}
	public Map<String, Float> getUtilityWeights() {
		return utilityWeights;
	}

	public void setUtilityWeights(float delay, float reliability) {
		utilityWeights.put("reliability", reliability);
		utilityWeights.put("delay", delay);
	}
	public void setUtilityWeights(float delay, float reliability,float constraints) {
		utilityWeights.put("reliability", reliability);
		utilityWeights.put("delay", delay);
		utilityWeights.put("constraint", constraints);
	}
	
	public void setWeightConstraint(float constraints) {
		utilityWeights.put("constraint", constraints);
	}
	
	public static void resetCount(){
		count.set(0);
	}
	
	//basic Setters and Getters
	public Gateway getGateway() {
		return this.gateway;
	}
	public void setGateway(Gateway gw) {
		this.gateway = gw;
	}
	
	public int getId() {
		return id;
	}


	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Map<Integer,Resource> getResources() {
		return resources;
	}


	public void setResources(Map<Integer,Resource> resources) {
		this.resources = resources;
	}


	public Map<Integer, Float> getRmessages() {
		return Rmessages;
	}
	public Map<Integer, Float> getAmessages() {
		return Amessages;
	}
	public Map<Integer, App> getApps() {
		return apps;
	}
	public void setApps(Map<Integer, App> apps) {
		this.apps = apps;
	}
	public Cluster getCluster() {
		return cluster;
	}


	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}


	public Float getUnitLoad() {
		return unitLoad;
	}


	public void setUnitLoad(float unitLoad) {
		this.unitLoad = unitLoad;
	}

	public List<String> getRequirements() {
		return requirements;
	}

	public void setRequirements(List<String> requirements) {
		this.requirements = requirements;
	}
	
	public void addRequirements(String req) {
		this.requirements.add(req);
	}
	
	public void removeRequirements(String req) {
		this.requirements.remove(req);
	}

	
	
}
