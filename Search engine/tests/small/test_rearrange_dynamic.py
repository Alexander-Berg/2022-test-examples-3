import json
import os
import pytest
import re
import shutil
import tempfile

import common
import fml_utils
import yatest.common

import test_uni_answer_data


def validate_scheme_and_json(rearrange, config):
    common.scheme_valid([common.RD_BUILD_PREFIX, rearrange, config])
    common.json_valid([common.RD_BUILD_PREFIX, rearrange, config])


def test_blender_fml_config_json():
    validate_scheme_and_json("blender", "fml_config.json")


def test_img_fml_config_json():
    validate_scheme_and_json("blender", "img_fml_config.json")


def test_formulas_storage_handling():
    result = yatest.common.execute(command=[common.BINARY_PATH, "catch_storage_errors", "--path", common.FMLS_GRAPHS_PATH])
    assert result.exit_code == 0
    assert result.std_err == ""


def test_video_blender_fml_config_json():
    validate_scheme_and_json("blender", "video_fml_config.json")


def test_blender_vertical_factors_json():
    validate_scheme_and_json("blender", "vertical_factors.json")


def test_facts_fml_config_json():
    validate_scheme_and_json("facts", "fml_config.json")


def test_video_wiztexts_json():
    common.scheme_valid([common.RD_BUILD_PREFIX, "video", "wiztexts.json"])


def test_video_filmrecommend_json():
    common.scheme_valid([common.RD_BUILD_PREFIX, "video", "filmrecommend.json"])


def test_video_voicesearch_json():
    common.scheme_valid([common.RD_BUILD_PREFIX, "video", "voicesearch.json"])


def test_antidup_banned_tsv():
    file_path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, "antidup", "banned_info_lists.proto_archive"))
    assert os.path.getsize(file_path) > 0


def validate_antidup_banned_list_no_errors(file_path):
    compiler = yatest.common.binary_path(os.path.join("kernel", "dups", "tools", "banned_list_tsv_to_proto", "banned_list_tsv_to_proto"))
    work_dir = tempfile.mkdtemp()
    try:
        result = yatest.common.execute(command=[compiler, "--track-repetitions", "--Werror", "--output", os.path.join(work_dir, "list.proto"), file_path])
        assert result.std_err == ""
        assert result.exit_code == 0
    finally:
        shutil.rmtree(work_dir)


@pytest.mark.parametrize("list_filename", [
    "banned.tsv",
    "banned_hosts.tsv",
    "unglue.tsv",
    "originals.tsv",
])
def test_antidup_manual_list(list_filename):
    file_path = yatest.common.source_path(os.path.join(common.RD_PREFIX, "antidup", "text_source", list_filename))
    return validate_antidup_banned_list_no_errors(file_path)


def test_context_verticals_verticals_config_json():
    validate_scheme_and_json("context_verticals", "verticals_config.json")


def test_recommender_rvqtexts_json():
    validate_scheme_and_json("recommender", "rvqtexts.json")


def test_entity_search_config():
    if os.path.exists(os.path.join(common.RD_BUILD_PREFIX, "entity_search", "config.pb.txt")):
        common.proto_text_format_valid([common.RD_BUILD_PREFIX, "entity_search", "config.pb.txt"], "NEntitySearchRearrangeRule.TConfig")


def test_ugc_scenario_template_json():
    common.scheme_valid([common.RD_BUILD_PREFIX, "ugc", "script_template.json"])


def test_ugc_config():
    common.proto_text_format_valid([common.RD_BUILD_PREFIX, "ugc", "config.pb.txt"], "NUgcRearrangeRule.TConfig")


def test_video_extra_items_configs():
    common.scheme_valid([common.RD_BUILD_PREFIX, "video_extra_items", "blacklist_entitysearch.json"])
    common.scheme_valid([common.RD_BUILD_PREFIX, "video_extra_items", "log_entitysearch.json"])


def test_videoserial_trie():
    printtrie_path = yatest.common.binary_path(os.path.join("tools", "printtrie", "printtrie"))
    trie_path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, "videoserial", "sertitle.trie"))
    result = yatest.common.execute(command=[printtrie_path, "-t", "TUtf16String", trie_path])
    assert result.exit_code == 0
    assert result.std_err == ""


def test_uni_answer_framework_json():
    common.scheme_valid([common.RD_BUILD_PREFIX, "uni_answer", "framework.json"])


def test_uni_answer_complete():
    file_path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, "uni_answer", "framework.json"))
    test_uni_answer_data.test_framework(file_path)


def test_bundles_json():
    bundles = [f for f in common.get_all_files_recursive(common.RD_BUILD_PREFIX) if f.endswith(fml_utils.BUNDLE_EXTENSION)]
    for bundle in bundles:
        common.json_valid([bundle])


