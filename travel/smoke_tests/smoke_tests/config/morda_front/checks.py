from travel.rasp.smoke_tests.smoke_tests.config.morda_front.env import env
from travel.rasp.smoke_tests.smoke_tests.config.morda_front.urls import desktop_urls, touch_urls


checks = [
    {
        'hosts': env.hosts,
        'params': {'timeout': env.timeout},
        'urls': desktop_urls,
    },
    {
        'hosts': env.touch_hosts,
        'params': {'timeout': env.touch_timeout},
        'urls': touch_urls,
    }
]
