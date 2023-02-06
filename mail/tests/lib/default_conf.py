import os

from contextlib import contextmanager
from functools import partial
from tempfile import mkdtemp
from urllib.parse import urlparse, urlunparse
from typing import ContextManager

from yatest.common import work_path, output_path
from yatest.common.network import PortManager

from mail.nwsmtp.tests.lib import HOST
from mail.nwsmtp.tests.lib.config import Conf
from mail.nwsmtp.tests.lib.config_io import read_conf, copy_conf, copy_files, get_conf_path


def is_conf_path(v):
    return isinstance(v, str) and v.startswith(("/etc/", "/var/"))


def is_url(v):
    if not isinstance(v, str):
        return False
    if urlparse(v).netloc:
        return True
    parts = v.split(":")
    if len(parts) == 2 and parts[-1].isdigit():
        return True
    return False


def replace_url(v, get_port):
    netloc = "%s:%d" % (HOST, get_port())
    parts = urlparse(v)
    if parts.netloc:
        if parts.scheme.endswith("s"):
            parts = parts._replace(scheme=parts.scheme[:-1])  # noqa
        return urlunparse(parts._replace(netloc=netloc))  # noqa
    return netloc


def disable_ipv6_only(ep):
    # Disable ipv6 only, because tests for
    # received_headers in sandbox forces ipv4
    if ep.get("ipv6_only"):
        ep["ipv6_only"] = False


def set_defaults(conf, base_path, get_port) -> None:
    conf.system.dir = base_path
    conf.system.gid = ""
    conf.system.uid = ""
    conf.system.pid = os.path.join(base_path, "nwsmtp.pid")

    for _, reactor_conf in conf.reactor.items():
        reactor_conf._pool_count = min(2, reactor_conf._pool_count)

    if "endpoints" in conf.nwsmtp:
        for endpoint in conf.nwsmtp.endpoints.listen:
            disable_ipv6_only(endpoint)
            endpoint["port"] = get_port()

    if "ssl_context" in conf.nwsmtp and conf.nwsmtp.ssl_context:
        conf.nwsmtp.ssl_context = {"key_file": work_path("cert.key"),
                                   "cert_file": work_path("cert.pem")}
    if conf.ymod_smtp_server and "ssl" in conf.ymod_smtp_server:
        conf.ymod_smtp_server.ssl = {"key_file": work_path("cert.key"),
                                     "cert_file": work_path("cert.pem")}

    conf.recognizer.encoding_dict = work_path("dict.dict")
    conf.recognizer.language_dict = work_path("queryrec.dict")
    conf.recognizer.language_weights = work_path("queryrec.weights")

    conf.nwsmtp.blackbox.keep_alive = False

    if conf.nwsmtp.cluster_name == "smtpcorp":
        conf.nwsmtp.blackbox.deny_auth_for_assessors = True

    conf.nwsmtp.blackbox.access_restricted_links_list = [{"key": "ru",  "link": "link_ru"},
                                                         {"key": "com", "link": "link_com"},
                                                         {"key": "tr",  "link": "link_tr"},
                                                         {"key": "by",  "link": "link_by"},
                                                         {"key": "kz",  "link": "link_kz"}]

    # TODO(MAILDLV-3265) Enable after fix use-of-uninitialized-value msan report
    conf.modules.mds_client.configuration.host_resolver.use = False
    # TODO(MAILDLV-3342) Figure out how to configure ymod_tvm for many destinations on localhost
    conf.modules.mds_client.configuration.tvm.use = False

    if conf.web_server:
        if "ssl" in conf.web_server and conf.web_server.ssl:
            conf.web_server.ssl = {"cert_file": work_path("web.pem"),
                                   "key_file": work_path("web.pem")}
        for endpoint in conf.web_server.endpoints.listen:
            disable_ipv6_only(endpoint)
            endpoint["port"] = get_port()

    if conf.ymod_smtp_server:
        for endpoint in conf.ymod_smtp_server.endpoints.listen:
            disable_ipv6_only(endpoint)
            endpoint["port"] = get_port()

    if conf.nwsmtp.cluster_name == "smtpcorp":
        conf.nwsmtp.message_processing.control_from.policy = "reject"

    if "smtp" in conf.nwsmtp.cluster_name:
        conf.nwsmtp.message_processing.control_from["white_list"] = {
            "use": 1,
            "domains": "control_from_white_list_domains_corp",
            "uids": "control_from_white_list_uids_corp"
        }

    conf.tvm.tvm_host = f"{HOST}:{get_port()}"
    conf.tvm.https = False
    conf.tvm.tvm_secret = "test_secret"

    conf.nwsmtp.aliases.maps = [work_path("virtual_alias_maps")]
    conf.nwsmtp.delivery.relays.local.targeting.use = False


@contextmanager
def make_conf(back: str, dest=None, customize_with=None) -> ContextManager[Conf]:
    if dest is None:
        dest = mkdtemp(back, dir=output_path())
    conf_path = get_conf_path(back)
    conf_path = copy_conf(conf_path, dest, back)

    obj = read_conf(conf_path)
    conf = Conf(obj, conf_path)
    conf.update_values(is_conf_path, lambda p: os.path.basename(p))

    copy_files(conf, dest)

    pm = PortManager()
    try:
        conf.update_values(is_url, partial(replace_url, get_port=pm.get_port))
        set_defaults(conf, dest, pm.get_port)

        if customize_with is not None:
            customize_with(conf)

        conf.save()

        yield conf
    finally:
        pm.release()
