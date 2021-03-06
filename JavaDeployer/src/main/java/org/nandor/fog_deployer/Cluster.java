package org.nandor.fog_deployer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Cluster {
	private String name;
	private Map<Integer, App> apps;
	private Map<Integer, Gateway> gateways;
	private Map<Integer, Float> gatewayShare;
	private Map<Integer,Float> shareLoad;
	private int id;
	private Map<Integer,Integer> deployment;

	private static final AtomicInteger count = new AtomicInteger(0);
	
	
	// Initialization
	public Cluster(String name) {
		this.name = name;
		apps = new HashMap<>();
		gateways = new HashMap<>();
		gatewayShare = new HashMap<>();
		shareLoad = new HashMap<>();
		deployment = new HashMap<>();
		this.id = count.incrementAndGet();
	}
	
	public Cluster(int id, String name) {
		this.name = name;
		apps = new HashMap<>();
		gateways = new HashMap<>();
		gatewayShare = new HashMap<>();
		shareLoad = new HashMap<>();
		deployment = new HashMap<>();
		this.id=id;
		if (count.get()<id){
			count.set(id);
		}
	}


	//Get App Info
	public Float getClusterCompoundUtility(){
		float util=(float)0.0;
		for (Integer a: getApps().keySet()){
			util+=getApps().get(a).getAppUtility();
		}
		return util;
	}
	
	public Float getClusterConstViolations(){
		float viol=(float)0.0;
		for (Integer a: getApps().keySet()){
			viol+=getApps().get(a).getConstraintViolations();
		}
		return viol;
	}
	public Float getClusterCompoundDelay(){
		float del=(float)0.0;
		for (Integer a: getApps().keySet()){
			del+=getApps().get(a).getTotDelay();
		}
		return del;
	}
	public Float getClusterCompoundReliability(){
		float rel=(float)0.0;
		for (Integer a: getApps().keySet()){
			rel+=getApps().get(a).getAppReliability();
		}
		return rel;
	}
	
	//Verify Validity of Deployment
	public int verifyIndValidity(){
		//System.out.println("Verifying Validity");
		int viol = 0;
		for (Integer g: this.getGateways().keySet()){
			Gateway gate = this.gateways.get(g);
			Float load = gate.getGwBaseLoad()+gate.getClusterShare(this.getId());
			if (gate.getGwLoad()>load){
				//System.out.println("False");
				//System.out.println("GW: "+gate.getId()+" Validity: "+gate.getGwLoad()+" Load: "+load);
				viol++;
			}
		}
		return viol + this.verifyCapabilityVailidty();
	}
	
	public int verifyValidityVerbose() {
		int viol = 0;
		for (Integer g: this.getGateways().keySet()){
			Gateway gate = this.gateways.get(g);
			Float load = gate.getGwBaseLoad()+gate.getClusterShare(this.getId());
			if (gate.getGwLoad()>load){
				System.out.println("GW: "+gate.getId()+" Load: "+gate.getGwLoad()+" MaxLoad: "+load);
				viol++;
			}
		}
		System.out.println("Final Resp:"+this.verifyCapabilityVailidtyVerbose());
		System.out.println("Utility:"+this.getClusterCompoundUtility());
		return viol+this.verifyCapabilityVailidty();
	}
	
	public int verifyCapabilityVailidty(){
		int viol =0;
		for (Integer a: this.getApps().keySet()){
			if (!this.getApps().get(a).validateRequirements()){
				viol++;
			}
		}
		return viol;
	}
	
	public int verifyCapabilityVailidtyVerbose(){
		int viol =0;
		for (Integer a: this.getApps().keySet()){
			if (!this.getApps().get(a).validateRequirements()){
				System.out.println("Failed App "+a+" req of: "+this.getApps().get(a).getRequirements()+" on Gw: "+this.getApps().get(a).getGateway().getId()+" with Capabilities: "+this.getApps().get(a).getGateway().getCapabilities());
				viol++;
			}
		}
		return viol;
	}
	
	
	//Compute Stuff
	public Float getClusterLoad(){
		Float totL=(float)0.0;
		for( Integer a : apps.keySet()){
			totL+=apps.get(a).getAppLoad((float)1.0);			
		}
		return totL;
	}
	
	
	
	//Add Stuff
	public Map<Integer, Integer> getDeployment() {
		return deployment;
	}
	public void setDeployment(Map<Integer, Integer> deployment) {
		this.deployment = deployment;
	}
	
	public void clearGateways()
	{
		this.gateways=new HashMap<>();
		this.gatewayShare = new HashMap<>();
	}
	
	public void removeGateway(Gateway gateway) {
		this.gateways.remove(gateway.getId());
		this.gatewayShare.remove(gateway.getId());
		this.shareLoad.remove(gateway.getId());
	}
	
	public void addGateway(Gateway gateway,Float share,Float load) {
		this.gateways.put(gateway.getId(), gateway);
		this.gatewayShare.put(gateway.getId(), share);
		this.shareLoad.put(gateway.getId(), load);
	}

	public void addGateway(Gateway gateway) {
		this.gateways.put(gateway.getId(), gateway);
		this.gatewayShare.put(gateway.getId(),(float)0.0);
	}
	
	public void modifyGateway(Gateway gateway, Float free) {
		this.gatewayShare.put(gateway.getId(),this.gatewayShare.get(gateway.getId())+free);
		
	}
	
	public Float getTotResShare() {
		Float tot = (float)0.0;
		for (Integer g: getGateways().keySet()){
			tot+=gatewayShare.get(g)*getGateways().get(g).getPjCap();
		}
		return tot;
	}
	
	public Float ShareRate() {
		return this.getTotResShare()/this.getClusterLoad();
	}

	
	public void addAppConn(App a) {
		apps.put(a.getId(), a);
		a.setCluster(this);
	}

	public String toString() {
		return name + "; " + "Gateways: " + gateways.keySet() + "; " + "Apps:" + apps.keySet();
	}

	public String getInfo(){
		return null;
	}
	// Basic Setters and Getters for Gateway
	
	public Map<Integer,Integer> getGwResourcesCount(){
		Map<Integer,Integer> gwResCnt=new HashMap<>();
		for (Integer a:getApps().keySet()){
			for (Integer r: getApps().get(a).getResources().keySet()){
				 Integer id = getApps().get(a).getResources().get(r).getGateway().getId();
				 if (gwResCnt.get(id)==null){
					 gwResCnt.put(id, 1);
				 }
				 else{
					 gwResCnt.put(id,gwResCnt.get(id)+1); 
				 }
			}
		}		
		return gwResCnt;	
	}
	
	public static void resetIndex() {
		count.set(0);
		
	}
	
	public Map<Integer, Gateway> getGateways() {
		return gateways;
	}

	public Map<Integer, Float> getGatewayShare() {
		return gatewayShare;
	}
	public Map<Integer, Float> getShareLoad() {
		return shareLoad;
	}

	
	public void setGateways(Map<Integer, Gateway> gateways) {
		this.gateways = gateways;
	}

	public Map<Integer, App> getApps() {
		return apps;
	}

	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public void setApps(Map<Integer, App> apps) {
		this.apps = apps;
	}

	public int getId() {
		return id;
	}



}
