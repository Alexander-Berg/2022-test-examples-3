#!/usr/bin/env python
# coding: utf-8

import os
import shutil
import time
import unittest


from pyb import core, logger
import context
from .common import symlink


ROOTDIR = os.path.abspath('root')
STUB = os.path.join(context.MARKETSEARCH_DATA_DIR, 'stub.py')
GENERATION = '20150101_1234'


def writefile(filepath, content):
    dirname = os.path.dirname(filepath)
    if not os.path.exists(dirname):
        os.makedirs(dirname)
    with open(filepath, 'w') as fobj:
        return fobj.write(content)


def create_app():
    def make_stubs():
        paths = [
            '/bin/true',  # затычка для /usr/sbin/iptruler в плагине marketsearch3
            '/usr/sbin/iptruler',
            '/etc/init.d/httpsearch',
            '/usr/lib/yandex/market-report-configs/generate.sh',
            '/usr/bin/check-search.sh',
            '/usr/bin/torrent_client_clt',
            '/usr/bin/sky_downloader',
            '/etc/init.d/advqserver-ust-yandex',
            '/etc/init.d/gunicorn',
            '/usr/sbin/service',
            '/home/monitor/agents/modules/advq_local.py',
            '/home/monitor/agents/modules/phrase_srv_monitor.py',
            '/usr/lib/yandex/backctld/index_unpacker'
        ]
        for path in paths:
            symlink(STUB, ROOTDIR + '/' + path)

    symlink(os.path.join(context.ETC_DATA_DIR, 'backctld2.cfg'), os.path.join(ROOTDIR, 'etc/backctld/backctld2.cfg'))
    symlink(os.path.join(context.ETC_DATA_DIR, 'plugins'), os.path.join(ROOTDIR, 'etc/backctld/plugins'))
    config = core.Config(prefix_dir=ROOTDIR)
    config.log_file = None
    config.debug()

    symlink('etc/plugins/', config.plugins_dir)
    make_stubs()

    logger.setup(config.log_file, use_stderr=True, args=[])
    app = core.App(config)

    ms3 = app._create_service('marketsearch3')
    writefile(ms3._config.httpsearch_list_path, '\n'.join(
        ['market-report', 'clicks-report', 'market-snippet-report', 'market-blue-report']) + '\n')
    return app


def reload_and_check(app, service):
    result = app.run([service, 'reload', GENERATION])
    if result[0] == '!':
        msg = '{} -> {}'.format(' '.join([service, 'reload', GENERATION]), result)
        raise Exception(msg)
    for i in range(10):
        result = app.run([service, 'check'])
        if result == 'ok':
            break
        elif result.startswith('! in progress'):
            time.sleep(0.1)
            continue
        else:
            raise RuntimeError(result)


def get_service_dist(app, service):
    serviceobj = app._create_service(service)
    dist = list(serviceobj.context.dists.items())[0][0]
    return dist


def download_and_check(app, service, dist):
    app.run([service, 'get_generation', dist])
    app.run([service, 'start_download', dist, 'http://', GENERATION])
    for i in range(10):
        result = app.run([service, 'is_download_finished', dist, GENERATION])
        if result == 'finished':
            break
        elif result.startswith('! in progress'):
            time.sleep(0.1)
            continue
        else:
            raise RuntimeError(result)
    result = app.run([service, 'stop_download', dist, GENERATION])
    if result.startswith('!'):
        raise RuntimeError(result)


def do_test():
    services = [
        ('advq', None),
        ('advquick', None),
        ('marketclicks', None),
        ('marketcorba', None),
        ('marketkgb', None),
        ('marketsearch3', None),
        ('marketsearchsnippet', None),
        ('marketsearchblue', ['search-part-blue', 'search-snippet-blue']),
    ]

    shutil.rmtree(ROOTDIR, ignore_errors=True)
    app = create_app()

    for service, dists in services:
        if not dists:
            dists = [get_service_dist(app, service)]
        # firewall
        app.run([service, 'close_iptruler'])
        app.run([service, 'open_iptruler'])
        # download
        for dist in dists:
            download_and_check(app, service, dist)
        # reload and check
        reload_and_check(app, service)
        # market specific
        if service.startswith('market'):
            for dist in dists:
                app.run([service, 'get_dist_generations', dist])


class TestCore(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)

    def test(self):
        do_test()


if '__main__' == __name__:
    unittest.main()
