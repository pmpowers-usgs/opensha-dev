package scratch.kevin.nshm23.prvi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensha.commons.logicTree.LogicTree;
import org.opensha.commons.logicTree.LogicTreeBranch;
import org.opensha.commons.logicTree.LogicTreeLevel;
import org.opensha.commons.logicTree.LogicTreeNode;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemSolution;
import org.opensha.sha.earthquake.faultSysSolution.hazard.AbstractLogicTreeHazardCombiner;
import org.opensha.sha.earthquake.faultSysSolution.modules.FaultGridAssociations;
import org.opensha.sha.earthquake.faultSysSolution.modules.GridSourceList;
import org.opensha.sha.earthquake.faultSysSolution.modules.GridSourceProvider;
import org.opensha.sha.earthquake.faultSysSolution.modules.MFDGridSourceProvider;
import org.opensha.sha.earthquake.faultSysSolution.util.TrueMeanSolutionCreator;
import org.opensha.sha.earthquake.rupForecastImpl.prvi25.gridded.PRVI25_GridSourceBuilder;
import org.opensha.sha.earthquake.rupForecastImpl.prvi25.logicTree.PRVI25_CrustalFaultModels;
import org.opensha.sha.earthquake.rupForecastImpl.prvi25.logicTree.PRVI25_LogicTreeBranch;
import org.opensha.sha.earthquake.rupForecastImpl.prvi25.logicTree.PRVI25_SubductionFaultModels;

import com.google.common.base.Preconditions;

public class CrustalSubductionTrueMeanCreator {

	public static void main(String[] args) throws IOException {
		File crustalDir = new File(args[0]);
		File subductionDir = new File(args[1]);
		File outputFile = new File(args[2]);
		boolean gridded = args.length < 4 ? false : Boolean.parseBoolean(args[3]);
		
		Map<PRVI25_CrustalFaultModels, FaultSystemSolution> crustalBASols = new HashMap<>();
		Map<PRVI25_SubductionFaultModels, FaultSystemSolution> subductionBASols = new HashMap<>();
		
		String suffix = gridded ? "_branch_averaged_gridded.zip" : "_branch_averaged.zip";
		
		for (PRVI25_CrustalFaultModels fm : PRVI25_CrustalFaultModels.values()) {
			File crustalBA = new File(crustalDir, "results_"+fm.getFilePrefix()+suffix);
			if (crustalBA.exists()) {
				FaultSystemSolution sol = FaultSystemSolution.load(crustalBA);
				if (gridded && sol.getGridSourceProvider() instanceof MFDGridSourceProvider) {
					GridSourceList gridSources = GridSourceList.convert(
							(MFDGridSourceProvider)sol.getGridSourceProvider(),
							sol.getRupSet().requireModule(FaultGridAssociations.class),
							new PRVI25_GridSourceBuilder.NSHM23_WUS_FiniteRuptureConverter());
					sol.setGridSourceProvider(gridSources);
				}
				crustalBASols.put(fm, sol);
			}
		}
		Preconditions.checkState(!crustalBASols.isEmpty());
		
		for (PRVI25_SubductionFaultModels fm : PRVI25_SubductionFaultModels.values()) {
			File subductionBA = new File(subductionDir, "results_"+fm.getFilePrefix()+suffix);
			if (subductionBA.exists())
				subductionBASols.put(fm, FaultSystemSolution.load(subductionBA));
		}
		Preconditions.checkState(!subductionBASols.isEmpty());
		
		List<LogicTreeLevel<? extends LogicTreeNode>> levels = new ArrayList<>();
		List<LogicTreeBranch<LogicTreeNode>> branches = new ArrayList<>();
		
		levels.add(PRVI25_LogicTreeBranch.CRUSTAL_FM);
		levels.add(PRVI25_LogicTreeBranch.SUB_FM);
		
		for (PRVI25_CrustalFaultModels crustalFM : crustalBASols.keySet())
			for (PRVI25_SubductionFaultModels subductionFM : subductionBASols.keySet())
				branches.add(new LogicTreeBranch<>(levels, List.of(crustalFM, subductionFM)));
		
		LogicTree<?> tree = LogicTree.fromExisting(levels, branches);
		
		TrueMeanSolutionCreator creator = new TrueMeanSolutionCreator(tree);
		creator.setDoGridProv(gridded);
		for (LogicTreeBranch<?> branch : tree) {
			FaultSystemSolution crustalSol = crustalBASols.get(branch.requireValue(PRVI25_CrustalFaultModels.class));
			FaultSystemSolution subductionSol = subductionBASols.get(branch.requireValue(PRVI25_SubductionFaultModels.class));
			
			FaultSystemSolution combined = AbstractLogicTreeHazardCombiner.combineSols(crustalSol, subductionSol, true);
			if (gridded) {
				GridSourceList crustalGridded = crustalSol.requireModule(GridSourceList.class);
				GridSourceList subductionGridded = subductionSol.requireModule(GridSourceList.class);
				combined.setGridSourceProvider(GridSourceList.combine(subductionGridded, crustalGridded));
			}
			
			creator.addSolution(combined, branch);
		}
		
		FaultSystemSolution trueMean = creator.build();
		trueMean.write(outputFile);
	}

}
