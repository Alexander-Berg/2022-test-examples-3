/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 06.06.2006</p>
 * <p>Time: 13:48:58</p>
 */
package ru.yandex.common.framework.xml;

/**
 * to test xml builders
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class MockObject {
    private final String name;
    private final long age;
    private final double weight;
    private final String description;

    public MockObject(
            final String name, final long age, final double weight, final String description) {
        this.name = name;
        this.age = age;
        this.weight = weight;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public long getAge() {
        return age;
    }

    public double getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }

}
