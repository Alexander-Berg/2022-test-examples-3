from travel.rasp.smoke_tests.smoke_tests.checkers import RedirectCheck
from travel.rasp.smoke_tests.smoke_tests.config.morda_front_newRouting.env import env


tablo_station = '2000006'
train_canonical_page = f'/station/{tablo_station}/'
train_arrival_canonical_page = f'/station/{tablo_station}/?event=arrival'
suburban_canonical_page = f'/station/{tablo_station}/suburban/'

only_train_station = '9620410'
only_train_canonocal_page = f'/station/{only_train_station}/'
only_train_arrival_canonocal_page = f'/station/{only_train_station}/?event=arrival'

only_suburban_station = '9603766'
only_suburban_canonocal_page = f'/station/{only_suburban_station}/'


# И ПОЕЗДА И ЭЛЕКТРИЧКИ
new_station_canonical_urls_tablo_station = [

    # поезда, отправление
    f'station/{tablo_station}/',
    [f'station/{tablo_station}', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],

    [f'station/{tablo_station}/?event=departure', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],
    [f'station/{tablo_station}?event=departure', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],

    [f'station/{tablo_station}/?type=train', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],
    [f'station/{tablo_station}?type=train', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],
    [f'station/{tablo_station}/train', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],
    [f'station/{tablo_station}/train/', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],

    [f'station/{tablo_station}/?type=train&event=departure', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],
    [f'station/{tablo_station}?type=train&event=departure', {'code': 301, 'processes': [RedirectCheck(train_canonical_page)]}],

    # поезда, прибытие
    f'station/{tablo_station}/?event=arrival',
    [f'station/{tablo_station}?event=arrival', {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page)]}],
    [f'station/{tablo_station}/?type=train&event=arrival', {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page)]}],
    [f'station/{tablo_station}?type=train&event=arrival',  {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page)]}],

    [f'station/{tablo_station}/train/?event=arrival', {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page)]}],
    [f'station/{tablo_station}/train?event=arrival', {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page)]}],

    # электрички
    f'station/{tablo_station}/suburban/',
    [f'station/{tablo_station}/suburban', {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page)]}],
    [f'station/{tablo_station}/?type=suburban', {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page)]}],
    [f'station/{tablo_station}?type=suburban', {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page)]}],
]

new_station_diff_urls_tablo_station = [
    # основные страницы с параметрами, которые не должны отдавать редиректы
    train_canonical_page + '?date=all_days',
    train_canonical_page + '?date=tomorrow',

    train_arrival_canonical_page+'&date=all_days',

    suburban_canonical_page+'?direction=blablabla',
    suburban_canonical_page + '?date=today&direction=blablabla',
    suburban_canonical_page + '?date=today&direction=all',

    # популярные страницы на десктопе
    [f'station/{tablo_station}/?span=schedule&type=train',  {'code': 301, 'processes': [RedirectCheck(train_canonical_page+'?date=all_days')]}],
    # [f'station/{tablo_station}/?type=tablo',  {'code': 301, 'processes': [RedirectCheck(train_canonical_page )]}],
    [f'station/{tablo_station}/?span=schedule&type=train&event=arrival', {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page+'&date=all_days')]}],
    [f'station/{tablo_station}/?direction=blablabla&type=suburban', {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page+'?direction=blablabla')]}],
    [f'station/{tablo_station}/?span=tomorrow&type=train', {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=tomorrow')]}],
    [f'station/{tablo_station}/?span=day&direction=blablabla&type=suburban', {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page + '?date=today&direction=blablabla')]}],
    [
        f'station/{tablo_station}/?from=wraspstation&type=suburban&span=day&direction=all&req_id=1561',
        {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page + '?date=today&direction=all')]}
    ],
    [f'station/{tablo_station}/?span=schedule&type=train&event=departure', {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=all_days')]}],
    [f'station/{tablo_station}/?span=schedule&type=train&event=arrival',  {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page + '&date=all_days')]}],
    # [f'station/{tablo_station}?start=2019-07-25T07%3A00%3A00&type=tablo&span=5',   {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=2019-07-25')]}],
    [f'station/{tablo_station}?start=2019-07-25T07%3A00%3A00&span=5',   {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=2019-07-25')]}],
    # [f'station/{tablo_station}?start=2019-07-25T07%3A00%3A00&type=tablo&event=arrival&span=5',   {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page + '&date=2019-07-25')]}],
    [f'station/{tablo_station}?start=2019-07-25T07%3A00%3A00&event=arrival&span=5',  {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page + '&date=2019-07-25')]}],

    # популярные страницы в таче
    [f'station/{tablo_station}/suburban/?filter=all',  {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page + '?date=all_days')]}],
    [f'station/{tablo_station}/?filter=all',  {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=all_days')]}],
    [f'station/{tablo_station}/suburban/?filter=today',  {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page + '?date=today')]}],
    [f'station/{tablo_station}/train/?span=schedule',  {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=all_days')]}],
    [f'station/{tablo_station}/train/?span=tomorrow',   {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=tomorrow')]}],
    [f'station/{tablo_station}/train/?span=day',   {'code': 301, 'processes': [RedirectCheck(train_canonical_page + '?date=today')]}],
    [f'station/{tablo_station}/?filter=all&event=arrival',   {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page + '&date=all_days')]}],
    [f'station/{tablo_station}/suburban/?filter=all&direction=blablabla',    {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page + '?date=all_days&direction=blablabla')]}],
    [f'station/{tablo_station}/train/?event=arrival&span=tomorrow',     {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page + '&date=tomorrow')]}],
    [f'station/{tablo_station}/suburban/?filter=',     {'code': 301, 'processes': [RedirectCheck(suburban_canonical_page + '?date=today')]}],
    [f'station/{tablo_station}/train/?event=arrival&span=schedule',     {'code': 301, 'processes': [RedirectCheck(train_arrival_canonical_page + '&date=all_days')]}],

]

