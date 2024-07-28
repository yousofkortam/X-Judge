package com.xjudge.exception;

import lombok.*;

import java.util.Map;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationExceptionModel extends ExceptionModel{
    Map<String , String> errors;
    public ValidationExceptionModel(int statusCode, String message, String timeStamp, Map<String, String> errors) {
        super(statusCode, message, timeStamp);
        this.errors = errors;
    }
}
