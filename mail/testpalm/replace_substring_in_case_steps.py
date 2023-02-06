# Example: python3 replace_substring_in_case_steps.py --token AQAD-*** --project mobilemail --action replace --old_string 'from_string' --new_string 'to_string'

import argparse
import os
from tp_api_client.tp_api_client import TestPalmClient
import logging

OAUTH_TOKEN_TESTPALM = os.environ['OAUTH_TOKEN_TESTPALM']

logger = logging.getLogger('testpalm_logger')
logging.basicConfig(format=u'%(asctime)s [%(levelname)s] %(module)s: %(message)s', level=logging.INFO)


def handle_options():
    parser = argparse.ArgumentParser()
    parser.add_argument("-t", "--token", dest="token", default=OAUTH_TOKEN_TESTPALM)
    parser.add_argument("-p", "--project", dest="project")
    parser.add_argument("-a", "--action", dest="action")
    parser.add_argument("-from", "--old_string", dest="old_string")
    parser.add_argument("-to", "--new_string", dest="new_string")
    return parser


def bold(text: str):
    return f'\033[1m{text}\033[0m'


if __name__ == '__main__':
    args = handle_options().parse_args()
    token, project = args.token, args.project
    action = args.action
    old_string, new_string = args.old_string, args.new_string

    tp_client = TestPalmClient(auth=token)
    cases = tp_client.get_testcases(project=project, include='id,stepsExpects', searchQuery=old_string)
    logger.info(f'Найдено {len(cases)} кейсов')

    def delete_steps_with_string(case):
        case['stepsExpects'] = [stepExpect for stepExpect in case['stepsExpects'] if
                                old_string not in stepExpect['step'] and 'expect' in stepExpect and old_string not in stepExpect['expect']]
        input_continue = input(
            f'\nВ кейсе {bold("{}-{}".format(project, case["id"]))}\n'
            f'Будут удалены шаги с текстом "{old_string}"\n\n'
            f'Нажмите Enter, чтобы изменить кейс, или любой символ, чтобы продолжить без изменений\n\n'
        )
        if input_continue == '':
            tp_client.update_testcase(project=project, data=case)

    def replace_steps_with_string(case):
        for step in case['stepsExpects']:
            step.pop('expectFormatted', None)
            step.pop('stepFormatted', None)
            for key, value in step.items():
                if value is not None and old_string in value:
                    step[key] = value.replace(old_string, new_string)
                    input_continue = input(
                        f'\nВ кейсе {bold("{}-{}".format(project, case["id"]))}\n'
                        f'Значение поля {bold(key)} будет изменено\n'
                        f'Предыдущее значение: \n{bold(value)}\n'
                        f'Новое значение: \n{bold(step[key])}\n\n'
                        f'Нажмите Enter, чтобы изменить кейс, или любой символ, чтобы продолжить без изменений\n\n'
                    )
                    if input_continue == '':
                        tp_client.update_testcase(project=project, data=case)
                    else:
                        continue

    for case in cases:
        if action == 'delete':
            delete_steps_with_string(case)
        elif action == 'replace':
            replace_steps_with_string(case)
        else:
            logger.error('Unknown action. Possible actions: "delete", "replace"')
