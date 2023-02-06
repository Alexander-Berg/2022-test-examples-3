# coding: utf8
from travel.rasp.smoke_tests.smoke_tests.config.blablacar.env import env
from travel.rasp.smoke_tests.smoke_tests.config.blablacar.urls import ping_url, urls, urls_with_status_codes


def gen_urls_with_provider(provider, urls):
    urls_with_providers = []
    for url in urls:
        if isinstance(url, str):
            urls_with_providers.append(f'{url}&provider={provider}')
        else:
            url, *url_args = url
            urls_with_providers.append([f'{url}&provider={provider}', *url_args])
    return urls_with_providers


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout, 'retries': env.retries},
        'urls': ping_url + gen_urls_with_provider('blablacar_v3', urls + urls_with_status_codes),
    },
    # Пока реально не работаем с Едем.РФ, а он уже в тестинге иногда не работает
    # {
    #     'host': env.host,
    #     'params': {'timeout': env.timeout, 'retries': env.retries},
    #     'urls': ping_url + gen_urls_with_provider('edem_rf', urls),
    # },
]
