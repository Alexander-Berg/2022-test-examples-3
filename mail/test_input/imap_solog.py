# encoding: utf-8
from __future__ import unicode_literals
import json

# Test case for imap_solog.

header = {'path': '/var/log/yimap/imap_so_report.tskv',
          'server': 'imap1a.mail.yandex.net'}

spam = """tskv\ttskv_format=imap_so_report\ttimestamp=1464697306\ttimezone=+0300\tclient=\tdest_folder=\tfolder=Inbox\treceived_date=1434450713\tstid=1434241241231\tip=::ffff:130.193.61.94\tmid=2160000000004640567\tseen=yes\tsession=XIFgW00iA0Uq\tsource=imap\tsuid=1120000000282353\ttype=flag:$Junk\tuid=3000340278"""
spam_meta = {'envelopes': [{'mid': '2160000000004640567', 'stid': '1434241241231', 'date': 1434450713}]}
spam_payload = {
    'source': 'imaplog',
    'type': 'flag:$Junk',
    'json': json.dumps([{
        'dest_folder': '',
        'karma_status': 0,
        'uid': '3000340278',
        'suid': '1120000000282353',
        'data': 1434450713,
        'stid': '1434241241231',
        'mid': '2160000000004640567',
        'unixtime': 1464697306,
        'client': '',
        'karma': 0,
        'ip': '::ffff:130.193.61.94',
        'seen': 'yes',
        'login': 'test_user',
        'folder': 'Inbox'
    }])
}

not_spam = """tskv\ttskv_format=imap_so_report\ttimestamp=1464698388\ttimezone=+0300\tclient=("name" "Thunderbird" "version" "31.3.0")\treceived_date=1434450713\tstid=1434241241231\tdest_folder=\tfolder=Inbox\tip=::ffff:141.8.176.91\tmid=2160000000004640567\tseen=yes\tsession=KQFm830iNuQq\tsource=imap\tsuid=1120000000282353\ttype=flag:NonJunk\tuid=3000340278"""
not_spam_meta = {'envelopes': [{'mid': '2160000000004640567', 'stid': '1434241241231', 'date': 1434450713}]}
not_spam_payload = {
    'source': 'imaplog',
    'type': 'flag:NonJunk',
    'json': json.dumps([{
        'dest_folder': '',
        'karma_status': 0,
        'uid': '3000340278',
        'suid': '1120000000282353',
        'data': 1434450713,
        'stid': '1434241241231',
        'mid': '2160000000004640567',
        'unixtime': 1464698388,
        'client': '("name" "Thunderbird" "version" "31.3.0")',
        'karma': 0,
        'ip': '::ffff:141.8.176.91',
        'seen': 'yes',
        'login': 'test_user',
        'folder': 'Inbox'
    }])
}

move = """tskv\ttskv_format=imap_so_report\ttimestamp=1464697294\ttimezone=+0300\tclient=\tdest_folder=Spam\tfolder=Inbox\treceived_date=1434450713\tstid=1434241241231\tip=::ffff:141.8.161.86\tmid=2160000000004640567\tseen=no\tsession=VIFAV00isGkq\tsource=imap\tsuid=1120000000282353\ttype=move\tuid=3000340278"""
move_meta = {'envelopes': [{'mid': '2160000000004640567', 'stid': '1434241241231', 'date': 1434450713}]}
move_payload = {
    'source': 'imaplog',
    'type': 'move',
    'json': json.dumps([{
        'dest_folder': 'Spam',
        'karma_status': 0,
        'uid': '3000340278',
        'suid': '1120000000282353',
        'data': 1434450713,
        'stid': '1434241241231',
        'mid': '2160000000004640567',
        'unixtime': 1464697294,
        'client': '',
        'karma': 0,
        'ip': '::ffff:141.8.161.86',
        'seen': 'no',
        'login': 'test_user',
        'folder': 'Inbox'
    }])
}

expected = {}
