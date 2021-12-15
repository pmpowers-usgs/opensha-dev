package scratch.nshm23.logicTree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.cli.CommandLine;
import org.opensha.commons.data.function.IntegerPDF_FunctionSampler;
import org.opensha.commons.logicTree.LogicTree;
import org.opensha.commons.logicTree.LogicTreeBranch;
import org.opensha.commons.logicTree.LogicTreeLevel;
import org.opensha.commons.logicTree.LogicTreeNode;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemRupSet;
import org.opensha.sha.earthquake.faultSysSolution.FaultSystemSolution;
import org.opensha.sha.earthquake.faultSysSolution.RuptureSets;
import org.opensha.sha.earthquake.faultSysSolution.RuptureSets.CoulombRupSetConfig;
import org.opensha.sha.earthquake.faultSysSolution.inversion.InversionConfiguration;
import org.opensha.sha.earthquake.faultSysSolution.inversion.Inversions;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.InversionConstraint;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.JumpProbabilityConstraint;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.JumpProbabilityConstraint.InitialModelParticipationRateEstimator;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.JumpProbabilityConstraint.RelativeRate;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.LaplacianSmoothingInversionConstraint;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.PaleoRateInversionConstraint;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.PaleoSlipInversionConstraint;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.impl.ParkfieldInversionConstraint;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.completion.CompletionCriteria;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.completion.TimeCompletionCriteria;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.params.GenerationFunctionType;
import org.opensha.sha.earthquake.faultSysSolution.inversion.sa.params.NonnegativityConstraintType;
import org.opensha.sha.earthquake.faultSysSolution.modules.PaleoseismicConstraintData;
import org.opensha.sha.earthquake.faultSysSolution.modules.SolutionLogicTree;
import org.opensha.sha.earthquake.faultSysSolution.modules.SolutionLogicTree.SolutionProcessor;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.PlausibilityConfiguration;
import org.opensha.sha.earthquake.faultSysSolution.util.FaultSysTools;

import scratch.UCERF3.enumTreeBranches.DeformationModels;
import scratch.UCERF3.enumTreeBranches.FaultModels;
import scratch.UCERF3.enumTreeBranches.ScalingRelationships;
import scratch.UCERF3.logicTree.U3LogicTreeBranch;
import scratch.UCERF3.logicTree.U3LogicTreeBranchNode;
import scratch.kevin.nshm23.InversionConfigurationFactory;
import scratch.nshm23.targetMFDs.DraftModelConstraintBuilder;
import scratch.nshm23.targetMFDs.SupraSeisBValInversionTargetMFDs;
import scratch.nshm23.targetMFDs.SupraSeisBValInversionTargetMFDs.SubSeisMoRateReduction;

public class DraftNSHM23InvConfigFactory implements InversionConfigurationFactory {
	
	protected FaultSystemRupSet buildGenericRupSet(LogicTreeBranch<?> branch, int threads) {
		return new RuptureSets.U3RupSetConfig(branch.requireValue(FaultModels.class),
					branch.requireValue(ScalingRelationships.class)).build(threads);
	}

	@Override
	public FaultSystemRupSet buildRuptureSet(LogicTreeBranch<?> branch, int threads) {
		// build empty-ish rup set without modules attached
		FaultSystemRupSet rupSet = buildGenericRupSet(branch, threads);
		
		return getSolutionLogicTreeProcessor().processRupSet(rupSet, branch);
	}
	
	private static U3LogicTreeBranch equivU3(LogicTreeBranch<?> branch) {
		U3LogicTreeBranch u3Branch = U3LogicTreeBranch.DEFAULT.copy();
		for (LogicTreeNode node : branch)
			if (node instanceof U3LogicTreeBranchNode<?>)
				u3Branch.setValue((U3LogicTreeBranchNode<?>)node);
		return u3Branch;
	}

	@Override
	public SolutionProcessor getSolutionLogicTreeProcessor() {
		return InversionConfigurationFactory.super.getSolutionLogicTreeProcessor();
	}
	
