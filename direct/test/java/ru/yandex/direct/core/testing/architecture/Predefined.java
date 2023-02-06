package ru.yandex.direct.core.testing.architecture;

import java.util.regex.Pattern;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;

public class Predefined {
    public static class IgnoreTestClasses implements ImportOption {
        private final Pattern testClassPattern = Pattern.compile(".*Test(Kt)?(\\$[^.]++)?\\.class$");
        private final Pattern testPackagePattern = Pattern.compile(".*/test\\w*/.*");

        public IgnoreTestClasses() {
        }

        @Override
        public boolean includes(Location location) {
            return !(location.matches(testClassPattern) || location.matches(testPackagePattern));
        }
    }
}
