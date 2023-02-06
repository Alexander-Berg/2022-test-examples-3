# -*- coding: utf-8 -*-
import mock
from nose_parameterized import parameterized

from mpfs.common.util.experiments.logic import (
    change_experiment_context_with,
    ExperimentManager,
    ExperimentClauseFacade,
)
from mpfs.common.util.experiments.clauses import ExperimentByUidClause
from mpfs.config import settings
from test.unit.base import NoDBTestCase


class ExperimentManagerTestCase(NoDBTestCase):

    def test_add_active_experiment(self):
        fake_experiment_settings = {
            'exp_1': {
                'enabled': True,
                'clauses': {
                    'by_uid': {
                        'enabled_for': ['123',],
                    }
                }
            }
        }
        with mock.patch.dict(settings.experiments, fake_experiment_settings):
            experiment_manager = ExperimentManager()
        experiment_manager.update_context(uid='123')
        assert experiment_manager.is_feature_active('exp_1')

    def test_add_inactive_experiment(self):
        fake_experiment_settings = {
            'exp_1': {
                'enabled': False,
                'clauses': {
                    'by_uid': {
                        'enabled_for': ['123',],
                    }
                }
            }
        }
        with mock.patch.dict(settings.experiments, fake_experiment_settings):
            experiment_manager = ExperimentManager()
        experiment_manager.update_context(uid='123')
        assert not experiment_manager.is_feature_active('exp_1')

    def test_add_active_but_doesnt_match_condition(self):
        fake_experiment_settings = {
            'exp_1': {
                'enabled': True,
                'clauses': {
                    'by_uid': {
                        'enabled_for': ['321',],
                    }
                }
            }
        }
        with mock.patch.dict(settings.experiments, fake_experiment_settings):
            experiment_manager = ExperimentManager()
        experiment_manager.update_context(uid='123')
        assert not experiment_manager.is_feature_active('exp_1')

    def test_change_context_context_manager(self):
        from mpfs.common.util.experiments.logic import experiment_manager
        experiment_manager.update_context(uid='123')
        assert experiment_manager.context['uid'] == '123'
        with change_experiment_context_with(uid='321'):
            assert experiment_manager.context['uid'] == '321'
        assert experiment_manager.context['uid'] == '123'


class ExperimentByUidTestCase(NoDBTestCase):
    @parameterized.expand([
        (set(), 0, '123', False),
        (set(), 100, '123', True),
        ({'123', }, 0, '123', True),
        ({'321', }, 0, '123', False),
        (set(), 50, '1', True),  # ...14862c == 8 < 50
        (set(), 50, '4', False),  # ...a7baf3 == 95 > 50
    ])
    def test_is_active(self, enabled_uids_surely, percentage, uid, is_active):
        assert is_active == ExperimentClauseFacade.is_clause_valid(
            ExperimentByUidClause(enabled_uids_surely=enabled_uids_surely,
                                  percentage=percentage,
                                  seed='1'),
            {'uid': uid}
        )


class ExclusionsTestCase(NoDBTestCase):
    def test_no_passport_request_for_empty_regex_list(self):
        fake_experiment_settings = {
            'exp_1': {
                'enabled': True,
                'clauses': {
                    'by_uid': {
                        'enabled_for': ['123',],
                    }
                },
                'exclusions': {
                    'by_uid': {
                        'enabled_for': ['123']
                    }
                }
            }
        }
        with mock.patch.dict(settings.experiments, fake_experiment_settings):
            experiment_manager = ExperimentManager()
        experiment_manager.update_context(uid='123')

        with mock.patch('mpfs.core.services.passport_service.passport.userinfo') as mocked_passport:
            experiment_manager.is_feature_active('exp_1')

        mocked_passport.assert_not_called()