# ТОЛЬКО ПОЕЗДА
new_station_canonical_urls_only_train_station = [
    # поезда, отправление
    f'station/{only_train_station}/',
    [f'station/{only_train_station}', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],

    [f'station/{only_train_station}/?event=departure', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}?event=departure', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],

    [f'station/{only_train_station}/?type=train', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}?type=train', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}/train', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}/train/', {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],

    [f'station/{only_train_station}/?type=train&event=departure',  {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}?type=train&event=departure',   {'code': 301, 'processes': [RedirectCheck(only_train_canonocal_page)]}],

    # поезда, прибытие
    f'station/{only_train_station}/?event=arrival',
    [f'station/{only_train_station}?event=arrival', {'code': 301, 'processes': [RedirectCheck(only_train_arrival_canonocal_page)]}],
    [f'station/{only_train_station}/?type=train&event=arrival', {'code': 301, 'processes': [RedirectCheck(only_train_arrival_canonocal_page)]}],
    [f'station/{only_train_station}?type=train&event=arrival',  {'code': 301, 'processes': [RedirectCheck(only_train_arrival_canonocal_page)]}],

    [f'station/{only_train_station}/train/?event=arrival',  {'code': 301, 'processes': [RedirectCheck(only_train_arrival_canonocal_page)]}],
    [f'station/{only_train_station}/train?event=arrival',   {'code': 301, 'processes': [RedirectCheck(only_train_arrival_canonocal_page)]}],

    # электрички
    [f'station/{only_train_station}/suburban/', {'code': 302, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}/suburban', {'code': 302, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}/?type=suburban', {'code': 302, 'processes': [RedirectCheck(only_train_canonocal_page)]}],
    [f'station/{only_train_station}?type=suburban', {'code': 302, 'processes': [RedirectCheck(only_train_canonocal_page)]}]

]

# только электрички
new_station_canonical_urls_only_suburban_station = [
    # поезда, отправление
    f'station/{only_suburban_station}/',
    [f'station/{only_suburban_station}', {'code': 301, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],

    [f'station/{only_suburban_station}/?event=departure', {'code': 301, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}?event=departure', {'code': 301, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],

    [f'station/{only_suburban_station}/?type=train', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}?type=train', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}/train', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}/train/', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],

    [f'station/{only_suburban_station}/?type=train&event=departure',  {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}?type=train&event=departure',   {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],

    # поезда, прибытие
    [f'station/{only_suburban_station}/?event=arrival', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}?event=arrival', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}/?type=train&event=arrival', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}?type=train&event=arrival',  {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],

    [f'station/{only_suburban_station}/train/?event=arrival',  {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}/train?event=arrival',   {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],

    # электрички
    [f'station/{only_suburban_station}/suburban/', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}/suburban', {'code': 302, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}/?type=suburban', {'code': 301, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}],
    [f'station/{only_suburban_station}?type=suburban', {'code': 301, 'processes': [RedirectCheck(only_suburban_canonocal_page)]}]

]

new_station_urls = new_station_diff_urls_tablo_station + new_station_canonical_urls_tablo_station + new_station_canonical_urls_only_suburban_station + new_station_canonical_urls_only_train_station


processes = [
    {
        'hosts':  env.hosts,
        # 'host': env.host_ru,
        'params': {'timeout':  env.timeout, 'allow_redirects': False, 'cookies': {'experiment__newStationPage': '1'}},
        'urls': new_station_urls,
    },
    {
        'hosts': env.touch_hosts,
        # 'host': env.host_ru_t,
        'params': {'timeout': env.timeout, 'allow_redirects': False,
                   'cookies': {'experiment__newStationPage': '1'}},
        'urls': new_station_urls,
    },

]
