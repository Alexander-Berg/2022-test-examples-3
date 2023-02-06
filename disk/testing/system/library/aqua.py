#!/usr/bin/python
# -*- coding: utf-8 -*-
import time

import requests

from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry
from urlparse import urljoin

# import module snippets
from ansible.module_utils.basic import *


DOCUMENTATION = '''
---
module: aqua
short_description: Обеспечивает работу с сервисом распределенного запуска тестов Aqua.
description:
     - Позволяет запускать тесты.
     - Позволяет получать информацию о запущенных тестах.
version_added: "2.2"
options:
  pack:
    description:
      - ID набора тестов для запуска
    required: false
  props:
    description:
      - переменные для запуска тестов
    required: false
  launch_id:
    description:
      - ID запущенных тестов
    required: false
requirements: []
author:
    - "Ivan Kuznetsov (@kis8ya)"
'''

EXAMPLES = '''
- name: Run tests
  aqua:
    pack: 1234567
    props
      platform.host: my.test.host
      platform.port: 63125
  register: launch_info

- name: Waiting for tests to be completed and report has been built
  aqua:
    launch_id: 654321
  register: tests_launch
  until: tests_launch.info.launchStatus == "REPORT_READY"
  delay: 120
  retries: 15
'''


class AquaClient(object):
    API_ENDPOINT = "http://aqua.yandex-team.ru/aqua-api/"

    @classmethod
    def _build_url(cls, url):
        return urljoin(cls.API_ENDPOINT, url)

    @classmethod
    def _session(cls):
        session = requests.Session()
        retry = Retry(total=5, read=5, connect=5,
                      backoff_factor=0.7,
                      status_forcelist=(500, 502, 504))
        session.mount("http://", HTTPAdapter(max_retries=retry))
        return session

    @classmethod
    def run(cls, pack, props=None):
        if props is None:
            props = {}

        response = cls._session().get(cls._build_url("services/launch/pack/{}").format(pack),
                                      params=props)

        return json.loads(response.content)

    @classmethod
    def get_launch_info(cls, launch_id):
        response = cls._session().get(cls._build_url("services/launch/{}").format(launch_id))

        return json.loads(response.content)

    @classmethod
    def restart_launch(cls, launch_id, failed_only=True):
        """Restarts launch.

        :param launch_id: launch id
        :param failed_only: restart only failed tests (default `True`)

        """
        response = cls._session().get(cls._build_url("services/launch/{}/restart?failed-only={}").format(launch_id,
                                                                                                         failed_only))

        return json.loads(response.content)

    @classmethod
    def wait_for_launch(cls, launch_id, poll_count=60):
        poll_count -= 1
        while (cls.get_launch_info(launch_id)['launchStatus'] not in ['REPORT_READY', 'REPORT_FAILED'] and
               poll_count):
            time.sleep(60)
            poll_count -= 1
        return cls.get_launch_info(launch_id)


def run_pack(pack, props):
    return AquaClient.run(pack, props)


def get_launch_info(launch_id):
    return AquaClient.get_launch_info(launch_id)


def run_pack_with_restarts(pack, props, restarts=2):
    """Runs pack and restarts failed tests."""
    run_info = AquaClient.run(pack, props)
    run_info = AquaClient.wait_for_launch(run_info['id'])
    del run_info['pack']
    while (run_info['failedSuites'] and
           restarts):
        restarts -= 1
        restart_info = AquaClient.restart_launch(run_info['id'])
        restart_info = AquaClient.wait_for_launch(restart_info['id'])
        # Update launch info
        run_info['passedSuites'] += restart_info['passedSuites']
        run_info['failedSuites'] = restart_info['failedSuites']
        run_info['reportRequestUrl'] = restart_info['reportRequestUrl']
        run_info['id'] = restart_info['id']
    return run_info


def main():
    module = AnsibleModule(
        argument_spec=dict(
            pack=dict(type="str", default=None),
            props=dict(type="dict"),
            restarts=dict(type="int", default=None),
            launch_id=dict(type="str", default=None)
        ),
        supports_check_mode=True
    )

    pack = module.params.get("pack")
    props = module.params.get("props")
    restarts = module.params.get("restarts")
    launch_id = module.params.get("launch_id")

    if pack:
        if restarts:
            result = run_pack_with_restarts(pack, props, restarts=restarts)
        else:
            result = run_pack(pack, props)
    elif launch_id:
        result = get_launch_info(launch_id)
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
