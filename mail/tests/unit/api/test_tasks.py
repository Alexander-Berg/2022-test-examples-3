import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries

from mail.ipa.ipa.core.actions.tasks import GetTasksAction
from mail.ipa.ipa.core.entities.enums import TaskState, TaskType
from mail.ipa.ipa.core.entities.task import Task, TasksPage


@pytest.fixture
def now(make_now):
    return make_now()


@pytest.fixture
def tasks(org_id, now):
    return [
        Task(task_type=TaskType.PARSE_CSV,
             task_id=123,
             meta_info={'name': 'csv-name'},
             entity_id=org_id,
             state=TaskState.FINISHED,
             created=now,
             nonterminal_children=0,
             failed_children=0,
             ),
        Task(task_type=TaskType.INIT_IMPORT,
             task_id=456,
             meta_info={'count': 10},
             entity_id=org_id,
             state=TaskState.FINISHED,
             created=now,
             nonterminal_children=5,
             failed_children=0,
             ),
        Task(task_type=TaskType.INIT_IMPORT,
             task_id=789,
             meta_info={'count': 11},
             entity_id=org_id,
             state=TaskState.FINISHED,
             created=now,
             nonterminal_children=0,
             failed_children=2,
             ),
    ]


@pytest.fixture(autouse=True)
def mock_get_tasks(mock_action, tasks):
    return mock_action(GetTasksAction, TasksPage(tasks=tasks, total=100, offset=5, limit=10))


@pytest.fixture
async def response(app, org_id, request_params):
    return await app.get(f'/import/{org_id}/tasks/',
                         params=request_params)


@pytest.fixture
async def response_json(response):
    return await response.json()


class TestGetTasksSuccess:
    @pytest.fixture
    def request_params(self):
        return {
            'limit': 10,
            'offset': 5,
        }

    def test_status(self, response):
        assert_that(response.status, equal_to(200))

    def test_returns_tasks(self, response_json, now):
        assert_that(
            response_json,
            has_entries({
                'code': 200,
                'status': 'success',
                'data': has_entries({
                    'total': 100,
                    'tasks': contains_inanyorder(
                        has_entries({
                            'task_id': '123',
                            'type': 'csv',
                            'created_at': now.isoformat(),
                            'finished': True,
                            'has_errors': False,
                            'params': {
                                'name': 'csv-name',
                            }
                        }),
                        has_entries({
                            'task_id': '456',
                            'type': 'json',
                            'created_at': now.isoformat(),
                            'finished': False,
                            'has_errors': False,
                            'params': {
                                'count': 10,
                            }
                        }),
                        has_entries({
                            'task_id': '789',
                            'type': 'json',
                            'created_at': now.isoformat(),
                            'finished': True,
                            'has_errors': True,
                            'params': {
                                'count': 11,
                            }
                        })
                    )
                })
            })
        )

    def test_calls_action(self, response, mock_get_tasks, org_id, request_params):
        mock_get_tasks.assert_called_once_with(org_id=org_id,
                                               limit=request_params['limit'],
                                               offset=request_params['offset'])


class TestGetTasksSuccessNoParams:
    @pytest.fixture
    def request_params(self):
        return {}

    def test_calls_action(self, response, mock_get_tasks, org_id, ipa_settings):
        mock_get_tasks.assert_called_once_with(org_id=org_id,
                                               limit=ipa_settings.TASKS_API_DEFAULT_LIMIT,
                                               offset=0)


class TestGetTasksInvalidParams:
    @pytest.fixture
    def request_params(self, ipa_settings):
        return {'limit': ipa_settings.TASKS_API_MAX_LIMIT + 1}

    def test_status(self, response):
        assert_that(response.status, equal_to(400))

    def test_json(self, response_json):
        assert_that(
            response_json,
            has_entries({
                'code': 400,
                'status': 'fail',
            })
        )
