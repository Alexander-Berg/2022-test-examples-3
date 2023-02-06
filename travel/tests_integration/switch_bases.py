# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import travel.rasp.admin.scripts.load_project  # noqa

import argparse
import logging
import mock
import os.path
from django.conf import settings

from travel.rasp.admin.lib.logs import create_current_file_run_log, print_log_to_stdout
from travel.rasp.admin.lib.maintenance.flags import flags, set_flag
from travel.rasp.admin.scripts.cron_switch_bases import switch_bases
from travel.rasp.admin.tests_integration.utils import get_log_file

log = logging.getLogger(__name__)


def switch_bases_step(log_file):
    log_file = get_log_file(log_file)

    if os.getenv('RASP_FORCE_NO_FLAG') == '1':
        flags['maintenance'] = 0
        set_flag('maintenance', 0, settings.WORK_DB)

    flags['switch'] = True
    log.info(u'Запускаем переключение баз.')
    switch_continue = os.getenv('RASP_SWITCH_CONTINUE') == '1'
    switch_bases(options=mock.Mock(no_prepare_all=False, is_continue=switch_continue), log_file=log_file)


def main():
    log_file = create_current_file_run_log()

    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--verbose', action='store_true', help=u'выводить лог на экран')

    args = parser.parse_args()
    if args.verbose:
        print_log_to_stdout()

    switch_bases_step(log_file)


if __name__ == '__main__':
    main()
