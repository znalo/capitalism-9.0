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

package rd.dev.simulation.command;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rd.dev.simulation.Simulation;
import rd.dev.simulation.custom.ActionStates;
import rd.dev.simulation.datamanagement.DataManager;
import rd.dev.simulation.model.Industry;
import rd.dev.simulation.model.SocialClass;
import rd.dev.simulation.utils.MathStuff;
import rd.dev.simulation.model.Stock;
import rd.dev.simulation.model.UseValue;
import rd.dev.simulation.model.UseValue.COMMODITY_ORIGIN_TYPE;
import rd.dev.simulation.utils.Dialogues;
import rd.dev.simulation.utils.Reporter;

/**
 * This class is responsible for all actions taken when the 'Trade' Button is pressed. It carries out the purchases, transferring output from sales to the
 * purchasers in the amounts decided by the constrained allocation, and pays for them
 * 
 * @author afree
 *
 */
public class Trade extends Simulation implements Command {
	private static final Logger logger = LogManager.getLogger(Trade.class);

	public void execute() {
		Reporter.report(logger, 0, "TRADE");
		advanceOneStep(ActionStates.M_C_Trade.getText(), ActionStates.M_C_PreTrade.getText());

		productivePurchasesTrade();
		socialClassesTrade();
	}

	/**
	 * each productive industry purchases the stocks that it needs
	 */
	private void productivePurchasesTrade() {
		List<Industry> industries = DataManager.industriesAll();
		Reporter.report(logger, 1, "The %d industries will now try to purchase the stocks they need. ", industries.size());

		for (Industry buyer : industries) {
			String buyerName = buyer.getIndustryName();
			Stock buyerMoneyStock = buyer.getMoneyStock();
			List<Stock> stocks = buyer.productiveStocks();

			Reporter.report(logger, 1, "Industry [%s] will purchase %d productive stocks to facilitate output of $%.0f ",
					buyerName, stocks.size(), buyer.getOutput());

			for (Stock s : stocks) {
				String useValueName = s.getUseValueName();
				UseValue stockUseValue = s.getUseValue();
				double quantityTransferred = s.getReplenishmentDemand();
				double unitPrice = stockUseValue.getUnitPrice();
				if (quantityTransferred > 0) {
					Reporter.report(logger, 2, "Industry [%s] is purchasing %.0f units of [%s] for $%.0f", s.getOwner(), quantityTransferred,
							s.getUseValueName(), quantityTransferred * unitPrice);
					Stock sellerMoneyStock = null;
					Stock sellerSalesStock = null;
					if (s.getUseValue().getCommodityOriginType() == COMMODITY_ORIGIN_TYPE.SOCIALlY_PRODUCED){
						// ask each class if it has some labour power to sell
						// TODO at this point we only accept the first offer
						// eventually we need to allow multiple sellers of Labour Power
						// but this should be part of a general reform to allow multiple sellers of every commodity
						for (SocialClass sc : DataManager.socialClassesAll()) {
							Stock salesStock = sc.getSalesStock();
							if (salesStock != null) {
								sellerMoneyStock = sc.getMoneyStock();
								sellerSalesStock = salesStock;
								Reporter.report(logger, 2, "Social class [%s] is going to sell %.0f units of [%s]", 
										sc.getSocialClassName(), quantityTransferred,s.getUseValueName());
							}
						}
						if (sellerSalesStock == null) {
							Dialogues.alert(logger, "Nobody is selling labour Power");
						}
					} else {
						Industry seller = DataManager.industryByProductUseValue(useValueName);
						Reporter.report(logger, 2, "The industry [%s] is selling [%s]", seller.getIndustryName(), s.getUseValueName());
						sellerMoneyStock = seller.getMoneyStock();
						sellerSalesStock = seller.getSalesStock();
					}
					try {
						sellerSalesStock.transferStock(s, quantityTransferred);
						buyerMoneyStock.transferStock(sellerMoneyStock, quantityTransferred * unitPrice);
					} catch (RuntimeException r) {
						Dialogues.alert(logger, "Problems transferring money. This is a programme error, so contact the developer " + r.getMessage());
					}
				}
			}
		}
	}

	/**
	 * each social class purchases the consumption goods that it needs
	 */
	private void socialClassesTrade() {
		Reporter.report(logger, 1, "Social Classes will now try to purchase the stocks they need");
		for (SocialClass buyer : DataManager.socialClassesAll()) {
			String buyerName = buyer.getSocialClassName();
			for (UseValue u : DataManager.useValuesByFunction(UseValue.COMMODITY_FUNCTION_TYPE.CONSUMER_GOOD)) {
				Industry seller = DataManager.industryByProductUseValue(u.commodityName());
				if (seller == null) {
					Dialogues.alert(logger, "Nobody seems to be selling the consumption good called [%s]", u.commodityName());
					break;
				}
				Stock consumptionStock = buyer.getConsumptionStock(u.commodityName());
				Stock buyerMoneyStock = buyer.getMoneyStock();
				Stock sellerSalesStock = seller.getSalesStock();
				Stock sellerMoneyStock = seller.getMoneyStock();
				double unitPrice = u.getUnitPrice();
				double allocatedDemand = consumptionStock.getReplenishmentDemand();
				double quantityAdded = allocatedDemand;
				double maximumQuantityAdded = buyerMoneyStock.getQuantity() / u.getUnitPrice();

				// a few little consistency checks

				if ((u == null) || (consumptionStock == null) || (buyerMoneyStock == null) || (sellerMoneyStock == null)
						|| (sellerSalesStock == null)) {
					Dialogues.alert(logger, "A stock required by [%s] to meet its needs is missing", buyerName);
					break;
				}
				if (buyer.getRevenue() > buyer.getMoneyQuantity()+MathStuff.epsilon) {
					logger.debug("Class {} has revenue {} and money {}",
							buyer.getSocialClassName(), buyer.getRevenue(),buyer.getMoneyQuantity());
					Dialogues.alert(logger,
							"Class %s has more revenue than money while purchasing the commodity %s. "
							+ "This is most probably a data error; try giving them more money."
							+ "If the problem persists, contact the developer",
							buyer.getSocialClassName(),u.commodityName());
					break;
				}
				if (maximumQuantityAdded < quantityAdded - MathStuff.epsilon) {
					logger.debug("Class {} cannot buy {} and instead has to buy {} with money {}",
							buyer.getSocialClassName(), quantityAdded,maximumQuantityAdded,buyer.getMoneyQuantity());
					Dialogues.alert(logger, "[%s] do not have enough money. This could be a data error; try giving them more money. If the problem persists, contact the developer", buyer.getSocialClassName());
					quantityAdded = maximumQuantityAdded;
					break;
				}

				// OK, it seems as if we are good to go
				
				Reporter.report(logger, 2, "The social class [%s] is buying %.0f units of [%s] for %.0f",
						buyerName, quantityAdded, u.commodityName(), quantityAdded * unitPrice);
				try {
					sellerSalesStock.transferStock(consumptionStock, quantityAdded);
					buyerMoneyStock.transferStock(sellerMoneyStock, quantityAdded * unitPrice);
					
				} catch (RuntimeException r) {
					logger.error("Transfer mis-specified:" + r.getMessage());
					r.printStackTrace();
				}
				double usedUpRevenue = quantityAdded * unitPrice;
				buyer.setRevenue(buyer.getRevenue() - usedUpRevenue);
				Reporter.report(logger, 2, "Disposable revenue reduced by $%.0f", usedUpRevenue);
			}
		}
	}


}