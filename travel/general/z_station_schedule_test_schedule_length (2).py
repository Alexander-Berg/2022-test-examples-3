# -*- encoding: utf-8 -*-

import travel.avia.admin.init_project  # noqa

import os.path
from datetime import date
from optparse import OptionParser

from travel.avia.library.python.common.models.schedule import RThread


script_name = os.path.basename(__file__)


def _main(supplier_code, show_threads=False):
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


def main():
    optparser = OptionParser(usage=usage, description=__doc__)
    optparser.add_option('-t', '--threads', action="store_true",
                         help=u"вывести первые 10 id ниток")

    options, args = optparser.parse_args()

    if not args:
        optparser.error('Please specify ')
    supplier_code = args[0]

    _main(supplier_code, options.threads)
