import json

from market.content_storage_service.lite.core.types.model import Model
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.types.recom import ReasonsToBuy
from market.content_storage_service.lite.core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.saas = T.market_content_storage.saas_server.connect()

        cls.index.hids += [
            Hid(hid=101, name='Цинтра', unique_name='Цинтра (unique)', output_type='GURU', leaf=True)
        ]

        cls.index.models += [
            Model(hyperid=1, hid=101, title='Model 1'),
            Model(hyperid=2, hid=101, title='Model 2'),
            Model(hyperid=3, hid=101, title='Model 3'),
            Model(hyperid=4, hid=101, title='Model 4'),
            Model(hyperid=5, hid=101, title='Model 5'),
        ]

        cls.index.reasons_to_buy += [
            ReasonsToBuy(
                model_id=1,
                reasons_json=[
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.875,
                    },
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Довольно хорошая карточка",
                        "value": 4.5,
                    },
                ]
            ),
            ReasonsToBuy(model_id=2, reasons_json=[]),
            ReasonsToBuy(
                model_id=3,
                reasons_json=[
                    {
                        # Уйдет тк value < value_threshold
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Неплохая вещь",
                        "anonymous": "false",
                        "value": 2.0,
                        "value_threshold": "4.0",
                    },
                    {
                        # Уйдет тк value < value_threshold
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.6,
                        "value_threshold": "0.8",
                    },
                    {
                        # Уйдет тк rating < rating_threshold
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "rating": "4.0",
                        "rating_threshold": "4.5",
                        "value": 0.95,
                        "recommenders_count": "100",
                        "share_threshold": "0.8",
                    },
                    {
                        "id": "random_factor",
                        "text": "Оставшийся фактор"
                    },
                ]
            ),
            ReasonsToBuy(
                model_id=4,
                reasons_json=[
                    {
                        # Уйдет тк anonymous == true
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Неплохая вещь",
                        "anonymous": "true",
                        "value": 4.5,
                        "value_threshold": "4.0",
                    },
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.625,
                        "value_threshold": "0.5",
                    },
                    {
                        # Уйдет по формуля (см. тест)
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "rating": "4.5",
                        "rating_threshold": "4.0",
                        "value": 0.75,
                        "recommenders_count": "10",
                        "share_threshold": "0.8",
                    },
                ]
            ),
            ReasonsToBuy(
                model_id=5,
                reasons_json=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Неплохая вещь",
                        "anonymous": "false",
                        "value": 4.5,
                        "value_threshold": "4.0",
                    },
                    {
                        "id": "best_by_factor",
                        "type": "consumerFactor",
                        "factor_id": "1",
                        "factor_priority": "1",
                        "factor_name": "factorName1",
                        "value": 0.625,
                        "value_threshold": "0.5",
                    },
                    {
                        "id": "customers_choice",
                        "type": "consumerFactor",
                        "rating": "4.5",
                        "rating_threshold": "4.0",
                        "value": 0.875,
                        "recommenders_count": "10",
                        "share_threshold": "0.8",
                    },
                ]
            ),
        ]

    def test_reasons_to_buy(self):
        '''
        Проверяем, что content-storage валидно возвращает причины для покупки

        Тест сделан по аналогии с репортом:
        https://a.yandex-team.ru/arc/trunk/arcadia/market/report/lite/test_reasons_to_buy.py?rev=r9379441#L464
        '''
        request = {
            'model_ids': [1, 2],
            'market_skus': []
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 1,
                        'title': {
                            'raw': 'Model 1'
                        },
                        'reasonsToBuy': [
                            {
                                "id": "best_by_factor",
                                "type": "consumerFactor",
                                "factor_id": "1",
                                "factor_priority": "1",
                                "factor_name": "factorName1",
                                "value": 0.875,
                            },
                            {
                                "id": "positive_feedback",
                                "type": "consumerFactor",
                                "author_puid": "1001",
                                "text": "Довольно хорошая карточка",
                                "value": 4.5,
                            },
                        ]
                    },
                    {
                        'id': 2,
                        'title': {
                            'raw': 'Model 2'
                        },
                        'reasonsToBuy': []
                    },
                ],
            },
            allow_different_len=False
        )

    def test_reasons_to_buy_filtering(self):
        '''
        Проверяем фильтрацию причин positive_feedback, best_by_factor и customers_choice

        1) positive_feedback фильтруется, если:
            - value ниже value_threshold
            - есть поле анонимности (anonymous)
        2) customers_choice фильтруется, если:
            - rating ниже rating_threshold
            - или если (0.5 + value * recommenders_count) / (1 + recommenders_count) меньше чем share_threshold
        3) best_by_factor фильтруется, если:
            - value ниже value_threshold
        '''

        # Ожидаем результат:
        # 1) У модели 3 отсортируются все причины (positive_feedback, best_by_factor и customers_choice)
        # 2) У модели 4 останется только best_by_factor
        # 3) У модели 5 останутся все факторы
        request = {
            'model_ids': [3, 4, 5],
            'market_skus': []
        }
        response = self.market_content_storage.request_json(
            'card_info',
            method='GET',
            body=json.dumps(request)
        )

        self.assertFragmentIn(
            response,
            {
                'models': [
                    {
                        'id': 3,
                        'title': {
                            'raw': 'Model 3'
                        },
                        'reasonsToBuy': [
                            {
                                "id": "random_factor",
                                "text": "Оставшийся фактор"
                            },
                        ]
                    },
                    {
                        'id': 4,
                        'title': {
                            'raw': 'Model 4'
                        },
                        'reasonsToBuy': [
                            {
                                "id": "best_by_factor",
                                "value": 0.625,
                                "factor_id": "1",
                                "factor_name": "factorName1",
                                "factor_priority": "1",
                                "type": "consumerFactor",
                            },
                        ]
                    },
                    {
                        'id': 5,
                        'title': {
                            'raw': 'Model 5'
                        },
                        'reasonsToBuy': [
                            {
                                "id": "positive_feedback",
                                "value": 4.5,
                                "type": "consumerFactor",
                                "author_puid": "1001",
                                "text": "Неплохая вещь",
                            },
                            {
                                "id": "best_by_factor",
                                "type": "consumerFactor",
                                "factor_id": "1",
                                "factor_priority": "1",
                                "factor_name": "factorName1",
                                "value": 0.625,
                            },
                            {
                                "id": "customers_choice",
                                "type": "consumerFactor",
                                "value": 0.875,
                            },
                        ]
                    },
                ],
            },
            allow_different_len=False
        )


if __name__ == '__main__':
    main()
