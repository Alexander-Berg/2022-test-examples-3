package ru.yandex.market.crm.campaign.test.matchers;

import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import ru.yandex.market.crm.core.domain.PageInfo;

/**
 * @author apershukov
 */
public class PageInfoMatcher extends BaseMatcher<PageInfo> {

    private final Integer pageCount;
    private final int pageNumber;
    private final int pageSize;

    public PageInfoMatcher(Integer pageCount, int pageNumber, int pageSize) {
        this.pageCount = pageCount;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof PageInfo)) {
            return false;
        }

        PageInfo pageInfo = (PageInfo) o;
        return Objects.equals(pageInfo.getPageCount(), pageCount) &&
                pageInfo.getPageNumber() == pageNumber &&
                pageInfo.getPageSize() == pageSize;
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(this.toString());
    }

    @Override
    public String toString() {
        return "PageInfoMatcher{" +
                "pageCount=" + pageCount +
                ", pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                '}';
    }
}
