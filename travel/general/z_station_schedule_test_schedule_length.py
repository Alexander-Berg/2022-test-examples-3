#!/usr/bin/env python
# coding: utf-8

import travel.rasp.admin.scripts.load_project  # noqa

import os.path
from datetime import date
from optparse import OptionParser

from common.models.schedule import RThread
from travel.rasp.admin.lib.logs import get_script_log_context, ylog_context


script_name = os.path.basename(__file__)


def main(supplier_code, show_threads=False):
    threads = {}

    for thread in RThread.objects.filter(route__supplier__code=supplier_code):
        dates = thread.get_mask(today=date.today()).dates()
        length = (dates[-1] - dates[0]).days + 1

        threads.setdefault(length, []).append(thread.id)

    keys = list(threads.keys())
    keys.sort(reverse=True)

    print u"Длинна периода в днях\tКоличество ниток по этим параметрам"

    for key in keys:
        print key, len(threads[key]), show_threads and threads[key][:10]


usage = u"Usage: python %prog <supplier_code> [options]"

# Если файл запустили из консоли парсим параметры и запускаем скрипт
if __name__ == '__main__':
    with ylog_context(**get_script_log_context()):
        optparser = OptionParser(usage=usage, description=__doc__)
        optparser.add_option('-t', '--threads', action="store_true",
                             help=u"вывести первые 10 id ниток")

        options, args = optparser.parse_args()

        if not args:
            optparser.error('Please specify ')
        supplier_code = args[0]

        main(supplier_code, options.threads)
