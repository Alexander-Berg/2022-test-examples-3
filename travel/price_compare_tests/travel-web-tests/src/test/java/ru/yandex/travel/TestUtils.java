package ru.yandex.travel;

import com.jcabi.email.Envelope;
import com.jcabi.email.Postman;
import com.jcabi.email.Protocol;
import com.jcabi.email.Token;
import com.jcabi.email.enclosure.EnHTML;
import com.jcabi.email.stamp.StRecipient;
import com.jcabi.email.stamp.StSender;
import com.jcabi.email.stamp.StSubject;
import com.jcabi.email.wire.SMTP;
import com.jcabi.immutable.Array;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.http.client.utils.URIBuilder;
import ru.yandex.travel.beans.SearchParameters;
import ru.yandex.travel.beans.Summary;
import ru.yandex.travel.elliptics.Elliptics;
import ru.yandex.travel.utils.TsvUtils;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.lessThan;

class TestUtils {

    static int successRate(int travel, int sletat) {
        if (Math.abs(travel - sletat) < 100) {
            return 0;
        } else if (sletat < travel) {
            return 1;
        } else {
            return 2;
        }
    }

    static void generateReport(List<Summary> result, String subject) throws IOException, TemplateException {
        Configuration cfg = new Configuration(VERSION_2_3_23);
        cfg.setClassForTemplateLoading(TestUtils.class, TestUtils.class.getSimpleName());
        Template template = cfg.getTemplate("mail.ftl");
        Map<String, Object> object = new HashMap<>();
        object.put("results", result);

        try (Writer writer = new StringWriter()) {
            template.process(object, writer);
            writer.flush();
            sendMail(writer.toString(), subject);
        }
    }

    static void sendMail(String file, String subject) throws IOException {
        Postman postman = new Postman.Default(
                new SMTP(new Token("", "")
                        .access(new Protocol.SMTP("outbound-relay.yandex.net", 25))));
        Envelope envelope = new Envelope.MIME(
                new Array<>(
                        new StSender("testrunner@yandex-team.ru"),
//                        new StRecipient("alexcrush@yandex-team.ru"),
                        new StRecipient("diff-prices@yandex-team.ru"),
                        new StRecipient("karly@yandex-team.ru"),     // https://st.yandex-team.ru/HOTELS-2858
                        new StRecipient("svfadeeva@yandex-team.ru"), // https://st.yandex-team.ru/HOTELS-2858
                        new StRecipient("malmary@yandex-team.ru"),   // https://st.yandex-team.ru/HOTELS-2858
                        new StRecipient("vd@level.travel"),
                        new StRecipient("im@level.travel"),
                        new StSubject(subject)),
                new Array<>(new EnHTML(file)));
        postman.send(envelope);
    }

    static SearchParameters convertSearchParameters(Map<String, String> data, boolean setHotel) {
        SearchParameters parameters = new SearchParameters()
                .setFromCity(data.get("from"))
                .setToCountry(data.get("to_country"))
                .setResort(data.get("to"));
        if (setHotel) {
            parameters.setHotel(data.get("sletatHotelName"));
        }
        Map<String, List<String>> query = getQueryMap(data.get("searchUrl"));
        try {
            LocalDate when = LocalDate.parse(query.get("when").get(0), ofPattern("yyyy-MM-dd"));
            boolean whenFlex = Boolean.parseBoolean(query.get("when_flex").get(0));
            boolean nightFlex = Boolean.parseBoolean(query.get("nights_flex").get(0));
            int nights = Integer.parseInt(query.get("nights").get(0));
            List<String> ages = query.get("ages");

            parameters.setFromDate(whenFlex ? toStringDay(when.minusDays(2)) : toStringDay(when));
            parameters.setToDate(whenFlex ? toStringDay(when.plusDays(2)) : toStringDay(when));
            parameters.setMinNights(nightFlex ? nights-2 : nights);
            parameters.setMaxNights(nightFlex ? nights+2 : nights);
            parameters.setAdults((int) ages.stream().filter(e -> e.equals("88")).count());
            parameters.setChilds(ages.stream().filter(e -> lessThan(16).matches(Integer.valueOf(e))).collect(toList()));
        } catch (Exception e) {
        }
        return parameters;

    }

    static String toStringDay(LocalDate date) {
        return ofPattern("dd.MM.yyyy").format(date);
    }

    static Map<String, List<String>> getQueryMap(String url) {
        Map<String, List<String>> parameters = new HashMap<>();
        try {
            URIBuilder builder = new URIBuilder(url);
            builder.getQueryParams().forEach(nameValuePair -> {
                List<String> valuesList = parameters.getOrDefault(nameValuePair.getName(), new ArrayList<>());
                valuesList.add(nameValuePair.getValue());
                parameters.put(nameValuePair.getName(), valuesList);
            });
        } catch (Exception e) {

        }
        return parameters;
    }

    static List<Map<String, String>> readSearches(String fileName) throws IOException {
        try (InputStream stream = Elliptics.downloadFromS3().fileS3(fileName)) {
            return TsvUtils.read(stream);
        }
    }

}
