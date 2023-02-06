# -*- coding: utf-8 -*-

import requests
import os
from startrek_client import Startrek
import config
from set_secret import set_secret

set_secret.set_secrets()
client = Startrek(useragent="curl/7.53.1", token=os.environ['STARTRECK_TOKEN'])

QUEUES_DATA = config.data

headers = {"Authorization": "OAuth " + os.environ['STARTRECK_TOKEN']}

for queue in QUEUES_DATA:
    filter = QUEUES_DATA.get(queue).get("filter")
    data = QUEUES_DATA.get(queue).get("data")
    issues = client.issues.find(filter)

    for issue in issues:
        r = requests.get("https://st-api.yandex-team.ru/v2/issues/" + issue.key + "/checklistItems",
                         headers=headers)
        if r.status_code == 200 and not r.json():
            print("add checklist to " + issue.key)
            p = requests.post(
                "https://st-api.yandex-team.ru/v2/issues/" + issue.key + "/checklistItems?notify=false",
                headers=headers, json=data)
