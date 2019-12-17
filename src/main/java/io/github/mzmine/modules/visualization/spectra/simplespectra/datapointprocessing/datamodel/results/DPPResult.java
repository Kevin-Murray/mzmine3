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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

/**
 * This interface is used to store data point processing results in a
 * {@link io.github.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint} When adding a
 * new result type, also add it to the ResultType enum.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public abstract class DPPResult<T> {

  public enum ResultType {
    SUMFORMULA, ISOTOPEPATTERN, ADDUCT, FRAGMENT, ISOTOPECOMPOSITION, ISOTOPICPEAK
  };

  final T value;

  public DPPResult(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  public abstract String toString();

  public abstract ResultType getResultType();
}
