import os

from vault_client.instances import Production as VaultClient


def set_secrets():
    client = VaultClient(
        authorization='OAuth {}'.format(os.environ['VAULT_TOKEN']),
        decode_files=True
    )
    head_version = client.get_version('sec-01cvpy390a83q49c4ak0bgwvjh')
    os.environ["TESTPALM_API"] = head_version['value']['testpalm']
    os.environ["STARTRECK_TOKEN"] = head_version['value']['startreck']
    os.environ["HITMAN_TOKEN"] = head_version['value']['hitman']
    os.environ["YT_TOKEN"] = head_version['value']['yt']
    os.environ["YQL_TOKEN"] = head_version['value']['yql']
    os.environ["STAT_TOKEN"] = head_version['value']['stat']
    os.environ["SSH_KEY"] = head_version['value']['openSSH']
    os.environ["SSH_PWD"] = head_version['value']['ssh_key_passphrase']
    os.environ["TESTPALM_OAUTH"] = head_version['value']['tp_oauth']
    os.environ["ABC_TOKEN"] = head_version['value']['abc.token']
    os.environ["AB_TOKEN"] = head_version['value']['ab.token']
    os.environ["PWD"] = head_version['value']['password']


if __name__ == '__main__':
    set_secrets()
