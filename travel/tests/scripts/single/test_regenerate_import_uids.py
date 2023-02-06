# coding: utf-8

from datetime import date

import mock
import pytest

from common.models.schedule import RThread
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from travel.rasp.admin.scripts.single.regenerate_import_uids import regenerate_import_uids
from tester.factories import create_thread
from tester.utils.datetime import replace_now


@replace_now('2015-01-01 00:00:00')
@pytest.mark.dbuser
def test_unite_duplicates():
    """
    Проверяем правильность склейки дней хождения для ниток дубликатов
    """
    today = environment.today()
    create_thread(year_days=str(RunMask(days=[date(2015, 1, 5)], today=today)))
    create_thread(year_days=str(RunMask(days=[date(2015, 1, 6)], today=today)))

    def new_gen_import_uid(self):
        self.import_uid = 'NEW COMMON IMPORT UID'
        return self.import_uid
    with mock.patch.object(RThread, 'gen_import_uid', new_gen_import_uid):
        regenerate_import_uids()

    thread = RThread.objects.get()
    assert thread.year_days == str(RunMask(days=[date(2015, 1, 5), date(2015, 1, 6)], today=today))
