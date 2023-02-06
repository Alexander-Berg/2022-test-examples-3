import json
import pytest
import os
import yatest.common
from mapreduce.yt.python.yt_stuff import yt_stuff
from mapreduce.yt.python.yt_stuff import YtConfig

vuserdata2_bin = yatest.common.binary_path("extsearch/video/quality/vuserdata2/vuserdata2/vuserdata2")
convert_vuserdata2_bin = yatest.common.binary_path("extsearch/video/quality/vuserdata2/tools/convert/convert")
query_to_static_bin = yatest.common.binary_path("extsearch/video/quality/vuserdata2/tools/prepare-query-to-static-factors/prepare-query-to-static-factors")
query_factors_bin = yatest.common.binary_path("extsearch/video/quality/vuserdata2/tools/prepare-query-factors/prepare-query-factors")
static_factors_bin = yatest.common.binary_path("extsearch/video/quality/vuserdata2/tools/prepare-static-factors-portion/prepare-static-factors-portion")

config_dir = yatest.common.data_path("video/vuserdata2/config")

config_file = yatest.common.source_path("yweb/webscripts/video/vuserdata2/config/test_vuserdata2.cfg")
stopwords_file = yatest.common.data_path("video/vuserdata2/config/stopwords.txt")
blockstat_file = yatest.common.data_path("video/vuserdata2/config/blockstat.dict")

build_all_input_file = yatest.common.data_path("video/vuserdata2/build_all_input.tsv")
build_all_input_web_file = yatest.common.data_path("video/vuserdata2/build_all_input_web.tsv")

MR_SERVER = ""
YT_STUFF = None
YT_ENV = None


def read_mapreduce_table(table_name, file_name):
    mapreduce_path = yatest.common.binary_path("yt/python/yt/wrapper/bin/mapreduce-yt_make/mapreduce-yt")
    yatest.common.execute([mapreduce_path, '-server', MR_SERVER, '-read', table_name, '-subkey'], stdout=open(file_name, 'w'), env=YT_ENV)


def write_mapreduce_table(table_name, file_name, subkey, isSorted=True):
    mapreduce_path = yatest.common.binary_path("yt/python/yt/wrapper/bin/mapreduce-yt_make/mapreduce-yt")
    command = [mapreduce_path, '-server', MR_SERVER, '-write', table_name]
    if subkey:
        command.append('-subkey')
    yatest.common.execute(command, stdin=open(file_name, 'r'), env=YT_ENV)
    if isSorted:
        yatest.common.execute([mapreduce_path, '-server', MR_SERVER, '-sort', table_name], env=YT_ENV)


def run_extract(start_timestamp, end_timestamp, options):
    dst_tables_prefix = 'stage_by_stage/extract_results/' + str(start_timestamp) + '-' + str(end_timestamp)
    yatest.common.execute([vuserdata2_bin, 'extract', '-s', MR_SERVER, '--src', 'user_sessions', '--dst', dst_tables_prefix,
                           '--url2author', 'url2author',
                           '--start-timestamp', str(start_timestamp), '--end-timestamp', str(end_timestamp),
                           '--config', config_file, '-b', blockstat_file] + options)


def run_merge(max_timestamp, userdatas_to_merge):
    cmd = [vuserdata2_bin, 'merge', '-s', MR_SERVER, '-c', config_file,
           '--dst', 'stage_by_stage/current_state', '--max-timestamp', str(max_timestamp)]

    cmd.extend(userdatas_to_merge)
    yatest.common.execute(cmd)


def run_calc_factors(max_timestamp):
    yatest.common.execute([vuserdata2_bin, 'calc-factors', '-s', MR_SERVER, '--src' , 'stage_by_stage/current_state', '--dst', 'stage_by_stage/factors',
                           '--max-timestamp', str(max_timestamp),
                           '--config', config_file, '--stopwords', stopwords_file])


def run_build_all(options):
    yatest.common.execute([vuserdata2_bin, 'build-all', '-s', MR_SERVER, '--dst', 'all',
                           '--url2author', 'url2author', '--stopwords', stopwords_file, '--config', config_file,
                           '-b', blockstat_file, '--canonizeUrlsPath', config_dir, '-f'] + options,
                          env=YT_ENV)


