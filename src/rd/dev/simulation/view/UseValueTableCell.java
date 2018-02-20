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
import rd.dev.simulation.model.Commodity;

public class UseValueTableCell extends TableCell<Commodity, String> {
	static final Logger logger = LogManager.getLogger("UseValueTableCell");

	Commodity.SELECTOR useValueSelector;

	public UseValueTableCell(Commodity.SELECTOR useValueSelector) {
		this.useValueSelector = useValueSelector;
	}

	@Override protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		if (item == null) {// this happens,it seems, when the tableRow is used for the column header-or perhaps when cell is empty?
			return;
		}
		Commodity commodity = getTableView().getItems().get(getIndex());
		if (commodity == null) {
			logger.debug(" Null Use Value");
			return;
		}
		
		if (item.equals("NaN")) {
			setText("-");
			return;
		}
		String deltaModifier="";
		
		if (commodity.changed(useValueSelector)) {
			setTextFill(Color.RED);
			deltaModifier=(ViewManager.displayDeltas?ViewManager.deltaSymbol:"");
		}

		String valueModifier= deltaModifier+ViewManager.valuesExpressionSymbol;
		String priceModifier= deltaModifier+ViewManager.pricesExpressionSymbol;

		if(ViewManager.displayDeltas) {
			item=commodity.showDelta(item, useValueSelector);
		}
		switch (useValueSelector) {
		case ALLOCATIONSHARE:
		case REPLENISHMENT_DEMAND:
		case TOTALSUPPLY:
		case TOTALQUANTITY:
		case PROFITRATE:
			if (ViewManager.displayHints)
				setStyle("-fx-background-color: rgba(220,220,220,0.3)");
			item=deltaModifier+item;
			break;
		case UNITVALUE:
		case TOTALVALUE:
			if (ViewManager.displayHints)
				setStyle("-fx-background-color: rgb(255,225,225,0.3)");
			item = valueModifier+ item;
			break;
		case UNITPRICE:
		case TOTALPRICE:
		case INITIALCAPITAL:
		case PROFIT:
			if (ViewManager.displayHints)
				setStyle("-fx-background-color: rgba(255,240,204,0.3)");
			item = priceModifier + item;
			break;
		default:
			item=deltaModifier+item;
		}
		setText(item);
	}
}
