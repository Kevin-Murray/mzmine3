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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class AreaShareChart extends StackPane {

  public AreaShareChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    Double sum = row.streamFeatures().map(Feature::getArea).reduce(0d, Double::sum);

    List<Rectangle> all = new ArrayList<>();
    int i = 0;
    int size = row.getFilesFeatures().size();
    for (Entry<RawDataFile, ModularFeature> entry : row.getFilesFeatures().entrySet()) {
      Double area = entry.getValue().getArea();
      if (area != null) {
        // color from sample
        // Color color = entry.getValue().get(RawColorType.class).getValue();
        Color fileColor = entry.getKey().getColor();
        if (fileColor == null) {
          fileColor = Color.DARKORANGE;
        }

        double ratio = area / sum;
        Rectangle rect = new Rectangle();
        rect.setFill(fileColor);
        // bind width
        rect.widthProperty().bind(this.widthProperty().multiply(ratio));
        rect.setHeight(i % 2 == 0 ? 20 : 25);
        all.add(rect);
        i++;
        if (progress != null)
          progress.addAndGet(1.0 / size);
      }
    }
    HBox box = new HBox(0, all.toArray(Rectangle[]::new));
    box.setPrefWidth(100);
    box.setAlignment(Pos.CENTER_LEFT);

    this.getChildren().add(box);
  }
}
