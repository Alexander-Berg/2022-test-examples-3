/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 17.02.2007
 * Time: 18:24:49
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.hello;

import ru.yandex.common.magic.defender.Defender;
import ru.yandex.common.magic.defender.DefenderData;
import ru.yandex.common.magic.defender.DefenderException;

/**
 * @author ashevenkov
 */
public class HelloDefender implements Defender {

    public void doServiceMethodCheck(DefenderData data) {
        String name = (String) data.getParam("name");
        if (name.equals("Bill Gates")) {
            throw new DefenderException("We woudn't say hello to Bill Gates NEVER!!!");
        }
    }
}
