package ru.yandex.market.wms.common.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.router

@TestConfiguration
class PagematchCotrollerTestConfig {

    private var baseUrl: String = "baseUrl"
    private var anotherUrl: String = "anotherUrl"

    @Bean
    fun router(): RouterFunction<ServerResponse> {
        return router {
            baseUrl.nest {
                PUT("/upsert", accept(MediaType.APPLICATION_JSON), ::processRequest)
                GET( ::processRequest)
            }
            anotherUrl.nest {
                POST("/activate", ::processRequest)
            }
        }
    }

    fun processRequest(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().build()
    }
}
