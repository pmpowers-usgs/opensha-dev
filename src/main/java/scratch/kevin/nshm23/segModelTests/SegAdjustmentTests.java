package scratch.kevin.nshm23.segModelTests;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.StatUtils;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.data.function.EvenlyDiscretizedFunc;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.geo.json.Feature;
import org.opensha.commons.gui.plot.HeadlessGraphPanel;
import org.opensha.commons.gui.plot.PlotCurveCharacterstics;
import org.opensha.commons.gui.plot.PlotLineType;
import org.opensha.commons.gui.plot.PlotSpec;
import org.opensha.commons.gui.plot.PlotSymbol;
import org.opensha.commons.gui.plot.PlotUtils;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemRupSet;
import org.opensha.sha.earthquake.faultSysSolution.modules.SectAreas;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.ClusterRupture;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.FaultSubsectionCluster;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.Jump;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.prob.Shaw07JumpDistProb;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.util.SectionDistanceAzimuthCalculator;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.targetMFDs.SupraSeisBValInversionTargetMFDs;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.targetMFDs.estimators.SectNucleationMFD_Estimator;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.targetMFDs.estimators.SegmentationImpliedSectNuclMFD_Estimator;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.targetMFDs.estimators.ThresholdAveragingSectNuclMFD_Estimator;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.targetMFDs.estimators.SegmentationImpliedSectNuclMFD_Estimator.MultiBinDistributionMethod;
import org.opensha.sha.faultSurface.FaultSection;
import org.opensha.sha.faultSurface.GeoJSONFaultSection;
import org.opensha.sha.magdist.IncrementalMagFreqDist;
import org.opensha.sha.magdist.SummedMagFreqDist;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.Doubles;

import scratch.UCERF3.enumTreeBranches.ScalingRelationships;

public class SegAdjustmentTests {

