# encoding: utf-8
from __future__ import unicode_literals
import json

# Test case for wmi_solog.
header = {'path': '/var/log/mops/so_report.tskv',
          'server': 'mops1a.mail.yandex.net'}

move = "tskv\ttskv_format=so_report\ttimestamp=1464705552\ttimezone=+0300\treceived_date=1434450713\tstid=1120000000330824\tclient=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36\tdest_folder=Спам\tfolder=Входящие\tip=2a02:6b8:0:3504:a9cb:ec69:b0b8:8878\tmid=2160000000011509010\tseen=yes\tsource=wmilog\tsuid=697023277\ttype=move\tuid=3000359278"
move_meta = {'envelopes': [{'mid': '2160000000011509010', 'stid': '1120000000330824', 'date': 1434450713}]}
move_payload = {
    'source': 'wmilog',
    'type': 'move',
    'json': json.dumps([{
        'dest_folder': 'Спам',
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '697023277',
        'unixtime': 1464705552,
        'stid': '1120000000330824',
        'mid': '2160000000011509010',
        'data': 1434450713,
        'client': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36',
        'karma': 0,
        'ip': '2a02:6b8:0:3504:a9cb:ec69:b0b8:8878',
        'seen': 'yes',
        'folder': 'Входящие',
        'login': 'test_user'
    }])
}

foo = "tskv\ttskv_format=so_report\ttimestamp=1464705557\ttimezone=+0300\treceived_date=1434450713\tstid=1120000000330824\tclient=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36\tfolder=Входящие\tip=2a02:6b8:0:3504:a9cb:ec69:b0b8:8878\tmid=2160000000011509009\tseen=yes\tsource=wmilog\tsuid=697023277\ttype=foo\tuid=3000359278"
foo_meta = {'envelopes': [{'mid': '2160000000011509009', 'stid': '1120000000330824', 'date': 1434450713}]}
foo_payload = {
    'source': 'wmilog',
    'type': 'foo',
    'json': json.dumps([{
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '697023277',
        'ip': '2a02:6b8:0:3504:a9cb:ec69:b0b8:8878',
        'stid': '1120000000330824',
        'mid': '2160000000011509009',
        'data': 1434450713,
        'client': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36',
        'karma': 0,
        'unixtime': 1464705557,
        'seen': 'yes',
        'folder': 'Входящие',
        'login': 'test_user'
    }])
}

antifoo = "tskv\ttskv_format=so_report\ttimestamp=1464705567\ttimezone=+0300\treceived_date=1434450713\tstid=1120000000330824\tclient=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36\tfolder=Спам\tip=2a02:6b8:0:3504:a9cb:ec69:b0b8:8878\tmid=2160000000004250247\tseen=yes\tsource=wmilog\tsuid=697023277\ttype=antifoo\tuid=3000359278"
antifoo_meta = {'envelopes': [{'mid': '2160000000004250247', 'stid': '1120000000330824', 'date': 1434450713}]}
antifoo_payload = {
    'source': 'wmilog',
    'type': 'antifoo',
    'json': json.dumps([{
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '697023277',
        'ip': '2a02:6b8:0:3504:a9cb:ec69:b0b8:8878',
        'stid': '1120000000330824',
        'mid': '2160000000004250247',
        'data': 1434450713,
        'client': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36',
        'karma': 0,
        'unixtime': 1464705567,
        'seen': 'yes',
        'folder': '\u0421\u043f\u0430\u043c',
        'login': 'test_user'
    }])
}

delete = "tskv\ttskv_format=so_report\ttimestamp=1464705584\ttimezone=+0300\treceived_date=1434450713\tstid=1120000000330824\tclient=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36\tfolder=Входящие\tip=2a02:6b8:0:3504:a9cb:ec69:b0b8:8878\tmid=2160000000011509009\tseen=no\tsource=wmilog\tsuid=697023277\ttype=delete\tuid=3000359278"
delete_meta = {'envelopes': [{'mid': '2160000000011509009', 'stid': '1120000000330824', 'date': 1434450713}]}
delete_payload = {
    'source': 'wmilog',
    'type': 'delete',
    'json': json.dumps([{
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '697023277',
        'ip': '2a02:6b8:0:3504:a9cb:ec69:b0b8:8878',
        'stid': '1120000000330824',
        'mid': '2160000000011509009',
        'data': 1434450713,
        'client': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 YaBrowser/16.4.1.8949 Yowser/2.5 Safari/537.36',
        'karma': 0,
        'unixtime': 1464705584,
        'seen': 'no',
        'folder': 'Входящие',
        'login': 'test_user'
    }])
}
