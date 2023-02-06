import datetime
import os

import common
import yatest.common


def _porno_llsts_valid(path):
    for line in open(path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 3
        assert parts[0] in ("owner", "host")


def test_blender_ethos_factors_json():
    common.scheme_valid([common.REARRANGE_BUILD_PREFIX, "blender", "ethos.factors.json"])
    common.json_valid([common.REARRANGE_BUILD_PREFIX, "blender", "ethos.factors.json"])


def test_blender_surf_factors_json():
    common.scheme_valid([common.REARRANGE_BUILD_PREFIX, "blender", "surf.factors.json"])
    common.json_valid([common.REARRANGE_BUILD_PREFIX, "blender", "surf.factors.json"])


def test_blender_translate_factors_json():
    common.scheme_valid([common.REARRANGE_BUILD_PREFIX, "blender", "translate.factors.json"])
    common.json_valid([common.REARRANGE_BUILD_PREFIX, "blender", "translate.factors.json"])


def test_blender_url_match_factors_json():
    common.scheme_valid([common.REARRANGE_BUILD_PREFIX, "blender", "url_match_factors.json"])
    common.json_valid([common.REARRANGE_BUILD_PREFIX, "blender", "url_match_factors.json"])


def test_blender_holidays_txt():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "blender", "holidays.txt"))
    for line in open(file_path, "r"):
        if line.startswith("#"):
            continue
        items = line.strip("\n").split("\t")
        assert len(items) == 2
        for item in items:
            assert len(item) > 0
        assert (items[1][0] == "+" or items[1][0] == "-")
        common.check_some_type_items([items[0]], int)
        try:
            datetime.datetime.strptime(items[1][1:], "%Y%m%d")
        except:
            assert False


def test_host_classifier_marks_marked_files():
    files = ["marked_afisha_event", "marked_auto", "marked_review", "marked_vacancy"]
    for filename in files:
        file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "host_classifier", "marks", filename))
        for line in open(file_path, "r"):
            parts = line.split("\t")
            assert len(parts) == 3
            common.check_some_type_items([parts[1]], int)
            common.check_some_type_items([parts[2]], list)


def test_video_ext_hostings_info_json():
    common.scheme_valid([common.REARRANGE_BUILD_PREFIX, "video", "ext_hostings_info.json"])
    common.json_valid([common.REARRANGE_BUILD_PREFIX, "video", "ext_hostings_info.json"])


def test_entity_search_config():
    common.proto_text_format_valid([common.REARRANGE_BUILD_PREFIX, "entity_search", "config.pb.txt"], "NEntitySearchRearrangeRule.TConfig")


def test_personalization_unique_formula_names():
    path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "personalization", "formulas"))
    result = yatest.common.execute(command=[common.BINARY_PATH, "formulas", "--path", path])
    assert result.exit_code == 0
    assert result.std_err == ""
    assert int(result.std_out) > 0


def test_porno_bad_list_txt():
    _porno_llsts_valid(yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "porno", "bad_list.txt")))


def test_porno_good_list_txt():
    _porno_llsts_valid(yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "porno", "good_list.txt")))


def test_press_declined_txt():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "press", "declined.txt"))
    for line in open(file_path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 3
        items = parts[1].split()
        assert len(items) == 2
        common.check_some_type_items(items, int)
        assert parts[2] == "DECLINED"


def test_press_hosts_txt():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "press", "hosts.txt"))
    for line in open(file_path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 2
        assert len(parts[0]) > 0
        items = parts[1].split()
        assert len(items) == 2
        common.check_some_type_items(items, int)


def test_press_partners2_txt():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "press", "partners2.txt"))
    for line in open(file_path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 2
        assert len(parts[0]) > 0
        common.check_some_type_items([parts[1]], float)


def test_press_top10000():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "press", "top10000"))
    for line in open(file_path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 3
        assert len(parts[0]) > 0
        assert len(parts[1]) > 0
        common.check_some_type_items([parts[2]], int)


def test_vendor_vendors_txt():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "vendor", "vendors.txt"))
    for line in open(file_path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 2


def test_recipe_hosts():
    file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "recipe_classifier", "tsv_source", "host_to_recipe_score.tsv"))
    for line in open(file_path, "r"):
        parts = line.strip("\n").split("\t")
        assert len(parts) == 2
        common.check_some_type_items([parts[1]], float)
