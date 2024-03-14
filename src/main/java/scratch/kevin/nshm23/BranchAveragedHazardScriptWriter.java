package scratch.kevin.nshm23;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opensha.commons.geo.GriddedRegion;
import org.opensha.commons.geo.json.Feature;
import org.opensha.commons.hpc.JavaShellScriptWriter;
import org.opensha.commons.hpc.mpj.FastMPJShellScriptWriter;
import org.opensha.commons.hpc.mpj.NoMPJSingleNodeShellScriptWriter;
import org.opensha.commons.hpc.pbs.BatchScriptWriter;
import org.opensha.commons.hpc.pbs.HovenweepScriptWriter;
import org.opensha.commons.hpc.pbs.USC_CARC_ScriptWriter;
import org.opensha.sha.earthquake.faultSysSolution.hazard.mpj.MPJ_SingleSolHazardCalc;
import org.opensha.sha.earthquake.param.IncludeBackgroundOption;
import org.opensha.sha.earthquake.rupForecastImpl.nshm23.util.NSHM23_RegionLoader;

import com.google.common.base.Preconditions;

import edu.usc.kmilner.mpj.taskDispatch.MPJTaskCalculator;

public class BranchAveragedHazardScriptWriter {

	public static void main(String[] args) throws IOException {
		String baseDirName = "2024_02_02-nshm23_branches-WUS_FM_v3";
		
//		String suffix = "true_mean";
//		String solFileName = "true_mean_solution.zip";
		
		String suffix = "ba_only";
		String solFileName = "results_WUS_FM_v3_branch_averaged_gridded.zip";
		
		boolean noMFDs = false;
		
		GriddedRegion gridReg = new GriddedRegion(
				NSHM23_RegionLoader.loadFullConterminousWUS(), 0.1, GriddedRegion.ANCHOR_0_0);
		
		IncludeBackgroundOption[] bgOps = IncludeBackgroundOption.values();
		
		String dirName = baseDirName+"-"+suffix;
		if (noMFDs)
			dirName += "-no_mfds";
		
		File localMainDir = new File("/home/kevin/OpenSHA/UCERF4/batch_inversions");
		File localDir = new File(localMainDir, dirName);
		Preconditions.checkState(localDir.exists() || localDir.mkdir());
		
//		File remoteMainDir = new File("/project/scec_608/kmilner/nshm23/batch_inversions");
//		int remoteTotalThreads = 20;
//		int remoteTotalMemGB = 50;
//		String queue = "scec";
//		int nodes = 36;
//		int mins = 600;
////		int nodes = 18;
////		JavaShellScriptWriter mpjWrite = new MPJExpressShellScriptWriter(
////				USC_CARC_ScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, USC_CARC_ScriptWriter.MPJ_HOME);
//		JavaShellScriptWriter parallelMPJWrite = new FastMPJShellScriptWriter(
//				USC_CARC_ScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, USC_CARC_ScriptWriter.FMPJ_HOME);
//		JavaShellScriptWriter singleMPJWrite = new NoMPJSingleNodeShellScriptWriter(USC_CARC_ScriptWriter.JAVA_BIN,
//				remoteTotalMemGB*1024, null);
//		BatchScriptWriter pbsWrite = new USC_CARC_ScriptWriter();
		
		File remoteMainDir = new File("/caldera/hovenweep/projects/usgs/hazards/ehp/kmilner/nshm23/batch_inversions");
		int remoteTotalThreads = 128;
		int remoteTotalMemGB = 448;
		String queue = null;
		int nodes = 4;
		int mins = 180;
//		int nodes = 18;
//		JavaShellScriptWriter mpjWrite = new MPJExpressShellScriptWriter(
//				USC_CARC_ScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, USC_CARC_ScriptWriter.MPJ_HOME);
		JavaShellScriptWriter parallelMPJWrite = new FastMPJShellScriptWriter(
				HovenweepScriptWriter.JAVA_BIN, remoteTotalMemGB*1024, null, HovenweepScriptWriter.FMPJ_HOME);
		JavaShellScriptWriter singleMPJWrite = new NoMPJSingleNodeShellScriptWriter(HovenweepScriptWriter.JAVA_BIN,
				remoteTotalMemGB*1024, null);
		BatchScriptWriter pbsWrite = new HovenweepScriptWriter();
		
		parallelMPJWrite.setEnvVar("MAIN_DIR", remoteMainDir.getAbsolutePath());
		singleMPJWrite.setEnvVar("MAIN_DIR", remoteMainDir.getAbsolutePath());
		String mainDirPath = "$MAIN_DIR";
		parallelMPJWrite.setEnvVar("DIR", mainDirPath+"/"+dirName);
		singleMPJWrite.setEnvVar("DIR", mainDirPath+"/"+dirName);
		String dirPath = "$DIR";
		
		List<File> classpath = new ArrayList<>();
		classpath.add(new File(dirPath+"/opensha-dev-all.jar"));
		parallelMPJWrite.setClasspath(classpath);
		
		List<File> singleClasspath = new ArrayList<>(classpath);
		singleClasspath.add(new File("/project/scec_608/kmilner/git/opensha/lib/mpj-0.38.jar"));
		singleMPJWrite.setClasspath(singleClasspath);

		// write the region
		File localReg = new File(localDir, "gridded_region.json");
		Feature.write(gridReg.toFeature(), localReg);
		
		String resultsPath = dirPath+"/results";
		String regPath = dirPath+"/"+localReg.getName();
		
		String solFilePath = "$DIR/"+solFileName;
		
		List<String> setupLines = new ArrayList<>();
		setupLines.add("if [[ ! -e "+solFilePath+" ]];then");
		setupLines.add("  ln -s $MAIN_DIR/"+baseDirName+"/"+solFileName+" "+solFilePath);
		setupLines.add("fi");
		parallelMPJWrite.setCustomSetupLines(setupLines);
		singleMPJWrite.setCustomSetupLines(setupLines);
		
		
		for (IncludeBackgroundOption bgOp : bgOps) {
			int myNodes;
			JavaShellScriptWriter mpjWrite;
			String dispatchArgs;
			if (bgOp == IncludeBackgroundOption.INCLUDE && bgOps.length == 3) {
				// do single node
				myNodes = 1;
				mpjWrite = singleMPJWrite;
				dispatchArgs = MPJTaskCalculator.argumentBuilder().exactDispatch(gridReg.getNodeCount()).threads(remoteTotalThreads).build();
			} else {
				// do parallel
				myNodes = nodes;
				mpjWrite = parallelMPJWrite;
				
				int maxDispatch;
				if (gridReg.getNodeCount() > 50000)
					maxDispatch = Integer.max(remoteTotalThreads*20, 1000);
				else if (gridReg.getNodeCount() > 10000)
					maxDispatch = Integer.max(remoteTotalThreads*10, 500);
				else if (gridReg.getNodeCount() > 5000)
					maxDispatch = remoteTotalThreads*5;
				else
					maxDispatch = remoteTotalThreads*3;
				dispatchArgs = MPJTaskCalculator.argumentBuilder().minDispatch(remoteTotalThreads)
						.maxDispatch(maxDispatch).threads(remoteTotalThreads).build();
			}
			
			String argz = "--input-file "+dirPath+"/"+solFileName;
			argz += " --output-dir "+resultsPath;
			argz += " --output-file "+resultsPath+"_hazard_"+bgOp.name()+".zip";
			argz += " --region "+regPath;
			if (noMFDs)
				argz += " --no-mfds";
			argz += " --gridded-seis "+bgOp.name();
			argz += " "+dispatchArgs;
			
			File jobFile = new File(localDir, "batch_hazard_"+bgOp.name()+".slurm");
			
			List<String> script = mpjWrite.buildScript(MPJ_SingleSolHazardCalc.class.getName(), argz);
			
			System.out.println("Writing "+jobFile.getAbsolutePath());
			
			pbsWrite.writeScript(jobFile, script, mins, myNodes, remoteTotalThreads, queue);
		}
	}

}