package ru.yandex.market.pers.qa.service.post;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.exception.EntityNotFoundException;
import ru.yandex.market.pers.qa.exception.QaResult;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.post.Link;
import ru.yandex.market.pers.qa.model.post.LinkType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author grigor-vlad
 * 01.07.2022
 */
public class LinkServiceTest extends PersQATest {
    private static final String ENTITY_ID = "1";
    private static final String LINK_ID = "12345";

    @Autowired
    private LinkService linkService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;

    @Test
    public void testLinkCreation() {
        int linkOrder = 0;
        long linkId = linkService.createLinkGetId(buildDefaultLink(linkOrder));

        List<Link> linksById = qaJdbcTemplate.query(
            "select * from post.link where id = ?",
            Link::valueOf, linkId);
        assertEquals(1, linksById.size());
        //check links fields
        Link linkFromDb = linksById.get(0);
        assertEquals(linkId, linkFromDb.getId());
        assertEquals(QaEntityType.POST_V2, linkFromDb.getEntityType());
        assertEquals(ENTITY_ID, linkFromDb.getEntityId());
        assertEquals(LinkType.SKU, linkFromDb.getLinkType());
        assertEquals(LINK_ID, linkFromDb.getLinkId());
        assertEquals(linkOrder, linkFromDb.getOrderNumber());
        assertNull(linkFromDb.getData());
    }

    @Test
    public void testGetLinkById() {
        int linkOrder = 0;
        long linkId = linkService.createLinkGetId(buildDefaultLink(linkOrder));
        Link linkById = linkService.getLinkById(linkId);

        //check links fields
        assertEquals(linkId, linkById.getId());
        assertEquals(QaEntityType.POST_V2, linkById.getEntityType());
        assertEquals(ENTITY_ID, linkById.getEntityId());
        assertEquals(LinkType.SKU, linkById.getLinkType());
        assertEquals(LINK_ID, linkById.getLinkId());
        assertEquals(linkOrder, linkById.getOrderNumber());
        assertNull(linkById.getData());

        //check throws exception on not existed linkId
        try {
            linkService.getLinkById(linkId + 1);
            Assertions.fail();
        } catch (EntityNotFoundException ex) {
            assertEquals(QaResult.LINK_NOT_FOUND, ex.getResult());
        }
    }

    @Test
    public void testGetLinkByEntity() {
        int linkCount = 3;
        for (int i = 0; i < linkCount; i++) {
            linkService.createLinkGetId(buildDefaultLink(i));
        }

        List<Link> linksByEntity = linkService.getLinksByEntity(QaEntityType.POST_V2, ENTITY_ID);
        assertEquals(3, linksByEntity.size());
    }

    private Link buildDefaultLink(int order) {
        Link link = new Link();
        link.setEntityType(QaEntityType.POST_V2);
        link.setEntityId(ENTITY_ID);
        link.setLinkType(LinkType.SKU);
        link.setLinkId(LINK_ID);
        link.setOrderNumber(order);
        return link;
    }
}
