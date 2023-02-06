__author__ = 'aokhotin'
STABLE_PRIEMKA_YANDEX_RU = "stable.priemka.yandex.ru"

REGIONS_DATA = {
    "RU": {"domain": "yandex.ru", "geo_id": 225},
    "KZ": {"domain": "yandex.kz", "geo_id": 159},
    "UA": {"domain": "yandex.ua", "geo_id": 187},
    "BY": {"domain": "yandex.by", "geo_id": 149},
    "TR": {"domain": "yandex.com.tr", "geo_id": 983},
}


def mutate_yandex_host_with_region(path, region):
    if len(path.split('.')) >= 2 and path.split('.')[-2].lower() == 'yandex':
        truncated_host = path[0:path.find("yandex")]
    else:
        truncated_host = path if path[-1] == '.' else path + '.'
    return "{}{}".format(truncated_host, REGIONS_DATA[region]["domain"])
