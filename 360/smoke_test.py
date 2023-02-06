# -*- coding: utf-8 -*-

from django.conf import settings
from django.core.management.base import BaseCommand

from ...smoke import Smoke


class Command(BaseCommand):
    help = u"""Run smoke tests"""

    def handle(self, *args, **options):
        print 'Start smoke testing.\n'

        smoke = Smoke(settings)

        smoke.test_mysql_connection()
        print
        # smoke.test_mysql_grants()
        # print

        smoke.test_yauth_passport_connection()
        print
        smoke.test_yauth_passport_grants()
        print

        smoke.test_external_passport_connection()
        print
        smoke.test_external_passport_grants()
        print

        smoke.test_internal_passport_connection()
        print
        smoke.test_internal_passport_grants()
        print

        smoke.test_external_furita_connection()
        print
        smoke.test_internal_furita_connection()
        print

        smoke.test_external_yserver_settings_connection()
        print
        smoke.test_internal_yserver_settings_connection()
        print

        smoke.test_external_wma_connection()
        print

        smoke.test_internal_wma_connection()
        print

        smoke.test_company_cards_connection()
        print

        smoke.test_ava_avatars_connection()
        print

        smoke.test_reindex_db_connection()
        print

        smoke.test_recalculate_counters_connection()
        print

        smoke.test_rpop_collector_connection()
        print

        smoke.test_external_abook_connection()
        print
        smoke.test_internal_abook_connection()
        print

        smoke.test_make_imap_folders_english_connection()
        print

        smoke.test_antispam_logstore_connection()
        print

        # TODO: test center connection

        # smoke.test_memcache_connection()
        # print

        # smoke.test_smtp_connection()
        # print

        # smoke.test_avatars()
        # print

        # smoke.test_sentry_connection()
        # print

        print 'Finished smoke testing.\n'
