package ru.yandex.market.pers.notify.push.generator.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.article.Article;
import ru.yandex.market.pers.notify.article.ArticleService;
import ru.yandex.market.pers.notify.external.history.LightHistoryElement;
import ru.yandex.market.pers.notify.mail.generator.UserWithPayload;
import ru.yandex.market.pers.notify.model.UserModel;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         25.01.17
 */
public class ArticleFilterTest extends MarketMailerMockedDbTest {
    public static final long uid1 = 1L;
    public static final long uid2 = 2L;
    public static final String uuid1 = "uuid1";
    public static final String uuid2 = "uuid2";
    public static final String uuid3 = "uuid3";

    private static final long articleHid1 = 90639L;
    private static final long articleHid2 = 91529L;
    private static final long noArticleHid1 = 90638L;
    private static final long noArticleHid2 = 91528L;
    
    @Autowired
    private ArticleService articleService;
    private ArticleFilter articleFilter;

    
    public static List<UserWithPayload> generateUsers() {
        return Arrays.asList(
            new UserWithPayload(new UserModel(uid1, uuid2, null), Arrays.asList(
                new LightHistoryElement(uid1, 1L, articleHid1, null, uuid2, null, null),
                new LightHistoryElement(uid1, 2L, articleHid1, null, uuid2, null, null),
                new LightHistoryElement(uid1, 1L, articleHid2, null, uuid2, null, null),
                new LightHistoryElement(uid1, 1L, noArticleHid1, null, uuid2, null, null),
                new LightHistoryElement(uid1, 2L, noArticleHid1, null, uuid2, null, null),
                new LightHistoryElement(uid1, 1L, noArticleHid2, null, uuid2, null, null)
                )),

            new UserWithPayload(new UserModel(uid2, uuid3, null), Arrays.asList(
                new LightHistoryElement(uid2, 1L, articleHid2, null, uuid3, null, null),
                new LightHistoryElement(uid2, 2L, articleHid2, null, uuid3, null, null),
                new LightHistoryElement(uid2, 1L, noArticleHid1, null, uuid3, null, null),
                new LightHistoryElement(uid2, 1L, noArticleHid2, null, uuid3, null, null)
                )),

            new UserWithPayload(new UserModel(null, uuid1, null), Arrays.asList(
                new LightHistoryElement(null, 1L, articleHid2, null, uuid1, null, null),
                new LightHistoryElement(null, 2L, articleHid2, null, uuid1, null, null),
                new LightHistoryElement(null, 1L, noArticleHid1, null, uuid1, null, null),
                new LightHistoryElement(null, 1L, noArticleHid2, null, uuid1, null, null)
                )),

            new UserWithPayload(new UserModel(null, uuid2, null), Arrays.asList(
                new LightHistoryElement(null, 1L, articleHid1, null, uuid2, null, null),
                new LightHistoryElement(null, 1L, noArticleHid1, null, uuid2, null, null)
                )),

            new UserWithPayload(new UserModel(null, uuid3, null), Arrays.asList(
                new LightHistoryElement(null, 1L, noArticleHid1, null, uuid3, null, null),
                new LightHistoryElement(null, 1L, noArticleHid2, null, uuid3, null, null)
                ))
        );
    }
    
    @BeforeEach
    public void init() throws Exception {
        articleFilter = new ArticleFilter(articleService);
        articleFilter.setLookAtArticleForDays(Integer.MAX_VALUE);
    }

    @Test
    @Disabled("FIXME")
    public void apply() throws Exception {
        List<UserWithPayload> users = generateUsers();
        
        // articles on what hids should be sent to users
        Map<UserModel, Long> hidsToUsers = new HashMap<UserModel, Long>() {{
            put(new UserModel(uid2, null, null), articleHid2);
            put(new UserModel(null, uuid1, null), articleHid2);
            put(new UserModel(null, uuid2, null), articleHid1);
        }};

        users = articleFilter.apply(users.stream()).collect(Collectors.toList());
        assertEquals(4, users.size());
        for (UserWithPayload user : users) {
            if (user.getModel().equals(new UserModel(uid1, null, null))) {
                continue;
            }
            assertEquals((long) hidsToUsers.get(user.getModel()), Long.parseLong(((Article) user.getPayload()).getHid().get(0)));
        }
        
        Article article = (Article) users.stream()
            .filter(u -> Objects.equals(new UserModel(uid1, null, null), u.getModel()))
                .findAny().orElse(null).getPayload(); 
        assertTrue(Long.parseLong(article.getHid().get(0)) == articleHid1 || Long.parseLong(article.getHid().get(0)) == articleHid2);
    }
}
