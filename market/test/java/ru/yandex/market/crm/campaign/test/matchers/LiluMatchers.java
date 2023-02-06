package ru.yandex.market.crm.campaign.test.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.crm.core.domain.PageInfo;

/**
 * @author apershukov
 */
public final class LiluMatchers {

    public static Matcher<PageInfo> pageInfo(Integer pageCount, int pageNumber, int pageSize) {
        return new PageInfoMatcher(pageCount, pageNumber, pageSize);
    }
}