	private static class DraftNSHM23SolProcessor implements SolutionProcessor {

		@Override
		public FaultSystemRupSet processRupSet(FaultSystemRupSet rupSet, LogicTreeBranch<?> branch) {
			// create equivalent U3 branch
			U3LogicTreeBranch u3Branch = equivU3(branch);
			
			System.out.println("Equivalent U3 branch: "+u3Branch);
			// attach U3 modules
			rupSet = FaultSystemRupSet.buildFromExisting(rupSet).forU3Branch(u3Branch).build();
			rupSet.addModule(branch);
			return rupSet;
		}

		@Override
		public FaultSystemSolution processSolution(FaultSystemSolution sol, LogicTreeBranch<?> branch) {
			return sol;
		}
		
	}

	@Override
	public InversionConfiguration buildInversionConfig(FaultSystemRupSet rupSet, LogicTreeBranch<?> branch,
			CommandLine cmd, int threads) {
		double bVal = branch.requireValue(SupraSeisBValue.class).bValue;
		DraftModelConstraintBuilder constrBuilder = new DraftModelConstraintBuilder(rupSet, bVal,
				true, false, true);
		
		SubSeisMoRateReduction reduction = SupraSeisBValInversionTargetMFDs.SUB_SEIS_MO_RATE_REDUCTION_DEFAULT;
		if (branch.hasValue(SubSeisMoRateReductionNode.class))
			reduction = branch.getValue(SubSeisMoRateReductionNode.class).getChoice();
		
		constrBuilder.subSeisMoRateReduction(reduction);
		
		SubSectConstraintModel constrModel = branch.requireValue(SubSectConstraintModel.class);
		
		double slipWeight = 1d;
		double paleoWeight = 5;
		double parkWeight = 100;
		double mfdWeight = 10;
		double nuclWeight = constrModel == SubSectConstraintModel.TOT_NUCL_RATE ? 0.5 : 0d;
		double nuclMFDWeight = constrModel == SubSectConstraintModel.NUCL_MFD ? 0.1 : 0d;
		double paleoSmoothWeight = paleoWeight > 0 ? 10000 : 0;
		
		constrBuilder.magDepRelStdDev(M->0.1*Math.pow(10, bVal*0.5*(M-6)));
		
		if (slipWeight > 0d)
			constrBuilder.slipRates().weight(slipWeight);
		
		if (paleoWeight > 0d) {
			constrBuilder.paleoRates().weight(paleoWeight);
			constrBuilder.paleoSlips().weight(paleoWeight);
		}
		
		if (parkWeight > 0d)
			constrBuilder.parkfield().weight(parkWeight);
		
		if (mfdWeight > 0d)
			constrBuilder.supraBValMFDs().weight(mfdWeight);
		
		if (nuclWeight > 0d)
			constrBuilder.sectSupraRates().weight(nuclWeight);
		
		if (nuclMFDWeight > 0d)
			constrBuilder.sectSupraNuclMFDs().weight(nuclMFDWeight);
		
		if (paleoSmoothWeight > 0d)
			constrBuilder.supraPaleoSmooth().weight(paleoSmoothWeight);
		
		IntegerPDF_FunctionSampler sampler = constrBuilder.getSkipBelowMinSampler();
		
		List<InversionConstraint> constraints = constrBuilder.build();
		
		SegmentationModel segModel = branch.getValue(SegmentationModel.class);
		if (segModel != null && segModel != SegmentationModel.NONE) {
			constraints = new ArrayList<>(constraints);
			
			InitialModelParticipationRateEstimator rateEst = new InitialModelParticipationRateEstimator(
					rupSet, Inversions.getDefaultVariablePerturbationBasis(rupSet));

//			double weight = 0.5d;
//			boolean ineq = false;
			double weight = 1d;
			boolean ineq = true;
			
			constraints.add(new JumpProbabilityConstraint.RelativeRate(
					weight, ineq, rupSet, segModel.getModel(rupSet), rateEst));
		}
		
		int avgThreads = threads / 4;
		
		CompletionCriteria completion;
		if (constrModel == SubSectConstraintModel.NUCL_MFD)
			completion = TimeCompletionCriteria.getInHours(5l);
		else
			completion = TimeCompletionCriteria.getInHours(2l);
		
		InversionConfiguration.Builder builder = InversionConfiguration.builder(constraints, completion)
				.threads(threads)
				.avgThreads(avgThreads, TimeCompletionCriteria.getInMinutes(5l))
				.perturbation(GenerationFunctionType.VARIABLE_EXPONENTIAL_SCALE)
				.nonNegativity(NonnegativityConstraintType.TRY_ZERO_RATES_OFTEN)
				.forCommandLine(cmd).sampler(sampler);
		
		return builder.build();
	}
	
