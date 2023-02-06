package ru.yandex.market.mcrm.db.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @see <a href="https://st.yandex-team.ru/OCRM-7491">OCRM-7491 - способ очистки базы</a>
 * @deprecated не используйте данный класс для облегчения написания тестов, которые не влияют друг на друга.
 * {@link org.springframework.test.annotation.DirtiesContext DirtiesContext} нужно использовать только в том случае,
 * когда реально загрязняется контекст приложения (т.е. меняются бины, их состояние в тестах, что потом нужно весь
 * контекст пересоздавать). Если нужно только чистить базу, то лучше очищать только ее. А лучше вообще писать
 * транзакционные тесты в одной транзакции с {@link org.springframework.test.annotation.Rollback rollback} в конце теста
 */
@Deprecated
@ExtendWith(SpringExtension.class)
@TestPropertySource("/test_db_support.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractDbTest {

    @AfterAll
    public static void dbStop() {
        TestMasterReadOnlyDataSourceConfiguration.closeAll();
    }
}
