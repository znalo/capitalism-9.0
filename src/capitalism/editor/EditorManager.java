/*
 *  capitalism.view.editoreman 2017-2019
 *  
 *  This file is part of the Capitalism Simulation, abbreviated to CapSim
 *  in thEditorManagerf this project
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import capitalism.utils.Reporter;
import capitalism.view.ViewManager;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class EditorManager {
	private final static Logger logger = LogManager.getLogger("EditorManager");

	private static Stage editorStage = null;
	private static Scene editorScene;
	private static Editor editor;;

	public static void buildEditorWindow() {
		Reporter.report(logger, 0, "Create Editor Window");
		editorStage = new Stage();
		editor = new Editor();
		editorScene = new Scene(editor, ViewManager.windowWidth, ViewManager.windowHeight);
		
		// style the Editor window (and in particular, the table columns)
		String css = Editor.class.getResource("/SimulationTheme.css").toExternalForm();
		editorScene.getStylesheets().add(css);

		// get ready to show the window
		editorStage.setScene(editorScene);
		
		// at present, when the editor fires up, we load the current project from the simulation.
		// this could change eg we could have a button in the Editor to do it or a combo box
		// to select the simulation
		EditorLoader.loadFromSimulation();

		// as soon as the editor window is shown, resize the tables to fit the space
		// that was allocated to them, showing only the rows that exist
		editorStage.setOnShown(collectHeights);
		// TODO take action when the editor window is resized
	}
	
	/**
	 * Show the editor window
	 */
	
	public static void showEditorWindow() {
		editorStage.showAndWait();
	}
	
	/**
	 * Close the editor window
	 */
	public static void closeEditorWindow() {
		editorStage.close();
	}

	/**
	 * Sizes the tables within the window, once it is shown (so that we know its dimensions)
	 */
	private static EventHandler<WindowEvent> collectHeights= new EventHandler <WindowEvent> (){
		@Override public void handle(WindowEvent t) {
		Editor.getHeights();
		Editor.setHeights();
		}
	};
}
