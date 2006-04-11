/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/


package net.sf.mzmine.methods.alignment;

import net.sf.mzmine.datastructures.AlignmentResult;
import net.sf.mzmine.userinterface.MainWindow;


/* New interface for all methods that process alignment results at client-side
and return their output as a new alignment result object.
Also normalizers will implement this interface (instead of Normalizer which
will be thrown away) in the future.
*/

public interface AlignmentResultProcessor {

	public AlignmentResult processAlignment(MainWindow _mainWin, AlignmentResult ar, AlignmentResultProcessorParameters arpParams);

}