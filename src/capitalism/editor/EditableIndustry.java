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
package capitalism.editor;

import java.util.HashMap;

import capitalism.model.Industry;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;

public class EditableIndustry {
	private StringProperty name;
	private StringProperty commodityName;
	private DoubleProperty output;
	private EditableStock salesStock;
	private EditableStock moneyStock;
	private HashMap<String, EditableStock> productiveStocks;

	enum EI_ATTRIBUTE {
		NAME("Name"), COMMODITY_NAME("Product"), OUTPUT("Output"), SALES("Sales"), MONEY("Money"), PRODUCTIVE_STOCK("Input");
		protected String text;

		EI_ATTRIBUTE(String text) {
			this.text = text;
		}
	}

	public EditableIndustry() {
		name = new SimpleStringProperty();
		output = new SimpleDoubleProperty();
		commodityName = new SimpleStringProperty();
		salesStock = new EditableStock();
		productiveStocks = new HashMap<String, EditableStock>();
	}

	public static ObservableList<EditableIndustry> editableIndustries(int timeStampID, int projectID) {
		ObservableList<EditableIndustry> result = FXCollections.observableArrayList();
		for (Industry c : Industry.allCurrent()) {
			EditableIndustry oneRecord = new EditableIndustry();
			oneRecord.setName(c.name());
			oneRecord.setCommodityName(c.getCommodityName());
			oneRecord.setOutput(c.getOutput());
			result.add(oneRecord);
		}
		return result;
	}

	public void set(EI_ATTRIBUTE attribute, double d) {
		switch (attribute) {
		case OUTPUT:
			output.set(d);
			break;
		case SALES:
			salesStock.getQuantityProperty().set(d);
		case MONEY:
			moneyStock.getQuantityProperty().set(d);
		default:
		}
	}

	public double getDouble(EI_ATTRIBUTE attribute) {
		switch (attribute) {
		case OUTPUT:
			return getOutput();
		case SALES:
			return salesStock.getQuantity();
		case MONEY:
			return moneyStock.getQuantity();
		default:
			return Double.NaN;
		}
	}

	public void set(EI_ATTRIBUTE attribute, String newValue) {
		switch (attribute) {
		case NAME:
			name.set(newValue);
			break;
		default:
			break;
		}
	}

	public String getString(EI_ATTRIBUTE attribute) {
		switch (attribute) {
		case NAME:
			return getName();
		default:
			return "";
		}
	}

	public void addProductiveStock(String commodityName) {
		EditableStock stock = new EditableStock();
		productiveStocks.put(commodityName, stock);
	}

	public static TableColumn<EditableIndustry, Double> makeStockColumn(String commodityName) {
		TableColumn<EditableIndustry, Double> col = new TableColumn<EditableIndustry, Double>(commodityName);
		col.setCellValueFactory(cellData -> cellData.getValue().stockDoubleProperty(commodityName));
		return col;
	}

	public static TableColumn<EditableIndustry, Double> makeDoubleColumn(EI_ATTRIBUTE attribute) {
		TableColumn<EditableIndustry, Double> col = new TableColumn<EditableIndustry, Double>(attribute.text);
		Callback<TableColumn<EditableIndustry, Double>, TableCell<EditableIndustry, Double>> cellFactory;
		cellFactory = new Callback<TableColumn<EditableIndustry, Double>, TableCell<EditableIndustry, Double>>() {
			public TableCell<EditableIndustry, Double> call(TableColumn<EditableIndustry, Double> p) {
				return new EditableIndustryCell();
			}
		};
		col.setCellValueFactory(
				cellData -> cellData.getValue().doubleProperty(attribute)
		// new PropertyValueFactory<EditableIndustry, Double>(fieldName)
		);
		col.setCellFactory(cellFactory);
		col.setOnEditCommit(
				new EventHandler<TableColumn.CellEditEvent<EditableIndustry, Double>>() {
					@Override public void handle(TableColumn.CellEditEvent<EditableIndustry, Double> t) {
						((EditableIndustry) t.getTableView().getItems().get(
								t.getTablePosition().getRow())).set(attribute, t.getNewValue());
					}
				});
		return col;
	}

