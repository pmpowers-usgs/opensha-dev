package scratch.kevin.prvi25;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.opensha.commons.geo.GriddedRegion;
import org.opensha.commons.geo.json.Feature;
import org.opensha.commons.logicTree.LogicTree;
import org.opensha.commons.logicTree.LogicTreeLevel;
import org.opensha.commons.logicTree.LogicTreeLevel.FileBackedLevel;
import org.opensha.commons.logicTree.LogicTreeNode;
import org.opensha.commons.logicTree.LogicTreeNode.FileBackedNode;
import org.opensha.commons.util.modules.ModuleContainer;
import org.opensha.sha.earthquake.faultSysSolution.hazard.AbstractLogicTreeHazardCombiner;
import org.opensha.sha.earthquake.faultSysSolution.hazard.mpj.MPJ_LogicTreeHazardCalc;
import org.opensha.sha.earthquake.faultSysSolution.modules.SolutionLogicTree;
import org.opensha.sha.earthquake.faultSysSolution.util.FaultSysTools;
import org.opensha.sha.earthquake.param.IncludeBackgroundOption;

import com.google.common.base.Preconditions;

public class CrustalSubductionLogicTreeCombine extends AbstractLogicTreeHazardCombiner {
	
	private static final IncludeBackgroundOption GRID_SEIS_DEFAULT = IncludeBackgroundOption.EXCLUDE;
	private static final String RESULTS_FILE_NAME_DEFAULT = "results.zip";
	private static final String HAZARD_DIR_NAME_DEFAULT = "results";
	private static final String GRID_REG_FILE_NAME_DEFAULT = "results_hazard.zip"; // get region from hazard zip

