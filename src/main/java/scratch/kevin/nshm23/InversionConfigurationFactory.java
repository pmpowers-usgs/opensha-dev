package scratch.kevin.nshm23;

import org.apache.commons.cli.CommandLine;
import org.opensha.commons.logicTree.LogicTreeBranch;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemRupSet;
import org.opensha.sha.earthquake.faultSysSolution.inversion.InversionConfiguration;
import org.opensha.sha.earthquake.faultSysSolution.modules.SolutionLogicTree.SolutionProcessor;

public interface InversionConfigurationFactory {
	
	public FaultSystemRupSet buildRuptureSet(LogicTreeBranch<?> branch, int threads);
	
	public InversionConfiguration buildInversionConfig(FaultSystemRupSet rupSet, LogicTreeBranch<?> branch,
			CommandLine cmd, int threads);
	
	public default SolutionProcessor getSolutionLogicTreeProcessor() {
		return null;
	};

}