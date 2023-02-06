package converters;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.yandex.market.springmvctots.EnumConverter;
import ru.yandex.market.springmvctots.annotations.TsConverterMethodName;
import ru.yandex.market.springmvctots.annotations.TsExportEnumConverter;

@Controller
class ConvertersController {
    @PostMapping("/api/json")
    public String request(@RequestParam("theBody") ComplexObject body) {
        return null;
    }

    public static class ComplexObject {
        public List<Status> getComplexField() {
            return null;
        }
    }

    public enum Status implements HasDisplay {
        OPEN("Открыто", 1, true), CLOSED("Закрыто", -1, false), NOT_AVAILABLE("Не обслуживается", 404, false);

        private final String display;
        private final int code;
        private final boolean acceptItems;

        Status(String display, int code, boolean acceptItems) {
            this.display = display;
            this.code = code;
            this.acceptItems = acceptItems;
        }

        @Override
        public String getDisplay() {
            return display;
        }

        public static class StatusName extends EnumDisplay<Status> {
        }

        public int getCode() {
            return code;
        }

        @TsExportEnumConverter
        public boolean isAcceptItems() {
            return acceptItems;
        }
    }

    public interface HasDisplay {
        String getDisplay();
    }

    @TsConverterMethodName("apiStatusCode")
    public static class StatusCode implements EnumConverter<Status, Integer> {
        @Override
        public Integer convert(Status value) {
            return value.getCode();
        }
    }


    public abstract static class EnumDisplay<T extends Enum<T> & HasDisplay> implements EnumConverter<T, String> {
        public String convert(T value) {
            return value.getDisplay();
        }
    }
}
