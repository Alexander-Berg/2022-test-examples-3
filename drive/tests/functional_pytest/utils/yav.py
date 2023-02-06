import os

from library.python.vault_client.instances import Production as VaultClient


def _get_yav_client():
    if "YAV_TOKEN" not in os.environ:
        return VaultClient()
    with open(os.environ["YAV_TOKEN"]) as token_file:
        return VaultClient(rsa_auth=False, authorization=f"OAuth {token_file.read().strip()}")


def get_version():
    client = _get_yav_client()
    version = client.get_version("sec-01ey16akmwpyygcg5mrb4nyyjy")
    return version
