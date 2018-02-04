/*
 *  Copyright (C) Alan Freeman 2017-2019
 *  
 *  This file is part of the Capitalism Simulation, abbreviated to CapSim
 *  in the remainder of this project
 *
 *  Capsim is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either project 3 of the License, or
 *  (at your option) any later project.
 *
 *   Capsim is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Capsim.  If not, see <http://www.gnu.org/licenses/>.
 */
package rd.dev.simulation.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import rd.dev.simulation.Simulation;
import rd.dev.simulation.datamanagement.DataManager;
import rd.dev.simulation.model.Circuit;
import rd.dev.simulation.model.Stock;
import rd.dev.simulation.custom.TabbedTableViewer;

public class CircuitTableStockCell extends TableCell<Circuit, String> {
	static final Logger logger = LogManager.getLogger("CircuitTableCell");

	String stockValueUseName;

	public CircuitTableStockCell(String stockUseValueName) {
		this.stockValueUseName = stockUseValueName;
	}

	@Override protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null)
			return;
		Circuit circuit = getTableView().getItems().get(getIndex());
		if (circuit == null) {
			logger.debug(" Null Circuit");
			return;
		}
		Stock theStock = DataManager.stockProductiveByNameSingle(Simulation.timeStampDisplayCursor, circuit.getProductUseValueName(), stockValueUseName);
		boolean hasChanged = theStock.changed(TabbedTableViewer.displayAttribute);
		setTextFill(hasChanged ? Color.RED : Color.BLACK);// TODO not yet complete
		switch (TabbedTableViewer.displayAttribute) {
		case PRICE:
			if (ViewManager.displayHints)
				setStyle("-fx-background-color: rgba(255,240,204,0.3)");
			setText(ViewManager.pricesExpressionSymbol + item);
			break;
		case VALUE:
			if (ViewManager.displayHints)
				setStyle("-fx-background-color: rgb(255,225,225,0.3)");
			setText(ViewManager.valuesExpressionSymbol + item);
			break;
		case QUANTITY:
			if (ViewManager.displayHints)
				setStyle("-fx-background-color: rgba(220,220,220,0.3)");
			setText(item);
			break;
		}
	}
}