package com.bank.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {

    // Returns a logger for a given class
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    // Prevent instantiation
    private LoggerUtil() {}
}
