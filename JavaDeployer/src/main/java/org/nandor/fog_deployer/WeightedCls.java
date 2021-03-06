package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeightedCls extends Clustering {

	protected Map<String, Double> appWeights = new HashMap<>();
	protected Map<String, Double> gwWeights = new HashMap<>();
	protected WeighTrainer Wtrain = new WeighTrainer(0,0);
	public WeightedCls(Fog f) {
		super(f);
	}

	// Connections in a way that is not directional
	public Map<Integer, List<Integer>> getConnections() {
		Map<Integer, List<Integer>> nodes = new HashMap<>();
		for (Integer p : this.fog.getApps().keySet()) {
			if (nodes.containsKey(p)) {
				nodes.get(p).addAll(this.fog.getApps().get(p).getApps().keySet());
			} else {
				List<Integer> point = new ArrayList<>();
				point.addAll(this.fog.getApps().get(p).getApps().keySet());
				nodes.put(p, point);
			}
			for (Integer pBack : this.fog.getApps().get(p).getApps().keySet()) {
				if (nodes.containsKey(pBack)) {
					nodes.get(pBack).add(p);
				} else {
					List<Integer> pointBack = new ArrayList<>();
					pointBack.add(p);
					nodes.put(pBack, pointBack);
				}
			}
		}
		for (Integer p : nodes.keySet()) {
			List<Integer> deDupStringList = new ArrayList<>(new HashSet<>(nodes.get(p)));
			nodes.put(p, deDupStringList);

		}
		// System.out.println(nodes);
		return nodes;
	}

	//Weight Training Stuff
	
	public WeighTrainer getWeight(){
		return Wtrain;
	}
	
	public void initTrain(int maxStep,int maxFails){
		Wtrain = new WeighTrainer(maxStep,maxFails);
	}
	
	// Correlation Stuff
	// TODO Just a marker
	public Map<String, Double> Correlation(String variable, List<Map<String, Double>> data) {
		Map<String, Double> ret = new HashMap<>();
		// Assume that the first is representative
		for (String n : data.get(0).keySet()) {
			if (n != variable) {
				ret.put(n, CorrelationByVar(n, variable, data));
			}
		}
		return ret;
	}

	// Similarities between all points
	public Double CorrelationByVar(String variable2, String variable, List<Map<String, Double>> data) {

		// Using the equation
		// Pearson R Correlation
		// Rxy=1/(n-1)*(Sum_x,y((x-x')*(y-y')))/(Sx*Sy)
		// Where R[x,y] - Correlation between x and y
		// n - total number of data points
		// x,y - single data points
		// x',y' - Mean of all values of x and y
		// Sx,Sy - Standard Deviation for x and y

		// List<Map<String, Double>> data = allSimilarities();
		// System.out.println("Corr on Variable: "+variable+" Data Size:
		// "+data.size());

		// Check if any variable is NaN
		for (Map<String, Double> point : data) {
			for (String n : point.keySet()) {
				if (point.get(n).isNaN()) {
					//System.out.println("Point is NaN:" + point);
				}
			}
		}
		// Get the mean of the Deployments on the samme GW
		Double meanGw = 0.0;
		for (Map<String, Double> point : data) {
			meanGw += point.get(variable2);
		}
		meanGw = meanGw / data.size();
		// System.out.println("Mean Gw: "+meanGw);

		// Get the mean of the Distance Variable
		Double meanVar = 0.0;
		for (Map<String, Double> point : data) {
			meanVar += point.get(variable);
		}
		meanVar = meanVar / data.size();
		// System.out.println("Mean Var: "+meanVar);

		// Standard Deviation of Gw values
		Double stdGw = 0.0;
		for (Map<String, Double> point : data) {
			stdGw += (point.get(variable2) - meanGw) * (point.get(variable2) - meanGw);
		}
		stdGw = Math.sqrt(stdGw / (data.size() - 1));
		// System.out.println("Standard Dev Gw: "+stdGw);

		// Standard Deviation of Variable values
		Double stdVar = 0.0;
		for (Map<String, Double> point : data) {
			stdVar += (point.get(variable) - meanVar) * (point.get(variable) - meanVar);
		}
		stdVar = Math.sqrt(stdVar / (data.size() - 1));
		// System.out.println("Standard Dev Variable: "+stdVar);

		// Calculating the top part of the equation
		Double topSum = 0.0;
		for (Map<String, Double> point : data) {
			topSum += (point.get(variable2) - meanGw) * (point.get(variable) - meanVar);
		}
		// Return correlation value of the two variables
		Double corr = topSum / (stdVar * stdGw) / (data.size() - 1);
		// System.out.println("R: "+corr);
		// System.out.println("R^2: "+corr*corr);
		// In case of dummb/dummy data
		if (stdVar.isNaN() || stdGw.isNaN() || corr.isNaN()) {
			return 0.0;
		}
		return corr;
	}

	// App Similarities
	// TODO marker
	public List<Map<String, Double>> allGwSimilarities() {
		List<Map<String, Double>> ret = new ArrayList<Map<String, Double>>();
		// get all values for all Apps deployed check if they are on the same gw
		//List<Integer> visited = new ArrayList<Integer>();
		for (Integer a : this.fog.getApps().keySet()) {
			for (Integer g : this.fog.getGateways().keySet()) {
				Map<String, Double> tmp = getGwSimilarities(a, g);
				if (this.fog.getApps().get(a).getGateway().getId() == g) {
					tmp.put("Deployment", 1.0);
				} else {
					tmp.put("Deployment", 0.0);
				}
				ret.add(tmp);
			}
		}
		return ret;
	}
	//All gw similarities for the Gws
	public List<Map<String, Double>> allGwSimilarities(List<Map<Integer, Integer>> bests) {
		List<Map<String, Double>> ret = new ArrayList<Map<String, Double>>();
		// get all values for all Apps deployed check if they are on the same gw
		for (Map<Integer, Integer> b : bests) {
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(b);
//			if (this.fog.verifyIndValidity() && this.fog.checkIfAppsAllocated().size() == 0) {
				//List<Integer> visited = new ArrayList<Integer>();
				for (Integer a : this.fog.getApps().keySet()) {
					for (Integer g : this.fog.getGateways().keySet()) {
						Map<String, Double> tmp = getGwSimilarities(a, g);
						if (!(this.fog.getApps().get(a).getGateway() == null)) {
							if (this.fog.getApps().get(a).getGateway().getId() == g) {
								//tmp.put("Deployment", this.fog.getFogCompoundUtility().doubleValue());
								tmp.put("Deployment", 1.0);
								ret.add(tmp);
							} else {
								tmp.put("Deployment", 0.0);
								ret.add(tmp);
							}
						}
					}
				}
				this.fog.clearAppToGws();
//			}
		}
		return ret;
	}

	// Similarities between points
	public Map<String, Double> getGwSimilarities(Integer a, Integer g) {
		// Init
		Map<String, Double> ret = new HashMap<>();
		// Find parameters that you may think that inlfuences why apps chose
		// Gateways

		// Get Min and Max Values

		Map<String, Map<String, Double>> minMax = getMinMaxs();
		//System.out.println(minMax);
		// Whether having similar capability/requirements helps
		ret.put("Capabilities", this.appToGwCapability(a, g));

		// Whether the performance Coefficient and the Unit Load are Linked as
		// per size
		ret.put("PerfToULoad", this.perfToULoad(a, g, minMax));

		// Whether the capacity of the gateway links to the Unit load
		ret.put("CapToULoad", this.capToULoad(a, g, minMax));

		// Whether having resources on the Gateway helps the application
		ret.put("SharedRes", this.gwResShare(a, g));

		// Whether having a base load of any kind helps the app be on the
		// gateway
		ret.put("BaseLoad", this.gwBaseToApp(a, g, minMax));
		return ret;
	}

	// Whether having similar capability/requirements helps
	private Double appToGwCapability(Integer a, Integer g) {
		Double sim = 0.0;
		// Find all Requirements that are similar
		for (String req : this.fog.getApps().get(a).getRequirements()) {
			for (String req2 : this.fog.getGateways().get(g).getCapabilities()) {
				if (req.compareTo(req2) == 0) {
					sim += 1.0;
					break;
				}
			}
		}
		// Return the number of requirements shared for each app divided by
		// total and multiplied
		// ret = share1/tot1 * share2/tot2; where
		// share[x] - total number of requirements for app x the other app has ;
		// tot[x]-tot req count
		Double ret = sim / this.fog.getApps().get(a).getRequirements().size();
		return ret;
	}

	// Whether having resources on the Gateway helps the application
	private Double gwResShare(Integer a, Integer g) {
		Double sim = 0.0;
		// Find all Resources that share a gw from p1 to p2
		for (Integer res : this.fog.getApps().get(a).getResources().keySet()) {
			if (this.fog.getResources().get(res).getGateway().getId() == g) {
				sim += 1.0;
			}
		}
		if (this.fog.getApps().get(a).getResources().size() == 0) {
			return 0.0;
		}
		Double ret = sim / this.fog.getApps().get(a).getResources().size();
		return ret;
	}

	// Whether the performance Coefficient and the Unit Load are Linked as per
	private Double perfToULoad(Integer a, Integer g, Map<String, Map<String, Double>> minMax) {
		Double pC = this.fog.getGateways().get(g).getPjSpeed().doubleValue();
		Double uL = this.fog.getApps().get(a).getUnitLoad().doubleValue();
		//Return the absolute diffrence between the twos positioning in the min and max 
		//pcRel = pc/(max-min)+min
		Double pCRel;
		Double ulRel;
		if (minMax.get("perf").get("max")==minMax.get("perf").get("min")){
			pCRel = 1.0;
		}else{
			pCRel = (pC-minMax.get("perf").get("min"))/(minMax.get("perf").get("max")-minMax.get("perf").get("min"));
		}
		if (minMax.get("uLoad").get("max")==minMax.get("uLoad").get("min")){
			ulRel = 1.0;
		}else{
			ulRel = (uL-minMax.get("uLoad").get("min"))/(minMax.get("uLoad").get("max")-minMax.get("uLoad").get("min"));
		}
		/*System.out.println(pC);
		System.out.println(uL);
		System.out.println(pCRel);
		System.out.println(ulRel);
		if (this.relDiff(pCRel,ulRel).isNaN()){
			System.out.println(pC);
			System.out.println(uL);
			System.out.println(pCRel);
			System.out.println(ulRel);
		}*/
		return (1.0-this.relDiff(pCRel,ulRel));
	}

	// Whether the capacity of the gateway links to the Unit load
	private Double capToULoad(Integer a, Integer g, Map<String, Map<String, Double>> minMax) {
		Double cC = this.fog.getGateways().get(g).getPjCap().doubleValue();
		Double uL = this.fog.getApps().get(a).getUnitLoad().doubleValue();
		//Return the absolute diffrence between the twos positioning in the min and max 
		//pcRel = pc/(max-min)+min
		Double cCRel;
		Double ulRel;
		if (minMax.get("cap").get("max")==minMax.get("cap").get("min")){
			cCRel = 1.0;
		}else{
			cCRel = (cC-minMax.get("cap").get("min"))/(minMax.get("cap").get("max")-minMax.get("cap").get("min"));
		}
		if (minMax.get("uLoad").get("max")==minMax.get("uLoad").get("min")){
			ulRel = 1.0;
		}else{
			ulRel = (uL-minMax.get("uLoad").get("min"))/(minMax.get("uLoad").get("max")-minMax.get("uLoad").get("min"));
		}
		/*System.out.println(cC);
		System.out.println(uL);
		System.out.println(cCRel);
		System.out.println(ulRel);*/
		return (1.0-this.relDiff(cCRel,ulRel));
	}

	// Whether having a base load of any kind helps the app be on the gateway
	private Double gwBaseToApp(Integer a, Integer g, Map<String, Map<String, Double>> minMax) {
		Double bLoad = this.fog.getGateways().get(g).getGwBaseLoad().doubleValue();
		Double uL = this.fog.getApps().get(a).getUnitLoad().doubleValue();
		//Return the absolute diffrence between the twos positioning in the min and max 
		//pcRel = pc/(max-min)+min
		bLoad = bLoad/100.0;
		Double ulRel;
		if (minMax.get("uLoad").get("max")==minMax.get("uLoad").get("min")){
			ulRel = 1.0;
		}else{
			ulRel = (uL-minMax.get("uLoad").get("min"))/(minMax.get("uLoad").get("max")-minMax.get("uLoad").get("min"));
		}
		/*System.out.println(bLoad);
		System.out.println(uL);
		System.out.println(ulRel);*/
		return (1.0-this.relDiff(bLoad,ulRel));
	}
	
	// Get minimum and max values for future reference
	private Map<String, Map<String, Double>> getMinMaxs() {
		Map<String, Map<String, Double>> ret = new HashMap<>();
		Map<String, Double> perfCoeff = new HashMap<>();
		Map<String, Double> capCoeff = new HashMap<>();
		Map<String, Double> uLoad = new HashMap<>();
		Double pCMax = 0.0;
		Double cCMax = 0.0;
		Double pCMin = Double.MAX_VALUE;
		Double cCMin = Double.MAX_VALUE;
		for (Integer g : this.fog.getGateways().keySet()) {
			Double pC = this.fog.getGateways().get(g).getPjSpeed().doubleValue();
			Double cC = this.fog.getGateways().get(g).getPjCap().doubleValue();
			if (pC > pCMax) {
				pCMax = pC;
			}
			if (pC < pCMin) {
				pCMin = pC;
			}
			if (cC > cCMax) {
				cCMax = cC;
			}
			if (cC < cCMin) {
				cCMin = cC;
			}
		}
		perfCoeff.put("min", pCMin);
		perfCoeff.put("max", pCMax);
		capCoeff.put("min", cCMin);
		capCoeff.put("max", cCMax);
		Double uLMax = 0.0;
		Double uLMin = Double.MAX_VALUE;
		for (Integer a : this.fog.getApps().keySet()) {
			Double uL = this.fog.getApps().get(a).getUnitLoad().doubleValue();
			if (uL > uLMax) {
				uLMax = uL;
			}
			if (uL < uLMin) {
				uLMin = uL;
			}
		}
		uLoad.put("min", uLMin);
		uLoad.put("max", uLMax);
		ret.put("perf", perfCoeff);
		ret.put("cap", capCoeff);
		ret.put("uLoad", uLoad);
		return ret;
	}

	// App Similarities
	// TODO marker
	public List<Map<String, Double>> allAppSimilarities() {
		List<Map<String, Double>> ret = new ArrayList<Map<String, Double>>();
		// get all values for all Apps deployed check if they are on the same gw
		List<Integer> visited = new ArrayList<Integer>();
		for (Integer a1 : this.fog.getApps().keySet()) {
			for (Integer a2 : this.fog.getApps().keySet()) {
				if (a1 != a2 && !visited.contains(a2)) {
					Map<String, Double> tmp = getAppSimilarities(a1, a2);
					if (this.fog.getApps().get(a1).getGateway().getId() == this.fog.getApps().get(a2).getGateway()
							.getId()) {
						tmp.put("Deployment", 1.0);
					} else {
						tmp.put("Deployment", 0.0);
					}
					ret.add(tmp);
				}
			}
			visited.add(a1);
		}
		return ret;
	}
	//Get best from a number of bests
	public List<Map<String, Double>> allAppSimilarities(List<Map<Integer, Integer>> bests) {
		//TODO Make it to bests
		List<Map<String, Double>> ret = new ArrayList<Map<String, Double>>();
		// get all values for all Apps deployed check if they are on the same gw
		for (Map<Integer,Integer> best : bests){
			//Deploy Apps
			this.fog.clearAppToGws();
			this.fog.AssignAppsToGws(best);
			List<Integer> visited = new ArrayList<Integer>();
			for (Integer a1 : best.keySet()) {
				for (Integer a2 : best.keySet()) {
					if (a1 != a2 && !visited.contains(a2)) {
						if (this.fog.getApps().get(a1).getGateway() != null
								&& this.fog.getApps().get(a2).getGateway() != null) {
							Map<String, Double> tmp = getAppSimilarities(a1, a2);
							if (this.fog.getApps().get(a1).getGateway().getId() == this.fog.getApps().get(a2).getGateway()
									.getId()) {
								//tmp.put("Deployment", this.fog.getFogCompoundUtility().doubleValue());
								tmp.put("Deployment", 1.0);
								ret.add(tmp);
							} else {
								tmp.put("Deployment", 0.0);
								ret.add(tmp);
							}
						}
					}
				}
				visited.add(a1);
			}
			this.fog.clearAppToGws();
		}
		//System.out.println(ret);
		return ret;
	}

	// Similarities between points
	public Map<String, Double> getAppSimilarities(Integer p1, Integer p2) {
		// Inti
		Map<String, Double> ret = new HashMap<>();
		/*
		 * Need to make sure that these methods and non-directional so dist
		 * p1-p2 == dist p2-p1 in all cases The values found can be totally
		 * arbitrary as later on they will be compared to a min and max in the
		 * and given weights based on correlation parameters
		 */
		// Connection Distance
		ret.put("Distance", this.dijkstraSearsch(p1, p2));
		// Resource Location share
		ret.put("ResourceShare", this.getResShare(p1, p2));
		// Constraint Component
		ret.put("Constraints", this.getConstSim(p1, p2));
		// Msg Rate
		ret.put("MessageRate", this.getMsgRateComp(p1, p2));
		// Unit Load
		ret.put("UnitLoad", this.getUnitLoadComp(p1, p2));
		// Utility Weights ratio
		ret.put("UtilityWeights", this.getUtilityComp(p1, p2));
		// Requirements
		ret.put("RequirementSim", this.getReqComp(p1, p2));

		// Return
		return ret;
	}

	// Methods for getting Similarities
	private Double getResShare(Integer p1, Integer p2) {
		Double sim = 0.0;
		Double sim2 = 0.0;
		//boolean found = false;
		// Find all Resources that share a gw from p1 to p2
		if (this.fog.getApps().get(p1).getResources().size() == 0
				|| this.fog.getApps().get(p2).getResources().size() == 0) {
			return 0.0;
		}	
		for (Integer res : this.fog.getApps().get(p1).getResources().keySet()) {
			for (Integer res2 : this.fog.getApps().get(p2).getResources().keySet()) {
				if (this.fog.getResources().get(res).getGateway().getId() == this.fog.getResources().get(res2)
						.getGateway().getId()) {
					sim += 1.0;
					break;
				}
			}
		}
		// Find all Resources that share a gw from p2 to p1
		for (Integer res : this.fog.getApps().get(p2).getResources().keySet()) {
			for (Integer res2 : this.fog.getApps().get(p1).getResources().keySet()) {
				if (this.fog.getResources().get(res).getGateway().getId() == this.fog.getResources().get(res2)
						.getGateway().getId()) {
					sim2 += 1.0;
					break;
				}
			}
		}
		/*
		 * System.out.println(sim); System.out.println(sim2);
		 * System.out.println(this.fog.getApps().get(p1).getResources().size());
		 * System.out.println(this.fog.getApps().get(p2).getResources().size());
		 */
		// Return the number of resources shared for each app divided by total
		// and multiplied
		// ret = share1/tot1 * share2/tot2; where
		// share[x] - total number of resources for app x that shares a gw with
		// the other app; tot[x]-tot res count
		Double ret = sim / this.fog.getApps().get(p1).getResources().size() * sim2
				/ this.fog.getApps().get(p2).getResources().size();
		return ret;
	}

	private Double getConstSim(Integer p1, Integer p2) {
		// Check if they have constraints or not, and if they have these on the
		// same thing
		Double constVal = 0.0;
		for (String ct : this.fog.getApps().get(p1).getConstraints().keySet()) {
			if (this.fog.getApps().get(p2).getConstraints().get(ct) != null) {
				// We have found a constraint that both of them have,
				// first check if same type (existing, non existing)
				// check difference if existing
				Float ct1 = this.fog.getApps().get(p1).getConstraints().get(ct);
				Float ct2 = this.fog.getApps().get(p2).getConstraints().get(ct);
				/*
				 * if ((ct1 == Float.MAX_VALUE && ct2 == Float.MAX_VALUE) || (
				 * ct2 == Float.MIN_VALUE && ct1 == Float.MIN_VALUE)){ //Neither
				 * has a constraint System.out.println("No Constraints"); }
				 */
				if ((ct1 != Float.MAX_VALUE && ct2 != Float.MAX_VALUE)
						&& (ct2 != Float.MIN_VALUE && ct1 != Float.MIN_VALUE)) {
					// Both have constraints, compare
					// System.out.println("Both have Constraints, compare
					// then");
					// System.out.println(ct1+" "+ct2);
					// System.out.println(this.relDiff((double)ct1,(double)
					// ct2));
					constVal += (1.0 - this.relDiff((double) ct1, (double) ct2));
				}
			}
		}
		/*
		 * System.out.println(constVal);
		 * System.out.println(this.fog.getApps().get(p1).getConstraints().size()
		 * );
		 * System.out.println(this.fog.getApps().get(p2).getConstraints().size()
		 * );
		 */
		// ret = ct/tot1 * ct/tot2; where
		// ct - existing constraint diffrences; tot[x]-tot constraint count
		Double ret = constVal / this.fog.getApps().get(p1).getConstraints().size() * constVal
				/ this.fog.getApps().get(p2).getConstraints().size();
		return ret;
	}

	private Double getMsgRateComp(Integer p1, Integer p2) {
		// System.out.println((1-relDiff((double)this.fog.getApps().get(p1).getTotalMsgRate(),(double)this.fog.getApps().get(p2).getTotalMsgRate())));
		// Similarity between the two message rates
		return (1 - relDiff((double) this.fog.getApps().get(p1).getTotalMsgRate(),
				(double) this.fog.getApps().get(p2).getTotalMsgRate()));
	}

	private Double getUnitLoadComp(Integer p1, Integer p2) {
		// System.out.println((1-relDiff((double)this.fog.getApps().get(p1).getUnitLoad(),(double)this.fog.getApps().get(p2).getUnitLoad())));
		// Similarity between the two Unit Loads
		return (1 - relDiff((double) this.fog.getApps().get(p1).getUnitLoad(),
				(double) this.fog.getApps().get(p2).getUnitLoad()));
	}

	private Double getUtilityComp(Integer p1, Integer p2) {
		// Check if they have utilityweights or not, and if they have these on
		// the same thing
		Double utilVal = 0.0;
		for (String u : this.fog.getApps().get(p1).getUtilityWeights().keySet()) {
			if (this.fog.getApps().get(p2).getUtilityWeights().get(u) != null) {
				// We have found a constraint that both of them have,
				// first check if same type (existing, non existing)
				// check difference if existing
				Float u1 = this.fog.getApps().get(p1).getUtilityWeights().get(u);
				Float u2 = this.fog.getApps().get(p2).getUtilityWeights().get(u);
				utilVal += (1.0 - this.relDiff((double) u1, (double) u2));
			}
		}
		// ret = u/tot1 * u/tot2; where
		// u - existing utility weight diffrences; tot[x]-tot utility weight
		// count
		Double ret = utilVal / this.fog.getApps().get(p1).getUtilityWeights().size() * utilVal
				/ this.fog.getApps().get(p2).getUtilityWeights().size();
		return ret;
	}

	private Double getReqComp(Integer p1, Integer p2) {
		Double sim = 0.0;
		Double sim2 = 0.0;
		// Find all Requirements that are similar
		for (String req : this.fog.getApps().get(p1).getRequirements()) {
			for (String req2 : this.fog.getApps().get(p2).getRequirements()) {
				if (req.compareTo(req2) == 0) {
					sim += 1.0;
					break;
				}
			}
		}
		// Find all Requirements that are similar
		for (String req : this.fog.getApps().get(p2).getRequirements()) {
			for (String req2 : this.fog.getApps().get(p1).getRequirements()) {
				if (req.compareTo(req2) == 0) {
					sim2 += 1.0;
					break;
				}
			}
		}
		// Return the number of requirements shared for each app divided by
		// total and multiplied
		// ret = share1/tot1 * share2/tot2; where
		// share[x] - total number of requirements for app x the other app has ;
		// tot[x]-tot req count
		Double ret = sim / this.fog.getApps().get(p1).getRequirements().size() * sim2
				/ this.fog.getApps().get(p2).getRequirements().size();
		return ret;
	}

	// Extra Methods, usefull
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
	

	// Modified and Re-Implemented
	public Double dijkstraSearsch(Integer p, Integer p2) {
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
			// System.out.println(queue);
			// System.out.println(dist);
			Integer u = getShortestDist(dist, queue);
			if (u != null) {
				// System.out.println(u);
				queue.remove(queue.indexOf(u));
				for (Integer i : this.nodes.get(u)) {
					Double newDist = dist.get(u) + 1.0;
					if (newDist < dist.get(i)) {
						dist.put(i, newDist);
						prev.put(i, u);
					}
				}
			} else {
				if (dist.get(p2) != Double.POSITIVE_INFINITY) {
					return dist.get(p2);
				} else {
					// Find longest distance, use that, to not create an outlier
					Double maxD = 0.0;
					for (Integer d : dist.keySet()) {
						if (dist.get(d) > maxD && dist.get(d) != Double.POSITIVE_INFINITY) {
							maxD = dist.get(d);
						}
					}
					return maxD;
				}
			}
		}
		return 1.0/dist.get(p2);
	}
	
	public void setCorrelation(Map<String, Double> corrApp, Map<String, Double> corrGw, double limApp,double limGws) {
		//Find parameters of interest
		//appWeights gwWeights
		appWeights = new HashMap<>();
		gwWeights = new HashMap<>();
		//Apps
		for (String name: corrApp.keySet()){
			if (Math.abs(corrApp.get(name))>limApp){
				appWeights.put(name, Math.abs(corrApp.get(name)));
			}
		}
		//Gateways
		for (String name: corrGw.keySet()){
			if (Math.abs(corrGw.get(name))>limGws){
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
		System.out.println("Corr App: "+appWeights+" Corr Gw:"+gwWeights);
	}

	public void setCorrelation(Map<String, Double> corrApp, Map<String, Double> corrGw,double procLim) {
		//Find parameters of interest
		//appWeights gwWeights
		appWeights = new HashMap<>();
		gwWeights = new HashMap<>();
		//Get Maxes 
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
			if (Math.abs(corrApp.get(name))>appMax*procLim){
				appWeights.put(name, Math.abs(corrApp.get(name)));
			}
		}
		//Gateways
		for (String name: corrGw.keySet()){
			if (Math.abs(corrGw.get(name))>gwMax*procLim){
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
		System.out.println("Corr App: "+appWeights+" Corr Gw:"+gwWeights);
	}
	
	//Reset Wiehgts to same value
	public void resetGwWeights(Double val) {
		//Find parameters of interest
		//appWeights gwWeights
		gwWeights = new HashMap<>();
		List<String> names = new ArrayList<>();
		names.add("Capabilities");
		names.add("PerfToULoad");
		names.add("CapToULoad");
		names.add("SharedRes");
		names.add("BaseLoad");
		//Adjust so that their sum is 1 (why dunno, seems to make sense to me)
		for (String name: names){
			gwWeights.put(name,val);
		}
	}
	
	public void resetAppWeights(Double val) {
		//Find parameters of interest
		//appWeights gwWeights
		appWeights = new HashMap<>();
		List<String> names = new ArrayList<>();
		names.add("Distance");
		names.add("ResourceShare");
		names.add("Constraints");
		names.add("MessageRate");
		names.add("UnitLoad");
		names.add("UtilityWeights");
		names.add("RequirementSim");
		//Adjust so that their sum is 1 (why dunno, seems to make sense to me)
		for (String name: names){
			appWeights.put(name,val);
		}
	}
	
	public void setGwWeights(Map<String, Double> gwWeights) {
		this.gwWeights=gwWeights;
		
	}
	public void setAppWeights(Map<String, Double> appWeights) {
		this.appWeights=appWeights;	
	}
	
 public void clearWeights(){
	 appWeights = new HashMap<>();
	 gwWeights = new HashMap<>();
 }
	
	public void setCorrelation(Map<String, Double> corrApp, Map<String, Double> corrGw) {
		//Find parameters of interest
		//appWeights gwWeights
		if (corrApp.size()==0 && corrGw.size()==0){
			return;
		}
		appWeights = new HashMap<>();
		gwWeights = new HashMap<>();
		//Get Maxes 
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
				appWeights.put(name, corrApp.get(name));
		}
		//Gateways
		for (String name: corrGw.keySet()){
				gwWeights.put(name, corrGw.get(name));
		}
		
		//Adjust so that their sum is 1 (why dunno, seems to make sense to me)
		double sum1=0.0;
		for (String name: appWeights.keySet()){
			sum1+=Math.abs(appWeights.get(name));
		}
		for (String name: appWeights.keySet()){
			appWeights.put(name,appWeights.get(name)/sum1);
		}
		double sum2=0.0;
		for (String name: gwWeights.keySet()){
			sum2+=Math.abs(gwWeights.get(name));
		}
		for (String name: gwWeights.keySet()){
			gwWeights.put(name,gwWeights.get(name)/sum2);
		}
		
		//System.out.println("Corr App: "+appWeights+" Corr Gw:"+gwWeights);
	}
	
	public List<Set<Integer>> DBScan(Float eps,Integer minPts,Integer maxPts){
		//System.out.println("Apps:"+this.nodes.keySet());
		this.visited = new HashSet<>();
		this.noise = new HashSet<>();
		this.clust = new ArrayList<>();
		for (Integer p : nodes.keySet()){
			if (!this.visited.contains(p)){
				this.visited.add(p);
				//System.out.println("Neighbourhood Queest: "+p+" Apps: "+nodes.get(p));
				List<Integer> neighbour = getNeighbours(p,eps,4);
				//System.out.println(neighbour);
				if (neighbour.size()<minPts){
					this.noise.add(p);
				}else{
					this.clust.add(expandCluster(p,neighbour, eps,minPts,maxPts));
				}
			}
		}
		//System.out.println("Clusters: "+this.clust);
		//System.out.println("Noise: "+this.noise);
		noiseSort(maxPts);
		//Removing Clusters that are too small
		List<Set<Integer>> newClust = new ArrayList<>();
		for (Set<Integer> cls: this.clust){
			if (cls.size()<minPts){
				this.noise.addAll(cls);
			}else{
				newClust.add(cls);
			}
		}
		this.clust=newClust;
		noiseSort(maxPts);
		
		//Sort out stragglers Just ofr this type, otherwise it gets cray-cray
		newClust = new ArrayList<>();
		for (Set<Integer> cls: this.clust){
			if (cls.size()<minPts/1.5){
				this.noise.addAll(cls);
			}else{
				newClust.add(cls);
			}
		}
		this.clust=newClust;
		noiseSort(maxPts+2);
		return this.clust;
	}
	
	public List<Set<Integer>> DBScan(Integer minPts){
		//System.out.println("Apps:"+this.nodes.keySet());
		this.visited = new HashSet<>();
		this.noise = new HashSet<>();
		this.clust = new ArrayList<>();
		List<Set<Integer>> bestCls = new ArrayList<>();
		Float eps = (float)0.1;
		int iter = 0;
		int failiter = 0;
		Double prevValid = Double.MAX_VALUE;
		Double bestValid = Double.MAX_VALUE;
		Float bestEps = Float.MAX_VALUE;
		List<Double> epsVals = this.getMinMaxEpsValues(10.0,4);
		eps=epsVals.get(0).floatValue();
		boolean found = false;
		int fails = 0;
		//while ((this.validateClust(minPts)<=prevValid || bestValid > minPts*1.5)&& eps<epsVals.get(1).floatValue()){
		while (eps<epsVals.get(1).floatValue() && (!found || fails<3)){
			prevValid=this.validateClust(minPts);
			//System.out.println(" -> Clustering iter: "+iter+" with Eps: "+eps+" Valid: "+prevValid+"Found: "+found+" Fails:" +fails);
			eps+=epsVals.get(2).floatValue();
			this.visited = new HashSet<>();
			this.noise = new HashSet<>();
			this.clust = new ArrayList<>();
			for (Integer p : nodes.keySet()){
				if (!this.visited.contains(p)){
					this.visited.add(p);
					//System.out.println("Neighbourhood Queest: "+p+" Apps: "+nodes.get(p));
					List<Integer> neighbour = getNeighbours(p,eps,4);
					//System.out.println(neighbour);
					if (neighbour.size()<minPts){
						this.noise.add(p);
					}else{
						this.clust.add(expandCluster(p,neighbour, eps,minPts));
					}
				}
			}
			noiseSort();
			//Removing Clusters that are too small
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
			prevValid = this.validateClust(minPts);
			if (prevValid<bestValid){
				found=true;
				bestCls=this.clust;
				bestValid=prevValid;
				bestEps = eps;
			}
			if (prevValid == Double.MAX_VALUE){
				fails++;
			}else{
				fails=0;
			}
			iter++;
			/*System.out.println(" -> Eps: "+eps);
			System.out.println("Valid : "+this.validateClust(minPts));
			System.out.println("Clusters: "+this.clust);
			System.out.println("Noise: "+this.noise);*/
		}
		System.out.println("Eps Search Results - Best Eps:"+bestEps+" BestValid: "+bestValid);
		this.clust=bestCls;
		//Simple fix to overallocation resolve
		this.resolveOverAllocation();
		noiseSort();
		//System.out.println("Clusters: "+this.clust);
		//System.out.println("Noise: "+this.noise);*/
		return this.clust;
	}
	
	public List<Set<Integer>> singleClsDB(int minPts,int clsSize) {
		// TODO Auto-generated method stub
		boolean success = false;
		List<Set<Integer>> ret = new ArrayList<>();
		
		List<Double> epsVals = this.getMinMaxEpsValues(5.0,4);
		float eps = (epsVals.get(0).floatValue()+epsVals.get(1).floatValue())/2;
		visited = new HashSet<>();
		noise = new HashSet<>();
		clust = new ArrayList<>();
		List<Integer> points = new ArrayList<>(nodes.keySet());
		Random rand;
		int fails = 0;
		while (success == false) {
			Collections.shuffle(points);
			int p = points.get(0);
			if (fails%10==0){
				eps=eps-epsVals.get(2).floatValue();
			}
			if (eps<0){
				break;
			}
			System.out.println("SingleDB Attempt - Point: "+p+" Eps: "+eps+" fails: "+fails);
			if (!this.visited.contains(p)) {
				this.visited.add(p);
				// System.out.println("Neighbourhood Queest: "+p+" Apps:
				// "+nodes.get(p));
				List<Integer> neighbour = getNeighbours(p, eps, 4);
				// System.out.println(neighbour);
				if (neighbour.size() < minPts) {
					this.noise.add(p);
				} else {
					Set<Integer> cls = expandCluster(p, neighbour, eps, minPts,clsSize);
					if (cls.size()>=clsSize){
						this.clust.add(cls);
						ret.add(cls);
						return ret;
					}
				}
			}
			fails++;
		}
		return ret;
	}

	public List<Set<Integer>> DBScan(Integer minPts,List<Double> epsVals){
		//System.out.println("Apps:"+this.nodes.keySet());
		this.visited = new HashSet<>();
		this.noise = new HashSet<>();
		this.clust = new ArrayList<>();
		List<Set<Integer>> bestCls = new ArrayList<>();
		int iter = 0;
		int failiter = 0;
		Double prevValid = Double.MAX_VALUE;
		Double bestValid = Double.MAX_VALUE;
		Float eps=epsVals.get(0).floatValue();
		boolean found = false;
		int fails = 0;
		//while ((this.validateClust(minPts)<=prevValid || bestValid > minPts*1.5)&& eps<epsVals.get(1).floatValue()){
		while (eps<epsVals.get(1).floatValue() && (!found || fails<3)){
			prevValid=this.validateClust(minPts);
			System.out.println(" -> Clustering iter: "+iter+" with Eps: "+eps+" Valid: "+prevValid+"Found: "+found+" Fails:" +fails);
			eps+=epsVals.get(2).floatValue();
			this.visited = new HashSet<>();
			this.noise = new HashSet<>();
			this.clust = new ArrayList<>();
			for (Integer p : nodes.keySet()){
				if (!this.visited.contains(p)){
					this.visited.add(p);
					//System.out.println("Neighbourhood Queest: "+p+" Apps: "+nodes.get(p));
					List<Integer> neighbour = getNeighbours(p,eps,4);
					//System.out.println(neighbour);
					if (neighbour.size()<minPts){
						this.noise.add(p);
					}else{
						this.clust.add(expandCluster(p,neighbour, eps,minPts));
					}
				}
			}
			noiseSort();
			//Removing Clusters that are too small
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
			prevValid = this.validateClust(minPts);
			if (prevValid<bestValid){
				found=true;
				bestCls=this.clust;
				bestValid=prevValid;
			}
			if (prevValid == Double.MAX_VALUE){
				fails++;
			}else{
				fails=0;
			}
			iter++;
			/*System.out.println(" -> Eps: "+eps);
			System.out.println("Valid : "+this.validateClust(minPts));
			System.out.println("Clusters: "+this.clust);
			System.out.println("Noise: "+this.noise);*/
		}
		this.clust=bestCls;
		//Simple fix to overallocation resolve
		this.resolveOverAllocation();
		this.noiseSort();
		//System.out.println("Clusters: "+this.clust);
		//System.out.println("Noise: "+this.noise);*/
		return this.clust;
	}
	
	private void resolveOverAllocation() {
		Map<Integer,Integer> app = new HashMap<>();
		for (Set<Integer> c: this.clust){
			for (Integer a:c){
				if (app.get(a)==null){
					app.put(a, 1);
				}else{
					app.put(a,app.get(a)+1);
				}
			}
		}
		for (Integer a: app.keySet()){
			if (app.get(a)>1){
				//System.out.println("Duplicate app: "+a);
				//Search for all clusters, find distance and delete from all but the biggest 
				Set<Integer> tmpCls = null;
				double maxDist = 0.0;
				for (Set<Integer> c: this.clust){
					//Best CLuster found 
					if (c.contains(a)){
					    double tmp = this.distanceToCluster(a, c);
					    if (maxDist<tmp || tmpCls == null){
					    	maxDist=tmp;
					    	tmpCls=c;
					    }
					}
				}
				for (Set<Integer> c: this.clust){
					//Remove the rest of the useless clusters and F them
					if (c.contains(a)){
						if (c!=tmpCls){
							c.remove(a);
						}
					}
				}
			}
		}
		
	}
	
	private List<Double> getMinMaxEpsValues(Double div,Integer lvl) {
		List<Double> ret = new ArrayList<>();
		//For All apps get all the others Distance, get min max, split into ten
		List<Double> distances = new ArrayList<>();
		List<Integer> visited = new ArrayList<>();
		Map<Double,Integer> histogram = new HashMap<>();
		Double min=Double.MAX_VALUE,max=0.0;
		for (Integer a1: this.fog.getApps().keySet()){
			visited.add(a1);
			Map<Integer, Double> apps = dijkstraSearsch(a1,lvl.floatValue());
			for (Integer app:new HashSet<Integer>(apps.keySet())){
				if (apps.get(app)>lvl){
					apps.remove(app);
				}
			}
			for (Integer a2 : apps.keySet()) {
				if (!visited.contains(a2)){
					//Compute distance]
					Double tmp = this.getAppDistance(a1, a2);
					if (tmp>max){max=tmp;}
					if (tmp<min){min=tmp;}
					distances.add(tmp);
				}
			}
		}
		Double band = (max-min)/10.0;
		//System.out.println("Min: "+min+" Max:"+max+" Band "+band);
		for (int i=0;i<=10;i++){
			histogram.put(min+band*i, 0);
		}
		for (Double d: distances){
			int loc = (int) ((d-min)/band);
			//System.out.println("Loc:"+loc+"Value:"+(min+band*loc));
			histogram.put(min+band*loc, histogram.get(min+band*loc)+1);
		}
		//System.out.println("Histogram: "+histogram);
		Double retMin = Double.MAX_VALUE,retMax=0.0;
		//Min Maxes
		int tmpMax = 0;
		for (Double d: histogram.keySet()){
			if (histogram.get(d)>tmpMax){
				tmpMax = histogram.get(d);
				retMin = d;
			}
		}
		/*for (Double d: histogram.keySet()){
			if (histogram.get(d)>tmpMax/5 && d<retMax){
				if (d<retMin){retMin=d;}
			}
		}*/
		retMax = retMin+band/10;
		retMin = retMin-band;
		for (Double d: histogram.keySet()){
			if (histogram.get(d)>this.fog.getApps().size() && d>retMin){
				if (d>retMax){retMax=d;}
			}
		}
		retMax+=2*band;
		//Return
		ret.add(retMin);
		ret.add(retMax);
		ret.add((retMax-retMin)/div);
		System.out.println("Eps Vals:"+ret);
		/*ret = new ArrayList<>();
		ret.add(0.1);
		ret.add(7.0);
		ret.add(0.1);
		System.out.println(ret);*/
		//System.exit(1992);
		return ret;
	}
	
	private Double getAppDistance(Integer a1,Integer a2){
		double tmp = 0.0;
		for (String name : appWeights.keySet()) {
				switch (name) {
				case "Distance":
					tmp += this.dijkstraSearsch(a1, a2) * appWeights.get(name);
					break;
				case "ResourceShare":
					tmp += this.getResShare(a1, a2) * appWeights.get(name);
					break;
				case "Constraints":
					tmp += this.getConstSim(a1, a2) * appWeights.get(name);
					break;
				case "MessageRate":
					tmp += this.getMsgRateComp(a1, a2) * appWeights.get(name);
					break;
				case "UnitLoad":
					tmp += this.getUnitLoadComp(a1, a2) * appWeights.get(name);
					break;
				case "UtilityWeights":
					tmp += this.getUtilityComp(a1, a2) * appWeights.get(name);
					break;
				case "RequirementSim":
					tmp += this.getReqComp(a1, a2) * appWeights.get(name);
					break;
				}
		}
		return tmp;
	}

	private Double validateClust(Integer minPts) {
		double stdDev = 0.0;
		double min = Double.MAX_VALUE;
		double max = 0.0;
		double avg = 0.0;
		if (this.clust.size()<=1){
			if (this.clust.size()==1){
				return (double) this.clust.get(0).size();
			}else{
				return Double.MAX_VALUE;
			}
		}else{
			for (Set<Integer> clust: this.clust){
				if (clust.size()<min){min=clust.size();}
				if (clust.size()>max){max=clust.size();}
				avg+=clust.size();
			}
			avg=avg/(double)this.clust.size();
			for (Set<Integer> clust: this.clust){
				stdDev+=Math.abs(clust.size()-avg);
			}
			stdDev=stdDev/(double)this.clust.size();
			//System.out.println("Max: "+max+" Min: "+min+" Avg: "+avg+" Std Dev: "+stdDev);
			return stdDev;
		}
	}

	public Set<Integer> expandCluster(Integer p, List<Integer> neighbour, Float eps, Integer minPts) {
		Set<Integer> C = new HashSet<>();
		C.add(p);
		CopyOnWriteArrayList<Integer> neighbourSafe = new CopyOnWriteArrayList<Integer>();
		neighbourSafe.addAll(neighbour);
		// System.out.println(neighbourSafe);
		for (Integer n : neighbourSafe) {
			if (!this.visited.contains(n)) {
				this.visited.add(n);
				List<Integer> neighbourInt = getNeighbours(n, eps, 3);
				if (neighbourInt.size() >= minPts) {
					for (Integer n2 : neighbourInt) {
						if (!neighbourSafe.contains(n2)) {
							neighbourSafe.add(n2);
						}
					}
				}
				C.add(n);
			}
		}
		return C;
	}
	
	public Set<Integer> expandCluster(Integer p, List<Integer> neighbour, Float eps, Integer minPts,Integer maxPts) {
		Set<Integer> C = new HashSet<>();
		C.add(p);
		CopyOnWriteArrayList<Integer> neighbourSafe = new CopyOnWriteArrayList<Integer>();
		neighbourSafe.addAll(neighbour);
		// System.out.println(neighbourSafe);
		for (Integer n : neighbourSafe) {
			if (!this.visited.contains(n)) {
				this.visited.add(n);
				List<Integer> neighbourInt = getNeighbours(n, eps, 4);
				if (neighbourInt.size() >= minPts) {
					for (Integer n2 : neighbourInt) {
						if (!neighbourSafe.contains(n2)) {
							neighbourSafe.add(n2);
						}
					}
				}
				if (C.size()<maxPts){
					C.add(n);
				}else{
					return C;
				}
			}
		}
		return C;
	}
	
	public List<Integer> getNeighbours(Integer p, Float eps, Integer lvl) {
		//System.out.println("Point: "+p+"Eps: "+eps+" Weights:"+appWeights);
		List<Integer> neighbour = new ArrayList<>();
		//Get Apps Close to this one Only use Those maybe 
		Map<Integer, Double> apps = dijkstraSearsch(p,lvl.floatValue());
		for (Integer app:new HashSet<Integer>(apps.keySet())){
			if (apps.get(app)>lvl){
				apps.remove(app);
			}
		}
		for (Integer a : apps.keySet()) {
			double tmp = 0.0;
			for (String name : appWeights.keySet()) {
				if (a != p) {
					switch (name) {
					case "Distance":
						tmp += this.dijkstraSearsch(p, a) * appWeights.get(name);
						break;
					case "ResourceShare":
						tmp += this.getResShare(p, a) * appWeights.get(name);
						break;
					case "Constraints":
						tmp += this.getConstSim(p, a) * appWeights.get(name);
						break;
					case "MessageRate":
						tmp += this.getMsgRateComp(p, a) * appWeights.get(name);
						break;
					case "UnitLoad":
						tmp += this.getUnitLoadComp(p, a) * appWeights.get(name);
						break;
					case "UtilityWeights":
						tmp += this.getUtilityComp(p, a) * appWeights.get(name);
						break;
					case "RequirementSim":
						tmp += this.getReqComp(p, a) * appWeights.get(name);
						break;
					}
				}
			}
			//System.out.println("App:"+p+" App2:"+a+ "Value:"+tmp);
			if (tmp > eps) {
				neighbour.add(a);
			}
		}
		return neighbour;
	}

	public double distanceToCluster(Integer p, Set<Integer> clustP) {
		//Same as Before just for a cluster and not apps
		double tmp = 0.0;
		for (Integer a : clustP) {
			for (String name : appWeights.keySet()) {
				if (a != p) {
					switch (name) {
					case "Distance":
						tmp += this.dijkstraSearsch(p, a) * appWeights.get(name);
						break;
					case "ResourceShare":
						tmp += this.getResShare(p, a) * appWeights.get(name);
						break;
					case "Constraints":
						tmp += this.getConstSim(p, a) * appWeights.get(name);
						break;
					case "MessageRate":
						tmp += this.getMsgRateComp(p, a) * appWeights.get(name);
						break;
					case "UnitLoad":
						tmp += this.getUnitLoadComp(p, a) * appWeights.get(name);
						break;
					case "UtilityWeights":
						tmp += this.getUtilityComp(p, a) * appWeights.get(name);
						break;
					case "RequirementSim":
						tmp += this.getReqComp(p, a) * appWeights.get(name);
						break;
					}
				}
			}
		}
		return tmp/clustP.size();
	}

	public void noiseSort() {
		Set<Integer> noise = new HashSet<Integer>(this.fog.getApps().keySet());
		for (Set<Integer> clInt : this.clust) {
			noise.removeAll(clInt);
		}
		for (Integer p : noise) {
			Double minDist = Double.POSITIVE_INFINITY;
			Set<Integer> tmpCls = new HashSet<>();
			for (Set<Integer> cls : this.clust) {
				Double tmpDist = distanceToCluster(p, cls);
				if (tmpDist < minDist) {
					minDist = tmpDist;
					tmpCls = cls;
				}
			}
			if (tmpCls.size() == 0) {
				// Select random gw, add to it
				int max = 20;
				if (this.clust.size() != 0) {
					// Check if we found any Clusters at all
					if (this.clust.size() != 1) {
						Random rnd = new Random();
						int r = rnd.nextInt(this.clust.size() - 1);
						int maxRetry = 0;
						while (maxRetry < max) {
							maxRetry++;
							r = rnd.nextInt(this.clust.size() - 1);
						}
						if (maxRetry == max) {
							// Create Cluster if none found
							Set<Integer> newCls = new HashSet<Integer>();
							newCls.add(p);
							this.clust.add(newCls);
						}else{
							this.clust.get(r).add(p);
						}
					}else{
						Set<Integer> newCls = new HashSet<Integer>();
						newCls.add(p);
						this.clust.add(newCls);
					}
				} else {
					// Create Cluster if none found
					Set<Integer> newCls = new HashSet<Integer>();
					newCls.add(p);
					this.clust.add(newCls);
				}
			} else {
				// A min Distance Cluster was found, we should add the point to
				// it
				tmpCls.add(p);
			}
		}
	}


	public void noiseSort(int maxPts) {
		Set<Integer> noise = new HashSet<Integer>();
		for (Integer a : this.fog.getApps().keySet()) {
			boolean found = false;
			for (Set<Integer> clInt : this.clust) {
				for (Integer a1 : clInt) {
					if (a1 == a) {
						found = true;
					}
				}
			}
			if (found == false) {
				noise.add(a);
			}
		}
		for (Integer p : noise) {
			Double minDist = Double.POSITIVE_INFINITY;
			Set<Integer> tmpCls = new HashSet<>();
			for (Set<Integer> cls : this.clust) {
				Double tmpDist = distanceToCluster(p, cls);
				if (tmpDist < minDist) {
					minDist = tmpDist;
					tmpCls = cls;
				}
			}
			if (tmpCls.size() == 0 || tmpCls.size()>maxPts) {
				// Select random gw, add to it
				int max = 20;
				if (this.clust.size() != 0) {
					// Check if we found any Clusters at all
					if (this.clust.size() != 1) {
						Random rnd = new Random();
						int r = rnd.nextInt(this.clust.size() - 1);
						int maxRetry = 0;
						while (maxRetry < max && this.clust.get(r).size()>maxPts) {
							maxRetry++;
							r = rnd.nextInt(this.clust.size() - 1);
						}
						if (maxRetry == max) {
							// Create Cluster if none found
							Set<Integer> newCls = new HashSet<Integer>();
							newCls.add(p);
							this.clust.add(newCls);
						}else{
							this.clust.get(r).add(p);
						}
					}else{
						Set<Integer> newCls = new HashSet<Integer>();
						newCls.add(p);
						this.clust.add(newCls);
					}
				} else {
					// Create Cluster if none found
					Set<Integer> newCls = new HashSet<Integer>();
					newCls.add(p);
					this.clust.add(newCls);
				}
			} else {
				// A min Distance Cluster was found, we should add the point to
				// it
				tmpCls.add(p);
			}
		}
	}
	//Distribute Clusters to Gateways, sometimes wen eed to share so we need
	//Max Share to say how many clusters can share 1 resource 
	public void distributeGw2Cluster(int maxShare, double shareThreshold) {
		// TODO Auto-generated method stub
		//System.out.println("----- Distributing -----");
		/*System.out.println("Distribute " + this.fog.getGateways().size() + " Gateways to "
				+ this.fog.getClusters().size() + " Clusters with Rate: " + this.fog.getShareRate()
				+ " having MaxShare: " + maxShare + " Threshold: " + shareThreshold);*/
		// Minmax for values:
		//Set<Integer> gwNoise = new HashSet<Integer>();
		//Set<Integer> clsNoise = new HashSet<Integer>();
		Map<String, Map<String, Double>> minMax = getMinMaxs();
		Map<Integer, Map<Integer, Double>> clsGwPrefs = new HashMap<>();
		for (Integer g : this.fog.getGateways().keySet()) {
			double max = 0.0;
			for (Integer c : this.fog.getClusters().keySet()) {
				// Get Distance between clust and Gw
				double dist = cluster2GwDistance(c, g, minMax);
				if (dist > 0.0) {
					if (dist > max) {
						max = dist;
					}
					// Add info propperly
					if (clsGwPrefs.get(c) == null) {
						Map<Integer, Double> gwtmp = new HashMap<>();
						gwtmp.put(g, dist);
						clsGwPrefs.put(c, gwtmp);
					} else {
						clsGwPrefs.get(c).put(g, dist);
					}
				}
			}
		}
		int emptyCls = 0;
		while (emptyCls != clsGwPrefs.size()) {
			List<Integer> clust = new ArrayList<Integer>(clsGwPrefs.keySet());
			Collections.shuffle(clust);
			emptyCls=0;
			for (Integer c : clust) {
				if (this.fog.getClusters().get(c).ShareRate()>this.fog.getShareRate()*0.8){
					Map<Integer,Double> empty = new HashMap<Integer,Double>();
					clsGwPrefs.put(c, empty);
				}
				if (clsGwPrefs.get(c).size() == 0) {
					emptyCls++;
				} else {
					//System.out.println(clsGwPrefs.get(c));
					// Might be better to switch this for for a while that stops
					// when all gws are allocated
					// It should just go through the Clusters and not consider
					// those that are 0.1 above the system Avg
					// If not null then do the allocations
					Map<Integer, Double> clsOfInter = new HashMap<>();
					// Get Gw of Interest
					Integer maxGw = 0;
					Double maxDist = 0.0;
					for (Integer g : clsGwPrefs.get(c).keySet()) {
						if (clsGwPrefs.get(c).get(g) > maxDist) {
							maxDist = clsGwPrefs.get(c).get(g);
							maxGw = g;
						}
					}
					// Get Competing Clusters
					Double maxCompDist = 0.0;
					for (Integer c2 : clsGwPrefs.keySet()) {
						if (this.fog.getClusters().get(c2).ShareRate() < this.fog.getShareRate()*0.8) {
							if (c2 != c && clsGwPrefs.get(c2).get(maxGw) != null) {
								//System.out.println(adjustClsDistByShare(c2, clsGwPrefs.get(c2).get(maxGw)));
								if (maxCompDist < adjustClsDistByShare(c2, clsGwPrefs.get(c2).get(maxGw))) {
									maxCompDist = adjustClsDistByShare(c2, clsGwPrefs.get(c2).get(maxGw));
								}
								if (adjustClsDistByShare(c2,
										clsGwPrefs.get(c2).get(maxGw)) > adjustClsDistByShare(c, maxDist)
												* shareThreshold) {
									clsOfInter.put(c2, adjustClsDistByShare(c2, clsGwPrefs.get(c2).get(maxGw)));
								}
							}
						}
					}
					if (maxCompDist * shareThreshold > adjustClsDistByShare(c, maxDist)) {
						// Gw Should not be allocated to this cluster
						clsGwPrefs.get(c).remove(maxGw);
						// This may be a mistStake
					} else {
						// This Cluster should get a share and we know the Other
						// Clusters and their values
						clsOfInter.put(c, adjustClsDistByShare(c, maxDist));
						distributeGwtoCls(clsOfInter, maxGw, maxShare);
						// Remove Gw if it has been Allocated
						for (Integer cInt : clsGwPrefs.keySet()) {
							clsGwPrefs.get(cInt).remove(maxGw);
						}
					}
					/*
					 * System.out.println(clsGwPrefs.get(c));
					 * System.out.println(clsOfInter);
					 * System.out.println("Maxs:"+maxGw+ "Dist: "
					 * +adjustClsDistByShare(c,maxDist)+" CompDist: "
					 * +maxCompDist); break;
					 */
				}
			}
		} // End of CLuster Loop
	}
	
	public void sampleGw2Cluster() {
		Map<String, Map<String, Double>> minMax = getMinMaxs();
		Map<Integer, Map<Integer, Double>> clsGwPrefs = new HashMap<>();
		Integer c = new ArrayList<Integer>(this.fog.getClusters().keySet()).get(0);
		for (Integer g : this.fog.getGateways().keySet()) {
			double max = 0.0;
			// Get Distance between clust and Gw
			double dist = cluster2GwDistance(c, g, minMax);
			if (dist > 0.0) {
				// Add info propperly
				if (clsGwPrefs.get(c) == null) {
					Map<Integer, Double> gwtmp = new HashMap<>();
					gwtmp.put(g, dist);
					clsGwPrefs.put(c, gwtmp);
				} else {
					clsGwPrefs.get(c).put(g, dist);
				}
			}
		}
		int emptyCls = 0;
		while (emptyCls != clsGwPrefs.size()) {
			List<Integer> clust = new ArrayList<Integer>(clsGwPrefs.keySet());
			Collections.shuffle(clust);
			emptyCls = 0;
			if (this.fog.getClusters().get(c).ShareRate() >= this.fog.getShareRate()*0.95) {
				Map<Integer, Double> empty = new HashMap<Integer, Double>();
				clsGwPrefs.put(c, empty);
			}
			if (clsGwPrefs.get(c).size() == 0) {
				emptyCls++;
			} else {
				// System.out.println(clsGwPrefs.get(c));
				// Might be better to switch this for for a while that stops
				// when all gws are allocated
				// It should just go through the Clusters and not consider
				// those that are 0.1 above the system Avg
				// If not null then do the allocations
				Map<Integer, Double> clsOfInter = new HashMap<>();
				// Get Gw of Interest
				Integer maxGw = 0;
				Double maxDist = 0.0;
				for (Integer g : clsGwPrefs.get(c).keySet()) {
					if (clsGwPrefs.get(c).get(g) > maxDist) {
						maxDist = clsGwPrefs.get(c).get(g);
						maxGw = g;
					}
				}
				// This Cluster should get a share and we know the Other
				// Clusters and their values
				if (this.fog.getClusters().get(c).ShareRate() < this.fog.getShareRate()*0.95) {
					clsOfInter.put(c, adjustClsDistByShare(c, maxDist));
					distributeGwtoCls(clsOfInter, maxGw, 1);
					// Remove Gw if it has been Allocated
					for (Integer cInt : clsGwPrefs.keySet()) {
						clsGwPrefs.get(cInt).remove(maxGw);
					}
				}
				/*
				 * System.out.println(clsGwPrefs.get(c));
				 * System.out.println(clsOfInter);
				 * System.out.println("Maxs:"+maxGw+ "Dist: "
				 * +adjustClsDistByShare(c,maxDist)+" CompDist: " +maxCompDist);
				 * break;
				 */
			}
		} // End of CLuster Loop
	}
	
	private void distributeGwtoCls(Map<Integer, Double> clsOfInter,Integer g,int maxShare) {
		// Distributes and Allocated Gateway to Clusters
		//System.out.println("Distribute");
		//System.out.println(clsOfInter);
		//Remove smallest until size is equal or smaller than maxShare
		while ( clsOfInter.size()>maxShare){
			Double min = Double.MAX_VALUE;
			int minI=0;
			for (Integer c:clsOfInter.keySet()){
				if (clsOfInter.get(c)<min){
					min=clsOfInter.get(c);
					minI=c;
				}
			}
			clsOfInter.remove(minI);
		}
		//Now take rest and allocate equally
		Double gwResA = (100.0-this.fog.getGateways().get(g).getGwBaseLoad())/(double)clsOfInter.size();
		for (Integer c: clsOfInter.keySet()){
			this.fog.getClusters().get(c).addGateway(this.fog.getGateways().get(g),
					gwResA.floatValue(), (float) 0.0);
			this.fog.getGateways().get(g).addCluster(this.fog.getClusters().get(c),
					gwResA.floatValue(), (float) 0.0);
		}
		//System.out.println("Base: "+this.fog.getGateways().get(g).getGwBaseLoad()+" Per Cls given: "+gwResA);
		//System.out.println(clsOfInter);
		
	}

	public double adjustClsDistByShare(Integer cls, Double ret){
		double share = this.fog.getClusters().get(cls).ShareRate();
		if (share <= 1.2) {
			ret = ret /0.1;
		}else{
			ret= ret / share;
		}
		return ret;
	}
	
	public Double cluster2GwDistance(int cluster, int gw, Map<String, Map<String, Double>> minMax){	
		//For all the application inside cluster, find distance to Gw 
		//return this the average distance
		double totDist = 0.0;
		for (Integer a: this.fog.getClusters().get(cluster).getApps().keySet()){
			totDist+=app2GwDistance(a, gw,minMax);
		}
		double ret = totDist/(double)this.fog.getClusters().get(cluster).getApps().size();
		//Need to adjust for share rate which might be 0, in which case an arbitrary 0.5 ? should be added
		//This could be done later when looking at which cls should get the alloc because share rates change
		/*double share = this.fog.getClusters().get(cluster).ShareRate();
		if (share <= 0.5) {
			ret = ret /0.5;
		}else{
			ret= ret / this.fog.getClusters().get(cluster).ShareRate();
		}*/
		return ret;
	}
	
	public Double app2GwDistance(int a, int g, Map<String, Map<String, Double>> minMax){	
		// Get distance of app to gw based on weights
		Double ret = 0.0;
		// Find parameters that you may think that inlfuences why apps chose
		// Gateways
		for (String name : gwWeights.keySet()) {
			switch (name) {
			case "Capabilities":
				ret += this.appToGwCapability(a, g) * gwWeights.get(name);
				break;
			case "PerfToULoad":
				ret += this.perfToULoad(a, g, minMax) * gwWeights.get(name);
				break;
			case "CapToULoad":
				ret += this.capToULoad(a, g, minMax) * gwWeights.get(name);
				break;
			case "SharedRes":
				ret += this.gwResShare(a, g) * gwWeights.get(name);
				break;
			case "BaseLoad":
				ret += this.gwBaseToApp(a, g, minMax) * gwWeights.get(name);
				break;
			}
		}
		return ret;
	}
	
	//Done fairly but randomly 
	public void resolveApptoGwNoise() {
		// Delete all rubbish allocations
		// Redistribute things to make sense
		for (Integer g : this.fog.getGateways().keySet()) {
			Float tot = this.fog.getGateways().get(g).getGwBaseLoad();
			for (Integer c : this.fog.getGateways().get(g).getCluster().keySet()) {
				tot += this.fog.getGateways().get(g).getClusterShare(c);
			}
			// No Clusters assigned to GW need to assing
			//System.out.println("Gateway: "+g+" Tot: "+tot+" Base:"+this.fog.getGateways().get(g).getGwBaseLoad()+"Cls:"+this.fog.getGateways().get(g).getAllClusterShare().size());
			if (tot < 100.0 && tot < this.fog.getGateways().get(g).getGwBaseLoad()*1.1) {
				//System.out.println("Gateway: "+g+" Fixing");
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
