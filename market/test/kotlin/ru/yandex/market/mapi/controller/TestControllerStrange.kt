package ru.yandex.market.mapi.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.02.2022
 */
@RestController
class TestControllerStrange {

    @RequestMapping("/abc", method = [RequestMethod.GET, RequestMethod.POST, RequestMethod.HEAD])
    fun testMethod() {
    }

    @RequestMapping("/abc2", method = [RequestMethod.GET])
    fun testMethod1() {
    }
}