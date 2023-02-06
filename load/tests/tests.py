
from load.projects.firestarter.src.statuses import InternalStatus
from load.projects.firestarter.src.tasks import Task, TaskRecord
from load.projects.firestarter.src import external_calls
from load.projects.firestarter.src.external_calls import call_validator, call_tank_finder
from load.projects.firestarter.src.utils import get_target_from_config, get_tanks_from_config
import pytest
import json
import redis

test_db = redis.StrictRedis(host='localhost', port=6379, db=8)


def return_nothing(*args, **kwargs):
    return


@pytest.fixture
def patch_db(monkeypatch):
    monkeypatch.setattr(TaskRecord, 'create', return_nothing)


class TestExternalCall:

    validator_input_mock_expected = [
        (
            {},
            ({}, 'Reply from validator is not available, check backend logs'),
            ({}, ['Reply from validator is not available, check backend logs'])
        ),
        (
            {'input': 'config'}, ({'config': {'output': 'no errors'}}, ''), ({'output': 'no errors'}, [])
        ),
        (
            {'input': 'config'},
            ({'config': {'output': 'with errors'}, 'errors': {'phantom': 'first error', 'uploader': 'second error'}}, ''),
            ({'output': 'with errors'}, ['phantom: first error', 'uploader: second error'])
        )
    ]

    tank_finder_input_mock_expected = [
        ('tank_dc1', 'target_dc1', ({'result': True}, ''), (True, '')),
        ('tank_dc1', 'target_dc2', ({'result': False}, ''), (False, '')),
        (
            'tank_does_not_exist', 'target_not_found',
            ({'result': 'error', 'error_msg': 'Ip address not found'}, ''),
            (False, 'Ip address not found')
        ),
        (
            'valid_tank', 'valid_target',
            ({}, 'Reply from tank_finder is not available, check backend logs'),
            (False, 'Reply from tank_finder is not available, check backend logs')
        ),
    ]

    @pytest.mark.skip
    @pytest.mark.parametrize('_input, mock, expected', validator_input_mock_expected)
    def test_call_validator(self, monkeypatch, _input, mock, expected):

        def mockreturn(*args, **kwargs):
            return mock

        monkeypatch.setattr(external_calls, "external_call", mockreturn)
        result, err = call_validator(_input)
        assert (result, err) == expected

    @pytest.mark.skip
    @pytest.mark.parametrize('tank, target, mock, expected', tank_finder_input_mock_expected)
    def test_call_tank_finder(self, monkeypatch, tank, target, mock, expected):

        def mockreturn(*args, **kwargs):
            return mock

        monkeypatch.setattr(external_calls, "external_call", mockreturn)
        target, result, err = call_tank_finder(tank, 8083, target)
        assert (result, err) == expected


class TestGetTanksFromConfig:

    tanks_input_expected = [
        ({'neuploader': {'meta': {'use_tank': 'neuploader tank', 'use_tank_port': 13}}}, ('neuploader tank', 13)),
        ({'uploader': {'meta': {'use_tank': 'uploader tank', 'use_tank_port': 23}}}, ('uploader tank', 23)),
        (
            {
                'neuploader': {'meta': {'use_tank': 'neuploader tank', 'use_tank_port': 13}},
                'uploader': {'meta': {'use_tank': 'uploader tank', 'use_tank_port': 23}}
            },
            ('neuploader tank', 13)
        ),
        ({}, ('', ''))
    ]

    @pytest.mark.parametrize('test_input, expected', tanks_input_expected)
    def test_tanks_from_neuploader(self, test_input, expected):
        assert get_tanks_from_config(test_input) == expected


class TestGetTargetFromConfig:

    target_input_expected = [
        (
            {'phantom': {'address': 'ya.ru'}},
            ('ya.ru', '80')
        ),
        (
            {'phantom': {'address': 'ya.ru:443'}},
            ('ya.ru', '443')
        ),
        (
            {'phantom': {'address': '[2a02:6b8:c1a:2ebf:0:43ae:3677:3]'}},
            ('[2a02:6b8:c1a:2ebf:0:43ae:3677:3]', '80')
        ),
        (
            {'phantom': {'address': '[2a02:6b8:c1a:2ebf:0:43ae:3677:3]:443'}},
            ('[2a02:6b8:c1a:2ebf:0:43ae:3677:3]', '443')
        ),
        (
            {'pandora': {'config_content':
                             {'log': {'level': 'error'},
                              'pools': [
                                  {
                                      'ammo': {'source': {'path': './ammo.json', 'type': 'file'}, 'type': 'retriever_provider'},
                                      'gun':  {'target': 'retriever-1.retriever.load.retriever.mail.stable.qloud-d.yandex.net',
                                               'type': 'retriever_gun'},
                                      'id': 'HTTP pool',
                                      'result': {'destination': './phout.log', 'type': 'phout'},
                                      'rps': {'duration': '600s', 'ops': 150, 'type': 'const'},
                                      'startup': {'times': 5000, 'type': 'once'}
                                  }, ]
                              },
                         'enabled': True},
             'phantom': {'address': '', 'enabled': False}},
            ('retriever-1.retriever.load.retriever.mail.stable.qloud-d.yandex.net', '80')
        ),
        (
            {'pandora': {'config_content':
                             {'log': {'level': 'error'},
                              'pools': [
                                  {
                                      'ammo': {'source': {'path': './ammo.json', 'type': 'file'},
                                               'type': 'retriever_provider'},
                                      'gun': {
                                          'target': 'retriever-1.retriever.load.retriever.mail.stable.qloud-d.yandex.net:443',
                                          'type': 'retriever_gun'},
                                      'id': 'HTTP pool',
                                      'result': {'destination': './phout.log', 'type': 'phout'},
                                      'rps': {'duration': '600s', 'ops': 150, 'type': 'const'},
                                      'startup': {'times': 5000, 'type': 'once'}
                                  }, ]
                              },
                         'enabled': True},
             'phantom': {'address': '', 'enabled': False}},
            ('retriever-1.retriever.load.retriever.mail.stable.qloud-d.yandex.net', '443')
        ),
        (
            {'pandora': {'config_content':
                             {'log': {'level': 'error'},
                              'pools': [
                                  {
                                      'ammo': {'source': {'path': './ammo.json', 'type': 'file'},
                                               'type': 'retriever_provider'},
                                      'gun': {
                                          'target': 'retriever-1.retriever.load.retriever.mail.stable.qloud-d.yandex.net:80',
                                          'type': 'retriever_gun'},
                                      'id': 'HTTP pool',
                                      'result': {'destination': './phout.log', 'type': 'phout'},
                                      'rps': {'duration': '600s', 'ops': 150, 'type': 'const'},
                                      'startup': {'times': 5000, 'type': 'once'}
                                  }, ]
                              },
                         'enabled': True},
             'phantom': {'address': 'ya.ru:995', 'enabled': False}},
            ('ya.ru', '995')
        )
    ]

    @pytest.mark.parametrize("test_input, expected", target_input_expected)
    def test_get_target_from_config(self, test_input, expected):
        assert get_target_from_config(test_input) == expected


class TestTask:

    def test_task_init(self, patch_db):
        test_task = Task(json.dumps({'some': 'config'}))
        assert test_task.status == InternalStatus.NEW
        assert test_task.errors == {}
        assert test_task.id.isalnum()
