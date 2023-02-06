#!/usr/bin/env python
# -*- coding: utf-8 -*-
import json

import __classic_import     # noqa
import market.seo.experiments.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty


class T(env.ExperimentsSuite):
    def _get_idx(self, entity):
        """
        Получить какой-нибудь id элемента страницы, коллекции или сервиса.
        Если нужной сущности не существует - создать.
        """
        response = self.experiments.request_json(entity)
        if response.root:
            return response.root[0]['id']

        response = self.experiments.request_json('{}?name={}'.format(entity, entity), method='POST')
        return response.root['id']

    def test_create_experiment_without_splits(self):
        """Создание эксперимента без сплитов"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        exp = {
            'name': 'Эксперимент 1',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': True,
            'use_touch': False,
            'splits': [],
            'control_split': '//tmp/control',
        }
        response = self.experiments.request_json('experiments', method='POST', body=json.dumps(exp))
        exp_id = response.root['id']

        exp['id'] = exp_id
        exp['is_active'] = 0
        exp['start_date'] = ''
        exp['use_touch'] = 0
        exp['use_desktop'] = 1
        response = self.experiments.request_json('experiments')
        self.assertFragmentIn(response, [exp])

        response = self.experiments.request_json('experiments/{}'.format(exp_id))
        self.assertFragmentIn(response, exp, allow_different_len=False)

    def test_create_experiment_with_split(self):
        """Создание эксперимента со сплитами"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        exp = {
            'name': 'Эксперимент 2',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': False,
            'use_touch': True,
            'control_split': '//tmp/control',
            'splits': [
                {
                    'split_link': '//tmp/1',
                    'template': 'Some string',
                },
                {
                    'split_link': '//tmp/2',
                    'template': 'Some string 2',
                }
            ],
        }
        response = self.experiments.request_json('experiments', method='POST', body=json.dumps(exp))
        exp_id = response.root['id']

        exp['id'] = exp_id
        exp['is_active'] = 0
        exp['start_date'] = ''
        exp['use_touch'] = 1
        exp['use_desktop'] = 0
        response = self.experiments.request_json('experiments')
        self.assertFragmentIn(response, [exp])

        response = self.experiments.request_json('experiments/{}'.format(exp_id))
        self.assertFragmentIn(response, exp, allow_different_len=False)

    def test_delete_experiment(self):
        """Удаление эксперимента"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        exp = {
            'name': 'Эксперимент 3',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': False,
            'use_touch': True,
            'splits': [],
            'control_split': '//tmp/control',
        }
        response = self.experiments.request_json('experiments', method='POST', body=json.dumps(exp))
        exp_id = response.root['id']

        self.experiments.request_json('experiments/{}'.format(exp_id), method='DELETE')

        response = self.experiments.request_json('experiments')
        self.assertFragmentNotIn(response, {
            'id': exp_id,
        })

    def test_update_experiment(self):
        """Обновление эксперимента"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        exp = {
            'name': 'Эксперимент 4',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': True,
            'use_touch': True,
            'control_split': '//tmp/control',
            'splits': [
                {
                    'split_link': '//tmp/1',
                    'template': 'Some string',
                },
                {
                    'split_link': '//tmp/2',
                    'template': 'Some string 2',
                }
            ],
        }
        response = self.experiments.request_json('experiments', method='POST', body=json.dumps(exp))
        exp_id = response.root['id']

        exp['id'] = exp_id
        exp['description'] = 'New description'
        exp['splits'] = [
            {
                'split_link': '//tmp/3',
                'template': 'Some string',
            }
        ]
        exp['control_split'] = '//tmp/control_new'

        response = self.experiments.request_json('experiments/{}'.format(exp_id), method='PUT', body=json.dumps(exp))

        exp['is_active'] = 0
        exp['start_date'] = ''
        exp['use_touch'] = 1
        exp['use_desktop'] = 1
        self.assertFragmentIn(response, exp)

    def test_start_stop_experiment(self):
        """Запуск/остановка эксперимента"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        exp = {
            'name': 'Эксперимент 5',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': True,
            'use_touch': True,
            'splits': [],
            'control_split': '//tmp/control',
        }
        response = self.experiments.request_json('experiments', method='POST', body=json.dumps(exp))
        exp_id = response.root['id']

        self.experiments.request_json('experiments/{}/start'.format(exp_id), method='POST')

        response = self.experiments.request_json('experiments/{}'.format(exp_id))
        self.assertFragmentIn(response, {
            'id': exp_id,
            'is_active': 1,
            'start_date': NotEmpty(),
            'end_date': '',
        })

        self.experiments.request_json('experiments/{}/stop'.format(exp_id), method='POST')

        response = self.experiments.request_json('experiments/{}'.format(exp_id))
        self.assertFragmentIn(response, {
            'id': exp_id,
            'is_active': 0,
            'start_date': NotEmpty(),
            'end_date': NotEmpty(),
        })

    def change_splits(self):
        """Смена таблиц у сплитов внутри эксперимента"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        exp = {
            'name': 'Эксперимент 6',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': True,
            'use_touch': True,
            'control_split': '//tmp/control',
            'splits': [
                {
                    'split_link': '//tmp/3',
                    'template': '//tmp/4',
                },
                {
                    'split_link': '//tmp/4',
                    'template': '//tmp/3',
                }
            ],
        }

        response = self.experiments.request_json('experiments', method='POST', body=json.dumps(exp))
        exp['id'] = response.root['id']
        exp['splits'] = response.root['splits']

        for split in exp['splits']:
            split['split_link'] = split['template']

        response = self.experiments.request_json('experiments/{}'.format(exp['id']), method='PUT', body=json.dumps(exp))
        self.assertFragmentIn(response, exp)

    def get_active_experiments(self):
        """Only one active experiment should be returned"""
        element_id = self._get_idx('elements')
        service_id = self._get_idx('services')
        collection_id = self._get_idx('collections')

        self.experiments.request_json('experiments', method='POST', body=json.dumps({
            'name': 'Эксперимент 7',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': True,
            'use_touch': True,
            'control_split': '//tmp/control',
        }))

        response = self.experiments.request_json('experiments', method='POST', body=json.dumps({
            'name': 'Эксперимент 8',
            'description': 'Description',
            'page_element': element_id,
            'collection': collection_id,
            'service': service_id,
            'use_desktop': True,
            'use_touch': True,
            'control_split': '//tmp/control',
        }))

        exp_id = response.root['id']
        self.experiments.request_json('experiments/{}/start'.format(exp_id), method='POST')

        response = self.experiments.request_json('experiments')
        self.assertEqual(len(response.root), 2)

        response = self.experiments.request_json('experiments?is_active=1')
        self.assertEqual(len(response.root), 1)
        self.assertFragmentIn(response, {'id': exp_id})


if __name__ == '__main__':
    env.main()
