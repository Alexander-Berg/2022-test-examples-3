package ru.yandex.market.antifraud.filter.generators;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import ru.yandex.market.antifraud.filter.ClicksAndShows;
import ru.yandex.market.antifraud.filter.PeriodUtils;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.ShowGenerator;
import ru.yandex.market.antifraud.filter.TestEntity;
import ru.yandex.market.antifraud.filter.TestShow;
import ru.yandex.market.antifraud.filter.fields.Cookie;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.fields.Ids;
import ru.yandex.market.antifraud.filter.fields.PP;
import ru.yandex.market.antifraud.filter.fields.Url;
import ru.yandex.market.antifraud.filter.ip.IP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by entarrion on 19.02.15.
 */
public class ForShowsFilters {
    private static ImmutableList<Integer> reportPPS =
            ImmutableList.of(6, 13, 21);
    private String prefix;
    private DateTime dateTime;

    public ForShowsFilters(DateTime datetime, String rowIdPrefix) {
        setDateTime(datetime);
        setPrefix(rowIdPrefix);
    }

    public ForShowsFilters(DateTime datetime) {
        this(datetime, "AT_AF_S_" + RandomStringUtils.randomAlphanumeric(4));
    }

    public ForShowsFilters() {
        this(new DateTime());
    }

    public DateTime getDateTime() {
        Preconditions.checkNotNull(dateTime, "Datetime must be set");
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String rowIdPrefix) {
        this.prefix = rowIdPrefix;
    }

