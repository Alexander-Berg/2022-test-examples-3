import socket
import etcd3 as etcd

import etcd3.exceptions as exceptions


_BAD_FILE_DESCRIPTOR_ERROR = 9
_ADDRESS_ALREADY_IN_USE_ERROR = 98


def etcd_up(host, port) -> bool:
    """
        Try connect to etcd_host and etcd_port, if error then it is up
    : return : bool
        Is etcd up ? True : False
    """
    test_conn = socket.socket()
    try:
        test_conn.bind((host, port))
        client = etcd.client()
        client.get('UUID')
    except OSError as e:
        return e.errno == _ADDRESS_ALREADY_IN_USE_ERROR or e.errno == _BAD_FILE_DESCRIPTOR_ERROR
    except (
        exceptions.ConnectionFailedError,
        exceptions.InternalServerError,
        exceptions.ConnectionTimeoutError,
        exceptions.PreconditionFailedError
    ):
        return False
    return False
