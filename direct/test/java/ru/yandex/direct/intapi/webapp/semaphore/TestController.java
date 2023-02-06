package ru.yandex.direct.intapi.webapp.semaphore;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

@Controller
public class TestController {

    @RequestMapping(value = "/testLock")
    @Semaphore(permits = 1, key = "test")
    public void testLock() {
    }

    @RequestMapping(value = "/testNotLock")
    public void testNotLock() {
    }

    public HandlerMethod getHandlerForLockedMethod() {
        try {
            return new HandlerMethod(this, this.getClass().getMethod("testLock"));
        } catch (NoSuchMethodException e) {
            //cannot be
        }
        return null;
    }

    public HandlerMethod getHandlerForNotLockedMethod() {
        try {
            return new HandlerMethod(this, this.getClass().getMethod("testNotLock"));
        } catch (NoSuchMethodException e) {
            //cannot be
        }
        return null;
    }
}
