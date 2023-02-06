import logging
import os
import sys

from library.python.vault_client.instances import Production as VaultClient


def get_logger():
    log = logging.getLogger(__name__)
    stderr_handler = logging.StreamHandler(sys.stderr)
    stderr_handler.setFormatter(logging.Formatter('[%(asctime)s] %(pathname)s:%(lineno)d %(levelname)s - %(message)s'))
    log.addHandler(stderr_handler)
    log.setLevel(logging.DEBUG)
    return log


def decode_bytes(data: bytes):
    if isinstance(data, bytes):
        data = data.decode()
    assert isinstance(data, str)
    return data


def write_file(path: str, content: str):
    with open(path, 'w+') as f:
        f.write(decode_bytes(content))


def get_vault_secret(version: str):
    cli = VaultClient(decode_files=True)
    secret = cli.get_version(version)
    return secret['value']


def prepare_db_auth_data(version: str):
    '''
    Dumps the certificate in file 'root.crt' in working directory
    '''
    secret = get_vault_secret(version)
    password = secret['password']
    sslrootcert_path = os.path.join(os.getcwd(), 'root.crt')
    write_file(sslrootcert_path, secret['certificate'].decode())
    return password, sslrootcert_path
