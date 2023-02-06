import json

from market.content_storage_service.lite.core.types.model import Model, FastCard, VirtualCard, Sku
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.saas = T.market_content_storage.saas_server.connect()
        cls.market_content_storage.with_report_server = True
        cls.report = T.market_content_storage.report_server.connect()

        cls.index.hids += [
            Hid(hid=1, name='Цинтра', unique_name='Цинтра (unique)', output_type='GURU', leaf=True)
        ]

        cls.index.models += [Model(hyperid=100, hid=1, title='Model card')]

        cls.market_content_storage.set_virtual_range(20000, 40000)
        cls.index.virtual_cards += [VirtualCard(id=30000, hid=1, title='Virtual card')]

        cls.index.skus += [Sku(sku_id=200, model_id=100, title='Sku card', hid=1)]
        cls.index.fast_cards += [FastCard(id=201, hid=1, title='Fast card')]

    def test_debug_log(self):
        '''
        Проверяем вывод дебаг лога
        '''

        request = {
            'model_ids': [100, 30000, 201],
            'market_skus': [200, 30000, 201]
        }
        response = self.market_content_storage.request_json(
            'card_info?debug=',
            method='GET',
            body=json.dumps(request)
        )

        # Из-за хеш-мап мы не знаем порядок
        logs = response['debug']['debug_log']
        for debug_log in logs:
            if debug_log.find('Cards requested from saas:') != -1:
                assert debug_log[debug_log.find(':'):].count(' ') == 4  # общее кол-во карточек
                assert debug_log.count('M') == 2
                assert debug_log.count('S') == 2
                assert debug_log.count('201M') + debug_log.count('201S') + debug_log.count('200S') + debug_log.count('100M') == 4
            elif debug_log.find('Cards returned from saas:') != -1:
                assert debug_log[debug_log.find(':'):].count(' ') == 3
                assert debug_log.count('M') == 1
                assert debug_log.count('S') == 2
                assert debug_log.count('201S') + debug_log.count('200S') + debug_log.count('100M') == 3
            elif debug_log.find('Virtual cards requested from report:') != -1:
                assert debug_log[debug_log.find(':'):].count(' ') == 2
                assert debug_log.count('30000') == 2  # запросили как модель и скю
            elif debug_log.find('Fast cards requested from report:') != -1:
                assert debug_log[debug_log.find(':'):].count(' ') == 1
                assert debug_log.count('201') == 1
            elif debug_log.find('Cards returned from report:') != -1:
                assert debug_log[debug_log.find(':'):].count(' ') == 2
                assert debug_log.count('201') + debug_log.count('30000') == 2


if __name__ == '__main__':
    main()
