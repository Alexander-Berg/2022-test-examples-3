# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

from io import BytesIO

import mock
import pytest
from hamcrest import assert_that, has_properties

from common.models.geo import CodeSystem
from common.models.schedule import TrainTurnover
from common.settings import WorkInstance
from common.tester.factories import create_station_code, create_train_schedule_plan, create_station
from travel.rasp.admin.importinfo.models.af import AFTrainTurnoverFile, AFTrainTurnoverUpdateFile
from travel.rasp.admin.lib.maintenance import flags
from tester.utils.replace_setting import replace_setting


create_station = create_station.mutate(t_type="suburban")

change_xml = """
<?xml version="1.0" encoding="Windows-1251"?>
<turnovers>
  <train_turnover graph_code="plan_code" station_esr="111" number_before="number_11" number_after="number_12"
   template_code="1234567" start_date="2017-10-3" end_date="2017-10-8" mode="change"/>
</turnovers>
""".strip()


load_xml = """
<?xml version="1.0" encoding="Windows-1251"?>
<turnovers>
  <train_turnover graph_code="plan_code" station_esr="111" number_before="number_11" number_after="number_12"
   template_code="1234567" start_date="2017-10-3" end_date="2017-10-8"/>
</turnovers>
""".strip()


def create_test_data():
    station = create_station()
    esr_code_system = CodeSystem.objects.get(code='esr')
    create_station_code(station=station, system=esr_code_system, code='111')
    create_train_schedule_plan(code='plan_code')


@pytest.mark.dbuser
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_aftrainturnoverupdatefile_add(superuser_client, tmpdir):
    create_test_data()
    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}), \
            mock.patch.object(flags, 'flags') as m_flags:
        m_flags.__getitem__.side_effect = lambda x: 0
        resp = superuser_client.post('/importinfo/aftrainturnoverupdatefile/add/', {
            'update_file': BytesIO(change_xml.encode('cp1251')),
            'update_file_name': 'change_xml.xml',
        })
    assert resp.status_code == 302
    turnover_file = AFTrainTurnoverUpdateFile.objects.get(update_file_name='change_xml.xml')
    assert_that(turnover_file,
                has_properties({
                    'update_file': change_xml,
                    'loaded': True
                }))
    assert TrainTurnover.objects.count() == 1


@pytest.mark.dbuser
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_aftrainturnoverfile_add(superuser_client, tmpdir):
    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}), \
            mock.patch.object(flags, 'flags') as m_flags:
        m_flags.__getitem__.side_effect = lambda x: 0
        resp = superuser_client.post('/importinfo/aftrainturnoverfile/add/', {
            'schedule_file': BytesIO(load_xml.encode('cp1251')),
            'schedule_file_name': 'load_xml.xml',
        })

    assert resp.status_code == 302
    turnover_file = AFTrainTurnoverFile.objects.get(schedule_file_name='load_xml.xml')
    assert_that(turnover_file,
                has_properties({
                    'schedule_file': load_xml,
                    'loaded': False
                }))
    assert TrainTurnover.objects.count() == 0