    public List<TestShow> generateForNoneFilter() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(getUniqueShow(getDateTime(), 5));
        for (TestShow show : shows) {
            show.setFilter(FilterConstants.FILTER_0);
        }
        return showsPreparer(shows, 0);
    }

    public List<TestShow> generateFor02Filter() {
        List<TestShow> shows = getUniqueShow(getDateTime(), 10);
        for (TestShow show : shows) {
            show.setOriginalQuery("host=ya.ru");
            show.setFilter(FilterConstants.FILTER_2);
        }

        List<TestShow> showsNoCookie = getUniqueShow(getDateTime(), 10);
        for (TestShow show : showsNoCookie) {
            show.setOriginalQuery("host=ya.ru");
            show.setFilter(FilterConstants.FILTER_2);
            show.setCookie("");
            show.setIp("");
        }

        shows.addAll(showsNoCookie);
        return showsPreparer(shows, 2);
    }

    public List<TestShow> generateFor04Filter() {
        List<TestShow> shows = getUniqueShow(getDateTime(), 10);
        for (TestShow show : shows) {
            String ip = IP.generateIpFromBlackSubnet();
            show.setFilter(FilterConstants.FILTER_4);
            show.setIp(IP.getIPv6FromIPv4(ip));
        }

        List<TestShow> showsWithNoCookie = getUniqueShow(getDateTime(), 10);
        for (TestShow show : showsWithNoCookie) {
            show.setCookie("");
            show.setFilter(FilterConstants.FILTER_4);
            show.setIp(IP.getIPv6FromIPv4(IP.generateIpFromBlackSubnet()));
        }

        shows.addAll(showsWithNoCookie);
        return showsPreparer(shows, 4);
    }

    public List<TestShow> generateFor08Filter() {
        //Данные с 8 фильтром
        List<TestShow> shows = prepareShowsFor8filter(getUniqueShow(getDateTime(), 5));
        //Данные с нулевым фильтром
        List<TestShow> otherShows = prepareShowsFor08filter(getUniqueShow(getDateTime(), 2));
        shows.addAll(otherShows);
        return showsPreparer(shows, 8);
    }


    public List<TestShow> generateFor08FilterEmptyCookie() {
        List<TestShow> shows = prepareShowsFor8filterNoCookie(getUniqueShow(getDateTime(), 5));
        return showsPreparer(shows, 8);
    }

    private <T extends TestShow> List<T> prepareShowsFor8filterNoCookie(List<T> shows) {
        List<T> showsWithoutCookie = prepareShowsFor8filter(shows);
        String ip = IP.generateValidIPv4AsIPv6();
        showsWithoutCookie.stream().forEach(s -> {
            s.setIp(ip);
            s.setCookie("");
        });
        showsWithoutCookie.stream().forEach(s -> {
            s.setYandexUid("");
            s.setIp(ip);
        });
        return showsWithoutCookie;
    }

    private <T extends TestShow> List<T> prepareShowsFor08filter(List<T> shows) {
        if (shows == null || shows.isEmpty()) {
            return new ArrayList<>();
        }
        TestShow showOne = shows.get(0);
        String cookie = Cookie.generateCookieForClickTime(getDateTime());
        String md5 = Ids.generateWareMD5();
        String query = "Test_query_" + 9;
        int hyperId = Ids.generateHyperId();
        if (showOne instanceof TestShow) {
            showOne.setEventtime(showOne.getEventtime().minusMinutes(1));
            for (T show : shows) {
                show.setOriginalQuery(query);
                show.setWareMd5(md5);
                show.setCookie(cookie);
                show.setFilter(FilterConstants.FILTER_0);
            }
        } else if (showOne instanceof TestShow) {
            showOne.setEventtime(showOne.getEventtime().minusMinutes(1));
            for (T show : shows) {
                show.setYandexUid(cookie);
                show.setOriginalQuery(query);
                ((TestShow) show).setHyperId(hyperId);
                show.setFilter(FilterConstants.FILTER_0);
            }
        }
        return shows;
    }

    private <T extends TestShow> List<T> prepareShowsFor8filter(List<T> shows) {
        if (shows == null || shows.isEmpty()) {
            return new ArrayList<>();
        }
        String cookie = Cookie.generateCookieForClickTime(getDateTime());
        String md5 = Ids.generateWareMD5();
        int hyperId = Ids.generateHyperId();
        String query = "Test_query_" + 9;
        for (TestShow show : shows) {
            if (show instanceof TestShow) {
                show.setOriginalQuery(query);
                show.setWareMd5(md5);
                show.setCookie(cookie);
                show.setFilter(FilterConstants.FILTER_8);
            } else if (show instanceof TestShow) {
                show.setYandexUid(cookie);
                show.setHyperId(hyperId);
                show.setOriginalQuery(query);
                show.setFilter(FilterConstants.FILTER_8);
            }
        }
        TestShow withoutFilter;
        if (shows.get(0) instanceof TestShow) {
            withoutFilter = ((List<TestShow>) shows).stream().min(Comparator.comparing((s) -> s.getShowUid())).orElse(null);
            withoutFilter.setFilter(FilterConstants.FILTER_0);
        } else if (shows.get(0) instanceof TestShow) {
            withoutFilter = ((List<TestShow>) shows).stream().min(Comparator.comparing((s) -> s.getShowUid())).orElse(null);
            withoutFilter.setFilter(FilterConstants.FILTER_0);
        }
        return shows;
    }

    public List<TestShow> generateFor09Filter() {
        DateTime showTime = getDateTime();
        String cookie = Cookie.generateCookieForClickTime(showTime);
        List<TestShow> shows = getUniqueShow(showTime, 1800);
        for (TestShow show : shows) {
            show.setPp(getReportPP());
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_9);
            show.setCookie(cookie);
            showTime = showTime.minusSeconds(5); //making all shows within 24 hours
        }
        List<TestShow> showsFor0Filter = getUniqueShow(showTime, 20);
        for (TestShow show : showsFor0Filter) {
            show.setPp(1);
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_0);
            show.setCookie(cookie);
            showTime = showTime.minusSeconds(2);
        }
        shows.addAll(showsFor0Filter);
        return showsPreparer(shows, 9);
    }

    public List<TestShow> generateFor13FilterInstead9EmptyCookie() {
        DateTime showTime = getDateTime();
        String ip6 = IP.generateValidIPv4AsIPv6();
        List<TestShow> shows = getUniqueShow(showTime, 2001);
        for (TestShow show : shows) {
            show.setPp(getReportPP());
            show.setIp("");
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_13);
            show.setCookie("");
            show.setIp(ip6);
            showTime = showTime.minusSeconds(5); //making all shows within 24 hours
        }
        return showsPreparer(shows, 13);
    }

    public List<TestShow> generateFor10Filter() {
        DateTime showTime = PeriodUtils.truncateToMinute(getDateTime());
        String cookie = Cookie.generateCookieForClickTime(showTime);
        List<TestShow> shows = getUniqueShow(showTime, 201);
        for (TestShow show : shows) {
            show.setPp(getReportPP());
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_10);
            show.setCookie(cookie);
            showTime = showTime.plusMillis(200); //making all shows within one minute
        }
        List<TestShow> showsFor0Filter = getUniqueShow(showTime, 20);
        for (TestShow show : showsFor0Filter) {
            show.setPp(1);
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_0);
            show.setCookie(cookie);
            showTime = showTime.plusMillis(100);
        }
        shows.addAll(showsFor0Filter);
        return showsPreparer(shows, 10);
    }

    public List<TestShow> generateFor13FilterInsteadOf10() {
        DateTime showTime = PeriodUtils.truncateToMinute(getDateTime());
        List<TestShow> showsEmptyCookie = getUniqueShow(showTime, 201);
        String ip6 = IP.generateValidIPv4AsIPv6();
        for (TestShow show : showsEmptyCookie) {
            show.setPp(getReportPP());
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_13);
            show.setCookie("");
            show.setIp("");
            show.setIp(ip6);
            showTime = showTime.plusMillis(200); //making all shows within one minute
        }
        return showsPreparer(showsEmptyCookie, 13);
    }

    private int getReportPP() {
        return RndUtil.choice(reportPPS);
    }

    public List<TestShow> generateFor11Filter() {
        DateTime showTime = getDateTime();
        String cookie = Cookie.generateCookieForClickTime(showTime);
        List<TestShow> shows = getUniqueShow(showTime, 861);
        for (TestShow show : shows) {
            show.setPp(getReportPP());
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_11);
            show.setCookie(cookie);
            showTime = showTime.minusSeconds(1); //making all shows within one hour
        }

        List<TestShow> showsFor0Filter = getUniqueShow(showTime, 10);
        for (TestShow show : showsFor0Filter) {
            show.setPp(1);
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_0);
            show.setCookie(cookie);
            showTime = showTime.minusMillis(500);
        }
        shows.addAll(showsFor0Filter);
        return showsPreparer(shows, 11);
    }

    public List<TestShow> generateFor13FilterInsteadOf11() {
        DateTime showTime = getDateTime();
        List<TestShow> showsEmptyCookie = getUniqueShow(showTime, 1001);
        String ip6 = IP.generateValidIPv4AsIPv6();
        for (TestShow show : showsEmptyCookie) {
            show.setPp(getReportPP());
            show.setEventtime(showTime);
            show.setFilter(FilterConstants.FILTER_13);
            show.setCookie("");
            show.setIp("");
            show.setIp(ip6);
            showTime = showTime.minusSeconds(1); //making all shows within one hour
        }
        return showsPreparer(showsEmptyCookie, 13);
    }

    public ClicksAndShows generateFor12Filter() {
        return new ClicksAndShows(10, 10, getDateTime())
                .withClicksFilter(FilterConstants.FILTER_1)
                .withShowsFilter(FilterConstants.FILTER_12)
                .withCommonBlockId();
    }

    public List<TestShow> generateFor13Filter() {
        DateTime showTime = getDateTime();
        String emptyCookie = "";

        List<TestShow> shows = getUniqueShow(showTime, 20);
        int i = 0;
        for (TestShow show : shows) {
            show.setPp(getReportPP());
            show.setEventtime(showTime);
            showTime = showTime.minusSeconds(1);
            show.setFilter(FilterConstants.FILTER_13);
            if (i < 10) {
                show.setCookie(emptyCookie);
            } else {
                show.setCookie(show.getIp());
            }
        }

        i = 0;
        List<TestShow> showsFor0Filter = getUniqueShow(showTime, 10);
        for (TestShow show : showsFor0Filter) {
            show.setPp(1);
            show.setEventtime(showTime);
            showTime = showTime.minusSeconds(1);
            show.setFilter(FilterConstants.FILTER_0);
            if (i < 5) {
                show.setCookie(emptyCookie);
            } else {
                show.setCookie(show.getIp());
            }

        }
        shows.addAll(showsFor0Filter);
        return showsPreparer(shows, 13);
    }

    public List<TestShow> generateForAllFilters() {
        List<TestShow> shows = generateForNoneFilter();
        shows.addAll(generateFor02Filter());
        shows.addAll(generateFor04Filter());
        shows.addAll(generateFor08Filter());
        shows.addAll(generateFor08FilterEmptyCookie());
        shows.addAll(generateFor09Filter());
        shows.addAll(generateFor13FilterInstead9EmptyCookie());
        shows.addAll(generateFor10Filter());
        shows.addAll(generateFor13FilterInsteadOf10());
        shows.addAll(generateFor11Filter());
        shows.addAll(generateFor13FilterInsteadOf11());
        shows.addAll(generateFor13Filter());
        return shows;
    }

    private List<TestShow> getUniqueShow(DateTime timeOfClicks, int count) {
        List<String> rowIds = Ids.uniqueRowIdStartingFrom("click_rowid_", count);
        return showsWithRowIds(rowIds, timeOfClicks);
    }

    private List<TestShow> showsWithRowIds(Collection<String> rowIds, DateTime period) {
        List<TestShow> shows = new ArrayList<>();
        for (String rowId : rowIds) {
            TestShow show = ShowGenerator.uniqueShow(period, rowId);
            show.setPp(PP.getRandomMarketNotReportPP());
            shows.add(show);
        }
        return shows;
    }

    public List<TestShow> showsPreparer(List<TestShow> shows, int showsSetNumber) {
        String prefixTemplate = String.format("%s_F%02d_V%%02d_", getPrefix(), showsSetNumber);
        for (TestShow show : shows) {
            String prefix = String.format(prefixTemplate, show.getFilter().id());
            if (!show.getRowid().contains("click_rowid_")) {
                throw new IllegalArgumentException("Test shows should contain prefix " + "click_rowid_");
            }
            String rowId = show.getRowid().replaceAll("click_rowid_", prefix);
            show.setRowid(rowId);
        }
        return shows;
    }

}
