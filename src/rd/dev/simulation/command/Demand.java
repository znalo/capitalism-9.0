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

import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rd.dev.simulation.Simulation;
import rd.dev.simulation.custom.ActionStates;
import rd.dev.simulation.datamanagement.DataManager;
import rd.dev.simulation.model.Circuit;
import rd.dev.simulation.model.SocialClass;
import rd.dev.simulation.model.Stock;
import rd.dev.simulation.model.UseValue;
import rd.dev.simulation.utils.Dialogues;
import rd.dev.simulation.utils.Reporter;

/**
 * Demand
 * 
 * One of the areas where there is the most need to test variations and allow for user reconfiguration.
 * 
 * The basic strategy is that at this point, the class's requirements are set, and these will determine how much the class acquires when we get to trade.
 * However, in the allocation phase, demand may be constrained by supply.
 * 
 * All classes try to acquire sufficient necessities to reproduce themselves; demand is thus given by their size multiplied by consumption per person.
 * 
 * TODO Luxuries, which can be consumed by capitalists, are not yet included.
 * 
 * In keeping with industrial production, the class attempts to acquire a stock of necessities which it then consumes at a speed that depends on the 'turnover
 * time' of consumption goods. Note there is a slight ambiguity in that, were consumption goods to figure in production, we implicitly assume that they turnover
 * in production at the same rate as in consumption.
 * 
 * In consumption, we suppose that the stock is given by demographics and is consumed at a speed that depends on the turnover time of consumption goods.
 * The stock is consumed 'evenly' over several periods. In each such period, however, the classes will try to acquire extra consumption goods to get up to
 * the stock levels that correspond to their size.
 */
public class Demand extends Simulation implements Command {
	private static final Logger logger = LogManager.getLogger(Demand.class);

	public void execute() {
		advanceOneStep(ActionStates.M_C_Demand.getText(), ActionStates.M_C_PreTrade.getText());
		registerProductiveDemand();
		registerLabourResponse(DataManager.getGlobal(timeStampIDCurrent).getLabourSupplyResponse());
		registerSocialClassDemand();
	}


