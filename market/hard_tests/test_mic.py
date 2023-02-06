# -*- coding: utf-8 -*-
import os

import market.pylibrary.database as database
import market.pylibrary.filelock as filelock
import mock
from sqlalchemy import and_, select

import context
from context import create_workdir_test_environment, create_table_from_description
import datetime
import market.idx.pylibrary.mindexer_core.mic.mic as mic
from market.idx.marketindexer.marketindexer import miconfig
from market.pylibrary.mindexerlib import sql


def get_generations_dates(days):
    to_date = datetime.datetime(2018, 9, 13)

    # keep one per an half of hour
    count = days * 24 * 2
    return [to_date - datetime.timedelta(minutes=30*x) for x in range(1, count)]


def get_generation_name(date):
    return date.strftime("%Y%m%d_%H%M")


def make_generation_sql_record(date, hostname, state='complete'):
    return {
        sql.generations.c.name: get_generation_name(date),
        sql.generations.c.hostname: hostname,
        sql.generations.c.state: state,
        sql.generations.c.start_date: date,
    }


class TestMic(context.MysqlTestCase):
    def setUp(self):
        self.config = miconfig.default()
        self.DS = self.config.datasources
        self.HOSTNAME = sql.HOSTNAME
        self.INDEXER_WORKDIR = miconfig.default().working_dir
        self.create_test_environment()

    def create_db_test_environment(self):
        sql.setup_mysql_super(self.config)
        create_table_from_description(self.DS, 'super', sql.generations)
        create_table_from_description(self.DS, 'super', sql.blue_generations)
        create_table_from_description(self.DS, 'super', sql.generation_stats)
        create_table_from_description(self.DS, 'super', sql.blue_generation_stats)

    def create_test_environment(self):
        print("\nRECREATION TEST ENVIRONMENT\n")
        create_workdir_test_environment(self.INDEXER_WORKDIR)
        self.create_db_test_environment()

    def test_general_without_collections(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1430'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1500'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1530'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1600'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1630'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1700'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1730'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1800'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'cards'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'wizard'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'model'))

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20130910_1430', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed'},
                {sql.generations.c.name: '20130910_1500', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1530', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed'},
                {sql.generations.c.name: '20130910_1600', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed'},
                {sql.generations.c.name: '20130910_1630', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1700', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1701', sql.generations.c.hostname: 'not-yandex-machine',
                 sql.generations.c.state: 'failed'},
                {sql.generations.c.name: '20130910_1730', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1800', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'started'}
            ]))

        micl = mic.MI_Cleaner()
        micl.tidy_up(failed_count=1, complete_count=2)

        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1430')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1500')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1530')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1630')))

        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1600')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1700')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1730')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130910_1800')))

        with super_connection.begin():
            result = super_connection.execute(select([sql.generations.c.state])
                                              .where(and_(sql.generations.c.name == '20130910_1630',
                                                          sql.generations.c.hostname == self.HOSTNAME)))
            self.assertEqual(result.fetchone()[0], 'distonly')

    def test_freeze_basic(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1430'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1500'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1530'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130910_1600'))

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20130910_1430', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1500', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1530', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130910_1600', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
            ]))

        micl = mic.MI_Cleaner()

        micl.freeze('20130910_1430')

        with super_connection.begin():
            result = super_connection.execute(select([sql.generations.c.state])
                                              .where(and_(sql.generations.c.name == '20130910_1430',
                                                          sql.generations.c.hostname == self.HOSTNAME)))
            self.assertEqual(result.fetchone()[0], 'frozen')

    def test_general_with_collections(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130905_0950'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130905_1020'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130905_1041'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20130911_1200'))

        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'offers'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_1041'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_1020'))
        os.symlink(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_1020'),
                   os.path.join(self.INDEXER_WORKDIR, 'offers', 'recent'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_1020', 'config'))
        with open(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_1020', 'config', 'collection.json'), 'w') as f:
            f.write(context.COLLECTION_JSON_CONTENT)
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_0950'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_0950', 'config'))
        with open(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_0950', 'config', 'collection.json'), 'w') as f:
            f.write(context.COLLECTION_JSON_CONTENT)

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20130905_0950', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130905_1020', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130905_1041', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20130911_1200', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'}
            ]))

        publisher_lock = filelock.FileLock(os.path.join(self.INDEXER_WORKDIR, '20130905_0950'))
        publisher_lock.lock()

        micl = mic.MI_Cleaner()
        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            micl.tidy_up(failed_count=1, complete_count=2)

        publisher_lock.unlock()

        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130905_0950')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_0950')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20130905_1020')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, 'offers', '20130905_1020')))

    def test_not_clean_last_completed(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1430'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1500'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1600'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1630'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'cards'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'wizard'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'model'))
        os.symlink(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'),
                   os.path.join(self.INDEXER_WORKDIR, 'last_complete'))

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20131221_1430', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20131221_1500', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20131221_1530', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20131221_1600', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed'},
                {sql.generations.c.name: '20131221_1630', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'started'}
            ]))

        micl = mic.MI_Cleaner()
        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            micl.tidy_up(failed_count=0, complete_count=2, clean_generations=True)

        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1430')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1500')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1530')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1600')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1630')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, 'last_complete', 'cleaned')))

        with super_connection.begin():
            result = super_connection.execute(select([sql.generations.c.state])
                                              .where(and_(sql.generations.c.name == '20131221_1530',
                                                          sql.generations.c.hostname == self.HOSTNAME)))
            self.assertEqual(result.fetchone()[0], 'complete')

    def test_no_clean(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1430'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1500'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1600'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1630'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'cards'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'wizard'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'model'))
        os.symlink(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'),
                   os.path.join(self.INDEXER_WORKDIR, 'last_complete'))

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20131221_1430', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20131221_1500', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20131221_1530', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete'},
                {sql.generations.c.name: '20131221_1600', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed'},
                {sql.generations.c.name: '20131221_1630', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'started'}
            ]))

        micl = mic.MI_Cleaner()
        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            micl.tidy_up(failed_count=0, complete_count=2, clean_generations=False)

        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1430')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1500')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1530')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1600')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1630')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1500')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, 'last_complete', 'cleaned')))

        with super_connection.begin():
            result = super_connection.execute(select([sql.generations.c.state])
                                              .where(and_(sql.generations.c.name == '20131221_1530',
                                                          sql.generations.c.hostname == self.HOSTNAME)))
            self.assertEqual(result.fetchone()[0], 'complete')

    def test_no_clean_last_half_mode(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1430'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1500'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1600'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1630'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'cards'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'wizard'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'model'))
        os.symlink(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'),
                   os.path.join(self.INDEXER_WORKDIR, 'last_complete'))

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20131221_1430', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete', sql.generations.c.half_mode: True},
                {sql.generations.c.name: '20131221_1500', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete', sql.generations.c.half_mode: True},
                {sql.generations.c.name: '20131221_1530', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete', sql.generations.c.half_mode: False},
                {sql.generations.c.name: '20131221_1600', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed', sql.generations.c.half_mode: True},
                {sql.generations.c.name: '20131221_1630', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'started', sql.generations.c.half_mode: True}
            ]))

        micl = mic.MI_Cleaner()
        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            micl.tidy_up(failed_count=0, complete_count=1, keep_half=1, clean_generations=True)

        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1430')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1500')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1530')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1600')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1630')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, 'last_complete', 'cleaned')))

    def test_clean_all_half_mode(self):
        # filesystem
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1430'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1500'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1600'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, '20131221_1630'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'cards'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'wizard'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'model'))
        os.symlink(os.path.join(self.INDEXER_WORKDIR, '20131221_1530'),
                   os.path.join(self.INDEXER_WORKDIR, 'last_complete'))

        # db
        super_connection = database.connect(**self.DS['super'])
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=[
                {sql.generations.c.name: '20131221_1430', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete', sql.generations.c.half_mode: True},
                {sql.generations.c.name: '20131221_1500', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete', sql.generations.c.half_mode: True},
                {sql.generations.c.name: '20131221_1530', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'complete', sql.generations.c.half_mode: False},
                {sql.generations.c.name: '20131221_1600', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'failed', sql.generations.c.half_mode: True},
                {sql.generations.c.name: '20131221_1630', sql.generations.c.hostname: self.HOSTNAME,
                 sql.generations.c.state: 'started', sql.generations.c.half_mode: True}
            ]))

        micl = mic.MI_Cleaner()
        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            micl.tidy_up(failed_count=0, complete_count=1, keep_half=0, clean_generations=True)

        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1430')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1500')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1530')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1600')))
        self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, '20131221_1630')))
        self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, 'last_complete', 'cleaned')))

    def prepare_generations(self, complete_dates, failed_dates):
        # filesystem
        for date in complete_dates:
            os.mkdir(os.path.join(self.INDEXER_WORKDIR, get_generation_name(date)))
        for date in failed_dates:
            os.mkdir(os.path.join(self.INDEXER_WORKDIR, get_generation_name(date)))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'wizard'))
        os.mkdir(os.path.join(self.INDEXER_WORKDIR, 'model'))
        os.symlink(os.path.join(self.INDEXER_WORKDIR, get_generation_name(complete_dates[0])),
                   os.path.join(self.INDEXER_WORKDIR, 'last_complete'))

        # db
        super_connection = database.connect(**self.DS['super'])
        failed_records = [make_generation_sql_record(date, self.HOSTNAME, 'failed') for date in reversed(failed_dates)]
        complete_records = [make_generation_sql_record(date, self.HOSTNAME) for date in reversed(complete_dates)]
        with super_connection.begin():
            super_connection.execute(sql.generations.insert(values=failed_records + complete_records))

    def test_clean_daily_and_weekly(self):

        """
        Tests that MI_Cleaner correctly cleans daily and weekly generations

        Creates at the filesystem and in the db records about generations:
        1) complete generations over several days
        2) one fail generation (with very far date to test that start date calculation takes into account only complete
        generations

        runs cleaner with non empty weekly and daily requirements (where days count > weeks count * 7)

        checks that all needed generations are not deleted:
        1) N last complete generations
        2) M last failed generation
        3) 1 generation per day for K days since min_date(kept last complete generations) - 1 day
        4) 1 generation per week for J weeks since min_date(kept daily generations) - 1 week

        Also checks that every other generations are deleted
        """

        dates = get_generations_dates(40)
        failed_date = dates[-1] - datetime.timedelta(minutes=30)

        self.prepare_generations(dates, [failed_date])

        mi_cleaner = mic.MI_Cleaner()

        complete_count = 3
        keep_daily = 10
        keep_weekly = 1
        failed_count = 1

        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            mi_cleaner.tidy_up(failed_count=failed_count, complete_count=complete_count, keep_daily=keep_daily,
                               keep_weekly=keep_weekly, keep_half=0, clean_generations=True)
        last_generation = dates[0]
        daily_generations = [{'date': last_generation - datetime.timedelta(days=i+1), 'reason': '{} daily'.format(i+1)}
                             for i in range(keep_daily)]

        week_before_daily_last_generation = daily_generations[-1]['date'] + datetime.timedelta(
            days=6 - daily_generations[-1]['date'].weekday()) - datetime.timedelta(weeks=1)

        complete_generations = [{'date': date, 'reason': 'last complete'} for date in dates[:complete_count]]

        should_be_kept = complete_generations + daily_generations + [
            {'date': week_before_daily_last_generation, 'reason': 'first weekly'},
            {'date': failed_date, 'reason': 'failed count'}]
        should_be_removed = set(dates) - {generation['date'] for generation in should_be_kept}

        for generation in should_be_kept:
            self.assertTrue(os.path.exists(os.path.join(self.INDEXER_WORKDIR, get_generation_name(generation['date']))),
                            "generation should be kept: {} by the reason {}".format(generation['date'],
                                                                                    generation['reason']))
        for date in should_be_removed:
            self.assertFalse(os.path.exists(os.path.join(self.INDEXER_WORKDIR, get_generation_name(date))),
                             "generation should be removed: {}".format(date))

    def test_get_generations_by_config(self):

        """
        Tests that MI_Cleaner correctly returns generation list kept by config

        Fetching of generations by config is used for cleaning on workers
        Algorithms for workers and for master are different, so it checks that kept by days and weeks generations
        intersects with kept by complete count
        """
        dates = get_generations_dates(40)
        failed_date = dates[-1] - datetime.timedelta(minutes=30)

        self.prepare_generations(dates, [failed_date])

        config = miconfig.default()
        config.keep_complete = 2
        config.keep_failed = 1
        config.keep_complete_daily = 1
        config.keep_complete_weekly = 1

        mi_cleaner = mic.MI_Cleaner(config=config)

        with mock.patch('market.idx.pylibrary.mindexer_core.mifrpc.mifrpc.start_and_wait_tasks'):
            kept_generations = mi_cleaner.get_keep_generations_by_config()

        complete_generations = [get_generation_name(date) for date in dates[:config.keep_complete]]

        should_be_kept = complete_generations + [get_generation_name(failed_date)]

        self.assertItemsEqual(kept_generations.keys(), should_be_kept)
