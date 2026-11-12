/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package elius.virtualoperator.task.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import elius.webapp.framework.properties.PropertiesManager;
import elius.webapp.framework.properties.PropertiesManagerFactory;
import elius.virtualoperator.task.Task;
import elius.virtualoperator.task.TaskProcess;
import elius.virtualoperator.task.TaskStatus;


/**
 * ShellProcess.
 * <p>
 * Task that execute a shell.
 */
public class ShellProcess implements TaskProcess {
	
	// Get logger
	private static Logger logger = LogManager.getLogger(ShellProcess.class);
	
	// Process Result
	private String pResult;
	
	// Task Status
	private TaskStatus tStatus;
	
	/**
	 * Constructor
	 */
	public ShellProcess() {
        super();
    }

	@Override
    public String getName() {
        return "EVO_SHELL";
    }

	@Override
    public String getDescription() {
        return "Shell Process for Elius Virtual Operator";
    }


    @Override
    public String getVersion() {
        return "0.0.2";
    }    
	
	@Override
	public void execute(Task task) {

		logger.traceEntry("task={}", task.toString());

		// Application properties
		PropertiesManager appProperties = PropertiesManagerFactory.getInstance(ShellAttributes.DEFAULT_SHELL_PROPERTIES_FILE);

		// Load shell properties
		if(!appProperties.isLoaded()) {
			// Set error
			tStatus = TaskStatus.ERROR;
			// Set error message
			pResult = "Error loading properties";
			// Log message
			logger.error("Error loading " + ShellAttributes.DEFAULT_SHELL_PROPERTIES_FILE + " properties file");
			// Exit
			return;
		}
		
		// Get path where scripts are stored
		String pathScripts = appProperties.get(ShellAttributes.PROP_TASK_SHELL_SCRIPTS_PATH);
		
		// Get the working directory
		String pathWork = appProperties.get(ShellAttributes.PROP_TASK_SHELL_WORK_PATH);
		
		// Check if EVO is running on windows
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

		// Process Builder instance
		ProcessBuilder builder = new ProcessBuilder();


		// Create script path
		Path scriptPath = Path.of(pathScripts, task.getDetails());

		// Set command with the right settings
		if (isWindows) {
			// Set command for windows
			builder.command("C:\\Windows\\System32\\cmd.exe", "/c", scriptPath.toString());
		} else {
			// Set command on nix systems
			builder.command("/bin/sh", "-c", scriptPath.toString());
		}

		// Set working directory
		builder.directory(new File(pathWork));
		
		try {
			// Redirect standard error to standard output
			builder.redirectErrorStream(true);
			
			// Create the process
			Process process = builder.start();

			StringBuilder output = new StringBuilder();

			// Read output
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
			}

			// Wait for exit code
			int exitCode = process.waitFor(); 
			
			// Assign the oputput
			pResult = output.toString();

			// Set exit code
			if (0 == exitCode) {
				tStatus = TaskStatus.ENDED;
			} else {
				tStatus = TaskStatus.ERROR;

				if (pResult.isEmpty()) {
					pResult = "Process failed with exit code " + exitCode;
				}
			}

		} catch (Exception e) {
			pResult = "Generic error, check system log: " + e.getMessage();
			logger.error(pResult, e);
			tStatus = TaskStatus.ERROR;
			Thread.currentThread().interrupt();
		}
		
		logger.traceExit("status={}", tStatus);
	}

	
	@Override
	public String getProcessResult() {
		return pResult;
	}

	
	@Override
	public TaskStatus getTaskStatusResult() {
		return tStatus;
	}
	
}