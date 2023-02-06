package ru.yandex.market.health.configs.logshatter.sanitizer;

public class FakeSanitizer implements Sanitizer {
    public FakeSanitizer() {
    }

    @Override
    public String mask(String line) {

        return line.replaceAll("_VERY_VERY_SECRET_", "_VERY_VERY_XXXXXX_");
    }
}
