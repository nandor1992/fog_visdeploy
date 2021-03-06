package org.nandor.fog_deployer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Testing {

	public static JSONObject readJson(String file) {
		JSONParser parser = new JSONParser();
		JSONObject a = new JSONObject();
		try {
			a = (JSONObject) parser.parse(new FileReader(file));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return a;
	}

	public static void writeJson(String file, JSONObject json) {
		FileWriter f;
		try {
			f = new FileWriter(file);
			f.write(json.toJSONString());
			f.flush();
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static void fullRun() {
		//Read from exported File
		//Fog f=Exporter.readJsonFog(readJson("C:/Users/Nandor/Documents/FogOfThings/Gateway Apps/spark-test/src/main/java/org/nandor/spark/deploy-W.json"));

		//Generate New Fog
		//Fog f = Methods.InitFog(100, 0);//Old
		Fog f = Methods.InitDelayFog(120);
		//Fog f = Methods.InitMultiFog(40);
		//Fog f = Methods.InitReqFog(40);
		
		//Optimization Section
		//Methods.GAGlobal(f);
		Methods.DistanceClusteringDeployment(f);
		Methods.SampleWeDiCOptimization(f);
		//Methods.InitWeDiCOptimization(f);
		//Methods.RandomDeployment(f);
		
		//Set Deplopyment and leave best as current
		//Map<Integer, Integer> best = Methods.SampleWeDiCOptimization(f,12);
		//f.setDeplpyment(best);
		//f.deployFog();
		
		//Write Results to File
		//writeJson("C:/Users/Nandor/Documents/FogOfThings/Gateway Apps/spark-test/src/main/java/org/nandor/spark/deploy-W.json",Exporter.writeJsonFog(f));
	}
	
	public static void runGAValid(){
		Methods.GAPopSizeEvaluation(3);
		Methods.GAStopCondEvaluation(1);
		Methods.ClustSizeEvaluation(1);
	}
	
	public static void performanceRun() {
		int size = 2; //0-20;1-80;2-320;
		int sceType = 3; //Scenario Type 1-Delay 2-Multi 3-Capab
		int meType = 0; //MethodType //0Everything//1Ga//2Dist//3Samp//4Ini//5Random
		Methods.PerformanceAnalysis(size,sceType,meType);
	}
	
	public static void scalabilityRun() {
		int size = 1; //Size
		int sceType = 1; //Scenario Type 1-Delay 2-Multi 3-Capab
		int meType = 0; //MethodType //0Everything//1Ga//2Dist//3Samp//4Ini//5Random
		int count = 1; //Iter Count
		Methods.ScalabilityAnalysis(size,count,sceType,meType);
	}
	
	public static void timeDistributionRun(){
		Methods.timeDistributionAnalysis();
	}
	
	public static void ComponentEvals(){
		int caseType = 1;
		int clustType = 0; 
		int allocType = 4;
		Methods.ComponentAnalysis(caseType,clustType,allocType);
	}
	
	public static void WeightsAnalysis(){
		int caseType = 3;
		int choice = 0 ; //1 2
		Methods.WeightsAnalysis(caseType,choice);
	}
	
	public static void getSampleDeployments(){

		
		//Generate
		Fog f = Methods.InitMultiFog(10);
		
		writeJson("C:/Users/Nandor/Documents/FogOfThings/Gateway Apps/spark-test/src/main/java/org/nandor/spark/deploy-Init.json",Exporter.writeJsonFog(f));
		
		//Optimization Section
		Methods.dataG.newDataSet("Sample");
		List<Map<Integer, Integer>> bests = Methods.SampleWeDiCOptimization(f);
			
		//Deploy
		f.removeClusters();
		f.createClusters(Methods.dataG.getbestClusters());
		f.setDeplpyment(bests.get(0));
		f.deployFog();
		
		//Write Results to File
		writeJson("C:/Users/Nandor/Documents/FogOfThings/Gateway Apps/spark-test/src/main/java/org/nandor/spark/deploy-Sample.json",Exporter.writeJsonFog(f));
		
		
		//Distance Optimization
		Methods.dataG.newDataSet("Direction");
		bests = Methods.DistanceClusteringDeployment(f);
		
		//Deploy
		f.removeClusters();
		f.createClusters(Methods.dataG.getbestClusters());
		f.setDeplpyment(bests.get(0));
		f.deployFog();
				
		//Write Results to File
		writeJson("C:/Users/Nandor/Documents/FogOfThings/Gateway Apps/spark-test/src/main/java/org/nandor/spark/deploy-Direction.json",Exporter.writeJsonFog(f));
			
	}
	
	public static void main (String[] args) {	
		//fullRun();
		//runGAValid();
		//performanceRun();
		//scalabilityRun();
		//timeDistributionRun();
		//ComponentEvals();
		getSampleDeployments();
		//WeightsAnalysis();
		
	}
	
}
