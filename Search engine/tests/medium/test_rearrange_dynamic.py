import yatest.common

import common
import fml_utils

import json
import os
import re
import shutil
import tempfile

from collections import defaultdict

from patch_fml_config.config import iterate_config, load_config
from patch_fml_config.user_interfaces import UserInterfaces


def formulas_loading_valid(path):
    all_path = []
    for item in path:
        all_path.append("--path")
        all_path.append(item)
    result = yatest.common.execute(command=[common.BINARY_PATH, "formulas"] + all_path)
    assert result.exit_code == 0
    assert result.std_err == ""
    assert int(result.std_out) > 0


def check_load_formula_storage(formulas_path, directories):
    conf_path = yatest.common.build_path(os.path.join(common.REARRS_UPPER_BUILD_PREFIX, "conf", "conf.json"))
    path = []
    for item in directories:
        path.append("--path")
        path.append(item)
    result = yatest.common.execute(command=[common.BINARY_PATH, "formulas", "--root_path", formulas_path, "--conf", conf_path] + path)
    assert result.exit_code == 0
    assert result.std_err == ""
    assert int(result.std_out) > 0


def test_formulas_storage_loading():
    formulas_path = yatest.common.build_path(common.RD_BUILD_PREFIX)
    directories = common.get_all_directories_with_formulas(common.RD_BUILD_PREFIX)
    check_load_formula_storage(formulas_path, directories)


def test_bundles_are_extracted():
    extract_fml = yatest.common.source_path(os.path.join(common.RD_PREFIX, 'extract_fml.py'))
    bundles = [f for f in common.get_all_files_recursive(common.RD_BUILD_PREFIX) if f.endswith(fml_utils.BUNDLE_EXTENSION)]
    for bundle in bundles:
        result = yatest.common.execute(command=[extract_fml, "--dry-run", "--overwrite", "--out_log", bundle])
        assert result.exit_code == 0
        assert result.std_out.strip() == ""


def check_formulas_storage_loading_with_archive(to_archive):
    formulas_path = yatest.common.build_path(common.RD_BUILD_PREFIX)
    archiver = yatest.common.binary_path(os.path.join("tools", "archiver", "archiver"))
    work_dir = tempfile.mkdtemp()
    try:
        new_rd_dir = os.path.join(work_dir, 'rearrange.dynamic')
        shutil.copytree(formulas_path, new_rd_dir, ignore=lambda d, fns: set([os.path.basename(resfn) for resfn in to_archive if os.path.dirname(resfn) == d]))
        while True:
            empty_folders = [fn for fn in common.get_all_files_recursive(new_rd_dir, add_dirs=True) if os.path.isdir(fn) and not os.listdir(fn)]
            if not empty_folders:
                break
            for fn in empty_folders:
                os.rmdir(fn)
        exit_code = yatest.common.execute(command=[archiver, "--deduplicate", "--plain", "--output", os.path.join(new_rd_dir, 'models.archive')] + list(to_archive)).exit_code
        assert exit_code == 0
        check_load_formula_storage(new_rd_dir, common.get_all_directories_with_formulas(new_rd_dir))
    finally:
        shutil.rmtree(work_dir)


def test_formulas_storage_loading_with_archive():
    formulas_path = yatest.common.build_path(common.RD_BUILD_PREFIX)
    to_archive = set([fn for fn in common.get_all_files_recursive(os.path.join(formulas_path, 'blender')) if fn.endswith('.info') or fn.endswith('.xtd')])
    check_formulas_storage_loading_with_archive(to_archive)


def test_formulas_storage_loading_with_archive_blender_all():
    formulas_path = yatest.common.build_path(common.RD_BUILD_PREFIX)
    to_archive = set([fn for fn in common.get_all_files_recursive(os.path.join(formulas_path, 'blender')) if fn.endswith('.info') or fn.endswith('.xtd') or fn.endswith('.mnmc')])
    check_formulas_storage_loading_with_archive(to_archive)


def test_formulas_storage_loading_with_archive_all():
    formulas_path = yatest.common.build_path(common.RD_BUILD_PREFIX)
    to_archive = set([fn for fn in common.get_all_files_recursive(formulas_path) if fn.endswith('.info') or fn.endswith('.xtd') or fn.endswith('.mnmc')])
    to_archive = set([fn for fn in to_archive if 'personalization' not in fn])
    check_formulas_storage_loading_with_archive(to_archive)


