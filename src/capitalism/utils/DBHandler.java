/*
 *  Copyright (C) Alan Freeman 2017-2019
 *  
 *  This file is part of the Capitalism SimulationManager, abbreviated to CapSim
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
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import capitalism.Capitalism;
import capitalism.reporting.Dialogues;
import capitalism.reporting.Reporter;

/**
 * This class handles the interaction with the H2 database. It is encapsulated, so
 * another JPA-compliant database can be switched in here and, hopefully, only the SQL
 * initialising commands need to be changed.
 * TODO implement the SQL commands with JQL
 */

public class DBHandler {
	private static final Logger logger = LogManager.getLogger(DBHandler.class);
	private static Connection conn;

	/**
	 * a list of all the help files to be exported to the user help directory
	 */
	private static String helpFiles[] = {
			"commodityHelp.html",
			"industryHelp.html",
			"socialClassHelp.html",
			"start.png",
			"unhelp.png",
			"edit.png",
			"help.png",
			"littlePlus.png",
	};

	/**
	 * a list of all the data files to be exported to the user data directory
	 */
	private static String dataFiles[] = {
			"industries.csv",
			"socialClasses.csv",
			"projects.csv",
			"stocks.csv",
			"timeStamps.csv",
			"commodities.csv",
			"CreateRawTables.sql"
	};

	public DBHandler() {
	}

	/**
	 * reinitialise the whole database with the opening SQL queries and the user data
	 * assumes the connection is already open
	 * 
	 */
	public void restart() {
		try {
			conn.close();
		} catch (SQLException s) {
			Dialogues.alert(logger, "Sorry, we could not re-start because we were\n"
					+ "unable to close the database that is already open.");
			return;
		}
		openDatabase();
	}

	/**
	 * load a new database in csv format from a specified location
	 * 
	 * @param dataFileDirectory
	 *            the full path to the directory in which the data files are located. If in NIX format (with '\' instead of '/') this is converted to Windows
	 *            format.
	 */
	public static void loadCSVDatabase(String dataFileDirectory) {
		dataFileDirectory = dataFileDirectory.replace('\\', '/');
		try {
			conn.close();
		} catch (SQLException s) {
			Dialogues.alert(logger, "Sorry, we could not re-start because we were\n"
					+ "unable to close the database that is already open.");
			return;
		}
		try {
			Class.forName("org.h2.Driver");// is this necessary?
			String queryFirstPart = "jdbc:h2:mem:capitalism;INIT=RUNSCRIPT FROM '";
			String queryLastPart = "/CreateRawTables.sql'";
			String queryWhole = queryFirstPart + dataFileDirectory + queryLastPart;
			logger.debug("Attempting to connect to the database using URL {} ", queryWhole);
			conn = DriverManager.getConnection(queryWhole, "sa", "");
			Reporter.report(logger, 0, "Successfully loaded the data located at %s", dataFileDirectory);
		} catch (Exception e) {
			Dialogues.alert(logger, "Could not load the data because:\n" + e.getMessage());
		}
	}

	/**
	 * create the connection and execute the initialization file
	 * 
	 * @return true if successful, false otherwise
	 */
	private static boolean openDatabase() {
		try {
			Class.forName("org.h2.Driver");
			String urlPath = "jdbc:h2:mem:capitalism";
			logger.debug("Attempting to connect to the database using URL {} ", urlPath);
			conn = DriverManager.getConnection("jdbc:h2:mem:capitalism;INIT=RUNSCRIPT FROM '~/Documents/Capsim/data/CreateRawTables.sql'", "sa", "");
			logger.debug("Successful connection to the H2 database");
			return true;
		} catch (SQLException s) {
			logger.error("H2 Connection to the database failed because\n" + s.getMessage());
			return false;
		} catch (ClassNotFoundException c) {
			logger.error("SQL class not found. the exception handler says:/n" + c.getMessage());
			return false;
		} catch (Exception e) {
			logger.error("H2Database initialization files could not be loaded because:\n" + e.getMessage());
			return false;
		}
	}

