package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

public class WayFinder {
	
	private Fog f;
	private WeightedCls cls;
	private int clsSize;
	private int minSize;
	private double proc;
	private Map<String,Double> appWeights;
	private Map<String,Double> gwWeights;
	private int failCnt = 0;
	private int maxFailCnt = 4;
	
	public WayFinder(Fog f, WeightedCls cls, double proc,int minSize){
		this.f=f;
		this.cls=cls;
		this.proc=proc;
		this.clsSize = (int)(f.getApps().size()*proc);
		if(this.clsSize<minSize){
			this.clsSize=minSize;
		}
		this.minSize=minSize;
	}
	
	
	
	public List<Map<Integer, Integer>> sampleFogAttempt(){
		//Starting Condition
		genAppWeights(1.0,1.0,1.0,1.0,1.0,1.0,1.0);
		genGwWeights(1.0,1.0,1.0,1.0,1.0);
		//Initial Attempt
		List<Map<Integer, Integer>> ret = attemptRandInstance();
		while (ret == null && failCnt<maxFailCnt){
			//First Attempt failed do diiiiive util size < min then do min while solution is found and if none is found escape
			System.out.println();
			System.out.println("----- Failed Attempt with clsSize: "+clsSize+" and Weights:" +appWeights+" - "+gwWeights+" FailCnt: "+failCnt);
			if (clsSize==minSize){
				failCnt++;
				this.modifyWeights(1.0-(1.0/(double)(2*maxFailCnt+1)*(double)failCnt),1.0+(1.0/(double)(2*maxFailCnt+1)*(double)failCnt));
				}
			if (clsSize/2<=minSize){clsSize=minSize;}else{clsSize=clsSize/2;}
			ret = attemptRandInstance();
		}
		List<Map<Integer, Integer>> tmpRet = ret;
		int refClsSize = clsSize;
		while (clsSize!=(int)(f.getApps().size()*proc) && failCnt<maxFailCnt){
			//Grow Until Clusterin can be done
			if (tmpRet!=null){
				ret=tmpRet;
				refClsSize = clsSize;
				System.out.println();
				System.out.println("----- Successfull Attempt with clsSize: "+clsSize);
				this.interpretWeights(ret);
				if (failCnt>1){
					this.modifyWeights(1.0-(1.0/(double)(2*maxFailCnt+1)*(double)failCnt),1.0+(1.0/(double)(2*maxFailCnt+1)*(double)failCnt));
				}
				double multi = 2.0;
				if (failCnt!=0){
					multi = 2.0-failCnt/maxFailCnt;
				}
				if (clsSize*multi>(int)(f.getApps().size()*proc)){clsSize=(int)(f.getApps().size()*proc);}
				else{clsSize=(int)(clsSize*multi);}
				System.out.println("Cls Size: "+clsSize+" Max Fail: "+maxFailCnt+" FailCnt: "+failCnt+" and Weights:" +appWeights+" - "+gwWeights);
				tmpRet = attemptInstance();
			}else{
				System.out.println();
				System.out.println("----- Failed Attempt with clsSize: "+clsSize+" FailCnt: "+failCnt);
				failCnt++;
				clsSize=(int) (clsSize*(1.0-1/(double)maxFailCnt));
				if (clsSize<=refClsSize){
					break;
				}
				System.out.println("Cls Size: "+clsSize+" Max Fail: "+maxFailCnt+" FailCnt: "+failCnt+" and Weights:" +appWeights+" - "+gwWeights);
				this.modifyWeights(1.0-(1.0/(double)(2*maxFailCnt+1)*(double)failCnt),1.0+(1.0/(double)(2*maxFailCnt+1)*(double)failCnt));
				tmpRet = attemptInstance();
			}
		}
		return ret;
	}
	
	private void interpretWeights(List<Map<Integer, Integer>> ret) {
		//Basically probe from WeightTrainer
		Double appProcLim = 0.1;
		Double gwProcLim = 0.02;
		Map<String,Double> corrApp = cls.Correlation("Deployment",cls.allAppSimilarities(ret));
		Map<String,Double> corrGw = cls.Correlation("Deployment",cls.allGwSimilarities(ret));
		
		Double appMax = 0.0;
		Double gwMax = 0.0;
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
		//Apps
		for (String name: corrApp.keySet()){
			//System.out.println("Name:"+name);
				if (Math.abs(corrApp.get(name))>appMax*appProcLim*failCnt){
					appWeights.put(name, Math.abs(corrApp.get(name)));
				}
		}
		//Gateways
		for (String name: corrGw.keySet()){
				if (Math.abs(corrGw.get(name))>gwMax*gwProcLim*failCnt){
					gwWeights.put(name, Math.abs(corrGw.get(name)));
				}
		}
		
		//Adjust so that their sum is 1 (why dunno, seems to make sense to me)
		double sum1=0.0;
		for (String name: appWeights.keySet()){
			sum1+=appWeights.get(name);
		}
		for (String name: appWeights.keySet()){
			appWeights.put(name,appWeights.get(name)/sum1);
		}
		double sum2=0.0;
		for (String name: gwWeights.keySet()){
			sum2+=gwWeights.get(name);
		}
		for (String name: gwWeights.keySet()){
			gwWeights.put(name,gwWeights.get(name)/sum2);
		}
	}