def test_formulas_loading_single():
    # Trying to load each folder from formulas file regardless from another one
    file_path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, "formulas"))
    assert os.path.getsize(file_path) > 0
    directories = 0
    with open(file_path, "r") as f:
        for line in f.readlines():
            line = line.strip()
            if line.startswith("#"):
                continue
            directories += 1
            path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, line))
            assert os.path.isdir(path)
            formulas_loading_valid([path])
    # Check that at least one directory presents into file
    assert directories > 0


def test_formulas_loading_all():
    # Trying to load all blender formulas
    directories = common.get_all_directories_with_formulas(common.RD_BUILD_PREFIX)
    formulas_loading_valid(directories)


def test_facts_normalize_queries():
    special_words = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, common.RD_BUILD_PREFIX, "facts", "special_words.gzt.bin"))
    all_patterns = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, common.RD_BUILD_PREFIX, "facts", "all_patterns.txt"))
    queries = yatest.common.work_path("queries_for_normalization_tests.tsv")
    normalized_queries = yatest.common.work_path("normalized_queries.tsv")

    normalize_queries_path = yatest.common.binary_path(
        "quality/functionality/entity_search/factqueries/tools/normalize_queries/normalize_queries"
    )

    yatest.common.execute(
        [
            normalize_queries_path,
            '--gzt', special_words,
            '--regexp', all_patterns,
            '--input', queries,
            '--output', normalized_queries
        ],
        check_exit_code=True
    )

    return yatest.common.canonical_file(normalized_queries)


def test_turbo_loading_all():
    beauty_urls_blacklist = yatest.common.build_path(os.path.join(common.TEST_DATA_PREFIX, common.RD_BUILD_PREFIX, "turbo", "beauty_url_blacklist.json"))
    json.load(open(beauty_urls_blacklist))


def test_blender_halfyear_backexp_coverage():
    # both backexp_reg_exp and exceptions lists should be manually updated when creating new backexp
    backexp_reg_expr = re.compile('^.*exp.blender_back_halfyear == 2.*$')
    exp_reg_expr = re.compile(r'^.*exp\..*$')
    ignored_expressions = [
        re.compile(r'^.*WIZARD\.RearrMarker\.bno.*$')
    ]
    verticals_exceptions = [
        'recommender_iznanka'
    ]
    full_node_exceptions = [
        ('translate', 'desktop', 'ee'),
        ('translate', 'desktop', 'fi'),
        ('translate', 'desktop', 'lt'),
        ('translate', 'desktop', 'lv'),
        ('translate', 'desktop', 'pl'),
    ]

    prod_and_backexp_occurrence = defaultdict(int)
    fml_config_path = yatest.common.build_path(os.path.join(common.RD_BUILD_PREFIX, 'blender', 'fml_config.json'))
    with open(fml_config_path, 'r') as f:
        fml_cfg = load_config(f.read())

    for config_info in iterate_config(fml_cfg):
        is_backexp_node = backexp_reg_expr.match(config_info.expression)
        is_prod_node = not exp_reg_expr.match(config_info.expression)
        should_ignore_expression = any([e.match(config_info.expression) for e in ignored_expressions])
        if not (is_backexp_node or is_prod_node) or should_ignore_expression:
            continue
        uis = UserInterfaces.get_uis_from_whole_expression(config_info.expression)
        for ui in uis:
            if is_prod_node:
                prod_and_backexp_occurrence[(config_info.vertical, ui, config_info.tld)] += 1
            elif is_backexp_node:
                prod_and_backexp_occurrence[(config_info.vertical, ui, config_info.tld)] -= 1

    for node_description, occurrence_value in prod_and_backexp_occurrence.items():
        vertical, ui, tld = node_description
        should_be_skipped = (vertical in verticals_exceptions) or (node_description in full_node_exceptions)
        assert should_be_skipped or occurrence_value <= 0, 'No blender backexp node for %s %s %s' % (vertical, ui, tld)
        if should_be_skipped:
            assert occurrence_value > 0, 'There is blender backexp node for %s %s %s, consider remove it from test exceptions' % (vertical, ui, tld)
