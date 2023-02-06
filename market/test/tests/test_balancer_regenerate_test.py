# coding: utf8
import os
from ruamel.yaml import YAML
from ipaddress import IPv6Network

import yatest
from market.sre.tools.balancer_regenerate.lib import generate
from market.sre.tools.balancer_regenerate.lib.resolver import Resolver
from conftest import get_template_dir, get_temp_dir, get_cert_dir, ENV, get_value_dirs


class ResolverStub(Resolver):
    def get_hosts(self, service_def):
        return [{'name': 'testservice.market.yandex.net', 'port': 80}]


def test_balancer_regenerate_test(ip_service_map):
    # sandbox в общем случае не поддерживает ipv4 адреса, а балансер пытается их резолвить
    # поэтому заменяю в тестах все адреса на ::
    loopback = iter(IPv6Network(u'::/96'))

    test_ip_service_map = {
        'main': {
            env_name: {
                service: [str(next(loopback))]
                for service in env.keys()
            }
            for env_name, env in ip_service_map['main'].items()
        }
    }

    yaml = YAML(typ='safe')
    with open(yatest.common.source_path("market/sre/conf/fslb/test/values-static/00-main.yaml")) as fd:
        main_conf = yaml.load(fd)

    configs = [
        (filename, path)
        for path in get_value_dirs()
        for filename in os.listdir(path)
        if filename.endswith('.yaml') and not filename.startswith('00-')
    ]

    generate(
        env=ENV,
        resolver=ResolverStub(),
        dc='unk',
        main_conf=main_conf,
        configs=configs,
        ip_service_map=test_ip_service_map,
        cache_dir=get_temp_dir(),
        sd_cache_dir=get_temp_dir(),
        sd_host='2a02:6b8:0:3400:0:71d:0:c4',
        sd_port=8080,
        log_dir=get_temp_dir(),
        cert_dir=get_cert_dir(),
        template_dir=get_template_dir(),
        output_dir=get_temp_dir(),
        cert='testcert',
        yandex_balancer_bin=yatest.common.binary_path('balancer/daemons/balancer/balancer'),
        hostname="testhostname.market.yandex.net",
        lua_args=['ca_proxy_file={}'.format(os.path.join(get_cert_dir(), 'allCAs.pem')),
                  'set_no_file=false'],  # only for testing

    )
