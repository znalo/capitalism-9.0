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

package capitalism.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import capitalism.Simulation;
import capitalism.model.Commodity;
import capitalism.model.Global;
import capitalism.model.Industry;
import capitalism.model.Stock;
import capitalism.model.Commodity.ORIGIN;
import capitalism.utils.Dialogues;
import capitalism.utils.MathStuff;
import capitalism.utils.Reporter;
import capitalism.view.custom.ActionStates;

public class IndustriesProduce extends Simulation implements Command {
	private static final Logger logger = LogManager.getLogger("Industry Production");

	/**
	 * For each industry decrease the stocks and increase the sales stocks, using the coefficient to decide how much gets used up.
	 * For each social class decrease the stock of consumption goods and decide, on Malthusian principles, what happens to the classes
	 * (user algorithms could play a big role here). Recalculate the value produced (=C*MELT +L)
	 * 
	 * TODO should commodities re-calculate the unit value when all producers are done?
	 */
	public void execute() {
		Reporter.report(logger, 0, "INDUSTRY PRODUCTION");
		advanceOneStep(ActionStates.C_P_IndustriesProduce.text(), ActionStates.C_P_Produce.text());
		Global global = Global.getGlobal();
		double melt = global.getMelt();

		// initialise the accounting for how much of this commodity is used up and how much is created in production in the current period
		// so we can calculate how much surplus of it resulted from production in this period.
		// TODO write a query to do this

		for (Commodity u : Commodity.commoditiesByOrigin(ORIGIN.INDUSTRIALLY_PRODUCED)) {
			u.setStockUsedUp(0);
			u.setStockProduced(0);
		}
		
		// remember the initial productive capital so we can equalize the profit rate, if this option is selected, excluding money
		
		Simulation.setInitialProductiveCapitals();
		
		// Now do the actual production: value process then production process
		// all productive stocks except a stock of type labour power now contribute value to the product of the industry that owns them,
		// equal to their price at this time except stocks of type labour power, which contribute their magnitude, multiplied by their complexity, divided by the MELT
		// (TODO incorporate labour complexity)
		
		for (Industry c : Industry.industriesAll()) {
			String commodityType = c.getIndustryName();
			Stock salesStock = c.getSalesStock();
			Commodity commodity = c.getCommodity();
			double output = c.getOutput();
			double valueAdded = 0;
			Reporter.report(logger, 1, " Industry [%s] is producing %.0f. units of its output; the melt is %.4f", commodityType, output, melt);

			for (Stock s : Stock.productiveByIndustry(timeStampIDCurrent, commodityType)) {

				// a little consistency check ...
				if (!s.getStockType().equals("Productive")) {
					Dialogues.alert(logger,
							String.format("Non-productive stock of type [%s] called [%s] included as input ", s.getStockType(), s.getCommodityName()));
				}
				// .. end of little consistency check

				double coefficient = s.getProductionCoefficient();
				double stockUsedUp = output * coefficient;
				stockUsedUp = MathStuff.round(stockUsedUp);
				if (s.getCommodity().getOrigin() == ORIGIN.SOCIALlY_PRODUCED) {
					valueAdded += stockUsedUp * melt;
					Reporter.report(logger, 2, "  Labour Power has added value amounting to %.0f (intrinsic %.0f) to commodity [%s]", valueAdded, stockUsedUp,c.getIndustryName());
				} else {
					double valueOfStockUsedUp = stockUsedUp * commodity.getUnitValue();
					Reporter.report(logger, 2, "  Stock [%s] has transferred value $%.0f (intrinsic %.0f) to commodity [%s] ",
							s.getCommodityName(), valueOfStockUsedUp, valueOfStockUsedUp / melt, c.getIndustryName());
					valueAdded += valueOfStockUsedUp;
				}

				// the stock is reduced by what was used up, and account of this is registered with its use value
				Commodity u = s.getCommodity();
				if (stockUsedUp>0) {
				Reporter.report(logger, 2, "  %.0f units of [%s] were used up in producing the output [%s]", stockUsedUp, u.commodityName(),
						c.getIndustryName());
				double stockOfUSoFarUsedUp = u.getStockUsedUp();
				u.setStockUsedUp(stockOfUSoFarUsedUp + stockUsedUp); //TODO eliminate this and compute commodity stock usage from stocks themselves
				s.modifyBy(-stockUsedUp);
				s.setStockUsedUp(s.getStockUsedUp()+stockUsedUp);
				}
			}

			// to set the value of the output, we now use an overloaded version of modifyBy which only sets the value

			double extraSalesQuantity = output;
			extraSalesQuantity = MathStuff.round(extraSalesQuantity);
			salesStock.modifyBy(extraSalesQuantity, valueAdded);
			c.getCommodity().setStockProduced(c.getCommodity().getStockProduced() + extraSalesQuantity);
			Reporter.report(logger, 2,
					"  The sales stock of [%s] has grown to %.0f, its value to $%.0f (intrinsic value %.0f) and its price to $%.0f (intrinsic value %.0f)",
					c.getIndustryName(), salesStock.getQuantity(), salesStock.getValue(), salesStock.getValue() / melt, salesStock.getPrice(),
					salesStock.getPrice() / melt);
		}

		// now (and only now) we can calculate the surplus (if any) of each of the use values

		for (Commodity u : Commodity.commoditiesByOrigin(ORIGIN.INDUSTRIALLY_PRODUCED)) {
			u.setSurplusProduct(u.getStockProduced() - u.getStockUsedUp());
		}
		
		// persist the profit that has been made, so that the Prices phase has access to it
		for (Industry c:Industry.industriesAll()) {
			c.persistProfit();
		}
	}
}