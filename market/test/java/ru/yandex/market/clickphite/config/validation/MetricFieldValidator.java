package ru.yandex.market.clickphite.config.validation;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.health.configs.clickphite.MetricType;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.12.16
 */
public class MetricFieldValidator {

    private MetricFieldValidator() {
    }

    public static void validateField(MetricField field) {
        validateField(field, Collections.emptySet());
    }

    public static void validateField(MetricField field, Set<String> allowedFieldNames) {
        if (!field.getType().equals(MetricType.SIMPLE)) {
            return;
        }

        String fieldExpression = field.getField();
        validateFieldExpression(fieldExpression, allowedFieldNames);
    }

    public static void validateFieldExpression(String fieldExpression, Set<String> allowedFieldNames) {
        List<Token> tokens = Tokenizer.getTokens(fieldExpression);

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getType() != TokenType.WORD) {
                continue;
            }

            Token nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
            if (nextToken != null && nextToken.getType() == TokenType.BRACKET && nextToken.getValue().equals("(")) {
                // it's function name
                continue;
            }

            if (allowedFieldNames.contains(token.getValue())) {
                continue;
            }

            if (!precededByOpeningBracket(tokens, i) || !followedByClosingBracket(tokens, i)) {
                throw new IllegalStateException(
                    "Field '" + token.getValue() + "' must be under aggregate function." +
                        "\nExpression: " + fieldExpression +
                        "\nTokens: " + tokens.stream().map(Token::toString).collect(Collectors.joining(", "))
                );
            }
        }
    }

    private static boolean precededByOpeningBracket(List<Token> tokens, int tokenIndex) {
        for (int i = 0; i < tokenIndex; i++) {
            Token token = tokens.get(i);
            if (token.getType() == TokenType.BRACKET && token.getValue().equals("(")) {
                return true;
            }
        }

        return false;
    }

    private static boolean followedByClosingBracket(List<Token> tokens, int tokenIndex) {
        for (int i = tokenIndex + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == TokenType.BRACKET && token.getValue().equals(")")) {
                return true;
            }
        }

        return false;
    }
}
