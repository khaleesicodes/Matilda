package org.khaleesicodes.utils;

import org.khaleesicodes.AgentMatilda;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.*;

public class MatildaLogger {
    static Logger logger = Logger.getLogger(MatildaLogger.class.getName());

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream("LoggingConfig.properties"));
        } catch (SecurityException | IOException e1) {
            e1.printStackTrace();
        }
        logger.setLevel(Level.FINE);
        logger.addHandler(new ConsoleHandler());

        try {
            //FileHandler file name with max size and number of log files limit
            Handler fileHandler = new FileHandler("/Users/khaleesi/Bachelor/Matilda/matilda/Logs/log.log", 2000, 5);

            fileHandler.setFormatter(new MatildaFormatter());

            logger.addHandler(fileHandler);

            for(int i=0; i<1000; i++){
                //logging messages
                logger.log(Level.INFO, "Msg"+i);
            }
            logger.log(Level.CONFIG, "Config data");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

}
