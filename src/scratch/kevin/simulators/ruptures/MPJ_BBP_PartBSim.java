package scratch.kevin.simulators.ruptures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.opensha.commons.geo.Location;
import org.opensha.sha.simulators.RSQSimEvent;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.usc.kmilner.mpj.taskDispatch.MPJTaskCalculator;
import oracle.net.aso.i;
import scratch.kevin.bbp.BBP_Site;
import scratch.kevin.bbp.MPJ_BBP_Utils;
import scratch.kevin.simulators.ruptures.BBP_PartBValidationConfig.Scenario;

public class MPJ_BBP_PartBSim extends AbstractMPJ_BBP_MultiRupSim {
	
	private final int numSites;
	private final boolean randomAz;
	
	private List<File> runDirs;
	private List<RSQSimEvent> events;
	private List<List<Integer>> siteIndexes;
	private List<Double> siteDists;
	private Table<RSQSimEvent, Double, List<BBP_Site>> siteListCache;
	
	private static final int MAX_SITES_PER_SIM = 10;

	private MPJ_BBP_PartBSim(CommandLine cmd) throws IOException {
		super(cmd);
		
		int skipYears = -1;
		if (cmd.hasOption("skip-years"))
			skipYears = Integer.parseInt(cmd.getOptionValue("skip-years"));
		
		numSites = Integer.parseInt(cmd.getOptionValue("num-sites"));
		Preconditions.checkState(numSites > 0);
		
		randomAz = cmd.hasOption("random-azimuth");
		
		runDirs = new ArrayList<>();
		events = new ArrayList<>();
		siteIndexes = new ArrayList<>();
		siteDists = new ArrayList<>();
		siteListCache = HashBasedTable.create();
		
		double[] distances = getDistances(cmd);
		Scenario[] scenarios = getScenarios(cmd);
		
		for (Scenario scenario : scenarios) {
			if (rank == 0)
				debug("Loading matches for scenario: "+scenario);
			
			List<RSQSimEvent> eventMatches = scenario.getMatches(catalog, skipYears);
			
			if (rank == 0)
				debug("Loaded "+eventMatches.size()+" matches for scenario: "+scenario);
			
			if (eventMatches.isEmpty()) {
				if (rank == 0)
					debug("skipping...");
				continue;
			}
			
			File scenarioDir = new File(resultsDir, scenario.getPrefix());
			
			if (rank == 0)
				MPJ_BBP_Utils.waitOnDir(scenarioDir, 10, 2000);
			
			if (rank == 0)
				debug("Creating site lists with "+numSites+" sites per event, "+distances.length+" distances");
			for (RSQSimEvent event : eventMatches) {
				File eventDir = new File (scenarioDir, "event_"+event.getID());
				if (rank == 0)
					MPJ_BBP_Utils.waitOnDir(eventDir, 10, 2000);
				for (double distance : distances) {
					List<List<Integer>> siteBundles = new ArrayList<>();
					List<Integer> curSites = null;
					for (int i=0; i<numSites; i++) {
						if (curSites == null) {
							curSites = new ArrayList<>();
						} else if (curSites.size() == MAX_SITES_PER_SIM) {
							siteBundles.add(curSites);
							curSites = new ArrayList<>();
						}
						curSites.add(i);
					}
					if (curSites != null)
						siteBundles.add(curSites);
					for (List<Integer> siteBundle : siteBundles) {
						File bundleDir = new File(eventDir, "bundle_s"+siteBundle.get(0)+"_s"+siteBundle.get(siteBundle.size()-1));
						if (rank == 0)
							MPJ_BBP_Utils.waitOnDir(bundleDir, 10, 2000);
						String dirName = getDirName(scenario, event.getID(), distance);
						File runDir = new File(bundleDir, dirName);
						
						runDirs.add(runDir);
						events.add(event);
						siteIndexes.add(siteBundle);
						siteDists.add(distance);
					}
				}
			}
			
			if (rank == 0) {
			}
		}
	}
	
