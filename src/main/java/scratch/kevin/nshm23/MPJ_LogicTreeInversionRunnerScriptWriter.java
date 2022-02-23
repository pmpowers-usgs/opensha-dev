package scratch.kevin.nshm23;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.opensha.commons.hpc.JavaShellScriptWriter;
import org.opensha.commons.hpc.mpj.FastMPJShellScriptWriter;
import org.opensha.commons.hpc.mpj.MPJExpressShellScriptWriter;
import org.opensha.commons.hpc.pbs.BatchScriptWriter;
import org.opensha.commons.hpc.pbs.StampedeScriptWriter;
import org.opensha.commons.hpc.pbs.USC_CARC_ScriptWriter;
import org.opensha.commons.logicTree.LogicTree;
import org.opensha.commons.logicTree.LogicTreeBranch;
import org.opensha.commons.logicTree.LogicTreeLevel;
import org.opensha.commons.logicTree.LogicTreeNode;
import org.opensha.sha.earthquake.faultSysSolution.inversion.InversionConfigurationFactory;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.completion.CompletionCriteria;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.completion.TimeCompletionCriteria;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.params.GenerationFunctionType;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.params.NonnegativityConstraintType;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.NSHM23_InvConfigFactory;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.MaxJumpDistModels;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_LogicTreeBranch;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.NSHM23_U3_HybridLogicTreeBranch;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.RupturePlausibilityModels;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.SegmentationMFD_Adjustment;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.SegmentationModels;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.SubSectConstraintModels;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.SubSeisMoRateReductions;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.SupraSeisBValues;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.logicTree.U3_UncertAddDeformationModels;

import com.google.common.base.Preconditions;

import edu.usc.kmilner.mpj.taskDispatch.MPJTaskCalculator;
import scratch.UCERF3.enumTreeBranches.DeformationModels;
import scratch.UCERF3.enumTreeBranches.FaultModels;
import scratch.UCERF3.enumTreeBranches.ScalingRelationships;
import scratch.UCERF3.enumTreeBranches.SlipAlongRuptureModels;
import scratch.UCERF3.enumTreeBranches.TotalMag5Rate;
import scratch.UCERF3.inversion.U3InversionConfigFactory;
import scratch.UCERF3.logicTree.U3LogicTreeBranch;
import scratch.UCERF3.logicTree.U3LogicTreeBranchNode;

public class MPJ_LogicTreeInversionRunnerScriptWriter {
	
