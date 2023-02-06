#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import codecs
from configHelpers import load_config, save_config
from startrek_client import Startrek
st = Startrek(useragent='market-unskip', base_url='https://st-api.yandex-team.ru/', token=os.environ.get('ST_TOKEN'))

skip_file_full_path = '../skipped/market/desktop.json'


def close_st_issue(st, issue_key):
    from startrek_client.exceptions import NotFound
    try:
        st.issues[issue_key].update(tags={'add': ['deleted']})
        transition = st.issues[issue_key].transitions['close']
        transition.execute(comment='Тесты из этой задачи больше не сущесутвуют (не бегают в прогоне белых тестов).\nТикет для связи: MARKETFRONT-96481', resolution='fixed')
        return 1
    except NotFound:
        # ignore
        return 0


config_object = load_config(skip_file_full_path)

with codecs.open('./testNamesList.txt', encoding='utf8') as file:
    not_found = list(filter(
        lambda name: len(name.strip()) != 0,
        file.read().split('\n')
    ))

record_remove_count = 0
for record in config_object['actual']:
    record['fullNames'] = list(filter(lambda name: name not in not_found, record['fullNames']))
    if len(record['fullNames']) == 0:
        new_closed = close_st_issue(st, record['issue'])
        record_remove_count += new_closed

config_object['actual'] = list(filter(lambda skip: len(skip['fullNames']) != 0, config_object['actual']))

print("Закрыто тикетов: %d" % record_remove_count)

save_config(skip_file_full_path, config_object)
