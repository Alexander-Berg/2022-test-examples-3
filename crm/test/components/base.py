import json
import os
import requests

from crm.space.test.helpers import get_root_dir


URL = 'https://crm-test.yandex-team.ru/'


class TestBaseSuite:
    root_path = get_root_dir()
    base_data_path = 'tests/data'
    path = ''
    method = ''

    @classmethod
    def get_json_data(cls, relative_path):
        relative_path = relative_path.replace('.', os.path.sep)
        full_path = os.path.join(cls.root_path, cls.base_data_path, relative_path) + '.json'
        with open(full_path, 'r', encoding='utf-8') as data_file:
            json = data_file.read()
            return json

    @classmethod
    def get_dict_data(cls, relative_path):
        json_data = cls.get_json_data(relative_path)
        return json.loads(json_data)

    def get_response(self, params=None, body=None, data_path=None, **kwargs):
        if self.path == '' or self.method == '':
            raise Exception('"Path" or "Method" in request can\'t be empty')

        url = URL + self.path
        headers = {'Cookie': kwargs['Session_cookie'],
                   'x-csrf-token': kwargs['x-csrf-token'],
                   'Content-Type': 'application/json'}
        if data_path:
            body = self.get_json_data(data_path)
        with requests.Session() as session:
            res = session.request(self.method, url, params=params, data=body, headers=headers, verify=False)
        return res


class TestBaseIssue(TestBaseSuite):
    @staticmethod
    def get_issue_data(data, issue_id):
        return data["storage"]["issues"][issue_id]["data"]

    @staticmethod
    def get_issue_id(data):
        return list(data["storage"]["issues"])[0]


class TestBaseTimeline(TestBaseSuite):
    @staticmethod
    def get_timeline_mail_data(data, mail_id):
        return data['storage']['mails'][str(mail_id)]['data']
