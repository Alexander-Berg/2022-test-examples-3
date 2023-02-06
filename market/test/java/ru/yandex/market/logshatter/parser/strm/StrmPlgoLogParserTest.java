package ru.yandex.market.logshatter.parser.strm;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.ParserException;

class StrmPlgoLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new StrmPlgoLogParser());
        checker.setHost("testhost");
    }

    @Test
    @SuppressWarnings("MethodLength")
    void parseBalancingLog() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/plgo.log", 0);
        checker.check(
            line,
            new Date(1617282800920L),
            // timestamp_ms
            1617282800.920d,
            // host
            "testhost",
            // level
            "INFO",
            // caller
            "logging/logger.go:100",
            // msg
            "balancer log",
            // error
            "",

            // component
            "web",
            // endpoint
            "vod",
            // channel
            "",
            // content_id
            "/vh-ottenc-converted/vod-content/4a934decb013b81fa7985c6477f817c6/" +
                "7923430x1615374741x95c60aaa-8d26-4475-9def-66efc3146d72",
            // real_ip
            "::ffff:62.245.63.99",
            // request_id
            "",
            // request_uri
            "",
            // pusid
            "",
            // vsid
            "xx",

            // playlist_type
            "dash",
            // hls_playlist
            "",

            // content_top_item_cumulative_size
            7033370946829L,
            // content_top_item_index
            8648L,
            // is_hot_top_item
            false,
            // is_manual_top_item
            false,
            // is_top_item
            true,
            // zone
            "vod",
            // balancer_log_details
            "[{\"subnet\":\"62.245.63.0/24\"},{\"subnet\":\"62.245.63.0/24\",\"links\":[1537,1528,1514,1511,1523," +
                "1520,55,222,193],\"err\":null},{\"link_id\":1537,\"location\":\"stder\",\"err\":null}," +
                "{\"link_id\":1537,\"location\":\"stder\",\"err\":null},{\"link_id\":1528,\"location\":\"mskert\"," +
                "\"err\":null},{\"link_id\":1528,\"location\":\"mskert\",\"err\":null},{\"link_id\":1514," +
                "\"location\":\"kivretn\",\"err\":null},{\"link_id\":1514,\"location\":\"kivretn\",\"err\":null}," +
                "{\"link_id\":1511,\"location\":\"spbretn\",\"err\":null},{\"link_id\":1511,\"location\":\"spbretn\"," +
                "\"err\":null},{\"link_id\":1523,\"location\":\"spbrt\",\"err\":null},{\"link_id\":1520," +
                "\"location\":\"marrt\",\"err\":null},{\"link_id\":55,\"location\":\"mskneun\",\"err\":null}," +
                "{\"link_id\":55,\"location\":\"mskneun\",\"err\":null},{\"link_id\":222,\"location\":\"mskm9\"," +
                "\"err\":null},{\"link_id\":222,\"location\":\"mskm9\",\"err\":null},{\"link_id\":193," +
                "\"location\":\"spb\",\"err\":null},{\"subnet\":\"0.0.0.0/0\"},{\"subnet\":\"0.0.0.0/0\"," +
                "\"links\":[1529,154,1503,1501,1505,159,151,209,155],\"err\":null},{\"link_id\":1529," +
                "\"location\":\"itt\",\"err\":null},{\"link_id\":1529,\"location\":\"itt\",\"err\":null}," +
                "{\"link_id\":154,\"location\":\"kiv\",\"err\":null},{\"link_id\":154,\"location\":\"kiv\"," +
                "\"err\":null},{\"link_id\":1503,\"location\":\"cogent\",\"err\":null},{\"link_id\":1503," +
                "\"location\":\"cogent\",\"err\":null},{\"link_id\":1501,\"location\":\"telia\",\"err\":null}," +
                "{\"link_id\":1501,\"location\":\"telia\",\"err\":null},{\"link_id\":1505,\"location\":\"level3\"," +
                "\"err\":null},{\"link_id\":1505,\"location\":\"level3\",\"err\":null},{\"link_id\":159," +
                "\"location\":\"rad\",\"err\":null},{\"link_id\":159,\"location\":\"rad\",\"err\":null}," +
                "{\"link_id\":151,\"location\":\"rad\",\"err\":null},{\"link_id\":151,\"location\":\"rad\"," +
                "\"err\":null},{\"link_id\":209,\"location\":\"kiv\",\"err\":null},{\"link_id\":209," +
                "\"location\":\"kiv\",\"err\":null},{\"link_id\":155,\"location\":\"kiv\",\"err\":null}," +
                "{\"link_id\":155,\"location\":\"kiv\",\"err\":null},{\"subnet\":null}]",
            // link_ids
            new Integer[]{
                1537, 1537, 1537, 1528, 1528, 1528, 1514, 1514, 1514, 1511, 1511, 1511, 1523, 1520, 55, 55,
                55, 222, 222, 222, 193, 1529, 1529, 1529, 154, 154, 154, 1503, 1503, 1503, 1501, 1501, 1501, 1505,
                1505, 1505, 159, 159, 159, 151, 151, 151, 209, 209, 209, 155, 155, 155
            },
            // locations
            new String[]{
                "", "stder", "stder", "", "mskert", "mskert", "", "kivretn", "kivretn", "", "spbretn",
                "spbretn", "", "", "", "mskneun", "mskneun", "", "mskm9", "mskm9", "", "", "itt", "itt", "", "kiv",
                "kiv", "", "cogent", "cogent", "", "telia", "telia", "", "level3", "level3", "", "rad", "rad", "",
                "rad", "rad", "", "kiv", "kiv", "", "kiv", "kiv"
            },
            // fqdns
            new String[]{
                "", "", "ext-strm-stder15.strm.yandex.net", "", "", "ext-strm-mskert20.strm.yandex.net", "",
                "", "ext-strm-kivretn20.strm.yandex.net", "", "", "ext-strm-spbretn11.strm.yandex.net", "", "", "",
                "", "ext-strm-mskneun20.strm.yandex.net", "", "", "strm-mskm901.strm.yandex.net", "", "", "",
                "ext-strm-itt06.strm.yandex.net", "", "", "strm-kiv20.strm.yandex.net", "", "",
                "ext-strm-cogent09.strm.yandex.net", "", "", "ext-strm-telia02.strm.yandex.net", "", "",
                "ext-strm-level309.strm.yandex.net", "", "", "strm-rad14.strm.yandex.net", "", "",
                "strm-rad14.strm.yandex.net", "", "", "strm-kiv20.strm.yandex.net", "", "", "strm-kiv20.strm.yandex.net"
            },
            // chosen
            new Boolean[]{
                true, true, true, true, true, true, true, true, true, true, true, true, false, false, true, true,
                true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true, true, true, true, true, true
            },
            // check_host_available
            new String[]{"", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "", "", "y", "", "", "y", "",
                "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y",
                "", "", "y"},
            // check_host_drop
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_host_found
            new String[]{"", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "", "", "y", "", "", "y", "",
                "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y",
                "", "", "y"},
            // check_host_hot_top_slowdown
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_host_reject_drop_probability_not_zero
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_host_slowdown
            new String[]{"", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "", "", "n", "", "", "n", "",
                "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n",
                "", "", "n"},
            // check_link_drop
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_link_found
            new String[]{"y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "y", "y", "", "", "y", "", "", "y",
                "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "",
                "", "y", "", ""},
            // check_link_reject_drop_probability_not_zero
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_link_reject_internal
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_link_slowdown
            new String[]{"n", "", "", "n", "", "", "n", "", "", "n", "", "", "y", "y", "n", "", "", "n", "", "", "y",
                "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "", "", "n", "",
                "", "n", "", ""},
            // check_location_alive
            new String[]{"", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "", "", "y", "", "", "y", "", "",
                "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "",
                "", "y", ""},
            // check_location_cold_content_allowed
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // check_location_content_is_cached
            new String[]{"", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "", "", "y", "", "", "y", "", "",
                "", "y", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", ""},
            // check_location_found
            new String[]{"", "y", "y", "", "y", "y", "", "y", "y", "", "y", "y", "", "", "", "y", "y", "", "y", "y",
                "", "", "y", "y", "", "y", "y", "", "y", "y", "", "y", "y", "", "y", "y", "", "y", "y", "", "y", "y",
                "", "y", "y", "", "y", "y"},
            // check_location_ok
            new String[]{"", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "", "", "y", "", "", "y", "", "",
                "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "",
                "", "y", ""},
            // item_is_cached
            new String[]{"", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "", "", "y", "", "", "y", "", "",
                "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "", "", "y", "",
                "", "y", ""},
            // location_cache_capacity
            new Long[]{-1L, 43417202230872L, -1L, -1L, 163364741487776L, -1L, -1L, 63792942935210L, -1L, -1L,
                32977625889964L, -1L, -1L, -1L, -1L, 163364741487776L, -1L, -1L, 163364741487776L, -1L, -1L, -1L,
                17663716079812L, -1L, -1L, 63792942935210L, -1L, -1L, 63792942935210L, -1L, -1L, 50473234685920L,
                -1L, -1L, 63792942935210L, -1L, -1L, 50473234685920L, -1L, -1L, 50473234685920L, -1L, -1L,
                63792942935210L, -1L, -1L, 63792942935210L, -1L},
            // location_cache_capacity_error
            new String[]{"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
            // final_link_ids
            new Integer[]{},
            // final_locations
            new String[]{},
            // final_fqdns
            new String[]{},
            // final_no_cache
            new String[]{},

            // method
            "",
            // status
            0,
            // result
            "",
            // signed
            false,
            // quality
            "",
            // redundant
            0,

            // backend
            "",

            // liveinfo_channels_count
            0,
            // old_channels_count
            0,
            // new_channels_count
            0,

            // channel_pusid
            "",
            // old_live_edge
            new Date(0),
            // new_live_edge
            new Date(0),
            // ps_type
            "",
            // ps_last_modified
            new Date(0),
            // ps_last_updated
            new Date(0),
            // ps_live_edge
            new Date(0),
            // ps_live_edge_advanced
            new Date(0),
            // ps_live_edge_by_formula
            new Date(0),
            // time_window_type
            "",
            // time_window_start
            0L,
            // time_window_end
            0L,

            // channel_enabled
            false,
            // dvr
            0L,
            // playlist_last_chunk
            0L,
            // user_started_new_session
            false,
            // user_window_type
            "",
            // user_window_start
            0L,
            // user_window_end
            0L,

            // user_controls_dvr
            0L,
            // user_controls_force_dvr
            0L,
            // user_controls_shift
            0L,
            // user_controls_start
            new Date(0),
            // user_controls_end
            new Date(0)
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void parseChannelsLog() throws Exception {
        String line = StrmTestUtils.readTestLine("strm/plgo.log", 1);
        checker.check(
            line,
            new Date(1617634846607L),
            // timestamp_ms
            1617634846.607d,
            // host
            "testhost",
            // level
            "INFO",
            // caller
            "livechan/calculation.go:153",
            // msg
            "playlist source",
            // error
            "",

            // component
            "channels",
            // endpoint
            "",
            // channel
            "osetiya_iryston",
            // content_id
            "",
            // real_ip
            "",
            // request_id
            "",
            // request_uri
            "",
            // pusid
            "f630f758d68d6a78",
            // vsid
            "",

            // playlist_type
            "",
            // hls_playlist
            "",

            // content_top_item_cumulative_size
            -1L,
            // content_top_item_index
            -1L,
            // is_hot_top_item
            false,
            // is_manual_top_item
            false,
            // is_top_item
            false,
            // zone
            "",
            // balancer_log_details
            "[]",
            // link_ids
            new Integer[]{},
            // locations
            new String[]{},
            // fqdns
            new String[]{},
            // chosen
            new Boolean[]{},
            // check_host_available
            new String[]{},
            // check_host_drop
            new String[]{},
            // check_host_found
            new String[]{},
            // check_host_hot_top_slowdown
            new String[]{},
            // check_host_reject_drop_probability_not_zero
            new String[]{},
            // check_host_slowdown
            new String[]{},
            // check_link_drop
            new String[]{},
            // check_link_found
            new String[]{},
            // check_link_reject_drop_probability_not_zero
            new String[]{},
            // check_link_reject_internal
            new String[]{},
            // check_link_slowdown
            new String[]{},
            // check_location_alive
            new String[]{},
            // check_location_cold_content_allowed
            new String[]{},
            // check_location_content_is_cached
            new String[]{},
            // check_location_found
            new String[]{},
            // check_location_ok
            new String[]{},
            // item_is_cached
            new String[]{},
            // location_cache_capacity
            new Long[]{},
            // location_cache_capacity_error
            new String[]{},
            // final_link_ids
            new Integer[]{},
            // final_locations
            new String[]{},
            // final_fqdns
            new String[]{},
            // final_no_cache
            new String[]{},

            // method
            "",
            // status
            0,
            // result
            "",
            // signed
            false,
            // quality
            "",
            // redundant
            0,

            // backend
            "",

            // liveinfo_channels_count
            0,
            // old_channels_count
            0,
            // new_channels_count
            0,

            // channel_pusid
            "",
            // old_live_edge
            new Date(0),
            // new_live_edge
            new Date(0),
            // ps_type
            "TypeTRNS",
            // ps_last_modified
            new Date(1617634828000L),
            // ps_last_updated
            new Date(1617634830000L),
            // ps_live_edge
            new Date(1617634760000L),
            // ps_live_edge_advanced
            new Date(1617634838159L),
            // ps_live_edge_by_formula
            new Date(1617634750000L),
            // time_window_type
            "",
            // time_window_start
            0L,
            // time_window_end
            0L,

            // channel_enabled
            false,
            // dvr
            0L,
            // playlist_last_chunk
            0L,
            // user_started_new_session
            false,
            // user_window_type
            "",
            // user_window_start
            0L,
            // user_window_end
            0L,

            // user_controls_dvr
            0L,
            // user_controls_force_dvr
            0L,
            // user_controls_shift
            0L,
            // user_controls_start
            new Date(0),
            // user_controls_end
            new Date(0)
        );
    }

    @Test
    void parseWrongNestedCount() {
        String line = StrmTestUtils.readTestLine("strm/plgo.log", 2);

        ParserException exception = Assertions.assertThrows(ParserException.class, () -> checker.check(line));
        Assertions.assertEquals("Balancing arrays have different lengths", exception.getMessage());
    }
}