	/**
	 * Registers productive demand - the demand that arises because producers need stocks of inputs. One of the most complex methods in the simulation. Its
	 * purpose is to calculate 'constrained output', which is the output level that can be achieved, given the money in the producers' possession and the supply
	 * of the stocks they need. Each circuit manages a set of productive stocks. The coefficient of each stock specifies how much of it the circuit must own, in
	 * order to produce one unit of output, the circuit first calculates how much money is needed to produce one unit of output, by calculating what it must
	 * spend on each stock to be in a position to make one unit of output. When the circuit has worked out the number of units of output it can produce, it then
	 * calculates the amount of each stock it would need, ideally, to produce at this level. At this stage (of developing the code) no attempt is made to
	 * estimate the likely demand for the product: this means that the price/supply/demand mechanism alone constrains suppliers. this may or may not be
	 * realistic butit's the simplest assumption, hence the best basis on which to start.
	 * <p>
	 * The response of circuits to demand is taken care of separately, in the distribution stage. The circuit first tries to maintain its existing level of
	 * production (at the outset, specified by the initial conditions,subsequently by what it last achieved). The price then responds to shortages or excesses.
	 * If the circuit has money left over at the end of the period (= the start of the next period), it then tries to invest it. A reaction function then
	 * imposes price increases or decreases according to the discrepancy between the two. More complex adjustment mechanisms can and should be developed, but a
	 * major purpose of the simulation is to assess what can be achieved with a very generic reaction function.
	 * 
	 * NOTE the procedure calculates the required outlay on fixed capital which is not to be confused with the quantity of the stock that will be consumed in
	 * production; a common confusion especially in presentations of the subject derived from Bortkiewicz and his successors, all of whom assume the whole of
	 * the input is consumed in one period, and that all turnover times are equal.
	 * 
	 * The coefficient of the stock is the magnitude of stock that the capitalist must *invest* in order to produce at a given level of output.
	 * 
	 * NOTE that the constrained output is not set at this point; that is done by 'Constrain' (for illustrative purposes - the two could be combined)
	 */
	public void registerProductiveDemand() {
		Reporter.report(logger, 0, "REGISTER PRODUCTIVE DEMAND");

		// First, set demand to zero for all use values and all stocks

		List<UseValue> allUseValues = DataManager.useValuesAll(timeStampIDCurrent);
		for (UseValue u : allUseValues) {
			u.setTotalDemand(0);
		}
		List<Stock> allStocks = DataManager.stocksAll(timeStampIDCurrent);
		for (Stock s : allStocks) {
			s.setQuantityDemanded(0);
		}

		// Now, ask all the circuits to estimate how much of each of their productive stocks they would like to purchase
		// NOTE: social class demand for consumption goods is calculated separately
		// in SocialClass.registerDemand() which is called immediately after this

		List<Circuit> results = DataManager.circuitsAll();
		for (Circuit c : results) {
			double totalCost = 0;
			logger.debug(" Estimating demand for industry {}", c.getProductUseValueType());
			double moneyAvailable = c.getMoneyQuantity();

			// at this stage, proposedOutput will have been set in the preceding Accumulate phase of the past period on the basis of plausible private plans for expansion
			// In the Constraint phase we will test to see if these private proposals are publicly possible, and if need be, constrain them according to the availability 
			// of supply and the money in the hands of the purchasers. At this point we calculate the inputs that would be needed to achieve the proposed levels of output.
			
			double constrainedOutput=0;
			double proposedOutput=c.getProposedOutput();
			
			// cost the entirety of the proposed output (by setting currentOutput to zero)
			
			c.calculateOutputCosts(); 
			totalCost=c.getCostOfExpansion();
			
			
			Reporter.report(logger, 1, " Total cost of an output of %.2f is $%.2f and $%.2f is available.",
					proposedOutput, totalCost, moneyAvailable);
			
			// check for monetary constraints
			
			if (totalCost < moneyAvailable+Simulation.epsilon) {
				Reporter.report(logger, 2, "  Output is unconstrained by cost");
				constrainedOutput = proposedOutput;
			} else {

				// TODO the code below may not work, because cost is not a linear function of output if there are pre-existing stocks
				// the problem is that in these circumstances we will underestimate the cost.
				
				Reporter.report(logger, 1, " Output is constrained by cost");
				proposedOutput = proposedOutput * moneyAvailable / totalCost;
				c.calculateOutputCosts();
				double revisedTotalCost = c.getCostOfExpansion();
				if (revisedTotalCost<moneyAvailable+Simulation.epsilon) Dialogues.alert(logger, "There is not enough money to finance the required level of output by industry %s",c.getProductUseValueType());
			}

			c.setConstrainedOutput(constrainedOutput);
			
			// now go through all the stocks again, calculating how much of each will be needed
			// and adding this to the demand for the use value that the stock represents

			Reporter.report(logger, 1, " Demand will now be set for each stock owned by industry [%s] for an output level of %.2f",
					c.getProductUseValueType(), constrainedOutput);

			List<Stock> managedStocks = DataManager.stocksProductiveByCircuit(Simulation.timeStampIDCurrent,c.getProductUseValueType());

			for (Stock s : managedStocks) {
				double coefficient = s.getCoefficient();
				String useValueName = s.getUseValueName();
				UseValue u = s.getUseValue();
				if (u == null) {
					logger.error("THE USE VALUE [" + useValueName + "] DOES NOT EXIST. Cannot calculate the demand for it");
				} else {
					double existingStock = s.getQuantity();
					double requiredStockLevel = constrainedOutput * coefficient * u.getTurnoverTime();
					double newDemand = requiredStockLevel - existingStock;
					double totalDemandForThisUseValue = u.getTotalDemand();
					double newDemandForThisUseValue = totalDemandForThisUseValue + newDemand;
					Reporter.report(logger, 2, "  Productive stock [%s] requires $%.2f to raise its level from %.2f to %.2f",
							useValueName, newDemand, existingStock, requiredStockLevel);
					Reporter.report(logger, 2, "  The demand for commodity [%s] was %.2f and is now %.2f",
							useValueName, totalDemandForThisUseValue, newDemandForThisUseValue);
					s.setQuantityDemanded(Precision.round(newDemand, roundingPrecision));
					u.setTotalDemand(Precision.round(newDemandForThisUseValue, roundingPrecision));
				}
			}
		}
	}

