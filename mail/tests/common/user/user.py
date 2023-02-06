# -*- coding: utf-8 -*-
import requests
import time
from mail.furita.tests.common.utils.utils import (
    get_uid_suid_and_user_ticket
)
from mail.furita.tests.common.utils.service_ticket_provider import (
    get_service_ticket, X_YA_SERVICE_TICKET, X_YA_USER_TICKET
)


def process_mops_request(url, data):
    headers = {
        X_YA_SERVICE_TICKET: get_service_ticket("mops"),
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': str(len(data))
    }
    return requests.post(url, data=data, headers=headers)


def if_task_in_list(tasks, task_group_id):
    for task in tasks:
        if task["taskGroupId"] == task_group_id:
            return True
    return False


def last_mops_done(host, uid, previous_mops_response, retry=5):
    assert previous_mops_response.status_code == 200
    pr = previous_mops_response.json()
    if "result" not in pr or pr["result"] != "ok":
        return False
    if pr["taskType"] == "sync":
        return True

    task_group_id = pr["taskGroupId"]
    url = "http://{host}/stat?uid={uid}".format(host=host, uid=uid)
    while retry > 0:
        r = requests.get(url, headers={X_YA_SERVICE_TICKET: get_service_ticket("mops")})
        if r.status_code == 200:
            r = r.json()
            if not if_task_in_list(r["tasks"], task_group_id):
                return True
        time.sleep(1)
        retry -= 1
    return False


class User():
    def __init__(self, uid, userTicket, wmi_host, mops_host, sendbernar_host):
        self.__wmi = wmi_host
        self.__mops = mops_host
        self.__sendbernar = sendbernar_host

        self.__uid = uid
        self.__userTicket = userTicket

    @property
    def uid(self):
        return self.__uid

    @property
    def userTicket(self):
        return self.__userTicket

    def simple_send_message(self, to, subject):
        url = "https://{host}:443/send_message?uid={uid}&subj={subject}&to={to}".format(
            host=self.__sendbernar,
            uid=self.uid,
            subject=subject,
            to=to
        )
        r = requests.post(url, headers={X_YA_SERVICE_TICKET: get_service_ticket("sendbernar")})
        assert r.status_code == 200
        r = r.json()
        assert "messageId" in r
        return r["messageId"]

    def get_fid_by_name(self, name):
        url = "https://{host}:443/folders?uid={uid}".format(
            host=self.__wmi,
            uid=self.__uid
        )
        r = requests.get(url, headers={
            X_YA_SERVICE_TICKET: get_service_ticket("wmi"),
            X_YA_USER_TICKET: self.__userTicket,
        })
        assert r.status_code == 200
        r = r.json()
        assert "folders" in r

        result = None
        for fid, folder in r["folders"].items():
            if folder["symbolicName"]["title"] == name:
                result = fid
                break

        assert result is not None
        return result

    def get_messages_by_fid(self, fid):
        url = "https://{host}:443/messages_by_folder?uid={uid}&first=0&count=10000&fid={fid}".format(
            host=self.__wmi,
            uid=self.__uid,
            fid=fid
        )
        r = requests.get(url, headers={
            X_YA_SERVICE_TICKET: get_service_ticket("wmi"),
            X_YA_USER_TICKET: self.__userTicket
        })
        assert r.status_code == 200
        r = r.json()
        assert "envelopes" in r
        return r["envelopes"]

    def remove_messages_from_fid(self, fid):
        url = "http://{host}/remove".format(host=self.__mops)
        data = []
        data.append("uid=" + self.__uid)
        data.append("suid=1")
        data.append("mdb=pg")
        data.append("fid={fid}".format(fid=fid))
        data = "&".join(data)

        r = process_mops_request(url, data)
        return last_mops_done(host=self.__mops, uid=self.uid, previous_mops_response=r)


# == TEST USER INITIALISATION ==

def init_test_user(credentials, passport_host, akita_host, x_original, wmi_host, mops_host, sendbernar_host):
    uid, suid, userTicket = get_uid_suid_and_user_ticket(passport_host, akita_host, credentials, x_original)
    return User(uid=uid, userTicket=userTicket, wmi_host=wmi_host, mops_host=mops_host, sendbernar_host=sendbernar_host)
