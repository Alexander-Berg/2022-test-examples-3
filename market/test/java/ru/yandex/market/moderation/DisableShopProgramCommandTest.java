package ru.yandex.market.moderation;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.core.ds.DatasourceTransactionTemplateMock;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Vadim Lyalin
 */
@RunWith(MockitoJUnitRunner.class)
public class DisableShopProgramCommandTest {
    private DisableShopProgramCommand cmd;
    @Mock
    private ModerationService moderationService;

    @Before
    public void setUp() {
        cmd = new DisableShopProgramCommand(moderationService, new DatasourceTransactionTemplateMock());
    }

    @Test
    public void correctExecute() {
        String[] args = {"CPA", "1,2,3"};
        cmd.execute(new CommandInvocation("name", args, Collections.emptyMap()), new Terminal(System.in, System.out) {
            @Override
            protected void onStart() {
            }

            @Override
            protected void onClose() {
            }
        });

        Stream.of(1, 2, 3).forEach(shopId -> verify(moderationService).disableShopProgram(
                new ShopActionContext(DatasourceTransactionTemplateMock.MOCK_ACTION_ID, shopId), ShopProgram.CPA)
        );
        verifyNoMoreInteractions(moderationService);
    }
}
