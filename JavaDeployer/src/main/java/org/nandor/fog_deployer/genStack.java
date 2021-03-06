package org.nandor.fog_deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class genStack {
	
	List<Generation> generations = new ArrayList<>();
	private int size = 1;
	private Float low = (float)0.0;
	private Comparator comp;
	public genStack(int size){
		this.size = size;
		comp =  new Comparator<Generation>() {

			@Override
			public int compare(Generation o1, Generation o2) {
				if (o1.getUtility()>o2.getUtility()){
					return -1;
				}else if (o1.getUtility()<o2.getUtility()){
					return 1;
				}else{
					return 0;
				}
			}
		};
	}
	
	public void addGeneration(Map<Integer, Integer> gen, Float utility) {
		Collections.sort(generations, comp);
		if (isUnique(gen)) {
			if (generations.size() < size) {
				generations.add(new Generation(gen, utility));
			} else if (utility > generations.get(generations.size() - 1).getUtility()) {
				generations.remove(generations.size() - 1);
				generations.add(new Generation(gen, utility));
			}
			Collections.sort(generations, comp);
			low = generations.get(generations.size() - 1).getUtility();
		}
	}
	
	public boolean isUnique(Map<Integer, Integer> gen){
		for (Generation g:generations){
			if (g.comp(gen)){
				return false;
			}
		}
		return true;
	}
	
	public Float getLowestUtil(){
		return low;
	}
	
	public void showGeneratons(){
		System.out.println(generations);
	}

	public Map<Integer, Integer> getBest() {
		Collections.sort(generations, comp);
		if (generations.size()>0){
			return generations.get(0).getGeneration();
		}else{
			return null;
		}
	}

	public List<Map<Integer, Integer>>  getGenerations() {
		List<Map<Integer, Integer>> ret = new ArrayList<>();
		for (Generation g: generations){
			ret.add(g.getGeneration());
		}
		return ret;
	}

}

class Generation{
	Map<Integer,Integer> gen = new HashMap<>();
	Float utility = (float) 0.0;
	
	public Generation(Map<Integer,Integer> gen,Float utility){
		this.gen=gen;
		this.utility=utility;
	}
	
	public boolean comp(Map<Integer, Integer> gen2) {
		for (Integer g:gen2.keySet()){
			if (gen.get(g)!=gen2.get(g)){
				return false;
			}
		}
		return true;
	}

	public Float getUtility(){
		return utility;
	}
	
	public Map<Integer,Integer> getGeneration(){
		return gen;
	}
	
    public String toString() {
        return "Utility: " + getUtility() + " for Gen: " + getGeneration();
    }
	
}