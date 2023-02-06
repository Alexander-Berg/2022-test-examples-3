#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def prepare_its_experiment_flags(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=1234,
                vendor_id=2345,
                datasource_id=10,
                models=[
                    ModelWithBid(model_id=1000+i) for i in range(1, 4)
                ],
                bid=90,
            ),
            IncutModelsList(
                hid=1234,
                vendor_id=2346,
                datasource_id=11,
                models=[
                    ModelWithBid(model_id=1010+i) for i in range(1, 4)
                ],
                bid=45,
            )
        ]

    # для декстопной врезки моделей не хватает,
    # без флагов должна вернуться пустая.
    def test_its_experiment_flags_empty_file(self):
        self.save_experiment_flags()
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'Empty',
                    },
                },
            },
        })

    # с измененным значением флага должна вернуться ModelsList.
    def test_its_experiment_flags_set_default_value(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "default_value": "3"
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                    },
                },
            },
        })

    # с измененным значением флага должна вернуться пустая,
    # если значение изменено в cgi.
    def test_its_experiment_flags_set_default_value_changed_by_cgi(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "default_value": "3"
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        }, exp_flags={
            'market_madv_incut_high_snippets_desktop_min_docs': 6,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'Empty',
                    },
                },
            },
        })

    # с измененным значением флага с '!' должна вернуться ModelsList,
    # значение изменено в cgi проигнорируется.
    def test_its_experiment_flags_set_froced_default_value(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "default_value": "3!"
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        }, exp_flags={
            'market_madv_incut_high_snippets_desktop_min_docs': 6,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                    },
                },
            },
        })

    # с пустым значением флага должна вернуться пустая.
    def test_its_experiment_flags_empty_default_value(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "default_value": ""
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'Empty',
                    },
                },
            },
        })

    # с невалидным значением флага должна вернуться пустая.
    def test_its_experiment_flags_invalid_default_value(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "default_value": "спартак - чемпион"
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'Empty',
                    },
                },
            },
        })

    # тесты запускаются в тестинге sas,
    # с условием для прода, престейбла, vla или man должна вернуться пустая.
    def test_its_experiment_flags_other_dcs_envs(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "conditions": [
                    {
                        "condition": "IS_PROD",
                        "value": "3"
                    },
                    {
                        "condition": "IS_PREP",
                        "value": "3"
                    },
                    {
                        "condition": "IS_MAN",
                        "value": "3"
                    },
                    {
                        "condition": "IS_VLA",
                        "value": "3"
                    },
                ],
                "default_value": ""
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'Empty',
                    },
                },
            },
        })

    # для sas должна вернуться ModelsList.
    def test_its_experiment_flags_dc(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "conditions": [
                    {
                        "condition": "IS_SAS",
                        "value": "3"
                    },
                ],
                "default_value": ""
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                    },
                },
            },
        })

    # для тестинга должна вернуться ModelsList.
    def test_its_experiment_flags_env(self):
        self.save_experiment_flags({
            'incut_high_snippets_desktop_min_docs' : {
                "conditions": [
                    {
                        "condition": "IS_TEST",
                        "value": "3"
                    },
                ],
                "default_value": ""
            }
        })
        response = self.request({
            'hid': 1234,
            'frontend': 'desktop',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                    },
                },
            },
        })


if __name__ == '__main__':
    env.main()
