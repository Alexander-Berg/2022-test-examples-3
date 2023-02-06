# -*- coding: utf-8 -*-

import json
import time
import urllib

import requests
import yaml
import os

scriptPath = os.path.dirname(os.path.abspath(__file__))

automated = {
	"type": "AND",
	"left": {
		"type": "AND",
		"left": {
			"type": "EQ",
			"key": "attributes.595fa454c6123372653f4520",
			"value": "Основная функциональность"
		},
		"right": {
			"type": "EQ",
			"key": "isAutotest",
			"value": "true"
		}
	},
	"right": {
		"type": "EQ",
		"key": "status",
		"value": "actual"
	}
}

allCasesInMainFunct = {
	"type": "AND",
	"left": {

		"type": "EQ",
		"key": "attributes.595fa454c6123372653f4520",
		"value": "Основная функциональность"

	},
	"right": {
		"type": "EQ",
		"key": "status",
		"value": "actual"
	}
}

allActualCases = {
	"type": "EQ",
	"key": "status",
	"value": "actual"
}

reportConf = yaml.load(open("%s/reportConf.yaml" % scriptPath))
yaconfig = yaml.load(open("%s/config.yaml" % scriptPath))


def getTestCasesByFilterNum(runFilter):
	reqHeaders = {"TestPalm-Api-Token": yaconfig["AUTH_TP"], "Content-Type": "application/json"}

	runFilter = json.dumps(runFilter, ensure_ascii=False, separators=(",", ": "))
	print(runFilter)
	urlBody = urllib.quote(runFilter)

	url = "https://testpalm-api.yandex-team.ru:443/testcases/mail-liza?include=id&expression=" + urlBody

	res = requests.get(url, headers=reqHeaders)

	cases = json.loads(res.text)
	return len(cases)


allActualCasesNum = getTestCasesByFilterNum(allActualCases)
automatedCasesNum = getTestCasesByFilterNum(automated)
allCasesNum = getTestCasesByFilterNum(allCasesInMainFunct)

automatedCasesPrc = automatedCasesNum / float(allCasesNum)
print("Total number of actual cases: " + str(allActualCasesNum))
print("Number of cases in main funct: " + str(allCasesNum))
print("Automated: " + str(automatedCasesNum))
print("Percent of automated cases: " + str(automatedCasesPrc))

print(time.strftime("%Y-%m-01"))
data = [
	{
		"fielddate": time.strftime("%Y-%m-01"), "percent": automatedCasesPrc * 100
	}
]

r = requests.post(
	'https://upload.stat.yandex-team.ru/_api/report/data',
	headers={'StatRobotUser': 'robot_a-zoshchuk', 'StatRobotPassword': 'ehoo0mA22lEpsid'},
	data={
		'name': 'Mail/Others/automatedCases',
		'scale': 'm',
		'data': json.dumps({'values': data}),
	},
)

data = [
	{
		"fielddate": time.strftime("%Y-%m-01"),
		"allcases": allCasesNum,
		"automatedcases": automatedCasesNum,
		"allactualcases": allActualCasesNum
	}
]

r = requests.post(
	'https://upload.stat.yandex-team.ru/_api/report/data',
	headers={'StatRobotUser': 'robot_a-zoshchuk', 'StatRobotPassword': 'ehoo0mA22lEpsid'},
	data={
		'name': 'Mail/Others/MainFunctCasesNum',
		'scale': 'm',
		'data': json.dumps({'values': data}),
	},
)
