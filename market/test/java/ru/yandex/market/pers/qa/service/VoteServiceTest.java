package ru.yandex.market.pers.qa.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.exception.DuplicateEntityException;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.VoteValueType;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 21.05.2020
 */
public class VoteServiceTest extends PersQATest {

    public static final long UID = 123;

    @Autowired
    private VoteService voteService;

    @Test
    public void testCreateArticleVotesWithDuplicateVote() {
        long articleId = 1;

        voteService.createArticleVote(articleId, UID, VoteValueType.LIKE);
        try {
            voteService.addEntityVote(QaEntityType.ARTICLE, String.valueOf(articleId), UID, VoteValueType.LIKE);
            Assertions.fail("fail");
        } catch (DuplicateEntityException expected) {
            Assertions.assertEquals("Article vote is already exist", expected.getMessage());
        }
    }

}
