# -*- coding: utf-8 -*-
from __future__ import absolute_import

import pytest

from common.models.transport import TransportSubtype, TransportType
from travel.rasp.admin.scripts.schedule.af_processors.suburban.utils import copy_thread_attributes
from tester.factories import create_thread


@pytest.mark.dbuser
def test_copy_thread_attributes():
    t_subtype = TransportSubtype.objects.create(t_type_id=TransportType.BUS_ID, code='aaa', title_ru='aaa')
    thread = create_thread(t_subtype=t_subtype, t_type='bus')
    destination_thread = create_thread(t_type='bus')
    assert destination_thread.t_subtype != t_subtype

    copy_thread_attributes(destination_thread, thread)
    assert destination_thread.t_subtype == t_subtype
