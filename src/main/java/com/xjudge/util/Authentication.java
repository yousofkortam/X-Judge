package com.xjudge.util;

import com.xjudge.exception.UnauthenticatedException;

public class Authentication {

    public static void checkAuthentication(Object connectedUser) {
        if (connectedUser == null) {
            throw new UnauthenticatedException("You need to be authenticated to perform this operation");
        }
    }

}
