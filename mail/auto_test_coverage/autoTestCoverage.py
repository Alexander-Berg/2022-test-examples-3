# -*- coding: utf-8 -*-

import json
import logging
import os
import time
import urllib
import urllib3
import urllib.parse

from datetime import date, timedelta

import requests

logging.basicConfig(level=logging.INFO)
for k, v in os.environ.items():
    logging.info('%s: %s', k, v)

total_automated = 'Mobile_Soft_Online/Automation/iOS_autotest_weekly'
potential_automated = 'Mobile_Soft_Online/Automation/iOS_autotest_potential_weekly'

BASE_URL = "https://testpalm.yandex-team.ru:443/api/testcases/mobmail_ios/preview?expression="

TESTPALM_TOKEN = os.environ['TESTPALM_TOKEN']
OAUTH_TOKEN = os.environ['OAUTH_TOKEN']

automatedAcceptance = {"type":"AND","left":{"type":"AND","left":{"type":"AND","left":{"type":"AND","left":{"type":"EQ","key":"attributes.54292800e4b0b658a756b7ea","value":"Acceptance"},"right":{"type":"EQ","key":"attributes.542aaecbe4b0b658a756b7ff","value":"ios"}},"right":{"type":"NEQ","key":"attributes.5996ab65c612335fffccdac9","value":"iPad"}},"right":{"type":"NEQ","key":"status","value":"archived"}},"right":{"type":"EQ","key":"isAutotest","value":"true"}}
allCasesInAcceptance = {"type":"AND","left":{"type":"AND","left":{"type":"AND","left":{"type":"EQ","key":"attributes.54292800e4b0b658a756b7ea","value":"Acceptance"},"right":{"type":"EQ","key":"attributes.542aaecbe4b0b658a756b7ff","value":"ios"}},"right":{"type":"NEQ","key":"attributes.5996ab65c612335fffccdac9","value":"iPad"}},"right":{"type":"NEQ","key":"status","value":"archived"}}
#potentialAutomatedAcceptance = {"type":"AND","left":{"type":"AND","left":{"type":"AND","left":{"type":"AND","left":{"type":"AND","left":{"type":"EQ","key":"attributes.54292800e4b0b658a756b7ea","value":"Acceptance"},"right":{"type":"EQ","key":"attributes.542aaecbe4b0b658a756b7ff","value":"ios"}},"right":{"type":"NEQ","key":"attributes.5996ab65c612335fffccdac9","value":"iPad"}},"right":{"type":"NEQ","key":"status","value":"archived"}},"right":{"type":"NEQ","key":"isAutotest","value":"true"}},"right":{"type":"EQ","key":"attributes.55d59a39e4b062a0365c731c","value":"to%20automate"}}

def get_test_cases_by_filter_num(run_filter):
    req_headers = {"TestPalm-Api-Token": TESTPALM_TOKEN, "Content-Type": "application/json"}

    run_filter = json.dumps(run_filter, ensure_ascii=False, separators=(",", ": "))
    print(run_filter)
    url_body = urllib.parse.quote(run_filter)

    url = BASE_URL + url_body

    res = requests.get(url, headers=req_headers, verify=False)

    cases = json.loads(res.text)
    return len(cases)


automated_acceptance_num = get_test_cases_by_filter_num(automatedAcceptance)
allCases_acceptance_num = get_test_cases_by_filter_num(allCasesInAcceptance)
#potentialAutomatedAcceptanceNum = get_test_cases_by_filter_num(potentialAutomatedAcceptance)

automated_acceptance_prc = round(100 * automated_acceptance_num / float(allCases_acceptance_num))

#automatedFromPotentialAcceptancePrc = round(100 * automatedAcceptanceNum / float(potentialAutomatedAcceptanceNum))

print ("Всего кейсов в Acceptance: " + str(allCases_acceptance_num))
#print("Потенциально автоматизируемых в Acceptance: " + str(potentialAutomatedAcceptanceNum))
print("Автоматизировано Acceptance: " + str(automated_acceptance_num))
print("Процент автоматизации Acceptance: " + str(round(automated_acceptance_prc)))
#print("Процент автоматизации относительно потенциально автоматизируемых кейсов Acceptance: " + str(automatedFromPotentialAcceptancePrc))

# вычисляем дату понедельника нынешней недели
print(time.strftime((date.today() - timedelta(date.today().weekday())).isoformat()))

data = [
    {
    "fielddate": time.strftime((date.today() - timedelta(date.today().weekday())).isoformat()),
    # вычисляем дату понедельника нынешней недели
    "all_cases_acceptance":allCases_acceptance_num,
    "automated_acceptance_num": automated_acceptance_num
    }
]

logging.info("import data to stat has started")

json_dumps = json.dumps({'values': data})
r = requests.post(
  'https://upload.stat.yandex-team.ru/_api/report/data',
  headers={'Authorization': 'OAuth {token}'.format(token=OAUTH_TOKEN)},
  data={
  'name': total_automated,
  'scale': 'w',  # указываем, что периодичность отчета 'неделя'
  'json_data': json_dumps,
  }
)
print(r.text)
print(json_dumps)
