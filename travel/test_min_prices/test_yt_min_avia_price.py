# -*- coding: utf-8 -*-

from travel.rasp.tasks.min_prices.yt_min_avia_price import UniqueReducer, YtMinAviaPrice


def test_simple_runner():
    runner = YtMinAviaPrice({'RUR': 1, 'USD': 65, 'EUR': 70},
                            0, 1,
                            '', '', '', '', '')
    row_start = {'timestamp': '2016-06-28 12:05:33',
                 'date_forward': '2016-08-12',
                 'adults': '1',
                 'type': 'train',
                 'class_economy_price': '62975.0 RUR',
                 'class_business_price': None,
                 'object_from_id': '54',
                 'object_from_type': 'Settlement',
                 'object_to_id': '10633',
                 'object_to_type': 'Station',
                 'route_uid': 'QR 5727',
                 'key': '',
                 'class': 'economy',
                 'seats': '1',
                 '@table_index': 1}
    row = row_start.copy()
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    row['price'] = '62975.0'
    del row['@table_index']
    assert res == [row]

    row = row_start.copy()
    row['date_backward'] = None
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    row['price'] = '62975.0'
    del row['@table_index']
    assert res == [row]

    row = row_start.copy()
    row['adults'] = '2'
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    row['price'] = '31487.5'
    del row['@table_index']
    assert res == [row]

    row = row_start.copy()
    row['adults'] = None
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    row['price'] = '62975.0'
    del row['@table_index']
    row['adults'] = '1'
    assert res == [row]

    row = row_start.copy()
    row['route_uid'] = 'QR 5727;QR 234;QR 656;QR 657;QR 233;QR 5734'
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    assert res == []

    row = row_start.copy()
    row['route_uid'] = 'bla-bla-car'
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    assert res == []

    row = row_start.copy()
    row['date_backward'] = '2016-09-12'
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    assert res == []

    row = row_start.copy()
    row['date_forward'] = None
    row_copy = row.copy()
    res = [r for r in runner.simple_runner(row_copy)]
    assert res == []


def test_UniqueReducer():
    ur = UniqueReducer()
    row = {'timestamp': '2016-06-28 12:05:33',
           'date_forward': '2016-08-12',
           'price': '62975.0',
           'object_from_id': '54',
           'object_from_type': 'Settlement',
           'object_to_id': '10633',
           'object_to_type': 'Station',
           'route_uid': 'QR 5727',
           'key': '',
           'class': 'economy',
           'seats': '1',
           'type': 'bus'}
    rows = [row.copy()]
    row['price'] = '1'
    row['timestamp'] = '1'
    rows.append(row.copy())
    row['price'] = '2'
    row['timestamp'] = '0'
    rows.append(row.copy())
    row['price'] = '50000'
    row['timestamp'] = '2016-06-28 12:05:33'
    rows.append(row.copy())
    res = [r for r in ur((), rows)]
    assert res == [row]
