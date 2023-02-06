package ru.yandex.market.common.test.guava;

import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Supplier;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Подменяет реализацию {@link com.google.common.base.Suppliers#memoizeWithExpiration(Supplier, long, TimeUnit)
 * Suppliers#memoizeWithExpiration()}, чтобы кэш ответа не работал вообще.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@ParametersAreNonnullByDefault
public class ForgetfulSuppliersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static void instrumentGuavaSuppliers() {
        try {
            ByteBuddyAgent.install();
            new ByteBuddy()
                    .redefine(Class.forName("com.google.common.base.Suppliers$ExpiringMemoizingSupplier"))
                    .method(ElementMatchers.named("get"))
                    .intercept(MethodDelegation.toField("delegate"))
                    .make()
                    .load(Thread.currentThread().getContextClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        instrumentGuavaSuppliers();
    }

}
