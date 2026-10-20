package com.levantare.pricing.service;

/** A rule violation the user can fix (maps to HTTP 409 / 400). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