def test_local_scheme_txt():
    with open(os.path.join(common.RD_BUILD_PREFIX, "blender", "local_scheme.txt"), "r") as f:
        for line in f:
            if line.startswith("#"):
                continue
            scheme = line.split("#", 1)[0]
            path, value = scheme.split("=", 1)
            common.scheme_path_valid(path)
            common.scheme_data_valid(value)


def test_mnmc_fml_config():
    work_dir = tempfile.mkdtemp()
    try:
        shutil.copy(os.path.join(common.DATA_PATH, "alexsh_stability_news_desktop_ru.xtd_upper.mnmc"), work_dir)
        shutil.copy(os.path.join(common.DATA_PATH, "mnmc_fml_config.json"), work_dir)
        result = yatest.common.execute(command=[common.BINARY_PATH, "formulas", "--path", work_dir, "--fml_config", os.path.join(work_dir, "mnmc_fml_config.json")])
        assert result.exit_code == 0
        assert result.std_err == ""
    finally:
        shutil.rmtree(work_dir)


def test_facts_forbidden_sources_json():
    validate_scheme_and_json("facts", "forbidden_sources.json")


def test_facts_suggest_filters():
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "regexp_filters", "porno.nokb.pref"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "regexp_filters", "porno.pref"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "regexp_filters", "porno.pref.url"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "regexp_filters", "stop.nokb.pref"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "regexp_filters", "stop.pref"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "regexp_filters", "tr.porno.pref"])

    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "words_filters", "porno.lst"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "words_filters", "porno.nokb.lst"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "words_filters", "tr.porno.dic"])

    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "bad_combinations", "exceptions.gzt.bin"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "bad_combinations", "interactions"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "bad_combinations", "markers.gzt.bin"])
    common.file_nonempty([common.RD_BUILD_PREFIX, "facts", "suggest_filters", "bad_combinations", "objects.gzt.bin"])


def test_tv_translations_channels_data_json():
    path = [common.RD_BUILD_PREFIX, "tv_broadcasting", "channels_data.json"]
    common.scheme_valid(path)
    common.json_valid(path)
    file_path = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, *path))
    with open(file_path, "r") as f:
        data = json.loads(f.read(), encoding="utf-8")
        common.check_some_type_items(data.keys(), lambda x: x if x == "all" else int(x))


def test_tv_translations_ordered_channels_json():
    ordered_channels = [common.RD_BUILD_PREFIX, "tv_broadcasting", "ordered_channels.json"]
    ordered_channels_path = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, *ordered_channels))

    common.scheme_valid(ordered_channels)
    common.json_valid(ordered_channels)

    with open(ordered_channels_path, "r") as f_ordered_channels:
        ordered_channels_json = json.loads(f_ordered_channels.read(), encoding="utf-8")
        assert "channels" in ordered_channels_json.keys(), "%s does not contain key 'channels'" % ordered_channels_json
        assert "dssm_channels" in ordered_channels_json.keys(), "%s does not contain key 'dssm_channels'" % ordered_channels_json
        assert len(ordered_channels_json) == 2, "extra keys found in %s, expected [channels, dssm_channels]" % ordered_channels_json.keys()
        common.check_some_type_items(ordered_channels_json["channels"], int)
        common.check_some_type_items(ordered_channels_json["dssm_channels"], int)


def test_tv_translations():
    channels_data = [common.RD_BUILD_PREFIX, "tv_broadcasting", "channels_data.json"]
    channels_data_path = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, *channels_data))
    ordered_channels = [common.RD_BUILD_PREFIX, "tv_broadcasting", "ordered_channels.json"]
    ordered_channels_path = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, *ordered_channels))

    with open(channels_data_path, "r") as f_channels_data, open(ordered_channels_path, "r") as f_ordered_channels:
        channels_data_json = json.loads(f_channels_data.read(), encoding="utf-8")
        ordered_channels_json = json.loads(f_ordered_channels.read(), encoding="utf-8")

        for channel_id in ordered_channels_json["channels"]:
            assert channel_id in channels_data_json, "Key %s not in %s" % (channel_id, channels_data_json)
        for channel_id in channels_data_json.keys():
            if channel_id == "all":
                continue
            if "is_fake" in channels_data_json[channel_id]:
                continue
            assert channel_id in ordered_channels_json["channels"], "Key %s not in %s" % (channel_id, ordered_channels_json)


def __test_video_delayed_view_trie(trie_file_basename, trie_ops_mod):
    trie_ops_path = yatest.common.binary_path(os.path.join(
        'extsearch', 'video', 'quality', 'delayed_view', 'trie_ops', 'trie_ops'))
    trie_path = yatest.common.build_path(os.path.join(
        common.RD_BUILD_PREFIX, 'video_delayed_view', trie_file_basename))
    result = yatest.common.execute(command=[trie_ops_path, trie_ops_mod, '--trie_path', trie_path])
    assert result.exit_code == 0
    assert result.std_err == ''


