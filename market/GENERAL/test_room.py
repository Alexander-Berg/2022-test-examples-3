import os
import json
import flask
import requests

from flask import request

from lib.views import test_run_timeline
from lib.views import test_run_timeline_selection
from lib.util import timeutil

app = flask.Blueprint('test_room', __name__)


def handle_server_error(func):
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except Exception as e:
            body = json.dumps({
                'error': type(e).__name__,
                'message': str(e)
            })
            return flask.Response(body, status=500, content_type='application/json; charset=utf-8')
    return wrapper


@app.route('/api/v1/test-room/timeline')
@handle_server_error
def handle_test_room_timeline():
    current_time = timeutil.current_millis()
    test_run_id = request.args.get('test-run-id')
    if not test_run_id:
        return flask.Response(
            {'message': 'Parameter \'test-run-id\' is missing'},
            400,
            content_type='application/json; charset=utf-8'
        )

    token = os.environ['TEST_PALM_TOKEN']
    test_run = json.loads(requests.get(
        'https://testpalm-api.yandex-team.ru/testrun/bluemarketapps/%s' % test_run_id,
        headers={'Authorization': 'OAuth %s' % token}
    ).content)

    title = test_run['title']
    participants = []
    test_cases = []
    for test_group in test_run['testGroups']:
        path = filter(
            lambda part: part,
            test_group['path']
        )
        group_name = '. '.join(path)
        for test_case in test_group['testCases']:
            status = test_case['status']
            if status == 'CREATED':
                continue
            started_time = test_case['startedTime']
            finished_time = test_case['finishedTime']
            if not started_time:
                continue
            if not finished_time:
                finished_time = current_time
            assignee = test_case['startedBy']
            if assignee in participants:
                y = participants.index(assignee)
            else:
                y = len(participants)
                participants.append(assignee)
            case_name = test_case['testCase']['name']
            test_cases.append({
                'x': started_time,
                'x2': finished_time,
                'y': y,
                'name': '%s: %s' % (group_name, case_name)
            })

    body = json.dumps({
        'categories': participants,
        'series': [{
            'name': title,
            'data': test_cases
        }]
    })
    return flask.Response(body, status=200, content_type='application/json; charset=utf-8')


@app.route('/test-room/timeline')
def view_test_room_timeline_selection():
    return test_run_timeline_selection.render_test_run_timeline_selection_page()


@app.route('/test-room/timeline/<test_run_id>')
def view_test_room_timeline(test_run_id):
    return test_run_timeline.render_test_run_timeline_page(test_run_id)
