# -*- coding: utf-8 -*-

import json
import os
import time
import urllib

import requests
import yaml

scriptPath = os.path.dirname(os.path.abspath(__file__))

yaconfig = yaml.load(open(os.path.dirname(scriptPath) + "/config.yaml"))
projects = yaml.load(open(os.path.dirname(scriptPath) + "/autotestCoverageStat/reportConf.yaml"))

project_settings = {
    'mail-liza': {
        "CASETYPES": {
            "ОФ": {"Основная функциональность": "attributes.595fa454c6123372653f4520"},
            "Тестплан": {"Тестплан": "attributes.54002c9de4b0fc51d80071f9"},
            "Смоук": {"Smoke": "attributes.54002c9de4b0fc51d80071f9"},
            "Всего": {"Всего": ""}
        },
        "CANDIDATE": (
            "Кандидат на автоматизацию", "attributes.5c5453a66acd3d903a10fba9"
        )
    },
    'mail-touch': {
        "CASETYPES": {
            "ОФ": {"Основная функциональность": "attributes.5a60d6d6d108d83ca90eb3fd"},
            "Тестплан": {"Тестплан_асессоры": "attributes.56796d50c612332a9dc04d5e"},
            "Смоук": {"Smoke": "attributes.56796d50c612332a9dc04d5e"},
            "Всего": {"Всего": ""}
        },
        "CANDIDATE": (
            "Кандидат в автоматизацию", "attributes.58eba70b88955049314bf7d4"
        )
    },
    'cal': {
        "CASETYPES": {
            "ОФ": {"Основная функциональность": "attributes.5c9a45b6ee4f430c58836307"},
            "Тестплан": {"Тестплан": "attributes.57f379898895502c9b12bd4b"},
            "Смоук": {"Смоук": "attributes.57f379898895502c9b12bd4b"},
            "Всего": {"Всего": ""}
        },
        "CANDIDATE": (
            "Кандидат на автоматизацию", "attributes.5c9cc8d07c48e33b4a624932"
        )
    },
    'mail-hostroot': {
        "CASETYPES": {
            "ОФ": {"Основная функциональность": "attributes.5c9a44f2bb9745c9c2635ee4"},
            "Тестплан": {"Тестплан": "attributes.567826d9c612330e292e4e48"},
            "Всего": {"Всего": ""}
        },
        "CANDIDATE": (
            "Кандидат на автоматизацию", "attributes.5c9a4567bb9745c9c2635f2b"
        )
    },
}


def get_automated(req_value, req_key):
    request = {
        "type": "AND",
        "left": {
            "type": "AND",
            "left": {
                "type": "EQ",
                "key": req_key,
                "value": req_value
            },
            "right": {
                "type": "OR",
                "left": {
                    "type": "OR",
                    "left": {
                        "type": "OR",
                        "left": {
                            "type": "OR",
                            "left": {
                                "type": "EQ",
                                "key": "status",
                                "value": "on review"
                            },
                            "right": {
                                "type": "EQ",
                                "key": "status",
                                "value": "actual"
                            }
                        },
                        "right": {
                            "type": "EQ",
                            "key": "status",
                            "value": "needs changes"
                        }
                    },
                    "right": {
                        "type": "EQ",
                        "key": "status",
                        "value": "automation in progress"
                    }
                },
                "right": {
                    "type": "EQ",
                    "key": "status",
                    "value": "automated"
                }
            }
        },
        "right": {
            "type": "EQ",
            "key": "isAutotest",
            "value": "true"
        }
    }
    return request


