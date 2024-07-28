package com.xjudge.util.driverpool;
public interface DriverPool {
    WebDriverWrapper getDriverData();
    void releaseDriver(WebDriverWrapper driver);
}
