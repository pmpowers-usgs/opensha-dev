package scratch.ned.nshm23;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.Color;
import java.awt.geom.Area;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opensha.commons.data.function.HistogramFunction;
import org.opensha.commons.data.region.CaliforniaRegions;
import org.opensha.commons.data.xyz.GriddedGeoDataSet;
import org.opensha.commons.geo.BorderType;
import org.opensha.commons.geo.GriddedRegion;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.geo.LocationVector;
import org.opensha.commons.geo.Region;
import org.opensha.commons.mapping.gmt.GMT_MapGenerator;
import org.opensha.commons.mapping.gmt.elements.GMT_CPT_Files;
import org.opensha.commons.mapping.gmt.gui.GMT_MapGuiBean;
import org.opensha.commons.mapping.gmt.gui.ImageViewerWindow;
import org.opensha.commons.param.impl.CPTParameter;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.commons.util.FileUtils;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemRupSet;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemSolution;
import org.opensha.sha.earthquake.faultSysSolution.RuptureSets;
import org.opensha.sha.earthquake.faultSysSolution.RuptureSets.RupSetConfig;
import org.opensha.sha.earthquake.faultSysSolution.modules.GridSourceProvider;
import org.opensha.sha.earthquake.faultSysSolution.modules.PolygonFaultGridAssociations;
import org.opensha.sha.faultSurface.FaultSection;
import org.opensha.sha.faultSurface.QuadSurface;
import org.opensha.sha.faultSurface.RuptureSurface;
import org.opensha.sha.gui.infoTools.CalcProgressBar;
import org.opensha.sha.magdist.IncrementalMagFreqDist;
import org.opensha.sha.magdist.SummedMagFreqDist;

import com.google.common.base.Preconditions;

import scratch.UCERF3.analysis.GMT_CA_Maps;
import scratch.UCERF3.enumTreeBranches.FaultModels;
import scratch.UCERF3.enumTreeBranches.ScalingRelationships;
import scratch.UCERF3.enumTreeBranches.SlipAlongRuptureModels;
import scratch.UCERF3.enumTreeBranches.SpatialSeisPDF;
import scratch.UCERF3.erf.FaultSystemSolutionERF;
import scratch.UCERF3.erf.ETAS.ETAS_SimAnalysisTools;
import scratch.UCERF3.erf.ETAS.SeisDepthDistribution;
import scratch.UCERF3.logicTree.U3LogicTreeBranch;
import scratch.UCERF3.utils.MatrixIO;
import scratch.UCERF3.utils.RELM_RegionUtils;
import scratch.UCERF3.utils.U3FaultSystemIO;

/**
 * 
 * Questions: 
 * 
 * move getSectionPolygonRegion(*) method to faultSection; confirm that trace is not offset
 * 
 * 	 * TODO - confirm that trace is surface projection and not offset by DDW
 * 
 * move the following to a more general class: ETAS_SimAnalysisTools.writeMemoryUse()
 * 
 * Does rupSet.getMinMagForSection(s) really get the final minMag?
 * 
 * 
 * 
 * @author field
 *
 */
public class GridSourceProvider2023 {
	
	final static boolean D = true;
	
	final static double DEFAULT_MAX_FAULT_NUCL_DIST = 12d;		
	
	String defaultSectAtCubeCacheFilename = "/Users/field/tmp/defaultSectAtCubeCache";
	String defaultSectDistForCubeCacheFilename = "/Users/field/tmp/defaultSectDistForCubeCache";
	String defaultFracCubeUsedBySectCacheFilename = "/Users/field/tmp/defaultFracCubeUsedBySectCache";
	
	double maxFaultNuclDist;
	
	CubedGriddedRegion cgr;
	
	double[] spatialPDF;
	FaultSystemSolution fss;
	FaultSystemRupSet rupSet;
	HistogramFunction depthNuclProbHist;
	GriddedRegion griddedRegion;
	
	IncrementalMagFreqDist totGriddedSeisMFD; // supplied as input

	SummedMagFreqDist totalSubSeisMFD, totalTrulyOffFaultMFD; // both computed

	List<int[]> sectAtCubeList;
	List<float[]> sectDistToCubeList;
	List<float[]> fracCubeUsedBySectList;

