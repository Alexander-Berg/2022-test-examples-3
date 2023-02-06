package ru.yandex.market.mbi.web.paging;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.common.paging.PageTokenHelper;
import ru.yandex.common.paging.PagingDirectionsDTO;
import ru.yandex.common.paging.PagingException;
import ru.yandex.common.paging.PagingLimits;
import ru.yandex.common.paging.SeekPagingHelper;
import ru.yandex.common.paging.SeekSliceRequest;
import ru.yandex.common.paging.SeekSliceRequestBuilder;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;

@ParametersAreNonnullByDefault
class SeekPagingHelperTest {

    private static final PageTokenHelper PAGE_TOKEN_HELPER =
            new PageTokenHelper(new ApiObjectMapperFactory().createJsonMapper());

    private static <K> SeekSliceRequestBuilder<K> createSeekPagingHelperBuilder(Class<K> klass) {
        PagingLimits pagingLimits = new PagingLimits.Builder()
                .setDefaultLimit(10)
                .setMaxLimit(50)
                .build();
        return new SeekSliceRequestBuilder<>(PAGE_TOKEN_HELPER, klass, pagingLimits);
    }

    @Test
    void badToken() {
        SeekSliceRequestBuilder<Long> pagingBuilder = createSeekPagingHelperBuilder(Long.class);
        pagingBuilder.setPageToken("qwerty");
        PagingException apiInvalidRequestException = Assertions.assertThrows(
                PagingException.class,
                pagingBuilder::build
        );
        ReflectionAssert.assertReflectionEquals(
                new PagingException(PagingException.Code.BAD_PAGE_TOKEN, "bad page token"),
                apiInvalidRequestException
        );
    }

    @Test
    void badLimit() {
        SeekSliceRequestBuilder<Long> pagingBuilder = createSeekPagingHelperBuilder(Long.class);
        pagingBuilder.setLimit(-1);
        PagingException apiInvalidRequestException = Assertions.assertThrows(
                PagingException.class,
                pagingBuilder::build
        );
        ReflectionAssert.assertReflectionEquals(
                new PagingException(PagingException.Code.NON_POSITIVE_LIMIT, "non positive limit"),
                apiInvalidRequestException
        );
    }

    @Test
    void testInvariantString() {
        SeekSliceRequestBuilder<String> pagingBuilder = createSeekPagingHelperBuilder(String.class);
        SeekPagingHelper<String> helper = new SeekPagingHelper<>(pagingBuilder.build(), PAGE_TOKEN_HELPER);
        PagingDirectionsDTO directions =
                helper.createDirections(Arrays.asList("firstItemKey", "lastItemKey"));
        pagingBuilder.setPageToken(directions.getPrevPageToken());
        pagingBuilder.setLimit(13);
        SeekSliceRequest<String> prevPagePaging = pagingBuilder.build();
        Assertions.assertEquals(Optional.of("firstItemKey"), prevPagePaging.seekKey());
        Assertions.assertTrue(prevPagePaging.reverseOrder());
        Assertions.assertEquals(13, prevPagePaging.limit());

        pagingBuilder.setPageToken(directions.getNextPageToken());
        pagingBuilder.setLimit(13);
        SeekSliceRequest<String> nextPagePaging = pagingBuilder.build();
        Assertions.assertEquals(Optional.of("lastItemKey"), nextPagePaging.seekKey());
        Assertions.assertFalse(nextPagePaging.reverseOrder());
        Assertions.assertEquals(13, nextPagePaging.limit());
    }

    @Test
    void testInvariantLong() {
        SeekSliceRequestBuilder<Long> pagingBuilder = createSeekPagingHelperBuilder(Long.class);
        SeekPagingHelper<Long> helper = new SeekPagingHelper<>(pagingBuilder.build(), PAGE_TOKEN_HELPER);
        PagingDirectionsDTO directions =
                helper.createDirections(Arrays.asList(1111L, 9999L));
        pagingBuilder.setPageToken(directions.getPrevPageToken());
        pagingBuilder.setLimit(13);
        SeekSliceRequest<Long> prevPagePaging = pagingBuilder.build();
        Assertions.assertEquals(Optional.of(1111L), prevPagePaging.seekKey());
        Assertions.assertTrue(prevPagePaging.reverseOrder());
        Assertions.assertEquals(13, prevPagePaging.limit());

        pagingBuilder.setPageToken(directions.getNextPageToken());
        pagingBuilder.setLimit(13);
        SeekSliceRequest<Long> nextPagePaging = pagingBuilder.build();
        Assertions.assertEquals(Optional.of(9999L), nextPagePaging.seekKey());
        Assertions.assertFalse(nextPagePaging.reverseOrder());
        Assertions.assertEquals(13, nextPagePaging.limit());
    }

    @Test
    void testInvariantCustomDTO() {
        SeekSliceRequestBuilder<TestDTO> pagingBuilder = createSeekPagingHelperBuilder(TestDTO.class);
        SeekPagingHelper<TestDTO> helper = new SeekPagingHelper<>(pagingBuilder.build(), PAGE_TOKEN_HELPER);
        TestDTO first = new TestDTO();
        first.setI(1);
        first.setS("a");

        TestDTO last = new TestDTO();
        last.setI(9);
        last.setS("z");

        PagingDirectionsDTO directions = helper.createDirections(Arrays.asList(first, last));

        pagingBuilder.setPageToken(directions.getPrevPageToken());
        pagingBuilder.setLimit(13);
        SeekSliceRequest<TestDTO> prevPagePaging = pagingBuilder.build();
        TestDTO prevPagingKey = prevPagePaging.seekKey().orElseThrow(IllegalStateException::new);
        Assertions.assertEquals(1, prevPagingKey.getI().intValue());
        Assertions.assertEquals("a", prevPagingKey.getS());
        Assertions.assertTrue(prevPagePaging.reverseOrder());
        Assertions.assertEquals(13, prevPagePaging.limit());

        pagingBuilder.setPageToken(directions.getNextPageToken());
        pagingBuilder.setLimit(13);
        SeekSliceRequest<TestDTO> nextPagePaging = pagingBuilder.build();
        TestDTO nextPagingKey = nextPagePaging.seekKey().orElseThrow(IllegalStateException::new);
        Assertions.assertEquals(9, nextPagingKey.getI().intValue());
        Assertions.assertEquals("z", nextPagingKey.getS());
        Assertions.assertFalse(nextPagePaging.reverseOrder());
        Assertions.assertEquals(13, nextPagePaging.limit());
    }

    @XmlRootElement(name = "key")
    @XmlAccessorType(XmlAccessType.NONE)
    private static class TestDTO {
        private Integer i;
        private String s;

        @XmlAttribute(name = "i")
        private Integer getI() {
            return i;
        }

        private void setI(Integer i) {
            this.i = i;
        }

        @XmlAttribute(name = "s")
        private String getS() {
            return s;
        }

        private void setS(String s) {
            this.s = s;
        }
    }
}
