# -*- coding: utf-8 -*-

import unittest

import market.pylibrary.database as database
from sqlalchemy import select

import context
from market.idx.marketindexer.marketindexer import miconfig
from market.pylibrary.mindexerlib import sql
import market.idx.pylibrary.mindexer_core.stats.stats as stats


class TestGenerationStats(context.MysqlTestCase):
    def setUp(self):
        self.DS = miconfig.default().datasources
        context.create_table_from_description(self.DS, 'super', sql.generation_stats)

    def testGenerationStats_copy(self):
        def startswith_in(string, prefixes):
            for prefix in prefixes:
                if string.startswith(prefix):
                    return True
            return False

        # main functionality
        generation_name = '20140306_1608'
        stats_dict = {
            'generationlog-0-size': 2282,
            'generationlog-1-size': 2297,
            'generationlog-2-size': 2335,
            'generationlog-3-size': 2336,
            'generationlog-4-size': 2231,
            'generationlog-5-size': 2272,
            'generationlog-6-size': 2269,
            'generationlog-7-size': 2297,
            'generationlog-8-size': 9,
            'generationlog-size': 18328,
            'marketcorba-size': 3502,
            'marketsearch-size': 111788,
            'nocateg-offers': 2179001,
            'search-cards-size': 555,
            'search-part-0-size': 6984,
            'search-part-1-size': 6989,
            'search-part-10-size': 6979,
            'search-part-11-size': 6984,
            'search-part-12-size': 6975,
            'search-part-13-size': 6980,
            'search-part-14-size': 6978,
            'search-part-15-size': 6981,
            'search-part-2-size': 6998,
            'search-part-3-size': 6995,
            'search-part-4-size': 7001,
            'search-part-5-size': 6995,
            'search-part-6-size': 6993,
            'search-part-7-size': 6999,
            'search-part-8-size': 6977,
            'search-part-9-size': 6980,
            'search-report-data-size': 4049,
            'search-vcluster-report-data-size': 254,
            'search-vcluster-size': 763,
            'search-wizard-size': 1,
            'total-offers': 54770278,
        }

        gstats = stats.GenerationStatsSender()
        gstats.append_stats(generation_name, stats_dict)

        # first time
        gstats.copy_generation_stats_to_super_table('20140306_1608')
        # # reenterance with the same generation_name
        gstats.copy_generation_stats_to_super_table('20140306_1608')

        # checks
        super_connection = database.connect(**self.DS['super'])
        self.assertTrue(super_connection.has_table(sql.generation_stats),
                        msg='super.GENERATION_STATS table not automatically created')
        with super_connection.begin():
            result = super_connection.execute(select(
                [sql.generation_stats.c.generation_name, sql.generation_stats.c.name,
                 sql.generation_stats.c.value]))
            result_list = [r for r in result]
            self.assertEqual(36, len(result_list))
            for row in result_list:
                generation_name, name, value = row
                self.assertEqual('20140306_1608', generation_name)

                self.assertTrue(
                    startswith_in(name, [
                        'generationlog',
                        'marketcorba',
                        'marketsearch',
                        'nocateg',
                        'search-cards',
                        'search-part',
                        'search-report-data',
                        'search-vcluster-report',
                        'search-vcluster-size',
                        'search-wizard',
                        'total'
                    ])
                )

                if 'search-part-5-size' == name:
                    self.assertEqual(6995L, value)
                elif 'total-offers' == name:
                    self.assertEqual(54770278L, value)

    def test_total_size(self):
        stats_dict = {
            'generationlog-0-size': 1,
            'generationlog-1-size': 1,
            'generationlog-2-size': 1,
            'generationlog-3-size': 1,
            'generationlog-4-size': 1,
            'generationlog-5-size': 1,
            'generationlog-6-size': 1,
            'generationlog-7-size': 1,
            'generationlog-size': 8,
            'total-size': 8,
        }

        generation_name = '20140306_2208'

        gstats = stats.GenerationStatsSender()
        gstats.append_stats(generation_name, stats_dict)

        self.assertEqual(stats.get_total_size(generation_name), 8)


if '__main__' == __name__:
    unittest.main()
