import os

from yatest.common import work_path

from mail.nwsmtp.tests.lib.users import gen_uid
from mail.nwsmtp.tests.lib.confdict import ConfDict


class Configurable:
    def __init__(self, conf, base_path):
        self.conf = conf
        self.base_path = base_path

    def init(self):
        pass

    def save(self):
        raise NotImplementedError()


class PublicSuffix(Configurable):
    def __init__(self, conf, base_path):
        # sometimes it's dict, sometimes is't str,  thanks to config-dumper
        assert isinstance(conf.public_suffix_list, (ConfDict, str))
        super(PublicSuffix, self).__init__(conf, base_path)
        self.tlds = None

    def init(self):
        if not self.is_enabled():
            return
        with open(work_path("public_suffix_list.dat"), encoding="utf-8") as fd:
            self.tlds = fd.read()

    def is_enabled(self):
        return bool(self.conf.public_suffix_list)

    def get_path(self):
        return os.path.join(self.base_path, self.conf.public_suffix_list)

    def save(self):
        if not self.is_enabled():
            return
        with open(self.get_path(), "w", encoding="utf-8") as fd:
            fd.write(self.tlds)


class DomainMap(Configurable):
    def __init__(self, conf, base_path):
        super(DomainMap, self).__init__(conf, base_path)
        self.domain_map = None

    def init(self):
        self.domain_map = "null"

    def get_domain_map_path(self):
        iter_paths = (i["__text"] for i in self.conf
                      if i["__text"].startswith("domain_maps"))
        fname = next(iter_paths, None)
        if fname is not None:
            return os.path.join(self.base_path, fname)
        return None

    def save(self):
        path = self.get_domain_map_path()
        if path is None:
            return
        with open(path, "w") as fd:
            fd.write(self.domain_map)


class DKIMKeys(Configurable):
    def __init__(self, conf, base_path):
        super(DKIMKeys, self).__init__(conf, base_path)
        self.keys = []

    def get_keys_path(self):
        return os.path.join(
            self.base_path,
            self.conf.keys.default)

    def save(self):
        with open(self.get_keys_path(), "w") as fd:
            for key in self.keys:
                fd.write(key + "\n")


class ControlFrom(Configurable):
    def __init__(self, conf, base_path, is_corp):
        super(ControlFrom, self).__init__(conf, base_path)
        self.domains = is_corp and [] or ["whitelist.yaconnect.com"]
        self.uids = [gen_uid(is_corp)]

    def save(self):
        if "white_list" not in self.conf or not self.conf.white_list.use:
            return

        domains_path = os.path.join(self.base_path, self.conf.white_list.domains)
        uids_path = os.path.join(self.base_path, self.conf.white_list.uids)

        with open(domains_path, "w") as fd:
            fd.write("\n".join(self.domains))
        with open(uids_path, "w") as fd:
            fd.write("\n".join(str(i) for i in self.uids))


def with_dkim_keys(conf_func):
    def add_dkim_keys_func(conf):
        dkim_keys_path = work_path("keys")
        conf.dkim_keys.conf.keys.default = 'opendkim-keylist'
        conf.dkim_keys.keys += [
            x.format(keys_path=dkim_keys_path) for x in [
                'mail._domainkey.yandex.ru       yandex.ru:mail:{keys_path}/yandex.ru/mail.private',
                'mail._domainkey.yandex-team.ru  yandex-team.ru:mail:{keys_path}/yandex-team.ru/mail.private',
            ]
        ]

        return conf_func(conf)

    return add_dkim_keys_func
