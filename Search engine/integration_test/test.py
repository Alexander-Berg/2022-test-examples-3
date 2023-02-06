# coding=utf-8
import yatest.common


def get_input(subtest_name):
    return yatest.common.source_path('search/wizard/entitysearch/list_machine/integration_test/' + subtest_name + '.in')


def get_output(subtest_name):
    return yatest.common.output_path(subtest_name + '.out')


def get_list_machine():
    return yatest.common.binary_path('search/wizard/entitysearch/tools/list_machine/list_machine')


def get_ner_path():
    return yatest.common.binary_path('search/wizard/entitysearch/list_machine/integration_test/ner_data')


def get_remorph_path():
    return yatest.common.source_path('search/wizard/entitysearch/list_machine/proto')


def get_fix_path():
    return 'fixlist.txt'


def get_labels_path():
    return yatest.common.source_path('search/wizard/entitysearch/list_machine/labels/labels.cfg')


def get_arguments(subtest_name):
    return [
        get_list_machine(),
        '--input',
        get_input(subtest_name),
        '--output',
        get_output(subtest_name),
        '--ner-path',
        get_ner_path(),
        '--remorph-path',
        get_remorph_path(),
        '--fix-path',
        get_fix_path(),
        '--labels-path',
        get_labels_path(),
    ]


def run_subtest(subtest_name):
    yatest.common.execute(get_arguments(subtest_name))
    return yatest.common.canonical_file(get_output(subtest_name))


def test_titles():
    return run_subtest('titles')


def test_years():
    return run_subtest('years')


def test_countries():
    return run_subtest('countries')


def test_genres():
    return run_subtest('genres')


def test_participants():
    return run_subtest('participants')


def test_about():
    return run_subtest('about')


def test_for_what():
    return run_subtest('for_what')


def test_which():
    return run_subtest('which')


def test_dummy_which():
    return run_subtest('dummy_which')


def test_age():
    return run_subtest('age')


def test_for():
    return run_subtest('for')


def test_novelty():
    return run_subtest('novelty')


def test_3d():
    return run_subtest('3d')


def test_maker():
    return run_subtest('maker')
