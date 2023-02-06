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


def test_balancer_regenerate_prod(ip_service_map):
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
    with open(yatest.common.source_path("market/sre/conf/fslb/prod/values-static/00-main.yaml")) as fd:
        main_conf = yaml.load(fd)

    dont_test = {
        # Add here config filenames that should be excluded from tests along with a reason.
        '11-antirobot.market_slb-highload-stable.yaml': True,
        '11-antirobot.market_slb-sovetnik-stable.yaml': True,
        '57-vendors-runtime.market_slb-highload-stable.yaml': True,
        '60-abo.market_slb-highload-stable.yaml': True,
        '62-market-redirects-to-ru.market_slb-highload-stable.yaml': True,
        '63-market-redirects.market_slb-highload-stable.yaml': True,
        '64-market-release-haproxy.market_slb-front-stable-vla-01.yaml': True,
        '64-market-release-haproxy.market_slb-highload-stable.yaml': True,
        '65-market-thanks.market_slb-highload-stable.yaml': True,
        '66-touch-redirects.market_slb-highload-stable.yaml': True,
        '67-touch-release-haproxy.market_slb-front-stable-vla-01.yaml': True,
        '67-touch-release-haproxy.market_slb-highload-stable.yaml': True,
        '68-pics.market_slb-highload-stable.yaml': True,
        '69-app-exp.market_slb-front-stable-vla-01.yaml': True,
        '70-app_market.market_slb-front-stable-vla-01.yaml': True,
        '70-app_market.market_slb-highload-stable.yaml': True,
        '72-mbo_market_proxy.market_slb-highload-stable.yaml': True,
        '73-mbo.market_slb-highload-stable.yaml': True,
        '99-cms-rss.market_slb-highload-stable.yaml': True,
        '101-affiliate-runtime.market_slb-front-stable-vla-01.yaml': True,
        '102-fenek-beru.market_slb-highload-stable.yaml': True,
        '104-fox-beru.market_slb-highload-stable.yaml': True,
        '168-turkey.market_slb-highload-stable.yaml': True,
        '298-notaclaim-redirects.market_slb-highload-stable.yaml': True,
        '299-notaclaim.market_slb-highload-stable.yaml': True,
        '301-sovetnik-redir.market_slb-sovetnik-stable.yaml': True,
        '302-sovetnik-suggest.market_slb-sovetnik-stable.yaml': True,
        '303-sovetnik.market_slb-sovetnik-stable.yaml': True,
        '304-sovetnik-metabar-static.market_slb-sovetnik-stable.yaml': True,
        '305-sovetnik-static-com.market_slb-sovetnik-stable.yaml': True,
        '306-sovetnik-static-ru.market_slb-sovetnik-stable.yaml': True,
        '307-sovetnik-static.market_slb-sovetnik-stable.yaml': True,
        '962-market_exp.market_slb-front-stable-vla-01.yaml': True,
        '963-market_default.market_slb-front-stable-vla-01.yaml': True,
        '964-touch_default.market_slb-front-stable-vla-01.yaml': True,
        '965-touch_exp.market_slb-front-stable-vla-01.yaml': True,
        '1062-blue-market-release-redirect.market_slb-highload-stable.yaml': True,
        '1164-blue-market-default.market_slb-front-stable-vla-01.yaml': True,
        '1165-blue-market-exp.market_slb-front-stable-vla-01.yaml': True,
        '1166-blue-touch-default.market_slb-front-stable-vla-01.yaml': True,
        '1167-blue-touch-exp.market_slb-front-stable-vla-01.yaml': True,
        '19290-content_api.market_slb-front-stable-vla-01.yaml': True,
        '19291-content_api_mobile.market_slb-front-stable-vla-01.yaml': True,
        '19292-content_api_mobile_antirobot.market_slb-front-stable-vla-01.yaml': True,
        '23209-mc-beru.market_slb-highload-stable.yaml': True,
        '25200-bringly-desktop.market_slb-front-stable-vla-01.yaml': True,
        '25441-market-googlebot.market_slb-highload-stable.yaml': True,
        '26356-beru.market_slb-front-stable-vla-01.yaml': True,
        '26382-mobile-beru.market_slb-front-stable-vla-01.yaml': True,
        '28348-ekb-mobile-beru.market_slb-front-stable-vla-01.yaml': True,
        '289510-ipa-touch-white.market_slb-front-stable-vla-01.yaml': True,
        '28996-widgets-delivery.market_slb-front-stable-vla-01.yaml': True,
        '28996-widgets-delivery.market_slb-highload-stable.yaml': True,
        '29011-widgets-delivery.market_slb-front-stable-vla-01.yaml': True,
        '29914-marketaff.market_slb-front-stable-vla-01.yaml': True,
        '29946-affiliate.market_slb-front-stable-vla-01.yaml': True,
        '31322-widgets-delivery.market_slb-front-stable-vla-01.yaml': True,
        '32760-checkout.market_slb-front-stable-vla-01.yaml': True,
        '32761-m_checkout.market_slb-front-stable-vla-01.yaml': True,
        '32762-m_beru.market_slb-front-stable-vla-01.yaml': True,
        '33182-pokupki.market_slb-front-stable-vla-01.yaml': True,
        '33183-m_pokupki.market_slb-front-stable-vla-01.yaml': True,
        '34668-ipa-blue.market_slb-front-stable-vla-01.yaml': True,
        '36043-default_exp_desktop.market_slb-front-stable-vla-01.yaml': True,
        '36044-exp_desktop.market_slb-front-stable-vla-01.yaml': True,
        '36045-default_exp_touch.market_slb-front-stable-vla-01.yaml': True,
        '36046-exp_touch.market_slb-front-stable-vla-01.yaml': True,
        '38538-partner_market.market_slb-front-stable-vla-01.yaml': True,
        '64-market-release-haproxy.market_slb-highload-stable-vla-01.yaml': True,
        '67-touch-release-haproxy.market_slb-highload-stable-vla-01.yaml': True,
        '70-app_market.market_slb-highload-stable-vla-01.yaml': True,
        '64-market-release-haproxy.market_slb-highload-stable-vla-01.yaml': True,
        '67-touch-release-haproxy.market_slb-highload-stable-vla-01.yaml': True,
        '70-app_market.market_slb-highload-stable-vla-01.yaml': True,
        '35827-seller.market_slb-front-stable-vla-01.yaml': True,
    }

    configs = [
        (filename, path)
        for path in get_value_dirs()
        for filename in os.listdir(path)
        if filename.endswith('.yaml') and not filename.startswith('00-') and filename not in dont_test
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
