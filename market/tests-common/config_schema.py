# coding: utf8
import json
import re

from schema.schema import Schema, And, Use, Optional, Regex
from conftest import ENV

import logging


log = logging.getLogger(__name__)


def get_schema(ip_service_map):
    def regexp_validator(value):
        """ Проверяет, что регулярное выражение компилируется

        Конфиги yandex-balancer написаны на lua, поэтому все слэши в строках с регурярными выражениями удваиваются.
        """
        re.compile(value.replace('\\\\', '\\'))
        return True

    def vs_validator(vs):
        """ vs должен быть определен в файле 00-ip-service-map.yaml """
        # В тестинге vs может не существовать, в этом случае используется fallback на vs "market".
        if ENV == 'testing':
            return (vs in ip_service_map['main'][ENV] or 'market' in ip_service_map['main'][ENV])
        return (vs in ip_service_map['main'][ENV])

    def health_check_validator(health_check):
        return re.match(r'^GET [^\s]+ HTTP/1.[01]\r\nHost: [^\s]+\r\n\r\n$', json.loads(health_check))

    def interval_validator(interval):
        return re.match(r'^\d+[smh]$', interval)

    context = {
        Optional('antirobot'): bool,
        Optional('antirobot_service_name'): str,
        Optional('antirobot_cut_request_bytes'): And(Use(int), lambda n: n >= 512 and n % 256 == 0),
        Optional('attempts'): And(Use(int), lambda n: n > 0),
        Optional('cert'): str,
        # TODO: Это поле точно определено в шаблоне в этой секции?
        Optional('default_cert'): str,
        Optional('delay'): interval_validator,
        Optional('disable_https'): bool,
        Optional('disable_icookie'): bool,
        Optional('exp_getter'): bool,
        Optional('fallback_pool'): str,
        Optional('fast_503'): bool,
        Optional('fast_attempts'): And(Use(int), lambda n: n > 0),
        Optional('headers'): {str: Use(str)},
        Optional('health_check'): health_check_validator,
        Optional('http_port_shift'): And(Use(int), lambda n: n > 0),
        Optional('monitor'): bool,
        Optional('no_yandex_uid'): bool,
        Optional('paths'): [str],
        Optional('plain_http'): bool,
        Optional('port_shift'): And(Use(int), lambda n: n > 0),
        Optional('priority'): And(Use(int), lambda n: n > 0),
        Optional('proxy_host'): str,
        Optional('return_last_5xx'): bool,
        Optional('rewrite_regexp'): regexp_validator,
        Optional('rewrite_url'): str,
        Optional('rps_limiter'): bool,
        Optional('rps_limiter_skip_on_error'): bool,
        Optional('rps_limiter_log_quota'): bool,
        Optional('rps_limiter_namespace'): str,
        Optional('rps_limiter_quota_name'): str,
        Optional('rps_limiter_disable_file'): str,
        Optional('rps_limiter_backends_function'): str,
        Optional('rps_limiter_connect_timeout'): str,
        Optional('rps_limiter_backend_timeout'): str,
        Optional('nel'): bool,
        Optional('nel_max_age'): And(Use(int), lambda n: 0 <= n),
        Optional('nel_success_fraction'): And(Use(float), lambda n: 0 <= n <= 1),
        Optional('nel_failure_fraction'): And(Use(float), lambda n: 0 <= n <= 1),
        Optional('rs_weight'): And(Use(int), lambda n: 0 <= n < 1000),
        # FIXME: поле optional только из-за специального service_name=captcha, для которого оно не указывается
        # FIXME: Ееее, pcre и pire полностью несовместимы
        #   valid PCRE ([\\w\\d-_]+\\.)?metabar\\.ru
        #   valid PIRE ([\\w\\d_-]+\\.)?metabar\\.ru
        Optional('servername_regexp'): str,
        'service_name': Regex(
            r'^[a-zA-Z][\w]+$',
            error='service_name должен начинаться с буквы и содержать только буквы, цифры и подчеркивания'
        ),
        Optional('service_type'): And(str, lambda s: s in ("redirect", "proxy", "uaas")),
        Optional('special'): bool,
        Optional('starttime_cookie'): bool,
        Optional('status_code_blacklist_exceptions'): str,
        Optional('status_code_blacklist'): str,
        Optional('testid_from_host'): bool,
        Optional('trust_upstream_headers'): bool,
        Optional('uaas_service_name'): str,
        'vs': vs_validator,
        Optional('balancing_method'): And(str, lambda s: s in (
            "active",
            "hashing",
            "rr",
            "weighted2",
            "rendezvous_hashing",
        )),
        Optional('alternative_backends'): bool,
        Optional('alt_headers'): [
            {
                'alt_name': Regex(r'\w[\w_]+'),
                'alt_header': str,
                'alt_regexp': str
            }
        ],
        Optional('access_policy'): And(str, lambda s: s in ('allow', 'deny', 'webauth'), error='access_policy must be one from (allow, deny, webauth)'),
        Optional('allowed_path'): And(str, lambda s: s !='/', error='allowed_path can not be root, use access_policy:allow instead'),
        Optional('denied_path'): And(str, lambda s: s !='/', error='denied_path can not be root, use access_policy:deny instead'),
        Optional('webauth_external'): bool,
        Optional('webauth_idm_role'): And(str, lambda s: s.startswith('/'), lambda s: s.endswith('/'), error='webauth_idm_role has wrong format. Example: /idm_system/path/to/role/'),
        Optional('webauth_protected_path'): And(str, lambda s: s !='/', error='webauth_protected_path can not be root, use access_policy:webauth instead'),
    }

    servers = {
        'main': [
            {
                Optional('keepalive_count'): And(Use(int), lambda n: n >= 0),
                'name': And(Use(str), lambda n: not n.startswith('fdee:fdee')),
                Optional('plain_http_backend'): bool,
                Optional('web_sockets'): bool,
                Optional('port'): And(Use(int), lambda n: 1 <= n <= 65535),
                Optional('timeout'): Regex(r'^\d+(ms|s)$'),
                Optional('weight'): And(Use(int), lambda n: n > 0),
            },
        ],
        Optional('alternative'): [
            {
                Optional('keepalive_count'): And(Use(int), lambda n: n >= 0),
                'name': And(Use(str), lambda n: not n.startswith('fdee:fdee')),
                Optional('plain_http_backend'): bool,
                Optional('web_sockets'): bool,
                Optional('port'): And(Use(int), lambda n: 1 <= n <= 65535),
                Optional('timeout'): Regex(r'^\d+(ms|s)$'),
                Optional('weight'): And(Use(int), lambda n: n > 0),
            },
        ]
    }

    config_schema = Schema({
        'context': {
            Regex(r'^default(@(man|vla|sas|iva))?$'): context,
            Optional(Regex(r'^testing(@(man|vla|sas|iva))?$')): context,
        },
        Optional('servers'): {
            Regex(r'^default(@(man|vla|sas|iva))?$'): servers,
            Optional(Regex(r'^testing(@(man|vla|sas|iva))?$')): servers,
        },
    })

    return config_schema
