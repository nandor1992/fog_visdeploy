package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Clustering {

	protected Fog fog;
	protected Set<Integer> visited = new HashSet<>();
	protected Set<Integer> noise = new HashSet<>();
	protected List<Set<Integer>> clust = new ArrayList<>();
	protected Map<Integer,List<Integer>> nodes = new HashMap<>();
	
	public Clustering(Fog f){
		this.fog = f;
		this.nodes=getConnections();
	}
	
	public Map<Integer,List<Integer>> getConnections(){
		Map<Integer,List<Integer>> nodes = new HashMap<>();
		for (Integer p: this.fog.getApps().keySet()){
			if (nodes.containsKey(p)){
				nodes.get(p).addAll(this.fog.getApps().get(p).getApps().keySet());
			}else{
				List<Integer> point = new ArrayList<>();
				point.addAll(this.fog.getApps().get(p).getApps().keySet());
				nodes.put(p, point);
				for (Integer pBack: this.fog.getApps().get(p).getApps().keySet()){
					if (nodes.containsKey(pBack)){
						nodes.get(pBack).add(p);
					}else{
						List<Integer> pointBack = new ArrayList<>();
						pointBack.add(p);
						nodes.put(pBack, pointBack);
					}
				}
			}
		}
		for (Integer p: nodes.keySet()){
			List<Integer> deDupStringList = new ArrayList<>(new HashSet<>(nodes.get(p)));
			nodes.put(p, deDupStringList);
			
		}
		//System.out.println(nodes);
		return nodes;
	}
	
	//DBScan Components of Clustering
	
	public List<Set<Integer>> DBScan(Float eps,Integer minPts){
		//System.out.println("Apps:"+this.nodes.keySet());
		this.visited = new HashSet<>();
		this.noise = new HashSet<>();
		this.clust = new ArrayList<>();
		for (Integer p : nodes.keySet()){
			if (!this.visited.contains(p)){
				this.visited.add(p);
				//System.out.println("Neighbourhood Queest: "+p+" Apps: "+nodes.get(p));
				List<Integer> neighbour = getNeighbours(p,eps,0);
				//System.out.println(neighbour);
				if (neighbour.size()<minPts){
					this.noise.add(p);
				}else{
					this.clust.add(expandCluster(p,neighbour, eps,minPts));
				}
			}
		}
		//If there are smaller clusters than minModes they are Noise
		//System.out.println("Initial Allocation");
		//System.out.println("Noise: "+this.noise);
		//System.out.println("Clusters: "+this.clust);
		noiseSort();
		//System.out.println("Noice Cancellation");
		//System.out.println("Clusters: "+this.clust);
		//System.out.println("Small Clusters Removal");
		List<Integer> rems = new ArrayList<>();
		List<Set<Integer>> newClust = new ArrayList<>();
		for (Set<Integer> cls: this.clust){
			if (cls.size()<minPts){
				this.noise.addAll(cls);
			}else{
				newClust.add(cls);
			}
		}
		this.clust=newClust;
		noiseSort();
		//System.out.println("Clusters: "+this.clust);
		//System.out.println("Noise: "+this.noise);
		return this.clust;
	}

	
	public List<Integer> getNeighbours(Integer p, Float eps, Integer lvl){
		//TODO getNeighbour quick link
		List<Integer> neighbour = new ArrayList<>();
		//System.out.println("For Point: "+p+" lvl: "+lvl+" Nodes: "+ this.fog.getApps().get(p).getApps().keySet());
		if (lvl<=eps){
			for (Integer n: this.nodes.get(p)){
				neighbour.addAll(this.getNeighbours(n, eps, lvl+1));
			}
			neighbour.add(p);
		}
		//System.out.println(neighbour);
		return neighbour;
	}
	
	public Set<Integer> expandCluster(Integer p, List<Integer> neighbour, Float eps, Integer minPts){
		Set<Integer> C = new HashSet<>();
		C.add(p);
		CopyOnWriteArrayList<Integer> neighbourSafe = new CopyOnWriteArrayList<Integer>();
		neighbourSafe.addAll(neighbour);
		//System.out.println(neighbourSafe);
		for (Integer n: neighbourSafe){
			if (!this.visited.contains(n)){
				this.visited.add(n);
				List<Integer> neighbourInt=getNeighbours(n, eps, 0);
				if (neighbourInt.size()>=minPts){
					for (Integer n2: neighbourInt){
						if (!neighbourSafe.contains(n2)){
							neighbourSafe.add(n2);
						}
					}
				}
				C.add(n);
			}
		}
		return C;	
	}
	
	
	//OPTICS based Noise Sorting
	public void noiseSort(){	
		Set<Integer> intNoise = new HashSet<>();
		for (Integer p:this.noise){
			Double minDist=Double.POSITIVE_INFINITY;
			Set<Integer> tmpCls=new HashSet<>();
			for (Set<Integer> cls:this.clust){
				Double tmpDist=distanceToCluster(p, cls);
				if (tmpDist<minDist){
					minDist=tmpDist;
					tmpCls=cls;
				}
			}
			if (tmpCls.size()==0){
				//Select random gw, add to it
				if (this.clust.size()!=0){
					Random rnd = new Random();
					int r = rnd.nextInt(this.clust.size()-1);
					System.out.println("No cls found, add: "+p+" to: "+this.clust.get(r));
					this.clust.get(r).add(p);
				}else{
				intNoise.add(p);
				}
			}else{
			tmpCls.add(p);
			}
		}
		this.noise=intNoise;
	}
	
	public double distanceToCluster(Integer p,Set<Integer> clustP){
		//TODO DistanceToClusterMethod
		double avgDist = 0.0;
		double pCount = 0;
		for (Integer p2 : clustP){
			Double distPP2 = dijkstraSearsch(p, p2);
			if (distPP2!=null){
				pCount++;
				avgDist+=distPP2;
			}
		}
		if ( pCount==0){
			return 0.0;
		}
		else{
			return avgDist/pCount;
		}
	}
	
	
	public Double dijkstraSearsch(Integer p,Integer p2){
		//Init
		List<Integer> queue = new ArrayList<>();
		Map<Integer,Integer> prev = new HashMap<>();
		Map<Integer,Double> dist = new HashMap<>();
		for (Integer i: this.nodes.keySet()){
			dist.put(i, Double.POSITIVE_INFINITY);
			prev.put(i, null);
			queue.add(i);
		}
		dist.put(p,0.0);		
		while (queue.size()>0){
			//System.out.println(queue);
			//System.out.println(dist);
			Integer u = getShortestDist(dist,queue);
			if (u!=null){
				//System.out.println(u);
				queue.remove(queue.indexOf(u));
				for (Integer i: this.nodes.get(u)){
					Double newDist = dist.get(u)+1.0;
					if (newDist < dist.get(i)){
						dist.put(i,newDist);
						prev.put(i, u);
					}
				}
			}else{
				if (dist.get(p2)!=Double.POSITIVE_INFINITY){
					return dist.get(p2);
				}else{
					return null;
				}
			}
		}
		return dist.get(p2);
	}
	
	public Integer getShortestDist(Map<Integer,Double> dist,List<Integer> queue){
		Double min = Double.POSITIVE_INFINITY;
		Integer minI = null;
		for (Integer i: queue){
			if (dist.get(i)<min){
				minI=i;
				min=dist.get(i);
			}
		}
		return minI;
	}
	
	public Double getShortestDistVal(Map<Integer,Double> dist,List<Integer> queue){
		Double min = Double.POSITIVE_INFINITY;
		Integer minI = null;
		for (Integer i: queue){
			if (dist.get(i)<min){
				minI=i;
				min=dist.get(i);
			}
		}
		return min;
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
	
	public Map<Integer, Double> dijkstraSearsch(Integer p, Float maxDist) {
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
			Double dist1 = getShortestDistVal(dist, queue);
			if (u != null & dist1<=maxDist) {
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
	
}
