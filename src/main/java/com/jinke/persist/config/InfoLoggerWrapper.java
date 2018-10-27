package com.jinke.persist.config;

public class InfoLoggerWrapper implements InfoLogger {
    private InfoLogger infoLogger;

    public InfoLoggerWrapper(InfoLogger infoLogger) {
        this.infoLogger = infoLogger;
    }

    @Override
    public void info(String msg) {
        if (infoLogger == null) return;
        infoLogger.info(msg);
    }
}