	public static void main(String[] args) throws IOException {
		File outputDir = new File("/tmp/seg_tests");
		Preconditions.checkState(outputDir.exists() || outputDir.mkdir());
		
		// create fake rupture set
		
		double supraB = 0.5;
		Shaw07JumpDistProb segModel = Shaw07JumpDistProb.forHorzOffset(1d, 3, 2);
		
		List<FaultSection> sects = new ArrayList<>();
		double len1 = 50d;
		double len2 = 50d;
		double len3 = 50d;
		
		double az1 = 0.5*Math.PI;
		double az2 = 0.5*Math.PI;
		double az3 = 0.25*Math.PI;
		
//		double dist2 = 10d;
//		double dist3 = 5d;
		double dist2 = segModel.calcJumpDistance(0.1);
		double dist3 = segModel.calcJumpDistance(0.5);
		
		Location l10 = new Location(34, -118);
		Location l11 = LocationUtils.location(l10, az1, len1);
		
		Location l20 = LocationUtils.location(l11, az2, dist2);
		Location l21 = LocationUtils.location(l20, az2, len2);
		
		Location l30 = LocationUtils.location(l11, az3, dist3);
		Location l31 = LocationUtils.location(l30, az3, len3);
		
//		Location l3 = new Location
		String sect1JSON =
				"    {\n"+
				"      \"type\": \"Feature\",\n"+
				"      \"id\": 0,\n"+
				"      \"properties\": {\n"+
				"        \"FaultID\": 0,\n"+
				"        \"FaultName\": \"Test Fault 1\",\n"+
				"        \"DipDeg\": 90.0,\n"+
				"        \"Rake\": 0.0,\n"+
				"        \"LowDepth\": 10.0,\n"+
				"        \"UpDepth\": 0.0,\n"+
				"        \"SlipRate\": 1\n"+
				"      },\n"+
				"      \"geometry\": {\n"+
				"        \"type\": \"LineString\",\n"+
				"        \"coordinates\": [\n"+
				"          [\n"+
				"            "+l10.getLongitude()+",\n"+
				"            "+l10.getLatitude()+"\n"+
				"          ],\n"+
				"          [\n"+
				"            "+l11.getLongitude()+",\n"+
				"            "+l11.getLatitude()+"\n"+
				"          ]\n"+
				"        ]\n"+
				"      }\n"+
				"    }";
//		System.out.println(sect1JSON);
		sects.add(GeoJSONFaultSection.fromFeature(Feature.fromJSON(sect1JSON)));
		String sect2JSON =
				"    {\n"+
				"      \"type\": \"Feature\",\n"+
				"      \"id\": 1,\n"+
				"      \"properties\": {\n"+
				"        \"FaultID\": 0,\n"+
				"        \"FaultName\": \"Test Fault 2\",\n"+
				"        \"DipDeg\": 90.0,\n"+
				"        \"Rake\": 0.0,\n"+
				"        \"LowDepth\": 10.0,\n"+
				"        \"UpDepth\": 0.0,\n"+
				"        \"SlipRate\": 1\n"+
				"      },\n"+
				"      \"geometry\": {\n"+
				"        \"type\": \"LineString\",\n"+
				"        \"coordinates\": [\n"+
				"          [\n"+
				"            "+l20.getLongitude()+",\n"+
				"            "+l20.getLatitude()+"\n"+
				"          ],\n"+
				"          [\n"+
				"            "+l21.getLongitude()+",\n"+
				"            "+l21.getLatitude()+"\n"+
				"          ]\n"+
				"        ]\n"+
				"      }\n"+
				"    }";
		sects.add(GeoJSONFaultSection.fromFeature(Feature.fromJSON(sect2JSON)));
		String sect3JSON =
				"    {\n"+
				"      \"type\": \"Feature\",\n"+
				"      \"id\": 2,\n"+
				"      \"properties\": {\n"+
				"        \"FaultID\": 0,\n"+
				"        \"FaultName\": \"Test Fault 3\",\n"+
				"        \"DipDeg\": 90.0,\n"+
				"        \"Rake\": 0.0,\n"+
				"        \"LowDepth\": 10.0,\n"+
				"        \"UpDepth\": 0.0,\n"+
				"        \"SlipRate\": 1\n"+
				"      },\n"+
				"      \"geometry\": {\n"+
				"        \"type\": \"LineString\",\n"+
				"        \"coordinates\": [\n"+
				"          [\n"+
				"            "+l30.getLongitude()+",\n"+
				"            "+l30.getLatitude()+"\n"+
				"          ],\n"+
				"          [\n"+
				"            "+l31.getLongitude()+",\n"+
				"            "+l31.getLatitude()+"\n"+
				"          ]\n"+
				"        ]\n"+
				"      }\n"+
				"    }";
		sects.add(GeoJSONFaultSection.fromFeature(Feature.fromJSON(sect3JSON)));
		
		double minSupraMag = 6.05d;
		double maxSingleFaultMag = 6.95d;
//		double[] maxMultiFaultMags = { 7.95d, 7.25d };
//		double[] maxMultiFaultMags = { 7.95d, 7.95d };
		double[] maxMultiFaultMags = { 7.45d, 7.45d };
		
		double maxMag = StatUtils.max(maxMultiFaultMags);
		EvenlyDiscretizedFunc refMFD = SupraSeisBValInversionTargetMFDs.buildRefXValues(maxMag);
		
		int maxSingleIndex = refMFD.getClosestXIndex(maxSingleFaultMag);
		int maxMagIndex = refMFD.getClosestXIndex(maxMag);
		
		ScalingRelationships scale = ScalingRelationships.HANKS_BAKUN_08;
		
		List<ClusterRupture> rups = new ArrayList<>();
		// add single fault ruptures
		List<Double> rupMags = new ArrayList<>();
		List<Double> rupAreas = new ArrayList<>();
		List<FaultSubsectionCluster> sectClusters = new ArrayList<>();
		for (FaultSection sect : sects)
			sectClusters.add(new FaultSubsectionCluster(List.of(sect)));
		for (FaultSubsectionCluster cluster : sectClusters) {
			for (int i=refMFD.getClosestXIndex(minSupraMag); i<=refMFD.getClosestXIndex(maxSingleFaultMag); i++) {
				double mag = refMFD.getX(i);
				double area = scale.getArea(mag, cluster.startSect.getOrigDownDipWidth()*1e3);
				ClusterRupture rup = new ClusterRupture(cluster);
				rups.add(rup);
				rupMags.add(mag);
				rupAreas.add(area);
			}
		}
		FaultSystemRupSet singleFaultRupSet = FaultSystemRupSet.builderForClusterRups(sects, new ArrayList<>(rups))
				.rupMags(Doubles.toArray(rupMags))
				.rupAreas(Doubles.toArray(rupAreas))
				.build();
		SectionDistanceAzimuthCalculator distAzCalc = new SectionDistanceAzimuthCalculator(sects);
		// now add multi-fault ruptures, all involving sect1
		ClusterRupture baseRup = new ClusterRupture(sectClusters.get(0));
		for (int s=0; s<sectClusters.size()-1; s++) {
			FaultSubsectionCluster destCluster = sectClusters.get(s+1);
			double destMaxMag = maxMultiFaultMags[s];
			double dist = distAzCalc.getDistance(sects.get(0), destCluster.startSect);
			Jump jump = new Jump(sects.get(0), sectClusters.get(0), destCluster.startSect, destCluster, dist);
			System.out.println("Distance from 0 to "+(s+1)+": "+dist);
			System.out.println("\t"+segModel.getName()+": "+segModel.calcJumpProbability(dist));
			for (int i=refMFD.getClosestXIndex(maxSingleFaultMag)+1; i<=refMFD.getClosestXIndex(destMaxMag); i++) {
				double mag = refMFD.getX(i);
				double area = scale.getArea(mag, destCluster.startSect.getOrigDownDipWidth()*1e3);
				rups.add(baseRup.take(jump));
				rupMags.add(mag);
				rupAreas.add(area);
			}
		}
		
		for (int r=0; r<rups.size(); r++)
			System.out.println("Rupture "+r+", M="+rupMags.get(r).floatValue()+": "+rups.get(r));
		
		FaultSystemRupSet rupSet = FaultSystemRupSet.builderForClusterRups(sects, rups)
				.rupMags(Doubles.toArray(rupMags))
				.rupAreas(Doubles.toArray(rupAreas))
				.build();
		// fake sect areas to make nucleation/participation calcs work
		// make sections have typical subsection area, half as long as wide
		double[] sectAreas = new double[sects.size()];
		for (int s=0; s<sectAreas.length; s++) {
			FaultSection sect = sects.get(s);
			double width = sect.getOrigDownDipWidth()*1e3;
			double len = width*0.5;
			sectAreas[s] = len*width;
		}
		SectAreas areas = SectAreas.precomputed(rupSet, sectAreas);
		rupSet.addModule(areas);
		singleFaultRupSet.addModule(areas);
		
		List<SectNucleationMFD_Estimator> segAdjusters = new ArrayList<>();
		List<String> segNames = new ArrayList<>();
		List<String> segPrefixes = new ArrayList<>();
		List<Color> segColors = new ArrayList<>();
		
		segAdjusters.add(new ThresholdAveragingSectNuclMFD_Estimator.WorstAvgJumpProb(segModel));
		segNames.add("Jump Probability Threshold Averaging");
		segPrefixes.add("thresh_avg");
		segColors.add(Color.BLUE.darker());
		
		segAdjusters.add(new ThresholdAveragingSectNuclMFD_Estimator.RelGRWorstJumpProb(segModel, 50, true));
		segNames.add("Relative GR Threshold Averaging");
		segPrefixes.add("thresh_avg_rel_gr");
		segColors.add(Color.GREEN.darker());
		
		segAdjusters.add(new SegmentationImpliedSectNuclMFD_Estimator(segModel, MultiBinDistributionMethod.CAPPED_DISTRIBUTED, false));
		segNames.add("Capped-Redistribution");
		segPrefixes.add("capped_redist");
		segColors.add(Color.RED.darker());
		
		Color singleFaultColor = Color.CYAN.darker();
		Color origColor = Color.LIGHT_GRAY;
		
		SupraSeisBValInversionTargetMFDs singleFaultMFDs = new SupraSeisBValInversionTargetMFDs.Builder(
				singleFaultRupSet, supraB).applyDefModelUncertainties(false).build();
		
		SupraSeisBValInversionTargetMFDs.Builder mfdBuilder = new SupraSeisBValInversionTargetMFDs.Builder(rupSet, supraB);
		mfdBuilder.applyDefModelUncertainties(false);
		SupraSeisBValInversionTargetMFDs noAdjMFDs = mfdBuilder.build();
		
		Table<Integer, String, PlotSpec> plotsTable = HashBasedTable.create();
		Table<Integer, String, IncrementalMagFreqDist> origTargetsTable = HashBasedTable.create();
		Table<Integer, String, IncrementalMagFreqDist> targetsTable = HashBasedTable.create();
		Table<Integer, String, Map<Jump, EvenlyDiscretizedFunc>> jumpFuncsTable = HashBasedTable.create();
		
		double minNonZero = Double.POSITIVE_INFINITY;
		double maxY = 0d;
		
		for (int m=0; m<segAdjusters.size(); m++) {
			SectNucleationMFD_Estimator segAdjuster = segAdjusters.get(m);
			
			if (segAdjuster instanceof SegmentationImpliedSectNuclMFD_Estimator)
				((SegmentationImpliedSectNuclMFD_Estimator)segAdjuster).setTrackIndepJumpTargets(true);
			mfdBuilder.clearTargetAdjustments().adjustTargetsForData(segAdjuster);
			
			Color color = segColors.get(m);
			String name = segNames.get(m);
			
			SupraSeisBValInversionTargetMFDs adjMFDs = mfdBuilder.build();
			
			List<List<IncrementalMagFreqDist>> faultTargetMFDs = null;
			if (segAdjuster instanceof SegmentationImpliedSectNuclMFD_Estimator)
				faultTargetMFDs = ((SegmentationImpliedSectNuclMFD_Estimator)segAdjuster).getIndepJumpTargetSupraSeisMFDs();
			
			for (int s=0; s<sects.size(); s++) {
				IncrementalMagFreqDist singleFaultMFD = singleFaultMFDs.getOnFaultSupraSeisNucleationMFDs().get(s);
				IncrementalMagFreqDist origMFD = noAdjMFDs.getOnFaultSupraSeisNucleationMFDs().get(s);
				IncrementalMagFreqDist adjMFD = adjMFDs.getOnFaultSupraSeisNucleationMFDs().get(s);
				
				SummedMagFreqDist excess = null;
				List<IncrementalMagFreqDist> faultTargets = null;
				if (faultTargetMFDs != null) {
					faultTargets = faultTargetMFDs.get(s);
					excess = new SummedMagFreqDist(refMFD.getMinX(), refMFD.size(), refMFD.getDelta());
					for (IncrementalMagFreqDist target : faultTargets)
						excess.addIncrementalMagFreqDist(target);
					
					boolean hasExcess = false;
					for (int i=0; i<excess.size(); i++)
						if ((float)excess.getY(i) > (float)adjMFD.getY(i))
							hasExcess = true;
					if (!hasExcess)
						excess = null;
				}
				
				List<DiscretizedFunc> funcs = new ArrayList<>();
				List<PlotCurveCharacterstics> chars = new ArrayList<>();
				
				IncrementalMagFreqDist origSingleFault = origMFD.deepClone();
				for (int i=maxSingleIndex+1; i<refMFD.size(); i++)
					origSingleFault.set(i, 0d);
				IncrementalMagFreqDist origMultiFault = origMFD.deepClone();
				for (int i=0; i<=maxSingleIndex; i++)
					origMultiFault.set(i, 0d);
				
				// figure out jump bins, and add placeholder funcs
				Map<Jump, EvenlyDiscretizedFunc> jumpBins = new HashMap<>();
				for (int r : rupSet.getRupturesForSection(s)) {
					double mag = rupSet.getMagForRup(r);
					int bin = adjMFD.getClosestXIndex(mag);
					for (Jump jump : rups.get(r).getJumpsIterable()) {
						if (jump.toSection.getSectionId() < jump.fromSection.getSectionId())
							jump = jump.reverse();
						EvenlyDiscretizedFunc myJumpBins = jumpBins.get(jump);
						if (myJumpBins == null) {
							myJumpBins = new EvenlyDiscretizedFunc(adjMFD.getMinX(), adjMFD.getMaxX(), adjMFD.size());
							funcs.add(myJumpBins);
							chars.add(new PlotCurveCharacterstics(PlotSymbol.FILLED_CIRCLE, 4f, Color.BLACK)); // will get shaded by box a bit
							jumpBins.put(jump, myJumpBins);
						}
						myJumpBins.set(bin, 1d);
					}
				}
				jumpFuncsTable.put(s, name, jumpBins);
				
//				origMultiFault.setName("Original Multi-Fault");
//				funcs.add(origMultiFault);
//				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, Color.LIGHT_GRAY));

				singleFaultMFD.setName(jumpBins.size() > 1 ? "Single Fault" : "Without Jump");
				funcs.add(singleFaultMFD);
				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, singleFaultColor));
				
				origMFD.setName("Full GR Target");
				funcs.add(origMFD);
				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, origColor));
				
				if (excess != null) {
					excess.setName("Excess");
					funcs.add(excess);
					chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, new Color(0, 255, 255, 127)));
