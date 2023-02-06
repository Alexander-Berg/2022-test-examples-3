from retrying import retry

from TestPersQueuesScripts.autotestsStatistics.testpalm import TestPalm
from set_secret import set_secret
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

FILTER_TRUSTED_CASES = {"type": "EQ", "key": "", "value": ""}
FILTER_AUTOMATED = {"type": "EQ", "key": "isAutotest", "value": "true"}

tag_automate_map = {
    'mail-liza': 'Автоматизация',
    'mail-touch': 'Автоматизация',
    'cal': 'Автоматизация',
    'mobilemail': 'Autotest',
}


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def mark_cases_in_testpalm(project, cases_in_project):
    ids_for_tag = [
        {
            'id': get_trusted(cases_in_project),
            'tag': 'Trusted'
        },
        {
            'id': get_unrunned(project, cases_in_project) - get_unmarked(project, cases_in_project),
            'tag': 'Не запускался долгое время'
        },
        {
            'id': get_unmarked(project, cases_in_project),
            'tag': 'Возможно нет вилки'
        }
    ]

    for id_for_tag in ids_for_tag:
        ids = id_for_tag['id']

        set_secret.set_secrets()
        ids = list(set(ids))
        print(f'ids to change:\n {ids}')
        print(f'ids to change:\n {len(ids)}')
        testpalm = TestPalm(project)
        FILTER_TRUSTED_CASES['key'] = testpalm.get_attribute_key(tag_automate_map[project])
        FILTER_TRUSTED_CASES['value'] = id_for_tag['tag']
        ids_to_remove = [x['id'] for x in testpalm.request_testpalm_cases_by_filter(FILTER_TRUSTED_CASES)]
        for id in chunks(ids_to_remove, 300):
            testpalm.change_tag_in_case_attribute(
                id,
                tag_automate_map[project],
                id_for_tag['tag'],
                0
            )
        for id in chunks(ids, 300):
            testpalm.change_tag_in_case_attribute(
                id,
                tag_automate_map[project],
                id_for_tag['tag'],
                1
            )


def get_unrunned(project, cases_in_project):
    testpalm = TestPalm(project)
    cases_in_testpalm = testpalm.request_testpalm_cases_by_filter(FILTER_AUTOMATED)
    cases_in_testpalm = set([case['id'] for case in cases_in_testpalm])
    cases_in_run_autotests = set([int(case['id']) for case in cases_in_project])
    diff = cases_in_testpalm - cases_in_run_autotests
    return diff


def get_unmarked(project, cases_in_project):
    testpalm = TestPalm(project)
    cases_in_testpalm = testpalm.request_testpalm_cases_by_filter(FILTER_AUTOMATED)
    cases_in_testpalm = set([case['id'] for case in cases_in_testpalm])
    cases_in_run_autotests = set([int(case['id']) for case in cases_in_project])
    diff = cases_in_run_autotests - cases_in_testpalm
    return diff


def chunks(lst, n):
    """Yield successive n-sized chunks from lst."""
    for i in range(0, len(lst), n):
        yield lst[i:i + n]


def get_trusted(cases, max_num=200):
    envs = {}
    stability_map = {
        'android': 0.98,
        'ios': 0.98
    }
    cases_full = list([case for case in cases if (
            (case['total_stability'] >= stability_map.get(case['project'], 0.97)) & (
                (case['is_passed'] + case['is_finally_failed'] > 5) | (case.get('is_started', 0) > 5))
    )])
    cases_full.sort(key=lambda case: (case['total_stability'], int(case['id'])), reverse=True)
    trusted = []
    for case in cases_full:
        if case['project'] not in envs:
            envs[case['project']] = 0
    for case in cases_full:
        if envs[case['project']] < max_num:
            trusted.append(case['id'])
            envs[case['project']] += 1
    return trusted
