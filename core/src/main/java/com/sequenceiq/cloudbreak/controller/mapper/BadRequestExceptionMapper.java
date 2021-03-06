package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;

import ch.qos.logback.classic.Level;

@Component
public class BadRequestExceptionMapper extends SendNotificationExceptionMapper<BadRequestException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }
}