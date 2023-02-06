package ru.yandex.mail.common;

import java.io.IOException;
import java.lang.reflect.Method;

import com.google.common.reflect.ClassPath;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.junit.Test;

public class ListTests {
    public static void main(String[] args) {
        OptionParser parser = new OptionParser();

        ArgumentAcceptingOptionSpec<String> packageName = parser.accepts("package", "Package name with tests")
                .withRequiredArg().required();

        try {
            OptionSet options = parser.parse(args);

            String name = options.valueOf(packageName);

            ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());

            for(ClassPath.ClassInfo info : cp.getTopLevelClassesRecursive(name)) {
                if (info.getName().endsWith("Test")) {
                    for (Method m : info.load().getMethods()) {
                        if (m.getAnnotation(Test.class) != null) {
                            System.out.println(info.getName());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException io) {

            }

            e.printStackTrace(System.err);
            System.exit(1);
        }

        System.exit(0);
    }

}
