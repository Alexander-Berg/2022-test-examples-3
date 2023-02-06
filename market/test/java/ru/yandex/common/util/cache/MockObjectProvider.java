/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 16.09.2006</p>
 * <p>Time: 14:49:52</p>
 */
package ru.yandex.common.util.cache;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
class MockObjectProvider implements ObjectProvider<String, String> {
    private int callCounter = 0;

    public String getData(final String key) {
        callCounter++;
        return "test";
    }

    public int getCallCounter() {
        return callCounter;
    }
}
