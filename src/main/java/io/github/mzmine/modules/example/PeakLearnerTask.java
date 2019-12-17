/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.example;

import java.util.Arrays;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakSorter;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

class PeakLearnerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private PeakList peakList;
  private PeakList resultPeakList;

  // peaks counter
  private int processedPeaks;
  private int totalPeaks;

  // parameter values
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean removeOriginal;
  private ParameterSet parameters;

  /**
   * Constructor to set all parameters and the project
   * 
   * @param rawDataFile
   * @param parameters
   */
  public PeakLearnerTask(MZmineProject project, PeakList peakList, ParameterSet parameters) {
    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;
    // Get parameter values for easier use
    suffix = parameters.getParameter(LearnerParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(LearnerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LearnerParameters.rtTolerance).getValue();
    removeOriginal = parameters.getParameter(LearnerParameters.autoRemove).getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Learner task on " + peakList;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalPeaks == 0)
      return 0;
    return (double) processedPeaks / (double) totalPeaks;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running learner task on " + peakList);

    // Create a new results peakList which is added at the end
    resultPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());

    /**
     * - A PeakList is a list of Features (peak in retention time dimension with accurate m/z)<br>
     * ---- contains one or multiple RawDataFiles <br>
     * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
     * - A RawDataFile holds a full chromatographic run with all ms scans<br>
     * ---- Each Scan and the underlying raw data can be accessed <br>
     * ---- Scans can be filtered by MS level, polarity, ...<br>
     */
    // is the data provided by peaklist enough for this task or
    // do you want to work on one raw data file or on all files?
    RawDataFile dataFile = peakList.getRawDataFile(0);

    // get all peaks of a raw data file
    // Sort peaks by ascending mz
    Feature[] sortedPeaks = peakList.getPeaks(dataFile);
    Arrays.sort(sortedPeaks, new PeakSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Loop through all peaks
    totalPeaks = sortedPeaks.length;
    for (int i = 0; i < totalPeaks; i++) {
      // check for cancelled state and stop
      if (isCanceled())
        return;

      // current peak
      Feature aPeak = sortedPeaks[i];

      // do stuff
      // ...

      // add row to result feature list
      PeakListRow row = peakList.getPeakRow(aPeak);
      row = copyPeakRow(row);
      resultPeakList.addRow(row);

      // Update completion rate
      processedPeaks++;
    }

    // add to project
    addResultToProject();

    logger.info("Finished on " + peakList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Create a copy of a feature list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static PeakListRow copyPeakRow(final PeakListRow row) {
    // Copy the feature list row.
    final PeakListRow newRow = new SimplePeakListRow(row.getID());
    PeakUtils.copyPeakListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getPeaks()) {
      final Feature newPeak = new SimpleFeature(peak);
      PeakUtils.copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getDataFile(), newPeak);
    }

    return newRow;
  }

  /**
   * Add peaklist to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList
        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Learner task", parameters));

    // Remove the original peakList if requested
    if (removeOriginal)
      project.removePeakList(peakList);
  }

}
