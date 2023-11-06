package scratch.ned.nshm23;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.opensha.commons.calc.magScalingRelations.magScalingRelImpl.WC1994_MagLengthRelationship;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.geo.json.Feature;
import org.opensha.commons.geo.json.FeatureProperties;
import org.opensha.sha.earthquake.ProbEqkSource;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemRupSet;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemSolution;
import org.opensha.sha.earthquake.param.IncludeBackgroundOption;
import org.opensha.sha.faultSurface.FaultSection;
import org.opensha.sha.faultSurface.GeoJSONFaultSection;
import org.opensha.sha.faultSurface.RuptureSurface;
import org.opensha.sha.faultSurface.StirlingGriddedSurface;
import org.opensha.sha.magdist.IncrementalMagFreqDist;
import org.opensha.sha.magdist.SummedMagFreqDist;
import org.opensha.sha.util.TectonicRegionType;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.usgs.earthquake.nshmp.model.FaultRuptureSet;
import gov.usgs.earthquake.nshmp.model.NshmErf;
import gov.usgs.earthquake.nshmp.model.NshmSource;
import gov.usgs.earthquake.nshmp.model.SystemRuptureSet;


public class CEUS_FSS_creator {
	
	final static boolean D = true;
	
	 private static final Gson GSON = new GsonBuilder().create();

	
	 /**
	  * I first tried mapping ruptures to surfaces, but some mapped to both USGS and CUES SSCn surfaces 
	  * (which are on different logic trees).  I now do this from Peter's files.
	  */
	private static void getSurfacesForSources() {
	}
	
