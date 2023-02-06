# -*- coding: utf-8 -*-

import unittest

import context
import www_feed_archives as feed_archives

HTML = '''<html>
<head><title>Index of /marketindexer/download/384058/</title></head>
<body bgcolor="white">
<h1>Index of /marketindexer/download/384058/</h1><hr><pre><a href="../">../</a>
<a href="20160529_1259/">20160529_1259/</a>                                     29-May-2016 13:03                   -
<a href="complete/">complete/</a>                                          29-May-2016 13:03                   -
<a href="recent/">recent/</a>                                            29-May-2016 13:03                   -
<a href="cached">cached</a>                                             29-May-2016 12:59            23693577
<a href="process_20160517_0050.log">process_20160517_0050.log</a>                          17-May-2016 00:53              227417
<a href="process_20160517_2051.log">process_20160517_2051.log</a>                          17-May-2016 20:51               19474
</pre><hr></body>
</html>'''

class Test(unittest.TestCase):
    def test_get_hosts_by_group(self):
        hosts = feed_archives.get_hosts_by_group(['mi_orn-testing'])
        hosts.sort()
        self.assertEqual(2, len(hosts))
        self.assertEqual('orn01ht.market.yandex.net', hosts[0])
        self.assertEqual('orn02ht.market.yandex.net', hosts[1])

        hosts = feed_archives.get_hosts_by_group([''])
        self.assertEqual(0, len(hosts))

    def test_extract_file_infos(self):
        regex = feed_archives.LOG_TEMPLATE.format(feed_archives.FEED_SESSION_REGEX)
        files = list(feed_archives.extract_file_infos(HTML, regex))
        files.sort(key=lambda item: item.session_name)

        self.assertEqual(2, len(files))
        self.assertEqual('20160517_0050', files[0].session_name)
        self.assertEqual('227417', files[0].size)
        self.assertEqual('20160517_2051', files[1].session_name)
        self.assertEqual('19474', files[1].size)

    def test_merge_infos(self):
        logs = [feed_archives.FileInfo('5K', '20141126_0610', 'orn01ht'), feed_archives.FileInfo('6K', '20141126_0910', 'orn02ht')]
        archives = [feed_archives.FileInfo('15K', '20141126_0910', 'orn02ht'), feed_archives.FileInfo('16K', '20141126_1910', 'orn01ht')]
        rows = list(feed_archives.merge_infos(logs, archives, '111'))

        rows.sort(key=lambda item: item[0])
        self.assertEqual(3, len(rows))

        self.assertEqual('20141126_0610', rows[0][0])
        self.assertEqual('<a href="http://orn01ht:3131/marketindexer/download/111/process_20141126_0610.log">process_20141126_0610.log</a>', rows[0][1])
        self.assertEqual('5K', rows[0][2])
        self.assertEqual('-', rows[0][3])
        self.assertEqual('-', rows[0][4])

        self.assertEqual('20141126_0910', rows[1][0])
        self.assertEqual('<a href="http://orn02ht:3131/marketindexer/download/111/process_20141126_0910.log">process_20141126_0910.log</a>', rows[1][1])
        self.assertEqual('6K', rows[1][2])
        self.assertEqual('<a href="http://orn02ht:3131/marketindexer/archive/111/20141126_0910.tar.gz">20141126_0910.tar.gz</a>', rows[1][3])
        self.assertEqual('15K', rows[1][4])

        self.assertEqual('20141126_1910', rows[2][0])
        self.assertEqual('-', rows[2][1])
        self.assertEqual('-', rows[2][2])
        self.assertEqual('<a href="http://orn01ht:3131/marketindexer/archive/111/20141126_1910.tar.gz">20141126_1910.tar.gz</a>', rows[2][3])
        self.assertEqual('16K', rows[2][4])


if __name__ == '__main__':
    unittest.main()
