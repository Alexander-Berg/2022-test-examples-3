package ru.yandex.market.mapi.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.02.2022
 */
@RestController
class TestControllerStrangeOther {

    // non-unique name, but unique method -> unique pageId
    @RequestMapping("/abc3", method = [RequestMethod.PATCH])
    fun testMethod() {
    }

}