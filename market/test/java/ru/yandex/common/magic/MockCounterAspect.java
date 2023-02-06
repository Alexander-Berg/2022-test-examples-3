/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 05.03.2007
 * Time: 22:47:52
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author ashevenkov
 */
@Configurable
@Aspect
public class MockCounterAspect {

    private boolean gotcha = false;
    private boolean isCalled = false;

    public void setGotcha(boolean gotcha) {
        this.gotcha = gotcha;
    }

    @Before("target(com.google.gwt.user.client.rpc.RemoteService) && !initialization(new(..)) && execution(public * *(..))")
    public void doCheck(JoinPoint joinPoint) {
        isCalled = true;
        if (joinPoint.getSignature().getName().equals("sayHello")) {
            gotcha = true;
        }
    }

    public boolean isGotcha() {
        return gotcha;
    }

    public boolean isCalled() {
        return isCalled;
    }

    public void setCalled(boolean called) {
        isCalled = called;
    }
}
