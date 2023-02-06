# encoding: utf-8
from __future__ import unicode_literals

# Test case for imap_solog.

header = {'path': '/var/log/attach.tskv',
          'server': 'imap1a.mail.yandex.net'}

data_non_windat = """tskv\ttskv_format=mail-attach-log\tname=book.pdf\thid=10500\tfileType=application/pdf\tuid=1\tmid=2"""
data_windat = """tskv\ttskv_format=mail-attach-log\tname=winmail.dat\thid=10500\tfileType=ms-tnef\tuid=1\tmid=2"""
data_crap = """tskv\ttskv_format=mail-attach-log\thid=100500"""