	static String getDirName(Scenario scenario, int eventID, double distance) {
		return scenario.getPrefix()+"_event_"+eventID+"_dist_"+(float)distance;
	}

	@Override
	RSQSimEvent eventForIndex(int index) {
		return events.get(index);
	}

	@Override
	synchronized List<BBP_Site> sitesForIndex(int index) {
		RSQSimEvent event = eventForIndex(index);
		double dist = siteDists.get(index);
		List<BBP_Site> sites = siteListCache.get(event, dist);
		if (sites == null) {
			// need to build site list
			Location[] siteLocs = BBP_PartBValidationConfig.selectSitesSites(
					numSites, dist, randomAz, catalog, event);
			sites = new ArrayList<>();
			for (int i=0; i<numSites; i++)
				sites.add(new BBP_Site("s"+i, siteLocs[i], vm.getVs30(),
						RSQSimBBP_Config.SITE_LO_PASS_FREQ, RSQSimBBP_Config.SITE_HI_PASS_FREQ));
			siteListCache.put(event, dist, sites);
		}
		List<BBP_Site> ret = new ArrayList<>();
		for (Integer siteIndex : siteIndexes.get(index))
			ret.add(sites.get(siteIndex));
		return ret;
	}

	@Override
	File runDirForIndex(int index) {
		return runDirs.get(index);
	}

	@Override
	protected int getNumTasks() {
		return events.size();
	}
	
	static void addPartB_ScenarioOptions(Options ops) {
		Option scenarios = new Option("scen", "scenarios", true, "BBP Part B Scenario names (comma separated). Default is all");
		scenarios.setRequired(false);
		ops.addOption(scenarios);
		
		Option distances = new Option("d", "distances", true, "Distances to consider");
		distances.setRequired(false);
		ops.addOption(distances);
	}
	
	static Scenario[] getScenarios(CommandLine cmd) {
		if (cmd.hasOption("scenarios")) {
			String str = cmd.getOptionValue("scenarios");
			String[] strs = str.split(",");
			Scenario[] scenarios = new Scenario[strs.length];
			for (int i=0; i<strs.length; i++)
				scenarios[i] = Scenario.valueOf(strs[i]);
			return scenarios;
		}
		return Scenario.values();
	}
	
	static double[] getDistances(CommandLine cmd) {
		if (cmd.hasOption("distances")) {
			String str = cmd.getOptionValue("distances");
			String[] strs = str.split(",");
			double[] dists = new double[strs.length];
			for (int i=0; i<strs.length; i++)
				dists[i] = Double.parseDouble(strs[i]);
			return dists;
		}
		return BBP_PartBValidationConfig.DISTANCES;
	}
	
	public static Options createOptions() {
		Options ops = MPJTaskCalculator.createOptions();
		MPJ_BBP_Utils.addCommonOptions(ops, false, false, false, false);
		addCommonOptions(ops);
		
		Option numSites = new Option("ns", "num-sites", true, "Number of sites");
		numSites.setRequired(true);
		ops.addOption(numSites);
		
		addPartB_ScenarioOptions(ops);
		
		Option skipYears = new Option("skip", "skip-years", true, "Skip the given number of years at the start");
		skipYears.setRequired(false);
		ops.addOption(skipYears);
		
		Option randomAz = new Option("rand", "random-azimuth", false, "Flag for random azimuth");
		randomAz.setRequired(false);
		ops.addOption(randomAz);
		
		return ops;
	}
	
	public static void main(String[] args) {
		try {
			args = MPJTaskCalculator.initMPJ(args);
			
			Options options = createOptions();
			
			CommandLine cmd = parse(options, args, MPJ_BBP_PartBSim.class);
			
			MPJ_BBP_PartBSim driver = new MPJ_BBP_PartBSim(cmd);
			driver.run();
			
			finalizeMPJ();
			
			System.exit(0);
		} catch (Throwable t) {
			abortAndExit(t);
		}
	}
	
}
