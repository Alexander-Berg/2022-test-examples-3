package ru.yandex.market.sc.internal.test;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

public class Template {

    private final Map<String, String> variables = new HashMap<>();
    private final String template;

    public Template(String template) {
        this.template = template;
    }

    public static Template fromFile(String filepath) {
        String body = ScTestUtils.fileContent(filepath);
        return new Template(body);
    }

    public Template setValue(String name, String value) {
        this.variables.put(name, value);
        return this;
    }

    public String resolve() {
        StringSubstitutor sub = new StringSubstitutor(variables);
        String body = sub.replace(template);
        validateSubstitutions(body);
        return body;
    }

    private void validateSubstitutions(String body) {
        if (body.contains("${")) {
            if (body.indexOf("}", body.indexOf("${")) != -1) {
                throw new IllegalStateException("Not all variables were set for template:\n" + body);
            }
        }
    }
}
