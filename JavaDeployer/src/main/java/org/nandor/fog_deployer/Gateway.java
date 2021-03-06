package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Gateway {
	private String name;
	private String type;
	private Map<Integer,Resource> resources;
	private Map<Integer,App> apps;
	private static final AtomicInteger count = new AtomicInteger(0);
	private int id; 
	private Float Ldk = (float)0.73;
	private Float Lidle = (float)3.68;
	private Float PjCap = (float)1;
	private Float PjSpeed = (float)1;
	//Types of Resources: Cloud, Storage, LocalAccesPoint, Device
	private Map<Integer,Cluster> cluster;
	private Map<Integer,Float> clustShare;
	private Map<Integer,Float> shareLoad;
	private Map<Integer,Gateway> Gateways;
	private Map<Integer,Float> latencyGateways;
	private List<String> capabilities = new ArrayList<>();
	//Initialization
	public Gateway(String name,String type){
		this.type=type;
		this.name=name;
		resources=new HashMap<>();
		apps=new HashMap<>();
		cluster=new HashMap<>();
		clustShare=new HashMap<>();
		shareLoad = new HashMap<>();
		Gateways = new HashMap<>();
		latencyGateways = new HashMap<>();
		this.id=count.incrementAndGet();
	}
	public Gateway(int id,String name,String type,float Lidle,float PjCap,float PjSpeed){
		this.type=type;
		this.name=name;
		resources=new HashMap<>();
		apps=new HashMap<>();
		cluster=new HashMap<>();
		clustShare=new HashMap<>();
		shareLoad = new HashMap<>();
		Gateways = new HashMap<>();
		latencyGateways = new HashMap<>();
		this.Lidle=Lidle;
		this.PjCap=PjCap;
		this.PjSpeed=PjSpeed;
		this.id=id;
		if (count.get()<id){
			count.set(id);
		}
	}
	
	public String toString(){
	    return name;
}
	public String getInfo()
	{
		String ret=name+"; "
	    		 +"Type: "+type+"; ";
				 ret+="Gw_Load: "+this.getGwLoad()+"; ";
	    		 ret+="Clusters "+clustShare.toString()+"; ";
	     		 ret+="Apps: "+apps.keySet()+"; "
	    	    		 +"Resources: "+resources.keySet();
	    return ret;
	}
	
	
	public Float getAvgAddedLoad(Cluster c){
		Float ret = (float)0.0;
		for (Integer cls :clustShare.keySet()){
			if (cls!=c.getId())
			{
				ret+=clustShare.get(cls)/100*shareLoad.get(cls);
			}
		}
		return ret;
	}
	//Reliability
	public Float getGwReliabiltiy(){
		//RjGW=e-0.468*LjGw^3+0.3288LjGw^2-0.1416LjGw-0.036;
		double gL=this.getGwLoad()/(double)100;
		double pow = -0.468*gL*gL*gL+0.3288*gL*gL-0.1416*gL-0.036;
		double gwRel = Math.pow(10,pow);
		//System.out.println(gL);
		//System.out.println(pow);
		//System.out.println(gwRel);
		//Return gwRel in [0-1] range not in % as gw Load
		return (float)gwRel;
	}
	public Float getEstGwReliabiltiy(){
		//RjGW=e-0.468*LjGw^3+0.3288LjGw^2-0.1416LjGw-0.036;
		double gL=0.0;
		for (Integer cls :clustShare.keySet()){
			gL+=clustShare.get(cls)/100*shareLoad.get(cls);
		}
		double pow = -0.468*gL*gL*gL+0.3288*gL*gL-0.1416*gL-0.036;
		double gwRel = Math.pow(10,pow);
		//System.out.println(gL);
		//System.out.println(pow);
		//System.out.println(gwRel);
		//Return gwRel in [0-1] range not in % as gw Load
		return (float)gwRel;
	}
	
	
	//Calculations
	public Float getGwLoad(){
		Float load=(float)0.0;
		for (Integer key: this.apps.keySet()){
			load+=this.apps.get(key).getAppLoad();
		}
		load+=Lidle;
		for (Integer key: this.resources.keySet())
		{
			load+= this.resources.get(key).getTotMsgs() * Ldk/PjCap;
		}
		return load;
	}
	
	public Float getFreeLoad(){
		return (float)(100.0-getGwLoad())*getPjCap();
	}
	
	public Float getGwBaseLoad(){
		Float load=(float)0.0;
		load+=Lidle;
		for (Integer key: this.resources.keySet())
		{
			load+= this.resources.get(key).getTotMsgs() * Ldk/PjCap;
		}
		return load;
	}
	
	public Float getTotMsgRate() {
		Float load=(float)0.0;
		for (Integer key: this.resources.keySet())
		{
			load+= this.resources.get(key).getTotMsgs();
		}
		return load;
	}
	
	//GW to * Connection
	public void addResConn(Resource r)
	{
		resources.put(r.getId(), r);
		r.setGateway(this);
	}
	
	public void addAppConn(App a)
	{
		apps.put(a.getId(), a);
		a.setGateway(this);
	}
	
	public void clearApps()
	{
		apps.clear();
	}
	
	public Map<Integer, Gateway> getGateways() {
		return Gateways;
	}
	
	public Map<Integer, Float> getGatewaysLatency() {
		return latencyGateways;
	}
	
	public Float getGatewayLatency(int id){
		if (this.getId()==id){
			return (float)0.0;
		}else{
		return latencyGateways.get(id);
		}
	}
	
	public void addGateway(Gateway gateway,Float latency){
		this.Gateways.put(gateway.getId(), gateway);
		this.latencyGateways.put(gateway.getId(), latency);
	}
	
	public void addCluster(Cluster cluster) {
		this.cluster.put(cluster.getId(), cluster);
		this.clustShare.put(cluster.getId(), (float)0.0);
	}	
	
	public void removeCluster(Cluster cluster) {
		this.cluster.remove(cluster.getId());
		this.clustShare.remove(cluster.getId());
		this.shareLoad.remove(cluster.getId());
	}	
	
	public void addCluster(Cluster cluster,Float share,Float load) {
		this.cluster.put(cluster.getId(), cluster);
		this.clustShare.put(cluster.getId(), share);
		this.shareLoad.put(cluster.getId(), load);
	}
	
	public void modifyCluster(Cluster cluster2, Float free) {
		this.clustShare.put(cluster2.getId(),this.clustShare.get(cluster2.getId())+free);
		
	}
	
	public static void resetCount(){
		count.set(0);
	}
	//Basic Setters and Getters for Gateway
	public void clearClusters(){
		this.cluster = new HashMap<>();
		this.clustShare = new HashMap<>();
	}
	
	public Float getClusterShare(Cluster c1){
		return this.clustShare.get(c1.getId());
	}
	
	public Float getClusterShare(Integer c1){
		return this.clustShare.get(c1);
	}
	
	public Map<Integer, Float> getAllClusterShare(){
		return this.clustShare;
	}
	
	public Float getClusterLoadShare(Integer c1){
		return this.shareLoad.get(c1);
	}
	public Map<Integer,Cluster> getCluster() {
		return cluster;
	}
	public void setCluster(Map<Integer,Cluster> cluster) {
		this.cluster = cluster;
	}	
	public int getId() {
		return id;
	}
	public Map<Integer,Resource> getResources() {
		return resources;
	}
	public void setResources(Map<Integer,Resource> resources) {
		this.resources = resources;
	}
	public Map<Integer,App> getApps() {
		return apps;
	}
	public void setApps(Map<Integer,App> apps) {
		this.apps = apps;
	}
	public Float getPjCap() {
		return PjCap;
	}
	public void setPjCap(Float pjCap) {
		PjCap = pjCap;
	}
	public Float getPjSpeed() {
		return PjSpeed;
	}
	public void setPjSpeed(Float pjSpeed) {
		PjSpeed = pjSpeed;
	}
	public Float getLidle() {
		return Lidle;
	}

	public void setLidle(Float lidle) {
		Lidle = lidle;
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
	public List<String> getCapabilities() {
		return capabilities;
	}
	public void setCapabilities(List<String> capabilities) {
		this.capabilities = capabilities;
	}
	
	public void addCapabilities(String req) {
		this.capabilities.add(req);
	}
	
	public void removeCapabilitiets(String req) {
		this.capabilities.remove(req);
	}
	

}
