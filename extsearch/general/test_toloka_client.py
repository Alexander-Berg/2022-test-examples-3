import logging
from typing import Optional

from .model import TolokaTask, TolokaSolution, TolokaResult, TolokaResultItem, TolokaResultItemStatus
from .toloka_client import TolokaClient

assignments = TolokaResult(
    items=[TolokaResultItem(
        status=TolokaResultItemStatus.Accepted,
        tasks=[
            TolokaTask(
                input_values={
                    'key': '14731946017985284948',
                    'topics': ['Музыка', 'Кулинарные рецепты', 'Рукоделие', 'Автомобили', 'Кошки', 'Техника',
                               'Ремонт',
                               'Шоу', 'Спорт', 'Обучение', 'Рыбалка', 'Сделай сам', 'Книги', 'Игрушки',
                               'Юмор',
                               'Компьютерные игры', 'Beauty', 'Фитнес', 'Конструкторы', 'Танцы', 'Животные',
                               'Растения',
                               'Хобби', 'Гороскопы', 'Охота', 'Дерматология', 'Поэзия', 'Концерты',
                               'Путешествия'],
                    'screenshots': [
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb019.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb010.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb023.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb020.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb025.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb022.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb028.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb001.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb029.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb026.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb015.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb006.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb027.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb011.jpg',
                        'http://s3.mds.yandex.net/transcoder-test/vod-content/districts-testing/2714_thumb005.jpg']
                },
                known_solutions=None
            )
        ],
        solutions=[
            TolokaSolution(
                output_values={
                    'other': True,
                    'topics': {'Beauty': False,
                               'Шоу': False,
                               'Юмор': False,
                               'Книги': False,
                               'Кошки': False,
                               'Охота': False,
                               'Спорт': False,
                               'Танцы': False,
                               'Хобби': False,
                               'Музыка': False,
                               'Поэзия': False,
                               'Ремонт': False,
                               'Фитнес': False,
                               'Игрушки': False,
                               'Рыбалка': False,
                               'Техника': False,
                               'Животные': False,
                               'Концерты': False,
                               'Обучение': False,
                               'Растения': False,
                               'Гороскопы': False,
                               'Рукоделие': False,
                               'Сделай сам': False,
                               'Автомобили': False,
                               'Путешествия': False,
                               'Дерматология': False,
                               'Конструкторы': False,
                               'Компьютерные игры': False,
                               'Кулинарные рецепты': False},
                    'unknown': False
                }
            )
        ]
    )])


class FakeApi:
    def get_assignments(self, pool_id: str) -> TolokaResult:
        return assignments

    def get_pool_status(self, pool_id: str) -> Optional[str]:
        return "Closed"


def test_statuses():
    logging.basicConfig(level=logging.DEBUG)
    client = TolokaClient(
        "0",
        10,
        FakeApi(),
    )
    statuses = client.statuses("1")
    assert list(statuses.keys()) == ['14731946017985284948']
    assert list(statuses.values()) == [assignments.items[0].solutions]
