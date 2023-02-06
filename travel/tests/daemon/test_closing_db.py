# -*- coding: utf-8 -*-
import pytest

from travel.avia.library.python.common.models.partner import Partner


@pytest.mark.skip("no way of currently testing this")
def test_fail_without_closing_db_contextmanager():
    with pytest.raises(RuntimeError):
        list(Partner.objects.all())