def test_video_delayed_view_entity_base_trie():
    __test_video_delayed_view_trie('delayed_view_entity_base_trie', 'print_entity')


def test_video_delayed_view_serial_base_trie():
    __test_video_delayed_view_trie('delayed_view_serial_base_trie', 'print_serial')


def test_init_grouping_url_matcher():
    path = os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, 'dumper')
    result = yatest.common.execute(command=[common.BINARY_PATH, 'init_grouping_url_matcher', '--path', path])
    assert result.exit_code == 0
    assert result.std_err == ''


def test_blender_vowpal_wabbit():
    path = [common.RD_BUILD_PREFIX, 'blender', 'vowpal_wabbit', 'vowpal_wabbit.features.json']
    common.scheme_valid(path)
    result = yatest.common.execute(command=[
        common.BINARY_PATH,
        'vowpal_wabbit',
        '--path', os.path.join(*(path[:-1] + [''])),
        '--config-name', path[-1]
    ])
    assert result.exit_code == 0
    assert result.std_err == ''


def test_dumper_extraction():
    path = os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, 'dumper')
    result = yatest.common.execute(command=[common.BINARY_PATH, 'dumper_extraction', '--path', path])
    assert result.exit_code == 0


def test_video_serial_texts():
    path = [common.RD_BUILD_PREFIX, "videoserial", "serial_texts.json"]
    file_path = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, *path))

    common.json_valid(path)
    with open(file_path, "r") as f:
        data = json.loads(f.read(), encoding="utf-8")
        main_lang_codes = ["0", "1", "2", "5", "44"]
        main_text_types = ["season", "episode"]
        for lang_code in main_lang_codes:
            assert lang_code in data
            assert all(text_type in data[lang_code] for text_type in main_text_types)


def test_formula_names():
    exceptions = [
        "adresearch-54_v4_best",
        "adresearch-54_v4_best_sub1",
        "adresearch-54_v4_best_sub2",
        "adresearch-54_v4_best_sub3",
        "adresearch-54_v4_best_sub4",
        "adresearch-54_v4_cb2_sub2",
        "adresearch-54_v4_cb2_sub3",
        "adresearch-54_v4_cb2_sub4",
        "freshness-detector_conv_desktop_20180730",
        "freshness-detector_conv_desktop_20180730_sub1",
        "freshness-detector_conv_touch_20180615",
        "freshness-detector_conv_touch_20180615_sub1",
        "img_formula_gallery_2-3_right",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3_sub1",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3_sub2",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3_sub3",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3_sub4",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3_sub5",
        "market_touch_v6_mmc_0.1_mdc_2_sst_11e-3_sub6",
        "nirvana_periodic_threshold_-0.1.20180821_20180828.bna_xussr_deskpad",
        "nirvana_periodic_threshold_-0.1.20180821_20180828.bna_xussr_deskpad_sub1",
        "rkub-incut.june2015.mx",
        "tr-incut.june2015.mx",
        "tv-detector-surplus-94185",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub2",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub3",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub4",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub5",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub6",
        "video_xussr_baseline_touch.2020-02-18_2020-03-16_cb_1.1_SbSc_0.01_rb_0.05",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub2",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub3",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub4",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub5",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.0_SbSc_0.01_rb_0.05_sub6",
        "video_xussr_midas_touch.2020-02-18_2020-03-16_cb_1.1_SbSc_0.01_rb_0.05",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub1",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub2",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub3",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub4",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub5",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub6",
        "ydo_desktop_multifeature_winloss_mc_sst_1e-05_cb_1.2_uid_sub7",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub1",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub2",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub3",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub4",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub5",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub6",
        "ydo_touch_multifeature_winloss_mc_sst_1e-05_cb_1.2_v8_c_dec19_sub7",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub1",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub2",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub3",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub4",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub5",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub6",
        "ydo_touch_multifeature_winloss_mc_sst_1e-06_cb_1.0_uid_sub7"
    ]
    file_path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, "blender", "ya.make"))
    fname_reg_exp = re.compile(r'FROM_SANDBOX\((\d+)\s+OUT (.*)(\.xtd|\.info|\.mnmc|\.regtree)\)')
    correct_reg_exp = re.compile(r'^[a-zA-Z0-9._]+$')
    with open(file_path, "r") as f:
        for line in f:
            match = fname_reg_exp.match(line)
            if not match:
                continue
            fname = match.group(2)
            if fname in exceptions:
                continue
            assert correct_reg_exp.match(fname), "Invalid formula name: " + fname
