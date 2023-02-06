package ru.yandex.market.core.util.testing;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Логирует все обращения к JdbcTemplate.
 * todo сделать конфигурируемым
 *
 * @author Antonina Mamaeva mamton@yandex-team.ru
 */
@Aspect
public class PerfomanceLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(PerfomanceLoggingAspect.class);

    private String message = "processing time for request sql_%1$s[%2$s]# is %3$d ms";

    @Around("execution(* org.springframework.jdbc.core.JdbcTemplate.*(..)) && execution(public * *(..)) "
            + "&& !execution(public * get*(..)) && !execution(public * set*(..))")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        String sArgs = (joinPoint.getArgs() == null) ? "null" :
                Arrays.toString(joinPoint.getArgs()).replaceAll("\n", "_").replaceAll(" ", "_");
        while (sArgs.indexOf("__") >= 0) {
            sArgs = sArgs.replaceAll("__", "_");
        }
        log.debug(String.format(message, String.valueOf(joinPoint.getSignature()).replaceAll(" ", "_"),
                sArgs,
                endTime - startTime));
        return result;
    }

}
