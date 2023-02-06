/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 30.11.2007
 * Time: 14:06:01
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.framework;

import java.util.Date;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author ashevenkov
 */
public class JavaTest extends AbstractDependencyInjectionSpringContextTests {

    private Boo boo;

    public void setBoo(Boo boo) {
        this.boo = boo;
    }

    public JavaTest() {
        setAutowireMode(AUTOWIRE_BY_NAME);
    }

    protected String[] getConfigLocations() {
        return new String[]{"classpath:config/aop-test.xml"};
    }

    public void testAop() throws Exception {
        DefaultMoo moo = new DefaultMoo();
        long l1 = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            foo(boo);
        }
        long l2 = System.currentTimeMillis();
        System.out.println("l2 - l1 = " + (l2 - l1));
        for (int i = 0; i < 100000000; i++) {
            goo(moo);
        }
        long l3 = System.currentTimeMillis();
        System.out.println("l3 - l2 = " + (l3 - l2));
        for (int i = 0; i < 100000000; i++) {
            doo(moo);
        }
        long l4 = System.currentTimeMillis();
        System.out.println("l4 - l3 = " + (l4 - l3));
    }

    private void foo(Boo boo) {
        boo.doJob();
    }

    private void goo(Moo boo) {
        boo.predoJob();
        boo.doJob();
    }

    private void doo(Boo boo) {
        if (boo instanceof Moo) {
            Moo moo = (Moo) boo;
            moo.predoJob();
            moo.doJob();
        }
    }

    public void testA() throws Exception {
        System.out.println(new Date(1222811355000L));
    }
}
