# coding: utf-8


import os
import pytest
import yatest.common
from market.sre.library.python.maaslib.memcached import Memcached
from market.sre.library.python.maaslib.memcached import get_memcacheds
from market.sre.library.python.maaslib.memcached import get_memcached_configs
from market.sre.library.python.maaslib.memcached import MemcachedConfig
from market.sre.library.python.maaslib.memcached import MemcachedStatistic
from market.sre.library.python.maaslib.memcached_configschema import MEMCACHED_SCHEMA
from market.sre.library.python.maaslib.mcrouter import Mcrouter, get_mcrouters
from market.sre.library.python.maaslib.mcrouter import McrouterConfig
from market.sre.library.python.maaslib.utils import get_config_pathes
from market.sre.library.python.maaslib.utils import get_instances
from market.sre.library.python.maaslib.utils import bytes_to_megabytes
from market.sre.library.python.maaslib.utils import get_memory_percent_usage


@pytest.fixture(scope='module')
def fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path('market/sre/library/python/maaslib/tests/files')


def test_get_memcached_configs(fixtures_dir):
    # получаем инстансы MemcachedConfigs
    configs = get_memcached_configs(fixtures_dir, 'memcached_[0-9].conf')
    assert 3 == len(configs)
    for config in configs:
        assert isinstance(config, MemcachedConfig)

    # пробуем получить некоторые параметры конфигов.
    for config in configs:
        # - порты:
        assert isinstance(config.port, int)
        # - ip-адрес, который слушаем:
        assert '::' == config.listen
        # - пользователь, от которого запускаем:
        assert 'memcache' == config.user


def test_get_memcacheds(fixtures_dir):
    # проверяем, что возращаются именно инстансы мемкеша
    memcacheds = get_memcacheds(fixtures_dir, 'memcached_[0-9].conf')
    assert 3 == len(memcacheds)
    for m in memcacheds:
        assert isinstance(m, Memcached)

    # проверяем, что инстансов возвращает ровно три, имена не дублируются
    # и есть ровно те, которые ожидаются
    memc_names = ['memcached_1', 'memcached_2', 'memcached_3']
    for name in [m.name for m in memcacheds]:
        assert name in memc_names
        memc_names.remove(name)

    assert not memc_names


def test_get_mcrouters(fixtures_dir):
    # проверяем, что возращаются именно инстансы микроутера
    mcrouters = get_mcrouters(fixtures_dir, 'mcrouter_[0-9].conf')
    assert 2 == len(mcrouters)
    for m in mcrouters:
        assert isinstance(m, Mcrouter)

    # проверяем, что инстансов возвращает ровно два, имена не дублируются
    # и есть ровно те, которые ожидаются
    mcr_names = ['mcrouter_1', 'mcrouter_2']
    for name in [m.name for m in mcrouters]:
        assert name in mcr_names
        mcr_names.remove(name)

    assert not mcr_names


def test_init_memcached(fixtures_dir):
    config = MemcachedConfig(os.path.join(fixtures_dir, 'memcached_1.conf'))
    memcached = Memcached(config)
    assert memcached.name == 'memcached_1'


def test_init_mcrouter(fixtures_dir):
    config = McrouterConfig(os.path.join(fixtures_dir, 'mcrouter_1.conf'))
    mcrouter = Mcrouter(config)
    assert mcrouter.name == 'mcrouter_1'


def test_memcached_check_config(fixtures_dir):
    config = MemcachedConfig(os.path.join(fixtures_dir, 'memcached_3.conf'))
    expected = '''-d
logfile /var/log/yandex/memcached/feed_parser.log
-m 131072
--port=21220
-u memcache
-l ::
--extended=modern,hash_algorithm=jenkins
-R 2000
'''
    assert config.raw_config() == expected


def test_memcached_get_run_params(fixtures_dir):
    config = MemcachedConfig(os.path.join(fixtures_dir, 'memcached_3.conf'))
    memcached = Memcached(config)
    assert memcached.run_params == '-d --port=21220 -m 131072 -l :: ' + \
        '--extended=modern,hash_algorithm=jenkins -u memcache -R 2000 ' + \
        'logfile /var/log/yandex/memcached/feed_parser.log'


def test_memcached_get_port(fixtures_dir):
    for path in get_config_pathes(fixtures_dir, 'memc*.conf'):
        assert MemcachedConfig(path).port == 21220


