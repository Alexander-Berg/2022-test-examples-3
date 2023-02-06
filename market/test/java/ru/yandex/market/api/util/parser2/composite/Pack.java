package ru.yandex.market.api.util.parser2.composite;

import java.util.Objects;

import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.util.parser2.validation.ObjectValueValidator;

/**
 * @author dimkarp93
 */
public class Pack {
    static final String PARAM1 = "param1";
    static final String PARAM2 = "param2";

    static final String TEMPLATE = "first must be less";


    static class Entity {
        private int param1;
        private int param2;

        public Entity(int param1, int param2) {
            this.param1 = param1;
            this.param2 = param2;
        }

        public int getParam1() {
            return param1;
        }

        public int getParam2() {
            return param2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entity)) {
                return false;
            }
            Entity entity = (Entity) o;
            return param1 == entity.param1 &&
                param2 == entity.param2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(param1, param2);
        }
    };

    static class Mutable {
        private int param1;
        private int param2;

        public Mutable() {
        }

        public int getParam1() {
            return param1;
        }

        public int getParam2() {
            return param2;
        }

        public void setParam1(int param1) {
            this.param1 = param1;
        }

        public void setParam2(int param2) {
            this.param2 = param2;
        }

        public Entity build() {
            return new Entity(param1, param2);
        }
    };

    static final class FirstLessSecondValidator implements ObjectValueValidator<Mutable> {
        @Override
        public ValidationError validate(Mutable mutable) {
            return mutable.getParam1() < mutable.getParam2() ? null : new ValidationError(TEMPLATE);
        }
    }
}
