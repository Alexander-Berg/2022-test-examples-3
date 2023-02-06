# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import travel.rasp.admin.scripts.load_project  # noqa

import argparse
import logging
import mock
import os.path
from django.conf import settings

from travel.rasp.library.python.common23.models.core.schedule.rthread import RThread
from travel.rasp.admin.lib.logs import create_current_file_run_log, print_log_to_stdout
from travel.rasp.admin.scripts.cron_schedule import big_import
from travel.rasp.admin.tests_integration.utils import get_log_file

log = logging.getLogger(__name__)


def big_import_step(log_file):
    log_file = get_log_file(log_file)

    if os.getenv('RASP_FORCE_NO_FLAG') == '1':
        from travel.rasp.admin.lib.maintenance.flags import flags
        flags['maintenance'] = 0

    warning_action = settings.SCRIPT_WARNING_ACTION
    import_scripts_list = [(
        {'base_path': settings.SCRIPTS_PATH},
        [
            (True, ['python', '-W', warning_action, 'schedule/tis_train/import_tis.py', 'import']),
            (True, ['python', '-W', warning_action, 'schedule/bus/import_mta.py', '--import-from-default-file']),
            (True, ['python', '-W', warning_action, 'schedule/bus/two_stage_autoimport.py']),
            (True, ['python', '-W', warning_action, 'schedule/red_autoimport.py']),
            (True, ['python', '-W', warning_action, 'schedule/re_import_af_schedule.py', '-v', 'suburban',
                    '--ignore-maintenance-flag']),

            # проверяем, что нужные данные импортировались
            (True, check_imported_data)
        ]
    )]

    bi_continue = os.getenv('RASP_BI_CONTINUE') == '1'
    with mock.patch('travel.rasp.admin.scripts.cron_schedule.get_import_scripts_list', return_value=import_scripts_list):
        log.info('Запускаем БИ.')
        big_import(is_continue=bi_continue, exit_on_fail=False)


def check_two_stage_autoimport_imported_data():
    assert RThread.objects.filter(route__two_stage_package__isnull=False, t_type__code='bus').exists()
    assert RThread.objects.filter(route__two_stage_package__isnull=False, t_subtype__code='sea').exists()
    assert RThread.objects.filter(route__two_stage_package__isnull=False, t_subtype__code='river').exists()


def check_red_imported_data():
    assert RThread.objects.filter(route__red_metaroute__isnull=False, t_type__code='bus').exists()
    assert RThread.objects.filter(route__red_metaroute__isnull=False, t_subtype__code='sea').exists()
    assert RThread.objects.filter(route__red_metaroute__isnull=False, t_subtype__code='river').exists()


def check_tis_imported_data():
    assert RThread.objects.filter(supplier__code='tis').count() == 9
    assert {t.number for t in RThread.objects.filter(supplier__code='tis')} == {
        '001А',
        '001Б',
        '001Г',
        '001Г',
        '001Ж',
        '001И',
        '001М',
        '001Р',
        '001Э',
    }


def check_suburban_imported_data():
    assert RThread.objects.filter(supplier__code='af').count() == 136


def check_mta_imported_data():
    mta_threads_count = RThread.objects.filter(route__two_stage_package=None, supplier__code='mta').count()
    log.info('MTA threads: %d', mta_threads_count)
    assert mta_threads_count == 42


def check_imported_data():
    check_two_stage_autoimport_imported_data()
    check_tis_imported_data()
    check_mta_imported_data()
    check_red_imported_data()
    check_suburban_imported_data()


def main():
    log_file = create_current_file_run_log()

    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--verbose', action='store_true', help=u'выводить лог на экран')

    args = parser.parse_args()
    if args.verbose:
        print_log_to_stdout()

    big_import_step(log_file)


if __name__ == '__main__':
    main()
