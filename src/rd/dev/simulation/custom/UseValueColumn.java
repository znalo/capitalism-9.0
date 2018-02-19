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

package rd.dev.simulation.custom;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import rd.dev.simulation.model.UseValue;
import rd.dev.simulation.view.UseValueTableCell;

/**
 * A StockColumn contains the additional information needed to display a switchable graphic and to select the data item for display
 *
 * The data items delivered to the parent TableView for display in its cells are always strings;
 * the type conversion is handled by the Industry class.
 */
public class UseValueColumn extends TableColumn<UseValue, String> {
	/**
	 * Produces a column to be displayed in a UseValue table({@code TableView<UseValue,String>}), whose value is a fixed field in a {@code UseValue} bean
	 * that is chosen by the {@code selector} enum. Use the enum to set the header text and graphic, and prepare the column header so its graphic is switchable.
	 * 
	 * @param selector
	 *            an enum specifying which field to display
	 * 
	 * @param alignedLeft
	 *            true of the data is to be displayed aligned to the left in the column
	 * 
	 */
	UseValueColumn(UseValue.USEVALUE_SELECTOR selector, boolean alignedLeft) {
		super(selector.text());
		setCellFactory(new Callback<TableColumn<UseValue, String>, TableCell<UseValue, String>>() {
			@Override public TableCell<UseValue, String> call(TableColumn<UseValue, String> col) {
				return new UseValueTableCell(selector);
			}
		});
		setCellValueFactory(cellData -> cellData.getValue().wrappedString(selector));

		// tailor the visual appearance of the column header
		if (!alignedLeft)
			getStyleClass().add("table-column-right");
		TableUtilities.addGraphicToColummnHeader(this, selector.imageName(), selector.tooltip());
	}
}
