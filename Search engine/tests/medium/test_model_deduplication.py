import os
import yatest.common
import common

DEDUPLICATE_TOOL = yatest.common.source_path(os.path.join(common.RD_PREFIX, 'deduplicate_model_storage.py'))
MODEL_PATH = yatest.common.build_path('search/web/rearrs_upper/tests/data/dedup/model_storage')
MODEL_MAKE_FILE = yatest.common.source_path('search/web/rearrs_upper/tests/data/dedup/model_storage/ya.make')


def test_dedup_all():
    updated_bundles_dir = 'updated_bundles'
    dup_list_file = 'dup_list.txt'
    cmd = [
        DEDUPLICATE_TOOL,
        'all',
        '-m', MODEL_PATH,
        '--model_make_file', MODEL_MAKE_FILE,
        '-u', updated_bundles_dir,
        '-d', dup_list_file,
        '--print_dup_info'
    ]
    canon_out = yatest.common.canonical_execute(cmd, check_exit_code=True)
    return {
        'stdout': canon_out,
        'dup_list': yatest.common.canonical_file(dup_list_file),
        'updated_bundles': yatest.common.canonical_dir(updated_bundles_dir)
    }


def test_dedup_added():
    input_model_dir = yatest.common.build_path('search/web/rearrs_upper/tests/data/dedup/added_models')
    output_model_dir = 'out_models'
    cmd = [
        DEDUPLICATE_TOOL,
        'added',
        '-m', MODEL_PATH,
        '--model_make_file', MODEL_MAKE_FILE,
        '-i', input_model_dir,
        '-o', output_model_dir,
        '--print_dup_info'
    ]
    canon_out = yatest.common.canonical_execute(cmd, check_exit_code=True)
    return {
        'stdout': canon_out,
        'out_models': yatest.common.canonical_dir(output_model_dir)
    }
