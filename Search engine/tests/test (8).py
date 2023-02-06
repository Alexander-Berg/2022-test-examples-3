from mapreduce.yt.python.yt_stuff import yt_stuff  # noqa
import yatest.common
import json


def upload_test_file(yt_client, table_path, filename):
    yt_client.create('table', table_path, recursive=True, ignore_existing=True)
    yt_client.write_table(
        table_path,
        json.load(open(yatest.common.source_path('search/geo/tools/social_links/filter_bad_events/tests/input_data/%s' % filename))),
    )


def download_table(yt_client, table_path, filename):
    open(filename, 'w').writelines([(json.dumps(row, ensure_ascii=False) + '\n') for row in yt_client.read_table(table_path)])


def test(yt_stuff):  # noqa
    yt_client = yt_stuff.get_yt_client()

    upload_test_file(yt_client, '//bad_links_vk', 'bad_links_vk.json')
    upload_test_file(yt_client, '//bad_links_fb', 'bad_links_fb.json')
    upload_test_file(yt_client, '//actual_profiles', 'actual_profiles.json')

    yt_server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_SERVER': yt_server,
        'YT_PATH': '//',
    }
    yatest.common.execute(
        [
            yatest.common.binary_path(
                'search/geo/tools/social_links/filter_bad_events/filter_bad_events'
            ),
            '//bad_links_vk',
            '//bad_links_fb',
            '//actual_profiles',
            '//new_profiles',
            '--proxy', yt_server,
            '--out_stats', 'stats.json',
        ],
        env=yt_env,
    )
    download_table(yt_client, '//actual_profiles', 'test_actual_profiles.out')
    download_table(yt_client, '//new_profiles', 'test_new_profiles.out')
    return [yatest.common.canonical_file('test_actual_profiles.out', local=True),
            yatest.common.canonical_file('test_new_profiles.out', local=True),
            yatest.common.canonical_file('stats.json', local=True)]
