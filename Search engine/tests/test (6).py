from mapreduce.yt.python.yt_stuff import yt_stuff  # noqa
import yatest.common
import json
import library.python.resource as resource

import yt.wrapper as yt
yt.format.JSON_ENCODING_LEGACY_MODE = True


def test(yt_stuff):  # noqa
    yt_server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_SERVER': yt_server,
        'YT_PATH': '//',
    }
    yt_client = yt_stuff.get_yt_client()

    table_path = '//input_table'
    yt_client.create('table', table_path, recursive=True, ignore_existing=True)
    fin = resource.find('/test.data')
    fin = fin.rstrip()
    yt_client.write_table(
        table_path,
        [json.loads(line) for line in fin.decode("utf-8").split('\n')],
    )

    yatest.common.execute(
        [
            yatest.common.binary_path(
                'search/geo/tools/social_links/extract_facts/extract_facts'
            ),
            table_path,
            '--max_failed_job_count=1',
            '--proxy', yt_server,
            '--max_processed_age=0',
            '--out_stats', 'stats.json',
        ],
        env=yt_env,
    )
    output_path = yatest.common.output_path('test.out')
    with open(output_path, 'w') as fout:
        for row in yt_client.read_table(table_path, format='<encode_utf8=%false>json'):
            fout.write(json.dumps(row, ensure_ascii=False).encode("utf-8") + '\n')
    return [yatest.common.canonical_file(output_path),
            yatest.common.canonical_file('stats.json', local=True)]