//							new Color(color.getRed(), color.getGreen(), color.getBlue(), 127)));
				}
				
//				if (hasExcess && noRedistMFDs != null) {
//					adjMFD.setName("Redistribution");
//					funcs.add(adjMFD);
//					chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, new Color(255, 0, 255, 127)));
////							new Color(255, 0, 0, 127)));
//					
//					IncrementalMagFreqDist noRedist = noRedistMFDs.getSectSupraSeisNuclMFDs().get(s);
//					noRedist.setName("Without Redistribution");
//					funcs.add(noRedist);
//					chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, color));
//				} else {
					adjMFD.setName("Adjusted Target");
					funcs.add(adjMFD);
					chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, color));
//				}

//				origSingleFault.setName("Original Singal-Fault");
//				funcs.add(origSingleFault);
//				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, Color.GRAY));
				
				for (int i=0; i<origMFD.size(); i++) {
					double origY = origMFD.getY(i);
					double adjY = adjMFD.getY(i);
					
					if ((float)adjY >= (float)origY) {
						// plot it on top
						double x = adjMFD.getX(i);
						double x1 = x - 0.4*adjMFD.getDelta();
						double x2 = x + 0.4*adjMFD.getDelta();
						ArbitrarilyDiscretizedFunc func = new ArbitrarilyDiscretizedFunc();
						func.set(x1, origY);
						func.set(x2, origY);
						funcs.add(func);
						chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 4f, origColor));
					}
				}
