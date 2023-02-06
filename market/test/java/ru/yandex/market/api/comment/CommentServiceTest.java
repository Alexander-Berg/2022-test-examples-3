package ru.yandex.market.api.comment;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.yandex.market.api.domain.v2.VendorV2;
import ru.yandex.market.api.domain.v2.opinion.OpinionField;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.pers.PersQaClient;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.api.util.httpclient.adapter.FuturePipeImpl;
import ru.yandex.market.api.vendor.VendorService;
import ru.yandex.market.sdk.userinfo.domain.AggregateUserInfo;
import ru.yandex.market.sdk.userinfo.domain.Options;
import ru.yandex.market.sdk.userinfo.domain.PassportInfo;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;
import ru.yandex.market.sdk.userinfo.util.Result;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by fettsery on 25.04.19.
 */
@WithMocks
public class CommentServiceTest extends UnitTestBase {
    @Mock
    PersQaClient persQaClient;

    @Mock
    UserInfoService userInfoService;

    @Mock
    VendorService vendorService;

    @InjectMocks
    CommentService commentService;

    @Test
    public void enrichChildrenComments() {
        Comment childrenComment = new Comment() {{
            setId("2");
            setUser(new Comment.User() {{
                setId(2L);
            }});
        }};

        Comment vendorComment = new Comment() {{
            setId("3");
            setUser(new Comment.User() {{
                setId(3L);
            }});
            setParams(Collections.singletonMap("vendorId", "10"));
        }};

        Comment comment = new Comment() {{
            setId("1");
            setUser(new Comment.User() {{
                setId(1L);
            }});
            setChildren(Arrays.asList(childrenComment, vendorComment));
        }};
        
        when(persQaClient.getGradeCommentsAsync(eq(Collections.singletonList(424242L)), eq(10)))
            .thenReturn(Futures.newSucceededFuture(Collections.singletonList(comment)));

        PassportInfo userInfo1 = Mockito.mock(PassportInfo.class);
        when(userInfo1.getUid()).thenReturn(Uid.ofPassport(1L));
        when(userInfo1.getCAPIDisplayName()).thenReturn(Optional.of("Alice"));
        when(userInfo1.getAvatar(anyString())).thenReturn(Optional.empty());

        PassportInfo userInfo2 = Mockito.mock(PassportInfo.class);
        when(userInfo2.getUid()).thenReturn(Uid.ofPassport(2L));
        when(userInfo2.getCAPIDisplayName()).thenReturn(Optional.of("Bob"));
        when(userInfo2.getAvatar(anyString())).thenReturn(Optional.empty());

        Result<List<AggregateUserInfo>, ? extends ru.yandex.market.sdk.userinfo.domain.Error> result =
                Result.ofValue(Arrays.asList(new AggregateUserInfo(userInfo1), new AggregateUserInfo(userInfo2)));

        when(userInfoService.getUserInfoRawAsync(any(List.class), any(Options.class)))
            .thenReturn(new FuturePipeImpl<>(Pipelines.startWithValue(result)));

        VendorV2 vendor = new VendorV2();
        vendor.setName("Gillette");
        when(vendorService.getVendors(any(List.class), any(), any()))
            .thenReturn(Futures.newSucceededFuture(new Long2ObjectOpenHashMap<>(Collections.singletonMap(10L, vendor))));


        CommentTree tree = Futures.waitAndGet(commentService.getGradeCommentsTreeAsync(
            Collections.singletonList(424242L),
            10,
            Collections.singleton(OpinionField.COMMENT_USERS))
        );

        Assert.assertEquals("Alice", tree.get("1").getUser().getName());
        Assert.assertEquals("Bob", tree.get("2").getUser().getName());
        Assert.assertEquals("Gillette", tree.get("3").getUser().getName());
    }
}
