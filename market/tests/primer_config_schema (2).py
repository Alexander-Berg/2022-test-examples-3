# coding: utf8


import itertools
import logging
import re


from schema.schema import Schema, Regex, Or, And, Optional, SchemaError


log = logging.getLogger(__name__)
# https://cbonte.github.io/haproxy-dconv/1.8/configuration.html#2.4
timeval_regex = "^\\d+(us|ms|s|m|h|d)?$"


datacenters = ["iva", "sas", "vla", "man", "myt"]
environments = ["default", "unstable", "testing", "prestable", "production"]
environments += ['@'.join(i) for i in itertools.product(environments, datacenters)]


time_schema = Or(int, Regex(timeval_regex))


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


defaults_schema = Schema(Or(None, {
    Optional("check"): str,
    Optional("downinter"): time_schema,
    Optional("error_limit"): And(int, lambda n: 1 <= n),
    Optional("fall"): And(int, lambda n: 1 <= n),
    Optional("grpc_port"): And(int, lambda n: 0 < n <= 65535),
    Optional("grpc_port_offset"): And(int, lambda n: 0 <= n <= 65535),
    Optional("grpc_ssl"): bool,
    Optional("inter"): time_schema,
    Optional("maxconn"): And(int, lambda n: 0 <= n),
    Optional("maxqueue"): And(int, lambda n: 0 <= n),
    Optional("mode"): lambda s: s in {"tcp", "http", "health"},
    Optional("on_error"): lambda s: s in {"fastinter", "fail-check", "sudden-death", "mark-down"},
    Optional("retries"): And(int, lambda n: 0 <= n),
    Optional("rise"): And(int, lambda n: 1 <= n),
    Optional("slowstart"): time_schema,
    Optional("ssl"): str,
    Optional("timeout_client"): time_schema,
    Optional("timeout_client_fin"): time_schema,
    Optional("timeout_connect"): time_schema,
    Optional("timeout_queue"): time_schema,
    Optional("timeout_server"): time_schema,
    Optional("tfo"): bool,
    Optional("weight"): And(int, lambda n: 0 <= n <= 256),
}))

port = And(int, lambda n: 1 <= n <= 65535)

