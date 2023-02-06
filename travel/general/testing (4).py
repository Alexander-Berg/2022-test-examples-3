import os

from travel.rasp.pathfinder_proxy.settings.base import BaseSettings


class Settings(BaseSettings):
    TRAINS_URL_PREFIX = 'https://travel-test.yandex.ru/trains'

    REDIS_SERVICE_NAME = 'raas__testing__main'
    REDIS_PASSWORD_PATH = 'rasp-common-testing'
    REDIS_HOSTS = [
        'man-moztr906p99mk8ei.db.yandex.net',
        'sas-e0aovd3is1fiv7bi.db.yandex.net',
        'vla-9xebzav6v7mt4qhf.db.yandex.net'
    ]

    TRAIN_API_ENDPOINT = os.getenv('TRAIN_API_ENDPOINT', 'https://testing.train-api.rasp.internal.yandex.net')
    MORDA_BACKEND_ENDPOINT = os.getenv('MORDA_BACKEND_ENDPOINT', 'https://testing.morda-backend.rasp.yandex.net')
    TICKET_DAEMON_ENDPOINT = os.getenv('TICKET_DAEMON_ENDPOINT', 'http://ticket-daemon-api.testing.avia.yandex.net')
