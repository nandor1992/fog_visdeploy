package org.nandor.fog_deployer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Main {

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

    public static void runGAValid() {
        Methods.GAPopSizeEvaluation(3);
        Methods.GAStopCondEvaluation(1);
        Methods.ClustSizeEvaluation(1);
    }

    public static void performanceRun(int size, int sceType, int meType) {
//        int size = 2; //0-20;1-80;2-320;
//        int sceType = 3; //Scenario Type 1-Delay 2-Multi 3-Capab
//        int meType = 0; //MethodType //0Everything//1Ga//2Dist//3Samp//4Ini//5Random
        Methods.PerformanceAnalysis(size, sceType, meType);
    }

    public static void scalabilityRun(int size, int sceType, int meType, int count) {
//        int size = 1; //Size
//        int sceType = 1; //Scenario Type 1-Delay 2-Multi 3-Capab
//        int meType = 0; //MethodType //0Everything//1Ga//2Dist//3Samp//4Ini//5Random
//        int count = 1; //IteCountr
        Methods.ScalabilityAnalysis(size, count, sceType, meType);
    }

    public static void timeDistributionRun() {

        Methods.timeDistributionAnalysis();
    }

    public static void ComponentEvals(int caseType, int clustType, int allocType) {
       // int caseType = 1;
       // int clustType = 0;
       // int allocType = 4;
        Methods.ComponentAnalysis(caseType, clustType, allocType);
    }

    public static void WeightsAnalysis(int caseType, int choice) {
        //int caseType = 3;
        //int choice = 0; //1 2
        Methods.WeightsAnalysis(caseType, choice);
    }


    public static Fog getSampleDeployments(String fog_string, int opt_type) {

        JSONParser parser = new JSONParser();
        JSONObject json_fog = null;
        try {
            json_fog = (JSONObject) parser.parse(fog_string);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }


        //Generate
        Fog f = Exporter.readJsonFog(json_fog);
        //Optimization Section
        Methods.dataG.newDataSet("Example");
        Methods.dataG.setBestCluster(f.retreiveCluster());
        List<Map<Integer, Integer>> best = null;
        switch (opt_type) {
            case 1:
                best = Methods.GAGlobal(f);
                break;
            case 2:
                best = Methods.DistanceClusteringDeployment(f);
                break;
            case 3:
                best = Methods.SampleWeDiCOptimization(f);
                break;
            case 4:
                best = Methods.InitWeDiCOptimization(f);
                break;
            case 5:
                best = Methods.RandomDeployment(f);
                break;
            default:
                best = Methods.GAGlobal(f);
                break;
        }

        //Deploy
        if (Methods.dataG.getbestClusters().isEmpty()){
            f.removeClusters();
            f.createClusters(Methods.dataG.getbestClusters());
        }
        System.out.println("Cluster: "+Methods.dataG.getbestClusters().toString());
        f.setDeplpyment(best.get(0));
        f.deployFog();
        System.out.println("Compound Utility: "+f.getFogCompoundUtility().toString());
        return f;

    }

    public static void main(String[] args) {
        Fog f = Methods.InitDelayFog(120);
    }

}
