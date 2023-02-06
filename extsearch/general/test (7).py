from yatest import common
import json
import extsearch.video.quality.recommender.core.protos.chain_pb2 as chain_pb2
import os


def testSessions(yt_stuff):
    os.environ['MR_RUNTIME'] = 'YT'
    os.environ['YT_PREFIX'] = '//'
    config_path = 'sessions.cfg'
    data_dir = 'dataDir'
    config = json.load(open(config_path))
    server = yt_stuff.get_server()
    config["MR"]["Server"] = server
    config_path = 'sessions_new.cfg'
    with open(config_path, 'w') as config_file:
        json.dump(config, config_file)

    yt_client = yt_stuff.get_yt_client()
    clean_path = 'clean_small'
    clean = open(clean_path, 'rb').read()
    table_path = '//user_sessions/pub/search/daily/2018-04-01/clean'
    yt_client.create("table", table_path, recursive=True)
    yt_client.write_table(table_path, clean, format='json', raw=True)

    binary = common.binary_path('extsearch/video/quality/recommender/tools/sessions/sessions')
    params = ['sessions', '-J', 'search', '-c', config_path, '--dataDir', data_dir]
    out_table = '//home/videoindex/recommender/sessions2/features/user_search/yandex_search/video_loki_user_trees.20180401'
    out_table_web = '//home/videoindex/recommender/sessions2/features/user_search/yandex_search/web_loki.20180401'
    yt_client.create("table", out_table, recursive=True)
    yt_client.create("table", out_table_web, recursive=True)
    common.execute([binary] + params)

    result = []
    for rec in yt_client.read_table(out_table):
        value = chain_pb2.TUserChain()
        value.ParseFromString(rec['value'])
        value.IdUrlHash = 0  # Random number in prod tables
        rec['value'] = value
        s = str(rec)
        result.append(s)
    result.sort()
    out_path = 'out'
    s = str(result)
    with open(out_path, 'wb') as out_file:
        out_file.write(s)
    return common.canonical_file(out_path)
