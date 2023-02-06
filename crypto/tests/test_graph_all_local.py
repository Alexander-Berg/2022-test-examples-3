import os
import json
import shutil
from datetime import timedelta

import white

from libs.matchers.table_matchers import (
    check_column_in_table,
    get_precent_yuidfp_in_table,
    YUID_WITH_ALL,
    PUID_LOGIN,
    get_table_column_names,
    get_param_from_str,
    get_all_tables,
    FP_ALL_LIST,
)
import pytest
import allure
from rtcconf import config
from testdata import data_set

FILE_UPLOADS = []
TEST_DATA = ""

TEST_FAILED = False

YT_DIR = "yt-data"
YT_ID = "yt-graph-all-local-test"
PROXY_PORT = 9013

dir_path = os.path.dirname(os.path.realpath(__file__))

DEVID_RAW_PATH = "//crypta/production/state/indevice/2016-04-11/perfect/devid_raw_day/"
PAIRS_PATH = "//crypta/production/state/graph/2016-04-11/pairs/"


def prepare_local_cypress(test_data, cypress_dir):
    shutil.copytree(test_data, cypress_dir)


def mark_file_uploads(prepared_dir):
    all_files = []
    for root, dirs, files in os.walk(prepared_dir):
        for filename in files:
            all_files.append(os.path.join(root, filename))

    file_uploads = []
    for fname in all_files:
        if not fname.endswith(".meta"):
            meta = fname + ".meta"
            with open(meta, "wb") as f:
                if "geodata4.bin" in fname or fname.endswith(".vw") or "IPOperators.xml" in fname:
                    f.write('{"type" = "file";}\n')
                    file_uploads.append((fname, "/" + fname[len(prepared_dir) :]))
                else:
                    f.write('{"type" = "table"; "format" = "json";}\n')
    return file_uploads


def prepare_cypress():
    global FILE_UPLOADS
    global TEST_DATA
    extracted_test_data = os.getenv("LOCAL_GRAPH_TEST_DATA")
    FILE_UPLOADS = mark_file_uploads(extracted_test_data)
    TEST_DATA = extracted_test_data
    return extracted_test_data


def get_file_uploads():
    return FILE_UPLOADS


def get_node_config():
    script_dir, _script_name = os.path.split(__file__)
    return os.path.join(script_dir, "node_config.yson")


def get_scheduler_config():
    script_dir, _script_name = os.path.split(__file__)
    return os.path.join(script_dir, "scheduler_config.yson")