	public static void main(String[] args) throws IOException {
		File localMainDir = new File("/home/kevin/OpenSHA/UCERF4/batch_inversions");
		
		List<String> extraArgs = new ArrayList<>();
		
		File remoteMainDir = new File("/project/scec_608/kmilner/nshm23/batch_inversions");
		int remoteToalThreads = 20;
		int remoteInversionsPerBundle = 1;
		int remoteTotalMemGB = 53;
		String queue = "scec";
		int nodes = 36;
		double itersPerSec = 200000;
		int runsPerBranch = 1;
//		JavaShellScriptWriter mpjWrite = new MPJExpressShellScriptWriter(
//				USC_CARC_ScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, USC_CARC_ScriptWriter.MPJ_HOME);
		JavaShellScriptWriter mpjWrite = new FastMPJShellScriptWriter(
				USC_CARC_ScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, USC_CARC_ScriptWriter.FMPJ_HOME);
		BatchScriptWriter pbsWrite = new USC_CARC_ScriptWriter();
		
//		File remoteMainDir = new File("/work/00950/kevinm/stampede2/nshm23/batch_inversions");
//		int remoteToalThreads = 48;
//		int remoteInversionsPerBundle = 3;
//		int remoteTotalMemGB = 100;
//		String queue = "skx-normal";
//		int nodes = 50;
//		double itersPerSec = 200000;
//		int runsPerBranch = 1;
////		String queue = "skx-dev";
////		int nodes = 4;
//		JavaShellScriptWriter mpjWrite = new FastMPJShellScriptWriter(
//				StampedeScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, StampedeScriptWriter.FMPJ_HOME);
//		BatchScriptWriter pbsWrite = new StampedeScriptWriter();

		String dirName = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
//		String dirName = "2022_02_17";
		
		/*
		 * UCERF3 logic tree
		 */
//		LogicTree<U3LogicTreeBranchNode<?>> logicTree = LogicTree.buildExhaustive(
//				U3LogicTreeBranch.getLogicTreeLevels(), true);
//		dirName += "-u3_branches";
//		
////		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.class;
////		int avgNumRups = 250000;
//		
////		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.OriginalCalcParamsNewAvg.class;
////		dirName += "-orig_calc_params-new_avg-converged";
////		int avgNumRups = 250000;
//		
//		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.OriginalCalcParamsNewAvgNoWaterLevel.class;
//		dirName += "-orig_calc_params-new_avg-converged-noWL";
//		int avgNumRups = 250000;
//		
//		dirName += "-new_perturb";
//		extraArgs.add("--perturb "+GenerationFunctionType.VARIABLE_EXPONENTIAL_SCALE.name());
//		
////		dirName += "-try_zero_often";
////		extraArgs.add("--non-negativity "+NonnegativityConstraintType.TRY_ZERO_RATES_OFTEN.name());
//		
////		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.ThinnedRupSet.class;
////		int avgNumRups = 150000;
////		dirName += "-thinned_0.1";
//		
////		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.OriginalCalcParams.class;
////		dirName += "-orig_calc_params";
////		int avgNumRups = 250000;
////		remoteInversionsPerBundle = 4;
////		runsPerBranch = 10;
////		String completionArg = null;
////		int invMins = 30;
//		
////		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.NoPaleoParkfieldSingleReg.class;
////		dirName += "-no_paleo-no_parkfield-single_mfd_reg";
//		
////		Class<? extends InversionConfigurationFactory> factoryClass = U3InversionConfigFactory.CoulombRupSet.class;
////		dirName += "-coulomb";
//		
////		U3LogicTreeBranchNode<?>[] required = { FaultModels.FM3_1, DeformationModels.GEOLOGIC,
////				ScalingRelationships.SHAW_2009_MOD, TotalMag5Rate.RATE_7p9 };
////		U3LogicTreeBranchNode<?>[] required = { FaultModels.FM3_1, DeformationModels.ZENGBB,
////				ScalingRelationships.SHAW_2009_MOD };
//		U3LogicTreeBranchNode<?>[] required = { FaultModels.FM3_1 };
////		U3LogicTreeBranchNode<?>[] required = {  };
//		Class<? extends LogicTreeNode> sortBy = null;
		/*
		 * END UCERF3 logic tree
		 */

		/*
		 * NSHM23 logic tree
		 */
		List<LogicTreeLevel<? extends LogicTreeNode>> levels = NSHM23_U3_HybridLogicTreeBranch.levels;
		dirName += "-nshm23_u3_hybrid_branches";
//		List<LogicTreeLevel<? extends LogicTreeNode>> levels = NSHM23_LogicTreeBranch.levels;
//		dirName += "-nshm23_branches";
		
		levels = new ArrayList<>(levels);
		for (int i=levels.size(); --i>=0;)
			if (levels.get(i).getType().isAssignableFrom(SegmentationModels.class)
					|| levels.get(i).getType().isAssignableFrom(SegmentationMFD_Adjustment.class))
				levels.remove(i);
		dirName += "-no_seg";
//		levels.add(LogicTreeLevel.forEnum(MaxJumpDistModels.class, "Max Dist Segmentation", "MaxDist"));
//		dirName += "-max_dist";
		
//		dirName += "-reweight_seg_2_3_4";
			
		LogicTree<LogicTreeNode> logicTree = LogicTree.buildExhaustive(levels, true);
		
		Class<? extends InversionConfigurationFactory> factoryClass = NSHM23_InvConfigFactory.class;
		
//		Class<? extends InversionConfigurationFactory> factoryClass = NSHM23_InvConfigFactory.HardcodedPrevWeightAdjust.class;
//		dirName += "-no_reweight_use_prev";
		
//		Class<? extends InversionConfigurationFactory> factoryClass = NSHM23_InvConfigFactory.NoMFDScaleAdjust.class;
//		dirName += "-no_scale_adj_mfds";
		
		LogicTreeNode[] required = {
				FaultModels.FM3_1,
				RupturePlausibilityModels.COULOMB,
//				RupturePlausibilityModels.UCERF3,
//				RupturePlausibilityModels.UCERF3_REDUCED,
//				U3_UncertAddDeformationModels.U3_ZENG,
//				ScalingRelationships.SHAW_2009_MOD,
				SlipAlongRuptureModels.UNIFORM,
				SubSectConstraintModels.TOT_NUCL_RATE,
				SubSeisMoRateReductions.SUB_B_1,
//				SupraSeisBValues.B_0p8,
//				SegmentationModels.SHAW_R0_3,
//				SegmentationMFD_Adjustment.JUMP_PROB_THRESHOLD_AVG
//				SegmentationMFD_Adjustment.CAPPED_REDIST
//				SegmentationMFD_Adjustment.CAPPED_REDIST_SELF_CONTAINED
//				SegmentationMFD_Adjustment.GREEDY
//				SegmentationMFD_Adjustment.GREEDY_SELF_CONTAINED
//				SegmentationMFD_Adjustment.JUMP_PROB_THRESHOLD_AVG_MATCH_STRICT
				};
		double avgNumRups = 300000;
//		LogicTreeNode[] required = { FaultModels.FM3_1, SubSeisMoRateReductionNode.SYSTEM_AVG };
//		LogicTreeNode[] required = { FaultModels.FM3_1, SubSeisMoRateReductionNode.FAULT_SPECIFIC };
		Class<? extends LogicTreeNode> sortBy = SubSectConstraintModels.class;
		/*
		 * END NSHM23 logic tree
		 */
		
		int rounds = 2000;
//		int rounds = 5000;
//		int rounds = 10000;
		double numIters = avgNumRups*rounds;
		double invSecs = numIters/itersPerSec;
		int invMins = (int)(invSecs/60d + 0.5);
		String completionArg = rounds+"ip";
		System.out.println("Estimate "+invMins+" minues per inversion");
		
//		String completionArg = "1m"; int invMins = 1;
//		String completionArg = "10m"; int invMins = 10;
//		String completionArg = "30m"; int invMins = 30;
//		String completionArg = "2h"; int invMins = 2*60;
//		String completionArg = "5h"; int invMins = 5*60;
//		String completionArg = null; int invMins = defaultInvMins;
		
//		int numSamples = nodes*5;
//		int numSamples = nodes*4;
//		long randSeed = System.currentTimeMillis();
		long randSeed = 12345678l;
		int numSamples = 0;
		
		if (required != null && required.length > 0) {
			for (LogicTreeNode node : required)
				dirName += "-"+node.getFilePrefix();
			logicTree = logicTree.matchingAll(required);
		}
		
		if (numSamples > 0) {
			if (numSamples < logicTree.size()) {
				System.out.println("Reducing tree of size "+logicTree.size()+" to "+numSamples+" samples");
				dirName += "-"+numSamples+"_samples";
				logicTree = logicTree.sample(numSamples, false, new Random(randSeed));
			} else {
				System.out.println("Won't sample logic tree, as tree has "+logicTree.size()+" values, which is fewer "
						+ "than the specified "+numSamples+" samples.");
			}
		}
		
		if (sortBy != null) {
			Comparator<LogicTreeBranch<?>> groupingComparator = new Comparator<LogicTreeBranch<?>>() {

				@SuppressWarnings("unchecked")
				@Override
				public int compare(LogicTreeBranch<?> o1, LogicTreeBranch<?> o2) {
					LogicTreeNode v1 = o1.getValue(sortBy);
					LogicTreeNode v2 = o2.getValue(sortBy);
					if (v1.equals(v2))
						return ((LogicTreeBranch<LogicTreeNode>)o1).compareTo((LogicTreeBranch<LogicTreeNode>)o2);
					
					return v1.getShortName().compareTo(v2.getShortName());
				}
			};
			logicTree = logicTree.sorted(groupingComparator);
		}
		
		System.out.println("Built "+logicTree.size()+" branches");
		
		int origNodes = nodes;
		nodes = Integer.min(nodes, logicTree.size());
		
		int numCalcs = logicTree.size()*runsPerBranch;
		int nodeRounds = (int)Math.ceil((double)numCalcs/(double)(nodes*remoteInversionsPerBundle));
		double calcNodes = Math.ceil((double)numCalcs/(double)(nodeRounds*remoteInversionsPerBundle));
		System.out.println("Implies "+(float)calcNodes+" nodes for "+nodeRounds+" rounds");
		nodes = Integer.min(nodes, (int)calcNodes);
		if (origNodes != nodes)
			System.out.println("Adjusted "+origNodes+" to "+nodes+" nodes to evenly divide "+numCalcs+" calcs");
		
		if (completionArg != null)
			dirName += "-"+completionArg;
		
		File localDir = new File(localMainDir, dirName);
		Preconditions.checkState(localDir.exists() || localDir.mkdir());
		File remoteDir = new File(remoteMainDir, dirName);
		
		List<File> classpath = new ArrayList<>();
		classpath.add(new File(remoteDir, "opensha-dev-all.jar"));
		
		File localLogicTree = new File(localDir, "logic_tree.json");
		logicTree.write(localLogicTree);
		File remoteLogicTree = new File(remoteDir, localLogicTree.getName());
		
		mpjWrite.setClasspath(classpath);
		if (mpjWrite instanceof MPJExpressShellScriptWriter)
			((MPJExpressShellScriptWriter)mpjWrite).setUseLaunchWrapper(true);
		else if (mpjWrite instanceof FastMPJShellScriptWriter)
			((FastMPJShellScriptWriter)mpjWrite).setUseLaunchWrapper(true);
		
		int annealingThreads = remoteToalThreads/remoteInversionsPerBundle;
		
		File resultsDir = new File(remoteDir, "results");
		String argz = "--logic-tree "+remoteLogicTree.getAbsolutePath();
		argz += " --output-dir "+resultsDir.getAbsolutePath();
		argz += " --inversion-factory '"+factoryClass.getName()+"'"; // surround in single quotes to escape $'s
		argz += " --annealing-threads "+annealingThreads;
		argz += " --cache-dir "+new File(remoteMainDir, "cache").getAbsolutePath();
		if (remoteInversionsPerBundle > 0)
			argz += " --runs-per-bundle "+remoteInversionsPerBundle;
		if (completionArg != null)
			argz += " --completion "+completionArg;
		if (runsPerBranch > 1)
			argz += " --runs-per-branch "+runsPerBranch;
		for (String arg : extraArgs)
			argz += " "+arg;
		argz += " "+MPJTaskCalculator.argumentBuilder().exactDispatch(remoteInversionsPerBundle).build();
		List<String> script = mpjWrite.buildScript(MPJ_LogicTreeInversionRunner.class.getName(), argz);
		
		int mins = (int)(nodeRounds*invMins);
		mins += Integer.max(60, invMins);
		mins += (nodeRounds-1)*10; // add a little extra for overhead associated with each round
		System.out.println("Total job time: "+mins+" mins = "+(float)((double)mins/60d)+" hours");
		
		pbsWrite.writeScript(new File(localDir, "batch_inversion.slurm"), script, mins, nodes, remoteToalThreads, queue);
		
		// now write hazard script
		argz = "--input-file "+new File(resultsDir.getAbsolutePath()+".zip");
		argz += " --output-dir "+resultsDir.getAbsolutePath();
		argz += " "+MPJTaskCalculator.argumentBuilder().exactDispatch(1).threads(remoteToalThreads).build();
		script = mpjWrite.buildScript(MPJ_LogicTreeHazardCalc.class.getName(), argz);
		
		mins = (int)60*10;
		nodes = Integer.min(40, nodes);
		if (queue != null && queue.equals("scec"))
			// run hazard in the high priority queue
			queue = "scec_hiprio";
		pbsWrite.writeScript(new File(localDir, "batch_hazard.slurm"), script, mins, nodes, remoteToalThreads, queue);
	}

}