def test_mcrouter_get_config(fixtures_dir):
    mcrouter_config = McrouterConfig(os.path.join(fixtures_dir, 'mcrouter_1.conf'))
    assert mcrouter_config.raw_config() == '''{
   "pools": {
       "local": {
           "servers": [
              "cache01ht.market.yandex.net:21220",
              "cache02ht.market.yandex.net:21220"
           ]
      },
      "global": {
          "servers": [
              "cache01ht.market.yandex.net:21220",
              "cache01vt.market.yandex.net:21220",
              "cache02ht.market.yandex.net:21220",
              "cache02vt.market.yandex.net:21220"
          ]
      }
   },
    "route": {
        "type": "OperationSelectorRoute",
        "operation_policies": {
            "add": "AllAsyncRoute|Pool|global",
            "delete": "AllAsyncRoute|Pool|global",
            "get": "AllFastestRoute|Pool|local",
            "set": "AllAsyncRoute|Pool|global"
        }
    },
    "run_params": "--port=11220 --config-file={config} --log-path={log_path} --disable-miss-on-get-errors --use-asynclog-version2 --enable-flush-cmd"
}
'''


def test_mcrouter_get_run_params(fixtures_dir):
    config = McrouterConfig(os.path.join(fixtures_dir, 'mcrouter_1.conf'))
    assert config.run_params == '--port=11220 --config-file={config} --log-path={log_path} --disable-miss-on-get-errors --use-asynclog-version2 --enable-flush-cmd'


def test_mcrouter_get_port(fixtures_dir):
    config = McrouterConfig(os.path.join(fixtures_dir, 'mcrouter_2.conf'))
    mcrouter = Mcrouter(config)
    assert mcrouter.port == 11223


def test_get_config_pathes(fixtures_dir):
    expected_mcrouter = [
        'mcrouter_2.conf',
        'mcrouter_1.conf'
    ]
    expected_memcached = [
        'memcached_1.conf',
        'memcached_3.conf',
        'memcached_2.conf'
    ]
    mcrouter_pathes = [os.path.split(p)[-1] for p in get_config_pathes(fixtures_dir, 'mcro*.conf')]
    memcached_pathes = [os.path.split(p)[-1] for p in get_config_pathes(fixtures_dir, 'memc*[0-9].conf')]

    assert sorted(mcrouter_pathes) == sorted(expected_mcrouter)
    assert sorted(memcached_pathes) == sorted(expected_memcached)


def test_get_instances(fixtures_dir):
    mcrouter = [
        'tests/files/mcrouter_2.conf',
        'tests/files/mcrouter_1.conf'
    ]
    memcached = [
        'tests/files/memcached_1.conf',
        'tests/files/memcached_3.conf',
        'tests/files/memcached_2.conf'
    ]
    print(get_instances(mcrouter, Mcrouter))
    assert all([isinstance(inst, Mcrouter) for inst in get_instances(mcrouter, Mcrouter)])
    assert all([isinstance(inst, Memcached) for inst in get_instances(memcached, Memcached)])


def test_memcached_statistic(fixtures_dir):
    with open(os.path.join(fixtures_dir, 'memcached_statistic.raw')) as raw_statistic:
        stats = MemcachedStatistic(raw_statistic.read())

    assert stats.pid == "834268"
    assert stats.lru_maintainer_juggles == "10600"
    assert stats.lru_crawler_starts == "4845"
    assert stats.limit_maxbytes == "103079215104"

    d = stats.as_dict()
    assert "10600" == d["lru_maintainer_juggles"]
    assert "4845" == d["lru_crawler_starts"]
    assert "103079215104" == d["limit_maxbytes"]


def test_bytes_to_megabytes():
    assert 98304 == bytes_to_megabytes(103079215104)


def test_memcached_config_as_dict(fixtures_dir):
    config = MemcachedConfig(os.path.join(fixtures_dir, 'memcached_1.conf'))
    assert {
        '-m': 131072,
        '-u': 'memcache',
        '-o': ['modern'],
        '-p': 21220,
        '-R': 2000,
        '-d': True,
        '-l': '::',
        'logfile': '/var/log/yandex/memcached/feed_parser.log'
    } == config.as_dict()


def test_validate_memcached_config(fixtures_dir):
    config = MemcachedConfig(os.path.join(fixtures_dir, 'memcached_1.conf'))
    assert config.validate(MEMCACHED_SCHEMA)


def test_get_memory_percent_usage(fixtures_dir):
    mem_total = 266197729280  # in bytes, 256Gb
    with open(os.path.join(fixtures_dir, 'memcached_statistic_1.raw'), 'r') as raw_statistic_1:
        memstat_one = MemcachedStatistic(raw_statistic_1.read())
    with open(os.path.join(fixtures_dir, 'memcached_statistic_2.raw'), 'r') as raw_statistic_2:
        memstat_two = MemcachedStatistic(raw_statistic_2.read())

    assert 77.44560059381736 == get_memory_percent_usage(
        mem_total,
        memstat_one.limit_maxbytes,
        memstat_two.limit_maxbytes
    )
    assert 77.44560059381736 == get_memory_percent_usage(
        mem_total,
        memstat_one.as_dict()['limit_maxbytes'],
        memstat_two.as_dict()['limit_maxbytes']
    )