	/**
	 * copy a file from the .jar file into the user file system. The base directory for these files in the user system is 
	 * {@code Utilities.getUserBasePath()} and is set there statically
	 * 
	 * for example {@code copyDataFilesToUserDirectory("/data","commodities.csv")} copies the file called {@code usecommodities.csv} to the location
	 * {@code Documents/Capsim/data/commodities.csv}, because the user base is initialised in {@code Utilities} to be {@code Documents/Capsim}
	 * 
	 * @param subDirectory
	 *            the sub-directory of our standard location into which the file should be copied
	 * @param resource
	 *            the file name of the resource, with no path
	 * @param basePath
	 *            the absolute path to the main directory in which subDirectory is to be found
	 * 
	 */
	public static void copyFileToUserDirectory(String basePath, String subDirectory, String resource) {
		URL inputUrl = DBHandler.class.getClassLoader().getResource(resource);
		String userDestinationFile = basePath + subDirectory + resource;
		File dest = new File(userDestinationFile);
		logger.debug("Copying the file called {} to the user file system at location {}", resource, userDestinationFile);
		try {
			// TODO trap file does not exist
			FileUtils.copyURLToFile(inputUrl, dest);
		} catch (IOException e) {
			e.printStackTrace();
			logger.debug("Failure");
		}
	}

	/**
	 * Copy all the data files into a standardised directory in the user's file system.
	 * This is the easiest and most robust way I could think of to work around all the difficulties
	 * associated with accessing data files in an exported .jar file. Needs research.
	 * 
	 * @return true if successful, false otherwise
	 */
	public static boolean exportDataFiles() {
		try {
			for (String file:dataFiles) {
				copyFileToUserDirectory(Capitalism.getUserBasePath(), "data/", file);
			}
			return true;
		} catch (RuntimeException e) {
			logger.debug("Error copying data files:%s", e.getMessage());
			return false;
		}
	}

	/**
	 * Export the help files and images to the user directory
	 * 
	 * @return false if fail
	 */
	public static boolean ExportHelpFiles() {
		try {
			for (String file : helpFiles) {
				copyFileToUserDirectory(Capitalism.getUserBasePath(), "help/", file);
			}
			return true;
		} catch (RuntimeException e) {
			logger.debug("Error copying help files:%s", e.getMessage());
			return false;
		}
	}

	/**
	 * export the data files to the user directory and open them
	 * 
	 * @return true if it worked, false otherwise
	 */
	public static boolean initialiseDataBaseAndStart() {
		if (!exportDataFiles())
			return false;
		return openDatabase();
	}

	/**
	 * save the database, in its current state, to a directory as a set of CSV files
	 * 
	 * @param saveDirectory
	 *            the directory in which to save the files
	 */
	public static void saveCSVDataBase(File saveDirectory) {
		String[] standardFiles = { "timeStamps", "projects", "commodities", "stocks", "socialClasses", "industries" };
		String baseDirectoryURL;
		try {
			if (saveDirectory == null)
				return;
			baseDirectoryURL = saveDirectory.getCanonicalPath().replace('\\', '/');
		} catch (IOException e) {
			Dialogues.alert(logger, "Failed to create a meaningful name for the save directory");
			e.printStackTrace();
			return;
		}
		for (int i = 0; i < standardFiles.length; i++) {
			Reporter.report(logger, 2, "Checking whether the file called '%s' exists", standardFiles[i]);
		}
		for (int i = 0; i < standardFiles.length; i++) {
			saveOneTable(baseDirectoryURL, standardFiles[i]);
		}
		// Copy the initializer file so it can load the data files
		copyFileToUserDirectory(baseDirectoryURL, "/", "CreateRawTables.sql");
	}

	/**
	 * save one table
	 * 
	 * @param baseDirectory
	 *            the folder into which the table will be saved
	 * @param tableName
	 *            the name of the table to save, which will also be the name of the .csv file
	 */
	public static void saveOneTable(String baseDirectory, String tableName) {
		Reporter.report(logger, 1, "Saving %s to %s", tableName, baseDirectory);
		Statement s;
		try {
			s = conn.createStatement();
		} catch (SQLException e1) {
			Dialogues.alert(logger, "Could not access the database to save it");
			e1.printStackTrace();
			return;
		}
		String queryString;
		queryString = "CALL CSVWRITE('" + baseDirectory + "/" + tableName + ".csv', 'SELECT * FROM " + tableName + "');";
		Reporter.report(logger, 2, "Saving with %s", queryString);
		try {
			s.executeQuery(queryString);
		} catch (SQLException e) {
			Dialogues.alert(logger, "Could not save the file called " + tableName);
			e.printStackTrace();
			e.printStackTrace();
		}
	}
}
