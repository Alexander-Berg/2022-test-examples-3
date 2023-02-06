package ru.yandex.direct.grid.processing.service.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageFilter;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageOrderBy;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageOrderByField;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImagesContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientEntityGraphQlServiceImagesTest {
    private static final String QUERY_TEMPLATE = "{\n"
            + "  images(input: %s) {\n"
            + "    rowset {\n"
            + "      avatarsHost\n"
            + "      type\n"
            + "      imageHash\n"
            + "      mdsGroupId\n"
            + "      name\n"
            + "      namespace\n"
            + "      imageSize {\n"
            + "        height\n"
            + "        width\n"
            + "      }\n"
            + "      formats {\n"
            + "        path\n"
            + "        imageSize {\n"
            + "          height\n"
            + "          width\n"
            + "        }\n"
            + "        smartCenters {\n"
            + "          ratio\n"
            + "          height\n"
            + "          width\n"
            + "          x\n"
            + "          y\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "    imageHashes\n"
            + "    totalCount\n"
            + "  }"
            + "}\n";

    private static final LocalDateTime YESTERDAY = LocalDate.now().minusDays(1).atTime(0, 0);
    private static final LocalDateTime BEFORE_YESTERDAY = LocalDate.now().minusDays(2).atTime(0, 0);
    private static final String FIRST_IMAGE_NAME = "first_image_name";
    private static final String SECOND_IMAGE_NAME = "second_image_name";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    private ClientInfo clientInfo;
    private int shard;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void getImages_whenOrderAsc_success() {
        BannerImageFormat firstImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, firstImage);
        steps.bannerSteps().setImageCreationDate(shard, firstImage.getImageHash(), BEFORE_YESTERDAY);
        BannerImageFormat secondImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, secondImage);
        steps.bannerSteps().setImageCreationDate(shard, secondImage.getImageHash(), YESTERDAY);

        List<GdImageOrderBy> orderBy = singletonList(new GdImageOrderBy()
                .withField(GdImageOrderByField.CREATE_TIME)
                .withOrder(Order.ASC));
        Map<String, Object> data = sendRequest(clientInfo.getClientId(), null, orderBy);

        String firstImageHash = GraphQLUtils.getDataValue(data, "images/rowset/0/imageHash");
        String secondImageHash = GraphQLUtils.getDataValue(data, "images/rowset/1/imageHash");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstImageHash).as("firstImageHash").isEqualTo(firstImage.getImageHash());
            soft.assertThat(secondImageHash).as("secondImageHash").isEqualTo(secondImage.getImageHash());
        });
    }

    @Test
    public void getImages_whenOrderDesc_success() {
        BannerImageFormat firstImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, firstImage);
        steps.bannerSteps().setImageCreationDate(shard, firstImage.getImageHash(), BEFORE_YESTERDAY);
        BannerImageFormat secondImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, secondImage);
        steps.bannerSteps().setImageCreationDate(shard, secondImage.getImageHash(), YESTERDAY);

        List<GdImageOrderBy> orderBy = singletonList(new GdImageOrderBy()
                .withField(GdImageOrderByField.CREATE_TIME)
                .withOrder(Order.DESC));
        Map<String, Object> data = sendRequest(clientInfo.getClientId(), null, orderBy);

        String secondImageHash = GraphQLUtils.getDataValue(data, "images/rowset/0/imageHash");
        String firstImageHash = GraphQLUtils.getDataValue(data, "images/rowset/1/imageHash");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstImageHash).as("firstImageHash").isEqualTo(firstImage.getImageHash());
            soft.assertThat(secondImageHash).as("secondImageHash").isEqualTo(secondImage.getImageHash());
        });
    }

    @Test
    public void getImages_withoutOrder_success() {
        BannerImageFormat firstImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, firstImage);
        steps.bannerSteps().setImageCreationDate(shard, firstImage.getImageHash(), BEFORE_YESTERDAY);
        BannerImageFormat secondImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, secondImage);
        steps.bannerSteps().setImageCreationDate(shard, secondImage.getImageHash(), YESTERDAY);

        Map<String, Object> data = sendRequest(clientInfo.getClientId(), null, null);

        String secondImageHash = GraphQLUtils.getDataValue(data, "images/rowset/0/imageHash");
        String firstImageHash = GraphQLUtils.getDataValue(data, "images/rowset/1/imageHash");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstImageHash).as("firstImageHash").isEqualTo(firstImage.getImageHash());
            soft.assertThat(secondImageHash).as("secondImageHash").isEqualTo(secondImage.getImageHash());
        });
    }

    @Test
    public void getImages_withNotExistName_success() {
        BannerImageFormat firstImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, firstImage);

        Map<String, Object> data = sendRequest(clientInfo.getClientId(), "notExistName", null);

        Integer totalCount = GraphQLUtils.getDataValue(data, "images/totalCount");
        assertThat(totalCount).as("totalCount").isEqualTo(0);
    }

    @Test
    public void getImages_withName_success() {
        BannerImageFormat firstImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, firstImage);
        steps.bannerSteps().setImageName(shard, firstImage.getImageHash(), FIRST_IMAGE_NAME + ".jpg");
        BannerImageFormat secondImage = TestBanners.defaultBannerImageFormat(null);
        steps.bannerSteps().createBannerImageFormat(clientInfo, secondImage);
        steps.bannerSteps().setImageName(shard, secondImage.getImageHash(), SECOND_IMAGE_NAME + ".jpg");

        Map<String, Object> data = sendRequest(clientInfo.getClientId(), SECOND_IMAGE_NAME, null);

        Integer totalCount = GraphQLUtils.getDataValue(data, "images/totalCount");
        String imageName = GraphQLUtils.getDataValue(data, "images/rowset/0/name");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(totalCount).as("totalCount").isEqualTo(1);
            soft.assertThat(imageName).as("imageName").contains(SECOND_IMAGE_NAME);
        });
    }

    private Map<String, Object> sendRequest(ClientId clientId,
                                            @Nullable String nameContains,
                                            @Nullable List<GdImageOrderBy> orderBy) {
        String filter = getFilter(clientId, nameContains, orderBy);
        String query = String.format(QUERY_TEMPLATE, filter);
        GridGraphQLContext context = getGridGraphQLContext(clientInfo.getUid());
        gridContextProvider.setGridContext(context);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

    private GridGraphQLContext getGridGraphQLContext(Long uid) {
        User user = userService.getUser(uid);
        return ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
    }

    private String getFilter(ClientId clientId, @Nullable String nameContains, @Nullable List<GdImageOrderBy> ob) {
        GdImageFilter filter = new GdImageFilter()
                .withNameContains(nameContains);
        GdLimitOffset limitOffset = new GdLimitOffset()
                .withLimit(100)
                .withOffset(0);
        GdClientSearchRequest clientSearchRequest = new GdClientSearchRequest()
                .withId(clientId.asLong());
        GdImagesContainer imagesContainer = new GdImagesContainer()
                .withFilter(filter)
                .withLimitOffset(limitOffset)
                .withSearchBy(clientSearchRequest)
                .withCacheKey("")
                .withOrderBy(ob);
        return graphQlSerialize(imagesContainer);
    }

}
