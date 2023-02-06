from __future__ import unicode_literals
from datetime import datetime
from tp_api_client import tp_api_client


def output_in_console(string):
    print("{date}   {string}".format(date=datetime.now(), string=string))


def split(case_ids, run_size):
    cases = [case_ids[d:d + run_size] for d in range(0, len(case_ids), run_size)]

    if len(cases[-1]) < run_size / 2:
        cases[-2] = cases[-2] + cases[-1]
        cases.remove(cases[-1])
    return cases


def prepare_testrun_data(ids_for_test_run, count, testing_data, assessor_ticket=None):
    theme, part = '', ''

    if testing_data['themes']:
        theme = testing_data['themes'][count % len(testing_data['themes'])]

    if testing_data['part']:
        part = f'part{testing_data["part"]}'

    env = testing_data['environments'][count % len(testing_data['environments'])]
    version = testing_data['version_app'].replace('.', '')

    data = {
        "title": f"[{env}] Business logic part {count} [{theme}]",
        "version": f"Assessors{testing_data['platform']}{version}{part}",
        "environments": [{
            "title": env,
            "description": env,
            "default": False
        }],
        "testGroups": [{
            "path": [],
            "testCases": [],
            "defaultOrder": True
        }],
        "runnerConfig": None,
        "status": "CREATED"
    }

    if assessor_ticket:
        data["parentIssue"] = {
            "trackerId": "Startrek",
            "groupId":  assessor_ticket.split('-')[0],
            "id": assessor_ticket
        }

    for case_id in ids_for_test_run:
        data["testGroups"][0]["testCases"].append({
            "testCase": {
                "id": case_id,
                "status": "ACTUAL"
            },
            "status": "CREATED"
        })
    return data


def get_and_run(testing_data, suites, assessor_ticket, add_uuid_case=False):
    client = tp_api_client.TestPalmClient(auth=testing_data['AUTH_TP'])
    suite_id = suites[testing_data['scenario']]

    cases = client.get_testcases_from_suite(project=testing_data['testpalm_project'],
                                            suite_id=suite_id,
                                            include='id,attributes')
    definitions = client.get_project_definitions(project=testing_data['testpalm_project'])
    definition_id = next((definition['id'] for definition in definitions if definition['title'] == f'Feature {testing_data["platform"]}'), None)
    case_ids = list(
        map(lambda x: x['id'],
            sorted(cases, key=lambda x: x['attributes'][definition_id][0])
            if definition_id is not None else cases))

    ids_for_test_runs = split(case_ids, testing_data['run_size'])
    output_in_console(ids_for_test_runs)

    part = 1

    for ids_for_test_run in ids_for_test_runs:
        if add_uuid_case and not ids_for_test_run.count(testing_data['testpalm_uuid_case']):
            ids_for_test_run.append(testing_data['testpalm_uuid_case'])
        data = prepare_testrun_data(ids_for_test_run, part, testing_data, assessor_ticket)
        client.create_testrun_from_cases(project=testing_data['testpalm_project'], data=data)
        part += 1
