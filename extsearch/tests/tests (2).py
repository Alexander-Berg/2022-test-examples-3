import pytest
from yatest import common

from rtmapreduce.tests.yatest import rtmr_test


manifests = {
    "video_actions": {
        "input_tables": [
            "user_sessions_for_video_sample"
        ],
        "output_tables": [
            "video_actions",
            "video_actions_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_rejects"
        ]
    },

    "video_actions_part1": {
        "input_tables": [
            "user_sessions_for_video_samp_part1"
        ],
        "output_tables": [
            "video_actions_part1",
            "video_actions_part1_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_part1_rejects"
        ]

    },

    "video_actions_part2": {
        "input_state": [
            "video_actions_part2_state"
        ],
        "input_tables": [
            "user_sessions_for_video_samp_part2"
        ],
        "output_tables": [
            "video_actions_part2",
            "video_actions_part2_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_part2_rejects"

        ]
    },

    "video_actions_tvo": {
        "input_tables": [
            "user_sessions_for_video_tvo"
        ],
        "output_tables": [
            "video_actions_tvo",
            "video_actions_tvo_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_tvo_rejects"
        ]
    },

    "video_actions_user_subscription": {
        "input_tables": [
            "user_sessions_for_video_user_subscription"
        ],
        "output_tables": [
            "video_actions_user_subscription",
            "video_actions_user_subscription_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_user_subscription_rejects"
        ]
    },

    "video_actions_short_search": {
        "input_tables": [
            "user_sessions_for_video_short_search"
        ],
        "output_tables": [
            "video_actions_short_search",
            "video_actions_short_search_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_short_search_rejects"
        ]
    },

    "video_actions_short_tvo": {
        "input_tables": [
            "user_sessions_for_video_short_tvo"
        ],
        "output_tables": [
            "video_actions_short_tvo",
            "video_actions_short_tvo_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_short_tvo_rejects"
        ]
    },

    "video_actions_doc2doc": {
        "input_tables": [
            "user_sessions_for_video_doc2doc"
        ],
        "output_tables": [
            "video_actions_doc2doc",
            "video_actions_doc2doc_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_doc2doc_rejects"
        ]
    },

    "video_actions_porno": {
        "input_tables": [
            "user_sessions_for_video_porno_search"
        ],
        "output_tables": [
            "video_actions_porno_search",
            "video_actions_porno_search_rejects"
        ],
        "expect_empty_tables": [
            "video_actions_porno_search_rejects"
        ]
    },

    "ugc_actions": {
        "input_tables": [
            "splitted_ugc_db_update_log",
            "ugc_update_log_not_interested"
        ],
        "output_tables": [
            "ugc_actions",
            "ugc_actions_rejects"
        ],
        "expect_empty_tables": [
            "ugc_actions_rejects"
        ]
    },
    "reaction_actions": {
        "input_tables": [
            "ugc_reaction_remap"
        ],
        "output_tables": [
            "reaction_actions",
            "reaction_actions_rejects"
        ],
        "expect_empty_tables": [
            "reaction_actions_rejects"
        ]
    },

    "reaction_remap": {
        "input_tables": [
            "ugc_test_reaction_updates_log",
        ],
        "output_tables": [
            "reaction_remap",
        ],
    },

    "spy_actions": {
        "input_tables": [
            "user_sessions_for_video_spy"
        ],
        "output_tables": [
            "spy_actions",
            "spy_actions_rejects"
        ],
        "expect_empty_tables": [
            "spy_actions_rejects"
        ]
    },

    "strm_actions": {
        "input_tables": [
            "raw_strm"
        ],
        "output_tables": [
            "strm_actions/actions",
            "strm_actions/rejects"
        ],
        "expect_empty_tables": [
            "strm_actions/rejects"
        ]
    },

    "strm_actions_first_heartbeat": {
        "input_tables": [
            "remapped_strm"
        ],
        "output_tables": [
            "strm_actions/actions",
            "strm_actions/rejects"
        ],
        "expect_empty_tables": [
            "strm_actions/rejects"
        ]
    },

    "strm_actions_reqid": {
        "input_tables": [
            "reqid_strm"
        ],
        "output_tables": [
            "strm_actions/actions",
            "strm_actions/rejects"
        ],
        "expect_empty_tables": [
            "strm_actions/rejects"
        ]
    },

    "strm_actions_icookie": {
        "input_tables": [
            "icookie_strm"
        ],
        "output_tables": [
            "strm_actions/actions",
            "strm_actions/rejects"
        ],
        "expect_empty_tables": [
            "strm_actions/rejects"
        ]
    },

    "reqans_actions": {
        "input_tables": [
            "raw_logs_video_rt_vh_apphost_log"
        ],
        "output_tables": [
            "reqans_actions/actions",
            "reqans_actions/rejects"
        ],
        "expect_empty_tables": [
            "reqans_actions/rejects"
        ]
    },

    "app_metrika_remap": {
        "input_tables": [
            "raw_app_metrika"
        ],
        "output_tables": [
            "app_metrika_actions/remapped_logs"
        ],
        "expect_empty_tables": [
        ]
    },

    "app_metrika_station_views_remap": {
        "input_tables": [
            "station_views"
        ],
        "output_tables": [
            "app_metrika_actions/remapped_logs"
        ],
        "expect_empty_tables": [
        ]
    },

    "app_metrika_station_views_actions": {
        "input_tables": [
            "station_views"
        ],
        "output_tables": [
            "app_metrika_actions/actions",
            "app_metrika_actions/rejects",
        ],
        "expect_empty_tables": [
            "app_metrika_actions/rejects"
        ]
    },

    "app_metrika_actions": {
        "input_tables": [
            "raw_app_metrika"
        ],
        "output_tables": [
            "app_metrika_actions/actions",
            "app_metrika_actions/rejects",
        ],
        "expect_empty_tables": [
            "app_metrika_actions/rejects"
        ]
    },

    "app_metrika_tv_actions": {
        "input_tables": [
            "metrika_mobile_tv_log"
        ],
        "output_tables": [
            "app_metrika_tv/actions",
            "app_metrika_tv/rejects",
        ],
        "expect_empty_tables": [
            "app_metrika_tv/rejects"
        ]
    },

    "kinopoisk_remap": {
        "input_tables": [
            "raw_kinopoisk"
        ],
        "output_tables": [
            "kinopoisk_actions/remapped_logs"
        ],
        "expect_empty_tables": [
        ]
    },

    "kinopoisk_actions": {
        "input_tables": [
            "raw_kinopoisk"
        ],
        "output_tables": [
            "kinopoisk_actions/actions",
            "kinopoisk_actions/rejects"
        ],
        "expect_empty_tables": [
            "kinopoisk_actions/rejects"
        ]
    },

    "kinopoisk_deep_view_actions": {
        "input_tables": [
            "kinopoisk_deep_remapped"
        ],
        "output_tables": [
            "kinopoisk_actions/actions",
            "kinopoisk_actions/rejects"
        ],
        "expect_empty_tables": [
            "kinopoisk_actions/rejects"
        ]
    },

    "kinopoisk_interrupted_actions": {
        "input_tables": [
            "kinopoisk_interruptions_remapped"
        ],
        "output_tables": [
            "kinopoisk_actions/actions",
            "kinopoisk_actions/rejects"
        ],
        "expect_empty_tables": [
            "kinopoisk_actions/rejects"
        ]
    }

}


@pytest.mark.parametrize("task", manifests.keys())
def test_rtmr_actions_exporter(task, tmpdir):
    rtmr_test.init(tmpdir)
    manifest = manifests[task]

    manifest["dynlibs"] = [
        common.binary_path("extsearch/video/kernel/profile/tools/rtmr_user_actions/dynlib/librtmr_user_actions-dynlib.so")
    ]
    manifest["config"] = common.source_path("extsearch/video/kernel/profile/tools/rtmr_user_actions/tests/" + task + ".cfg")

    return rtmr_test.run(task, manifest, split_files=True, output_format="simple")
