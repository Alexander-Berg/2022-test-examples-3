package nullablefield;

import javax.annotation.Nullable;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuramalinov
 * @created 02.10.18
 */
@RestController
public class NullableFieldController {
    @RequestMapping("/api/call")
    public void makeCall(Input input) {
    }

    public static class Input {
        @Nullable
        private String field;
        private String getter;
        private String setterIsNotEnough;

        public String getField() {
            return field;
        }

        public Input setField(String field) {
            this.field = field;
            return this;
        }

        @Nullable
        public String getGetter() {
            return getter;
        }

        public Input setGetter(String getter) {
            this.getter = getter;
            return this;
        }

        public String getSetterIsNotEnough() {
            return setterIsNotEnough;
        }

        public Input setSetterIsNotEnough(@Nullable String setterIsNotEnough) {
            this.setterIsNotEnough = setterIsNotEnough;
            return this;
        }
    }
}
