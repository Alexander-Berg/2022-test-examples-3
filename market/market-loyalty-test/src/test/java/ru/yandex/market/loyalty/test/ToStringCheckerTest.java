package ru.yandex.market.loyalty.test;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.market.loyalty.test.ToStringChecker.missedFields;

public class ToStringCheckerTest {

    @Test
    @Ignore
    public void shouldHandleAllSubclassesInThisClass() throws IOException {
        assertThat(
                missedFields(),
                containsInAnyOrder(
                        containsString(NotValidClassWithSuper.class.getSimpleName()),
                        containsString(NotValidClassWithMissingField.class.getSimpleName()),
                        containsString(NotValidClassWithFieldSimilarFieldNames.class.getSimpleName())
                )
        );
    }

    static class ValidClassWithSuper extends Supper {
        private int field;

        @Override
        public String toString() {
            return "ClassWithSuper{" +
                    "field=" + field +
                    '}' + super.toString();
        }
    }

    static class NotValidClassWithSuper extends Supper {
        private int field;

        @Override
        public String toString() {
            return "ClassWithSuper{" +
                    "field=" + field +
                    '}';
        }
    }

    static class Supper {
        private int superFiled;

        @Override
        public String toString() {
            return "Supper{" +
                    "superFiled=" + superFiled +
                    '}';
        }
    }

    static class NotValidClassWithMissingField {
        private int first;
        private int second;

        @Override
        public String toString() {
            return "NotValidClassWithMissingField{" +
                    "first=" + first +
                    '}';
        }
    }

    static class NotValidClassWithFieldSimilarFieldNames {
        private int status;
        private boolean statusUpdated;

        @Override
        public String toString() {
            return "NotValidClassWithFieldSimilarFieldNames{" +
                    "statusUpdated=" + statusUpdated +
                    '}';
        }
    }

}
