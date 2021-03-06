/*
 *  Copyright (C) Alan Freeman 2017-2019
 *  
 *  This file is part of the Capitalism Simulation, abbreviated to CapSim
 *  in the remainder of this project
 *
 *  Capsim is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either project of the License, or
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
package capitalism.view.command;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import capitalism.controller.Simulation;
import capitalism.model.OneProject;
import capitalism.reporting.Dialogues;
import capitalism.utils.DBHandler;
import capitalism.utils.XMLStuff;
import capitalism.view.custom.ImageButton;

public class DumpCommand implements DisplayCommand{
	private static final Logger logger = LogManager.getLogger(DumpCommand.class);

	@Override public void execute(ImageButton caller) {
		logger.debug("User saved the current project");
		OneProject oneProject= new OneProject();
		oneProject.wrap(Simulation.projectIDcurrent());
		File file=Dialogues.saveFileChooser("Where should this project be saved?");
		if (file == null) return;
		logger.debug("Saving new project to {}", file.getAbsolutePath());
		XMLStuff.exportToXML(oneProject,file);
		File saveDirectory = Dialogues.directoryChooser("Temporary CSV dump directory");
		DBHandler.saveCSVDataBase(saveDirectory);//temporary belt and braces check
	}

}