	/**
	 * Primitive response function for the supply of Labour Power
	 * 
	 * @param supplyResponse 
	 * 			a per-project parameter
	 *          if this is FLEXIBLE, the supply changes to match demand
	 *          if it is FIXED,supply is unaffected by demand
	 */
	private void registerLabourResponse(Simulation.SupplyResponse supplyResponse) {
		switch (supplyResponse) {
		case FLEXIBLE:
			SocialClass workers = DataManager.socialClassByName(timeStampIDCurrent, "Workers");
			UseValue labourPower=DataManager.useValueOfType(timeStampIDCurrent, UseValue.USEVALUETYPE.LABOURPOWER);
			double demandForLabourPower = labourPower.getTotalDemand();
			Reporter.report(logger, 2, "  Labour Power supply has increased in response to demand. It was %.2f and is now %.2f", workers.getSalesQuantity(),
					demandForLabourPower);
			workers.getSalesStock().modifyTo(demandForLabourPower);
			
			// A bit of a botch. More labour power has been summoned from the reserve army than originally recorded by RegisterProductiveDemand.
			// So we retrospectively increase the supply.
			
			labourPower.setTotalSupply(demandForLabourPower);
			break;
		case FIXED:
			Reporter.report(logger, 2, "  Labour Power supply is unaffected by demand. ");
			break;
		default:
		}
	}

	/**
	 * Register the demand for consumption goods by the various social classes (at present normally 2-3, but this can be made quite
	 * variable eg by creating different classes based on property rights, income level, etc.). There are as yet no
	 * reproduction dynamics: we don't suppose that a failure to secure the goods results in death or disability. This also needs to be customised.
	 * <p>
	 * The consumption of all classes is determined by their revenue, but the manner in which they receive this revenue differs.
	 * Workers' income is the result of a purchase, whereas that of other classes is a residual. Workers receive money in the trade phase of the simulation.
	 * Other classes receive money in the distribution phase.
	 * <p>
	 * In consequence workers consume their income in the current period, by and large, to reproduce themselves and the commodity they sell, being their labour
	 * power. Other classes consume their income in the period after they receive it, and also invest some of it. Notwithstanding, the consumption of all
	 * classes is constrained by their money. They can only consume what they can pay for. We make no attempt as yet to account for consumer durables.
	 * <p>
	 * In this section we only 'estimate' the demand; the actual consumption takes place in the production stage (because, in consuming, classes produce
	 * themselves). We suppose that all classes attempt to spend all their revenue. If, in the 'registerAllocation' stage they are prevented from doing so by
	 * shortages, they will consume the goods allocated to them. More sophisticated assumptions can be added later, including as user-supplied algorithms.
	 * 
	 * Note we depart from Sraffian or simplistic schemas in which workers are supplied directly by their employers with goods. In reality, and in
	 * this simulation, they are supplied with money, not goods. They then spend this money to get the goods.
	 * 
	 */
	public void registerSocialClassDemand() {
		Reporter.report(logger, 0, "REGISTER DEMAND FROM CLASSES");
		UseValue consumptionGoodUseValue = DataManager.useValueOfConsumptionGoods(timeStampIDCurrent);
		double priceOfConsumptionGoods = consumptionGoodUseValue.getUnitPrice();
		double demand = 0.0;
		List<SocialClass> classes = DataManager.socialClassesAll(timeStampIDCurrent);
		for (SocialClass sc : classes) {
			double money = sc.getMoneyQuantity();
			double thisClassDemand = 0.0;
			if (sc.getSocialClassName().equals("Workers")) {
				Reporter.report(logger, 1,
						" [Workers] revenue given by the wage. They will sell %.2f of their labour power for $%.2f",
						sc.getSalesQuantity(), sc.getSalesPrice());
				sc.setRevenue(sc.getSalesPrice());
			} else {
				Reporter.report(logger, 1,
						" [%s] revenue, established in the previous period, is %.2f ",
						sc.getSocialClassName(), sc.getRevenue());
			}
			Reporter.report(logger, 1,
					" Demand for necessities priced at $%.2f by the class [%s], whose revenue is %.2f, is %.2f ",
					priceOfConsumptionGoods, sc.getSocialClassName(), sc.getRevenue(),sc.getRevenue() / priceOfConsumptionGoods);
			thisClassDemand = sc.getRevenue() / priceOfConsumptionGoods;
			double minimumDemand = money / priceOfConsumptionGoods;
			if (minimumDemand >= thisClassDemand) {
				Reporter.report(logger, 1, " Demand is unconstrained by money and will be set at %.2f", thisClassDemand);
			} else {
				thisClassDemand = minimumDemand;
				Reporter.report(logger, 1, " Demand is constrained by money and will be set at %.2f", minimumDemand);
			}

			// remember how much this class wants, so that we know how much to buy when we get to trade.
			// this may be adjusted downwards in the allocation phase

			sc.setConsumptionQuantityDemanded(Precision.round(thisClassDemand, roundingPrecision));
			demand += thisClassDemand;
		}
		Reporter.report(logger, 1, " Total demand for Consumption Goods is %.2f", demand);
		consumptionGoodUseValue.setTotalDemand(Precision.round(demand, roundingPrecision));
	}
}