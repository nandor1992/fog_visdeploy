package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class AdvancedCls extends Clustering {

	protected float fractioned = (float) 70.0;
	protected float locality = (float) 0.70;

	protected float wConns = (float) 1.0;
	protected float wRes = (float) 0.1;
	protected float wReq = (float) 0.0;
	protected float wSim = (float) 0.0;
	protected float simUl = (float) 1.0;

	public AdvancedCls(Fog f) {
		super(f);
	}

	// Distance for Neighbours version 2
	
	public Map<Integer, Double> getNeighbours(Integer p, Float eps) {
		// Need to Determine what We care about when looking at distance, have
		// weights for these
		// Need to be able to find all points in the system, and give a distance
		// to them based on these,
		// Find an eps value to which this fits.

		// Get Neighbours that are connected through Dijkstra search
		//System.out.println("---- Get Neighbours for P: " + p + " with eps: " + eps + " ----");
		//System.out.println("Dijkstra Results");
		Map<Integer, Double> res = dijkstraSearsch(p);
		// Adjust for Weight
		// Maybe only need to look for direct conns
		for (Integer dj : res.keySet()) {
			res.put(dj, 1 / res.get(dj) * wConns);
		}
		res.remove(p);
		//System.out.println(res);

		// Get Apps that have resources on the same GW
		//System.out.println("Resource Results");
		Map<Integer, Float> resApps = new HashMap<>();
		Set<Integer> resIds = new TreeSet<>();
		// GetGatewas of interest and find apps that have similar gateways.
		for (Integer r : this.fog.getApps().get(p).getResources().keySet()) {
			// Get all resources of App, then get all the resources from the
			// same GW
			for (Integer gR : this.fog.getResources().get(r).getGateway().getResources().keySet()) {
				if (gR != r) {
					resIds.add(gR);
				}
			}
		}
		for (Integer r : resIds) {
			if (resApps.containsKey(this.fog.getResources().get(r).getApp())) {
				resApps.put(this.fog.getResources().get(r).getApp(),
						resApps.get(this.fog.getResources().get(r).getApp()) + (float) 1.0 * wRes);
			} else {
				resApps.put(this.fog.getResources().get(r).getApp(), (float) 1.0 * wRes);
			}
		}
		resApps.remove(p);
		//System.out.println(resApps);

		// Get Apps that have similar requirements or same, same for now
		//System.out.println("Requirements Sim Results");
		List<String> reqs = this.fog.getApps().get(p).getRequirements();
		Map<Integer, Float> reqApps = new HashMap<>();
		for (Integer a : this.fog.getApps().keySet()) {
			if (this.fog.getApps().get(a).getRequirements().equals(reqs)){
				reqApps.put(a, (float) 1.0*wReq);
			}
		}
		reqApps.remove(p);
		//System.out.println(reqApps);
		
		
		// Get Apps that have similar characteristics, only looking at Load 
		//System.out.println("Load Similarity Results");
		Map<Integer, Float> simApps = new HashMap<>();
		Float ref = this.fog.getApps().get(p).getUnitLoad();
		//System.out.println(ref);
		for (Integer a : this.fog.getApps().keySet()) {
			//System.out.println("App: "+a+" Value: "+this.fog.getApps().get(a).getUnitLoad());
			Float ret = (float) Math.sqrt((double)(this.fog.getApps().get(a).getUnitLoad()-ref)*(this.fog.getApps().get(a).getUnitLoad()-ref));
			if (ret<=simUl){	
				simApps.put(a, (1-ret)*wSim);
			}
		}
		simApps.remove(p);
		//System.out.println(simApps);
		
		//Compile all the lists together into one preferably res
		//Resource
		for (Integer a:  resApps.keySet()){
			if (res.containsKey(a)){
				res.put(a, res.get(a)+resApps.get(a));
			}else{
				res.put(a,(double)resApps.get(a));
			}
		}
		//Requirements
		for (Integer a:  reqApps.keySet()){
			if (res.containsKey(a)){
				res.put(a, res.get(a)+reqApps.get(a));
			}else{
				res.put(a,(double)reqApps.get(a));
			}
		}
		//Similarity
		for (Integer a:  simApps.keySet()){
			if (res.containsKey(a)){
				res.put(a, res.get(a)+simApps.get(a));
			}else{
				res.put(a,(double)simApps.get(a));
			}
		}
		// Looking Through all the components and validating individual pints to
		// a list
		//System.out.println("Final:");
		//System.out.println(res);
		return res;
	}

	
	public List<Integer> getNeighbours(Integer p, Float eps, Integer lvl) {
		// Need to Determine what We care about when looking at distance, have
		//Adapter Method
		List<Integer> neighbour = new ArrayList<>();
		Map<Integer, Double> res = getNeighbours(p, (float)eps);
		//System.out.println("Final:");
		//System.out.println(res);
		for (Integer dj : res.keySet()) {
			if (res.get(dj) >= eps) {
				neighbour.add(dj);
			}
		}
		//System.out.println("--------");
		neighbour.remove(p);
		return neighbour;
	}

	public double distanceToCluster(Integer p, Set<Integer> clustP) {
		// Modify how distance of rogue or Noise nodes is calculated to clusters
		// (based on previous
		//System.out.println("---- Get Distance between P: " + p + " Cluster: " + clustP + " ----");
		Map<Integer, Double> res = getNeighbours(p, Float.MIN_VALUE);
		Float sum = (float)0.0;
		Float pCount = (float)0.0;
		for (Integer p1:clustP){
			if (res.get(p1)!=null){
				sum+=res.get(p1).floatValue();
				pCount+=(float)1.0;
			}
		}
		//System.out.println("Distance Sum"+sum);
		return 1/sum*pCount;
	}

	public Map<Integer, Double> dijkstraSearsch(Integer p) {
		// Init
		List<Integer> queue = new ArrayList<>();
		Map<Integer, Integer> prev = new HashMap<>();
		Map<Integer, Double> dist = new HashMap<>();
		for (Integer i : this.nodes.keySet()) {
			dist.put(i, Double.POSITIVE_INFINITY);
			prev.put(i, null);
			queue.add(i);
		}
		dist.put(p, 0.0);
		while (queue.size() > 0) {
			//System.out.println(queue);
			//System.out.println(dist);
			Integer u = getShortestDist(dist, queue);
			if (u != null) {
				//System.out.println(u);
				queue.remove(queue.indexOf(u));
				for (Integer i : this.nodes.get(u)) {
					Double newDist = dist.get(u) + 1.0;
					if (newDist < dist.get(i)) {
						dist.put(i, newDist);
						prev.put(i, u);
					}
				}
			}else{break;}
		}
		return dist;
	}

	// Distribution Version 2
	public void distributeGw2Cluster() {
		// Concepts: First Come first Serve
		// Concepts: Minimum Fraction Allowed to have a share of
		// Dependent (Singular, Shared) and Independent Gateways, independent
		// ones can be allocated at will
		this.fog.clearGwClustConns();
		Float totFreeGw = this.fog.getTotalFreeCapacity();
		Float totLoad = this.fog.getTotalLoad();
		Float ratio = totFreeGw / totLoad;
		Map<Integer, ArrayList<Integer>> locGws = new HashMap<>();
		// Getting Appropriate number of locality based important Gateways
		for (Integer c : this.fog.getClusters().keySet()) {
			Float clsLoad = this.fog.getClusters().get(c).getClusterLoad();
			// System.out.println("Cluster: "+c+"Apps:
			// "+this.fog.getClusters().get(c).getApps().keySet()+" Load:
			// "+this.fog.getClusters().get(c).getClusterLoad());
			Map<Integer, Integer> gwRes = this.fog.getClusters().get(c).getGwResourcesCount();
			locGws.put(c, new ArrayList<Integer>());
			Float allocLoad = (float) 0.0;
			// System.out.println(gwRes);
			while (allocLoad < clsLoad * ratio * locality && !gwRes.isEmpty()) {
				int bestIndi = 0;
				int bestIndId = 0;
				for (Integer g : gwRes.keySet()) {
					if (gwRes.get(g) > bestIndi) {
						bestIndi = gwRes.get(g);
						bestIndId = g;
					}
				}
				locGws.get(c).add(bestIndId);
				gwRes.remove(bestIndId);
				// System.out.println(bestIndId+" "+bestIndi);
				allocLoad += (float) (100.0 - this.fog.getGateways().get(bestIndId).getGwLoad())
						* this.fog.getGateways().get(bestIndId).getPjCap();
			}
			// System.out.println("Gws: "+locGws+" AllocLoad: "+allocLoad );
		}
		// Finding Gateways that are of the three types
		ArrayList<Integer> freeGws = new ArrayList<Integer>(this.fog.getGateways().keySet());
		Map<Object, List<Integer>> sharedGws = new HashMap<>();
		for (Integer g : locGws.keySet()) {
			for (Object g1 : locGws.get(g)) {
				if (freeGws.contains(g1)) {
					freeGws.remove(g1);
				}
				if (sharedGws.get(g1) == null) {
					List<Integer> l = new ArrayList<>();
					l.add(g);
					sharedGws.put(g1, l);
				} else if (sharedGws.get(g1) != null) {
					sharedGws.get(g1).add(g);
				}
			}
		}
		ArrayList<Object> i = new ArrayList<Object>(sharedGws.keySet());

		for (Object g:i){
			if (sharedGws.get(g).size()==1){
				sharedGws.remove(g);
			}
		}
		// Removing the average shared one
		//System.out.println("Before:");
		// System.out.println(locGws);
		// System.out.println(freeGws);
		// System.out.println(sharedGws);
		/*for (Integer g : sharedGws.keySet()) {
			int i=0;
			List<Integer> cList = new ArrayList<Integer>(locGws.keySet());
			for (Integer c: cList){
				List<Integer> gList = new ArrayList<Integer>(locGws.get(c));
				for (Integer g1:gList){
					if (g==g1){
						if (sharedGws.get(g).size()-1>=i){
							i++;
							locGws.get(c).remove(locGws.get(c).indexOf(g));
						}
					}
				}
			}
		}*/
		for (Object g: sharedGws.keySet()){
			Float minCls = Float.MAX_VALUE;
			Integer min = 0;
			for (Integer c: sharedGws.get(g)){
				if ( minCls > this.fog.getClusters().get(c).ShareRate()){
					minCls = this.fog.getClusters().get(c).ShareRate();
					min=c;
				}			
			}
			for (Integer c: sharedGws.get(g)){
				if ( min!=c){
					locGws.get(c).remove(locGws.get(c).indexOf(g));
				}			
			}
		}
		//System.out.println("After:");
		// System.out.println(locGws);
		// System.out.println(freeGws);
		// System.out.println(sharedGws);
		// Do Final Allocations of Gws to Clusters
		Map<Integer, Float> shareLeft = new HashMap<>();
		for (Integer c : locGws.keySet()) {
			// Allocate each individual Gw to Cluster and calculate how much is
			// needed and add all full portions
			// System.out.print("Cluster: "+c);
			Float allocRes = (float) 0.0;
			ArrayList<Integer> j = 	locGws.get(c);
			for (Integer g : j) {
				Float res = (float) (100.0 - this.fog.getGateways().get(g).getGwLoad())
						* this.fog.getGateways().get(g).getPjCap();
				if (allocRes + res < this.fog.getClusters().get(c).getClusterLoad() * 0.95 * ratio) {
					allocRes += res;
				} else {
					locGws.get(c).remove(g);
					freeGws.add(g);
				}
			}
			// System.out.print(" 1. Alloc: "+allocRes+ " Tot:
			// "+this.fog.getClusters().get(c).getClusterLoad()*0.95*ratio);
			// Add from free until full
			List<Integer> j2 = new ArrayList<Integer>(freeGws);
			for (Integer g : j2) {
				Float res = (100 - this.fog.getGateways().get(g).getGwLoad())
						* this.fog.getGateways().get(g).getPjCap();
				if (allocRes + res < this.fog.getClusters().get(c).getClusterLoad() * 0.95 * ratio) {
					allocRes += res;
					locGws.get(c).add(g);
					freeGws.remove(g);
				}
			}
			// System.out.println(" 2. Alloc: "+allocRes+ " Tot:
			// "+this.fog.getClusters().get(c).getClusterLoad()*0.95*ratio);
			shareLeft.put(c, this.fog.getClusters().get(c).getClusterLoad() * ratio - allocRes);
		}
		// System.out.println("Saving");
		// System.out.println(locGws);
		// System.out.println(freeGws);
		// System.out.println(shareLeft);
		// System.out.println(freeGws);
		// Do Actuall Allocations of Gws to Clusters everything here is 100%
		// allocations.
		for (Integer c : locGws.keySet()) {
			for (Object g : locGws.get(c)) {
				this.fog.getGateways().get(g).addCluster(this.fog.getClusters().get(c),
						(float) (100.0 - this.fog.getGateways().get(g).getGwLoad()), (float) 0.0);
				this.fog.getClusters().get(c).addGateway(this.fog.getGateways().get(g),
						(float) (100.0 - this.fog.getGateways().get(g).getGwLoad()), (float) 0.0);
			}
		}
		// Do remainder part
		Map<Integer, Float> allocGws = new HashMap<>();
		for (Integer g : freeGws) {
			allocGws.put(g, this.fog.getGateways().get(g).getFreeLoad());
		}
		List<Integer> j3 = new ArrayList<Integer>(shareLeft.keySet());
		/*
		 * for (Integer c: j3){ // Check if any gw can store this while
		 * retaining % of space left //fractioned = (float)0.35; for (Integer
		 * g:freeGws){ if (allocGws.get(g)-fractioned > shareLeft.get(c)){
		 * allocGws.put(g, allocGws.get(g)-shareLeft.get(c));
		 * this.fog.getGateways().get(g).addCluster(this.fog.getClusters().get(c
		 * ),shareLeft.get(c)/this.fog.getGateways().get(g).getPjCap(),(float)0.
		 * 0);
		 * this.fog.getClusters().get(c).addGateway(this.fog.getGateways().get(g
		 * ),shareLeft.get(c)/this.fog.getGateways().get(g).getPjCap(),(float)0.
		 * 0); shareLeft.remove(c); break; }else if (allocGws.get(g)*0.95 <
		 * shareLeft.get(c) && allocGws.get(g)*1.05 > shareLeft.get(c)){
		 * allocGws.put(g, allocGws.get(g)-shareLeft.get(c));
		 * this.fog.getGateways().get(g).addCluster(this.fog.getClusters().get(c
		 * ),(float)(100.0-this.fog.getGateways().get(g).getGwLoad()),(float)0.0
		 * );
		 * this.fog.getClusters().get(c).addGateway(this.fog.getGateways().get(g
		 * ),(float)(100.0-this.fog.getGateways().get(g).getGwLoad()),(float)0.0
		 * ); shareLeft.remove(c); break; } } }
		 */
		// System.out.println(shareLeft);
		// System.out.println(allocGws);
		// Give Up Fill-Em Up
		for (Integer c : shareLeft.keySet()) {
			Float req = shareLeft.get(c);
			while (req > 1.0 && allocGws.size() > 0) {
				Integer iter = allocGws.keySet().iterator().next();
				if (allocGws.get(iter) > req) {
					allocGws.put(iter, allocGws.get(iter) - req);
					this.fog.getGateways().get(iter).addCluster(this.fog.getClusters().get(c),
							req / this.fog.getGateways().get(iter).getPjCap(), (float) 0.0);
					this.fog.getClusters().get(c).addGateway(this.fog.getGateways().get(iter),
							req / this.fog.getGateways().get(iter).getPjCap(), (float) 0.0);
					req = (float) 0.0;
				} else {
					req = req - allocGws.get(iter);
					this.fog.getGateways().get(iter).addCluster(this.fog.getClusters().get(c),
							allocGws.get(iter) / this.fog.getGateways().get(iter).getPjCap(), (float) 0.0);
					this.fog.getClusters().get(c).addGateway(this.fog.getGateways().get(iter),
							allocGws.get(iter) / this.fog.getGateways().get(iter).getPjCap(), (float) 0.0);
					allocGws.remove(iter);
				}
			}
		}
		// System.out.println(shareLeft);
		// System.out.println(allocGws);
	}

	public void resolveAnomalies() {

		// Delete all rubbish allocations
		// Redistribute things to make sane
		for (Integer g : this.fog.getGateways().keySet()) {
			Iterator<Integer> i = new TreeSet<Integer>(this.fog.getGateways().get(g).getAllClusterShare().keySet())
					.iterator();
			while (i.hasNext()) {
				Integer share = i.next();
				if (this.fog.getGateways().get(g).getAllClusterShare().get(share) < fractioned) {
					this.fog.getGateways().get(g).removeCluster(this.fog.getClusters().get(share));
					this.fog.getClusters().get(share).removeGateway(this.fog.getGateways().get(g));
				}
			}
			Float tot = this.fog.getGateways().get(g).getGwBaseLoad();
			for (Integer c : this.fog.getGateways().get(g).getCluster().keySet()) {
				tot += this.fog.getGateways().get(g).getClusterShare(c);
			}
			//System.out.println("Gw: " + g + " Tot: " + tot);
			if (tot <= 99.9 && tot > this.fog.getGateways().get(g).getGwBaseLoad()) {
				// Gateway needs to be expanded as cluster was deleted
				i = new TreeSet<Integer>(this.fog.getGateways().get(g).getAllClusterShare().keySet()).iterator();
				while (i.hasNext()) {
					Integer share = i.next();
					// Modify by multiplying
					this.fog.getGateways().get(g).modifyCluster(this.fog.getClusters().get(share),
							(float) (100.0 - tot) / this.fog.getGateways().get(g).getCluster().size());
					this.fog.getClusters().get(share).modifyGateway(this.fog.getGateways().get(g),
							(float) (100.0 - tot) / this.fog.getGateways().get(g).getCluster().size());
				}
			} else if (tot < this.fog.getGateways().get(g).getGwBaseLoad() + 1.0) {
				// No Clusters assigned to GW need to assing
				Integer minCls = 0;
				Float min = Float.MAX_VALUE;
				for (Integer c : this.fog.getClusters().keySet()) {
					if (this.fog.getClusters().get(c).ShareRate() < min) {
						min = this.fog.getClusters().get(c).ShareRate();
						minCls = c;
					}
				}
				this.fog.getGateways().get(g).addCluster(this.fog.getClusters().get(minCls),
						(float) (100.0 - this.fog.getGateways().get(g).getGwBaseLoad()), (float) 0.0);
				this.fog.getClusters().get(minCls).addGateway(this.fog.getGateways().get(g),
						(float) (100.0 - this.fog.getGateways().get(g).getGwBaseLoad()), (float) 0.0);
			}
		}
	}

}