	// The following list contains, for each cube, a map of sections and their distance-fraction wts (where
	// the wts represent the fraction of seismicity assinged to the fault section below the min seismo mag).
	ArrayList<HashMap<Integer,Double>> sectDistFractWtMapAtCubeList;
	// this is the total wt for each section summed from sectDistFractWtMapList (divide the wt directly above
	// by this value to get the nucleation fraction for the section in the associated cube) 
	double[] totSectDistFracWtAtCubeArray;

	
	/**
	 * 
	 * @param fss
	 * @param griddedRegion
	 * @param spatialPDF
	 * @param totGriddedSeisMFD
	 */
	public GridSourceProvider2023(FaultSystemSolution fss, CubedGriddedRegion cgr, 
			double[] spatialPDF, IncrementalMagFreqDist totGriddedSeisMFD, HistogramFunction depthNuclProbHist) {

		this(fss, cgr, spatialPDF, totGriddedSeisMFD, depthNuclProbHist, DEFAULT_MAX_FAULT_NUCL_DIST);
	}
	
	
	/**
	 * 
	 * @param fss
	 * @param griddedRegion
	 * @param spatialPDF
	 * @param totGriddedSeisMFD
	 * @param maxDepth
	 * @param numCubeDepths
	 * @param numCubesPerGridEdge
	 */
	public GridSourceProvider2023(FaultSystemSolution fss, CubedGriddedRegion cgr, 
			double[] spatialPDF, IncrementalMagFreqDist totGriddedSeisMFD, HistogramFunction depthNuclProbHist, double maxFaultNuclDist) {
		
		this.fss = fss;
		this.cgr = cgr;
		this.rupSet = fss.getRupSet();
		this.spatialPDF = spatialPDF;
		this.totGriddedSeisMFD = totGriddedSeisMFD;
		this.depthNuclProbHist = depthNuclProbHist;
		this.maxFaultNuclDist = maxFaultNuclDist;
		this.griddedRegion = cgr.getGriddedRegion();
		
		if(griddedRegion.getNodeCount() != spatialPDF.length)
			throw new RuntimeException("griddedRegion and spatialPDF have differe sizes: "+griddedRegion.getNodeCount()+" vs "+spatialPDF.length);
		
		// test that spatialPDF sums to 1.0
		double testSum=0;
		for(double val:spatialPDF) testSum += val;
		if(testSum>1.001 || testSum < 0.999)
			throw new RuntimeException("spatialPDF values must sum to 1.0; sum="+testSum);

		
		testSum = depthNuclProbHist.calcSumOfY_Vals();
		if(testSum>1.0001 || testSum < 0.9999)
			throw new RuntimeException("depthNuclProbHist y-axis values must sum to 1.0; sum=testSum");
		// could also check the exact x-axis discretization of depthNuclProbHist
			
		
		readOrGenerateCacheData();
		
		makeSectDistFractWtMapList();
		
		computeTotalOnAndOffFaultGriddedSeisMFDs();
		
		if(D) System.out.println("Done with constructor");
		
	}


	/**
	 * 
	 * @param faultSection
	 * @param distance
	 * @param accountForDip
	 * @return
	 */
	public static Region getSectionPolygonRegion(FaultSection faultSection, double distance, boolean accountForDip) {
		LocationList trace = faultSection.getFaultTrace();
		checkArgument(trace.size() > 1);
		double dipDir = faultSection.getDipDirection();
		double distPlusDip = distance;
		if(accountForDip)
			distPlusDip += faultSection.getOrigDownDipWidth() * Math.cos(faultSection.getAveDip() * Math.PI / 180d);
		LocationList locList = new LocationList();
		locList.add(trace.get(0));
		LocationVector v = new LocationVector(dipDir, distPlusDip, 0);

		for (int i = 0; i < trace.size(); i++) {
			locList.add(LocationUtils.location(trace.get(i), v));
		}
		locList.add(trace.get(trace.size()-1));
		double reverseDipDir = (dipDir + 180) % 360;
		v = new LocationVector(reverseDipDir, distance, 0);
		for (int i = trace.size()-1; i >= 0; i--) {
			locList.add(LocationUtils.location(trace.get(i), v));
		}
		return new Region(locList, BorderType.MERCATOR_LINEAR);
	}
	
	
	/**
	 * 
	 * @param sectIndex
	 */
	private void getCubeDistancesAndFractionsForFaultSection(int sectIndex, 
			HashMap<Integer,Double> cubeDistMap, 
			HashMap<Integer,Double> cubeFracUsedMap) {

		FaultSection fltSection = rupSet.getFaultSectionData(sectIndex);

		Region fltPolygon = getSectionPolygonRegion(fltSection, maxFaultNuclDist, true);
		
//		System.out.println(fltSection.getName()+"\nsectIndex = "+sectIndex+"\ndip = "+fltSection.getAveDip()
//		+"\ndipDir = "+fltSection.getDipDirection()+
//		"\nupSeisDep = "+fltSection.getOrigAveUpperDepth()+
//		"\nddw = "+fltSection.getOrigDownDipWidth()+"\nTrace:\n");
//		for(Location loc:fltSection.getFaultTrace())
//			System.out.println(loc);
//		System.out.println("\nPolygonRegion:\n");
//		for(Location loc:fltPolygon.getBorder())
//			System.out.println(loc);
		
//		System.out.println("\nGriddedSurface:\n");
//		RuptureSurface sectSurf = fltSection.getFaultSurface(1.0, false, false);
//		for(int i=0;i<sectSurf.getEvenlyDiscretizedNumLocs();i++) {
//			System.out.println(sectSurf.getEvenlyDiscretizedLocation(i));
//		}
		
		QuadSurface sectQuadSurf = new QuadSurface(fltSection,false);

		double subDiscFactor = 4; // how much to subdivide cubes for computing ave distance and fract cube used
		// Discretize polygon at CubeSpacing/subDiscFactor in lat, lon, and depth for computing average depth and faction of cube used
		GriddedRegion griddedPolygonReg = new GriddedRegion(fltPolygon, cgr.getCubeLatLonSpacing()/subDiscFactor, GriddedRegion.ANCHOR_0_0);
//		System.out.println("\nGriddedRegionPoints:\n");

		for(int i=0;i<griddedPolygonReg.getNumLocations();i++) {
			Location loc = griddedPolygonReg.getLocation(i);
			double depthDiscr = cgr.getCubeDepthDiscr() / subDiscFactor;
			for(double depth = depthDiscr/2;depth<cgr.getMaxDepth();depth+=depthDiscr) {
				Location loc2 = new Location(loc.getLatitude(),loc.getLongitude(),depth);
				double dist = LocationUtils.distanceToSurf(loc2, sectQuadSurf);
				int cubeIndex = cgr.getCubeIndexForLocation(loc2);
				if(dist>maxFaultNuclDist || cubeIndex==-1)
					continue;
				if(cubeDistMap.containsKey(cubeIndex)) {
					Double curVal = cubeDistMap.get(cubeIndex);
					cubeDistMap.replace(cubeIndex,curVal+dist);
					curVal = cubeFracUsedMap.get(cubeIndex);
					cubeFracUsedMap.replace(cubeIndex,curVal+1d);
				}
				else {
					cubeDistMap.put(cubeIndex,dist);
					cubeFracUsedMap.put(cubeIndex,1d);
				}
			}
		}
		for(int key:cubeDistMap.keySet()) {
			double num = cubeFracUsedMap.get(key);
			double aveDist = cubeDistMap.get(key)/num;
			cubeDistMap.replace(key, aveDist);
			cubeFracUsedMap.replace(key, num/(subDiscFactor*subDiscFactor*subDiscFactor));
		}
	}


	
	