	public static void main(String[] args) throws IOException {
		CommandLine cmd = FaultSysTools.parseOptions(createOptions(), args, CrustalSubductionLogicTreeCombine.class);
		args = cmd.getArgs();
		Preconditions.checkArgument(args.length == 3, "USAGE: <crustal-dir> <subduction-dir> <output-dir>");
		ModuleContainer.VERBOSE_DEFAULT = false;
		File crustalDir = new File(args[0]);
		Preconditions.checkState(crustalDir.exists(),
				"Crustal directory doesn't exist: %s", crustalDir.getAbsolutePath());
		File subductionDir = new File(args[1]);
		Preconditions.checkState(subductionDir.exists(),
				"Subduction directory doesn't exist: %s", subductionDir.getAbsolutePath());
		File outputDir = new File(args[2]);
		Preconditions.checkState(outputDir.exists() || outputDir.mkdir(),
				"Output directory doesn't exist and couldn't be created: %s", outputDir.getAbsolutePath());
		
		String resultsFileName = RESULTS_FILE_NAME_DEFAULT;
		if (cmd.hasOption("results-file-name"))
			resultsFileName = cmd.getOptionValue("results-file-name");
		String hazardDirName = HAZARD_DIR_NAME_DEFAULT;
		if (cmd.hasOption("hazard-dir-name"))
			hazardDirName = cmd.getOptionValue("hazard-dir-name");
		String gridRegFileName = GRID_REG_FILE_NAME_DEFAULT;
		if (cmd.hasOption("grid-reg-file-name"))
			gridRegFileName = cmd.getOptionValue("grid-reg-file-name");
		IncludeBackgroundOption bgOp = GRID_SEIS_DEFAULT;
		if (cmd.hasOption("gridded-seis"))
			bgOp = IncludeBackgroundOption.valueOf(cmd.getOptionValue("gridded-seis"));
		
		LogicTree<?> crustalLT;
		SolutionLogicTree crustalSLT;
		LogicTree<?> subductionLT;
		SolutionLogicTree subductionSLT;
		if (cmd.hasOption("logic-tree-file-name")) {
			String logicTreeFileName = cmd.getOptionValue("logic-tree-file-name");
			File crustalLogicTreefile = new File(crustalDir, logicTreeFileName);
			crustalLT = LogicTree.read(crustalLogicTreefile);
			File subductionLogicTreeFile = new File(subductionDir, logicTreeFileName);
			subductionLT = LogicTree.read(subductionLogicTreeFile);
			File crustalSLTFile = new File(crustalDir, resultsFileName);
			if (crustalSLTFile.exists())
				crustalSLT = SolutionLogicTree.load(crustalSLTFile, crustalLT);
			else
				crustalSLT = null;
			File subductionSLTFile = new File(subductionDir, resultsFileName);
			if (subductionSLTFile.exists())
				subductionSLT = SolutionLogicTree.load(subductionSLTFile, subductionLT);
			else
				subductionSLT = null;
		} else {
			crustalSLT = SolutionLogicTree.load(new File(crustalDir, resultsFileName));
			crustalLT = crustalSLT.getLogicTree();
			subductionSLT = SolutionLogicTree.load(new File(subductionDir, resultsFileName));
			subductionLT = subductionSLT.getLogicTree();
		}
		
		if (crustalSLT != null)
			crustalSLT.setVerbose(false);
		if (subductionSLT != null)
			subductionSLT.setVerbose(false);
		
		GriddedRegion gridReg = loadGridReg(new File(crustalDir, gridRegFileName));
		Preconditions.checkState(gridReg.equals(loadGridReg(new File(subductionDir, gridRegFileName))),
				"Crustal and subduction gridded regions differ");
		
		String outputHazardFileName;
		if (cmd.hasOption("output-hazard-file-name"))
			outputHazardFileName = cmd.getOptionValue("output-hazard-file-name");
		else if (bgOp == IncludeBackgroundOption.EXCLUDE)
			outputHazardFileName = "results_hazard.zip";
		else if (bgOp == IncludeBackgroundOption.INCLUDE)
			outputHazardFileName = "results_hazard_full_gridded.zip";
		else if (bgOp == IncludeBackgroundOption.ONLY)
			outputHazardFileName = "results_hazard_gridded_only.zip";
		else
			throw new IllegalStateException();
		
//		CrustalSubductionLogicTreeCombine combiner = new CrustalSubductionLogicTreeCombine(
//				crustalLT, crustalSLT, new File(crustalDir, hazardDirName), bgOp,
//				subductionLT, subductionSLT, new File(subductionDir, hazardDirName), bgOp,
//				resultsOutputFile,
//				new File(outputDir, outputHazardFileName), gridReg);
		CrustalSubductionLogicTreeCombine combiner = new CrustalSubductionLogicTreeCombine(
				crustalLT, subductionLT);
		
		if (!cmd.hasOption("disable-write-results")) {
			File resultsOutputFile = new File(outputDir, resultsFileName);
			Preconditions.checkNotNull(crustalSLT);
			Preconditions.checkNotNull(subductionSLT);
			combiner.setWriteSLTs(crustalSLT, subductionSLT, resultsOutputFile);
		}
		
		combiner.setCombineHazardMaps(new File(crustalDir, hazardDirName), bgOp,
				new File(subductionDir, hazardDirName), bgOp, new File(outputDir, outputHazardFileName), gridReg);
		
		long treeSize = (long)crustalLT.size()*(long)subductionLT.size();
		if (cmd.hasOption("pre-samples")) {
			// more memory efficient pre-sampling version
			int samples = Integer.parseInt(cmd.getOptionValue("pre-samples"));
			long seed = cmd.hasOption("rand-seed") ? Long.parseLong(cmd.getOptionValue("rand-seed")) : treeSize*(long)samples;
			combiner.pairwiseSampleTree(samples, seed);
		}
		
		if (cmd.hasOption("samples")) {
			int samples = Integer.parseInt(cmd.getOptionValue("samples"));
			if (samples < treeSize) {
				long seed = cmd.hasOption("rand-seed") ? Long.parseLong(cmd.getOptionValue("rand-seed")) : treeSize*(long)samples;
				combiner.sampleTree(samples, seed);
			}
		}
		
		LogicTree<LogicTreeNode> combTree = combiner.getCombTree();
		File treeOutputFile;
		if (cmd.hasOption("logic-tree-file-name"))
			treeOutputFile = new File(outputDir, cmd.getOptionValue("logic-tree-file-name"));
		else
			treeOutputFile = new File(outputDir, "logic_tree_"+bgOp.name()+".json");
		combTree.write(treeOutputFile);
		
		try {
			combiner.build();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static Options createOptions() {
		Options ops = new Options();

		ops.addOption(null, "pre-samples", true, "Number of pre-samples to draw. This is more memory efficient and doesn't require "
				+ "instantiating the entire tree in memory first.");
		ops.addOption(null, "samples", true, "Number of samples to draw");
		ops.addOption(null, "rand-seed", true, "Random seed for use when sampling");
		ops.addOption(null, "gridded-seis", true, "Gridded seismicity option. One of "
				+FaultSysTools.enumOptions(IncludeBackgroundOption.class)+". Default: "+GRID_SEIS_DEFAULT.name());
		ops.addOption(null, "results-file-name", true, "Results file name. Default: "+RESULTS_FILE_NAME_DEFAULT);
		ops.addOption(null, "logic-tree-file-name", true, "Logic tree file name. Default uses that from results file");
		ops.addOption(null, "hazard-dir-name", true, "Hazard dir name. Default: "+HAZARD_DIR_NAME_DEFAULT);
		ops.addOption(null, "grid-reg-file-name", true, "Gridded region file name. Default: "+GRID_REG_FILE_NAME_DEFAULT);
		ops.addOption(null, "disable-write-results", false, "Flag to disable writing the combined solution logic tree fule");
		ops.addOption(null, "output-hazard-file-name", true, "Name for output hazard file; default depends on background seismicity.");
		
		return ops;
	}

	public CrustalSubductionLogicTreeCombine(LogicTree<?> outerLT, LogicTree<?> innerLT) {
		super(outerLT, innerLT);
	}
	
	private static GriddedRegion loadGridReg(File regFile) throws IOException {
		Preconditions.checkState(regFile.exists(), "Supplied region file doesn't exist: %s", regFile.getAbsolutePath());
		if (regFile.getName().toLowerCase().endsWith(".zip")) {
			// it's a zip file, assume it's a prior hazard calc
			ZipFile zip = new ZipFile(regFile);
			ZipEntry regEntry = zip.getEntry(MPJ_LogicTreeHazardCalc.GRID_REGION_ENTRY_NAME);
			System.out.println("Reading gridded region from zip file: "+regEntry.getName());
			BufferedReader bRead = new BufferedReader(new InputStreamReader(zip.getInputStream(regEntry)));
			GriddedRegion region = GriddedRegion.fromFeature(Feature.read(bRead));
			zip.close();
			return region;
		} else {
			Feature feature = Feature.read(regFile);
			return GriddedRegion.fromFeature(feature);
		}
	}
	
	private static void remapTree(LogicTree<?> tree, Map<LogicTreeLevel<?>, LogicTreeLevel<?>> levelRemaps,
			Map<LogicTreeNode, LogicTreeNode> nodeRemaps, String nameAdd, String shortNameAdd) {
		for (LogicTreeLevel<?> level : tree.getLevels()) {
			String name = level.getName();
			if (name.toLowerCase().contains("crustal") || name.toLowerCase().contains("subduction")) {
				// keep it as is
				levelRemaps.put(level, level);
				for (LogicTreeNode node : level.getNodes())
					nodeRemaps.put(node, node);
			} else {
				// remap to a file backed
				List<FileBackedNode> modNodes = new ArrayList<>();
				for (LogicTreeNode node : level.getNodes()) {
					FileBackedNode modNode = new FileBackedNode(nameAdd+" "+node.getName(), node.getShortName(),
							node.getNodeWeight(null), shortNameAdd+node.getFilePrefix());
					modNodes.add(modNode);
					nodeRemaps.put(node, modNode);
				}
				FileBackedLevel modLevel = new FileBackedLevel(nameAdd+" "+name, shortNameAdd+level.getShortName(), modNodes);
				modLevel.setAffected(level.getAffected(), level.getNotAffected(), false);
				levelRemaps.put(level, modLevel);
			}
		}
	}

	@Override
	protected void remapOuterTree(LogicTree<?> tree, Map<LogicTreeLevel<?>, LogicTreeLevel<?>> levelRemaps,
			Map<LogicTreeNode, LogicTreeNode> nodeRemaps) {
		remapTree(tree, levelRemaps, nodeRemaps, "Crustal", "Crust");
	}

	@Override
	protected void remapInnerTree(LogicTree<?> tree, Map<LogicTreeLevel<?>, LogicTreeLevel<?>> levelRemaps,
			Map<LogicTreeNode, LogicTreeNode> nodeRemaps) {
		remapTree(tree, levelRemaps, nodeRemaps, "Subduction", "Sub");
	}

	@Override
	protected boolean doesOuterSupplySols() {
		return true;
	}

	@Override
	protected boolean doesInnerSupplySols() {
		return true;
	}

	@Override
	protected boolean isSerializeGridded() {
		// TODO
		return false;
	}

	@Override
	protected List<LogicTreeLevel<?>> getCommonLevels() {
		return List.of();
	}

}
