# -*- coding: utf-8 -*-

import httplib
import json
import shutil
import tempfile
import traceback

from remote import *
from remote_command import *
from utils import *


DMOCK_CONFIG_NAME = 'dmock_config'
DMOCK_PORT = 28832

EXTERNAL_SERVICE_LIST = (
    ('formalizer_host_port', 'cs-formalizer-rep.vs.market.yandex.net:34512'),
    ('request_categories_classificator_host_port', 'cs-mxclassifier-report.vs.market.yandex.net:80'),
    ('ichwill_host_port', 'ichwill.vs.market.yandex.net:81'),
    ('recommender_host_port', 'recommender.vs.market.yandex.net'),
    ('reqwizard_host_port', 'reqwizard.yandex.net:8891'),
    ('reqwizard_host_port_main_report', 'reqwizard.yandex.net:8891'),
    ('delivery_calc_host_port', 'delicalc.report.mbi.vs.market.yandex.net:30012'),
    ('reqwizard_cached_host_port', 'reqwizard-cached.vs.market.yandex.net:8891'),
    ('crypta_profile_host_port', 'bigb-fast.yandex.ru'),
    ('crypta_models_host_port', 'classerv.rtcrypta.yandex.net:8080'),
    ('bigb_host_port', 'bigb-fast.yandex.ru'),
    ('bk_host_port', 'yabs.yandex.ru:80'),
    ('adv_machine_host_port', 'web.apphost.yandex.ru'),
    ('omm_host_port', 'online-merch-machine.yandex.net'),
    ('turbo_saas_host_port', 'saas-searchproxy-kv.yandex.net:17000'),
    ('blue_omm_host_port', 'online-beru-machine.yandex.net'),
    ('market_snippet_prod_host_port', 'saas-searchproxy-kv.yandex.net:17000'),
    ('market_snippet_test_host_port', 'saas-searchproxy-prestable.yandex.net:17000'),
    ('toloka_omm_host_port', 'toloka-omm.vs.market.yandex.net'),
)


def create_external_services_confg(cluster_index):
    config = '[external_services_data]\n'
    service_id = 0
    for service_name, _ in EXTERNAL_SERVICE_LIST:
        config += '{service_name} = {host}.market.yandex.net:{port}/cache/{service_id}\n'.format(
            service_name=service_name,
            host=get_hp_snippet_host_name(cluster_index),
            port=DMOCK_PORT,
            service_id=service_id
        )
        service_id += 1
    return config


def create_dmock_config(cluster_index):
    CONFIG = '''Core {{
        Log {{
            Level: DEBUG
            Target {{
                FilePath: "/var/log/search/dmock.log"
            }}
        }}
        Server {{
            ListenThreads: 20
            RequestQueueSize: 200
            Port: {dmock_port}
        }}
    }}
'''.format(dmock_port=DMOCK_PORT)
    with TempDir() as temp_dir:
        dmock_conf_path = os.path.join(temp_dir.name, 'dmock.conf')
        with open(dmock_conf_path, 'w') as f:
            f.write(CONFIG)
        upload_files_to_hosts(get_hp_snippet_host_list(cluster_index), dmock_conf_path, get_remote_temp_dir())


def start_dmock(cluster_index):
    execute_command_on_hosts(
        get_hp_snippet_host_list(cluster_index),
        'start-stop-daemon --start --quiet --background --pidfile {0}/dmock.pid --make-pidfile --chuid httpsearch --startas {0}/dmock -- -c {0}/dmock.conf'.format(get_remote_temp_dir()),
        run_as_root=True
    )


def wait_for_dmock_start(cluster_index):
    print '>>> Waiting for Dmock to start'
    while True:
        for ret_code, out in execute_command_on_hosts(
            get_hp_snippet_host_list(cluster_index),
            'curl -sS ::1:{dmock_port}/ping'.format(dmock_port=DMOCK_PORT),
            get_results=True
        ):
            if ret_code == 0 and out:
                ping_parts = out[0].split(';')
                if len(ping_parts) >= 2 and ping_parts[1] == 'OK':
                    return


def load_dmock_config(cluster_index):
    temp_dir = tempfile.mkdtemp()
    try:
        copy_files_from_host(
            get_tank_host(cluster_index),
            os.path.join(get_remote_temp_dir(), DMOCK_CONFIG_NAME),
            temp_dir,
            append_host_to_dst=False
        )
        dmock_config_path = os.path.join(temp_dir, DMOCK_CONFIG_NAME)
        if os.path.isfile(dmock_config_path):
            with open(dmock_config_path, 'r') as f:
                dmock_config = f.read()
        return json.loads(dmock_config)
    finally:
        shutil.rmtree(temp_dir)


def make_dmock_config(cluster_index):

    def send_http_query(host, port, query):
        conn = httplib.HTTPConnection(host=host, port=port, strict=True, timeout=30)
        conn.request('GET', query)
        try:
            return conn.getresponse().read()
        finally:
            conn.close()

    temp_dir = tempfile.mkdtemp()
    try:
        try:
            for service_id, service_cache_id in load_dmock_config(cluster_index).iteritems():
                resp = send_http_query('dmock.vs.market.yandex.net', 80, '/unregister?sid={}'.format(service_cache_id))
                print 'Service {} with id {} unregistered with dmock: {}'.format(service_id, service_cache_id, resp)
        except Exception:
            traceback.print_exc()

        dmock_config = dict()
        for service_id, host_and_port in EXTERNAL_SERVICE_LIST:
            response = send_http_query('dmock.vs.market.yandex.net', 80, '/register?uri={}'.format(host_and_port))
            service_cache_id = int(json.loads(response)['id'])
            dmock_config[service_id] = service_cache_id
            print 'Service {} registered with dmock as {}'.format(service_id, service_cache_id)

        dmock_config_path = os.path.join(temp_dir, DMOCK_CONFIG_NAME)
        with open(dmock_config_path, 'w') as f:
            f.write(json.dumps(dmock_config, indent=4))
        copy_files_to_host(get_tank_host(cluster_index), dmock_config_path, get_remote_temp_dir())
    finally:
        shutil.rmtree(temp_dir)
