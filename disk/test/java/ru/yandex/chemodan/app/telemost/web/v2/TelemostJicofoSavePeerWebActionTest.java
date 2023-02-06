package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceDtoDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferencePeerDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceDto;
import ru.yandex.chemodan.app.telemost.repository.model.ConferencePeerDto;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TelemostJicofoSavePeerWebActionTest extends AbstractConferenceWebActionTest {
    @Autowired
    private ConferenceDtoDao conferenceDtoDao;
    @Autowired
    private ConferencePeerDao conferencePeerDao;

    @Test
    public void testSavePeer() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        String randomConfId = Random2.R.nextDigits(16);
        String randomPeerId = Random2.R.nextDigits(16);

        HttpResponse response = helper.put(
                String.format("/v2/jicofo/conferences/%s/peers/%s", randomConfId, randomPeerId),
                "{\"display_name\": \"отображаемое имя\"}"
        );

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);
        assertEquals(0, result.size());


        Option<ConferenceDto> byConferenceId = conferenceDtoDao.findByConferenceId(randomConfId);
        Assert.assertTrue(byConferenceId.isPresent());

        ListF<ConferencePeerDto> conferencePeers = conferencePeerDao.findPeersInConference(randomConfId, Cf.list(randomPeerId));
        assertEquals(1, conferencePeers.size());
        ConferencePeerDto conferencePeer = conferencePeers.single();
        assertEquals(randomPeerId, conferencePeer.getPeerId());
        assertFalse(conferencePeer.getUid().isPresent());
        assertEquals("отображаемое имя", conferencePeer.getDisplayName());
    }

    @Test
    public void testUpdatePeer() throws IOException {
        A3TestHelper helper = getA3TestHelper();

        String fullUrl = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        String userId1 =
                testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(fullUrl) + "/connection", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                        false);
        String userId2 =
                testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(fullUrl) + "/connection", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                        false);

        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(fullUrl);
        Conference conference = conferenceService.joinConference(
                Option.empty(), conferenceUriData, conferenceUriData.getUserToken(), Option.empty());

        HttpResponse response = helper.put(
                String.format("/v2/jicofo/conferences/%s/peers/%s", conference.getConferenceId(), userId2),
                "{\"display_name\": \"отображаемое имя\"}"
        );

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);
        assertEquals(0, result.size());


        Option<ConferenceDto> byConferenceId = conferenceDtoDao.findByConferenceId(conference.getConferenceId());
        Assert.assertTrue(byConferenceId.isPresent());

        ListF<ConferencePeerDto> conferencePeers = conferencePeerDao.findPeersInConference(conference.getConferenceId(), Cf.list(userId2));
        assertEquals(1, conferencePeers.size());
        ConferencePeerDto conferencePeer = conferencePeers.single();
        assertEquals(userId2, conferencePeer.getPeerId());
        assertFalse(conferencePeer.getUid().isPresent());
        assertEquals("отображаемое имя", conferencePeer.getDisplayName());
    }

}