def get_all_actual(req_value, req_key):
    request = {
        "type": "AND",
        "left": {
            "type": "EQ",
            "key": req_key,
            "value": req_value
        },
        "right": {
            "type": "OR",
            "left": {
                "type": "OR",
                "left": {
                    "type": "OR",
                    "left": {
                        "type": "OR",
                        "left": {
                            "type": "OR",
                            "left": {
                                "type": "EQ",
                                "key": "status",
                                "value": "on review"
                            },
                            "right": {
                                "type": "EQ",
                                "key": "status",
                                "value": "actual"
                            }
                        },
                        "right": {
                            "type": "EQ",
                            "key": "status",
                            "value": "needs changes"
                        }
                    },
                    "right": {
                        "type": "EQ",
                        "key": "status",
                        "value": "automation in progress"
                    }
                },
                "right": {
                    "type": "EQ",
                    "key": "status",
                    "value": "automated"
                }
            },
            "right": {
                "type": "EQ",
                "key": "status",
                "value": "needs repair"
            }
        }
    }
    return request


def get_all_candidates(req_value, req_key, candidate_value, candidate_key):
    request = {
        "type": "AND",
        "left": {
            "type": "AND",
            "left": {
                "type": "EQ",
                "key": req_key,
                "value": req_value
            },
            "right": {
                "type": "EQ",
                "key": candidate_key,
                "value": candidate_value
            }
        },
        "right": {
            "type": "OR",
            "left": {
                "type": "OR",
                "left": {
                    "type": "OR",
                    "left": {
                        "type": "OR",
                        "left": {
                            "type": "EQ",
                            "key": "status",
                            "value": "on review"
                        },
                        "right": {
                            "type": "EQ",
                            "key": "status",
                            "value": "actual"
                        }
                    },
                    "right": {
                        "type": "EQ",
                        "key": "status",
                        "value": "needs changes"
                    }
                },
                "right": {
                    "type": "EQ",
                    "key": "status",
                    "value": "automation in progress"
                }
            },
            "right": {
                "type": "EQ",
                "key": "status",
                "value": "automated"
            }
        }
    }
    return request


def getTestCasesByFilterNum(project_name, runFilter):
    reqHeaders = {"TestPalm-Api-Token": yaconfig["AUTH_TP"], "Content-Type": "application/json"}

    runFilter = json.dumps(runFilter, ensure_ascii=False, separators=(",", ": "))
    # print(runFilter)
    urlBody = urllib.quote(runFilter)

    url = "https://testpalm-api.yandex-team.ru:443/testcases/%s?include=id&expression=%s" % (project_name, urlBody)

    res = requests.get(url, headers=reqHeaders)

    cases = json.loads(res.text)
    return len(cases)


def get_cases_stat(project_name):
    print project_name
    casetypes = project_settings.get(project_name).get('CASETYPES')
    candidates = project_settings.get(project_name).get('CANDIDATE')
    for case_type in casetypes:
        casetype = casetypes.get(case_type)
        for tag_name, attribute in casetype.items():
            print tag_name, attribute
            actual = getTestCasesByFilterNum(project_name, get_all_actual(tag_name, attribute))
            auto = getTestCasesByFilterNum(project_name, get_automated(tag_name, attribute))
            candidate = getTestCasesByFilterNum(project_name,
                                                get_all_candidates(tag_name, attribute, candidates[0], candidates[1]))

            send_to_stat(project_name, case_type, actual, auto, candidate)


def send_to_stat(project_name, casetype, actual, auto, candidates):
    data = [
        {
            "fielddate": time.strftime("%Y-%m-01"),
            "project": project_name,
            "casetype": casetype,
            "all": actual,
            "automate": auto,
            "maybe": candidates
        }
    ]
    print data

    attempt_number = 1
    while attempt_number < 5:
        r = requests.post(
            'https://upload.stat.yandex-team.ru/_api/report/data',
            headers={'Authorization': 'OAuth %s' % yaconfig["AUTH_STAT"]},
            data={
                'name': 'Mail/Others/AutomationCases',
                'scale': 'm',
                'data': json.dumps({'values': data}),
            },
        )
        print r.status_code
        if r.status_code == 200:
            break
        attempt_number += 1


if __name__ == '__main__':
    print(time.strftime("%Y-%m-%d"))
    for project in project_settings.keys():
        get_cases_stat(project)
