package scratch.kevin.bbp;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.data.Range;
import org.jfree.ui.TextAnchor;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.data.function.LightFixedXFunc;
import org.opensha.commons.gui.plot.HeadlessGraphPanel;
import org.opensha.commons.gui.plot.PlotCurveCharacterstics;
import org.opensha.commons.gui.plot.PlotLineType;
import org.opensha.commons.gui.plot.PlotSpec;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.common.primitives.Doubles;

public class SeismogramPlotter {
	
	public static DiscretizedFunc[] loadBBP_Seis(File bbpFile) throws IOException {
		return loadBBP_Seis(Files.readLines(bbpFile, Charset.defaultCharset()));
	}
	
	public static DiscretizedFunc[] loadBBP_Seis(List<String> lines) {
		List<Double> times = new LinkedList<>();
		List<Double> ns = new LinkedList<>();
		List<Double> ew = new LinkedList<>();
		List<Double> vert = new LinkedList<>();
		
		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("#"))
				continue;
			String[] split;
			if (line.contains("\t"))
				split = line.split("\t");
			else
				split = line.split("\\s+");
			Preconditions.checkState(split.length == 4);
			double t = Double.parseDouble(split[0]);
			
			times.add(t);
			ns.add(Double.parseDouble(split[1]));
			ew.add(Double.parseDouble(split[2]));
			vert.add(Double.parseDouble(split[3]));
		}
		
		double[] xVals = Doubles.toArray(times);
		
		LightFixedXFunc nsFunc = new LightFixedXFunc(xVals, Doubles.toArray(ns));
		nsFunc.setName("N-S");
		LightFixedXFunc ewFunc = new LightFixedXFunc(xVals, Doubles.toArray(ew));
		ewFunc.setName("E-W");
		LightFixedXFunc vertFunc = new LightFixedXFunc(xVals, Doubles.toArray(vert));
		vertFunc.setName("Vert");
		
		return new DiscretizedFunc[] { nsFunc, ewFunc, vertFunc };
	}
	
	private static double absMaxY(DiscretizedFunc func) {
		return Math.max(Math.abs(func.getMaxY()), Math.abs(func.getMinY()));
	}
	
	private static DiscretizedFunc offset(DiscretizedFunc func, double yOffset) {
		DiscretizedFunc offset = func.deepClone();
		for (int i=0; i<func.size(); i++)
			offset.set(i, offset.getY(i)+yOffset);
		return offset;
	}
	
	public static void plotSeismograms(DiscretizedFunc[] seismograms, String title, boolean accel, File outputDir, String prefix,
			boolean split, List<DiscretizedFunc[]> comps) throws IOException {
		Preconditions.checkState(seismograms.length == 3);
		
		double horzMax = Math.max(absMaxY(seismograms[0]), absMaxY(seismograms[1]));
		double vertMax = absMaxY(seismograms[2]);
		
//		if (comps != null) {
//			for (DiscretizedFunc[] comp : comps) {
//				horzMax = Math.max(horzMax, absMaxY(comp[0]));
//				horzMax = Math.max(horzMax, absMaxY(comp[1]));
//				vertMax = Math.max(vertMax, absMaxY(comp[2]));
//			}
//		}
		
		vertMax *= 1.2;
		horzMax *= 1.2;
		
		List<PlotSpec> specs = new ArrayList<>();
		
		List<DiscretizedFunc> nsFuncs = new ArrayList<>();
		List<DiscretizedFunc> ewFuncs = new ArrayList<>();
		List<DiscretizedFunc> vertFuncs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		nsFuncs.add(seismograms[0]);
		ewFuncs.add(seismograms[1]);
		vertFuncs.add(seismograms[2]);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 1f, Color.BLUE));
		
		double maxX = seismograms[0].getMaxX();
		double horzMinY = -horzMax;
		double vertMinY = -vertMax;
		
		double offsetMult = 2;
		
		if (comps != null) {
			for (int i=0; i<comps.size(); i++) {
				DiscretizedFunc[] comp = comps.get(i);
				maxX = Math.max(maxX, seismograms[0].getMaxX());
				nsFuncs.add(0, offset(comp[0], -horzMax*offsetMult*(i+1)));
				ewFuncs.add(0, offset(comp[1], -horzMax*offsetMult*(i+1)));
				vertFuncs.add(0, offset(comp[2], -vertMax*offsetMult*(i+1)));
				chars.add(0, new PlotCurveCharacterstics(PlotLineType.SOLID, 1f, Color.GRAY));
				horzMinY = -horzMax*offsetMult*(i+1) - horzMax;
				vertMinY = -vertMax*offsetMult*(i+1) - vertMax;
			}
		}
		
		String xAxisLabel = "Time (s)";
		String yAxisLabel;
		if (accel)
			yAxisLabel = "Acceleration (cm/s/s)";
		else
			yAxisLabel = "Velocity (cm/s)";
		PlotSpec nsSpec = new PlotSpec(nsFuncs, chars, title, xAxisLabel, yAxisLabel);
		PlotSpec ewSpec = new PlotSpec(ewFuncs, chars, title, xAxisLabel, yAxisLabel);
		PlotSpec vertSpec = new PlotSpec(vertFuncs, chars, title, xAxisLabel, yAxisLabel);
		
		specs.add(nsSpec);
		specs.add(ewSpec);
		specs.add(vertSpec);
		
		Range xRange = new Range(0d, maxX);
		List<Range> xRanges = new ArrayList<>();
		xRanges.add(xRange);
		Range horzYRange = new Range(horzMinY, horzMax);
		Range vertYRange = new Range(vertMinY, vertMax);
		List<Range> yRanges = new ArrayList<>();
		yRanges.add(horzYRange);
		yRanges.add(horzYRange);
		yRanges.add(vertYRange);
		
		String[] names = { "N/S", "E/W", "Vert" };
		if (split) {
			String[] fnames = { "ns", "ew", "vert" };
			for (int i=0; i<specs.size(); i++) {
				HeadlessGraphPanel gp = buildGP();
				
				PlotSpec spec = specs.get(i);
				spec.setTitle(spec.getTitle()+" ("+names[i]+")");
				gp.drawGraphPanel(spec, false, false, xRange, yRanges.get(i));
				
				gp.getChartPanel().setSize(800, 800);
				File file = new File(outputDir, prefix+"_"+fnames[i]);
				gp.saveAsPNG(file.getAbsolutePath()+".png");
				gp.saveAsPDF(file.getAbsolutePath()+".pdf");
			}
		} else {
			HeadlessGraphPanel gp = buildGP();
			
			for (int i=0; i<specs.size(); i++) {
				double y;
				if (i ==2)
					y = vertMax;
				else
					y = horzMax;
				XYTextAnnotation ann = new XYTextAnnotation("  "+names[i], 0, y);
				ann.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
				ann.setTextAnchor(TextAnchor.TOP_LEFT);
				List<XYTextAnnotation> anns = new ArrayList<>();
				anns.add(ann);
				specs.get(i).setPlotAnnotations(anns);
			}
			
			gp.drawGraphPanel(specs, false, false, xRanges, yRanges);
			
			gp.getChartPanel().setSize(800, 800);
			File file = new File(outputDir, prefix);
			gp.saveAsPNG(file.getAbsolutePath()+".png");
			gp.saveAsPDF(file.getAbsolutePath()+".pdf");
		}
		
	}
	
	private static HeadlessGraphPanel buildGP() {
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(24);
		gp.setPlotLabelFontSize(24);
		gp.setLegendFontSize(20);
		gp.setBackgroundColor(Color.WHITE);
		return gp;
	}
	
	public static File findBBP_SeisFile(File dir, String siteName, boolean accel) throws FileNotFoundException {
		for (File file : dir.listFiles()) {
			String name = file.getName();
			if (!name.contains(siteName))
				continue;
			if (accel && name.endsWith(".acc.bbp"))
				return file;
			if (!accel && name.endsWith(".vel.bbp"))
				return file;
		}
		throw new FileNotFoundException("No BBP seismogram files found for site "+siteName+" in "+dir.getAbsolutePath());
	}

	public static void main(String[] args) throws IOException {
		File rsDir = new File("/data/kevin/simulators/catalogs/rundir2194_long/event_srfs/"
				+ "event_136704_0.05s_ADJ_VEL_bbp");
		String siteName = "SBSM";
		
		boolean accel = false;
		DiscretizedFunc[] seis = loadBBP_Seis(findBBP_SeisFile(rsDir, siteName, accel));
		List<DiscretizedFunc[]> comps = null;
		
		File compDir = new File("/home/kevin/bbp/parallel/2017_10_04-rundir2194_long-event136704-dx1.16-noHF/results");
		comps = new ArrayList<>();
		comps.add(loadBBP_Seis(findBBP_SeisFile(new File(compDir, "run_0"), siteName, accel)));
		comps.add(loadBBP_Seis(findBBP_SeisFile(new File(compDir, "run_1"), siteName, accel)));
		comps.add(loadBBP_Seis(findBBP_SeisFile(new File(compDir, "run_2"), siteName, accel)));
		
		plotSeismograms(seis, "Event 136704, SBSM", accel, new File("/tmp"), "test_seis", true, comps);
		plotSeismograms(seis, "Event 136704, SBSM", accel, new File("/tmp"), "test_seis", false, comps);
	}

}