	public static TableColumn<EditableIndustry, String> makeStringColumn(EI_ATTRIBUTE attribute) {
		TableColumn<EditableIndustry, String> col = new TableColumn<EditableIndustry, String>(attribute.text);
		Callback<TableColumn<EditableIndustry, String>, TableCell<EditableIndustry, String>> cellFactory;
		cellFactory = new Callback<TableColumn<EditableIndustry, String>, TableCell<EditableIndustry, String>>() {
			public TableCell<EditableIndustry, String> call(TableColumn<EditableIndustry, String> p) {
				return new EditableIndustryStringCell();
			}
		};
		col.setCellValueFactory(cellData -> cellData.getValue().stringProperty(attribute)
		// TODO need to abstract here
		// new PropertyValueFactory<EditableIndustry, String>(fieldName)
		);
		col.setCellFactory(cellFactory);
		col.setOnEditCommit(
				new EventHandler<TableColumn.CellEditEvent<EditableIndustry, String>>() {
					@Override public void handle(TableColumn.CellEditEvent<EditableIndustry, String> t) {
						((EditableIndustry) t.getTableView().getItems().get(
								t.getTablePosition().getRow())).set(attribute, t.getNewValue());
					}
				});
		return col;
	}

	private ObservableValue<Double> doubleProperty(EI_ATTRIBUTE attribute) {
		switch (attribute) {
		case OUTPUT:
			return output.asObject();
		default:
			return new SimpleDoubleProperty(Double.NaN).asObject();
		}
	}

	private ObservableValue<Double> stockDoubleProperty(String commodityName) {
		EditableStock stock = productiveStocks.get(commodityName);
		return stock.getQuantityProperty().asObject();
	}

	private static class EditableIndustryStringCell extends TableCell<EditableIndustry, String> {
		private TextField textField;

		public EditableIndustryStringCell() {
		}

		@Override public void startEdit() {
			super.startEdit();
			if (textField == null) {
				createTextField();
			}
			setGraphic(textField);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			textField.selectAll();
		}

		@Override public void cancelEdit() {
			super.cancelEdit();
			setText(String.valueOf(getItem()));
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}

		@Override public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				if (isEditing()) {
					if (textField != null) {
						textField.setText(getString());
					}
					setGraphic(textField);
					setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				} else {
					setText(getString());
					setContentDisplay(ContentDisplay.TEXT_ONLY);
				}
			}
		}

		private void createTextField() {
			textField = new TextField(getString());
			textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
			textField.setOnKeyPressed(new EventHandler<KeyEvent>() {

				@Override public void handle(KeyEvent t) {
					if (t.getCode() == KeyCode.ENTER) {
						commitEdit(textField.getText());
					} else if (t.getCode() == KeyCode.ESCAPE) {
						cancelEdit();
					}
				}
			});
		}

		private String getString() {
			return getItem() == null ? "" : getItem().toString();
		}
	}

	private static class EditableIndustryCell extends TableCell<EditableIndustry, Double> {
		private TextField textField;

		public EditableIndustryCell() {
		}

		@Override public void startEdit() {
			super.startEdit();
			if (textField == null) {
				createTextField();
			}
			setGraphic(textField);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			textField.selectAll();
		}

		@Override public void cancelEdit() {
			super.cancelEdit();
			setText(String.valueOf(getItem()));
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}

		@Override public void updateItem(Double item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				if (isEditing()) {
					if (textField != null) {
						textField.setText(getString());
					}
					setGraphic(textField);
					setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				} else {
					setText(getString());
					setContentDisplay(ContentDisplay.TEXT_ONLY);
				}
			}
		}

		private void createTextField() {
			textField = new TextField(getString());
			textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
			textField.setOnKeyPressed(new EventHandler<KeyEvent>() {

				@Override public void handle(KeyEvent t) {
					if (t.getCode() == KeyCode.ENTER) {
						commitEdit(Double.parseDouble(textField.getText()));
					} else if (t.getCode() == KeyCode.ESCAPE) {
						cancelEdit();
					}
				}
			});
		}

		private String getString() {
			return getItem() == null ? "" : getItem().toString();
		}
	}

	private ObservableValue<String> stringProperty(EI_ATTRIBUTE attribute) {
		switch (attribute) {
		case NAME:
			return name;
		case COMMODITY_NAME:
			return commodityName;
		default:
			return new SimpleStringProperty("");
		}

	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	/**
	 * @return the commodityName
	 */
	public String getCommodityName() {
		return commodityName.get();
	}

	/**
	 * @param commodityName
	 *            the commodityName to set
	 */
	public void setCommodityName(String commodityName) {
		this.commodityName.set(commodityName);
	}

	/**
	 * @return the output
	 */
	public Double getOutput() {
		return output.get();
	}

	/**
	 * @param output
	 *            the output to set
	 */
	public void setOutput(Double output) {
		this.output.set(output);
	}
}