//				origMFD.setName("Original");
//				funcs.add(origMFD);
//				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 3f, new Color(0, 0, 0, 60)));
				
				if (faultTargets != null) {
					boolean first = true;
					for (EvenlyDiscretizedFunc faultTarget : faultTargets) {
						EvenlyDiscretizedFunc copy = new EvenlyDiscretizedFunc(
								faultTarget.getMinX(), faultTarget.getMaxX(), faultTarget.size());
						for (int i=0; i<faultTarget.size(); i++) {
							if (faultTarget.getY(i) == 0d)
								copy.set(i, Double.NaN);
							else
								copy.set(i, faultTarget.getY(i));
						}
						faultTarget = copy;
						if (first)
							faultTarget.setName("Fault Target"+(faultTargets.size() > 1 ? "s" : ""));
						else
							faultTarget.setName(null);
						
						funcs.add(faultTarget);
						chars.add(new PlotCurveCharacterstics(PlotLineType.DOTTED, 2f, PlotSymbol.FILLED_CIRCLE, 3f, Color.BLACK));
						first = false;
					}
				}
				
				for (DiscretizedFunc func : funcs) {
					if (func instanceof IncrementalMagFreqDist) {
						for (Point2D pt : func) {
							if (pt.getY() > 0d) {
								minNonZero = Math.min(minNonZero, pt.getY());
								maxY = Math.max(maxY, pt.getY());
							}
						}
					}
				}
				
				PlotSpec spec = new PlotSpec(funcs, chars, name,
						"Magnitude", "Target Nucleation Rate");
				spec.setLegendVisible(true);
				
				plotsTable.put(s, name, spec);
				targetsTable.put(s, name, adjMFD);
				origTargetsTable.put(s, name, origMFD);
			}
		}
		
		HeadlessGraphPanel gp = PlotUtils.initHeadless();
		Range xRange = new Range(minSupraMag-0.05, maxMag+0.05);
		Range yRange = new Range(Math.pow(10, Math.floor(Math.log10(minNonZero))), Math.pow(10, Math.ceil(Math.log10(maxY)+0.8)));
		
		for (int s=0; s<sects.size(); s++) {
			for (int m=0; m<segAdjusters.size(); m++) {
				String name = segNames.get(m);
				PlotSpec spec = plotsTable.get(s, name);
				IncrementalMagFreqDist target = targetsTable.get(s, name);
				IncrementalMagFreqDist origMFD = origTargetsTable.get(s, name);

				// add annotations
				Map<Jump, EvenlyDiscretizedFunc> jumpFuncs = jumpFuncsTable.get(s, name);
				List<Jump> jumps = new ArrayList<>(jumpFuncs.keySet());
				jumps.sort(new Comparator<Jump>() {

					@Override
					public int compare(Jump o1, Jump o2) {
						EvenlyDiscretizedFunc func1 = jumpFuncs.get(o1);
						EvenlyDiscretizedFunc func2 = jumpFuncs.get(o2);
						
						boolean first = true;
						for (int i=0; i<func1.size(); i++) {
							boolean has1 = func1.getY(i) > 0d;
							boolean has2 = func2.getY(i) > 0d;
							
							if (has1 || has2) {
								if (first && has1 != has2) {
									if (has1)
										// want the one with the leftmost bin to be first
										return -1;
									else
										return 1;
								}
								first = false;
								// if we're here, both have the same first bin
								if (has1 != has2) {
									if (has1)
										// want the one that has more bins to be second
										return 1;
									else
										return 2;
								}
							}
						}
						return 0;
					}
				});
				
				double yDeltaFract = 0.05;
				double logMaxY = Math.log10(yRange.getUpperBound());
				double logMinY = Math.log10(yRange.getLowerBound());
				double logYspan = logMaxY - logMinY;
				double leftX = xRange.getLowerBound()+0.05;
				double insetLeftX = xRange.getLowerBound()+0.15;
				double rightX = xRange.getUpperBound()-0.05;
				
				DecimalFormat fractDF = new DecimalFormat("0.0##");
				DecimalFormat distDF = new DecimalFormat("0.0");
				DecimalFormat eDF = new DecimalFormat("0.00E0");
				
				Font font1 = new Font(Font.SANS_SERIF, Font.BOLD, 22);
				Font font2 = new Font(Font.SANS_SERIF, Font.BOLD, 18);
				double yCount = 0.75; // start a bit down
				double yDeltaEach = logYspan*yDeltaFract;
				List<XYAnnotation> anns = new ArrayList<>();
				for (int j=0; j<jumps.size(); j++) {
					Jump jump = jumps.get(j);
					
					double logY = logMaxY - yDeltaEach*yCount;
					double y = Math.pow(10, logY);
					yCount ++;
					
					String nameStr = jumps.size() > 1 ? "Jump "+(j+1) : "Jump";
					nameStr += ": "+distDF.format(jump.distance)+" km, P≤"+fractDF.format(segModel.calcJumpProbability(jump.distance));
					XYTextAnnotation nameAnn = new XYTextAnnotation(nameStr, leftX, y);
					nameAnn.setFont(font1);
					nameAnn.setTextAnchor(TextAnchor.CENTER_LEFT);
					anns.add(nameAnn);
					
					logY = logMaxY - yDeltaEach*yCount;
					y = Math.pow(10, logY);
					
					// scalars for converting nucleation to participation
					double[] nuclToParticScalars = new double[target.size()];
					double[] avgBinAreas = new double[nuclToParticScalars.length];
					int[] avgCounts = new int[avgBinAreas.length];

					List<Integer> myRupIndexes = new ArrayList<>();
					List<Double> myMags = new ArrayList<>();

					// loop over ruptures for which this section participates
					for (int r : rupSet.getRupturesForSection(s)) {
						int index = target.getClosestXIndex(rupSet.getMagForRup(r));
						avgCounts[index]++;
						avgBinAreas[index] += rupSet.getAreaForRup(r);
						myRupIndexes.add(r);
						myMags.add(rupSet.getMagForRup(r));
					}
					double sectArea = rupSet.getAreaForSection(s);
					for (int i=0; i<nuclToParticScalars.length; i++) {
						if (avgCounts[i] > 0) {
							avgBinAreas[i] /= avgCounts[i];
							nuclToParticScalars[i] = avgBinAreas[i]/sectArea;
						}
					}
					
					double sumPartic = 0d;
					double sumJumpPartic = 0d;
					double sumOrigPartic = 0d;
					double sumOrigJumpPartic = 0d;
					EvenlyDiscretizedFunc jumpFunc = jumpFuncs.get(jump);
					for (int i=0; i<jumpFunc.size(); i++) {
						if (jumpFunc.getY(i) > 0) {
							// affects this bin
							// move to correct y value
							jumpFunc.set(i, y);
							sumJumpPartic += target.getY(i)*nuclToParticScalars[i];
							sumOrigJumpPartic += origMFD.getY(i)*nuclToParticScalars[i];
						}
						sumPartic += target.getY(i)*nuclToParticScalars[i];
						sumOrigPartic += origMFD.getY(i)*nuclToParticScalars[i];
					}
					
					List<String[]> insetLines = new ArrayList<>();
					insetLines.add(new String[] {"Jump Magnitudes:"});
					insetLines.add(new String[] {"Original Jump Rate Share:",
							eDF.format(sumOrigJumpPartic)+" / "+eDF.format(sumOrigPartic)+" = "+fractDF.format(sumOrigJumpPartic/sumOrigPartic)});
					insetLines.add(new String[] {"Adjusted Jump Rate Share:",
							eDF.format(sumJumpPartic)+" / "+eDF.format(sumPartic)+" = "+fractDF.format(sumJumpPartic/sumPartic)});
//					insetLines.add(new String[] {"Total Target Participation Rate:", eDF.format(sumPartic)});
//					insetLines.add(new String[] {"Jump Participation Rate:", eDF.format(sumJumpPartic)});
//					insetLines.add(new String[] {"Jump Fractional Participation Rate:", fractDF.format(sumJumpPartic/sumPartic)});
//							eDF.format(sumJumpPartic)+" / "+eDF.format(sumPartic)+" = "+fractDF.format(sumJumpPartic/sumPartic)});
					
					for (String[] insetLine : insetLines) {
						logY = logMaxY - yDeltaEach*yCount;
						y = Math.pow(10, logY);
						yCount += 0.8;
						
						XYTextAnnotation left = new XYTextAnnotation(insetLine[0], insetLeftX, y);
						left.setFont(font2);
						left.setTextAnchor(TextAnchor.CENTER_LEFT);
						anns.add(left);
						if (insetLine.length > 1) {
							XYTextAnnotation right = new XYTextAnnotation(insetLine[1], rightX, y);
							right.setFont(font2);
							right.setTextAnchor(TextAnchor.CENTER_RIGHT);
							anns.add(right);
						}
					}
					
					yCount += 0.2;
				}
				
				// now add transparent white backing for the text box
				double logY = logMaxY - yDeltaEach*yCount;
				double y = Math.pow(10, logY);
				XYBoxAnnotation box = new XYBoxAnnotation(xRange.getLowerBound(), y,
						xRange.getUpperBound(), yRange.getUpperBound(), null, null, new Color(255, 255, 255, 120));
				anns.add(0, box);
				spec.setPlotAnnotations(anns);
				
				String plotPrefix = "sect_"+s+"_target_"+segPrefixes.get(m);
				
				gp.drawGraphPanel(spec, false, true, xRange, yRange);
				
				PlotUtils.writePlots(outputDir, plotPrefix, gp, 800, 650, true, false, false);
			}
		}
	}

}
