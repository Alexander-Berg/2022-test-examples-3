# -*- coding: utf-8 -*-

import os
import unittest
import random
from lxml import etree

from market.idx.mif.configurator.configurator import XmlConfigGenerator
from market.idx.mif.configurator.configurator import Configurator
from market.idx.mif.configurator.configurator import iter_feedid_session_noffers
from market.idx.mif.configurator.configurator import generate_index_scheme_std
from market.idx.mif.mif.util import rm


class TestXmlConfig(unittest.TestCase):
    TMPDIR = 'tmp'
    FEEDSESSION_PATH = os.path.join(TMPDIR, 'feed_session')
    feedid_session_noffers = [
        ('1', 'session1', 'session1', 'base1', 10),
        ('2', 'session2', 'session3', 'base2', 20),
        ('3', 'session3', 'session4', 'base3', 30),
        ('4', '', 'session0', 'base4', 0),
        ('5', 'bla-bla', 'bla-bla', 'base5', 0)
        ]

    hashdiv16 = [
        ['0', '21267647932558653966460912964485513216'],
        ['21267647932558653966460912964485513216', '42535295865117307932921825928971026432'],
        ['42535295865117307932921825928971026432', '63802943797675961899382738893456539648'],
        ['63802943797675961899382738893456539648', '85070591730234615865843651857942052864'],
        ['85070591730234615865843651857942052864', '106338239662793269832304564822427566080'],
        ['106338239662793269832304564822427566080', '127605887595351923798765477786913079296'],
        ['127605887595351923798765477786913079296', '148873535527910577765226390751398592512'],
        ['148873535527910577765226390751398592512', '170141183460469231731687303715884105728'],
        ['170141183460469231731687303715884105728', '191408831393027885698148216680369618944'],
        ['191408831393027885698148216680369618944', '212676479325586539664609129644855132160'],
        ['212676479325586539664609129644855132160', '233944127258145193631070042609340645376'],
        ['233944127258145193631070042609340645376', '255211775190703847597530955573826158592'],
        ['255211775190703847597530955573826158592', '276479423123262501563991868538311671808'],
        ['276479423123262501563991868538311671808', '297747071055821155530452781502797185024'],
        ['297747071055821155530452781502797185024', '319014718988379809496913694467282698240'],
        ['319014718988379809496913694467282698240', '0'],
        ]

    def _make_feed_sessions(self, count):
        def make_line(n):
            sess = 'session%d' % n
            return (str(n).zfill(9), sess, sess, '', random.randint(0, 15000))
        return [make_line(i) for i in xrange(1, count+1)]

    def setUp(self):
        rm(self.TMPDIR)
        os.mkdir(self.TMPDIR)
        lines = [','.join(map(str, list(l))) for l in self.feedid_session_noffers]
        content = '\n'.join(lines)
        with open(self.FEEDSESSION_PATH, 'w') as f:
            f.write(content)

    def tearDown(self):
        rm(self.TMPDIR)

    def _check_feed(self, efeed, feedid, published_session, finished_session, base_session):
        self.assertEqual(efeed.find('id').text, feedid)
        for name, value in (('published_session', published_session),
                            ('finished_session', finished_session),
                            ('base_session', base_session)):

            if value is None:
                self.assertEqual(efeed.find(name), None)
            else:
                self.assertEqual(efeed.find(name).text, str(value))

    def _check_hbase_index(self, eindex, part_num, shard_borders):
        self.assertEqual(eindex.xpath('part_num')[0].text, str(part_num))
        ehash_ranges = eindex.findall('hash_range')
        ehash_ranges.sort(key=lambda erange: long(erange.find('range_start').text))
        self.assertEqual(len(ehash_ranges), len(shard_borders))
        for n, borders in enumerate(shard_borders):
            self.assertEqual(ehash_ranges[n].xpath('range_start')[0].text, borders[0])
            self.assertEqual(ehash_ranges[n].xpath('range_end')[0].text, borders[1])

    def _check_yt_index(self, eindex, part_num, expected_table_nums):
        self.assertEqual(eindex.xpath('part_num')[0].text, str(part_num))
        etables = eindex.findall('table')
        self.assertEqual(len(etables), len(expected_table_nums))
        for n, expected_num in enumerate(expected_table_nums):
            expected_name = '//test/mi3/main/generation/offers/{}'.format(format(expected_num, '04d'))
            self.assertEqual(etables[n].text, expected_name)

    def _calc_num_offers(self, feedid_session_noffers_iter=None):
        class FeedConfig(object):

            def __init__(self, feed_id, published_session, finished_session, base_session, noffers):
                self.feed_id = feed_id
                self.published_session = published_session
                self.finished_session = finished_session
                self.base_session = base_session
                self.noffers = noffers

        session = self.feedid_session_noffers
        num_offers = 0
        if feedid_session_noffers_iter is not None:
            session = feedid_session_noffers_iter
        for fields in session:
            feed = FeedConfig(*fields)
            num_offers += feed.noffers
        return num_offers

    def _create_generator(self, nhosts, nparts, iter,
                          generation_type='full', hbase_host=None, hbase_port=None, use_yt=False,
                          feed_max_offers_percent=100, feed_guaranteed_offers_count=0, fake_msku_offers_count=0):
        index_scheme = generate_index_scheme_std(nhosts, nparts)

        return XmlConfigGenerator(index_scheme=index_scheme,
                                  generation='generation',
                                  base_generation='base_generation' if generation_type == 'diff' else '',
                                  collection='collection',
                                  feedid_session_noffers_iter=iter,
                                  threads_per_worker=255,
                                  ts_offset_start=555,
                                  shopsdat='shops-utf8.dat.report.generated',
                                  hbase_host=hbase_host,
                                  hbase_port=hbase_port,
                                  feeds_table='feeds',
                                  sessions_table='sessions',
                                  offers_table='offers',
                                  offers_diff_table='offers-diff',
                                  categories_table='categories',
                                  use_yt=use_yt,
                                  yt_home_dir='//test',
                                  feed_max_offers_percent=feed_max_offers_percent,
                                  feed_guaranteed_offers_count=feed_guaranteed_offers_count,
                                  fake_msku_offers_count=fake_msku_offers_count)

    def _create_hbase_yt_generators(self, **kwargs):
        patched_kwargs = kwargs

        patched_kwargs['use_yt'] = False
        hbase_generator = self._create_generator(**patched_kwargs)

        patched_kwargs['use_yt'] = True
        yt_generator = self._create_generator(**patched_kwargs)

        return hbase_generator, yt_generator

    def _make_iters(self):
        return [self.feedid_session_noffers,
                iter_feedid_session_noffers(self.FEEDSESSION_PATH)]

    def _generate_indexes(self, generator, nworker):
        config_data = generator.generate(self._calc_num_offers(), nworker)
        eroot = etree.fromstring(config_data)
        eindexes = eroot.findall('./indexes/index')
        eindexes.sort(key=lambda eindex: int(eindex.find('part_num').text))
        return eindexes

    def test(self):
        def get_feed_id(feed):
            return feed.find('id').text

        for iter in self._make_iters():
            generator = self._create_generator(nhosts=2, nparts=3, iter=iter)
            config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
            eroot = etree.fromstring(config_data)

            self.assertEqual(eroot.xpath('/config/threads_number')[0].text, '255')
            self.assertEqual(eroot.xpath('/config/ts_offset')[0].text, '555')
            self.assertEqual(eroot.xpath('/config/shopsdat')[0].text,
                             'shops-utf8.dat.report.generated')

            config_data = generator.generate(self._calc_num_offers(), 1, 'offers', generator.index_scheme)
            eroot = etree.fromstring(config_data)
            self.assertEqual(eroot.xpath('/config/threads_number')[0].text, '255')
            self.assertEqual(eroot.xpath('/config/shopsdat')[0].text,
                             'shops-utf8.dat.report.generated')

    def test_exceed_max_ts(self):
        host = 'hbasehost'
        port = 12345

        feedid_session_noffers = [
            ('1', 'session1', 'session1', '', 2 ** 31 - 557),
            ('2', 'session2', 'session3', '', 1)]

        generator = self._create_generator(nhosts=2, nparts=3, iter=feedid_session_noffers,
                                           hbase_host=host, hbase_port=port)

        config_data = generator.generate(self._calc_num_offers(feedid_session_noffers), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/ts_offset')[0].text, '555')
        self.assertRaises(RuntimeError, generator.generate, self._calc_num_offers(feedid_session_noffers), 1, 'offers', generator.index_scheme)

    def test_generation_type(self):
        generator = self._create_generator(nhosts=2, nparts=4, iter=self.feedid_session_noffers,
                                           hbase_host='127.0.0.1', hbase_port=9090, generation_type='full')
        config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/generation_type')[0].text, 'full')

        generator = self._create_generator(nhosts=2, nparts=4, iter=self.feedid_session_noffers,
                                           hbase_host='127.0.0.1', hbase_port=9090, generation_type='diff')
        config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/generation_type')[0].text, 'diff')

    def test_ts_offset(self):
        generator = self._create_generator(nhosts=1, nparts=4, iter=self.feedid_session_noffers,
                                           use_yt=True)
        config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/ts_offset')[0].text, '555')
        self.assertEqual(eroot.xpath('/config/ts_offset_end')[0].text, '627')

        generator = self._create_generator(nhosts=1, nparts=4, iter=self.feedid_session_noffers,
                                           use_yt=True)
        config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/ts_offset')[0].text, '555')
        self.assertEqual(eroot.xpath('/config/ts_offset_end')[0].text, '627')

        generator = self._create_generator(nhosts=2, nparts=4, iter=self.feedid_session_noffers, use_yt=True)
        config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/ts_offset')[0].text, '555')
        self.assertEqual(eroot.xpath('/config/ts_offset_end')[0].text, '591')

        generator = self._create_generator(nhosts=1, nparts=4, iter=self.feedid_session_noffers,
                                           use_yt=True, fake_msku_offers_count=20)
        config_data = generator.generate(self._calc_num_offers(), 0, 'offers', generator.index_scheme)
        eroot = etree.fromstring(config_data)
        self.assertEqual(eroot.xpath('/config/ts_offset')[0].text, '555')
        self.assertEqual(eroot.xpath('/config/ts_offset_end')[0].text, '627')


class TestConfigurator(unittest.TestCase):

    def _gen_collections_config(self, nparts, nhosts):
        c = Configurator(nhosts, nparts)
        return c._generate_collection_config(hosts=['host-{}'.format(n) for n in xrange(nhosts)],
                                             dist_prefix='search-part-',
                                             search_part_dist_prefixes=['search-part-base-', 'search-part-additions-'],
                                             mif_port=11111)

    def _check_worker_config(self, worker, parts):
        self.assertEqual(worker['parts'], parts)
        self.assertEqual(worker['dists'], ['search-part-{}'.format(n) for n in parts])

    def test_mif_port(self):
        workers = self._gen_collections_config(nparts=3, nhosts=2)
        self.assertEqual(workers[0]['port'], 11111)
        self.assertEqual(workers[1]['port'], 11111)

    def test_auto_mode(self):
        workers = self._gen_collections_config(nparts=3, nhosts=2)
        self.assertEqual(len(workers), 2)
        self._check_worker_config(workers[0], [0])
        self._check_worker_config(workers[1], [1, 2])

    def test_manual_mode(self):
        workers = self._gen_collections_config(nparts=3, nhosts=2)
        self.assertEqual(len(workers), 2)
        self._check_worker_config(workers[0], [0])
        self._check_worker_config(workers[1], [1, 2])

    def test_like_big_testing(self):
        workers = self._gen_collections_config(nparts=16, nhosts=8)
        self.assertEqual(len(workers), 8)
        for n, worker in enumerate(workers):
            self._check_worker_config(worker, [n*2, n*2 + 1])

    def test_like_prod(self):
        workers = self._gen_collections_config(nparts=16, nhosts=16)
        self.assertEqual(len(workers), 16)
        for n, worker in enumerate(workers):
            self._check_worker_config(worker, [n])

    def test_one_host_auto(self):
        workers = self._gen_collections_config(nparts=16, nhosts=1)
        self.assertEqual(len(workers), 1)
        self._check_worker_config(workers[0], [n for n in xrange(16)])


if __name__ == '__main__':
    unittest.main()
