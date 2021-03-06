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

package capitalism.view.tables;

import capitalism.controller.Simulation;
import capitalism.model.Commodity;
import capitalism.model.SocialClass;
import capitalism.view.TabbedTableViewer;
import capitalism.view.TableUtilities;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * A CircuitGraphicsColumn contains the additional information needed to display a switchable graphic and to select the data item for display
 *
 * The data items delivered to the parent TableView for display in its cells are always strings;
 * the type conversion is handled by the Industry class.
 * TODO parameterise SocialClass so we can re-use for other data models (eg Circuits, for which the code is almost identical
 */
public class SocialClassColumn extends TableColumn<SocialClass, String> {
	/**
	 * Produces a column to be displayed in a socialClass table({@code TableView<SocialClass,String>}), whose value is a fixed field in a {@code SocialClass}
	 * bean
	 * that is chosen by the {@code selector} enum. Use the enum to set the header text and graphic, and prepare the column header so its graphic is switchable.
	 * 
	 * @param socialClassAttribute
	 *            an enum specifying which field to display
	 * @param alignedLeft
	 *            true if the data in this column should be displayed aligned to the left (typically text fields such as commodity names or owner names)
	 */
	public SocialClassColumn(SocialClass.SOCIALCLASS_ATTRIBUTE socialClassAttribute, boolean alignedLeft) {
		super(socialClassAttribute.text());
		setCellFactory(new Callback<TableColumn<SocialClass, String>, TableCell<SocialClass, String>>() {
			@Override public TableCell<SocialClass, String> call(TableColumn<SocialClass, String> col) {
				return new SocialClassTableCell(socialClassAttribute);
			}
		});
		setCellValueFactory(cellData -> cellData.getValue().wrappedString(socialClassAttribute, TabbedTableViewer.displayAttribute));

		// tailor the visual appearance of the column header

		setPrefWidth(75.0);
		if (!alignedLeft)
			getStyleClass().add("table-column-right");
		TableUtilities.addGraphicToColummnHeader(this, socialClassAttribute.imageName(), socialClassAttribute.tooltip());
	}

	/**
	 * Produces a column to be displayed in a Industry table({@code TableView<Industry,String>}), whose value is a {@code Stock} field referenced by a foreign key
	 * in a {@code Industry} bean. The magnitude is selected by the {@code name} of the Stock.
	 * Use Stock itself to set the header text and graphic, and prepare the column header so its graphic is switchable.
	 * 
	 * @param u
	 *            an enum specifying which commodity's stock to display
	 */

	public SocialClassColumn(Commodity u) {
		String consumptionStockName=u.name();
		setCellFactory(new Callback<TableColumn<SocialClass, String>, TableCell<SocialClass, String>>() {
			@Override public TableCell<SocialClass, String> call(TableColumn<SocialClass, String> col) {
				return new SocialClassTableStockCell(consumptionStockName);
			}
		});
		setCellValueFactory(cellData -> cellData.getValue().wrappedString(consumptionStockName));

		// tailor the visual appearance of the column header

		setText(consumptionStockName);
		setPrefWidth(75.0);
		getStyleClass().add("table-column-right");
		Commodity stockCommodity = Commodity.single(Simulation.projectIDCurrent(), Simulation.timeStampIDCurrent(), consumptionStockName);
		TableUtilities.addGraphicToColummnHeader(this, stockCommodity.getImageName(), u.getToolTip());
	}
}
