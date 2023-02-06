# coding=utf-8


import requests
import subprocess
import yatest

from .service_ticket_provider import get_service_ticket, X_YA_SERVICE_TICKET


def get_real_path(resource):
    """ Возвращаем путь к ресурсу (это то, что указано в секции DATA в ya.make) """
    path = yatest.common.source_path(resource).split("/")
    path.pop()
    result = "/".join(path)
    return result


def exec_remote_command(host, cmd):
    command = [
        "ssh",
        "-o UserKnownHostsFile=/dev/null",
        "-o StrictHostKeyChecking=no",
        host,
        "{cmd}".format(cmd=cmd)
    ]
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (response, _) = p.communicate()
    return response


def get_uid_suid_and_user_ticket(passport_host, akita_host, credentials, original_host):
    url = "https://{host}/passport".format(host=passport_host)
    r = requests.post(url, data=credentials, allow_redirects=False)
    assert r.status_code == 302

    cookies = r.cookies.get_dict()

    url = "https://{host}:443/ninja_auth".format(host=akita_host)
    headers = {
        'X-Original-Host': original_host,
        X_YA_SERVICE_TICKET: get_service_ticket("akita")
    }
    r = requests.get(url, headers=headers, cookies=cookies)
    assert r.status_code == 200

    r = r.json()
    assert "account_information" in r
    r = r["account_information"]
    assert "account" in r and "ticket" in r
    account = r["account"]
    assert "userId" in account

    return account["userId"], account["serviceUserId"], r["userTicket"]
