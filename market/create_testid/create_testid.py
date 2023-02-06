import copy
import datetime
import json
import requests
import time
from abt_admin_experiment_client import create_abt_admin_client
import nirvana.job_context as nv


TESTID_MARKET_CONFIG = {
    'type': 'ABT',
    'replace_token': '<testid>',
    'params': [{
        'HANDLER': 'MARKET',
        'CONTEXT': {
            'MARKET': {
                'testid': ['<testid>']
            }
        }
    }],
}


def create_testid(rearr, params):
    abt_admin_client = create_abt_admin_client(
        params['abt_oauth_token'],
        use_test_api=False,
    )
    today = datetime.date.today().strftime('%m/%d/%y')
    testid_config = copy.deepcopy(TESTID_MARKET_CONFIG)
    testid_config['title'] = 'Market Search Algorithm Pipeline. Date={}'.format(today)
    testid_config['params'][0]['CONTEXT']['MARKET']['rearr'] = rearr
    testid_config['queue_id'] = 7
    testid_config['testid'] = {
        'id': abt_admin_client.create_testid(testid_config),
    }
    return testid_config


def are_valid(
    test_id,
    max_checks=30,
    sleep_between_checks=60,
    sleep_time_in_the_end=600,
):
    print('Check whether testid is valid.')
    ab_url = 'https://ab.yandex-team.ru/api/testid/activity/current?id={}&tag=testing&config_id=0'
    i_check = 0
    valid = False
    while not valid and i_check < max_checks:
        check_url = ab_url.format(test_id)
        check_data = requests.get(check_url, verify=False, allow_redirects=True)
        valid = test_id in check_data.content
        i_check += 1
        time.sleep(sleep_between_checks)
    time.sleep(sleep_time_in_the_end)
    return valid


def main():
    ctx = nv.context()
    inputs = ctx.get_inputs()
    outputs = ctx.get_outputs()
    params = ctx.get_parameters()
    with open(inputs.get('config'), 'r') as input_file:
        rearr = json.load(input_file)
    output_config = create_testid(rearr, params)
    if not are_valid(str(output_config['testid']['id'])):
        raise ValueError('Testid is not valid.')
    print('Testid is valid')
    with open(outputs.get('config'), 'w') as output:
        json.dump(output_config, output)


if __name__ == '__main__':
    main()
