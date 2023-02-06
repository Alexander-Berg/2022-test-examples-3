# coding: utf-8

import pytest

from common.models.schedule import RThread, Route
from common.utils.date import RunMask
from travel.rasp.admin.scripts.clean_empty_threads import main
from tester.factories import create_thread


@pytest.mark.dbuser
def test_main():
    simple_thread = create_thread(year_days=RunMask())
    basic_thread = create_thread(year_days=RunMask())
    changes_thread = create_thread(basic_thread=basic_thread, year_days=RunMask.ALL_YEAR_DAYS)

    main()

    assert not RThread.objects.filter(pk=simple_thread.pk).exists()
    assert not Route.objects.filter(pk=simple_thread.route_id).exists()
    assert RThread.objects.filter(pk=basic_thread.pk).exists()
    assert RThread.objects.filter(pk=changes_thread.pk).exists()
