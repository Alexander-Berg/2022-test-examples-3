#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import codecs
from datetime import datetime
from dateutil import parser
from configHelpers import load_config

from startrek_client import Startrek
st = Startrek(useragent='market-unskip', base_url='https://st-api.yandex-team.ru/', token=os.environ.get('ST_TOKEN'))

skip_file_full_path = '../skipped/market/touch.json'

config_object = load_config(skip_file_full_path)

with codecs.open('./testNamesList.txt', encoding='utf8') as file:
    test_names_to_check = list(filter(
        lambda name: len(name.strip()) != 0,
        file.read().split('\n')
    ))

BORDER_DATE = datetime.fromisoformat('2021-12-01 00:00:00+00:00')
skipped_after_border_date = []

for record in config_object['actual']:
    for test_name in test_names_to_check:
        if test_name in record['fullNames']:
            key = record['issue']
            issue_created = parser.isoparse(st.issues[key]['createdAt'])
            print('{}, {}'.format(key, issue_created))

            if issue_created > BORDER_DATE:
                skipped_after_border_date.append(test_name)

print('Skipped after border date:')
print('\n'.join(skipped_after_border_date))