def run_convert(table_name):
    yatest.common.execute([convert_vuserdata2_bin, '-s', MR_SERVER, '-i', table_name, '-o', table_name + '_converted'], env=YT_ENV)


def run_query_to_static(queries, query_urls, dst):
    yatest.common.execute([query_to_static_bin, '-s', MR_SERVER, '-q', queries, '-u', query_urls, '-d', dst], env=YT_ENV)


def run_query_factors(queries, dst):
    yatest.common.execute([query_factors_bin, '-s', MR_SERVER, '-c', queries, '-o', dst, '-q', '{"ru":10,"ua":3,"tr":1}', '-z'], env=YT_ENV)


def run_static_factors(mode, src, dst):
    yatest.common.execute([static_factors_bin, '-s', MR_SERVER, '-i', src, '-o', dst, '-n', mode, '--suffixSubst', '.mobile:MobileReg,:Reg'], env=YT_ENV)


def convert_factors_file(file_name):
    lines = []
    with open(file_name) as f:
        for line in f.readlines():
            tokens = line.strip().split('\t')
            header = ''
            building_header = True

            for index in xrange(0, len(tokens)):
                token = tokens[index]
                if index > 0 and token.isdigit():
                    header += '\t' + token
                    building_header = False
                elif building_header:
                    if len(header) > 0:
                        header += '\t'
                    header += token
                else:
                    lines.append(header + '\t' + token)
    with open(file_name, 'w') as f:
        f.write('\n'.join(lines))


def convert_index_file(file_name):
    lines = []
    with open(file_name) as f:
        for line in f.readlines():
            tokens = line.strip().split('\t')
            header = ''
            building_header = True

            for index in xrange(0, len(tokens)):
                token = tokens[index]
                if index > 0 and len(token) == 0:
                    building_header = False
                elif building_header:
                    if len(header) > 0:
                        header += '\t'
                    header += token
                else:
                    lines.append(header + '\t' + token)
    with open(file_name, 'w') as f:
        f.write('\n'.join(lines))


def add_data_lines(lines, stats, prefix):
    if type(stats) is dict:
        for key, value in stats.iteritems():
            add_data_lines(lines, value, prefix + '\t' + key)
    else:
        lines.append(prefix + '\t' + str(stats))


def add_pos_stats_lines(lines, pos_stats, prefix):
    add_data_lines(lines, pos_stats['StatisticsData'], prefix + '\t' + json.dumps(pos_stats['SerpPositionData']))


def add_timed_stats_lines(lines, timed_stats, prefix):
    if len(prefix) > 0:
        prefix += '\t'
    prefix += 'all' if 'TimestampInterval' not in timed_stats else str(timed_stats['TimestampInterval']['Start']) + '-' + str(timed_stats['TimestampInterval']['End'])
    for pos_stats in timed_stats['PositionedStatistics']:
        add_pos_stats_lines(lines, pos_stats, prefix)


def add_stats_lines(lines, stats, prefix):
    for timed_stats in stats['TimedStatistics']:
        add_timed_stats_lines(lines, timed_stats, prefix)


def convert_stats_file(file_name):
    lines = []
    with open(file_name) as f:
        for line in f.readlines():
            tokens = line.decode('utf-8').strip().split('\t')
            header = ''
            building_header = True

            for token in tokens:
                if len(token) == 0:
                    building_header = False
                else:
                    if building_header:
                        if len(header) > 0:
                            header += '\t'
                        header += token
                    else:
                        add_stats_lines(lines, json.loads(token), header)
    with open(file_name, 'w') as f:
        f.write(u'\n'.join(lines).encode('utf-8'))


def load_input_tables():
    user_sessions_file = yatest.common.data_path("video/vuserdata2/user_sessions")
    url2author_file = yatest.common.data_path("video/vuserdata2/url2author")
    embed_file = yatest.common.data_path("video/vuserdata2/embed.filtered")
    media_file = yatest.common.data_path("video/vuserdata2/media.filtered")
    mirrors_file = yatest.common.data_path("video/vuserdata2/home.robot.mirrors.production.map")

    write_mapreduce_table("user_sessions", user_sessions_file, True)
    write_mapreduce_table("url2author", url2author_file, False)
    write_mapreduce_table("home/mirrors/production/map", mirrors_file, False)
    write_mapreduce_table("video/urlbase/prevdata/embed2url", embed_file, False)
    write_mapreduce_table("video/urlbase/prevdata/media", media_file, False)