	private void readOrGenerateCacheData() {
		
		File sectAtCubeCacheFile = new File(defaultSectAtCubeCacheFilename);
		File sectDistForCubeCacheFile = new File(defaultSectDistForCubeCacheFilename);
		File fracCubeUsedBySectCacheFile = new File(defaultFracCubeUsedBySectCacheFilename);
		
		// make cache files if they don't exist
		if (!sectAtCubeCacheFile.exists() || !sectDistForCubeCacheFile.exists() || !fracCubeUsedBySectCacheFile.exists()) { // read from file if it exists
			if(D) ETAS_SimAnalysisTools.writeMemoryUse("Memory before running generateAndWriteCacheDataToFiles()");
			generateAndWriteCacheDataToFiles();
			if(D) ETAS_SimAnalysisTools.writeMemoryUse("Memory after running generateAndWriteCacheDataToFiles()");
			System.gc(); // garbage collection
		}
		
		// now read them
		try {
			if(D) ETAS_SimAnalysisTools.writeMemoryUse("Memory before reading "+sectAtCubeCacheFile);
				sectAtCubeList = MatrixIO.intArraysListFromFile(sectAtCubeCacheFile);
			if(D) ETAS_SimAnalysisTools.writeMemoryUse("Memory before reading "+sectDistForCubeCacheFile);
				sectDistToCubeList = MatrixIO.floatArraysListFromFile(sectDistForCubeCacheFile);
			if(D) ETAS_SimAnalysisTools.writeMemoryUse("Memory before reading "+fracCubeUsedBySectCacheFile);
				fracCubeUsedBySectList = MatrixIO.floatArraysListFromFile(fracCubeUsedBySectCacheFile);
			if(D) ETAS_SimAnalysisTools.writeMemoryUse("Memory after reading isCubeInsideFaultPolygon");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void generateAndWriteCacheDataToFiles() {
		if(D) System.out.println("Starting "+this.getClass().getName()+".generateAndWriteListListDataToFile(); THIS WILL TAKE TIME AND MEMORY!");
		long st = System.currentTimeMillis();
		CalcProgressBar progressBar = null;
		if(D) {
			try {
				progressBar = new CalcProgressBar("Sections to process in generateAndWriteCacheDataToFiles()", "junk");
			} catch (Exception e1) {} // headless			
		}
		ArrayList<ArrayList<Integer>> sectAtCubeListTemp = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Float>> sectDistToCubeListTemp = new ArrayList<ArrayList<Float>>();
		ArrayList<ArrayList<Float>> fracCubeUsedBySectListTemp = new ArrayList<ArrayList<Float>>();

		for(int i=0; i<cgr.getNumCubes();i++) {
			sectAtCubeListTemp.add(new ArrayList<Integer>());
			sectDistToCubeListTemp.add(new ArrayList<Float>());
			fracCubeUsedBySectListTemp.add(new ArrayList<Float>());
		}
		
		if (progressBar != null) progressBar.showProgress(true);
		int numSect = rupSet.getNumSections();
		for(int sectIndex=0;sectIndex<numSect;sectIndex++) {
			if (progressBar != null) progressBar.updateProgress(sectIndex, numSect);
			
			HashMap<Integer,Double> cubeDistMap = new HashMap<Integer,Double>();
			HashMap<Integer,Double> cubeFracUsedMap = new HashMap<Integer,Double>();
			
			getCubeDistancesAndFractionsForFaultSection(sectIndex, cubeDistMap, cubeFracUsedMap);

			if(cubeDistMap != null) {	// null if section is outside the region
				for(int cubeIndex:cubeDistMap.keySet()) {
					sectAtCubeListTemp.get(cubeIndex).add(sectIndex);
					sectDistToCubeListTemp.get(cubeIndex).add(new Float(cubeDistMap.get(cubeIndex)));
					fracCubeUsedBySectListTemp.get(cubeIndex).add(new Float(cubeFracUsedMap.get(cubeIndex)));
				}			
			}
		}
		
		ETAS_SimAnalysisTools.writeMemoryUse("Memory before writing files");
		File sectAtCubeCacheFile = new File(defaultSectAtCubeCacheFilename);
		File sectDistForCubeCacheFile = new File(defaultSectDistForCubeCacheFilename);
		File fracCubeUsedBySectCacheFile = new File(defaultFracCubeUsedBySectCacheFilename);
		try {
			MatrixIO.intListListToFile(sectAtCubeListTemp,sectAtCubeCacheFile);
			MatrixIO.floatListListToFile(sectDistToCubeListTemp, sectDistForCubeCacheFile);
			MatrixIO.floatListListToFile(fracCubeUsedBySectListTemp, fracCubeUsedBySectCacheFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//System.exit(0);
		
		if (progressBar != null) progressBar.showProgress(false);
		
		if(D) System.out.println(this.getClass().getName()+".generateAndWriteListListDataToFile() took "+(System.currentTimeMillis()-st)/60000+ " min");

	}
	

	public void tempDoSection(int sectIndex) {

		HashMap<Integer,Double> cubeDistMap = new HashMap<Integer,Double>();
		HashMap<Integer,Double> cubeFracUsedMap = new HashMap<Integer,Double>();
		
		getCubeDistancesAndFractionsForFaultSection(sectIndex, cubeDistMap, cubeFracUsedMap);
		
		HashMap<Integer,Double> cubeNuclWtMap = new HashMap<Integer,Double>();

		double sum=0;
		for(int key:cubeDistMap.keySet()) {
			sum += ((maxFaultNuclDist-cubeDistMap.get(key))/maxFaultNuclDist) * cubeFracUsedMap.get(key);
		}
//		System.out.println("sum="+sum);
		for(int key:cubeDistMap.keySet()) {
			double wt = ((maxFaultNuclDist-cubeDistMap.get(key))/maxFaultNuclDist) * cubeFracUsedMap.get(key)/sum;
			cubeNuclWtMap.put(key, wt);
		}


//		for(int key:cubeDistMap.keySet()) {
//			Location loc = getCubeLocationForIndex(key);
//			System.out.println(loc.getLatitude()+"\t"+loc.getLongitude()+"\t"+loc.getDepth()+"\t"+
//			cubeDistMap.get(key).floatValue()+"\t"+cubeFracUsedMap.get(key).floatValue()+"\t"+cubeNuclWtMap.get(key).floatValue());
//		}
	}

	private double getDistWt(double dist) {
		return (maxFaultNuclDist-dist)/maxFaultNuclDist;
	}
	
	/**
	 * This list contains, for each cube, a map of the sections therein and their distance-fraction wts
	 */
	private void makeSectDistFractWtMapList() {
		
		sectDistFractWtMapAtCubeList = new ArrayList<HashMap<Integer,Double>>();
		totSectDistFracWtAtCubeArray = new double[rupSet.getNumSections()];
		
		for(int c=0;c<cgr.getNumCubes();c++) {
			HashMap<Integer,Double> sectWtMap = new HashMap<Integer,Double>();
			int numSect = sectAtCubeList.get(c).length;
			for(int i=0;i<numSect;i++) {
				int sectIndex = sectAtCubeList.get(c)[i];
				float dist = sectDistToCubeList.get(c)[i];
				float frac = fracCubeUsedBySectList.get(c)[i];
				double wt = getDistWt(dist)*frac/numSect;  // divided equally among sections
				sectWtMap.put(sectIndex, wt);
				totSectDistFracWtAtCubeArray[sectIndex] += wt;
			}
			sectDistFractWtMapAtCubeList.add(sectWtMap);
		}
	}
	
	

	
	
	/**
	 * The computes how many different sections nucleate in each cube and then creates a
	 * histogram (how many have 0, 1, 2, etc sections in the cube)
	 */
	public void computeHistogramOfNumSectionsInCubes() {
		int[] numSectAtCubeList = new int[cgr.getNumCubes()];
		HistogramFunction numCubesWithNumSectHist = new HistogramFunction(0.0, 21,1.0);

		for(int c=0; c<cgr.getNumCubes(); c++) {
			numSectAtCubeList[c] = sectAtCubeList.get(c).length;
			numCubesWithNumSectHist.add(numSectAtCubeList[c], 1.0);
			if(numSectAtCubeList[c]==12) {
				System.out.println("\nCube "+c+ " has 12 sections; "+cgr.getCubeLocationForIndex(c));
				for(int i=0;i<sectAtCubeList.get(c).length;i++) {
					int s = sectAtCubeList.get(c)[i];
					float dist = sectDistToCubeList.get(c)[i];
					float frac = fracCubeUsedBySectList.get(c)[i];
					float wt = (float)getDistWt(dist)*frac;
					System.out.println(s+"\t"+dist+"\t"+frac+"\t"+wt+"\t"+rupSet.getFaultSectionData(s).getName());
				}
			}
		}
		System.out.println(numCubesWithNumSectHist);	
	}
	
	/**
	 * this creates a blank (zero y-axis values) MFD with the same discretization as the constructor supplied totGriddedSeisMFD.
	 * @return
	 */
	private SummedMagFreqDist getBlankMFD() {
		return new SummedMagFreqDist(totGriddedSeisMFD.getMinX(), totGriddedSeisMFD.size(),totGriddedSeisMFD.getDelta());
	}
	
	
	private void computeTotalOnAndOffFaultGriddedSeisMFDs() {
		
		totalSubSeisMFD = getBlankMFD();
		totalTrulyOffFaultMFD = getBlankMFD();
		
		for(int c=0;c<cgr.getNumCubes();c++) {
			SummedMagFreqDist mfd = getSubSeismoMFD_ForCube(c);
			if(mfd != null)
				totalSubSeisMFD.addIncrementalMagFreqDist(mfd);
		}
		
		for(int i=0;i<totGriddedSeisMFD.size();i++) {
			totalTrulyOffFaultMFD.add(i, totGriddedSeisMFD.getY(i) - totalSubSeisMFD.getY(i));
		}
		if(D) {
			System.out.println("totGriddedSeisMFD:\n"+totGriddedSeisMFD);
			System.out.println("totGriddedSeisMFD Cumulative::\n"+totGriddedSeisMFD.getCumRateDistWithOffset());
			System.out.println("totSubSeisMFD:\n"+totalSubSeisMFD);
			System.out.println("totalTrulyOffFaultMFD:\n"+totalTrulyOffFaultMFD);
		}
	}
	
	
	
	public SummedMagFreqDist getSubSeismoMFD_ForCube(int cubeIndex) {
		HashMap<Integer,Double> sectWtMap = sectDistFractWtMapAtCubeList.get(cubeIndex);
		if(sectWtMap.size()==0) // no sections nucleate here
			return null;
		SummedMagFreqDist subSeisMFD = getBlankMFD();
		int gridIndex = cgr.getRegionIndexForCubeIndex(cubeIndex);
		int depIndex = cgr.getDepthIndexForCubeIndex(cubeIndex);
		for(int s:sectWtMap.keySet()) {
			double wt = sectWtMap.get(s)*spatialPDF[gridIndex]*depthNuclProbHist.getY(depIndex)/(cgr.getNumCubesPerGridEdge()*cgr.getNumCubesPerGridEdge());
			double minMag = rupSet.getMinMagForSection(s);
			double minMagIndex = totGriddedSeisMFD.getClosestXIndex(minMag);
			for(int i=0; i<minMagIndex;i++)
				subSeisMFD.add(i, wt*totGriddedSeisMFD.getY(i));
		}
		return subSeisMFD;
	}
	
	
	
	public SummedMagFreqDist getTrulyOffFaultMFD_ForCube(int cubeIndex) {
		
		double scaleFactor = totGriddedSeisMFD.getY(0)/totalTrulyOffFaultMFD.getY(0);
		
		HashMap<Integer,Double> sectWtMap = sectDistFractWtMapAtCubeList.get(cubeIndex);
		double wtSum =0;
		for(int s:sectWtMap.keySet()) {
			wtSum+=sectWtMap.get(s);
		}
		SummedMagFreqDist trulyOffMFD = getBlankMFD();
		int gridIndex = cgr.getRegionIndexForCubeIndex(cubeIndex);
		int depIndex = cgr.getDepthIndexForCubeIndex(cubeIndex);
		double wt = (1d-wtSum)*scaleFactor*spatialPDF[gridIndex]*depthNuclProbHist.getY(depIndex)/(cgr.getNumCubesPerGridEdge()*cgr.getNumCubesPerGridEdge());
		
		for(int i=0; i<totalTrulyOffFaultMFD.size();i++)
			trulyOffMFD.add(i, wt*totalTrulyOffFaultMFD.getY(i));

		return trulyOffMFD;
	}
	
	public SummedMagFreqDist getGriddedSeisMFD_ForCube(int cubeIndex) {
		SummedMagFreqDist cubeMFD = getBlankMFD();
		SummedMagFreqDist mfd = getSubSeismoMFD_ForCube(cubeIndex);
		if(mfd != null)
			cubeMFD.addIncrementalMagFreqDist(mfd);
		mfd = getTrulyOffFaultMFD_ForCube(cubeIndex);
		if(mfd != null)
			cubeMFD.addIncrementalMagFreqDist(mfd);
		return cubeMFD;
	}

	
	
	public SummedMagFreqDist getSubSeismoMFD_ForGridCell(int gridIndex) {
		SummedMagFreqDist subSeisMFD = getBlankMFD();
		for(int c:cgr.getCubeIndicesForGridCell(gridIndex)) {
			SummedMagFreqDist mfd = getSubSeismoMFD_ForCube(c);
			if(mfd != null)
				subSeisMFD.addIncrementalMagFreqDist(mfd);
		}
		return subSeisMFD;
	}
	
	
	public SummedMagFreqDist getTrulyOffFaultMFD_ForCell(int gridIndex) {
		SummedMagFreqDist subSeisMFD = getBlankMFD();
		for(int c:cgr.getCubeIndicesForGridCell(gridIndex)) {
			subSeisMFD.addIncrementalMagFreqDist(getTrulyOffFaultMFD_ForCube(c));
		}
		return subSeisMFD;
	}


	public SummedMagFreqDist getGriddedSeisMFD_ForCell(int gridIndex) {
		SummedMagFreqDist gridSeisMFD = getBlankMFD();
		gridSeisMFD.addIncrementalMagFreqDist(getSubSeismoMFD_ForGridCell(gridIndex));
		gridSeisMFD.addIncrementalMagFreqDist(getTrulyOffFaultMFD_ForCell(gridIndex));
		return gridSeisMFD;
	}
	
	
	private void testTotalGriddedSeisMFD() {
		SummedMagFreqDist testMFD = getBlankMFD();

		for(int i=0;i<cgr.getGriddedRegion().getNumLocations();i++) {
			testMFD.addIncrementalMagFreqDist(getGriddedSeisMFD_ForCell(i));
		}
		System.out.println("testTotalGriddedSeisMFD():");
		for(int i=0;i<totGriddedSeisMFD.size();i++) {
			System.out.println(totGriddedSeisMFD.getX(i)+"\t"+totGriddedSeisMFD.getY(i)+"\t"+testMFD.getY(i)+"\t"+(float)(testMFD.getY(i)/totGriddedSeisMFD.getY(i)));
		}
		
	}

	private void testTotalTrulyOffFaultGriddedSeisMFD() {
		
		SummedMagFreqDist testMFD = getBlankMFD();
		
		for(int c=0;c<cgr.getNumCubes();c++) {
			SummedMagFreqDist mfd = getTrulyOffFaultMFD_ForCube(c);
			testMFD.addIncrementalMagFreqDist(mfd);
		}
		
		System.out.println("testTotalTrulyOffFaultGriddedSeisMFD():");
		for(int i=0;i<totalTrulyOffFaultMFD.size();i++) {
			System.out.println(totalTrulyOffFaultMFD.getX(i)+"\t"+totalTrulyOffFaultMFD.getY(i)+"\t"+testMFD.getY(i)+"\t"+(float)(testMFD.getY(i)/totalTrulyOffFaultMFD.getY(i)));
		}
	}

	
	/**
	 * This test fails at M=5.05 because of a low min supra mag for Quien Sabe (5.07)
	 */
	private void testTotalMgt4_RatesInCells() {
		
		double[] ratioArray = new double[spatialPDF.length];
		double totRate = totGriddedSeisMFD.getY(4.05);
		double ave=0, min=Double.MAX_VALUE, max=-Double.MAX_VALUE;
		
		for(int i=0;i<spatialPDF.length;i++) {
			double ratio = getGriddedSeisMFD_ForCell(i).getY(4.05)/(totRate*spatialPDF[i]);
			ratioArray[i]=ratio;
			ave+=ratio;
			if(min>ratio) min=ratio;
			if(max<ratio) max=ratio;
		}
		ave /= spatialPDF.length;
		
		System.out.println("testTotalMgt5_RatesInCells(): \nave="+(float)ave+"\nmin="+
		(float)min+"\nmax="+(float)max+"\n");

//		for(int i=0;i<spatialPDF.length;i++) {
//			Location loc = this.griddedRegion.getLocation(i);
//			System.out.println(loc.getLongitude()+"\t"+loc.getLatitude()+"\t"+ratioArray[i]);
//		}
	}
	
	
	/**
	 * This tests that sectDistFractWtMapAtCubeList and  totSectDistFracWtAtCubeArray are correct
	 * (they are for all fault sections inside the RELM region)
	 */
	private void testSectDistFractWtMapList() {
		
		double[] testArray = new double[rupSet.getNumSections()];
		
		for(int c=0;c<cgr.getNumCubes();c++) {
			HashMap<Integer,Double> sectWtMap = sectDistFractWtMapAtCubeList.get(c);
			for(int sectIndex: sectWtMap.keySet()) {
				testArray[sectIndex] += sectWtMap.get(sectIndex)/totSectDistFracWtAtCubeArray[sectIndex];
			}
		}
		
		System.out.println("testSectDistFractWtMapList():");
		
		double ave=0, min=Double.MAX_VALUE, max=-Double.MAX_VALUE;
		for(int i=0;i<testArray.length;i++) {
			ave+=testArray[i];
			if(min>testArray[i]) min=testArray[i];
			if(max<testArray[i]) max=testArray[i];
			if(testArray[i]<0.99)
				System.out.println(testArray[i]+" for "+rupSet.getFaultSectionData(i).getName());
		}
		
		ave /= testArray.length;
		
		System.out.println("nave="+(float)ave+"\nmin="+(float)min+"\nmax="+(float)max+"\n");
		
	}
	
	/**
	 * This plots the event rates above the specified magnitude for cubes at the given depth
	 * @param depth
	 * @param dirName
	 * @return
	 */
	public String plotRateAtDepthMap(double depth, double mag, String dirName) {
		
		GMT_MapGenerator mapGen = GMT_CA_Maps.getDefaultGMT_MapGenerator();
		
		CPTParameter cptParam = (CPTParameter )mapGen.getAdjustableParamsList().getParameter(GMT_MapGenerator.CPT_PARAM_NAME);
		cptParam.setValue(GMT_CPT_Files.MAX_SPECTRUM.getFileName());
		cptParam.getValue().setBelowMinColor(Color.WHITE);
		
		GriddedRegion gridRegForCubes = cgr.getGridRegForCubes();
		mapGen.setParameter(GMT_MapGenerator.MIN_LAT_PARAM_NAME,gridRegForCubes.getMinGridLat());
		mapGen.setParameter(GMT_MapGenerator.MAX_LAT_PARAM_NAME,gridRegForCubes.getMaxGridLat());
		mapGen.setParameter(GMT_MapGenerator.MIN_LON_PARAM_NAME,gridRegForCubes.getMinGridLon());
		mapGen.setParameter(GMT_MapGenerator.MAX_LON_PARAM_NAME,gridRegForCubes.getMaxGridLon());
		mapGen.setParameter(GMT_MapGenerator.GRID_SPACING_PARAM_NAME, gridRegForCubes.getLatSpacing());	// assume lat and lon spacing are same

		GriddedGeoDataSet xyzDataSet = new GriddedGeoDataSet(gridRegForCubes, true);
		int depthIndex = cgr.getCubeDepthIndex(depth);
		int numCubesAtDepth = xyzDataSet.size();
		CalcProgressBar progressBar = new CalcProgressBar("Looping over all points", "junk");
		progressBar.showProgress(true);
		
		int magIndex = totGriddedSeisMFD.getClosestXIndex(mag);

		for(int i=0; i<numCubesAtDepth;i++) {
			progressBar.updateProgress(i, numCubesAtDepth);
			int cubeIndex = cgr.getCubeIndexForRegAndDepIndices(i, depthIndex);
			SummedMagFreqDist mfd = getGriddedSeisMFD_ForCube(cubeIndex);
			double rate = 0.0;
			if(mfd != null)
				rate = mfd.getCumRate(magIndex);
			if(rate == 0.0)
				rate = 1e-16;
			xyzDataSet.set(i, rate);
//			Location loc = xyzDataSet.getLocation(i);
//			System.out.println(loc.getLongitude()+"\t"+loc.getLatitude()+"\t"+xyzDataSet.get(i));
		}
		progressBar.showProgress(false);
		
		mapGen.setParameter(GMT_MapGenerator.LOG_PLOT_NAME,true);
//		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MODE_NAME,GMT_MapGenerator.COLOR_SCALE_MODE_FROMDATA);
		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MODE_NAME,GMT_MapGenerator.COLOR_SCALE_MODE_MANUALLY);
//		double maxZ = Math.ceil(Math.log10(xyzDataSet.getMaxZ()))+0.5;
//		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MIN_PARAM_NAME,maxZ-5);
//		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MAX_PARAM_NAME,maxZ);
		
		if(mag<5) {
			mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MIN_PARAM_NAME,-5d);
			mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MAX_PARAM_NAME,-1d);			
		}
		else {
			mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MIN_PARAM_NAME,-11d);
			mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MAX_PARAM_NAME,-6d);
		}

