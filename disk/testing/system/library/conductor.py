#!/usr/bin/python
# -*- coding: utf-8 -*-

import requests
import os
import json
import yaml

from httplib import OK
from urlparse import urljoin

# import module snippets
from ansible.module_utils.basic import *


DOCUMENTATION = '''
---
module: conductor
short_description: Обеспечивает работу с сервисом выкатки пакетов Conductor.
description:
     - Позволяет создавать тикеты на выкладку.
     - Позволяет отслеживать статус тикета.
version_added: "2.2"
options:
  packages:
    description:
      - Список пар {"name": <название пакета>, "version": <версия пакета>}
    required: false
  branch:
    description:
      - branch, куда намереваемся устанавливать пакет
    required: false
  filters:
    description:
      - Фильтры, позволяеющие устанавливать пакеты на конкретные группы (детали смотри
        в документации по Conductor)
    required: false
  ticket:
    description:
      - Номер тикета, для получения его статуса
    required: false
requirements: []
author:
    - "Ivan Kuznetsov (@kis8ya)"
'''

EXAMPLES = '''
- name: Create ticket
  conductor:
    packages:
      - name: python-mpfs-api
        version: 1.23-79
    branch: testing
    filters:
      deploy_groups:
        - project: disk
          workflow: mpfs-api
          deploy_group: disk.mpfs-api-testing-latest
  register: created_ticket

- name: Wait for ticket to be done
  conductor:
    ticket: 123456
  register: ticket
  until: ticket.info.value.status == 'done'
  delay: 180
  retries: 6
'''


class ConductorClient(object):
    CONFIG_PATH = "~/.conductor"

    def __init__(self):
        full_config_path = os.path.expanduser(self.CONFIG_PATH)
        with open(full_config_path) as config_file:
            config = yaml.load(config_file, Loader=yaml.BaseLoader)

        self.base_url = config['base_url']
        self.token = config['token']
        self.auth_cookie = config['auth_cookie']
        self.oauth_headers = {'Authorization': 'OAuth {}'.format(self.token)}

    def _build_url(self, url):
        return urljoin(self.base_url, url)

    def create_ticket(self, packages, branch, filters, comment):
        data = {"ticket": {"branch": branch,
                           "comment": comment},
                "package": {str(index): package["name"]
                            for index, package in enumerate(packages)},
                "version": {str(index): package["version"]
                            for index, package in enumerate(packages)},
                "downgrade": {str(index): int(package.get("downgrade", 1))
                              for index, package in enumerate(packages)},
                "filters": filters}

        response = requests.post(self._build_url("auth_update/ticket_add"),
                                 json=data,
                                 headers={'Cookie': 'conductor_auth={}'.format(self.auth_cookie),
                                          'Content-Type': 'application/json'})

        if response.status_code == OK:
            result = {'ticket': response.content.split(" ")[1]}
        else:
            result = {'code': response.status_code,
                      'content': response.content,
                      'data': data}

        return result

    def get_ticket_info(self, ticket_id):
        response = requests.get(self._build_url("api/v1/tickets/{}").format(ticket_id),
                                headers=self.oauth_headers)

        return json.loads(response.content)

    def get_package_info(self, package_name):
        response = requests.get(self._build_url("api/package_version/{}").format(package_name),
                                headers=self.oauth_headers)

        return json.loads(response.content)[package_name]


def create_ticket(packages, branch, filters, comment):
    return ConductorClient().create_ticket(packages, branch, filters, comment)


def get_ticket_info(ticket_id):
    return ConductorClient().get_ticket_info(ticket_id)


def get_package_info(package_name):
    return ConductorClient().get_package_info(package_name)


def main():
    module = AnsibleModule(
        argument_spec=dict(
            packages=dict(type="list", default=None),
            branch=dict(type="str", default=None),
            comment=dict(type="str", default=None),
            filters=dict(type="dict", default=None),
            ticket=dict(type="str", default=None),
            package=dict(type="str", default=None)
        ),
        supports_check_mode=True
    )

    packages = module.params.get("packages")
    branch = module.params.get("branch")
    comment = module.params.get("comment")
    filters = module.params.get("filters")
    ticket = module.params.get("ticket")
    package = module.params.get("package")

    if ticket:
        result = get_ticket_info(ticket)
    elif packages:
        result = create_ticket(packages, branch, filters, comment)
    elif package:
        result = get_package_info(package)
    else:
        module.fail_json(msg="No valid options were provided.",
                         result=False)

    result_args = dict(
        result=True,
        info=result
    )
    module.exit_json(**result_args)


if __name__ == '__main__':
    main()
