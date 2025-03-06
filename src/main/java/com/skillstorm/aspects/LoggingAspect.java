package com.skillstorm.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // Pointcut for all methods in the com.skillstorm package:
    @Pointcut("within(com.skillstorm..*)")
    public void everything() {
        /* Empty Hook */
    }

    @Around("everything()")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Log method entry
        log.trace("Method entry: {}", pjp.getSignature());

        Object result;
        try {
            // Proceed with method execution
            result = pjp.proceed();
        } catch (Throwable t) {
            // Log method exception
            log.error("Method execution failed: {}", pjp.getSignature(), t);
            throw t;
        }

        long executionTime = System.currentTimeMillis() - startTime;
        // Log method exit
        log.trace("Method exit: {} (execution time: {} ms)", pjp.getSignature(), executionTime);

        return result;
    }
}
