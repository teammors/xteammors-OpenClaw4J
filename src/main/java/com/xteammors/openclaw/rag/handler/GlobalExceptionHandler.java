package com.xteammors.openclaw.rag.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception e) {
        log.error("global exception", e);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "The file size exceeds the limit");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.badRequest().body(response);
    }
}
