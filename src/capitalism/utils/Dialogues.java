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
package capitalism.utils;

import java.io.File;

import org.apache.logging.log4j.Logger;

import capitalism.view.ViewManager;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;

public class Dialogues {

	/**
	 * legacy version of alert(logger, formatString, args) to support calls being phased out.
	 * 
	 * TODO eliminate these calls
	 * 
	 * record the alert in the specified log file and display it in an alert window for the user to see
	 * 
	 * @param logger
	 *            the logger to use
	 * @param formatString
	 *            a format string suitable for the {@code String.format()} method
	 */

	public static void alert(Logger logger, String formatString) {

		alert(logger,formatString,(Object[]) null);
		
	}

	/**
	 * record the alert in the specified log file and display it in an alert window for the user to see
	 * 
	 * @param logger
	 *            the logger to use
	 * @param args
	 *            the arguments for a call to {@code String.format()} using formatString
	 * @param formatString
	 *            a format string suitable for the {@code String.format()} method
	 */

	public static void alert(Logger logger, String formatString, Object... args) {

		RuntimeException r = new RuntimeException(formatString);
		logger.debug(formatString);


		Reporter.report(logger, 0, formatString, args);
		StackTraceElement a[] = r.getStackTrace();
		for (int i = 0; i < a.length; i++) {
			String logMessage = a[i].toString();
			logger.debug("++++++++ at " + logMessage);
		}

		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Warning");
		alert.setHeaderText("There is a problem");
		alert.setContentText(String.format(formatString, args)+"\nConsult debug.log for details");

		alert.showAndWait();
	}

	public static void info(String header, String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(header);
		alert.setContentText(message);
		alert.showAndWait();
	}

	public static File directoryChooser(String title) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle(title);
		File defaultDirectory = new File("c:/Users/afree/Documents");
		chooser.setInitialDirectory(defaultDirectory);
		File selectedDirectory = chooser.showDialog(ViewManager.getPrimaryStage());
		return selectedDirectory;
	}
}
