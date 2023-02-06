import json

from market.content_storage_service.lite.core.types.model import Model
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.saas = T.market_content_storage.saas_server.connect()

        cls.index.hids += [
            Hid(hid=1, name='Цинтра', unique_name='Цинтра (unique)', output_type='GURU', leaf=True)
        ]

        cls.index.models += [
            Model(hyperid=100, hid=1),
        ]

    def test_metrics(self):
        request = {
            "model_ids": [100]
        }

        # Ходим с платформами
        for platform in ['desktop', 'touch', 'ios', 'android', 'gwent(unknown)', '']:
            response = self.market_content_storage.request_json(
                'card_info?client={}'.format(platform),
                method='GET',
                body=json.dumps(request)
            )
            self.assertFragmentIn(response, {"models": [{"id": 100}]}, allow_different_len=False)

        # Если будет проставлен хеддер роботов != 0, то будет пустой ответ
        # Сделано как в репорте: https://a.yandex-team.ru/arcadia/market/report/library/context/context.cpp?rev=r9489735#L82
        response = self.market_content_storage.request_json(
            'card_info?client=ios',
            method='GET',
            body=json.dumps(request),
            headers={'X-Yandex-Antirobot-Degradation': '0.95'}
        )
        self.assertFragmentIn(response, {"models": []}, allow_different_len=False)

        # Если будет проставлен хеддер роботов == 0, то мы вернем ответ
        response = self.market_content_storage.request_json(
            'card_info?client=ios',
            method='GET',
            body=json.dumps(request),
            headers={'X-Yandex-Antirobot-Degradation': '0'}
        )
        self.assertFragmentIn(response, {"models": [{"id": 100}]}, allow_different_len=False)

        # Проверяем статистики
        response_stats = self.market_content_storage.request_json('stat', method='GET')
        self.assertFragmentIn(response_stats, [
            ['cs_CARD_INFO_rps_DESKTOP_dmmm', 1],
            ['cs_CARD_INFO_rps_TOUCH_dmmm', 1],
            ['cs_CARD_INFO_rps_ANDROID_dmmm', 1],
            ['cs_CARD_INFO_rps_IOS_dmmm', 2],
            ['cs_CARD_INFO_rps_UNKNOWN_dmmm', 2],
            ['cs_CARD_INFO_rps_ROBOTS_dmmm', 1],
        ])


if __name__ == '__main__':
    main()
