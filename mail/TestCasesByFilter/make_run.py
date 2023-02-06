# -*- coding: utf-8 -*-
import json
import requests
import yaml
import os

runObject = {
	"lastModifiedTime": 1505987276890,
	"parentIssue": {
		"isResolved": False,
		"isBug": True,
		"createdTime": 0,
		"assignee": [],
		"trackerId": "Startrek",
		"groupId": "MailExp",
		"id": "taskKey"
	},
	"environments": [{
		"title": "PC",
		"description": "PC",
		"default": False
	}],
	"title": "",
	"resolution": {
		"counter": {
			"passed": 0,
			"failed": 0,
			"skipped": 0,
			"broken": 0,
			"created": 97,
			"started": 0,
			"total": 97
		},
		"issues": []
	},
	"duration": 0,
	"ignoreSuiteOrder": True,
	"currentEnvironment": {
		"title": "PC",
		"description": "PC",
		"default": False
	},
	"internalResolveAllowed": False,
	"estimate": {
		"avgDuration": 47514,
		"maxDuration": 13904330,
		"minDuration": 6667,
		"testcasesWithRuns": 2,
		"testcasesWithoutRuns": 95
	},
	"createdTime": 1505987276890,
	#    "modifiedBy": "a-zoshchuk",
	#    "participants": ["a-zoshchuk"],
	"launcherInfo": {
		"external": False,
		"internalExecAllowed": False
	},
	"startedTime": 0,
	"testGroups": [{
		"testCases": [
			#     {
			#     "uuid": "2699b8fb-9d87-4719-b5d2-26dfd3d7fee6",
			#     "testCase": {
			#         "id": 2284,
			#         "name": "[Места отображения] Проверить верстку иконки у писем-напоминаний",
			#         "isAutotest": True
			#     },
			#     "status": "CREATED",
			#     "startedTime": 0,
			#     "finishedTime": 0,
			#     "duration": 0,
			#     "parametersList": []
			# }
		],
		"path": [],
		"defaultOrder": True
	}],
	"tags": [],
	"executionTime": 0,
	#    "createdBy": "a-zoshchuk",
	"finishedTime": 0,
	"assignee": [],
	"properties": [],
	"status": "CREATED",
	"version": "",
}

scriptPath = os.path.dirname(os.path.abspath(__file__) + '/SupportQueueMonitoring')
yaconfig = yaml.load(open(os.path.dirname(scriptPath) + "/config.yaml"))


def make_run(cases_for_run, num_of_runs, version_name, task_key, browser):
	global runObject
	runObject["testGroups"][0]["testCases"] = []
	k = 0
	for case in cases_for_run:
		runObject["testGroups"][0]["testCases"].append({"testCase": {"id": case}, "status": "CREATED"})
		k += 1

	runObject["parentIssue"]["id"] = task_key
	runObject["version"] = version_name + " Асессоры"
	runObject["title"] = version_name + " Асессоры " + str(num_of_runs) + ' ' + browser
	runObject["environments"][0]["title"] = browser
	runObject["environments"][0]["description"] = browser
	runObject["currentEnvironment"]["title"] = browser
	runObject["currentEnvironment"]["description"] = browser
	run_object_to_send = json.dumps(runObject, separators=(",", ": "))
	run_object_to_send = run_object_to_send.encode('utf=8')
	print(runObject)

	headers = {"TestPalm-Api-Token": yaconfig["AUTH_TP"], "Content-Type": "application/json"}
	r = requests.post("https://testpalm.yandex-team.ru:443/api/testrun/mail-liza/import", headers=headers,
					  data=run_object_to_send)
	print(r.text)
