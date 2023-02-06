package ru.yandex.market.crm.campaign.test.utils;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.tag.Tag;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.util.LengthLimitingStringBuilder;

/**
 * Простой сервис для рендеринга Jinja2-шаблонов, который поддерживает часть фич Рассылятора.
 * Мы не даём гарантий, что результат рендеринга соответствует рендерингу в Рассыляторе, но,
 * хотя бы, он не будет падать, если закидывать ему на рендеринг наши шаблоны со специфичными
 * для Рассылятора тегами.
 *
 * @author zloddey
 */
public class JinjavaSender extends Jinjava {
    public JinjavaSender() {
        super();
        getGlobalContext().registerTag(new TransparentTag("opens_counter", null));
        getGlobalContext().registerTag(new TransparentTag("wrap", "endwrap"));
    }

    /**
     * Во время рендеринга убирает открывающий тег tagName и закрывающий тег endTagName, но рендерит всё,
     * что находится между ними. Если endTagName равен null, то просто игнорирует встречающийся tagName.
     */
    private static class TransparentTag implements Tag {

        private final String tagName;
        private final String endTagName;

        private TransparentTag(String tagName, String endTagName) {
            this.tagName = tagName;
            this.endTagName = endTagName;
        }

        @Override
        public String interpret(TagNode tagNode, JinjavaInterpreter interpreter) {
            try (JinjavaInterpreter.InterpreterScopeClosable ignored = interpreter.enterScope()) {
                LengthLimitingStringBuilder result =
                        new LengthLimitingStringBuilder(interpreter.getConfig().getMaxOutputSize());

                for (Node child : tagNode.getChildren()) {
                    result.append(child.render(interpreter));
                }

                return result.toString();
            }
        }

        @Override
        public String getName() {
            return tagName;
        }

        @Override
        public String getEndTagName() {
            return endTagName;
        }
    }
}
