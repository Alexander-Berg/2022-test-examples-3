package ru.yandex.core;

import com.sun.net.httpserver.Authenticator;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Created by nasyrov on 18.04.2016.
 */
public class CrmTestRunner extends BlockJUnit4ClassRunner {

    public CrmTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier) {
        notifier.addListener(new RunListener() {
            @Override
            public void testFailure(Failure failure) throws Exception {
                System.out.print(CrmApi.lastJson);

                super.testFailure(failure);
            }
        });
        super.run(notifier);
    }
}
