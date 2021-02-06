/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.imsfeaturevisualizer;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonMobilogramTimeSeriesToRtMobilityHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.SingleSpectrumProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.FeatureUtils;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleEdge;

public class SingleIMSFeatureVisualiserPane extends GridPane {

  private final SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> heatmapChart;
  private final SimpleXYChart<SingleSpectrumProvider> msmsSpectrumChart;
  private final SimpleXYChart<SummedMobilogramXYProvider> mobilogramChart;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final ModularFeature feature;
  private SimpleObjectProperty<MobilityScan> selectedMobilityScan;

  public SingleIMSFeatureVisualiserPane(ModularFeature f) {
    super();

    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    this.feature = f;
    String fstr = FeatureUtils.featureToString(f);
    this.heatmapChart = new SimpleXYZScatterPlot<>("Ion trace - " + fstr);
    this.msmsSpectrumChart = new SimpleXYChart<>("MS/MS - " + fstr);
    this.mobilogramChart = new SimpleXYChart<>("Extracted mobilogram");

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    selectedMobilityScan = new SimpleObjectProperty<>();

    initCharts();
    heatmapChart.setDataset(new IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(feature));
    mobilogramChart.addDataset(new SummedMobilogramXYProvider(feature, true));

    Scan msmsSpectrum = feature.getMostIntenseFragmentScan();
    if (msmsSpectrum != null) {
      msmsSpectrumChart.addDataset(new SingleSpectrumProvider(msmsSpectrum));
    }

    initChartPanes();

    ColumnConstraints col0 = new ColumnConstraints(150);
    ColumnConstraints col1 = new ColumnConstraints(340);

    RowConstraints row0 = new RowConstraints(250);
    RowConstraints row1 = new RowConstraints(60);
    RowConstraints row2 = new RowConstraints();
    RowConstraints row3 = new RowConstraints(150);

    getRowConstraints().addAll(row0, row1, row2, row3);
    getColumnConstraints().addAll(col0, col1);
  }

  private void initCharts() {
    final MobilityType mobilityType = ((IMSRawDataFile) feature.getRawDataFile()).getMobilityType();

    heatmapChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    heatmapChart.setDomainAxisNumberFormatOverride(rtFormat);
    heatmapChart.setRangeAxisLabel(mobilityType.getAxisLabel());
    heatmapChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmapChart.setLegendAxisLabel(unitFormat.format("Intensity", "counts"));
    heatmapChart.setLegendNumberFormatOverride(intensityFormat);
    heatmapChart.setDefaultPaintscaleLocation(RectangleEdge.RIGHT);
    heatmapChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmapChart.setShowCrosshair(false);
    heatmapChart.cursorPositionProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.getDataset() instanceof ColoredXYDataset) {
        ColoredXYDataset dataset = (ColoredXYDataset) newValue.getDataset();
        if (dataset.getValueProvider() instanceof MassSpectrumProvider) {
          MassSpectrumProvider spectrumProvider = (MassSpectrumProvider) dataset
              .getValueProvider();
          MassSpectrum spectrum = spectrumProvider.getSpectrum(newValue.getValueIndex());
          if (spectrum instanceof MobilityScan) {
            selectedMobilityScan.set((MobilityScan) spectrum);
          }
        }
      }
    });
    NumberAxis axis = (NumberAxis) heatmapChart.getXYPlot().getRangeAxis();
    axis.setAutoRange(true);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeMinimumSize(0.005);
    axis.setVisible(false);

    msmsSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    msmsSpectrumChart.setDomainAxisLabel("m/z");
    msmsSpectrumChart.setRangeAxisLabel(unitFormat.format("Intensity", "counts"));
    msmsSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    msmsSpectrumChart.setShowCrosshair(false);

    mobilogramChart.setDomainAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(unitFormat.format("Intensity", "counts"));
    mobilogramChart.setRangeAxisLabel(mobilityType.getAxisLabel());
    mobilogramChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.getXYPlot().getDomainAxis().setInverted(true);
    mobilogramChart.setShowCrosshair(false);
    mobilogramChart.switchLegendVisible();
    mobilogramChart.addDatasetsChangedListener(l -> {
      Platform.runLater(() -> {
        NumberAxis a = (NumberAxis) heatmapChart.getXYPlot().getRangeAxis();
        a.setAutoRangeIncludesZero(false);
        a.setAutoRangeStickyZero(false);
        a.setAutoRangeMinimumSize(0.005);
        a.setAutoRange(true);
        mobilogramChart.switchLegendVisible();
      });

    });

    ChartGroup mobilityGroup = new ChartGroup(false, false, false, true);
    mobilityGroup.add(new ChartViewWrapper(mobilogramChart));
    mobilityGroup.add(new ChartViewWrapper(heatmapChart));
  }

  private void initChartPanes() {
//    Canvas legendCanvas = new Canvas();
//    heatmapChart.setLegendCanvas(legendCanvas);
//    legendCanvas.widthProperty().bind(heatmapChart.widthProperty());
//    legendCanvas.setHeight(60);

    add(new BorderPane(mobilogramChart), 0, 0);
    add(new BorderPane(heatmapChart), 1, 0);
//    add(new BorderPane(legendCanvas), 1, 1);

    ComboBox<Scan> fragmentScanSelection = new ComboBox<>();
    fragmentScanSelection.setItems(feature.getAllMS2FragmentScans());
    fragmentScanSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
      msmsSpectrumChart.removeAllDatasets();
      msmsSpectrumChart.addDataset(new SingleSpectrumProvider(newValue));
    });

    FlowPane controls = new FlowPane(new Label("Fragment spectrum: "));
    controls.setHgap(5);
    controls.getChildren().add(fragmentScanSelection);
    controls.setAlignment(Pos.TOP_CENTER);

    BorderPane spectrumPane = new BorderPane(msmsSpectrumChart);
    add(spectrumPane, 0, 2, 2, 1);
  }

  public SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> getHeatmapChart() {
    return heatmapChart;
  }

  public SimpleXYChart<SingleSpectrumProvider> getMsmsSpectrumChart() {
    return msmsSpectrumChart;
  }

  public SimpleXYChart<SummedMobilogramXYProvider> getMobilogramChart() {
    return mobilogramChart;
  }

  public MobilityScan getSelectedMobilityScan() {
    return selectedMobilityScan.get();
  }

  public void setSelectedMobilityScan(MobilityScan selectedMobilityScan) {
    this.selectedMobilityScan.set(selectedMobilityScan);
  }

  public SimpleObjectProperty<MobilityScan> selectedMobilityScanProperty() {
    return selectedMobilityScan;
  }
}
