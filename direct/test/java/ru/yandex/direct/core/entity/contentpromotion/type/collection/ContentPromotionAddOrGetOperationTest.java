package ru.yandex.direct.core.entity.contentpromotion.type.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionAddOrGetOperation;
import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionSingleObjectRequest;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.contentpromotion.type.ContentPromotionCoreTypeSupportFacade;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class ContentPromotionAddOrGetOperationTest {

    private ContentPromotionRepository contentPromotionRepository = mock(ContentPromotionRepository.class);
    private ContentPromotionCoreTypeSupportFacade coreTypeSupportFacade =
            mock(ContentPromotionCoreTypeSupportFacade.class);

    @Parameterized.Parameters(name = "send: {0}, expect: {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {null, null},
                {"      ", "      "},
                {"https:/ /www.youtube.com", "https:/ /www.youtube.com"},
                {"youtu.be", "https://www.youtube.com"},
                {"youtu.be/Abirvalg", "https://www.youtube.com/watch?v=Abirvalg"},
                {"www.youtu.be", "https://www.youtube.com"},
                {"www.youtu.be/Abirvalg", "https://www.youtube.com/watch?v=Abirvalg"},
                {"http://youtu.be", "http://www.youtube.com"},
                {"http://youtu.be/Abirvalg", "http://www.youtube.com/watch?v=Abirvalg"},
                {"http://www.youtu.be", "http://www.youtube.com"},
                {"https://youtu.be", "https://www.youtube.com"},
                {"https://www.youtu.be", "https://www.youtube.com"},
                {"https://youtu.be/oTJ?list=PLT9", "https://www.youtube.com/watch?v=oTJ"},
                {"https://youtu.be?list=PLT9", "https://www.youtube.com?list=PLT9"},
                {"http://www.youtube.com", "http://www.youtube.com"},
                {"http://www.youtube.com/v/-wtI", "http://www.youtube.com/v/-wtI"},
                {"http://www.youtube.com/v/-wtI?v=3&hide=1", "http://www.youtube.com/v/-wtI?v=3"},
                {"http://www.youtube.com?v=3&hide=1", "http://www.youtube.com?v=3"},
                {"http://youtube.com", "http://www.youtube.com"},
                {"http://youtube.com/v/-wtI", "http://www.youtube.com/v/-wtI"},
                {"http://youtube.com/v/-wtI?v=3&hide=1", "http://www.youtube.com/v/-wtI?v=3"},
                {"https://youtube.com", "https://www.youtube.com"},
                {"https://youtube.com/v/-wtI", "https://www.youtube.com/v/-wtI"},
                {"https://youtube.com/v/-wtI?v=3&hide=1", "https://www.youtube.com/v/-wtI?v=3"},
                {"https://www.youtube.com", "https://www.youtube.com"},
                {"https://www.youtube.com/v/-wtI", "https://www.youtube.com/v/-wtI"},
                {"https://www.youtube.com/v/-wtI?v=3&hide=1", "https://www.youtube.com/v/-wtI?v=3"},
                {"www.youtube.com", "https://www.youtube.com"},
                {"www.youtube.com/v/-wtI", "https://www.youtube.com/v/-wtI"},
                {"www.youtube.com/v/-wtI?v=3&hide=1", "https://www.youtube.com/v/-wtI?v=3"},
                {"youtube.com", "https://www.youtube.com"},
                {"youtube.com/v/-wtI", "https://www.youtube.com/v/-wtI"},
                {"youtube.com/v/-wtI?v=3&hide=1", "https://www.youtube.com/v/-wtI?v=3"},
                {"youtube.com/?v=3&hide=1", "https://www.youtube.com/?v=3"},
                {"http://www.youtube.com/wat?v=6d&ft=youtu.be", "http://www.youtube.com/wat?v=6d"},
                {"http://www.site.be", "http://www.site.be"},
                {"http://www.site.be/v/-wtI", "http://www.site.be/v/-wtI"},
                {"http://www.site.be/v/-wtI?version=3&hide=1", "http://www.site.be/v/-wtI?version=3&hide=1"},
                {"http://site.com", "http://site.com"},
                {"http://site.com/v/-wtI", "http://site.com/v/-wtI"},
                {"http://site.com/v/-wtI?version=3&hide=1", "http://site.com/v/-wtI?version=3&hide=1"},
                {"http://site.com?version=3&hide=1", "http://site.com?version=3&hide=1"},
                {"rutube.ru/video/daa20600b8178d82732caa031565db01/",
                        "https://rutube.ru/video/daa20600b8178d82732caa031565db01/"},
                {"https://yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082",
                        "https://yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082"},
                {"https://yandex.ru/efir?from=efir&from_block=ya_organic_results&stream_id=486e8c6ecba13d90b5f20372848a9082",
                        "https://yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082"},
                {"https://www.yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082",
                        "https://www.yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082"},
                {"https://www.yandex.ru/efir?from=efir&from_block=ya_organic_results&stream_id=486e8c6ecba13d90b5f20372848a9082",
                        "https://www.yandex.ru/efir?stream_id=486e8c6ecba13d90b5f20372848a9082"},
                {"https://yandex.ru/efir?from_block=efir_newtab", "https://yandex.ru/efir?from_block=efir_newtab"},
                {"https://yandex.ru/efir?", "https://yandex.ru/efir?"},
                {"https://yandex.ru/efir", "https://yandex.ru/efir"},
                {"https://yandex.by/efir?stream_id=486e8c6ecba13d90b5f20372848a9082",
                        "https://yandex.by/efir?stream_id=486e8c6ecba13d90b5f20372848a9082"},
        };
        return Arrays.asList(data);
    }

    @Parameterized.Parameter()
    public String send;
    @Parameterized.Parameter(1)
    public String expect;

    @Test
    public void getCorrectUrl() {
        List<ContentPromotionSingleObjectRequest> contentToAddOrGet = List.of(new ContentPromotionSingleObjectRequest()
                .withContentType(ContentPromotionContentType.VIDEO)
                .withUrl(send));
        new ContentPromotionAddOrGetOperation(ClientId.fromLong(5L), contentToAddOrGet,
                contentPromotionRepository, coreTypeSupportFacade);
        assertThat(contentToAddOrGet.get(0).getUrl()).isEqualTo(expect);
    }
}
