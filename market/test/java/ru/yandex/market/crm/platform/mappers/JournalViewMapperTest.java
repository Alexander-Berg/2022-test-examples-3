package ru.yandex.market.crm.platform.mappers;

import org.junit.Assert;
import org.junit.Test;

public class JournalViewMapperTest {

    @Test
    public void full() {
        String id = JournalViewMapper.extractId("/journal/goodsstory/11-korejskih-kosmeticheskih-sredstv?utm_source" +
                "=mama");
        Assert.assertEquals("Полный идентификатор статьи состоит из раздела и идентификатора статьи в рамках этого " +
                "раздела", "goodsstory/11-korejskih-kosmeticheskih-sredstv", id);
    }

    @Test
    public void category() {
        String id = JournalViewMapper.extractId("/journal/goodsstory/?utm_source=mama");
        Assert.assertEquals("Если просматриваем категорию журнала,  то сохраняем информацию о ее просмотре",
                "goodsstory", id);
    }

    @Test
    public void fullWithoutParams() {
        String id = JournalViewMapper.extractId("/journal/goodsstory/11-korejskih-kosmeticheskih-sredstv");
        Assert.assertEquals("Полный идентификатор статьи состоит из раздела и идентификатора статьи в рамках этого " +
                "раздела", "goodsstory/11-korejskih-kosmeticheskih-sredstv", id);
    }

    @Test
    public void notJournal() {
        String id = JournalViewMapper.extractId("/product/1234?utm_source=qwerty");
        Assert.assertNull("должны получить null т.к. просматриваемая страница не является статьей журнала Маркета", id);
    }
}