globals_schema = Schema(Or(None, {
    Optional("addr_fdee"): Or(str, None),
    Optional("addr_ip4"): str,
    Optional("addr_ip6"): str,
    Optional("balance"): Or(
        lambda s: s in {"random", "roundrobin", "static-rr", "leastconn", "first", "source", "url_param", "rdp-cookie"},
        Regex(r"^uri( whole)?( len \d+)?( depth \d+)?$"),
        Regex(r"^hdr\(.+\)?$"),
    ),
    Optional("balance_arg"): str,
    Optional("client_counters_avg"): time_schema,
    Optional("client_counters_max_rrate"): And(int, lambda n: 0 < n),
    Optional("client_counters_expire"): time_schema,
    Optional("client_counters_len"): And(int, lambda n: 0 < n <= 65535),
    Optional("client_counters_selector"): str,
    Optional("client_counters_size"): str,
    Optional("client_counters_type"): lambda s: s in {"binary", "string", "integer", "ip", "ipv6"},
    Optional("debug"): bool,
    Optional("display_name"): str,
    Optional("display_port"): port,
    Optional("grpc"): bool,
    Optional("grpc_display_port"): And(int, lambda n: 0 < n <= 65535),
    Optional("grpc_listen_port"): And(int, lambda n: 0 < n <= 65535),
    Optional("grpc_service_port"): And(int, lambda n: 0 < n <= 65535),
    Optional("grpc_ssl"): bool,
    Optional("grpc_ssl_cert"): str,
    Optional("hash_type"): Regex(r"^(map-based|consistent) (sdbm|djb2|wt6|crc32)( avalanche)?$"),
    Optional("host_counters_avg"): time_schema,
    Optional("host_counters_max_rrate"): And(int, lambda n: 0 < n),
    Optional("host_counters_expire"): time_schema,
    Optional("host_counters_len"): And(int, lambda n: 0 < n <= 65535),
    Optional("host_counters_selector"): str,
    Optional("host_counters_size"): str,
    Optional("host_counters_type"): lambda s: s in {"binary", "string", "integer", "ip", "ipv6"},
    Optional("http_reuse"): lambda n: n in {"never", "safe", "aggressive", "always"},
    Optional("listen_port"): port,
    Optional("max_rps"): And(int, lambda n: 1 <= n),
    Optional("max_spread_checks"): And(int, lambda n: 0 <= n),
    Optional("maxconn"): And(int, lambda n: 0 <= n),
    Optional("min_nbsrv"): And(int, lambda n: 0 <= n),
    Optional("min_nbsrv_tcp"): And(int, lambda n: 0 <= n),
    Optional("name"): str,
    Optional("nbthread"): And(int, lambda n: 1 <= n),
    Optional("ping"): str,
    Optional("ping_expect"): Regex(r"^(!\s+)?r?(status|string)\s+.+$"),
    Optional("ping_vhost"): str,
    Optional("port"): port,
    Optional("quiet"): bool,
    Optional("retries"): And(int, lambda n: 0 <= n),
    Optional("retry_on"): str,
    Optional("retry_post"): bool,
    Optional("service_name"): str,
    Optional("service_port"): port,
    Optional("spread_checks"): And(int, lambda n: 0 <= n),
    Optional("sticky"): bool,
    Optional("timeout_client"): time_schema,
    Optional("timeout_client_fin"): time_schema,
    Optional("timeout_connect"): time_schema,
    Optional("timeout_queue"): time_schema,
    Optional("timeout_server"): time_schema,
    Optional("tune_bufsize"): And(int, lambda n: 16384 <= n),
    Optional("tune_h2_header_table_size"): And(int, lambda n: 4096 <= n <= 65536),
    Optional("tune_h2_max_concurrent_streams"): And(int, lambda n: 0 < n),
    Optional("tune_http_maxhdr"): And(int, lambda n: 0 < n),
    Optional("rl_size_table"): Regex(r"^\d+(\w+)?$"),
    Optional("rl_expire"): Regex(r"^\d+(ms|s|h|m)?$"),
    Optional("rl_user_agent"): str,
    Optional("rl_rate"): int,
}))

meta_schema = Schema(Or(None, {
    Optional("monrun_ignore"): bool,
}))

