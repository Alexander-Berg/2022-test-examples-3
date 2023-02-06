import os
import yatest.common as yc
import subprocess
from multiprocessing import Process
import requests
import time


port = 25650
shard_data_map = {'fakeshard': 'FakeShard', 'antirobot': 'Antirobot', 'bert': 'Bert', 'merger': 'Merger'}


def get_options(cache_size, shard_name):
    shard_data_name = shard_data_map[shard_name]
    path = yc.binary_path(f'search/begemot/data/{shard_data_name}/search/wizard/data/wizard')
    options = [
        '--cache_size',
        str(cache_size),
        '--cache-indexing',
        '--port',
        str(port),
        '--port2',
        str(port),
        "--data",
        path,
        "--grpc",
        str(port + 1),
    ]
    return options


def get(handle, payload):
    return requests.get(f'http://localhost:{port}/{handle}', params=payload).json()


def stat_query():
    return get('admin', {'action': 'stat'})


def search_query(text):
    payload = {'text': text, 'format': 'json', 'dbgwzr': '0'}
    return get('wizard', payload)


def run(stdout, shard_name, cache_size):
    binary = yc.binary_path(f'search/daemons/begemot/{shard_name}/{shard_name}')

    outdir = yc.output_path('logs')
    if not os.path.exists(outdir):
        os.mkdir(outdir)
    output = os.path.join(outdir, stdout)

    args = [binary]
    args.extend(get_options(cache_size, shard_name))
    with open(f'{output}.{shard_name}.{cache_size}', 'w') as output_file:
        subprocess.call(args, stdout=output_file)


def query_shelling(shard_name):
    stat_query()
    response1 = search_query("kitten")
    stat = stat_query()

    total1 = 0
    keys1 = 0
    for i in stat:
        if i[0].find('CacheSize') != -1:
            total1 = int(i[1])
        if i[0].find('CacheKeysSize') != -1:
            keys1 = int(i[1])

    if shard_name == 'fakeshard':
        eps = 0.1  # empirical

        assert total1 != 0
        ratio1 = keys1 / total1
        perfect_ratio1 = 1 / 3
        assert perfect_ratio1 - eps < ratio1 < perfect_ratio1 + eps

    search_query("another%20one")
    stat = stat_query()
    total1 = 0
    keys1 = 0
    for i in stat:
        if i[0].find('CacheSize') != -1:
            total1 = int(i[1])
        if i[0].find('CacheKeysSize') != -1:
            keys1 = int(i[1])

    response2 = search_query("kitten")
    assert response1 == response2

    stat2 = stat_query()
    total2 = 0
    keys2 = 0
    for i in stat2:
        if i[0].find('CacheSize') != -1:
            total2 = int(i[1])
        if i[0].find('CacheKeysSize') != -1:
            keys2 = int(i[1])
    assert total2 == total1
    assert keys2 == keys1


def test_run():
    global port
    for shard_name in ['fakeshard']:
        for cache_size in [3000]:
            run_proc = Process(
                target=run,
                args=(
                    "out",
                    shard_name,
                    cache_size,
                ),
            )
            run_proc.start()
            time.sleep(10)

            query_shelling(shard_name)
            run_proc.kill()
            run_proc.join()
            port += 1
