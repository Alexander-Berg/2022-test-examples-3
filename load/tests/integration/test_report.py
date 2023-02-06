import requests

basic_url = 'http://localhost:5000'


def test_report_without_chart():
    resp = requests.get('{}/report?job=1822827'.format(basic_url))
    assert resp.status_code == 400
    assert resp.json() == {"error": "Bad Request", "reason": "Sorry, you have forgotten to define chart type"}


def test_report_without_job():
    resp = requests.get('{}/report?chart=quantiles'.format(basic_url))
    assert resp.status_code == 400
    assert resp.json() == {"error": "Bad Request", "reason": "Job id must be given as argument"}


def test_report_unknown_chart():
    resp = requests.get('{}/report?job=2637789&chart=unknown'.format(basic_url))
    assert resp.status_code == 400
    assert resp.json() == {"error": "Bad Request", "reason": "Unknown chart type"}


def test_report_quantiles():
    resp = requests.get('{}/report?chart=quantiles&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert [key for key in resp.json()] == ['data', 'job', 'meta', 'result']
    assert [key for key in resp.json()['data']] == ['cases', 'responses_per_second', 'ts']


def test_report_proto_codes():
    resp = requests.get('{}/report?chart=proto_codes&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert [key for key in resp.json()] == ['data', 'job', 'meta', 'result']
    assert [key for key in resp.json()['data']] == ['cases', 'responses_per_second', 'ts']


def test_report_net_codes():
    resp = requests.get('{}/report?chart=net_codes&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert [key for key in resp.json()] == ['data', 'job', 'meta', 'result']
    assert [key for key in resp.json()['data']] == ['cases', 'responses_per_second', 'ts']


def test_report_instances():
    resp = requests.get('{}/report?chart=instances&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json()
    assert [key for key in resp.json()] == ['data', 'job', 'meta', 'result']
    assert [key for key in resp.json()['data']] == ['responses_per_second', 'threads', 'ts']


def test_report_table_proto_codes():
    resp = requests.get('{}/report?chart=table_proto_codes&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json()


def test_report_table_net_codes():
    resp = requests.get('{}/report?chart=table_net_codes&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json()


def test_report_table_cum_quantiles():
    resp = requests.get('{}/report?chart=table_cum_quantiles&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json()


def test_report_cases():
    resp = requests.get('{}/report?chart=cases&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json()['job'] == 2637789
    assert 'overall' in resp.json()['cases']


def test_report_jobs_for_no_task():
    resp = requests.get('{}/report?chart=jobs'.format(basic_url))
    assert resp.status_code == 400
    assert resp.json() == {"error": "Bad Request", "reason": "Task name must be given as argument"}


def test_report_jobs_for_not_valid_task():
    resp = requests.get('{}/report?chart=jobs&task=unknown-task'.format(basic_url))
    assert resp.status_code == 400
    assert resp.json() == {"error": "Bad Request", "reason": "Task name unknown-task is not valid ticket name"}


def test_report_jobs_for_valid_task():
    resp = requests.get('{}/report?chart=jobs&task=load-318'.format(basic_url))
    assert resp.status_code == 200
    assert [key for key in resp.json()] == ['jobs', 'task']
    assert resp.json()['task'] == 'LOAD-318'


def test_report_job_metadata_for_valid_job():
    resp = requests.get('{}/report?chart=job_meta&job=2637789'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json()['job'] == 2637789
    assert [key for key in resp.json()['meta']] == sorted([
        'ammo_path', 'artefacts', 'component_code', 'component_name', 'configinitial', 'dsc', 'duration', 'end',
        'imbalance', 'load_schedule', 'loop_cnt', 'monitoring', 'name', 'quit_status_code', 'quit_status_text',
        'start', 'tank', 'target', 'task', 'ver'
    ])


def test_report_table_jobs():
    resp = requests.get('{}/report?chart=table_jobs'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json() == {}


def test_report_table_tasks():
    resp = requests.get('{}/report?chart=table_tasks'.format(basic_url))
    assert resp.status_code == 200
    assert [key for key in resp.json()] == sorted(['tasks', 'user'])


def test_report_table_ammo():
    resp = requests.get('{}/report?chart=table_ammo'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json() == {}


def test_report_table_scenarios():
    resp = requests.get('{}/report?chart=table_scenarios'.format(basic_url))
    assert resp.status_code == 200
    assert resp.json() == {}


def test_report_current_user():
    resp = requests.get('{}/report?chart=current_user'.format(basic_url))
    assert resp.status_code == 200
    assert [key for key in resp.json()] == sorted(['author', 'is_dismissed', 'is_robot', 'login', 'uid'])
