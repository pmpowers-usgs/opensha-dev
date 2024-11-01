package scratch.kevin.nshm23.wrapper;

import static org.opensha.sha.util.TectonicRegionType.ACTIVE_SHALLOW;

import java.awt.geom.Point2D;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import org.opensha.commons.data.Site;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.geo.Location;
import org.opensha.commons.param.Parameter;
import org.opensha.sha.calc.HazardCurveCalculator;
import org.opensha.sha.earthquake.param.IncludeBackgroundOption;
import org.opensha.sha.gui.infoTools.IMT_Info;
import org.opensha.sha.imr.ScalarIMR;
import org.opensha.sha.imr.attenRelImpl.ngaw2.NGAW2_Wrappers.ASK_2014_Wrapper;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGA_Param;
import org.opensha.sha.imr.param.SiteParams.Vs30_Param;
import org.opensha.sha.util.TectonicRegionType;

import gov.usgs.earthquake.nshmp.model.NshmErf;

public class NshmErfTest {

  // private static final Path MODEL =
  // Path.of("../nshmp-lib/src/test/resources/model/test-model");
  // private static final Path MODEL = Path.of("../nshm-conus-2018-5.1-maint");
  private static final Path MODEL = Path.of("/home/kevin/git/nshm-conus");

  public static void main(String[] args) {

    Set<TectonicRegionType> trts = EnumSet.of(ACTIVE_SHALLOW);
    NshmErf erf = new NshmErf(MODEL, trts, IncludeBackgroundOption.INCLUDE);
    System.out.println("NSHM ERF size: " + erf.getNumSources());
    erf.getTimeSpan().setDuration(1.0);
    erf.updateForecast();

    ScalarIMR gmpe = new ASK_2014_Wrapper();
    gmpe.setParamDefaults();
    gmpe.setIntensityMeasure(PGA_Param.NAME);

    Site site = new Site(new Location(34, -118)); // Los Angeles
    // Site site = new Site(new Location(40.75, -111.90)); // Salt lake City

    for (Parameter<?> param : gmpe.getSiteParams()) {
      site.addParameter((Parameter<?>) param.clone());
    }
    site.getParameter(Double.class, Vs30_Param.NAME).setValue(760.0);

    DiscretizedFunc linearXVals = new IMT_Info().getDefaultHazardCurve(gmpe.getIntensityMeasure());
    DiscretizedFunc hazardCurve = new ArbitrarilyDiscretizedFunc();
    for (Point2D pt : linearXVals) {
      hazardCurve.set(Math.log(pt.getX()), 0d);
    }

    HazardCurveCalculator curveCalc = new HazardCurveCalculator();
    curveCalc.getHazardCurve(hazardCurve, site, gmpe, erf);

    System.out.println("DONE");
    // System.out.println(hazardCurve);
    System.out.println("X\tY");
    for (Point2D pt : hazardCurve)
      System.out.println((float) pt.getX() + "\t" + pt.getY());
  }

}
