package com.assessment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentServiceLoggerTest {

    @Test
    void testLoggerMethods() {
        AssessmentServiceLogger logger = new AssessmentServiceLogger(AssessmentServiceLoggerTest.class.getName());

        assertNotNull(logger);

        // Call all log methods to exercise them
        logger.debug("debug message");
        logger.info("info message");
        logger.warn("warn message");
        logger.trace("trace message");
        logger.performance("perf message");

        Exception exception = new RuntimeException("test exception");

        logger.error(exception);
        logger.error("error with message", exception);
        logger.fatal(exception);

        // Flags
        logger.isDebugEnabled();
        logger.isInfoEnabled();
        logger.isTraceEnabled();
    }
}
