package ru.yandex.market.pers.tvm.spring

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.yandex.market.pers.test.common.PersTestMocksHolder
import ru.yandex.market.pers.tvm.TvmChecker

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.03.2021
 */
@Configuration
open class TvmCheckerConfiguration {
    @Bean
    open fun tvmChecker(): TvmChecker {
        return PersTestMocksHolder.registerMock(TvmChecker::class.java)
    }

    @Bean
    open fun tvmWebMvcConfig(tvmChecker: TvmChecker?): TvmWebMvcConfig {
        return TvmWebMvcConfig(tvmChecker)
    }

    @Bean
    open fun tvmCheckMvcMock(): TvmCheckMvcMock {
        return TvmCheckMvcMock()
    }

    @RestController
    @TvmProtected
    @RequestMapping("/test/tvm")
    open class ControllerWithTvmCheck {
        @GetMapping(value = ["/method"])
        fun method(): String {
            return "{\"status\":\"method is executed\"}"
        }

        @TvmIgnored
        @GetMapping(value = ["/methodWithoutTvm"])
        fun methodWithoutTvm(): String {
            return "{\"status\":\"method executed well\"}"
        }

        @GetMapping(value = ["/methodErr"])
        fun methodError(): ResponseEntity<String> {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("{\"status\":\"ruined\"}")
        }
    }

    @RestController
    @TvmProtected
    @RequestMapping("/test/notvm")
    open class ControllerWithoutTvmCheck {
        @TvmIgnored
        @GetMapping(value = ["/method"])
        fun method(): String {
            return "{\"status\":\"method executed well - no tvm required\"}"
        }
    }

    @RestController
    @TvmProtected(trustedNames = ["trusted_client", "another_client"])
    @RequestMapping("/test/tvm/restricted")
    open class ControllerWithRestrictedTvmCheck {
        @GetMapping(value = ["/method"])
        fun method(): String {
            return "{\"status\":\"restricted well\"}"
        }

        @TvmProtected(trustedNames = ["secure_client"])
        @GetMapping(value = ["/method2"])
        fun methodChanged(): String {
            return "{\"status\":\"restricted 2 well\"}"
        }
    }

    @Service
    @ControllerAdvice
    open class ErrorHandler {
        @ExceptionHandler
        @ResponseBody
        @ResponseStatus(HttpStatus.FORBIDDEN)
        fun handle(exception: AccessDeniedException): String {
            return "{\"status\":\"tvm error: " + exception.message + "\"}"
        }
    }
}