	public static class NoPaleoParkfield extends DraftNSHM23InvConfigFactory {

		@Override
		public InversionConfiguration buildInversionConfig(FaultSystemRupSet rupSet, LogicTreeBranch<?> branch,
				CommandLine cmd, int threads) {
			InversionConfiguration config = super.buildInversionConfig(rupSet, branch, cmd, threads);
			return InversionConfiguration.builder(config).except(PaleoRateInversionConstraint.class)
				.except(PaleoSlipInversionConstraint.class).except(ParkfieldInversionConstraint.class)
				.except(LaplacianSmoothingInversionConstraint.class).build();
		}
		
	}
	
	public static class CoulombRupSet extends DraftNSHM23InvConfigFactory {
		
		Map<FaultModels, FaultSystemRupSet> rupSetCache = new HashMap<>();

		@Override
		protected synchronized FaultSystemRupSet buildGenericRupSet(LogicTreeBranch<?> branch, int threads) {
			FaultModels fm = branch.requireValue(FaultModels.class);
			// check cache
			FaultSystemRupSet rupSet = rupSetCache.get(fm);
			if (rupSet != null)
				return rupSet;
			// need to build one
			rupSet = new RuptureSets.CoulombRupSetConfig(fm,
					branch.requireValue(ScalingRelationships.class)).build(threads);
			// cache it
			rupSetCache.put(fm, rupSet);
			return rupSet;
		}

		@Override
		public SolutionProcessor getSolutionLogicTreeProcessor() {
			return new DraftCoulombNSHM23SolProcessor();
		}
		
	}
	
	private static class DraftCoulombNSHM23SolProcessor extends DraftNSHM23SolProcessor {
		
		Map<FaultModels, PlausibilityConfiguration> configCache = new HashMap<>();

		@Override
		public FaultSystemRupSet processRupSet(FaultSystemRupSet rupSet, LogicTreeBranch<?> branch) {
			rupSet = super.processRupSet(rupSet, branch);
			if (!rupSet.hasModule(PlausibilityConfiguration.class)) {
				// for branch averaging
				rupSet.addAvailableModule(new Callable<PlausibilityConfiguration>() {

					@Override
					public PlausibilityConfiguration call() throws Exception {
						FaultModels fm = branch.requireValue(FaultModels.class);
						PlausibilityConfiguration config;
						synchronized (configCache) {
							config = configCache.get(fm);
							if (config == null) {
								config = new RuptureSets.CoulombRupSetConfig(fm,
										branch.requireValue(ScalingRelationships.class)).getPlausibilityConfig();
								configCache.put(fm, config);
							}
						}
						return config;
					}
				}, PlausibilityConfiguration.class);
				
			}
			return rupSet;
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		File dir = new File("/home/kevin/OpenSHA/UCERF4/batch_inversions/"
				+ "2021_11_24-nshm23_draft_branches-FM3_1/");
//				+ "2021_11_30-nshm23_draft_branches-FM3_1-FaultSpec");
		File ltFile = new File(dir, "results.zip");
		SolutionLogicTree tree = SolutionLogicTree.load(ltFile);
		
		FaultSystemSolution ba = tree.calcBranchAveraged();
		
		ba.write(new File(dir, "branch_averaged.zip"));
	}

}
