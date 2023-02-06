package ru.yandex.market.clab.common.service.good;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.yandex.market.clab.common.service.barcode.SsBarcodeRepository;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.01.2019
 */
public class GoodServiceActionSourceTest {

    private GoodService goodService;
    private GoodRepositoryStub goodRepository;

    @Before
    public void before() {
        goodRepository = spy(new GoodRepositoryStub());
        goodService = new GoodServiceImpl(goodRepository,
            mock(SsBarcodeRepository.class));
    }

    @After
    public void after() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void saveWithAuthIsUser() {
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        Good good = goodService.createGood(new Good());

        goodService.updateGoodState(good.getId(), GoodState.ACCEPTED);

        verify(goodRepository, times(2)).save(anyList(), eq(ActionSource.USER));
    }

    @Test
    public void saveWithAuthIsRobot() {
        Good good = goodService.createGood(new Good());

        goodService.updateGoodState(good.getId(), GoodState.NOT_RECEIVED);

        verify(goodRepository, times(2)).save(anyCollection(), eq(ActionSource.ROBOT));
    }

}
