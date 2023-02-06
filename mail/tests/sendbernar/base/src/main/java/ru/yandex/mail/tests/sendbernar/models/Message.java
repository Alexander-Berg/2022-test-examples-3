package ru.yandex.mail.tests.sendbernar.models;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.FilterSearch;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.mbody.MbodyApi;
import ru.yandex.mail.tests.mbody.MbodyResponses;
import ru.yandex.mail.tests.mbody.generated.AddressesResult;
import ru.yandex.mail.tests.mbody.generated.ApiMbody;
import ru.yandex.mail.tests.mbody.generated.Attachment;
import ru.yandex.mail.tests.mbody.generated.Mbody;
import ru.yandex.mail.tests.mbody.generated.Flag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.mbody.MbodyProperties.properties;

public class Message {
    private ApiMbody apiMbody() {
        return MbodyApi.apiMbody(properties().mbodyUri(), props().getCurrentRequestId(), rule.account().tvmTicket());
    }

    private String mid;
    private UserCredentials rule;
    private Mbody mbodyResp;
    private String headersResp;
    private FilterSearch filterSearchResp;

    private Mbody mbody() {
        if (mbodyResp == null) {
            mbodyResp = apiMbody()
                    .message()
                    .withUid(getUid())
                    .withMid(mid)
                    .withFlags(Flag.OUTPUT_AS_CDATA.toString())
                    .get(shouldBe(MbodyResponses.ok200()))
                    .as(Mbody.class);
        }

        return mbodyResp;
    }

    private String getUid() {
        return rule.account().uid();
    }

    private String headers() {
        if (headersResp == null) {
            headersResp = apiMbody().headers()
                    .withMid(mid)
                    .withUid(getUid())
                    .get(identity())
                    .peek()
                    .asString();
        }

        return headersResp;
    }

    private FilterSearch filterSearch() {
        if (filterSearchResp == null) {
            filterSearchResp = FilterSearch.filterSearch(
                    HoundApi.apiHound(HoundProperties.properties().houndUri(), "").filterSearch()
                            .withUid(getUid())
                            .withMids(mid)
                            .post(shouldBe(HoundResponses.ok200()))
            );
        }

        return filterSearchResp;
    }

    public Message(String mid, UserCredentials rule) {
        this.mid = mid;
        this.rule = rule;
        this.mbodyResp = null;
        this.filterSearchResp = null;
    }

    public String getHeader(String header) {
        header = header.toLowerCase();
        JsonParser parser = new JsonParser();
        JsonElement headersArray = parser.parse(headers())
                .getAsJsonObject().get(mid)
                .getAsJsonObject().get(header);

        if (headersArray == null) {
            return null;
        } else {
            return headersArray.getAsJsonArray().get(0).getAsString();
        }
    }

    public List<String> getHeaders(String header) {
        header = header.toLowerCase();
        JsonParser parser = new JsonParser();
        JsonElement headers = parser.parse(headers())
                .getAsJsonObject().get(mid)
                .getAsJsonObject().get(header)
                .getAsJsonArray();

        return new Gson().fromJson(headers, new TypeToken<List<String>>(){}.getType());
    }

    public List<Attachment> getAttachments() {
        return mbody().getAttachments();
    }

    public String getAttachHidByName(String name) {
        List<Attachment> attachments = mbody().getAttachments();

        Optional<Attachment> att = attachments
                .stream()
                .filter(entry -> entry
                                .getBinaryTransformerResult()
                                .getTypeInfo()
                                .getName()
                                .equals(name))
                .findFirst();

        return att.isPresent() ? att.get().getBinaryTransformerResult().getHid() : null;
    }

    public String noReplyNotification() {
        return mbody()
                .getInfo()
                .getNoReplyResult()
                .getNotification();
    }

    public String firstline() {
        return filterSearch().firstline();
    }

    public String subject() {
        return filterSearch().subject();
    }

    public String content() {
        return mbody()
                .getBodies().get(0)
                .getTransformerResult()
                .getTextTransformerResult()
                .getContent();
    }

    public String text() {
        return Jsoup.parse(content()).text();
    }

    public String fromEmail() {
        Map<String, String> from = filterSearch().from();

        return from.get("local")+"@"+from.get("domain");
    }

    public String fromName() {
        Map<String, String> from = filterSearch().from();

        return from.get("displayName");
    }

    public String toEmail() {
        Map<String, String> to = filterSearch().to().get(0);

        return to.get("local")+"@"+to.get("domain");
    }

    public List<String> toEmailList() {
        return filterSearch()
                .to()
                .stream()
                .map(email -> String.format("%s@%s", email.get("local"), email.get("domain")).toLowerCase())
                .collect(Collectors.toList());
    }

    public String ccEmail() {
        List<Map<String, String>> ccList = filterSearch().cc();

        return ccList.isEmpty() ? "" : ccList.get(0).get("local")+"@"+ccList.get(0).get("domain");
    }

    public String bccEmail() {
        Optional<String> bcc = mbody()
                .getInfo()
                .getAddressesResult()
                .stream()
                .filter(email -> email
                                    .getDirection()
                                    .equals("bcc"))
                .map(AddressesResult::getEmail)
                .findFirst();

        return bcc.isPresent() ? bcc.get().toLowerCase() : "";
    }

    public String messageId() {
        return mbody().getInfo().getMessageId();
    }

    public void exists() {
        mbody();
    }
}