def build_index(prefix):
    run_query_to_static(prefix + '/factors/markerdopp.2', prefix + '/factors/markerdopp_url.2', prefix + '/index/query_to_static/portion')
    run_query_factors(prefix + '/factors/markerdopp.2', prefix + '/index/query/query_factors.tsv')
    run_static_factors('url', prefix + '/factors/url.2', prefix + '/index/static/url_portion.tab')
    run_static_factors('host', prefix + '/factors/host.2', prefix + '/index/static/host_portion.htab')
    run_static_factors('author', prefix + '/factors/author.2', prefix + '/index/static/author_portion.atab')
    run_static_factors('wares', prefix + '/factors/wares_url.2', prefix + '/index/static/wares_portion.tab')


def build_all(options):
    run_build_all(options)
    print('run_build_all is done')
    build_index('all')


def get_table_canonical_file(table_name):
    file_name = yatest.common.output_path(table_name.replace('/', '.'))

    is_stats_table = (table_name.find('extract_results') != -1 or table_name.find('current_state') != -1) \
                     and table_name.find('ctr_remap') == -1 \
                     and table_name.find('min_max_average_timestamp') == -1 \
                     and table_name.find('query_norms') == -1

    if is_stats_table:
        run_convert(table_name)
        table_name += '_converted'

    yatest.common.execute(['touch', file_name])
    read_mapreduce_table(table_name, file_name)

    if table_name.find('index') != -1:
        convert_index_file(file_name)
    elif table_name.find('factors') != -1:
        convert_factors_file(file_name)
    elif is_stats_table:
        convert_stats_file(file_name)
    return yatest.common.canonical_file(file_name)


def get_canonical_files(test_name):
    url_norms = ["query", "url", "query_url", "host", "query_host", "author"]
    query_norms = ["marker", "markerdopp", "markersyn", "wares", "word", "related", "trigram"]
    tables = [
#        ["extract_results/100001600-100001700/" + name for name in url_norms],
        ["current_state/" + name for name in url_norms + ["ctr_remap", "min_max_average_timestamp"]],
        ["factors/%s.2" % name for name in
            query_norms +
            ["url", "host", "author"] +
            ["%s_%s" % (query, url)
                for query in query_norms
                for url in ["url", "host", "author"]
            ]
        ],
        ["index/query_to_static/portion", "index/query/query_factors.tsv", "index/static/url_portion.tab",
         "index/static/host_portion.htab", "index/static/author_portion.atab", "index/static/wares_portion.tab"]
    ]
    return [get_table_canonical_file(test_name + "/" + table) for table in sum(tables, [])]


def run_extract_urls():
    for day in xrange(1, 8):
        yatest.common.execute([
            vuserdata2_bin, 'extract-urls', '-s', MR_SERVER,
            '--src', 'user_sessions',
            '--dst', "marked/2015080{}".format(day),
            '-b', blockstat_file])


def init_yt(yt_stuff):
    global MR_SERVER
    global YT_STUFF
    global YT_ENV

    os.environ["MR_RUNTIME"] = "YT"
    os.environ["YT_PREFIX"] = "//"

    YT_STUFF = yt_stuff
    MR_SERVER = yt_stuff.get_server()

    YT_ENV = {
        'YT_STORAGE': 'yes',
        'YT_PROXY': MR_SERVER,
        'YT_PATH': '//',
        'YT_PREFIX': '//',
    }


def init():
    load_input_tables()


@pytest.fixture(scope="module")
def yt_config(request, tmpdir_factory):
    return YtConfig(
        scheduler_config={
            "scheduler": {
                "enable_tmpfs": False
            }
        },
        node_config={
            "resource_limits": {
                "cpu": 15
            },
            "exec_agent": {
                "job_controller": {
                    "resource_limits": {
                        "user_slots": 10,
                        "cpu": 10,
                        "memory": 27179869184,
                    }
                }
            },
        }
    )


def test_all_yt(yt_stuff):
    init_yt(yt_stuff)
    init()
    build_all(['--src-file', build_all_input_file])
    return get_canonical_files("all")

# def test_extract_urls():
#     init_yamr()
#     init()
#     run_extract_urls()
#     return [get_table_canonical_file(table) for table in ("marked/2015080{}".format(day) for day in xrange(1, 8))]
