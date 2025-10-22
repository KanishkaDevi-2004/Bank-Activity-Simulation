package com.bank.util;

import org.apache.log4j.PropertyConfigurator;

public class LoggerUtil {
    static {
        PropertyConfigurator.configure("src/main/resources/log4j.properties");
    }

    // Prevent instantiation
    private LoggerUtil() {}
}
