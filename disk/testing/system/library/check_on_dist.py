#!/usr/bin/python
# -*- coding: utf-8 -*-

import requests
import json

from httplib import OK

# import module snippets
from ansible.module_utils.basic import *


DOCUMENTATION = '''
---
module: check_on_dist
short_description: Checks is there specified package in dist repository.
description:
     - You can check package on dist.
     - This module can also be used to wait when package will be added in dist repository after
       uploading this package. For this purpose, you should use that module with do-until loop.
version_added: "2.2"
options:
  name:
    description:
      - package name. Alias: I(package)
    required: true
    aliases: ["package"]
  version:
    description:
      - package version
    required: true
requirements: []
author:
    - "Ivan Kuznetsov (@kis8ya)"
'''

EXAMPLES = '''
- name: Check package every 5 mins (3 times)
  check_on_dist:
    package: my-app
    version: 1.23-45
  register: dist
  until: dist.result
  delay: 300
  retries: 3
'''


def find_on_dist(name, version):
    response = requests.get("http://dist.yandex.ru/find?pkg={}&ver={}".format(name, version))
    if response.status_code == OK:
        return json.loads(response.content)


def main():
    module = AnsibleModule(
        argument_spec=dict(
            name=dict(type="str", required=True, aliases=["package"]),
            version=dict(type="str", required=True)
        ),
        supports_check_mode=True
    )

    name = module.params.get("name")
    version = module.params.get("version")

    package_locations = find_on_dist(name, version)

    if not package_locations:
        module.fail_json(msg="Package was not found.", result=False)

    result_args = dict(
        result=True,
        distributions=package_locations
    )
    module.exit_json(**result_args)


if __name__ == '__main__':
    main()