	private static ArrayList<GeoJSONFaultSection> getFaultSectionList(String nshmModelDirPath) {
		ArrayList<GeoJSONFaultSection> list = new ArrayList<GeoJSONFaultSection>();
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/features/Cheraw (SSCn).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/features/Cheraw.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/Commerce/features/commerce.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (Axial, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (Axial, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (Bootheel).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (Charleston Uplift).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (New Madrid, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (New Madrid, west).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (Reelfoot, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - SSCn (Reelfoot, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (center, center).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (center, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (center, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (east, center).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (east, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (east, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (mid-east, center).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (mid-east, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (mid-east, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (mid-west, center).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (mid-west, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (mid-west, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (west, center).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (west, north).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/features/New Madrid - USGS (west, south).geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/OK/Meers/features/Meers.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (North)/features/eastern-rift-margin-north.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/features/crittenden-county.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/features/eastern-rift-margin-south-extension.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/features/eastern-rift-margin-south.geojson"));
		list.add(getFaultSection(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/features/meeman-shelby.geojson"));
		return list;
	}
	
	
	private static GeoJSONFaultSection getFaultSection(String filePathString) {
		Feature feature=null;
		try {
			feature = Feature.read(new File(filePathString));
		} catch (IOException e) {
			System.out.println("Problem with input file: "+filePathString);
			e.printStackTrace();
		}
		return GeoJSONFaultSection.fromNSHMP_HazFeature(feature);
	}
	
	private static boolean areSourceRupSurfacesIdentical(ProbEqkSource src) {
		if(src.getNumRuptures()==1)
			return true;
		RuptureSurface firstSurface = src.getRupture(0).getRuptureSurface();
		int numPtsFirst = firstSurface.getEvenlyDiscretizedNumLocs();
		LocationList firstLocList = firstSurface.getEvenlyDiscritizedListOfLocsOnSurface();
		for(int s=1;s<src.getNumRuptures();s++) {
			RuptureSurface surface = src.getRupture(s).getRuptureSurface();
			if(surface.getEvenlyDiscretizedNumLocs() != numPtsFirst)
				return false;
			LocationList locList = surface.getEvenlyDiscritizedListOfLocsOnSurface();
			if(firstLocList.size() != locList.size())
				return false;
			for(int l=0;l<firstLocList.size();l+=5)
				if(!firstLocList.get(l).equals(locList.get(l)))
					return false;
		}
		
		return true;
	}
	
	
	private static boolean isPointSource(NshmSource src) {
    	int numLocs = src.getRupture(0).getRuptureSurface().getEvenlyDiscretizedNumLocs();
    	if(numLocs == 1) 
    		return true;
    	else
    		return false;
	}
	
	
	public static ArrayList<FaultSystemSolution> getFaultSystemSolutionList(String nshmModelDirPath) {

		// get parent fault sections from Peter's features files
		ArrayList<GeoJSONFaultSection> faultSectionData = getFaultSectionList(nshmModelDirPath);
//		try {
//			System.out.println(faultSectionData.get(0).toFeature().toJSON());
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}

		ArrayList<Integer> parSectID_List = new ArrayList<Integer>();
		
		// make parSectID_List
		if(D) System.out.println("index\tsectID\trake");
		for(int s=0;s<faultSectionData.size();s++) {
			GeoJSONFaultSection sect = faultSectionData.get(s);
			if(!parSectID_List.contains(sect.getSectionId())) {
				parSectID_List.add(sect.getSectionId());
				if(D) System.out.println(s+"\t"+sect.getSectionId()+"\t"+sect.getAveRake());
			}
			else
				throw new RuntimeException("section IDs are not unique; duplicate: "+sect.getSectionId());
		}
		if (D)System.out.println("parSectID_List.size()="+parSectID_List.size());
		
		// Read from Peter's rupture-set.json and and cluster-set.json files
//		ArrayList<Integer> srcIDsList = new ArrayList<Integer>();  // a list of all the source IDs (no duplicates)
		HashMap<Integer,int[]> srcFltSectsMap = new HashMap<Integer,int[]>(); // the fault section used by each source (same order as above)
		getSrcIDsAndFaultSectionsLists(srcFltSectsMap, nshmModelDirPath);
		Set<Integer> srcIDsList = srcFltSectsMap.keySet();  // a list of all the source IDs (no duplicates)

		// some tests
		if(D) System.out.println("number of unique source IDs: "+srcIDsList.size());
		// make sure all sections are used and none are missing (with respect to Peter's files)
		ArrayList<Integer> testParSectID_List = new ArrayList<Integer>(); // fill up with the par IDs from all the sources
		for(int srcID:srcFltSectsMap.keySet()) {
//			for(int i=0;i<srcIDsList.size();i++) {
			int[] sects = srcFltSectsMap.get(srcID);
			if(sects.length==1)
				if(sects[0] != srcID) // source ID = faultSection ID if only one fault section used
					throw new RuntimeException("problem");

			if(D)System.out.print("\n"+srcID+"\t");
			for(int sect:sects) {
				if (D)System.out.print(sect+", ");	
				if(!testParSectID_List.contains(sect))
					testParSectID_List.add(sect);
			}
		}
		System.out.print("\n");
    	if(parSectID_List.size() != testParSectID_List.size())
    		throw new RuntimeException("parSectID_List.size() != testParSectID_List.size()");
    	for(int id : testParSectID_List)
    		if(!parSectID_List.contains(id))
    			throw new RuntimeException("parSectID_List does not contain: "+id);
    	if(D)System.out.println("parSectID_List passed tests");
		
		
		// create the ERF
		NshmErf erf = getNshmERF(nshmModelDirPath);
	    erf.getTimeSpan().setDuration(1.0);
	    erf.updateForecast();
		ArrayList<Integer> floaterSrcID_List = new ArrayList<Integer>();

	    if (D) System.out.println("NSHM ERF NumSources: " + erf.getNumSources());
		ArrayList<Integer> testSrcIDsList = new ArrayList<Integer>();  // a list of all the source IDs (no duplicates)
	    int numPtSrces=0;
	    for(int s=0;s<erf.getNumSources();s++) {
	    	NshmSource src = (NshmSource)erf.getSource(s);
	    	if(isPointSource(src)) { // skip gridded seismicity sources
	    		numPtSrces+=1;
	    		continue;
	    	}
	    	boolean noFloaters = areSourceRupSurfacesIdentical(src); // look for ruptures that have area less than the full fault
	    	if(!noFloaters) {
	    		floaterSrcID_List.add(src.getNSHM_ID());
	    	}
	    	boolean srcIdEqualsSectionId = parSectID_List.contains(src.getNSHM_ID());
	    	if(!testSrcIDsList.contains(src.getNSHM_ID()))
	    		testSrcIDsList.add(src.getNSHM_ID());
	    	if (D)System.out.println(s+"\t"+src.getNumRuptures()+"\t"+noFloaters+"\t"+srcIdEqualsSectionId+"\t"+src.getNSHM_ID()+"\t"+src.getName());
	    }

	    if(D) {
	    	System.out.println("numPtSrces="+numPtSrces+"\tnumFltSrces="+(erf.getNumSources()-numPtSrces));
	    	System.out.println("Floater Source IDs:");
		    for(int id:floaterSrcID_List)
		    	System.out.println("\t"+id);
	    }

	    // run more tests; first make sure ERF sources are consistent with Peter's files
    	if(testSrcIDsList.size() != srcIDsList.size())
    		throw new RuntimeException("testSrcIDsList.size() != srcIDsList.size()");
    	for(int id : testSrcIDsList)
    		if(!srcIDsList.contains(id))
    			throw new RuntimeException("srcIDsList does not contain: "+id);
    	if(D)System.out.println("srcIDsList passed tests");

    	// Compute MFDs for each source
    	HashMap<Integer, SummedMagFreqDist> mfdForSrcIdMap = new HashMap<Integer, SummedMagFreqDist>();
    	HashMap<Integer, Double> rakeForSrcIdMap = new HashMap<Integer, Double>();
    	HashMap<Integer, String> nameForSrcIdMap = new HashMap<Integer, String>();
    	for(int id:srcIDsList) {
        	mfdForSrcIdMap.put(id, new SummedMagFreqDist(5.05,40,0.1));
    	}
	    for(int s=0;s<erf.getNumSources();s++) {
	    	NshmSource src = (NshmSource)erf.getSource(s);
	    	if(isPointSource(src)) 
	    		continue;	
	    	Integer srcID = src.getNSHM_ID();
	    	if(nameForSrcIdMap.keySet().contains(srcID)) { // check for name change
	    		String firstName = nameForSrcIdMap.get(srcID);
	    		if(!firstName.equals(src.getName()))  // all pass this test
	    			System.out.println("WARNING: Soource name change in ERF for ID="+srcID+";\t"+firstName+",\t"+nameForSrcIdMap.get(srcID));
	    	}
	    	else { // add name
	    		nameForSrcIdMap.put(srcID, src.getName());
	    	}
	    	SummedMagFreqDist mfd = mfdForSrcIdMap.get(srcID);
	    	for(int r=0;r<src.getNumRuptures();r++) {
	    		double mag = src.getRupture(r).getMag();
	    		int iMag = mfd.getClosestXIndex(mag);
	    		double rate = src.getRupture(r).getProbability();  // rate approx equal to prob
	    		mfd.add(iMag, rate); // this requires the exact x value (no tolerance)
	    		double rake = src.getRupture(r).getAveRake();
	    		if(rakeForSrcIdMap.containsKey(srcID)) {
	    			if(rakeForSrcIdMap.get(srcID) != rake)
	    				throw new RuntimeException("rake change within source");
	    			else
	    				rakeForSrcIdMap.put(srcID, rake);
	    		}
	    	}
	    }
	    // print min and max mag for each src mfd
	    if(D) {
		    for(int srcID:mfdForSrcIdMap.keySet()) {
		    	SummedMagFreqDist mfd = mfdForSrcIdMap.get(srcID);
		    	System.out.println(srcID+"\t"+(float)mfd.getMinMagWithNonZeroRate()+"\t"+(float)mfd.getMaxMagWithNonZeroRate()+"\trake="+
		    	rakeForSrcIdMap.get(srcID)+"\t"+nameForSrcIdMap.get(srcID));
		    }
	    }
	    
	    // now make the big fss, we need the following without floater sources and with fault section indices that start from zero
	    // HashMap<Integer, SummedMagFreqDist> mfdForSrcIdMap, 
		// HashMap<Integer, ArrayList<Integer>> surfListForSrcIdMap, 
	    // ArrayList<GeoJSONFaultSection> faultSectionData
	    
	    HashMap<Integer, SummedMagFreqDist> mfdForSrcIdMapSubset = new HashMap<Integer, SummedMagFreqDist>();
	    HashMap<Integer, ArrayList<Integer>> surfListForSrcIdMapSubset = new HashMap<Integer, ArrayList<Integer>>();
	    ArrayList<GeoJSONFaultSection> faultSectionDataSubset = new ArrayList<GeoJSONFaultSection>();
	    HashMap<Integer, Integer> newFltIndexMap = new HashMap<Integer, Integer>();
	    
	    // get a new instance of fault sections list so we can overide the IDs
	    ArrayList<GeoJSONFaultSection> duplicateFaultSectionList = getFaultSectionList(nshmModelDirPath);

	    // make list of fault sections
	    int newFltIndex=0;
	    for(int s=0;s<duplicateFaultSectionList.size();s++) {
	    	GeoJSONFaultSection fltSection = duplicateFaultSectionList.get(s);
	    	int sectID = fltSection.getSectionId();
	    	if(floaterSrcID_List.contains(sectID)) { // this assumes floater sources only have one fault section
	    		if (D) System.out.println("Skipping fault section/floater "+sectID);
	    		continue;
	    	}
	    	fltSection.setParentSectionId(sectID);
	    	fltSection.setSectionId(newFltIndex);
	    	faultSectionDataSubset.add(fltSection);
	    	newFltIndexMap.put(sectID, newFltIndex);
	    	newFltIndex+=1;
	    }
	    // now make HashMaps without floater sources and translated ids:
	    for(int srcID:mfdForSrcIdMap.keySet()) {
	    	if(floaterSrcID_List.contains(srcID)) { // this assumes floater sources only have one fault section
	    		if (D) System.out.println("Skipping fault section/floater "+srcID);
	    		continue;
	    	}
	    	int[] oldSectForSrcArray = srcFltSectsMap.get(srcID);
	    	ArrayList<Integer> newSectForSrcList = new ArrayList<Integer>();
	    	for( int oldSectID:oldSectForSrcArray)
	    		newSectForSrcList.add(newFltIndexMap.get(oldSectID));
	    	surfListForSrcIdMapSubset.put(srcID, newSectForSrcList);
	    	mfdForSrcIdMapSubset.put(srcID, mfdForSrcIdMap.get(srcID));
	    	
	    }
	    
	    FaultSystemSolution bigFSS = getFaultSystemSolution(mfdForSrcIdMapSubset, 
	    		surfListForSrcIdMapSubset, faultSectionDataSubset);
	    
	    
	    if(D) { // test participation mfds for each fault section
	    	
	    	// for FSS:
	    	Boolean testPassed = true;
	    	SummedMagFreqDist fssTotalMFD = new SummedMagFreqDist(5.05,40,0.1);
	    	HashMap<Integer, SummedMagFreqDist> sectMfdMapFSS = new HashMap<Integer, SummedMagFreqDist>();
	    	for(int rup=0;rup<bigFSS.getRupSet().getNumRuptures();rup++) {
	    		double rate = bigFSS.getRateForRup(rup);
	    		double mag = bigFSS.getRupSet().getMagForRup(rup);
	    		int iMag = fssTotalMFD.getClosestXIndex(mag);
	    		List<Integer> sectsForRupList = bigFSS.getRupSet().getSectionsIndicesForRup(rup);
	    		for(int i:sectsForRupList) {
	    			if(!sectMfdMapFSS.keySet().contains(i)) {
	    				sectMfdMapFSS.put(i, new SummedMagFreqDist(5.05,40,0.1));
	    			}
	    			sectMfdMapFSS.get(i).add(iMag, rate);
	    		}
	    		fssTotalMFD.add(iMag, rate);
	    	}
	    	
	    	// From ERF:
	    	SummedMagFreqDist erfTotalMFD = new SummedMagFreqDist(5.05,40,0.1);
	    	HashMap<Integer, SummedMagFreqDist> sectMfdMapERF = new HashMap<Integer, SummedMagFreqDist>();
	    	for(int id:newFltIndexMap.keySet()) {
	    		sectMfdMapERF.put(id, new SummedMagFreqDist(5.05,40,0.1));
	    	}
		    for(int s=0;s<erf.getNumSources();s++) {
		    	NshmSource src = (NshmSource)erf.getSource(s);
		    	if(isPointSource(src)) 
		    		continue;	
		    	Integer srcID = src.getNSHM_ID();
		    	// skip floaters
		    	if(floaterSrcID_List.contains(srcID)) { 
		    		continue;
		    	}
		    	for(int r=0;r<src.getNumRuptures();r++) {
		    		double mag = src.getRupture(r).getMag();
		    		int iMag = erfTotalMFD.getClosestXIndex(mag);
		    		double rate = src.getRupture(r).getProbability();  // rate approx equal to prob
		    		erfTotalMFD.add(iMag, rate); // this requires the exact x value (no tolerance)
			    	for(int sectID:srcFltSectsMap.get(srcID)) {
				    	SummedMagFreqDist mfd = sectMfdMapERF.get(sectID);
				    	mfd.add(iMag, rate);
			    	}
		    	}
		    }
		    // compare
		    for(int i=0;i<fssTotalMFD.size();i++) {
		    	double val1 = fssTotalMFD.getY(i);
		    	double val2 = fssTotalMFD.getY(i);
		    	if(val1 == 0.0) {
		    		if(val2 != 0)
		    			throw new RuntimeException("PROBLEM: zero in one bu not the other at index "+i+"\n"+fssTotalMFD+"\n"+fssTotalMFD);
		    	}
		    	else {
		    		double ratio = val1/val2;
		    		if(ratio<0.99 || ratio > 1.01) {
		    			throw new RuntimeException("PROBLEM: >1% difference at index "+i+"\n"+fssTotalMFD+"\n"+fssTotalMFD);
		    		}
		    	}
		    }
		    for(int sectID:sectMfdMapERF.keySet()) {
		    	SummedMagFreqDist mfd1 = sectMfdMapERF.get(sectID);
		    	SummedMagFreqDist mfd2 = sectMfdMapFSS.get(newFltIndexMap.get(sectID));
			    for(int i=0;i<mfd1.size();i++) {
			    	double val1 = mfd1.getY(i);
			    	double val2 = mfd2.getY(i);
			    	if(val1 == 0.0) {
			    		if(val2 != 0)
			    			throw new RuntimeException("PROBLEM: zero in one bu not the other at index "+i+"\n"+mfd1+"\n"+mfd2);
			    	}
			    	else {
			    		double ratio = val1/val2;
			    		if(ratio<0.99 || ratio > 1.01) {
			    			throw new RuntimeException("PROBLEM: >1% difference at index "+i+"\n"+mfd1+"\n"+mfd2);
			    		}
			    	}
			    }
		    }
		    System.out.println("ERF versus bigFSS tests passed!!");
	    }   

	    
		ArrayList<FaultSystemSolution> fssList = new ArrayList<FaultSystemSolution>();
		fssList.add(bigFSS);
		
		return fssList;
	}
	
	
	/**
	 * This assumes none of the sources have floaters (no partial fault-section ruptures).  
	 * This also assumes fault sections have incremental IDs starting at 0.  Rupture lengths 
	 * and areas are computed from fault sections provided.
	 * @param mfdForSrcIdMap
	 * @param surfListForSrcIdMap
	 * @return
	 */
	private static FaultSystemSolution getFaultSystemSolution(HashMap<Integer, SummedMagFreqDist> mfdForSrcIdMap, 
			HashMap<Integer, ArrayList<Integer>> surfListForSrcIdMap, 
			ArrayList<GeoJSONFaultSection> faultSectionData) {

	    // compute number of ruptures from nonzero rates in MFDs
	    int numRups = 0;
	    for(int srcID:mfdForSrcIdMap.keySet()) {
	    	SummedMagFreqDist mfd = mfdForSrcIdMap.get(srcID);
	    	for(int i=0;i<mfd.size();i++)
	    		if(mfd.getY(i) >0)
	    			numRups += 1;		
	    }
	    
		double[] mags = new double[numRups];
		double[] rakes = new double[numRups];  // defaults to zero here
		double[] rupAreas = new double[numRups];
		double[] rupLengths = new double[numRups];
		double[] rupRates = new double[numRups];
	    List<List<Integer>> sectionForRups = new ArrayList<>();
	    
	    if(D) System.out.println("numRups="+numRups);
	    
	    int r = 0;
	    for(int srcID:mfdForSrcIdMap.keySet()) {
	    	ArrayList<Integer> sectForRupList = surfListForSrcIdMap.get(srcID);
	    	double length = 0;
	    	double area = 0;
	    	double rake = faultSectionData.get(sectForRupList.get(0)).getAveRake();  // get rake of first section
	    	for(int id:sectForRupList) {
	    		length += faultSectionData.get(id).getTraceLength();
	    		area += faultSectionData.get(id).getArea(false);
	    		// check that rake is constant across sections
	    		double tempRake = faultSectionData.get(id).getAveRake();
	    		if(tempRake != rake)
	    			if (D) System.out.println("WARNING: Rake changes among sections for srcID="+srcID
	    					+"\n\t"+tempRake+" for "+faultSectionData.get(id).getName() 
	    					+ "\n\t"+rake+" for "+faultSectionData.get(sectForRupList.get(0)).getName());
	    	}
	    	SummedMagFreqDist mfd = mfdForSrcIdMap.get(srcID);
	    	for(int i=0;i<mfd.size();i++) {
	    		double rate = mfd.getY(i);
	    		if(rate >0) {
	    			rupRates[r] = rate;
	    			mags[r] = mfd.getX(i);
	    			rupAreas[r] = area;
	    			rakes[r] = rake;
	    			rupLengths[r] = length;
	    			sectionForRups.add(sectForRupList);
	    			r += 1;		
	    		}
	    	}
	    }

	    
	    FaultSystemRupSet rupSet = new FaultSystemRupSet(
				faultSectionData,
				sectionForRups,
				mags,
				rakes,
				rupAreas,
				rupLengths); 
	    
	    FaultSystemSolution fss= new FaultSystemSolution(rupSet, rupRates);
	    return fss;
	}
	
	
	
	
	private static NshmErf getNshmERF(String nshmModelDirPath) {
	    Set<TectonicRegionType> trts = EnumSet.of(TectonicRegionType.STABLE_SHALLOW);
	    NshmErf erf = new NshmErf(Path.of(nshmModelDirPath), trts, IncludeBackgroundOption.EXCLUDE);
	    return erf;
	}
	
	
	private static void testForPeter(String nshmModelDirPath) {
	    Set<TectonicRegionType> trts = EnumSet.of(TectonicRegionType.STABLE_SHALLOW);
	    NshmErf erf = new NshmErf(Path.of(nshmModelDirPath), trts, IncludeBackgroundOption.EXCLUDE);
	    erf.getTimeSpan().setDuration(1.0);
	    erf.updateForecast();
	    int s=232; // Cheraw USGS source
	    NshmSource src = (NshmSource)erf.getSource(s); 
    	System.out.println(s+"\t"+src.getName()+"\t"+src.getNSHM_ID()+"\t"+src.getNumRuptures());
	    for(int r=0;r<src.getNumRuptures();r++) {
	    	double area = src.getRupture(r).getRuptureSurface().getArea();
	    	double ddw = src.getRupture(r).getRuptureSurface().getAveWidth();
	    	double tor = src.getRupture(r).getRuptureSurface().getAveRupTopDepth();
	    	double len = src.getRupture(r).getRuptureSurface().getAveLength();
		    System.out.println("\t"+r+"\t"+src.getRupture(r).getMag()+"\t"+(float)area+"\t"+(float)ddw+"\t"+(float)tor+"\t"+(float)len);
	    }
	}
	
	/**
	 * This is for parsing Peter's cluster-set.json files
	 * @param filePath
	 */
	private static void parseClusterSetFile(String filePath, HashMap<Integer,int[]> srcFltSectsMap) {
		 String ID = "id";
		 String NAME = "name";
		 String SECTIONS = "sections";
		 String RUPTURE_SETS = "rupture-sets";

		Path path = Paths.get(filePath);
		
	    JsonObject obj=null;
	    try (BufferedReader br = Files.newBufferedReader(path)) {
	    	obj = JsonParser.parseReader(br).getAsJsonObject();
	    } catch (IOException ioe) {
	    	throw new RuntimeException(ioe);
	    }
	    
//	    System.out.println("Cluster set: "+obj.get(ID).getAsInt()+"\t"+obj.get(NAME).getAsString());
	    
	    JsonArray rupSets = obj.get(RUPTURE_SETS).getAsJsonArray();
	    for (JsonElement rupSet : rupSets) {
	    	JsonObject rupSetObj = rupSet.getAsJsonObject();
		    int srcID = rupSetObj.get(ID).getAsInt();
		    int[] fltSectionIDs;

		    if (rupSetObj.has(SECTIONS)) {
			      fltSectionIDs = GSON.fromJson(rupSetObj.get(SECTIONS), int[].class);
		    } else {
		    	fltSectionIDs = new int[]{srcID}; // section ID same as src ID
		    }
		    
		    if(!srcFltSectsMap.keySet().contains(srcID)) { // no need for redundancies because src IDs have unique rup surface
		    	srcFltSectsMap.put(srcID, fltSectionIDs);
		    }

//				System.out.println("\t"+srcID);
	    }
	}

	
	
	/**
	 * This is for parsing Peter's rupture-set.json files
	 * @param filePath
	 */
	private static void parseRuptureSetFile(String filePath,HashMap<Integer,int[]> srcFltSectsMap) {
		
		 String ID = "id";
		 String NAME = "name";
		 String SECTIONS = "sections";

		Path path = Paths.get(filePath);
		
	    JsonObject obj=null;
	    try (BufferedReader br = Files.newBufferedReader(path)) {
	    	obj = JsonParser.parseReader(br).getAsJsonObject();
	    } catch (IOException ioe) {
	    	throw new RuntimeException(ioe);
	    }
	    
//	    System.out.println("Rupture set: "+obj.get(ID).getAsInt()+"\t"+obj.get(NAME).getAsString());
	    
	    int srcID = obj.get(ID).getAsInt();
	    
	    int[] fltSectionIDs;
	    if (obj.has(SECTIONS)) {
	      fltSectionIDs = GSON.fromJson(obj.get(SECTIONS), int[].class);
	    } else {
	    	fltSectionIDs = new int[]{srcID}; // section ID same as src ID
	    }
//	    for(int sect:fltSectionIDs)
//	    	System.out.println("\t"+sect);
	    
	    if(!srcFltSectsMap.keySet().contains(srcID)) { // no need for redundancies because src IDs have unique rup surface
	    	srcFltSectsMap.put(srcID, fltSectionIDs);
	    }

	}
	
	/**
	 * I found these files by listing for "rupture-set.json" and "cluster-set.json" at various directory depths
	 * @param srcIDsList
	 * @param srcFltSectsList
	 */
	private static void getSrcIDsAndFaultSectionsLists(HashMap<Integer,int[]> srcFltSectsMap, String nshmModelDirPath) {
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/usgs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/Commerce/2-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/Commerce/3-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/OK/Meers/usgs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (North)/1-eq/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (North)/2-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/sscn/recurrence-rate/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/OK/Meers/sscn/cluster-in/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/OK/Meers/sscn/cluster-out/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/crittenden-co/2-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/crittenden-co/3-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/crittenden-co/4-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/meeman-shelby/2-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/meeman-shelby/3-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/TN/Eastern Rift Margin (South)/meeman-shelby/4-eqs/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/sscn/slip-rate/full-rupture/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/sscn/slip-rate/partial-rupture/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-out/reelfoot-extended/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-out/reelfoot-short/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/center/cluster-out/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/east/cluster-out/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/mid-east/cluster-out/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/mid-west/cluster-out/rupture-set.json", srcFltSectsMap);
		parseRuptureSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/west/cluster-out/rupture-set.json", srcFltSectsMap);

		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsaxn-rftl-nmnl/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsaxn-rftl-nmns/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsaxn-rfts-nmnl/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsaxn-rfts-nmns/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsbl-rftl-nmnl/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsbl-rftl-nmns/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsbl-rfts-nmnl/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/sscn/cluster-in/axsbl-rfts-nmns/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/center/cluster-in/all/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/center/cluster-in/center-south/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/east/cluster-in/all/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/east/cluster-in/center-south/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/mid-east/cluster-in/all/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/mid-east/cluster-in/center-south/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/mid-west/cluster-in/all/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/mid-west/cluster-in/center-south/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/west/cluster-in/all/cluster-set.json", srcFltSectsMap);
		parseClusterSetFile(nshmModelDirPath+"stable-crust/fault/MO/New Madrid/usgs/west/cluster-in/center-south/cluster-set.json", srcFltSectsMap);
	}

	
	public static void main(String[] args) {
		
		String nshmModelDirPath = "/Users/field/nshm-haz_data/nshm-conus-6.0.0/";

		getFaultSystemSolutionList(nshmModelDirPath);
		
//	    WC1994_MagLengthRelationship wcMagLength = new WC1994_MagLengthRelationship();
//	    double mag = 6.55;
//	    System.out.println(wcMagLength.getMedianLength(mag));
//	    System.out.println(wcMagLength.getMedianLength(mag,0.0));
//	    System.out.println(wcMagLength.getMedianLength(mag,-90));
//	    System.out.println(wcMagLength.getMedianLength(mag,90));

		
//		testJsonRead("stable-crust/fault/MO/New Madrid/usgs/center/cluster-out/rupture-set.json");
		
		// srcID != sectionID
//		parseRuptureSetFile("stable-crust/fault/TN/Eastern Rift Margin (South)/crittenden-co/3-eqs/rupture-set.json");

		// srcID=sectionID
//		parseRuptureSetFile("stable-crust/fault/TN/Eastern Rift Margin (North)/1-eq/rupture-set.json");


//		parseClusterSetFile("stable-crust/fault/MO/New Madrid/usgs/west/cluster-in/center-south/cluster-set.json");
//		testForPeter();
//		getFaultSystemSolution();
		

		   
//	    // write attributes of something
//	    try {
//	    	File file = new File("junkRightHere");
//	    	DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
//	    	String lineString = "bla bla";
//	    	out.writeChars(lineString);
//	    	out.close();
//	    } catch (IOException e) {
//	    	e.printStackTrace();
//	    }


		
//		File jsonFile = new File(nshmModelDirPath+"stable-crust/fault/CO/Cheraw/features/Cheraw (SSCn).geojson");
//		Feature feature;
//		try {
//			feature = Feature.read(jsonFile);
//			GeoJSONFaultSection sect = GeoJSONFaultSection.fromNSHMP_HazFeature(feature);
//			System.out.println(sect.toFeature().toJSON());
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}


	}

}