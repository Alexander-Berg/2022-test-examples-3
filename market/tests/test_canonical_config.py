# coding: utf8
import os
import re

import yatest

from market.sre.tools.balancer_regenerate.lib import generate
from market.sre.tools.balancer_regenerate.lib.resolver import Resolver


class ResolverStub(Resolver):

    def get_hosts(self, service_def):
        return [{'name': 'testservice.market.yandex.net', 'port': 80}]


def test_canonical_config():
    """ Сравнить сгенерированный из шаблонов конфиг с каноническим.

    Тест нужен для того, чтобы проверять правки шаблонов конфигов балансера
    при коммите в аркадию, а не ручным просмотром diff-а на балансерах.
    """
    template_dir = yatest.common.source_path("market/sre/tools/balancer_regenerate/templates")
    output_dir = yatest.common.output_path()
    cert_dir = yatest.common.source_path("market/sre/conf/fslb/tests-common/cert")

    main_conf = {
        'main': {
            'default': {
                'template_dir': template_dir,
                'template_source': 'balancer.conf',
                'output_file': 'balancer.conf',
                'output_dir': output_dir,
                'archive_dir': output_dir,
                'ca_file': 'ca_file',
                'default_cert': 'csadmin',
                'cert': 'csadmin',
                'vs_map': '00-ip-service-map.yaml',
            },
        },
    }
    ip_service_map = {
        'main': {
            'default': {
                'local': ['::1'],
                'market': {"http2": True, 'ips': ['*']},
            }
        }
    }

    test_data_dir = yatest.common.source_path('market/sre/tools/balancer_regenerate/tests/testdata')
    configs = [
        (filename, test_data_dir)
        for filename in os.listdir(test_data_dir)
        if filename.endswith('.yaml') and not filename.startswith('00-')
    ]

    generate(
        env='production',
        resolver=ResolverStub(),
        dc='unk',
        main_conf=main_conf,
        configs=configs,
        ip_service_map=ip_service_map,
        cache_dir=output_dir,
        sd_cache_dir=output_dir,
        sd_host='2a02:6b8:0:3400:0:71d:0:c4',
        sd_port=8080,
        log_dir=output_dir,
        cert_dir=cert_dir,
        template_dir=template_dir,
        output_dir=output_dir,
        cert='testcert',
        yandex_balancer_bin=yatest.common.binary_path('balancer/daemons/balancer/balancer'),
        hostname="testhostname.market.yandex.net",
        lua_args=['ca_proxy_file={}'.format(os.path.join(cert_dir, 'allCAs.pem')),
                  'set_no_file=false'],  # only for testing
    )

    with open(os.path.join(output_dir, 'balancer.conf'), 'rt') as fd:
        config_text = fd.read()
    for d in [output_dir, cert_dir, template_dir]:
        config_text = re.sub("\"{}/?\"".format(d), '"."', config_text)
        config_text = re.sub("{}/?".format(d), '', config_text)

    return config_text
