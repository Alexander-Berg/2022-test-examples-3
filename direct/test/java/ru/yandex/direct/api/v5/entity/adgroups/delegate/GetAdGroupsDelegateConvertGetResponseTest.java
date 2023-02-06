package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.adgroups.GetResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateConvertGetResponseTest {
    private static final Long ID = 1L;
    private static final Long LIMIT = 5L;

    @Autowired
    private GetAdGroupsDelegate delegate;

    private static AdGroup buildTextAdGroup() {
        return new TextAdGroup()
                .withId(ID)
                .withType(AdGroupType.BASE)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withBsRarelyLoaded(false);
    }

    @Test
    public void convertGetResponse_resultIsEmpty() {
        List<AdGroup> getItems = emptyList();
        Set<AdGroupAnyFieldEnum> requestedFields = EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID);

        GetResponse response = delegate.convertGetResponse(getItems, requestedFields, null);
        assertThat(response.getAdGroups(), beanDiffer(emptyList()));
    }

    @Test
    public void convertGetResponse_resutIsNotEmpty() {
        List<AdGroup> getItems = Collections.singletonList(buildTextAdGroup());
        Set<AdGroupAnyFieldEnum> requestedFields = EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID);

        GetResponse response = delegate.convertGetResponse(getItems, requestedFields, null);

        assertThat(response.getAdGroups(), beanDiffer(Collections.singletonList(new AdGroupGetItem().withId(ID))));
    }

    @Test
    public void convertGetResponse_limitedByIsNull() {
        List<AdGroup> getItems = emptyList();
        Set<AdGroupAnyFieldEnum> requestedFields = EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID);

        GetResponse response = delegate.convertGetResponse(getItems, requestedFields, null);
        assertThat(response.getLimitedBy(), nullValue());
    }

    @Test
    public void convertGetResponse_limitedByIsNotNull() {
        List<AdGroup> getItems = emptyList();
        Set<AdGroupAnyFieldEnum> requestedFields = EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID);

        GetResponse response = delegate.convertGetResponse(getItems, requestedFields, LIMIT);
        assertThat(response.getLimitedBy(), equalTo(LIMIT));
    }
}
