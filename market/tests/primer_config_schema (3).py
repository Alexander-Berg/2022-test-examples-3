# coding: utf8
import itertools
import re

from ipaddress import IPv6Address, IPv4Address
from schema.schema import Schema, Regex, Or, And, Optional, SchemaError

# https://nginx.ru/en/docs/syntax.html
buffers = And(str, Regex("^\\d+\\s+\\d+(k|K|m|M)?$"))
port = And(int, lambda n: 1 <= n <= 65535)
service_name = Regex("^[\\w()|_.-]+$")
size = Or(int, Regex("^\\d+(k|K|m|M)$"))
time_schema = Or(int, Regex("^\\d+(ms|s|m|h|d|w|M|y)?$"))
weight = And(int, lambda n: 0 <= n <= 1000)
onoff = And(str, lambda n: n in {"on", "off"})
redirect_codes = Or(301, 302, 307, 308)

datacenters = ["iva", "sas", "vla", "man", "myt"]
environments = ["default", "unstable", "testing", "prestable", "production"]
environments += ['@'.join(i) for i in itertools.product(environments, datacenters)]


def validate_section_name(section_name):
    if section_name in environments:
        return True

    if section_name.startswith('%'):
        return True

    if section_name.startswith('/'):
        try:
            re.compile(section_name[1:-1])
            return True
        except re.error as e:
            raise SchemaError(str(e))

    raise SchemaError('Invalid config section name: %s' % section_name)


def validate_upstream(value):
    """ Валидация конфигурации апстрима """
    if value.get('upstream_skip', False):
        if not value.get('upstream_name') and not value.get('upstream_name_map'):
            raise SchemaError('upstream_name or upstream_name_map should be specified if upstream_skip is True')

    return True


globals_schema = Schema(And(
    validate_upstream,
    {
        Optional('service_name'): service_name,
        Optional('access_log'): bool,
        Optional('access_policy'): And(str, lambda s: s in ('allow', 'deny', 'webauth'), error='access_policy must be one from (allow, deny, webauth)'),
        Optional('addr_fdee'): lambda x: IPv6Address(x.decode('utf8')),
        Optional('addr_ip4'): lambda x: IPv4Address(x.decode('utf8')),
        Optional('addr_ip6'): lambda x: IPv6Address(x.decode('utf8')),
        Optional('allowed_path'): And(str, lambda s: s !='/', error='allowed_path can not be root, use access_policy:allow instead'),
        Optional('backlog'): int,
        Optional('chunked_transfer_encoding'): bool,
        Optional('client_max_body_size'): size,
        Optional('default'): str,
        Optional('denied_locations'): str,
        Optional('denied_path'): And(str, lambda s: s !='/', error='denied_path can not be root, use access_policy:deny instead'),
        Optional('dont_set_real_ip'): bool,
        Optional('expose_port'): bool,
        Optional('fastopen'): str,
        Optional('force_remote_addr'): bool,
        Optional('http_port'): port,
        Optional('http2'): bool,
        Optional('idm_protected_location'): str,
        Optional('keepalive_timeout'): time_schema,
        Optional('large_buffers'): buffers,
        Optional('limit_req'): str,
        Optional('limit_req_zone'): str,
        Optional('log_name'): str,
        Optional('nginx_port'): port,
        Optional('noreuseport'): bool,
        Optional('ping_connect_timeout'): time_schema,
        Optional('ping_location'): str,
        Optional('ping_read_timeout'): time_schema,
        Optional('ping_send_timeout'): time_schema,
        Optional('proxy_buffering'): onoff,
        Optional('proxy_connect_timeout'): time_schema,
        Optional('proxy_max_temp_file_size'): size,
        Optional('proxy_read_timeout'): time_schema,
        Optional('proxy_send_timeout'): time_schema,
        Optional('rcvbuf'): size,
        Optional('redirect_from_host'): str,
        Optional('retry_limit'): int,
        Optional('retry_on'): str,
        Optional('retry_time_limit'): time_schema,
        Optional('server_name_alternative'): service_name,
        Optional('server_name_additional'): str,
        Optional('service_domain'): service_name,
        Optional('service_port'): port,
        Optional('sndbuf'): size,
        Optional('ssl'): bool,
        Optional('ssl_certificate'): str,
        Optional('ssl_http_port'): port,
        Optional('ssl_http_redirect'): bool,
        Optional('ssl_http_redirect_code'): redirect_codes,
        Optional('upstream_addr'): str,
        Optional('upstream_fallback'): str,
        Optional('upstream_fallback_ssl'): bool,
        Optional('upstream_host_header'): str,
        Optional('upstream_keepalive'): int,
        Optional('upstream_name'): str,
        Optional('upstream_name_map'): str,
        Optional('upstream_port'): port,
        Optional('upstream_servers_count'): int,
        Optional('upstream_skip'): bool,
        Optional('upstream_ssl'): bool,
        Optional('valid_dn'): str,
        Optional('valid_dn_testing'): str,
        Optional('web_sockets'): bool,
        Optional('webauth_external'): bool,
        Optional('webauth_idm_role'): And(str, lambda s: s.startswith('/'), lambda s: s.endswith('/'), error='webauth_idm_role has wrong format. Example: /idm_system/path/to/role/'),
        Optional('webauth_protected_path'): And(str, lambda s: s != '/', error='webauth_protected_path can not be root, use access_policy:webauth instead'),
        Optional('weight'): weight,
        Optional('x_forwarded_proto'): bool,
    }))

config_schema = Schema({
    "params": {
        Regex("^(default|%[\\w_-]+)$"): {
            Optional("generate"): bool,
            Optional("resolve"): bool,
            Optional("templates"): list,
            Optional("static_file"): str,
        },
    },
    Optional("values"): {
        And(str, validate_section_name): {
            Optional("globals"): globals_schema,
        },
    },
    Optional("servers"): {
        And(str, validate_section_name): {
            And(str, lambda s: s in {"geo"}): Or(None, [{
                "name": str,
                Optional("resolve"): bool,
            }])
        },
    },
})
