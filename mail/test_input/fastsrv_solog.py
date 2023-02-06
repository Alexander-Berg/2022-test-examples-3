# encoding: utf-8
from __future__ import unicode_literals
import json

header = {'path': '/var/log/spam_reports.tskv',
          'server': 'yaback1j.mail.yandex.net'}

move = "tskv\ttskv_format=fastsrv-so-report\tsession=QXSJNSHF\tuid=3000359278\treceived_date=1434450713\tstid=1120000000330824\tsuid=879302453\tmid=159596311794943373\tip=141.8.161.8\tdst_folder=redFolder\tfolder=inbox\tseen=no\ttype=move\ttimestamp=1470070624\ttimezone=+0300"
move_meta = {'envelopes': [{'mid': '159596311794943373', 'stid': '1120000000330824', 'date': 1434450713}]}
move_payload = {
    'source': 'fastsrvlog',
    'type': 'move',
    'json': json.dumps([{
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '879302453',
        'ip': '141.8.161.8',
        'stid': '1120000000330824',
        'mid': '159596311794943373',
        'data': 1434450713,
        'dst_folder': 'redFolder',
        'karma': 0,
        'unixtime': 1470070624,
        'seen': 'no',
        'folder': 'inbox',
        'login': 'test_user'
    }])
}

flag = "tskv\ttskv_format=fastsrv-so-report\tsession=5kpae0sm\tuid=3000359278\treceived_date=1434450713\tstid=1120000000330824\tsuid=928007095\tmid=159596311794942869\tip=130.193.58.185\tdst_folder=\tfolder=inbox\tseen=no\ttype=flag:LABEL\ttimestamp=1470070637\ttimezone=+0300"
flag_meta = {'envelopes': [{'mid': '159596311794942869', 'stid': '1120000000330824', 'date': 1434450713}]}
flag_payload = {
    'source': 'fastsrvlog',
    'type': 'flag:LABEL',
    'json': json.dumps([{
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '928007095',
        'ip': '130.193.58.185',
        'stid': '1120000000330824',
        'mid': '159596311794942869',
        'data': 1434450713,
        'dst_folder': '',
        'karma': 0,
        'unixtime': 1470070637,
        'seen': 'no',
        'folder': 'inbox',
        'login': 'test_user'
    }])
}

filtered = "tskv\ttskv_format=fastsrv-so-report\tsession=EgYLSyqo\tuid=3000359278\treceived_date=1434450713\tstid=1120000000330824\tsuid=696107383\tmid=159596311794943022\tip=130.193.58.147\tdst_folder=\tfolder=spam\tseen=no\ttype=flag:отфильтровано\ttimestamp=1470070637\ttimezone=+0300"
filtered_meta = {'envelopes': [{'mid': '159596311794943022', 'stid': '1120000000330824', 'date': 1434450713}]}
filtered_payload = {
    'source': 'fastsrvlog',
    'type': 'flag:отфильтровано',
    'json': json.dumps([{
        'karma_status': 0,
        'uid': '3000359278',
        'suid': '696107383',
        'ip': '130.193.58.147',
        'stid': '1120000000330824',
        'mid': '159596311794943022',
        'data': 1434450713,
        'dst_folder': '',
        'karma': 0,
        'unixtime': 1470070637,
        'seen': 'no',
        'folder': 'spam',
        'login': 'test_user'
    }])
}
