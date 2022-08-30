package scratch.kevin.nshm23;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opensha.commons.data.function.EvenlyDiscretizedFunc;
import org.opensha.commons.data.xyz.GeoDataSet;
import org.opensha.commons.data.xyz.GeoDataSetMath;
import org.opensha.commons.data.xyz.GriddedGeoDataSet;
import org.opensha.commons.geo.GriddedRegion;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.Region;
import org.opensha.commons.gui.plot.PlotCurveCharacterstics;
import org.opensha.commons.gui.plot.PlotLineType;
import org.opensha.commons.mapping.gmt.elements.GMT_CPT_Files;
import org.opensha.commons.util.MarkdownUtils;
import org.opensha.commons.util.MarkdownUtils.TableBuilder;
import org.opensha.commons.util.cpt.CPT;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.util.RupSetMapMaker;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.NSHM23_InvConfigFactory;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_DeformationModels;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_FaultModels;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_MaxMagOffFault;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_RegionalSeismicity;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_SpatialSeisPDFs;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.targetMFDs.SupraSeisBValInversionTargetMFDs;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.util.NSHM23_RegionLoader;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.util.NSHM23_RegionLoader.SeismicityRegions;
import org.opensha.sha.faultSurface.FaultSection;
import org.opensha.sha.magdist.GutenbergRichterMagFreqDist;

public class SeisGridPlotter {

