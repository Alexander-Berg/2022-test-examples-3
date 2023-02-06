# -*- coding: utf-8 -*-
import requests
import furita_helpers as helpers
import yatest.common
from mail.devpack.lib.components.sharpei import Sharpei, TvmApi
from mail.notsolitesrv.devpack.components.relay import RelayComponent
from mail.pg.furitadb.devpack.components.furitadb import FuritaDb
from yatest.common import work_path


class FuritaRequests(object):
    def __init__(self, host):
        self.Host = host

    def get(self, url, **kwargs):
        return requests.get(self.Host + url, **kwargs)

    def post(self, url, **kwargs):
        return requests.post(self.Host + url, **kwargs)


class Furita():
    def __init__(self, conf, port, config_params):
        self.furita_host = "http://localhost:{port}".format(port=port)
        self.conf = conf
        self.config_params = config_params

    def start(self, retry=5):
        self.execution = helpers.furita_start(self.furita_host, self.conf, retry)
        return False if self.execution is None else True

    def stop(self, retry=5):
        return helpers.furita_stop(self.furita_host, self.execution, retry)

    def restart(self):
        self.stop()
        self.start()


def get_default_params(devpack):
    sharpei_port = devpack["coordinator"].components[Sharpei].webserver_port()
    so_check_form_port = devpack["pm"].get_port()
    search_port = devpack["pm"].get_port()
    mops_port = devpack["pm"].get_port()
    tvmapi_port = devpack["coordinator"].components[TvmApi].port
    furita_port = devpack["pm"].get_port()
    furitadb_port = devpack["coordinator"].components[FuritaDb].port()
    secret_path = work_path("tvm_secret")
    tupita_port = devpack["pm"].get_port()
    blackbox_port = devpack["pm"].get_port()
    smtp_port = devpack["coordinator"].components[RelayComponent].port

    r = {
        "__FURITA_ALLOW_FORWARD__": "all",
        "__SEARCH_LOCAL_PORT__": "{port}".format(port=search_port),
        "__SHARPEI_LOCAL_PORT__": "{port}".format(port=sharpei_port),
        "__FURITA_LOCAL_PORT__": "{port}".format(port=furita_port),
        "__FURITA_DB_PORT__": "{port}".format(port=furitadb_port),
        "__SO_CHECK_FORM_LOCAL_PORT__": "{port}".format(port=so_check_form_port),
        "__TVMAPI_LOCAL_PORT__": tvmapi_port,
        "__MOPS_LOCAL_PORT__": mops_port,
        "__TVM_SECRET_PATH__": secret_path,
        "__TUPITA_LOCAL_PORT__": tupita_port,
        "__BLACKBOX_LOCAL_PORT__": blackbox_port,
        "__SMTP_LOCAL_PORT__": smtp_port,
    }
    return r


def generate_and_store_config(params):
    return gen_config("isolated.yml", params)


def get_furita(params):
    generate_and_store_config(params)
    furita_port = params["__FURITA_LOCAL_PORT__"]
    service = Furita("isolated.yml", furita_port, params)
    is_started = service.start()
    return service if is_started else None


def gen_config(fname, params):
    """
        fname -- имя конфига, куда всё будет сохранено (формат: etc/my_new_conf.yml)
        params -- словарь параметров и их значений
    """
    template_config_file = yatest.common.source_path("mail/furita/package/deploy/etc/furita/isolated_template.yml")
    template = ""
    with open(template_config_file, "r") as f:
        template = "".join(f.readlines())

    for param, value in params.items():
        template = template.replace(param, "{v}".format(v=value))
    config = template

    config_file = "{prefix}/{name}".format(prefix=helpers.get_run_path(), name=fname)
    with open(config_file, "w") as f:
        f.write(config)
