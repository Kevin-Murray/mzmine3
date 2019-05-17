/*
 * Copyright 2006-2019 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.Window;
import java.text.DecimalFormat;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * Module to compare single spectra with spectral databases
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationSpectralDatabaseParameters extends SimpleParameterSet {

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "(GNPS json, MONA json, NIST msp, JCAMP-DX jdx) Name of file that contains information for peak identification");

  public static final MassListParameter massList = new MassListParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final OptionalParameter<DoubleParameter> usePrecursorMZ =
      new OptionalParameter<>(new DoubleParameter("Use precursor m/z",
          "Use precursor m/z as a filter. Precursor m/z of library entry and this scan need to be within m/z tolerance. Entries without precursor m/z are skipped.",
          MZmineCore.getConfiguration().getMZFormat(), 0d));

  public static final DoubleParameter noiseLevel = new DoubleParameter("Minimum ion intensity",
      "Signals below this level will be filtered away from mass lists",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter minCosine = new DoubleParameter("Minimum  cosine similarity",
      "Minimum cosine similarity. (All signals in the masslist are compared against all spectral library entries. "
          + "Considers only signals which were found in both the masslist and the library entry)",
      new DecimalFormat("0.00"), 0.95);

  public static final IntegerParameter minMatch = new IntegerParameter("Minimum  matched signals",
      "Minimum number of matched signals in spectra and spectral library entry (within mz tolerance)",
      20);

  public SpectraIdentificationSpectralDatabaseParameters() {
    super(new Parameter[] {dataBaseFile, massList, mzTolerance, usePrecursorMZ, noiseLevel,
        minCosine, minMatch});
  }


  public ExitCode showSetupDialog(Scan scan, Window parent, boolean valueCheckRequired) {
    // set precursor mz to parameter if MS2 scan
    // otherwise leave the value to the one specified before
    if (scan.getPrecursorMZ() != 0)
      this.getParameter(usePrecursorMZ).getEmbeddedParameter().setValue(scan.getPrecursorMZ());
    else
      this.getParameter(usePrecursorMZ).setValue(false);

    return super.showSetupDialog(parent, valueCheckRequired);
  }
}