		String metadata = "Map from calling plotRateAtDepthMap(*) method";
		
		try {
				String url = mapGen.makeMapUsingServlet(xyzDataSet, "M≥"+mag+" Rates at "+depth+" km depth", metadata, dirName);
				metadata += GMT_MapGuiBean.getClickHereHTML(mapGen.getGMTFilesWebAddress());
				ImageViewerWindow imgView = new ImageViewerWindow(url,metadata, true);		
				
				File downloadDir = new File(dirName);
				if (!downloadDir.exists())
					downloadDir.mkdir();
				File zipFile = new File(downloadDir, "allFiles.zip");
				// construct zip URL
				String zipURL = url.substring(0, url.lastIndexOf('/')+1)+"allFiles.zip";
				FileUtils.downloadURL(zipURL, zipFile);
				FileUtils.unzipFile(zipFile, downloadDir);

//			System.out.println("GMT Plot Filename: "+name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "For rates at depth above mag map: "+mapGen.getGMTFilesWebAddress()+" (deleted at midnight)";
	}

	
	/**
	 * This plots the event rates above the specified magnitude for cubes at the given depth
	 * @param depth
	 * @param dirName
	 * @return
	 */
	public String plotRateAtDepthMap2(double depth, double mag, String dirName) {
		
		GMT_MapGenerator mapGen = GMT_CA_Maps.getDefaultGMT_MapGenerator();
		
		CPTParameter cptParam = (CPTParameter )mapGen.getAdjustableParamsList().getParameter(GMT_MapGenerator.CPT_PARAM_NAME);
		cptParam.setValue(GMT_CPT_Files.GMT_POLAR.getFileName());
		cptParam.getValue().setBelowMinColor(Color.WHITE);
		
		GriddedRegion gridRegForCubes = cgr.getGridRegForCubes();
		mapGen.setParameter(GMT_MapGenerator.MIN_LAT_PARAM_NAME,gridRegForCubes.getMinGridLat());
		mapGen.setParameter(GMT_MapGenerator.MAX_LAT_PARAM_NAME,gridRegForCubes.getMaxGridLat());
		mapGen.setParameter(GMT_MapGenerator.MIN_LON_PARAM_NAME,gridRegForCubes.getMinGridLon());
		mapGen.setParameter(GMT_MapGenerator.MAX_LON_PARAM_NAME,gridRegForCubes.getMaxGridLon());
		mapGen.setParameter(GMT_MapGenerator.GRID_SPACING_PARAM_NAME, gridRegForCubes.getLatSpacing());	// assume lat and lon spacing are same

		GriddedGeoDataSet xyzDataSet = new GriddedGeoDataSet(gridRegForCubes, true);
		int depthIndex = cgr.getCubeDepthIndex(depth);
		int numCubesAtDepth = xyzDataSet.size();
		CalcProgressBar progressBar = new CalcProgressBar("Looping over all points", "junk");
		progressBar.showProgress(true);
		
		int magIndex = totGriddedSeisMFD.getClosestXIndex(mag);

		double maxRate =0;
		for(int i=0; i<numCubesAtDepth;i++) {
			progressBar.updateProgress(i, numCubesAtDepth);
			int cubeIndex = cgr.getCubeIndexForRegAndDepIndices(i, depthIndex);
			SummedMagFreqDist mfd = getGriddedSeisMFD_ForCube(cubeIndex);
			double rate = 0.0;
			if(mfd != null)
				rate = mfd.getCumRate(magIndex);
			xyzDataSet.set(i, rate);
			if(maxRate<rate) maxRate=rate;
		}
		for(int i=0; i<numCubesAtDepth;i++) {
			double oldVal = xyzDataSet.get(i);
			xyzDataSet.set(i, oldVal/maxRate);
		}
		
		progressBar.showProgress(false);
		
		mapGen.setParameter(GMT_MapGenerator.LOG_PLOT_NAME,false);
//		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MODE_NAME,GMT_MapGenerator.COLOR_SCALE_MODE_FROMDATA);
		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MODE_NAME,GMT_MapGenerator.COLOR_SCALE_MODE_MANUALLY);
//		double maxZ = Math.ceil(Math.log10(xyzDataSet.getMaxZ()))+0.5;
//		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MIN_PARAM_NAME,maxZ-5);
//		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MAX_PARAM_NAME,maxZ);
		
		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MIN_PARAM_NAME,-1.0);
		mapGen.setParameter(GMT_MapGenerator.COLOR_SCALE_MAX_PARAM_NAME,1.0);			

		String metadata = "Map from calling plotRateAtDepthMap(*) method";
		
		try {
				String url = mapGen.makeMapUsingServlet(xyzDataSet, "M≥"+mag+" Rates at "+depth+" km depth", metadata, dirName);
				metadata += GMT_MapGuiBean.getClickHereHTML(mapGen.getGMTFilesWebAddress());
				ImageViewerWindow imgView = new ImageViewerWindow(url,metadata, true);		
				
				File downloadDir = new File(dirName);
				if (!downloadDir.exists())
					downloadDir.mkdir();
				File zipFile = new File(downloadDir, "allFiles.zip");
				// construct zip URL
				String zipURL = url.substring(0, url.lastIndexOf('/')+1)+"allFiles.zip";
				FileUtils.downloadURL(zipURL, zipFile);
				FileUtils.unzipFile(zipFile, downloadDir);

//			System.out.println("GMT Plot Filename: "+name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "For rates at depth above mag map: "+mapGen.getGMTFilesWebAddress()+" (deleted at midnight)";
	}

	
	public static void main(String[] args) {
		
		String fileName="/Users/field/workspace/OpenSHA/dev/scratch/UCERF3/data/scratch/InversionSolutions/2013_05_10-ucerf3p3-production-10runs_COMPOUND_SOL_FM3_1_SpatSeisU3_MEAN_BRANCH_AVG_SOL.zip";
		FaultSystemSolution fss;
		try {
			fss = U3FaultSystemIO.loadSol(new File(fileName));
		} catch (Exception e) {
			throw ExceptionUtils.asRuntimeException(e);
		}

		CaliforniaRegions.RELM_TESTING_GRIDDED griddedRegion = RELM_RegionUtils.getGriddedRegionInstance();

		SeisDepthDistribution seisDepthDistribution = new SeisDepthDistribution();
		double delta=2;
		HistogramFunction binnedDepthDistFunc = new HistogramFunction(1d, 12,delta);
		for(int i=0;i<binnedDepthDistFunc.size();i++) {
			double prob = seisDepthDistribution.getProbBetweenDepths(binnedDepthDistFunc.getX(i)-delta/2d,binnedDepthDistFunc.getX(i)+delta/2d);
			binnedDepthDistFunc.set(i,prob);
		}
		System.out.println("Total Depth Prob Sum: "+binnedDepthDistFunc.calcSumOfY_Vals());

		
		double[] spatialPDF = SpatialSeisPDF.UCERF3.getPDF();
		// this sums to 0.9994463999998295; correct it to 1.0
		double sum=0;
		for(double val:spatialPDF) sum+=val;
		for(int i=0;i<spatialPDF.length;i++)
			spatialPDF[i] = spatialPDF[i]/sum;
		
		// make spatialPDF constant for testing
		for(int i=0;i<spatialPDF.length;i++)
			spatialPDF[i] = 1.0/spatialPDF.length;

		
		// Get target total gridded seis MFD
		GridSourceProvider gridSrcProvider = fss.getGridSourceProvider();
		IncrementalMagFreqDist tempMFD = gridSrcProvider.getNodeMFD(0);
		SummedMagFreqDist totGriddedSeisMFD = new SummedMagFreqDist(tempMFD.getMinX(), tempMFD.size(),tempMFD.getDelta());
		for(int i=0;i<gridSrcProvider.size();i++)
			totGriddedSeisMFD.addIncrementalMagFreqDist(gridSrcProvider.getNodeMFD(i));		
//		System.out.println(totGriddedSeisMFD);
//		System.exit(0);
		
		CubedGriddedRegion cgr = new CubedGriddedRegion(griddedRegion);

		GridSourceProvider2023 gridProvider = new GridSourceProvider2023(fss, cgr, spatialPDF, totGriddedSeisMFD, binnedDepthDistFunc);
				
//		gridProvider.testGetCubeIndicesForGridCell();
		
		long startTime = System.currentTimeMillis();

//		gridProvider.plotRateAtDepthMap(7d,2.55,"RatesAboveM2pt5_AtDepth7km");
		gridProvider.plotRateAtDepthMap2(7d,7.35,"RatesAboveM7pt3_AtDepth7km");
		
//		gridProvider.testTotalGriddedSeisMFD();
//		gridProvider.testTotalTrulyOffFaultGriddedSeisMFD();
//		gridProvider.testTotalMgt4_RatesInCells();
//		gridProvider.testSectDistFractWtMapList();
		
//		gridProvider.computeHistogramOfNumSectionsInCubes();
		
		long runtime = System.currentTimeMillis()-startTime;
		double runtimeMin = runtime/60000d;
		System.out.println("Runtime = "+(float)runtimeMin+" min");
		
//		CalcProgressBar progressBar = null;
//		try {
//			progressBar = new CalcProgressBar("Sections to process", "junk");
//			progressBar.showProgress(true);
//		} catch (Exception e1) {} // headless
//
//		long startTime = System.currentTimeMillis();
//		int numSect = fss.getRupSet().getNumSections();
//		for(int s=0;s<numSect;s++) {
//			if (progressBar != null) progressBar.updateProgress(s, numSect);
//			gridProvider.tempDoSection(s);
//		}
//		long runtime = System.currentTimeMillis()-startTime;
//		double runtimeMin = runtime/60000d;
//		System.out.println("Runtime = "+(float)runtimeMin+" min");
//		
/*
		int sectIndex=0;
		for(int s=0;s<fss.getRupSet().getNumSections();s++) {
//			if(fss.getRupSet().getFaultSectionData(s).getOrigAveUpperDepth() > 3d) {
			if(fss.getRupSet().getFaultSectionData(s).getAveDip() > 89d) {
				sectIndex = s;
				break;
			}
		}

		gridProvider.doSection(sectIndex);

*/

	}

}
