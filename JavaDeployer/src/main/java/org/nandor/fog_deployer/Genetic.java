package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

public class Genetic {
	
	protected Fog fog;
	protected Cluster cls;
	protected int gwCount;
	protected int AppCnt;
	protected double randPop = 0.4;
	protected double elitPop = 0.2;
	protected double crossPop = 0.2;
	protected double mutPop = 0.2;
	protected double mutChance  = 0.1;
	protected Map<Integer, Integer> bestIndi = new HashMap<>();
	private List<Map<Integer, Integer>> bestGens = new ArrayList<>();
	private Map<Integer,List<Map<Integer, Integer>>> bestCGens = new HashMap<>();
	
	public Genetic(Fog fog){
		this.fog=fog;
		this.gwCount=this.fog.getGateways().size();
		this.AppCnt=this.fog.getApps().size();
	}
	/*
	 * Methods Created for Cluster wide Exhaustive Deplyment
	 */
	
	public Map<Integer,Integer> ExhaustiveCluster( Cluster c) {
		//TODO Exhaustive Easy to find
		Set<Integer> gwIDs =c.getGateways().keySet();
		Set<Integer> appIDs =c.getApps().keySet();
		System.out.println("-------Cluster Search: "+c.getId()+" Apps: "+appIDs+" Gws: "+gwIDs);
		//System.out.println(gwIDs);
		//System.out.println(appIDs);
		Map<Integer,Integer> randInd = this.initFirstGen(new ArrayList<Integer>(gwIDs), new ArrayList<Integer>(appIDs));
		Map<Integer,Integer> bestGen = new HashMap<Integer,Integer>(randInd);
		Long cnt = (long) 0;
		Float util = (float)0.0;
		while (randInd!=null){
			cnt++;
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(randInd);
			if (c.verifyIndValidity()==0){
				if (c.getClusterCompoundUtility()>util){
					util=c.getClusterCompoundUtility();
					bestGen=new HashMap<Integer,Integer>(randInd);
					//System.out.println("New Best Gen: "+cnt+" Util: "+util+" Gen: "+bestGen);
				}
			}
			if (cnt % 10000000==0){
				System.out.println("Count: "+cnt+" Gen: "+randInd);
			}
			randInd =  nextGeneration(new ArrayList<Integer>(gwIDs),randInd);
		}
		this.fog.clearAppToGws();
		this.fog.AssignAppsToGws(bestGen);
		System.out.println("The best of the Population "+cnt+" for Cls: "+c.getId()+" is: "+c.getClusterCompoundUtility()+" With: "+bestGen);
		return bestGen;
	}
	
	public Map<Integer,Integer> ExhaustiveAlloc() {
		//TODO Exhaustive Easy to find
		Set<Integer> gwIDs =this.fog.getGateways().keySet();
		Set<Integer> appIDs =this.fog.getApps().keySet();
		System.out.println("-------Full Search Apps: "+appIDs+" Gws: "+gwIDs);
		//System.out.println(gwIDs);
		//System.out.println(appIDs);
		Map<Integer,Integer> randInd = this.initFirstGen(new ArrayList<Integer>(gwIDs), new ArrayList<Integer>(appIDs));
		Map<Integer,Integer> bestGen = new HashMap<Integer,Integer>(randInd);
		Long cnt = (long) 0;
		Float util = (float)0.0;
		long start=System.currentTimeMillis();
		while (randInd!=null){
			cnt++;
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(randInd);
			if (this.fog.verifyIndValidity()==0){
				if (this.fog.getFogCompoundUtility()>util){
					util=this.fog.getFogCompoundUtility();
					bestGen=new HashMap<Integer,Integer>(randInd);
					System.out.println("New Best Gen: "+cnt+" Util: "+util+" Gen: "+bestGen);
				}
			}
			if (cnt % 500000==0){
				System.out.println("Count: "+cnt+" Gen: "+randInd+" Util:"+this.fog.getFogCompoundUtility());
			}
			randInd =  nextGeneration(new ArrayList<Integer>(gwIDs),randInd);
		}
		this.fog.clearAppToGws();
		this.fog.AssignAppsToGws(bestGen);
		System.out.println("The best of the Population "+cnt+" is: "+this.fog.getFogCompoundUtility()+" With: "+bestGen);
		return bestGen;
	}
	
	public Map<Integer,Integer> initFirstGen(ArrayList<Integer> gws, ArrayList<Integer> apps){
		Integer first = gws.get(0);
		Map<Integer,Integer> gen = new HashMap<>();
		for (Integer a: apps){
			gen.put(a, first);
		}
		return gen;
	}
	