	//Modify Weights we are working with
	private void modifyWeights(double min, double max) {
		System.out.println("Modify Weight Min:"+min+" Max:"+max);
		Random rand = new Random();
		for (String name:this.appWeights.keySet()){
			appWeights.put(name, appWeights.get(name)*(rand.nextDouble()*(max-min)+min));
		}
		for (String name:this.gwWeights.keySet()){
			gwWeights.put(name, gwWeights.get(name)*(rand.nextDouble()*(max-min)+min));
		}
		
	}

	//Generate App Weights
	public void genAppWeights(double constr,double req,double share,double rate,double weights, double ul, double dist){
		Map<String,Double> ret = new HashMap<>();
		ret.put("Constraints",constr);
		ret.put("RequirementSim",req);
		ret.put("ResourceShare",share);
		ret.put("MessageRate",rate);
		ret.put("UtilityWeights",weights);
		ret.put("UnitLoad",ul);
		ret.put("Distance",dist);
		appWeights=ret;
	}
	//Generate GW Weights
	public void genGwWeights(double capab,double res, double perf, double base, double capac){
		Map<String,Double> ret = new HashMap<>();
		ret.put("Capabilities",capab);
		ret.put("SharedRes",res);
		ret.put("PerfToULoad",perf);
		ret.put("BaseLoad",base);
		ret.put("CapToULoad",capac);
		gwWeights=ret;
	}
	//Single Instance Attempt
	public List<Map<Integer, Integer>> attemptRandInstance(){
		List<Integer> apps = new ArrayList<>();
		List<Integer> totApps = new ArrayList<Integer>(f.getApps().keySet());
		while (apps.size()<=clsSize){
			//Get Random app add it to list
			Collections.shuffle(totApps);
				apps.add(totApps.get(0));
			
		}
		f.clearGwClustConns();
		f.removeClusters();
		List<Set<Integer>> tmpList= new ArrayList<>();
		tmpList.add(new HashSet<Integer>(apps));
		f.createClusters(tmpList);//eps, minPts
		cls.setGwWeights(gwWeights);
		cls.setAppWeights(appWeights);
		Methods.sampleResourceAlloc(f,cls);
		//displayClsAndRes(f);
		List<Map<Integer, Integer>> tmp = Methods.sampGAClus(f,false);
		cls.clearWeights();
		return  tmp;
	}
	
	public List<Map<Integer, Integer>> attemptInstance(){
		f.clearGwClustConns();
		f.removeClusters();
		cls.setGwWeights(gwWeights);
		cls.setAppWeights(appWeights);
		List<Set<Integer>> clss = cls.singleClsDB(7,clsSize);//eps, minPts
		if (clss.size()==0){
			return null;
		}
		f.createClusters(clss);
		Methods.sampleResourceAlloc(f,cls);
		//displayClsAndRes(f);
		List<Map<Integer, Integer>> tmp = Methods.sampGAClus(f,false);
		cls.clearWeights();
		return  tmp;
	}
	
	//Single Shot old Method
	public List<Map<Integer, Integer>> singleShot(){
		List<Integer> apps = new ArrayList<>();
		List<Integer> totApps = new ArrayList<Integer>(f.getApps().keySet());
		while (apps.size()<clsSize){
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
		Methods.sampleResourceAlloc(f,cls);
		//displayClsAndRes(f);
		List<Map<Integer, Integer>> tmp = Methods.sampGAClus(f, true);
		cls.clearWeights();
		return tmp;
	}
	
	//Main
	public static void main(String[] args) {
		Fog f = Methods.InitFog(40, 0);
		WeightedCls cls = new WeightedCls(f);
		WayFinder wf = new WayFinder(f, cls, 0.6, 10);
		System.out.println(wf.sampleFogAttempt());
	}

}