config_schema = Schema({
    "params": {
        Regex("^(default|%[\\w_-]+)$"): {
            Optional("generate"): bool,
            Optional("resolve"): bool,
            Optional("static_file"): str,
            Optional("templates"): list,
        },
    },
    Optional("values"): {
        And(str, validate_section_name): {
            # Defaults
            Optional("be1_defaults"): defaults_schema,
            Optional("be2_defaults"): defaults_schema,
            Optional("be3_defaults"): defaults_schema,
            Optional("be4_defaults"): defaults_schema,
            Optional("be5_defaults"): defaults_schema,
            Optional("be_defaults"): defaults_schema,
            Optional("defaults"): defaults_schema,
            Optional("fe_defaults"): defaults_schema,

            Optional("grpc_be1_defaults"): defaults_schema,
            Optional("grpc_be2_defaults"): defaults_schema,
            Optional("grpc_be3_defaults"): defaults_schema,
            Optional("grpc_be4_defaults"): defaults_schema,
            Optional("grpc_be5_defaults"): defaults_schema,
            Optional("grpc_be_defaults"): defaults_schema,
            Optional("grpc_defaults"): defaults_schema,
            Optional("grpc_fe_defaults"): defaults_schema,

            Optional("http_be1_defaults"): defaults_schema,
            Optional("http_be2_defaults"): defaults_schema,
            Optional("http_be3_defaults"): defaults_schema,
            Optional("http_be4_defaults"): defaults_schema,
            Optional("http_be5_defaults"): defaults_schema,
            Optional("http_be_defaults"): defaults_schema,
            Optional("http_defaults"): defaults_schema,
            Optional("http_fe_defaults"): defaults_schema,

            # Globals
            Optional("be1_globals"): globals_schema,
            Optional("be2_globals"): globals_schema,
            Optional("be3_globals"): globals_schema,
            Optional("be4_globals"): globals_schema,
            Optional("be5_globals"): globals_schema,
            Optional("be_globals"): globals_schema,
            Optional("fe_globals"): globals_schema,
            Optional("globals"): globals_schema,

            Optional("grpc_be1_globals"): globals_schema,
            Optional("grpc_be2_globals"): globals_schema,
            Optional("grpc_be3_globals"): globals_schema,
            Optional("grpc_be4_globals"): globals_schema,
            Optional("grpc_be5_globals"): globals_schema,
            Optional("grpc_be_globals"): globals_schema,
            Optional("grpc_fe_globals"): globals_schema,
            Optional("grpc_globals"): globals_schema,

            Optional("http_be1_globals"): globals_schema,
            Optional("http_be2_globals"): globals_schema,
            Optional("http_be3_globals"): globals_schema,
            Optional("http_be4_globals"): globals_schema,
            Optional("http_be5_globals"): globals_schema,
            Optional("http_be_globals"): globals_schema,
            Optional("http_fe_globals"): globals_schema,
            Optional("http_globals"): globals_schema,

            # Headers
            Optional("be1_headers"): {str: str},
            Optional("be2_headers"): {str: str},
            Optional("be3_headers"): {str: str},
            Optional("be4_headers"): {str: str},
            Optional("be5_headers"): {str: str},
            Optional("headers"): {str: str},

            Optional("grpc_be1_headers"): {str: str},
            Optional("grpc_be2_headers"): {str: str},
            Optional("grpc_be3_headers"): {str: str},
            Optional("grpc_be4_headers"): {str: str},
            Optional("grpc_be5_headers"): {str: str},
            Optional("grpc_headers"): {str: str},

            Optional("http_be1_headers"): {str: str},
            Optional("http_be2_headers"): {str: str},
            Optional("http_be3_headers"): {str: str},
            Optional("http_be4_headers"): {str: str},
            Optional("http_be5_headers"): {str: str},
            Optional("http_headers"): {str: str},

            # TODO: допустимые значения
            Optional("no_options"): dict,
            Optional("no_options_backends"): dict,

            Optional("grpc_no_options"): dict,
            Optional("grpc_no_options_backends"): dict,

            Optional("http_no_options"): dict,
            Optional("http_no_options_backends"): dict,

            Optional("options"): Or(None, dict),
            Optional("options_backends"): dict,

            Optional("grpc_options"): Or(None, dict),
            Optional("grpc_options_backends"): dict,

            Optional("http_options"): Or(None, dict),
            Optional("http_options_backends"): dict,

            Optional("be_no_options"): dict,
            Optional("be1_no_options"): dict,
            Optional("be2_no_options"): dict,
            Optional("be3_no_options"): dict,
            Optional("be4_no_options"): dict,
            Optional("be5_no_options"): dict,

            Optional("grpc_be_no_options"): dict,
            Optional("grpc_be1_no_options"): dict,
            Optional("grpc_be2_no_options"): dict,
            Optional("grpc_be3_no_options"): dict,
            Optional("grpc_be4_no_options"): dict,
            Optional("grpc_be5_no_options"): dict,

            Optional("http_be_no_options"): dict,
            Optional("http_be1_no_options"): dict,
            Optional("http_be2_no_options"): dict,
            Optional("http_be3_no_options"): dict,
            Optional("http_be4_no_options"): dict,
            Optional("http_be5_no_options"): dict,

            Optional("be_no_options_backends"): dict,
            Optional("be1_no_options_backends"): dict,
            Optional("be2_no_options_backends"): dict,
            Optional("be3_no_options_backends"): dict,
            Optional("be4_no_options_backends"): dict,
            Optional("be5_no_options_backends"): dict,

            Optional("grpc_be_no_options_backends"): dict,
            Optional("grpc_be1_no_options_backends"): dict,
            Optional("grpc_be2_no_options_backends"): dict,
            Optional("grpc_be3_no_options_backends"): dict,
            Optional("grpc_be4_no_options_backends"): dict,
            Optional("grpc_be5_no_options_backends"): dict,

            Optional("http_be_no_options_backends"): dict,
            Optional("http_be1_no_options_backends"): dict,
            Optional("http_be2_no_options_backends"): dict,
            Optional("http_be3_no_options_backends"): dict,
            Optional("http_be4_no_options_backends"): dict,
            Optional("http_be5_no_options_backends"): dict,

            Optional("be_options"): dict,
            Optional("be1_options"): dict,
            Optional("be2_options"): dict,
            Optional("be3_options"): dict,
            Optional("be4_options"): dict,
            Optional("be5_options"): dict,

            Optional("grpc_be_options"): dict,
            Optional("grpc_be1_options"): dict,
            Optional("grpc_be2_options"): dict,
            Optional("grpc_be3_options"): dict,
            Optional("grpc_be4_options"): dict,
            Optional("grpc_be5_options"): dict,

            Optional("http_be_options"): dict,
            Optional("http_be1_options"): dict,
            Optional("http_be2_options"): dict,
            Optional("http_be3_options"): dict,
            Optional("http_be4_options"): dict,
            Optional("http_be5_options"): dict,

            Optional("be_options_backends"): dict,
            Optional("be1_options_backends"): dict,
            Optional("be2_options_backends"): dict,
            Optional("be3_options_backends"): dict,
            Optional("be4_options_backends"): dict,
            Optional("be5_options_backends"): dict,

            Optional("grpc_be_options_backends"): dict,
            Optional("grpc_be1_options_backends"): dict,
            Optional("grpc_be2_options_backends"): dict,
            Optional("grpc_be3_options_backends"): dict,
            Optional("grpc_be4_options_backends"): dict,
            Optional("grpc_be5_options_backends"): dict,

            Optional("http_be_options_backends"): dict,
            Optional("http_be1_options_backends"): dict,
            Optional("http_be2_options_backends"): dict,
            Optional("http_be3_options_backends"): dict,
            Optional("http_be4_options_backends"): dict,
            Optional("http_be5_options_backends"): dict,

            Optional("meta"): meta_schema,
        },
    },
    Optional("servers"): {
        And(str, validate_section_name): {
            And(str, lambda s: s in {"main", "fallback", "be1", "be2", "be3"}): Or(None, [{
                Optional("addr"): str,
                Optional("agent_inter"): time_schema,
                Optional("agent_port"): int,
                Optional("agent_port_offset"): And(int, lambda n: 0 <= n),
                Optional("agent_send"): str,
                # TODO: допустимые значения all
                Optional("backup"): Or(None, str),
                # TODO: проверить, что есть в шаблоне all
                Optional("check"): str,
                Optional("check_port"): And(int, lambda n: 0 <= n <= 65536),
                Optional("check_port_offset"): And(int, lambda n: 0 <= n),
                Optional("downinter"): time_schema,
                Optional("error_limit"): And(int, lambda n: 1 <= n),
                Optional("fall"): And(int, lambda n: 1 <= n),
                Optional("grpc_port"): And(int, lambda n: 0 < n <= 65535),
                Optional("grpc_port_offset"): And(int, lambda n: 0 <= n <= 65535),
                Optional("grpc_ssl"): bool,
                Optional("inter"): time_schema,
                Optional("maxconn"): And(int, lambda n: 0 <= n),
                Optional("maxqueue"): And(int, lambda n: 0 <= n),
                Optional("name"): str,
                Optional("order"): int,
                Optional("port"): int,
                Optional("port_offset"): And(int, lambda n: 0 <= n),
                Optional("resolve"): bool,
                Optional("rise"): And(int, lambda n: 1 <= n),
                Optional("slowstart"): time_schema,
                Optional("sort_order"): And(str, lambda s: s in ["shuffled", "reversed"]),
                Optional("ssl"): str,
                Optional("tfo"): bool,
                Optional("track_name"): str,
                Optional("track_port"): int,
                Optional("track_port_offset"): And(int, lambda n: 0 <= n),
                Optional("weight"): And(int, lambda n: 0 <= n <= 256),
                Optional("label"): str,
                Optional("dns_resolvers"): str,
            }])
        },
    },
})
