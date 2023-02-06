# coding: utf8

from common.apps.suburban_events.models import LVGD01_TR2PROC_query, LVGD01_TR2PROC, LVGD01_TR2PROC_feed
from travel.rasp.library.python.common23.date import environment


def create_row(i, **kwargs):
    row = {
        'ID': i,
        'IDTR': i,
        'IDRASP': i,
        'STORASP': i,
        'STOEX': i,
        'NAMESTO': '{}_{}'.format(i, i),
        'STNRASP': i,
        'STNEX': i,
        'NAMESTN': '{}_{}'.format(i, i),
        'NOMPEX': '{}_{}'.format(i, i),
        'NAMEP': '{}_{}'.format(i, i),
        'SOURCE': i,
        'KODOP': i,
        'DOR': i,
        'OTD': i,
        'NOMRP': i,
        'STOPER': i,
        'STOPEREX': i,
        'STNAME': '{}_{}'.format(i, i),
        'TIMEOPER_N': environment.now(),
        'TIMEOPER_F': environment.now(),
        'KM': i,
        'PRSTOP': i,
        'PRIORITY': i,
        'PRIORITY_RATING': i,
    }
    row.update(kwargs)

    row_prepared = dict(row)
    row_prepared['ID_TRAIN'] = row_prepared.pop('ID')

    # ожидаемый объект в базе
    lvgd_obj = LVGD01_TR2PROC(**row_prepared)

    return row, lvgd_obj


def create_rzd_query(**kwargs):
    default_kwargs = {
        'queried_at': environment.now(),
        'query_from': environment.now(),
        'query_to': environment.now(),
    }
    default_kwargs.update(kwargs)
    return LVGD01_TR2PROC_query.objects.create(**default_kwargs)