	public static void main(String[] args) throws IOException {
		
		File outputDir = new File("/home/kevin/markdown/nshm23-misc/seis_pdfs");
		
		List<String> lines = new ArrayList<>();

		double maxMagOff = NSHM23_MaxMagOffFault.MAG_7p6.getMaxMagOffFault();
		List<? extends FaultSection> subSects = NSHM23_DeformationModels.GEOLOGIC.build(NSHM23_FaultModels.NSHM23_v2);
		
		Region region = NSHM23_RegionLoader.loadFullConterminousWUS();
		List<SeismicityRegions> seisRegions = NSHM23_InvConfigFactory.getSeismicityRegions(region);
		GriddedRegion gridReg = NSHM23_InvConfigFactory.getGriddedSeisRegion(region); 
		RupSetMapMaker mapMaker = new RupSetMapMaker(subSects, region);
		mapMaker.setSectTraceChar(new PlotCurveCharacterstics(PlotLineType.SOLID, 0.5f, new Color(0, 0, 0, 80)));
		mapMaker.setSectOutlineChar(null);
		mapMaker.setWritePDFs(false);
		
		EvenlyDiscretizedFunc refMFD = SupraSeisBValInversionTargetMFDs.buildRefXValues(8d);
		
		CPT linearCPT = GMT_CPT_Files.RAINBOW_UNIFORM.instance().rescale(0d, 0.01);
		CPT logCPT = GMT_CPT_Files.RAINBOW_UNIFORM.instance().rescale(-6, -1);
		
		CPT ratioCPT = GMT_CPT_Files.GMT_POLAR.instance().rescale(0.5, 2d);
		CPT logRatioCPT = GMT_CPT_Files.GMT_POLAR.instance().rescale(-1d, 1d);
		
		lines.add("# NSHM23 Spatial Seismicity PDFs");
		lines.add("");
		
		int tocIndex = lines.size();
		String topLink = "*[(top)](#table-of-contents)*";
		
		for (NSHM23_RegionalSeismicity seisRates : NSHM23_RegionalSeismicity.values()) {
			lines.add("## "+seisRates.getName());
			lines.add(topLink); lines.add("");
			
			GriddedGeoDataSet averagePDF = buildPDF(seisRates, NSHM23_SpatialSeisPDFs.AVERAGE,
					maxMagOff, gridReg, refMFD, seisRegions);
			GriddedGeoDataSet logAveragePDF = averagePDF.copy();
			logAveragePDF.log10();
			
			lines.add("### Average PDF, "+seisRates.getName());
			lines.add(topLink); lines.add("");
			
			TableBuilder table = MarkdownUtils.tableBuilder();
			table.addLine("Linear", "Log10");
			String prefix = seisRates.getFilePrefix()+"_average";
			mapMaker.plotXYZData(averagePDF, linearCPT, "Rate M≥5");
			mapMaker.plot(outputDir, prefix, "Average PDF, "+seisRates.getName());
			table.initNewLine();
			table.addColumn("![Map]("+prefix+".png)");
			mapMaker.plotXYZData(logAveragePDF, logCPT, "Log10 (Rate M≥5)");
			mapMaker.plot(outputDir, prefix+"_log", "Average PDF, "+seisRates.getName());
			table.addColumn("![Map]("+prefix+"_log.png)");
			table.finalizeLine();
			lines.addAll(table.build());
			lines.add("");
			
			for (NSHM23_SpatialSeisPDFs pdf : NSHM23_SpatialSeisPDFs.values()) {
				if (pdf.getNodeWeight(null) == 0d)
					continue;
				lines.add("### "+pdf.getName()+", "+seisRates.getName());
				lines.add(topLink); lines.add("");
				System.out.println("Processing "+pdf.getName()+", "+seisRates.getName());
				table = MarkdownUtils.tableBuilder();
				table.addLine(MarkdownUtils.boldCentered(pdf.getName()), "Ratio to Mean");
				
				GriddedGeoDataSet combinedPDF = buildPDF(seisRates, pdf, maxMagOff, gridReg, refMFD, seisRegions);
				
				String title = pdf.getName()+", "+seisRates.getName();
				
				table.initNewLine();
				mapMaker.plotXYZData(combinedPDF, linearCPT, "Rate M≥5");
				prefix = seisRates.getFilePrefix()+"_"+pdf.getFilePrefix();
				mapMaker.plot(outputDir, prefix, title);
				table.addColumn("![Map]("+prefix+".png)");
				
				GeoDataSet ratio = GeoDataSetMath.divide(combinedPDF, averagePDF);
				mapMaker.plotXYZData(ratio, ratioCPT, "Rate M≥5 / Avg Rate M≥5");
				mapMaker.plot(outputDir, prefix+"_ratio", title);
				table.addColumn("![Map]("+prefix+"_ratio.png)");
				table.finalizeLine();
				
				combinedPDF.log10();
				table.initNewLine();
				mapMaker.plotXYZData(combinedPDF, logCPT, "Log10(Rate M≥5)");
				mapMaker.plot(outputDir, prefix+"_log", title);
				table.addColumn("![Map]("+prefix+"_log.png)");
				
				ratio.log10();
				mapMaker.plotXYZData(ratio, logRatioCPT, "Log10(Rate M≥5 / Avg Rate M≥5)");
				mapMaker.plot(outputDir, prefix+"_log_ratio", title);
				table.addColumn("![Map]("+prefix+"_log_ratio.png)");
				table.finalizeLine();
				lines.addAll(table.build());
				lines.add("");
			}
		}
		
		// add TOC
		lines.addAll(tocIndex, MarkdownUtils.buildTOC(lines, 2));
		lines.add(tocIndex, "## Table Of Contents");

		// write markdown
		MarkdownUtils.writeReadmeAndHTML(lines, outputDir);
	}
	
	private static GriddedGeoDataSet buildPDF(NSHM23_RegionalSeismicity seisRates, NSHM23_SpatialSeisPDFs pdf,
			double maxMagOff, GriddedRegion gridReg, EvenlyDiscretizedFunc refMFD, List<SeismicityRegions> seisRegions) throws IOException {
		GriddedGeoDataSet combinedPDF = new GriddedGeoDataSet(gridReg, false);
		for (int i=0; i<combinedPDF.size(); i++)
			combinedPDF.set(i, Double.NaN);
		
		for (SeismicityRegions primaryReg : seisRegions) {
			GutenbergRichterMagFreqDist totalGR = seisRates.build(primaryReg, refMFD, maxMagOff);
			double rateM5 = totalGR.getCumRate(totalGR.getClosestXIndex(5.01));
			
			GriddedGeoDataSet regPDF = pdf.loadXYZ(primaryReg);
			regPDF.scale(rateM5);
			
			for (int i=0; i<regPDF.size(); i++) {
				Location loc = regPDF.getLocation(i);
				int index = gridReg.indexForLocation(loc);
				if (index >= 0)
					combinedPDF.set(index, regPDF.get(i));
			}
		}
		return combinedPDF;
	}

}
