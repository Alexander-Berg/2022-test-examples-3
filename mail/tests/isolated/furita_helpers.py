import yatest.common
from yatest.common import network
import requests
import time
import os
import json
import logging

from mail.devpack.lib.config_master import generate_config
from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.lib.components.base import FakeRootComponent
from mail.devpack.lib.components.sharpei import Sharpei
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.notsolitesrv.devpack.components.relay import RelayComponent
from mail.pg.furitadb.devpack.components.furitadb import FuritaDb

logger = logging.getLogger(__name__)


class FuritaService(FakeRootComponent):
    NAME = 'furita_all'
    DEPS = [Sharpei, FuritaDb, RelayComponent]


class Env(object):
    def __init__(self, pm, top_comp_cls=None, devpack_root=None):
        self.pm = pm
        self.config = None
        self.top_comp_cls = top_comp_cls
        self.devpack_root = devpack_root or "devpack"

    def gen_mdb_config(self, gp, shards_count, replica_per_shard=0, port_offset=6300):
        config = {}
        shards = []
        for shard_id in range(shards_count):
            shard = {'name': 'shard{}'.format(shard_id + 1),
                     'id': shard_id + 1,
                     'reg_weight': 1,
                     'dbs': [{'port': gp(port_offset), 'type': 'master'}]}
            port_offset += 1
            for replica_id in range(replica_per_shard):
                shard['dbs'].append({'port': gp(port_offset), 'type': 'replica'})
                port_offset += 1
            shards.append(shard)
        config['shards'] = shards
        return config

    def get_config(self):
        if not self.config:
            gp = lambda port: self.pm.get_port(port)
            self.config = generate_config(gp, self.devpack_root, top_comp_cls=self.top_comp_cls)
            self.config['mdb'] = self.gen_mdb_config(gp, 2)
        return self.config

    def get_arcadia_bin(self, path):
        return yatest.common.binary_path(path)

    def log_stdout(self):
        return False

    def get_java_path(self):
        return yatest.common.runtime.java_path()


def devpack_start():
    pm = network.PortManager()
    coordinator = Coordinator(Env(pm, FuritaService, devpack_root=os.environ.get('FURITA_DEVPACK_ROOT')), FuritaService)
    coordinator.start(FuritaService)
    devpack = {}
    devpack["coordinator"] = coordinator
    devpack["pm"] = pm
    return devpack


def devpack_stop(devpack):
    devpack["coordinator"].stop(FuritaService)
    devpack["coordinator"].purge()
    devpack["coordinator"].hard_purge()
    devpack["pm"].release()


def create_user(name, devpack):
    bb_response = devpack["coordinator"].components[FakeBlackbox].register("{name}@yandex.ru".format(name=name))
    assert bb_response.status_code == 200
    try:
        bb_response_dict = json.loads(bb_response.text)
        if bb_response_dict["status"] == "ok" and "uid" in bb_response_dict:
            return bb_response_dict["uid"]
        else:
            return None
    except:
        return None


def get_user_shard(uid, devpack):
    sharpei = devpack["coordinator"].components[Sharpei]
    sharpei_response = sharpei.api().conninfo(uid=uid, mode='master')
    assert sharpei_response.status_code == 200

    shard_name = sharpei_response.json()["name"]
    return devpack["coordinator"].components[Mdb].shard_by_name(shard_name)


def create_named_rule(context, uid, rule_name):
    response = context.furita_api.api_edit(uid=uid, name=rule_name)
    assert response.status_code == 200
    return response.json()["id"]


def furita_start(host, config, retry=3):
    furita = yatest.common.binary_path("mail/furita/src/bin/furita")
    cmd = [furita, config]
    fname = "furita.bin.stderr.log"
    with open(fname, "w+") as furita_stderr:
        logger.info("Furita run path: {}".format(get_run_path()))
        execution = yatest.common.execute(cmd, wait=False, cwd=get_run_path(), stderr=furita_stderr)
        time.sleep(1)
        with open(fname, "r") as err_file_log:
            err_text_lines = "\n".join(err_file_log.readlines())
            assert err_text_lines == ""
        ping = False
        while not ping and retry > 0:
            ping = furita_ping(host)
            time.sleep(3)
            retry -= 1
        return execution if ping else None


def furita_stop(host, process, retry=5):
    process.kill()

    ping = True
    while ping and retry > 0:
        ping = furita_ping(host)
        time.sleep(3)
        retry -= 1
    return not ping


def furita_ping(host):
    try:
        r = requests.get("{host}/ping".format(host=host), timeout=0.5)
        return r.status_code == 200 and r.text == "pong"
    except:
        return False


def get_run_path():
    path = yatest.common.source_path("mail/furita/etc").split("/")
    path.pop()
    result = "/".join(path)
    return result
