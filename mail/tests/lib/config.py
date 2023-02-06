import os

from mail.nwsmtp.tests.lib.confdict import ConfDict
from mail.nwsmtp.tests.lib.config_io import write_conf
from mail.nwsmtp.tests.lib.configurable import DKIMKeys, DomainMap, PublicSuffix, ControlFrom
from mail.nwsmtp.tests.lib.util import is_corp


class Conf:
    def __init__(self, conf_obj: dict, path: str):
        self.conf = ConfDict(conf_obj["config"])
        self.path = path
        self.base_path = os.path.dirname(self.path)
        self.dkim_keys = DKIMKeys(self.modules.dkim.configuration, self.base_path)
        self.public_suffix = PublicSuffix(self.nwsmtp, self.base_path)
        self.control_from = ControlFrom(self.nwsmtp.message_processing.control_from,
                                        self.base_path, is_corp)
        if 'my_dest_domains' in self.nwsmtp.delivery.routing:
            self.domain_map = DomainMap(self.nwsmtp.delivery.routing.my_dest_domains, self.base_path)
        else:
            self.domain_map = None

    def is_corp(self):
        return is_corp(os.path.basename(self.base_path))

    def save(self):
        for item in (self.dkim_keys, self.public_suffix, self.control_from):
            item.init()
            item.save()
        if self.domain_map:
            self.domain_map.init()
            self.domain_map.save()
        write_conf({"config": self.conf.d}, self.path)

    @property
    def log(self):
        return self.conf.log

    @property
    def system(self):
        return self.conf.system

    @property
    def reactor(self):
        return self.system.reactor

    @property
    def modules(self):
        return self.conf.modules.module

    @property
    def web(self):
        return self.modules.web.configuration

    @property
    def recognizer(self):
        return self.modules.recognizer.configuration

    @property
    def nwsmtp(self):
        return self.modules.nwsmtp.configuration

    @property
    def ymod_smtp_server(self):
        if "ymod_smtp_server" in self.modules:
            return self.modules.ymod_smtp_server.configuration
        return None

    @property
    def tvm(self):
        return self.modules.tvm.configuration

    @property
    def web_server(self):
        if "web_server" in self.modules:
            return self.modules.web_server.configuration
        return None

    @property
    def remove_headers_list(self):
        if "remove_headers_list" in self.modules.nwsmtp.configuration.message_processing:
            return self.modules.nwsmtp.configuration.message_processing.remove_headers_list
        return None

    def values(self):
        return values(self.conf)

    def update_values(self, pred, callback):
        return update_values(self.conf, pred, callback)


def values(d):
    for _, v in d.items():
        if isinstance(v, (dict, ConfDict)):
            yield from values(v)
        elif isinstance(v, list):
            for i in v:
                if isinstance(i, dict):
                    yield from values(i)
                else:
                    yield i
        else:
            yield v


def update_values(d, pred, callback):
    for k, v in d.items():
        if isinstance(v, (dict, ConfDict)):
            update_values(v, pred, callback)
        elif isinstance(v, list):
            for i, _ in enumerate(v):
                if isinstance(v[i], dict):
                    update_values(v[i], pred, callback)
                elif pred(v[i]):
                    v[i] = callback(v[i])
        else:
            if pred(v):
                d[k] = callback(v)
