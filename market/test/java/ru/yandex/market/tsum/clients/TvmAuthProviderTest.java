package ru.yandex.market.tsum.clients;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.tvm.TvmAuthProvider;
import ru.yandex.passport.tvmauth.exception.NonRetriableException;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 2019-03-15
 */
public class TvmAuthProviderTest {
    @Ignore // MARKETINFRA-4999
    @Test
    public void testInit() {
        //Проверяем, что клиент нормально создаётся, и нативные либы подгружаются
        try {
            //специально используем заведомо неправильный секрет
            TvmAuthProvider tvmAuthProvider = new TvmAuthProvider(42, "friend", 422184);
            tvmAuthProvider.close();
        } catch (Exception e) {
            //Из JNI можно кинуть checked exception без предупреждения!
            if (!(e instanceof NonRetriableException && e.getMessage().contains("Signature is bad: common reason is " +
                "bad tvm_secret or tvm_id\\/tvm_secret mismatch."))) {
                //если получим не исключение, говорящее о том, что у нас неправильный секрет, а что-то другое - это
                // проблема
                throw e;
            }
        }
    }
}
