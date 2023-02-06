package ru.yandex.market.mbisfintegration.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.QueryResult;
import ru.yandex.mj.generated.client.self_client.api.SearchApiClient;
import ru.yandex.mj.generated.client.self_client.model.BaseEntity;
import ru.yandex.mj.generated.client.self_client.model.Filter;
import ru.yandex.mj.generated.client.self_client.model.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.mj.generated.client.self_client.model.SearchRequest.MetaclassEnum.DISTRIBUTIONTYPE;

class SearchApiServiceTest extends AbstractApiTest {

    private static final Account SEARCH_RESULT = new Account().withId("someId").withName("someName");

    @Autowired
    private SearchApiClient client;

    @Override
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        when(soap.query(anyString())).thenReturn(new QueryResult().withRecords(SEARCH_RESULT));
    }

    @Test
    void searchPartners() throws Exception {
        Filter filter = new Filter()
                .attribute(Filter.AttributeEnum.SHOPID)
                .addValueItem(123L);
        SearchRequest searchRequest = new SearchRequest()
                .metaclass(SearchRequest.MetaclassEnum.ACCOUNT_SHOP)
                .addFiltersItem(filter)
                .limit(15);
        assertThat(call(client.searchEntities(searchRequest)).getEntities())
                .containsOnly(new BaseEntity().gid(SEARCH_RESULT.getId()).title(SEARCH_RESULT.getName()));
        verify(soap, times(1)).query("SELECT Id,Name FROM Account WHERE ShopID__c IN (123) LIMIT 15");
    }

    @Test
    void searchDistributionTypes() {
        SearchRequest searchRequest = new SearchRequest().metaclass(DISTRIBUTIONTYPE);
        assertThat(call(client.searchEntities(searchRequest)).getEntities())
                .containsExactlyInAnyOrder(
                        new BaseEntity().gid("ADV").title("ADV"),
                        new BaseEntity().gid("DBS").title("DBS"),
                        new BaseEntity().gid("FBS").title("FBS"),
                        new BaseEntity().gid("FBY").title("FBY"),
                        new BaseEntity().gid("FBY+").title("FBY+"),
                        new BaseEntity().gid("C&C").title("C&C")
                );
    }
}