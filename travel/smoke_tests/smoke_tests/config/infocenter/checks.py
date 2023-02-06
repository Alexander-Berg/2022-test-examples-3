from travel.rasp.smoke_tests.smoke_tests.config.infocenter.urls import urls, urls_admin
from travel.rasp.smoke_tests.smoke_tests.config.infocenter.env import env


checks = [
    {
        'urls': urls,
        'params': {'timeout': env.timeout},
        'host': env.host,
    },
    {
        'urls': urls_admin,
        'params': {'timeout': env.timeout},
        'host': env.host_admin,
    },
]
