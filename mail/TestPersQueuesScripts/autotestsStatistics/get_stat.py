import json
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent.parent.parent))
from TestPersQueuesScripts.autotestsStatistics.mark_trusted_cases import mark_cases_in_testpalm
from TestPersQueuesScripts.autotestsStatistics.send_to_stat import get_data_from_stat_for_month
from set_secret import set_secret
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


project_map = {
    'mail-liza': ['liza'],
    'mail-touch': ['touch'],
    'cal': ['cal'],
    'mobilemail': ['android', 'ios'],
}

if __name__ == '__main__':
    set_secret.set_secrets()
    data = get_data_from_stat_for_month()
    for project in project_map:
        case_info = {}
        case_info_with_exec_time = []
        data_project = list([id_stat for id_stat in data if id_stat['project'] in project_map[project]])
        for result in data_project:
            if int(result.get('is_prod', '0')) != 1:
                continue
            if result['execution_time']:
                execution_time = result['execution_time']
                num_of_runned = 1
            else:
                execution_time = 0
                num_of_runned = 0
            if result['id'] not in case_info:
                case_info[result['id']] = {
                    'is_passed': result['is_passed'],
                    'is_failed': result['is_failed'],
                    'is_skipped': result['is_skipped'],
                    'is_finally_failed': result['is_finally_failed'],
                    'is_intermediate': result['is_intermediate'],
                    'execution_time_sum': execution_time,
                    'num_of_days': num_of_runned,
                    'project': result['project'],
                    'is_prod': result['is_prod']
                }
                if 'is_started' in result:
                    case_info[result['id']]['is_started'] = result['is_started']
            else:
                case_info[result['id']]['is_passed'] += result['is_passed']
                case_info[result['id']]['is_failed'] += result['is_failed']
                case_info[result['id']]['is_skipped'] += result['is_skipped']
                case_info[result['id']]['is_finally_failed'] += result['is_finally_failed']
                case_info[result['id']]['is_intermediate'] += result['is_intermediate']
                case_info[result['id']]['execution_time_sum'] += execution_time
                case_info[result['id']]['num_of_days'] += num_of_runned
                case_info[result['id']]['project'] = result['project']
                case_info[result['id']]['is_prod'] = result['is_prod']
                if 'is_started' in result:
                    case_info[result['id']]['is_started'] += result['is_started']
        for id in case_info:
            record_to_add = {
                'id': id,
                'is_passed': case_info[id]['is_passed'],
                'is_failed': case_info[id]['is_failed'],
                'is_skipped': case_info[id]['is_skipped'],
                'is_finally_failed': case_info[id]['is_finally_failed'],
                'is_intermediate': case_info[id]['is_intermediate'],
                'execution_time': case_info[id]['execution_time_sum'] / case_info[id]['num_of_days'] if case_info[id][
                    'num_of_days'] else 0,
                'project': case_info[id]['project'],
                'is_prod': case_info[id]['is_prod'],
            }
            if 'is_started' in case_info[id]:
                record_to_add['is_started'] = case_info[id]['is_started']
            case_info_with_exec_time.append(record_to_add)
        for id in case_info_with_exec_time:
            if id['is_passed'] + id['is_finally_failed'] + id.get('is_started', 0) > 0:
                if ('is_started' in id) & (id.get('is_started', 0) > 0):
                    id['total_stability'] = id['is_passed'] / (id['is_passed'] + max(id['is_started'] - id['is_passed'], id['is_finally_failed']))
                else:
                    id['total_stability'] = id['is_passed'] / (id['is_passed'] + id['is_finally_failed'])
            else:
                id['total_stability'] = 0
        with open(f'{project}-total.json', 'w', encoding='utf-8') as f:
            json.dump(case_info_with_exec_time, f, ensure_ascii=False, indent=4)

    for project in project_map:
        with open(f'{project}-total.json', 'r', encoding='utf-8') as f:
            cases_in_project = json.load(f)
        print(project)
        print(len(cases_in_project))
        mark_cases_in_testpalm(project, cases_in_project)
