package ru.yandex.market.crm.triggers.services.bpm.delegates.qa;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.mds.AvatarImageService;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.services.trigger.variables.VideoCommentInfo;
import ru.yandex.market.crm.triggers.services.bpm.ProcessCancelReason;
import ru.yandex.market.crm.triggers.services.bpm.delegates.DelegateExecutionContext;
import ru.yandex.market.crm.triggers.services.bpm.variables.QaCommentEvent;
import ru.yandex.market.crm.triggers.services.pers.PersInternalCommentInfo;
import ru.yandex.market.crm.triggers.services.pers.PersQaClient;
import ru.yandex.market.crm.triggers.services.pers.VhClient;
import ru.yandex.market.crm.triggers.services.pers.VhContent;
import ru.yandex.market.crm.triggers.services.pers.VhResponse;
import ru.yandex.market.crm.triggers.services.pers.VotesDto;
import ru.yandex.market.crm.triggers.services.pers.VotesInfo;
import ru.yandex.market.mcrm.avatars.HttpAvatarWriteClient;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.author.client.api.dto.AgitationDto;
import ru.yandex.market.pers.author.client.api.dto.AuthorIdDto;
import ru.yandex.market.pers.author.client.api.dto.ProductIdDto;
import ru.yandex.market.pers.author.client.api.dto.VideoInfoDto;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoList;
import ru.yandex.market.pers.author.client.api.dto.pager.DtoPager;
import ru.yandex.market.pers.author.client.api.model.VideoEntityType;
import ru.yandex.market.pers.author.client.api.model.VideoMetaInfo;
import ru.yandex.market.pers.author.client.api.model.VideoUserType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoCommentInjectorTest {
    public static final String VIDEO_TITLE = "Mi Band 4";
    public static final String VIDEO_URL = "https://frontend.vh.yandex.ru/player/vByWf3NfpKn4";
    public static final Long MODEL_ID = 1234567L;
    public static final String MODEL_NAME = "Some Test Name";
    public static final String MODEL_IMAGE = "Some Test Model Image";
    public static final String MARKET_PRODUCT_URL = "https://market.yandex.ru/product/";
    public static final String IMAGE = "http://avatars-int.mds.yandex.net:13000/get-yapic/slug/islands-retina-50";
    public static final Long UID = 4L;
    public static final long VIDEO_ID = 3;
    public static final long COMMENT_ID = 1;
    public static final String MODEL_LINK = "https://market.yandex.ru/product/" + MODEL_ID;
    private static final long LIKES_COUNT = 23;

    private final QaCommentEvent comment = new QaCommentEvent(
        "new_comment",
        13,
        1,
        (long) 2,
        3,
        4,
        UID,
        "text"
    );

    private final QaCommentEvent commentWithoutParent = new QaCommentEvent(
        "new_comment",
        13,
        1,
        null,
        3,
        4,
        null,
        "text"
    );

    private VideoCommentInjector injector;
    @Mock
    private PersQaClient qaClient;
    @Mock
    private BlackBoxClient yandexBlackboxClient;
    @Mock
    private PersAuthorClient persAuthorClient;
    @Mock
    private ReportService reportService;
    @Mock
    private VhClient vhClient;

    private static DelegateExecutionContext mockCtxWithEvent(QaCommentEvent event, String state) {
        DelegateExecutionContext ctx = mock(DelegateExecutionContext.class);
        when(ctx.getProcessVariable(ProcessVariablesNames.Event.VIDEO_COMMENT)).thenReturn(event);
        when(ctx.getProcessVariable(ProcessVariablesNames.VIDEO_COMMENT_STATE)).thenReturn(state);
        when(ctx.getComponentColor()).thenReturn(Color.GREEN);
        when(ctx.getUid()).thenReturn(Uid.asPuid(UID));
        return ctx;
    }

    @Before
    public void before() {
        AvatarImageService avatarImageService = new AvatarImageService(
            new HttpAvatarWriteClient(null,
                null,
                "http://avatars-int.mds.yandex.net:13000",
                null
            ),
            new HttpAvatarWriteClient(null,
                null,
                "http://avatars-int.mds.yandex.net:13001",
                null
            )
        );

        injector = new VideoCommentInjector(
            qaClient,
            yandexBlackboxClient,
            avatarImageService,
            persAuthorClient,
            reportService,
            vhClient);
    }

    @Test
    public void testCommentIsSetToProcessVariablesIfCorrectData() throws Exception {
        DelegateExecutionContext ctx = mockCtxWithEvent(commentWithoutParent, "NEW");

        mockVhInfo();
        mockVideo(VIDEO_ID);
        mockAgitation(UID);
        mockVotes(UID, VIDEO_ID);
        mockModelInfo();
        mockAuthor("test", UID, "slug");
        mockCommentInfo(COMMENT_ID, "text1", 1557146510000l, UID.toString(), "NEW");
        injector.doExecute(ctx);

        ArgumentCaptor<VideoCommentInfo> captor = ArgumentCaptor.forClass(VideoCommentInfo.class);
        verify(ctx).setProcessVariable(eq(ProcessVariablesNames.VIDEO_COMMENT), captor.capture(), eq(false));

        VideoCommentInfo actual = captor.getValue();
        assertEquals(COMMENT_ID, actual.getCommentInfo().getCommentId());
        assertEquals("text1", actual.getCommentInfo().getText());
        assertEquals("6 мая 2019", actual.getCommentInfo().getDate());
        assertEquals("test", actual.getCommentInfo().getAuthorInfo().getName());
        assertEquals("test", actual.getCommentInfo().getAuthorInfo().getDisplayName());
        assertEquals(IMAGE, actual.getCommentInfo().getAuthorInfo().getAvatar());
        assertNull(actual.getParentCommentInfo());

        assertEquals(VIDEO_ID, actual.getVideoInfo().getVideoId());
        assertEquals(VIDEO_URL, actual.getVideoInfo().getLink());
        assertEquals(IMAGE, actual.getVideoInfo().getPreview());
        assertEquals(IMAGE, actual.getVideoInfo().getAuthorAvatar());
        assertEquals(MODEL_ID.longValue(), actual.getVideoInfo().getProductId());
        assertEquals(MODEL_NAME, actual.getVideoInfo().getProductName());
        assertEquals(MARKET_PRODUCT_URL + MODEL_ID, actual.getVideoInfo().getProductLink());
        assertEquals(String.format("https://market.yandex.ru/product--any/%d/video/%d#comment-%d",
            MODEL_ID, VIDEO_ID, COMMENT_ID),
            actual.getVideoInfo().getVideoCommentLink());
        assertEquals(String.format("https://market.yandex.ru/product/%d/video/%d", MODEL_ID, VIDEO_ID),
            actual.getVideoInfo().getVideoLinkWithComments());

        assertEquals(MODEL_ID.longValue(), actual.getAgitationInfo().getProductId());
        assertEquals(MODEL_NAME, actual.getAgitationInfo().getProductName());
        assertEquals(MODEL_IMAGE, actual.getAgitationInfo().getProductImage());
        assertEquals(MARKET_PRODUCT_URL + MODEL_ID, actual.getAgitationInfo().getProductLink());
        assertEquals(MARKET_PRODUCT_URL + MODEL_ID + "/video/add", actual.getAgitationInfo().getVideoAddLink());
        assertEquals(50, actual.getAgitationInfo().getVideoAddExpertisePoint());
        assertEquals("https://market.yandex.ru/my/tasks", actual.getAgitationInfo().getAuthorProfile());

        assertEquals(LIKES_COUNT, actual.getUserContentStatInfo().getLikes());
        assertEquals("лайка", actual.getUserContentStatInfo().getLikesText());
        assertEquals(11121, actual.getUserContentStatInfo().getViews());
        assertEquals("просмотр", actual.getUserContentStatInfo().getViewsText());
    }

    @Test
    public void testProcessIsCanceledIfNoCommentInfo() {
        DelegateExecutionContext ctx = mockCtxWithEvent(comment, "NEW");

        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_VIDEO_COMMENT);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.COMMENT), anyObject(), eq(false));
    }

    @Test
    public void testProcessIsCanceledIfNoCommentInfoBecauseOfStatus() {
        DelegateExecutionContext ctx = mockCtxWithEvent(comment, "NEW");
        mockCommentInfo(comment.getCommentId(), "", 1, "1", "BANNED");
        injector.doExecute(ctx);

        verify(ctx).cancelProcess(ProcessCancelReason.NOT_NOTIFIABLE_VIDEO_COMMENT);
        verify(ctx, never()).setProcessVariable(eq(ProcessVariablesNames.COMMENT), anyObject(), eq(false));
    }

    private void mockAuthor(String name, long puid, String avatarSlug) {
        UserInfo answerAuthor = new UserInfo();
        answerAuthor.setDisplayName(name);
        answerAuthor.setUid(puid);
        answerAuthor.setAvatarSlug(avatarSlug);
        when(yandexBlackboxClient.getUserInfoByUid(eq(puid), anyObject(), anyObject(), anyObject()))
            .thenReturn(answerAuthor);
    }

    private void mockVideo(long id) {
        VideoInfoDto videoInfoDto = new VideoInfoDto();
        videoInfoDto.setId(id);
        videoInfoDto.setTitle(VIDEO_TITLE);
        videoInfoDto.setMetaInfo(new VideoMetaInfo(VIDEO_URL, null, null, IMAGE, 10, 1, 1));
        videoInfoDto.setProductIdDto(new ProductIdDto(VideoEntityType.MODEL, MODEL_ID));
        videoInfoDto.setAuthorIdDto(new AuthorIdDto(VideoUserType.UID, UID.toString()));
        when(persAuthorClient.getInternalVideoInfo(List.of(id)))
            .thenReturn(new DtoPager<>(List.of(videoInfoDto), null));
    }

    private void mockAgitation(long uid) {
        AgitationDto agitationDto = new AgitationDto();
        agitationDto.setId("6-" + MODEL_ID);
        agitationDto.setType(6);
        agitationDto.setEntityId(MODEL_ID.toString());
        when(persAuthorClient.getUserAgitationsByUid(uid, 6, 1, 1))
            .thenReturn(new DtoPager<>(List.of(agitationDto), null));
    }

    private void mockVotes(long uid, long videoId) {
        VotesInfo votesInfo = new VotesInfo();
        VotesDto votesDto = new VotesDto();
        votesDto.setLikeCount(LIKES_COUNT);
        votesInfo.setVotes(votesDto);
        when(qaClient.getVideoVotesInfo(uid, videoId))
            .thenReturn(new DtoList(List.of(votesInfo)));
    }

    private void mockCommentInfo(long commentId, String text, long update, String authorId, String state) {
        PersInternalCommentInfo.AuthorInfo authorInfo = new PersInternalCommentInfo.AuthorInfo();
        authorInfo.setId(authorId);

        PersInternalCommentInfo commentInfo = new PersInternalCommentInfo();
        commentInfo.setId(commentId);
        commentInfo.setText(text);
        commentInfo.setAuthor(authorInfo);
        commentInfo.setUpdateTime(update);
        commentInfo.setState(state);
        when(qaClient.getInternalCommentInfo(commentId)).thenReturn(commentInfo);
    }

    private void mockModelInfo() {
        ModelInfo modelInfo = new ModelInfo(MODEL_ID.toString());
        modelInfo.setLink(MODEL_LINK);
        modelInfo.setName(MODEL_NAME);
        modelInfo.setImg(MODEL_IMAGE);
        when(reportService.getProductInfo(MODEL_ID.toString(), Color.GREEN)).thenReturn(modelInfo);
    }

    private void mockVhInfo() {
        VhResponse vhResponse = new VhResponse();
        VhContent content = new VhContent();
        content.setViewsCount(11121);
        vhResponse.setContent(content);
        when(vhClient.getVideoInfoFromVh(any(String.class))).thenReturn(vhResponse);
    }
}