	public Map<Integer,Integer> nextGeneration(ArrayList<Integer> gws, Map<Integer,Integer> gen){
		Integer first = gws.get(0);
		Integer last = gws.get(gws.size()-1);
		Integer iter = 0;
		for (Integer g: gen.keySet()){
			if (gen.get(g)==last){
				gen.put(g, first);
				iter++;
			}else{
				gen.put(g, gws.get(gws.indexOf(gen.get(g))+1));
				break;
			}
		}
		if (iter == gen.size()){
			return null;
		}else{
			return gen;
		}
	}
	
	/*
	 * Methods Created for Cluster Wide Deployment based on Gateways available for Cluster
	 */	
	
	public Map<Integer,Integer> GACluster(int size, int generations, Cluster c,boolean safe,int max) {
		
		//TODO CLustering easy to Find
		int gwCnt = c.getGateways().size();
		int appCnt = c.getApps().size();
		this.fog.clearAppToGws();
		Set<Integer> gwIDs =c.getGateways().keySet();
		Set<Integer> appIDs =c.getApps().keySet();
		System.out.println(" -> GA Cluster "+c.getId()+" GwCount:" + gwCnt + " AppCnt:" +appCnt+ " Size:"+size+" Gens:"+generations);
		// Ga Parameters
		float bestUtil = (float)0.0;
		//System.out.println("----- Initializing new Population for Cluster "+c.getId()+" -----");
		List<Map<Integer, Integer>> pop = randomPop(size,gwIDs,appIDs,c); 
		int i=0;
		int bestI = 0;
		int sinceLastBest = 0;
		while (sinceLastBest<generations || (safe  && bestUtil==0.0 )) {
			i++;
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBestGens(pop, (int)(size*elitPop),c,false);
			//System.out.print("Best: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop),c));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop),c.getGateways().keySet(),c));
			//System.out.print("Mutate: "+newpop.size());
			newpop.addAll(randomPop((int)(size-newpop.size()),gwIDs,appIDs,c));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop,c);
			if (newUtil>bestUtil){
				bestUtil=newUtil;
				bestI = i;
				sinceLastBest=0;
				//System.out.println("The best of the Population "+i+" is: "+bestUtil+" With: "+this.bestIndi);
			}else{sinceLastBest++;}
			pop=newpop;
			if (i % max == 0){
				if (bestUtil!=0.0){
					break;
				}else{
					System.out.println("Ga Failed at pop "+i+" !");
					this.fog.clearAppToGws();
					this.fog.AssignAppsToGws(pop.get(0));
					System.out.println(pop.get(0));
					c.verifyValidityVerbose();
					return null;
				}
				
			}
		}
		if (bestUtil!=0){
			this.bestCGens.put(c.getId(),getBestGens(pop, (int)(size*elitPop),c,true));
			System.out.println("The best of the Population "+bestI+" is: "+bestUtil);//+" With: "+this.bestIndi
			return this.bestIndi;
		}else{
			System.out.println("Ga Failed at pop "+i+" !");
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(pop.get(0));
			System.out.println(pop.get(0));
			//c.verifyValidityVerbose();
			return null;
		}
	}
	
	
	public Map<Integer,Integer> GACluster(int size, int count, Cluster c,boolean safe) {
		
		int gwCnt = c.getGateways().size();
		int appCnt = c.getApps().size();
		this.fog.clearAppToGws();
		Set<Integer> gwIDs =c.getGateways().keySet();
		Set<Integer> appIDs =c.getApps().keySet();
		System.out.println("GA Cluster GwCount: " + gwCnt + " AppCnt: " +appCnt);
		// Ga Parameters
		float bestUtil = (float)0.0;
		System.out.println("----- Initializing new Population for Cluster "+c.getId()+" -----");
		List<Map<Integer, Integer>> pop = randomPop(size,gwIDs,appIDs,c); 
		int i=0;
		int bestI = 0;
		while (i < count || (!safe || bestUtil==0.0 )) {
			i++;
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBest(pop, (int)(size*elitPop),c);
			//System.out.print("Best: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop),c));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop),c.getGateways().keySet(),c));
			//System.out.print("Mutate: "+newpop.size());
			newpop.addAll(randomPop((int)(size-newpop.size()),gwIDs,appIDs,c));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop,c);
			if (newUtil>bestUtil){
				bestUtil=newUtil;
				bestI = i;
				System.out.println("The best of the Population "+i+" is: "+bestUtil+" With: "+this.bestIndi);
			}
			pop=newpop;
			if (i % 1000 == 0){
				System.out.println("Pop "+pop.toString());
				this.fog.clearAppToGws();
				this.fog.AssignAppsToGws(pop.get(0));
				System.out.println(pop.get(0));
				c.verifyValidityVerbose();
				break;
			}
		}
		this.bestCGens.put(c.getId(),getBestGens(pop, 5,c,true));
		System.out.println("The best of the Population "+bestI+" is: "+bestUtil+" With: "+this.bestIndi);
		return this.bestIndi;
	}
	
	//Return Results
	public List<Map<Integer, Integer>> getBestClsGens(){
		List<Map<Integer, Integer>> ret = new ArrayList<>();
		int size = 0;
		//Get Max value from all :-??
		for (Integer c:this.bestCGens.keySet()){
			if (this.bestCGens.get(c).size()>size){
				size = this.bestCGens.get(c).size();
			}
		}
		//Create one deployment from all of these 
		int i =0;
		while (i<size){
			Map<Integer, Integer> tmp = new HashMap<>();
			for (Integer c:this.bestCGens.keySet()){
				if (i>this.bestCGens.get(c).size()-1){
					tmp.putAll(this.bestCGens.get(c).get(this.bestCGens.get(c).size()-1));
				}else{
					tmp.putAll(this.bestCGens.get(c).get(i));
				}
			}
			i++;
			ret.add(tmp);
		}
		return ret;
	}
	
	//Cross Version 2
	private List<Map<Integer,Integer>> crossingPop(List<Map<Integer,Integer>> pop, int i,Cluster c) {
		List<Map<Integer,Integer>> ret = new ArrayList<>();
		Random rand = new Random();
		List<Map<Integer,Integer>> basepop = getBestGens(pop, 2*i,c,false);
		Iterator<Map<Integer, Integer>> iter = basepop.iterator();
		while( iter.hasNext()){
			Map<Integer,Integer> indi = iter.next();
			if (iter.hasNext()){
				Map<Integer,Integer> indi2 = iter.next();
				Map<Integer,Integer> newIndi = new HashMap<>();
				for (int j: indi.keySet()){
					newIndi.put(j,  rand.nextBoolean() ? indi.get(j) : indi2.get(j));
				}
				//System.out.println("Crossing");
				//System.out.println(indi);
				//System.out.println(indi2);
				//System.out.println(newIndi);
				ret.add(newIndi);
			}
			else{
				ret.add(indi);
			}
		}
		return ret;
	}
	
	
	//Mute Version 2
	private List<Map<Integer,Integer>> mutatePop(List<Map<Integer,Integer>> pop, int i,Set<Integer> gw,Cluster c) {
		List<Map<Integer,Integer>> ret = new ArrayList<>();
		Random rand = new Random();
		List<Integer> gws = new ArrayList<Integer>(gw);
		List<Map<Integer,Integer>> basepop = getBestGens(pop, i,c,false);
		Iterator<Map<Integer, Integer>> iter = basepop.iterator();
		while( iter.hasNext()){
			Map<Integer,Integer> indi = iter.next();
			Map<Integer,Integer> newIndi = new HashMap<>();
			for (int j: indi.keySet()){
				if (rand.nextFloat()<mutChance){
					Collections.shuffle(gws);
					newIndi.put(j,gws.get(0));
				}else{
					newIndi.put(j, indi.get(j));
				}
			}
			//System.out.println("Mutation");
			//System.out.println(indi);
			//System.out.println(newIndi);
			ret.add(newIndi);
		}
		return ret;
	}
	
	//Get Best Stuff
	public Float getBest(List<Map<Integer,Integer>> pop,Cluster c){
		Float best = (float)0.0;
		Map<Integer,Integer> bestIndi = new HashMap<>();
		Iterator<Map<Integer, Integer>> iter = pop.iterator();
		while( iter.hasNext()){
			Map<Integer,Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			if (c.verifyIndValidity()==0){
				float tmp = c.getClusterCompoundUtility();
				if (tmp>best){
					best=tmp;
					bestIndi = indi;
				}
			}
		}
		this.bestIndi=bestIndi;
		return best;
	}
	
	public List<Map<Integer, Integer>> getBestGens(List<Map<Integer, Integer>> pop, int size,Cluster c, boolean onlyValid) {
		Iterator<Map<Integer, Integer>> iter = pop.iterator();
		genStack g1 = new genStack(size);
		/// All the gens that are valid
		while (iter.hasNext()) {
			Map<Integer, Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = c.getClusterCompoundUtility();
			if (c.verifyIndValidity()==0) {
				g1.addGeneration(indi, tmp);
			}
		}
		this.bestIndi = g1.getBest();
		List<Map<Integer, Integer>> ret = g1.getGenerations();
		// Get the generations that are not valid but we need something
		if (!onlyValid) {
			iter = pop.iterator();
			genStack g2 = new genStack(size - g1.generations.size());
			/// All the gens that are valid
			while (iter.hasNext() && size > g1.generations.size()) {
				Map<Integer, Integer> indi = iter.next();
				if (!g1.isUnique(indi)){
					this.fog.clearAppToGws();
					this.fog.AssignAppsToGws(indi);
					float tmp = c.getClusterCompoundUtility();
					//If it breaks here you are using valid ones which would be odd
					float viol = (float)c.verifyIndValidity();
					g2.addGeneration(indi, tmp/viol);
				}
			}
			ret.addAll(g2.getGenerations());
		}
		return ret;
	}
	
	public List<Map<Integer, Integer>> getBest(List<Map<Integer, Integer>> pop, int size,Cluster c) {
		List<Map<Integer, Integer>> ret = new ArrayList<>();
		Map<Integer, Float> stack = new HashMap<>();
		Map<Integer, Float> stack2 = new HashMap<>();
		Iterator<Map<Integer, Integer>> iter = pop.iterator();
		int retS = 0;
		while (iter.hasNext() && retS < size) {
			Map<Integer, Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = c.getClusterCompoundUtility();
			if (c.verifyIndValidity()==0) {
				//System.out.println(tmp);
				stack.put(pop.indexOf(indi), tmp);
				retS += 1;
			}
		}
		// System.out.println("Stack:"+stack);
		while (iter.hasNext()) {
			Map<Integer, Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			if (c.verifyIndValidity()==0) {
				// System.out.println(tmp);
				float tmp = c.getClusterCompoundUtility();
				Entry<Integer, Float> min = null;
				for (Entry<Integer, Float> entry : stack.entrySet()) {
					if (min == null || min.getValue() > entry.getValue()) {
						min = entry;
					}
				}
				if( min != null){
					if (min.getValue() < tmp) {
						stack.remove(min.getKey());
						stack.put(pop.indexOf(indi), tmp);
					}
				}

			}
		}
		Iterator<Map<Integer, Integer>> iter2 = pop.iterator();
		int retS2 = size - retS;
		while (iter2.hasNext() && retS2 > stack2.size()) {
			Map<Integer, Integer> indi = iter2.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (!stack.containsValue(pop.indexOf(indi))) {
				// System.out.println(tmp);
				stack2.put(pop.indexOf(indi), tmp);
			}
		}
		while (iter2.hasNext()&& retS2>0) {
			Map<Integer, Integer> indi = iter2.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (!stack.containsValue(pop.indexOf(indi))) {
				// System.out.println(tmp);
				Entry<Integer, Float> min = null;
				for (Entry<Integer, Float> entry : stack2.entrySet()) {
					if (min == null || min.getValue() > entry.getValue()) {
						min = entry;
					}
				}
				if (min.getValue() < tmp) {
					stack2.remove(min.getKey());
					stack2.put(pop.indexOf(indi), tmp);
				}

			}
		}
		// System.out.println("Stack:"+stack);
		for (Integer i : stack.keySet()) {
			ret.add(pop.get(i));
		}
		for (Integer i : stack2.keySet()) {
			ret.add(pop.get(i));
		}
		return ret;
	}
	
	
	
	//Random Generate Stuff
	public Map<Integer,Integer> randomInd(Set<Integer> gw, Set<Integer> app){
		Map<Integer,Integer> gen = new HashMap<>();
		Random rand = new Random();
		List<Integer> gws = new ArrayList<Integer>(gw);
		for (Integer i:app){
			Collections.shuffle(gws);
			gen.put(i,gws.get(0));
		}
		return gen;		
	}
	
	public Map<Integer,Integer> randomInd(Set<Integer> gw, Set<Integer> app, Cluster c){
		Map<Integer,Integer> gen = new HashMap<>();
		Random rand = new Random();
		List<Integer> gws = new ArrayList<Integer>(gw);
		for (Integer i:app){
			Collections.shuffle(gws);
			gen.put(i,gws.get(0));
		}
		this.fog.clearAppToGws();
		this.fog.AssignAppsToGws(gen);
		while (!(c.verifyIndValidity()==0)){
			this.fog.clearAppToGws();
			gen = new HashMap<>();
			for (Integer i:app){
				Collections.shuffle(gws);
				gen.put(i,gws.get(0));
			}
			this.fog.AssignAppsToGws(gen);
		}
		return gen;		
	}
	
	
	public List<Map<Integer,Integer>> randomPop(int size,Set<Integer> gw, Set<Integer> app,Cluster c){
		List<Map<Integer,Integer>> pop = new ArrayList<>();
		for (int i=0; i<size;i++){
			pop.add(randomInd(gw,app));
		}
		return pop;
	}
	
	
	
	
	
	/*
	 * This is the part that take care of the Global optimization, most methods are created for this
	 */
	public Map<Integer, Integer> GAGlobal(int size, int count,boolean safe) {
		System.out.println("GA Global GwCount: " + this.gwCount + " AppCnt: " + this.AppCnt);
		long start=System.currentTimeMillis();
		// Ga Parameters
		//TODO Easy to find Global
		this.fog.clearAppToGws();
		float bestUtil = (float)0.0;
		System.out.println("----- Initializing new Population for Global GA -----");
		List<Map<Integer, Integer>> pop = randomPop(size);
		int i=0;
		int bestI=0;
		while (i < count || (!safe || bestUtil==0.0 )) {
			i++;
			if (i % (size*10) == 0){
				System.out.println("GA Global Failed");
				this.fog.clearAppToGws();
				this.fog.AssignAppsToGws(pop.get(0));
				System.out.println(pop.get(0));
				System.out.println(this.fog.verifyValidityVerbose());
				//this.fog.clearAppToGws();
				break;
			}
			//System.out.println("Pop "+pop.toString());
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBestGens(pop, (int)(size*elitPop),false);
			System.out.print("Best: "+newpop.size());
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop)));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop)));
			//System.out.print("Mutate: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			newpop.addAll(randomPop((int)(size-newpop.size())));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop);
			if (newUtil>bestUtil){
				bestUtil=newUtil;
				bestI=i;
				//" With: "+this.bestIndi+
				System.out.println("The best of the Population "+i+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
			}
			pop=newpop;
		}
		if (bestUtil!=0){
			this.bestGens  = getBestGens(pop, 20,true);
			System.out.println("The best of the Population "+bestI+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
			return this.bestIndi;
		}else{
			System.out.println("GA Global Failed");
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(pop.get(0));
			System.out.println(pop.get(0));
			System.out.println(this.fog.verifyValidityVerbose());
			//this.fog.clearAppToGws();
			return null;
		}
	}
	
	public Map<Integer,Integer> GAGlobal(int size,int generations,boolean safe,int max,DataGatherer dg) {
		System.out.println("GA Global GwCount:" + this.gwCount + " AppCnt:" + this.AppCnt + " Size:"+size+" Gens:"+generations);
		long start=System.currentTimeMillis();
		float prevBestUtil =(float) 0.0;
		// Ga Parameters
		//TODO Easy to find Global
		this.fog.clearAppToGws();
		float bestUtil = (float)0.0;
		int sinceLastBest = 0;
		//System.out.println("----- Initializing new Population for Global GA -----");
		List<Map<Integer, Integer>> pop = randomPop(size);
		int i=0;
		int bestI=0;
		//while (sinceLastBest<generations || (!safe || bestUtil==0.0 )) {
		while (sinceLastBest<generations || (safe  && bestUtil==0.0 )) {
			i++;
			if (i % max == 0){
				if (bestUtil!=0.0){
					break;
				}else{
					System.out.println("------ GA Global Failed ------");
					this.fog.clearAppToGws();
					this.fog.AssignAppsToGws(pop.get(0));
					System.out.println(pop.get(0));
					System.out.println(this.fog.verifyValidityVerbose());
					//Methods.displayClsAndRes(this.fog);
					//this.fog.clearAppToGws();
					break;
				}
			}
			//System.out.println("Pop "+pop.toString());
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBestGens(pop, (int)(size*elitPop),false);
			//System.out.println("Best Size: "+newpop.size()+" When needed: "+(size*elitPop));
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop)));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop)));
			//System.out.print("Mutate: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			newpop.addAll(randomPop((int)(size-newpop.size())));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop);
			if (newUtil>bestUtil){
				bestUtil=newUtil;
				bestI=i;
				sinceLastBest=0;
				//" With: "+this.bestIndi+
				if (bestUtil>prevBestUtil+0.1){
					prevBestUtil=bestUtil;
					System.out.println("The best of the Population "+i+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
				}	
			}else{
				sinceLastBest++;
			}
			if ((i%10==0 && (i >= 10))||i==1){
				dg.addUtility(bestUtil);
				dg.addIteration(i);
				dg.addTime((System.currentTimeMillis()-start)/(float)1000);
			}
			pop=newpop;
		}
		this.bestGens  = getBestGens(pop, 20,true);
		System.out.println("Best of "+i+"  Population was at: "+bestI+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
		
		return pop.get(0);
	}
	
	public float GAGlobal(int size,Double value) {
		System.out.println("GA Global GwCount:" + this.gwCount + " AppCnt:" + this.AppCnt + " Size:"+size+ " Ref: "+value);
		long start=System.currentTimeMillis();
		float prevBestUtil =(float) 0.0;
		int max = 10000;
		// Ga Parameters
		//TODO Easy to find Global
		this.fog.clearAppToGws();
		float bestUtil = (float)0.0;
		int sinceLastBest = 0;
		//System.out.println("----- Initializing new Population for Global GA -----");
		List<Map<Integer, Integer>> pop = randomPop(size);
		int i=0;
		int bestI=0;
		//while (sinceLastBest<generations || (!safe || bestUtil==0.0 )) {
		while (bestUtil<value) {
			i++;
			if (i % max == 0){
				if (bestUtil!=0.0){
					break;
				}else{
					System.out.println("------ GA Global Failed ------");
					this.fog.clearAppToGws();
					this.fog.AssignAppsToGws(pop.get(0));
					System.out.println(pop.get(0));
					System.out.println(this.fog.verifyValidityVerbose());
					//Methods.displayClsAndRes(this.fog);
					//this.fog.clearAppToGws();
					return (float)0.0;
				}
			}
			//System.out.println("Pop "+pop.toString());
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBestGens(pop, (int)(size*elitPop),false);
			//System.out.println("Best Size: "+newpop.size()+" When needed: "+(size*elitPop));
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop)));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop)));
			//System.out.print("Mutate: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			newpop.addAll(randomPop((int)(size-newpop.size())));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop);
			if (newUtil>bestUtil){
				bestUtil=newUtil;
				bestI=i;
				sinceLastBest=0;
				//" With: "+this.bestIndi+
				if (bestUtil>prevBestUtil+0.5){
					prevBestUtil=bestUtil;
					System.out.println("The best of the Population "+i+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
				}
			}else{
				sinceLastBest++;
			}
			pop=newpop;
		}
		this.bestGens  = getBestGens(pop, 20,true);
		System.out.println("Best of "+i+"  Population was at: "+bestI+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
		this.fog.setDeplpyment(pop.get(0));
		return (System.currentTimeMillis()-start)/(float)1000;
	}
	
	public List<Map<Integer, Integer>> getBestGens(){
		return bestGens;
	}
	
	/*
	  Map<Integer, Integer> rand = randomInd();
			System.out.println(rand);
			this.fog.AssignAppsToGws(rand);
			System.out.println("Fog Utility: " + this.fog.getFogCompoundUtility());
			System.out.println("Fog Delay: " + this.fog.getFogCompoundDelay());
			System.out.println("Fog Reliability: " + this.fog.getFogCompoundReliability());
	 */
	
	
	public Map<Integer, Float> GAGlobalEndCnd(int size, int count) {
		System.out.println("GA Global GwCount:" + this.gwCount + " AppCnt:" + this.AppCnt + " Size:"+size+" Gens:"+count);
		long start=System.currentTimeMillis();
		// Ga Parameters
		this.fog.clearAppToGws();
		float bestUtil = (float)0.0;
		float prevBestUtil = (float)0.0;
		List<Map<Integer, Integer>> pop = randomPop(size);
		int i=0;
		int bestI=0;
		Map<Integer,Float> bests = new HashMap<>();
		while (i < count ) {
			i++;
			//System.out.println("Pop "+pop.toString());
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBestGens(pop, (int)(size*elitPop),false);
			//System.out.print("Best: "+newpop.size());
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop)));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop)));
			//System.out.print("Mutate: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			newpop.addAll(randomPop((int)(size-newpop.size())));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop);
			if (newUtil>bestUtil){
				if (newUtil>prevBestUtil+0.1){
					System.out.println("The best of the Population "+i+" is: "+newUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);	
					prevBestUtil = newUtil;
				}
				bestUtil=newUtil;
				bestI=i;
				//" With: "+this.bestIndi+
				}
			if ((i%10==0 && (i >= 10))||i==1){
				bests.put(i, bestUtil);
			}
			pop=newpop;
		}
		if (bestUtil!=0){
			bests.put(i, bestUtil);
			this.bestGens  = getBestGens(pop, 20,true);
			System.out.println("The best of the Population "+bestI+" is: "+bestUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
			return bests;
		}else{
			System.out.println("GA Global Failed");
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(pop.get(0));
			System.out.println(pop.get(0));
			System.out.println(this.fog.verifyValidityVerbose());
			//this.fog.clearAppToGws();
			return null;
		}
	}
	
	public Float GAGlobalSize(int size, int count) {
		System.out.println("GA Global GwCount:" + this.gwCount + " AppCnt:" + this.AppCnt + " Size:"+size+" Gens:"+count);
		long start=System.currentTimeMillis();
		// Ga Parameters
		this.fog.clearAppToGws();
		float bestUtil = (float)0.0;
		float prevBestUtil = (float)0.0;
		List<Map<Integer, Integer>> pop = randomPop(size);
		int i=0;
		int bestI=0;
		float iniBUtil = (float)0.0;
		while (i < count ) {
			i++;
			//System.out.println("Pop "+pop.toString());
			//System.out.print("Loop Count " + i + ": ");
			//System.out.println("----- Get Elite Population -----");
			List<Map<Integer, Integer>> newpop = getBestGens(pop, (int)(size*elitPop),false);
			//System.out.print("Best: "+newpop.size());
			//System.out.println("----- Get Crossover Population -----");
			newpop.addAll(crossingPop(pop,(int)(size*crossPop)));
			//System.out.print("Cross: "+newpop.size());
			//System.out.println("----- Get Mutated Population -----");
			newpop.addAll(mutatePop(pop,(int)(size*mutPop)));
			//System.out.print("Mutate: "+newpop.size());
			//System.out.println("----- Initializing new Population -----");
			newpop.addAll(randomPop((int)(size-newpop.size())));
			//System.out.println("Tot: "+newpop.size());
			float newUtil=getBest(newpop);
			if (newUtil>bestUtil){
				if (bestUtil==0.0){
					iniBUtil = newUtil;
				}
				if (newUtil>prevBestUtil+0.1){
					System.out.println("The best of the Population "+i+" is: "+newUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);	
					prevBestUtil = newUtil;
				}
				bestUtil=newUtil;
				//" With: "+this.bestIndi+
				}
			pop=newpop;
		}
		if (bestUtil!=0){
			this.bestGens  = getBestGens(pop, 20,true);
			System.out.println("The best of the Population "+bestI+" is: "+bestUtil+"Init:"+iniBUtil+" At: "+(System.currentTimeMillis()-start)/(float)1000);
			return (bestUtil-iniBUtil)/this.fog.getApps().size();
		}else{
			System.out.println("GA Global Failed");
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(pop.get(0));
			System.out.println(pop.get(0));
			System.out.println(this.fog.verifyValidityVerbose());
			//this.fog.clearAppToGws();
			return null;
		}
	}
	
	private List<Map<Integer,Integer>> crossingPop(List<Map<Integer,Integer>> pop, int i) {
		List<Map<Integer,Integer>> ret = new ArrayList<>();
		Random rand = new Random();
		List<Map<Integer,Integer>> basepop = getBest(pop,2*i);
		Iterator<Map<Integer, Integer>> iter = basepop.iterator();
		while( iter.hasNext()){
			Map<Integer,Integer> indi = iter.next();
			if (iter.hasNext()){
				Map<Integer,Integer> indi2 = iter.next();
				Map<Integer,Integer> newIndi = new HashMap<>();
				for (int j: indi.keySet()){
					newIndi.put(j,  rand.nextBoolean() ? indi.get(j) : indi2.get(j));
				}
				//System.out.println("Crossing");
				//System.out.println(indi);
				//System.out.println(indi2);
				//System.out.println(newIndi);
				ret.add(newIndi);
			}
			else{
				ret.add(indi);
			}
		}
		return ret;
	}

	private List<Map<Integer,Integer>> mutatePop(List<Map<Integer,Integer>> pop, int i) {
		List<Map<Integer,Integer>> ret = new ArrayList<>();
		Random rand = new Random();
		List<Map<Integer,Integer>> basepop = getBest(pop,i);
		Iterator<Map<Integer, Integer>> iter = basepop.iterator();
		while( iter.hasNext()){
			Map<Integer,Integer> indi = iter.next();
			Map<Integer,Integer> newIndi = new HashMap<>();
			for (int j: indi.keySet()){
				if (rand.nextFloat()<mutChance){
					newIndi.put(j,rand.nextInt(this.gwCount)+1);
				}else{
					newIndi.put(j, indi.get(j));
				}
			}
			//System.out.println("Mutation");
			//System.out.println(indi);
			//System.out.println(newIndi);
			ret.add(newIndi);
		}
		return ret;
	}
	
	public Float getBest(List<Map<Integer,Integer>> pop){
		Float best = (float)0.0;
		Map<Integer,Integer> bestIndi = new HashMap<>();
		Iterator<Map<Integer, Integer>> iter = pop.iterator();
		while( iter.hasNext()){
			Map<Integer,Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (tmp>best && this.fog.verifyIndValidity()==0){
				best=tmp;
				bestIndi = indi;
			}
		}
		this.bestIndi=bestIndi;
		return best;
	}
	
	public List<Map<Integer, Integer>> getBestGens(List<Map<Integer, Integer>> pop, int size, boolean onlyValid) {
		Iterator<Map<Integer, Integer>> iter = pop.iterator();
		genStack g1 = new genStack(size);
		/// All the gens that are valid
		while (iter.hasNext()) {
			Map<Integer, Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (this.fog.verifyIndValidity()==0) {
				g1.addGeneration(indi, tmp);
			}
		}
		this.bestIndi = g1.getBest();
		List<Map<Integer, Integer>> ret = g1.getGenerations();
		// Get the generations that are not valid but we need something
		if (!onlyValid) {
			iter = pop.iterator();
			genStack g2 = new genStack(size - g1.generations.size());
			/// All the gens that are valid
			while (iter.hasNext() && size > g1.generations.size()) {
				Map<Integer, Integer> indi = iter.next();
				if (!g1.isUnique(indi)){
					this.fog.clearAppToGws();
					this.fog.AssignAppsToGws(indi);
					float tmp = this.fog.getFogCompoundUtility();
					float viol = this.fog.verifyIndValidity();
					//If fails you are using the wrong data
					g2.addGeneration(indi, tmp/viol);
				}
			}
			ret.addAll(g2.getGenerations());
		}
		return ret;
	}
	
	public List<Map<Integer, Integer>> getBest(List<Map<Integer, Integer>> pop, int size) {
		List<Map<Integer, Integer>> ret = new ArrayList<>();
		Map<Integer, Float> stack = new HashMap<>();
		Map<Integer, Float> stack2 = new HashMap<>();
		Iterator<Map<Integer, Integer>> iter = pop.iterator();
		int retS = 0;
		while (iter.hasNext() && retS < size) {
			Map<Integer, Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (this.fog.verifyIndValidity()==0) {
				// System.out.println(tmp);
				stack.put(pop.indexOf(indi), tmp);
				retS += 1;
			}
		}
		// System.out.println("Stack:"+stack);
		while (iter.hasNext()) {
			Map<Integer, Integer> indi = iter.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (this.fog.verifyIndValidity()==0) {
				// System.out.println(tmp);
				Entry<Integer, Float> min = null;
				for (Entry<Integer, Float> entry : stack.entrySet()) {
					if (min == null || min.getValue() > entry.getValue()) {
						min = entry;
					}
				}
				if (min.getValue() < tmp) {
					stack.remove(min.getKey());
					stack.put(pop.indexOf(indi), tmp);
				}

			}
		}
		Iterator<Map<Integer, Integer>> iter2 = pop.iterator();
		int retS2 = size - retS;
		while (iter2.hasNext() && retS2 > stack2.size()) {
			Map<Integer, Integer> indi = iter2.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (!stack.containsValue(pop.indexOf(indi))) {
				// System.out.println(tmp);
				stack2.put(pop.indexOf(indi), tmp);
			}
		}
		while (iter2.hasNext()&& retS2>0) {
			Map<Integer, Integer> indi = iter2.next();
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(indi);
			float tmp = this.fog.getFogCompoundUtility();
			if (!stack.containsValue(pop.indexOf(indi))) {
				// System.out.println(tmp);
				Entry<Integer, Float> min = null;
				for (Entry<Integer, Float> entry : stack2.entrySet()) {
					if (min == null || min.getValue() > entry.getValue()) {
						min = entry;
					}
				}
				if (min.getValue() < tmp) {
					stack2.remove(min.getKey());
					stack2.put(pop.indexOf(indi), tmp);
				}

			}
		}
		// System.out.println("Stack:"+stack);
		for (Integer i : stack.keySet()) {
			ret.add(pop.get(i));
		}
		for (Integer i : stack2.keySet()) {
			ret.add(pop.get(i));
		}
		return ret;
	}
	
	public Map<Integer,Integer> randomInd(){
		Map<Integer,Integer> gen = new HashMap<>();
		Random rand = new Random();
		for (int i=1;i<=AppCnt;i++){
			gen.put(i,rand.nextInt(this.gwCount)+1);
		}
		return gen;		
	}
	
	public Map<Integer,Integer> randomInd(boolean safe){
		Map<Integer,Integer> gen = new HashMap<>();
		Random rand = new Random();
		for (int i=1;i<=AppCnt;i++){
			gen.put(i,rand.nextInt(this.gwCount)+1);
		}
		this.fog.clearAppToGws();
		this.fog.AssignAppsToGws(gen);
		while (!(this.fog.verifyIndValidity()==0)){
			this.fog.clearAppToGws();
			gen = new HashMap<>();
			for (int i=1;i<=AppCnt;i++){
				gen.put(i,rand.nextInt(this.gwCount)+1);
			}
			this.fog.AssignAppsToGws(gen);
		}
		return gen;		
	}
	
	public List<Map<Integer,Integer>> randomPop(int size){
		List<Map<Integer,Integer>> pop = new ArrayList<>();
		for (int i=0; i<size;i++){
			pop.add(randomInd());
		}
		return pop;
	}
	
	
}