@pytest.mark.usefixtures("graph")
@pytest.mark.usefixtures("ytlocal")
class TestGraphAllLocal(object):
    def test_graph_run(self, graph):
        allure.attach("YT errors", (json.dumps(graph.report.errors, sort_keys=True, indent=4)))
        allure.attach("Max execution time", str(graph.report.max_time))
        assert not graph.report.errors
        assert graph.report.max_time < timedelta(minutes=10)
        if graph.run_status is None:
            assert 0, "Graph run status is not defined"
        assert graph.run_status, "Graph fail"

    def test_percent_yuid_from_fp_in_yuidwithall(self, graph):
        percent_yuidfp_in_yuidwithall = 56
        check_column_in_table(graph.yt, YUID_WITH_ALL, "reg_fp_dates", percent_yuidfp_in_yuidwithall)

    def test_something_uploaded_to_bb(self, graph):
        with pytest.allure.step("at least some yuids and devids yuids are uploaded to bb"):
            all_yuids = set(
                r["yuid"]
                for r in graph.yt.read_table("//crypta/production/state/graph/dicts/yuid_with_all", raw=False)
            )
            allure.attach("all yuids count", str(len(all_yuids)))

            all_devids = set(
                r["key"]
                for r in graph.yt.read_table(
                    "//crypta/production/state/indevice/2016-04-11/dev_yuid_info_ua", raw=False
                )
            )
            allure.attach("all devids count", str(len(all_devids)))

            # need to map devids to corresponding crypta id because of stupid upload logic
            devid_to_cid_mapping = {
                r["devid"]: r["cid"]
                for r in graph.yt.read_table("//crypta/production/state/graph/dicts/exact_devid_cid", raw=False)
            }
            all_crypta_ids_instead_of_devids = set(
                devid_to_cid_mapping[d] for d in all_devids if d in devid_to_cid_mapping
            )
            allure.attach("all cryptaid-devids count", str(len(all_crypta_ids_instead_of_devids)))

            uploaded_keys = set(
                r["key"]
                for r in graph.yt.read_table(
                    "//crypta/production/state/graph/2016-04-11/upload_bb/cid_to_bb_with_experiments", raw=False
                )
            )
            allure.attach("uploaded keys count", str(len(uploaded_keys)))

            assert len(all_yuids) > 0
            assert len(all_devids) > 0
            assert len(all_crypta_ids_instead_of_devids) > 0
            assert len(uploaded_keys) > 0

            uploaded_yuids = all_yuids.intersection(uploaded_keys)
            allure.attach("uploaded yuids count", str(len(uploaded_yuids)))
            uploaded_devids = all_crypta_ids_instead_of_devids.intersection(uploaded_keys)
            allure.attach("uploaded devids count", str(len(uploaded_devids)))

            assert len(uploaded_yuids) > 0
            assert len(uploaded_devids) > 0

    def test_vertices_dicts_are_ready(self, graph):
        with pytest.allure.step("at least some yuids and devids yuids are uploaded to bb"):
            all_yuids = set(
                r["yuid"]
                for r in graph.yt.read_table("//crypta/production/state/graph/dicts/yuid_with_all", raw=False)
            )
            allure.attach("all yuids count", str(len(all_yuids)))

            all_devids = set(
                r["key"]
                for r in graph.yt.read_table(
                    "//crypta/production/state/indevice/2016-04-11/dev_yuid_info_ua", raw=False
                )
            )
            allure.attach("all devids count", str(len(all_devids)))

            all_keys = all_yuids.union(all_devids)

            for key_col, export_vertices_dict in [
                ("devid", "exact_devid_cid"),
                ("key", "exact_vertices"),
                ("yuid", "exact_yuid_cid"),
                ("devid", "fuzzy_devid_cid"),
                ("key", "fuzzy_vertices"),
                ("yuid", "fuzzy_yuid_cid"),
            ]:
                dict_table = "//crypta/production/state/graph/dicts/%s" % export_vertices_dict
                dict_table_keys = [r[key_col] for r in graph.yt.read_table(dict_table, raw=False)]
                keys_found = all_keys.intersection(dict_table_keys)

                allure.attach("%s keys count" % export_vertices_dict, str(len(keys_found)))
                assert len(keys_found) > 0

    def test_kinopoisk_email_in_yuidwithall(self, graph):
        kinopoisk_emails = set(
            record["email"]
            for record in graph.yt.read_table("//crypta/production/state/graph/dicts/kinopoisk", raw=False)
        )
        yuidwithall_emails_dates = [
            record["email_dates"]
            for record in graph.yt.read_table(YUID_WITH_ALL, raw=False)
            if record.get("email_dates")
        ]
        allure.attach("Kinopoisk emails", str(kinopoisk_emails))
        allure.attach("Emails in yuid_with_all", str(yuidwithall_emails_dates))
        all_emails = [email for email_dates in yuidwithall_emails_dates for email in email_dates.iterkeys()]
        for kp_email in kinopoisk_emails:
            assert kp_email in all_emails

    def test_dit_msk_added(self, graph):

        for t in [
            "//crypta/production/state/graph/dicts/yuid_raw/yuid_with_phone_ditmsk",
            "//crypta/production/state/graph/v2/soup/email_md5_dit_id_ditmsk_dit-msk",
            "//crypta/production/state/graph/v2/soup/phone_md5_dit_id_ditmsk_dit-msk",
            "//crypta/production/state/graph/v2/soup/yandexuid_dit_id_ditmsk_dit-msk",
        ]:
            assert graph.yt.exists(t)
            assert graph.yt.row_count(t) > 0

    # def test_partners_yuid_with_all_output(self, graph):
    #     with open(os.path.join(dir_path, YUID_WITH_ALL_PARTNERS_AFTER_PROCESSING_PATH)) as f:
    #         reference = json.load(f)
    #         reference_by_yuid = {str(r['yandexuid']): r for r in reference}
    #     result_all = graph.yt.read_table(
    #         "//crypta/production/state/graph/dicts/yuid_with_all", raw=False
    #     )
    #     result_by_yuid = {str(r['yuid']): r for r in result_all}
    #
    #     for yuid, reference in reference_by_yuid.iteritems():
    #         result = result_by_yuid[yuid]
    #         allure.attach(
    #             "Partners yuid_with_all check for {}".format(yuid),
    #             str({
    #                 "result": result,
    #                 "reference": reference
    #             })
    #         )
    #     # This test for debugging
    #     assert 1 == 1
    #     for yuid, reference in reference_by_yuid.iteritems():
    #         result = result_by_yuid[yuid]
    #         for k, v in reference.iteritems():
    #             if isinstance(v, list):
    #                 assert set(v) == set(result[k])
    #             elif isinstance(v, dict):
    #                 assert json.dumps(v, sort_keys=True) == json.dumps(result[k], sort_keys=True)
    #             else:
    #                 assert v == result[k]

    def test_kinopoisk_pairs(self, graph):
        yuid_cid = {
            record["yuid"]: record["cid"]
            for record in graph.yt.read_table("//crypta/production/state/graph/dicts/exact_yuid_cid", raw=False)
        }
        allure.attach(
            "cids", str(yuid_cid.get("6619110241447613888")) + "; " + str(yuid_cid.get("1495205931451997969"))
        )
        assert yuid_cid.get("6619110241447613888") == yuid_cid.get("1495205931451997969")

    # def test_autoru_email_in_yuidwithall(self, graph):
    #     autoru_all = [
    #         "//statbox/autoru-front-log/2016-04-11"
    #     ]
    #     autoru_all_records = get_all_tables(graph.yt, autoru_all)
    #     autoru_emails = set(record['email'].lower() for record in autoru_all_records)
    #     yuidwithall_emails_dates = [record["email_dates"] for record in graph.yt.read_table(YUID_WITH_ALL, raw=False) if record.get("email_dates")]
    #     allure.attach("Autoru emails", str(autoru_emails))
    #     allure.attach("Emails in yuid_with_all", str(yuidwithall_emails_dates))
    #     all_emails = [email for email_dates in yuidwithall_emails_dates for email in email_dates.iterkeys()]
    #     for autoru_email in autoru_emails:
    #         assert autoru_email in all_emails

    def test_import_people_search(self, graph):
        imported_vk_ids = [
            r["id_value"]
            for r in graph.yt.read_table("//crypta/production/state/graph/dicts/people_search/vk", raw=False)
        ]

        allure.attach("imported people search", str(imported_vk_ids))
        assert "517155" in imported_vk_ids

    def test_vk_people_search_join(self, graph):
        yuid_vk_recs = list(graph.yt.read_table("//crypta/production/state/graph/dicts/yuid_with_id_vk", raw=False))

        vk_yuids = [r["yuid"] for r in yuid_vk_recs]
        vk_ids = [r["id_value"] for r in yuid_vk_recs]

        allure.attach("vk yuids", str(vk_yuids))
        allure.attach("vk ids", str(vk_ids))

        assert vk_yuids
        assert vk_ids

        assert "123666666" in vk_yuids
        assert "124666666" in vk_yuids
        assert "517155" in vk_ids  # decoded Z2NpY2dn
        assert "67677184" in vk_ids  # decoded aGloaWljamY=

    @pytest.mark.skip(reason="graph/2016-04-11/exact/yuid_pairs_stats not exist")
    def test_pair_sources_stats_is_computed(self, graph):
        recs = {
            r["source"] + "." + r["pair_type"]: r["count"]
            for r in graph.yt.read_table(
                "//crypta/production/state/graph/2016-04-11/exact/yuid_pairs_stats", raw=False
            )
        }

        assert len(recs.keys()) > 5

        allure.attach("pair source stats", str(recs))

    def test_vmetro_mac_address_matching(self, graph):
        pairs_table = "//crypta/production/state/graph/2016-04-11/pairs/dev_yuid_pairs_vmetro_indev"
        assert graph.yt.exists(pairs_table)

        device_dict_table = "//crypta/production/state/indevice/2016-04-11/perfect/devid_raw_month/devid_yuid_vmetro"
        recs = [
            r for r in graph.yt.read_table(device_dict_table) if r["devid"] == "e69067fa-c05b-41d3-a174-f8f48ffeb273"
        ]
        allure.attach(str(recs), "vmetro devid-yuid dict recs")

        assert len(recs) == 1

    def test_indevice_nolimit(self, graph):
        from testdata.data_set import testdata_dev_yuid_indevice_perfect_no_limit

        original_table = testdata_dev_yuid_indevice_perfect_no_limit.get_log()
        table_name = original_table.keys()[0]
        new_table = graph.yt.read_table(table_name, raw=False)
        # check : old deviceids are in new table
        new_devids = set(rec["devid"] for rec in new_table)
        assert all(rec["devid"] in new_devids for rec in original_table[table_name])

    def test_ui_private_mode(self, graph):
        bm_yuids = set(
            r["yuid"]
            for r in graph.yt.read_table("//crypta/production/state/graph/dicts/yuid_with_all", raw=False)
            if "ui_bm" in r["sources"]
        )

        allure.attach("browser manager yuids", str(bm_yuids))
        assert len(bm_yuids) > 0

    def test_metrica_sockets(self, graph):
        expected_yuid = "222221455542222"
        expected_devid = "1190cb06-34ea-4995-8a68-8ceed2e55760"
        expected_uuid = "35a6b993b6771036cda94cf37d507d6c"

        uuid_tbl = (
            "//crypta/production/state/indevice/2016-04-11/perfect/devid_raw_month/uuid_yuid_metrica_sockets_android"
        )
        devid_tbl = (
            "//crypta/production/state/indevice/2016-04-11/perfect/devid_raw_month/devid_yuid_metrica_sockets_android"
        )

        assert graph.yt.row_count(uuid_tbl) > 0
        assert graph.yt.row_count(devid_tbl) > 0

        for r in graph.yt.read_table(devid_tbl):
            assert r["yuid"] == expected_yuid
            assert r["devid"] == expected_devid

        for r in graph.yt.read_table(uuid_tbl):
            assert r["yuid"] == expected_yuid
            assert r["uuid"] == expected_uuid

    def test_ssp_apps(self, graph):
        devid = "21f44ec9-7f36-4a56-a529-88326db73750"
        devinfo = "//crypta/production/state/graph/dicts/dev_info_yt"
        recs = list(graph.yt.read_table(graph.yt.TablePath(devinfo, lower_key=devid, upper_key=devid + "\0")))
        assert len(recs) == 1

        apps = recs[0]["apps"]
        assert "com.kathleenOswald.solitaireGooglePlay" in apps
        assert "1234567890" not in apps
        assert "http://yandex.ru" not in apps

    # def test_instagram_puid(self, graph):
    #     insta_recs = [r for r in graph.yt.read_table(
    #         "//crypta/production/state/graph/dicts/yuid_with_all", raw=False
    #     ) if r.get('instagram_login_instagram_pochta_dates', None) or
    #                   r.get('instagram_id_instagram_pochta_dates', None)]
    #     assert len(insta_recs) > 0
    #     assert any((r['instagram_id_instagram_pochta_dates'] and
    #                 ('3534989606' in r['instagram_id_instagram_pochta_dates']) and
    #                 r['yuid'] == '99900011459458000')
    #                for r in insta_recs)
    #     assert any((r['instagram_login_instagram_pochta_dates'] and
    #                 ('test_insta_username_1' in r['instagram_login_instagram_pochta_dates']) and
    #                 r['yuid'] == '99900021459458000')
    #                for r in insta_recs)

    def test_sovetnik_import(self, graph):
        recs = [
            r
            for r in graph.yt.read_table("//crypta/production/state/graph/dicts/yuid_with_all", raw=False)
            if r.get("fb_sovetnik_dates") or r.get("ok_sovetnik_dates") or r.get("vk_sovetnik_dates")
        ]
        assert len(recs) == 1

    def test_strong_edges(self, graph):
        removed_edges = [
            r
            for r in graph.yt.read_table(
                "//crypta/production/state/graph/2016-04-11/exact/cluster/removed_pairs", raw=False
            )
        ]
        strong_source_types = [
            devid_pair_type.source_type
            for devid_pair_type in config.DEVID_PAIR_TYPES_PERFECT
            if devid_pair_type.strong is True
        ]
        strong_pair_type = "d_y"
        assert 0 == len(
            [
                rec
                for rec in removed_edges
                if rec.get("source_type") in strong_source_types and rec.get("pair_type") == strong_pair_type
            ]
        )

    @pytest.mark.parametrize(
        "table,expected",
        [
            ("yuid_with_email_wl_mailru", data_set.WATCH_LOG_MAILRU),
            ("yuid_with_vk_watch_log", data_set.WATCH_LOG_VK),
            ("yuid_with_ok_watch_log", data_set.WATCH_LOG_OK),
            ("yuid_with_avito_watch_log", data_set.WATCH_LOG_AVITO),
        ],
    )
    def test_mailru_from_watch_log(self, graph, table, expected):
        ids_wl = [
            r["id_value"]
            for r in graph.yt.read_table("//crypta/production/state/graph/2016-04-11/yuid_raw/" + table, raw=False)
        ]
        allure.attach("id from " + table, str(ids_wl))
        allure.attach("expected ids", str(expected))
        for id in expected:
            assert id[1] in ids_wl, "Not found id - " + id[1]

    def test_idserv_log_soup(self, graph):
        tbl = "//crypta/production/state/graph/v2/soup/yandexuid_idfa_idserv_idserv"
        assert graph.yt.exists(tbl)
        assert 1 == graph.yt.row_count(tbl)
        recs = list(graph.yt.read_table(tbl))
        assert recs[0]["id1"] == "22222222221459365211"
        assert recs[0]["id2"] == "fake-idfa"

        tbl = "//crypta/production/state/graph/v2/soup/yandexuid_mm_device_id_idserv_idserv"
        assert graph.yt.exists(tbl)
        assert 1 == graph.yt.row_count(tbl)
        recs = list(graph.yt.read_table(tbl))
        assert recs[0]["id1"] == "22222222221459365211"
        assert recs[0]["id2"] == "mmdevid"

        tbl = "//crypta/production/state/graph/v2/soup/mm_device_id_idfa_idserv_idserv"
        assert graph.yt.exists(tbl)
        assert 1 == graph.yt.row_count(tbl)
        recs = list(graph.yt.read_table(tbl))
        assert recs[0]["id1"] == "mmdevid"
        assert recs[0]["id2"] == "fake-idfa"

        tbl = "//crypta/production/state/graph/v2/soup/yandexuid_gaid_idserv_idserv"
        assert graph.yt.exists(tbl)
        assert 0 == graph.yt.row_count(tbl)

    def test_remove_pairs_from_black_list(self, graph):
        keys_from_pairs = [
            r["key"]
            for r in graph.yt.read_table(
                "//crypta/production/state/graph/2016-04-11/pairs/yuid_pairs_login", raw=False
            )
        ]
        removed_keys = [
            r["key"]["key"]
            for r in graph.yt.read_table(
                "//crypta/production/state/graph/2016-04-11/pairs/black_list/removed_pairs_from_black_list", raw=False
            )
        ]
        assert all([bl_key not in keys_from_pairs for bl_key in data_set.BLACK_LIST_KEYS])
        assert all([bl_key in removed_keys for bl_key in data_set.BLACK_LIST_KEYS])

    def test_class_org_for_email(self, graph):
        yuid_with_id_email_path = "//crypta/production/state/graph/dicts/yuid_with_id_email"
        all_id_values = [r["id_value"] for r in graph.yt.read_table(yuid_with_id_email_path, raw=False)]
        keys_from_pairs = [
            r["id_value"] for r in graph.yt.read_table(yuid_with_id_email_path, raw=False) if r["organization"]
        ]
        assert "new_email@bbb.ru" in keys_from_pairs
        assert "new_email@yandex.ru" not in keys_from_pairs
        assert "this_email_no_in_yuid_id@testdomain.ru" not in all_id_values

    def test_xuniqs_soup(self, graph):
        xuniq_soup_tables = [
            "//crypta/production/state/graph/v2/soup/" + x
            for x in [
                "yandexuid_yandexuid_xuniq_xuniq",
                "yandexuid_uuid_xuniq_xuniq",
                "yandexuid_xuniq_guid_xuniq_xuniq",
            ]
        ]
        tables = [
            "//crypta/production/state/graph/v2/soup/" + x
            for x in graph.yt.list("//crypta/production/state/graph/v2/soup")
            if "xuniq" in x
        ]

        assert 3 == len(tables)
        for t in xuniq_soup_tables:
            assert t in tables

        for t in xuniq_soup_tables:
            assert graph.yt.exists(t)
            assert 1 == graph.yt.row_count(t)

        rec = next(graph.yt.read_table(xuniq_soup_tables[0]))
        assert rec["id1"] == "8156730781512180560"
        assert rec["id2"] == "4444441444444444"

        rec = next(graph.yt.read_table(xuniq_soup_tables[1]))
        assert rec["id1"] == "8156730781512180560"
        assert rec["id2"] == "23a05dbc2e9da8a0958f796c28a2519c"

        rec = next(graph.yt.read_table(xuniq_soup_tables[2]))
        assert rec["id1"] == "8156730781512180560"
        assert rec["id2"] == "fakemailruguid"

    def test_toloka_metrics(self, graph):
        stats = "//crypta/production/state/toloka/metrics/2016-04-11/stats"
        assert graph.yt.exists(stats)
        assert graph.yt.row_count(stats) > 0

    @pytest.mark.parametrize(
        "name,uuid",
        [
            ("uuid_contain_trash", data_set.uuid_contain_trash),
            ("uuid_first_pos", data_set.uuid_first_pos),
            ("uuid_last_pos", data_set.uuid_last_pos),
            ("uuid_mid_pos", data_set.uuid_mid_pos),
            ("uuid_contains_hyphens", data_set.uuid_contains_hyphens),
        ],
    )
    def test_extract_uuid_from_redirlog(self, graph, name, uuid):
        uuids = [
            r["uuid"]
            for r in graph.yt.read_table(
                "//crypta/production/state/indevice/2016-04-11/perfect/logs/uuid_yuid_redir_tmp", raw=False
            )
        ]
        errors = [
            r["http_referer"]
            for r in graph.yt.read_table(
                "//crypta/production/state/indevice/2016-04-11/perfect/logs/error_extract_uuids", raw=False
            )
        ]
        allure.attach("uuids", str(uuids))
        allure.attach("errors", str(errors))
        assert uuid in uuids
        for ref in errors:
            assert uuid not in ref

    def test_error_extract_uuid_from_redirlog(self, graph):
        bad_uuid = data_set.uuid_bad_len
        uuids = [
            r["uuid"]
            for r in graph.yt.read_table(
                "//crypta/production/state/indevice/2016-04-11/perfect/logs/uuid_yuid_redir_tmp", raw=False
            )
        ]
        error_refs = [
            r["http_referer"]
            for r in graph.yt.read_table(
                "//crypta/production/state/indevice/2016-04-11/perfect/logs/error_extract_uuids", raw=False
            )
        ]
        assert bad_uuid not in uuids
        assert any(bad_uuid in ref for ref in error_refs)

    def test_toloka_hh_metrics(self, graph):
        stats = "//crypta/production/state/toloka/metrics/2016-04-11/household_exact"
        stats_20 = "//crypta/production/state/toloka/metrics/2016-04-11/household_2_0"
        assert graph.yt.exists(stats)
        assert graph.yt.row_count(stats) > 0
        assert graph.yt.exists(stats_20)
        assert graph.yt.row_count(stats_20) > 0

    def test_watch_log_parse_did(self, graph):
        ios = DEVID_RAW_PATH + "devid_yuid_watch_yp_did_ios"
        andr = DEVID_RAW_PATH + "devid_yuid_watch_yp_did_android"
        pairs_ios = PAIRS_PATH + "dev_yuid_pairs_watch_yp_did_ios_indev"
        pairs_andr = PAIRS_PATH + "dev_yuid_pairs_watch_yp_did_android_indev"
        for path in [ios, andr, pairs_ios, pairs_andr]:
            data = [str(r) for r in graph.yt.read_table(path, raw=False)]
            allure.attach(path, str(data))
        keys_ios = [r["key"] for r in graph.yt.read_table(pairs_ios, raw=False)]
        assert "11b22432-0112-4234-a353-c3e98c8baae5_222221455549991" in keys_ios

    def test_access_log_parse_did(self, graph):
        ios = DEVID_RAW_PATH + "devid_yuid_access_yp_did_ios"
        andr = DEVID_RAW_PATH + "devid_yuid_access_yp_did_android"
        pairs_ios = PAIRS_PATH + "dev_yuid_pairs_access_yp_did_ios_indev"
        pairs_andr = PAIRS_PATH + "dev_yuid_pairs_access_yp_did_android_indev"
        for path in [ios, andr, pairs_ios, pairs_andr]:
            data = [str(r) for r in graph.yt.read_table(path, raw=False)]
            allure.attach(path, str(data))
        keys_ios = [r["key"] for r in graph.yt.read_table(path, raw=False)]

    def test_yuid_apps(self, graph):
        yuid_apps = "//crypta/production/state/graph/2016-04-11/yuid_apps"
        yuid_apps_upload = "//crypta/production/state/graph/2016-04-11/yuid_apps_upload"
        assert graph.yt.exists(yuid_apps)
        assert graph.yt.row_count(yuid_apps) > 0
        assert graph.yt.exists(yuid_apps_upload)
        assert graph.yt.row_count(yuid_apps_upload) > 0

    def test_avito(self, graph):
        preproc = "//crypta/production/state/graph/v2/soup/email_avito_hash_avito_preproc"
        uuid_avito_hit = "//crypta/production/state/graph/v2/soup/uuid_avito_hash_avito_bs-hit-log"
        uuid_avito_rtb = "//crypta/production/state/graph/v2/soup/uuid_avito_hash_avito_bs-rtb-log"
        uuid_phone_hit = "//crypta/production/state/graph/v2/soup/uuid_phone_avito_bs-hit-log"
        uuid_phone_rtb = "//crypta/production/state/graph/v2/soup/uuid_phone_avito_bs-rtb-log"
        yuid_avito_hit = "//crypta/production/state/graph/v2/soup/yandexuid_avito_hash_avito_bs-hit-log"
        yuid_avito_rtb = "//crypta/production/state/graph/v2/soup/yandexuid_avito_hash_avito_bs-rtb-log"
        yuid_phone_hit = "//crypta/production/state/graph/v2/soup/yandexuid_phone_avito_bs-hit-log"
        yuid_phone_rtb = "//crypta/production/state/graph/v2/soup/yandexuid_phone_avito_bs-rtb-log"

        assert graph.yt.row_count(preproc) == 2
        preproc_emails = sorted([x["id1"] for x in graph.yt.read_table(preproc)])
        assert preproc_emails[0] == "login-for-avito@yandex.ru"
        assert preproc_emails[1] == "testings_for_work2@mail.ru"

        assert graph.yt.row_count(uuid_avito_hit) == 0

        assert graph.yt.row_count(uuid_avito_rtb) == 2
        uuid_avito_pairs = {(x["id1"], x["id2"]) for x in graph.yt.read_table(uuid_avito_rtb)}
        assert uuid_avito_pairs == {
            ("11111111111111111111111111111111", "avito_hash0"),
            ("33333333333333333333333333333333", "avito_hash2"),
        }

        assert graph.yt.row_count(uuid_phone_hit) == 0
        assert graph.yt.row_count(uuid_phone_rtb) == 1
        rec = next(graph.yt.read_table(uuid_phone_rtb))
        assert rec["id1"] == "22222222222222222222222222222222"
        assert rec["id2"] == "+79087654321"

        assert graph.yt.row_count(yuid_avito_hit) == 3
        yuid_avito_pairs = {(x["id1"], x["id2"]) for x in graph.yt.read_table(yuid_avito_hit)}
        assert yuid_avito_pairs == {
            ("5555551400000005", "avito_hash5"),
            ("5555551400000006", "72cf507224fcf30f069c528f2b5b575a"),
            ("5555551400000008", "4966f09bc693c0629b96d51aad107d3f"),
        }

        assert graph.yt.row_count(yuid_avito_rtb) == 1
        rec = next(graph.yt.read_table(yuid_avito_rtb))
        assert rec["id1"] == "5555551400000003"
        assert rec["id2"] == "avito_hash2"

        assert graph.yt.row_count(yuid_phone_hit) == 1
        rec = next(graph.yt.read_table(yuid_phone_hit))
        assert rec["id1"] == "5555551400000007"
        assert rec["id2"] == "+79087654321"

        assert graph.yt.row_count(yuid_phone_rtb) == 0
