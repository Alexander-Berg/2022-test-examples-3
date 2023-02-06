from socket import gethostbyaddr, gethostname

RECEIVED_TMPL = (
    "by {local_host} ({cluster_name}/Yandex) with {protocol} id {id};\r\n"
)

RECEIVED_TMPL_WITH_SOURCE = (
    "from {local_host} ({remote_host} [{remote_ip}])\r\n"
    "\tby {local_host} ({cluster_name}/Yandex) with {protocol} id {id};\r\n"
)

RECEIVED_TMPL_WITH_SOURCE_AND_USER = (
    "from {remote_host} ({remote_host} [{remote_ip}])\r\n"
    "\tby {local_host} ({cluster_name}/Yandex) with {protocol} id {id}\r\n"
    "\tfor <{email}>;"
)

CERTIFICATE_PART = (
    "(using TLSv1.3 with cipher TLS_AES_256_GCM_SHA384 (256/256 bits))\r\n"
    "\t(Client certificate not present)"
)


def get_received_info(env, session_id, protocol="SMTP", remote_ip="127.0.0.1", email=None):
    return {
        "remote_host": gethostbyaddr("127.0.0.1")[0],
        "remote_ip": remote_ip,
        "local_host": gethostname(),
        "cluster_name": env.conf.nwsmtp.cluster_name,
        "id": session_id,
        "protocol": protocol,
        "email": email
    }


def _build_received(tmpl, received_info) -> bytes:
    return b"Received: " + tmpl.format(**received_info).encode()


def build_received(received_info):
    return _build_received(RECEIVED_TMPL, received_info)


def build_received_with_source(received_info):
    return _build_received(RECEIVED_TMPL_WITH_SOURCE, received_info)


def build_received_with_source_and_user(received_info):
    return _build_received(RECEIVED_TMPL_WITH_SOURCE_AND_USER, received_info)
