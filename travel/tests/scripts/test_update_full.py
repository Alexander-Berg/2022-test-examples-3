# -*- coding: utf-8 -*-

import mock
import pytest
from datetime import datetime, date

from travel.rasp.suburban_tasks.tests.factories import create_full_strains, create_current_strains, create_update, \
    create_current_strainsvar, create_full_strainsvar, create_current_srasprp, create_full_srasprp, \
    create_current_scalendar, create_full_scalendar, create_ic00_stan, IC00_STAN_Factory, STRAINS_Factory, \
    STRAINSVAR_Factory, SRASPRP_Factory, SCALENDAR_Factory, SDOCS_Factory
from common.tester.utils.datetime import replace_now

from travel.rasp.suburban_tasks.suburban_tasks.models import Update, Current_STRAINS, Current_STRAINSVAR, Current_SRASPRP, Current_SCALENDAR, IC00_STAN, \
    Full_STRAINS, Full_STRAINSVAR, Full_SRASPRP, Full_SCALENDAR, Full_SDOCS
from travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full import set_last_gvc_date, update_full_update_tables, apply_full_update, \
    HasFreshImportException, reload_stations, update_rzd_tables


def build_set_last_gvc_date_side_effect(last_gvc_date, is_fake_gvc_date=False):
    def side_effect(update):
        update.last_gvc_date = last_gvc_date
        update.is_fake_gvc_date = is_fake_gvc_date
        update.save()
        return
    return side_effect


@pytest.mark.dbuser
@replace_now('2015-01-01 00:00:00')
def test_set_last_gvc_date_has_data():
    last_gvc_date = datetime(2015, 1, 2)

    with mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.get_changes_spec_buf_rows',
                    return_value=[{'DATE_GVC': last_gvc_date}]):
        update_log = Update()
        set_last_gvc_date(update_log)

    assert update_log.last_gvc_date == last_gvc_date


@pytest.mark.dbuser
@replace_now('2015-01-01 00:00:00')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.DEFAULT_LAST_GVC_DATE_OFFSET', 2)
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.LAST_CHANGES_PAST_DATE', 3)
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.get_changes_spec_buf_rows', return_value=[])
def test_set_last_gvc_date_has_no_data(m_get_changes):
    update_log = Update()
    set_last_gvc_date(update_log)

    assert update_log.last_gvc_date == datetime(2014, 12, 30)
    m_get_changes.assert_has_calls([
        mock.call(datetime(2014, 12, 29), datetime(2015, 1, 1)),
    ])


@pytest.mark.dbuser
@replace_now('2015-01-01 00:00:00')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.MIN_DAYS_FROM_LAST_FULL_UPDATE', 10)
def test_fail_if_has_fresh_import():
    create_update(action_type=Update.FULL_UPDATE, updated_at="2014-12-25", last_gvc_date="2014-12-25")
    create_update(action_type=Update.CHANGES_UPDATE, updated_at="2013-12-25", last_gvc_date="2013-12-25")

    with pytest.raises(HasFreshImportException):
        update_full_update_tables()


@pytest.mark.dbuser
@replace_now('2015-01-01 20:00:00')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.MIN_DAYS_FROM_LAST_FULL_UPDATE', 10)
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.update_rzd_tables', side_effect=lambda x: x)
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.set_last_gvc_date',
            side_effect=build_set_last_gvc_date_side_effect(datetime(2014, 2, 2)))
def test_success_if_has_no_fresh_import(m_update, m_set_last_gvc):
    full_update = create_update(action_type=Update.FULL_UPDATE, updated_at="2014-12-15", last_gvc_date="2014-12-15")
    create_update(action_type=Update.CHANGES_UPDATE, updated_at="2015-01-01", last_gvc_date="2014-01-01")

    returned_update = update_full_update_tables()

    assert returned_update != full_update
    assert returned_update.action_type == Update.FULL_UPDATE
    assert returned_update.last_gvc_date == datetime(2014, 2, 2)
    m_update.assert_called_once_with(returned_update)
    m_set_last_gvc.assert_called_once_with(returned_update)


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.update_rzd_tables', side_effect=lambda x: x)
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.set_last_gvc_date',
            side_effect=build_set_last_gvc_date_side_effect(datetime(2015, 1, 1)))
def test_success_full_update_has_no_prev_imports(m_update, m_set_last_gvc):
    returned_update = update_full_update_tables()

    update = Update.objects.get()
    assert returned_update == update
    assert update.action_type == Update.FULL_UPDATE
    assert update.last_gvc_date == datetime(2015, 1, 1)
    m_update.assert_called_once_with(returned_update)
    m_set_last_gvc.assert_called_once_with(returned_update)


@pytest.mark.dbuser
def test_failed_full_update():
    with pytest.raises(Exception), \
            mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.update_rzd_tables', side_effect=Exception), \
            mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.set_last_gvc_date', return_value=True):
        update_full_update_tables()

    assert not Update.objects.exists()


@pytest.mark.dbuser
def test_apply_full_update():
    full_update = create_update(action_type=Update.FULL_UPDATE, updated_at="2014-11-01", last_gvc_date="2014-11-01")
    another_full_update = create_update(action_type=Update.FULL_UPDATE, updated_at="2014-11-01",
                                        last_gvc_date="2014-11-01")

    create_current_strains(IDTR=30)
    create_full_strains(IDTR=20, update=another_full_update)
    create_current_strainsvar(IDR=30)
    create_full_strainsvar(IDR=20, update=another_full_update)
    create_current_srasprp(IDR=30)
    create_full_srasprp(IDR=20, update=another_full_update)
    create_current_scalendar(IDTR=30)
    create_full_scalendar(IDTR=20, update=another_full_update)

    strain = create_full_strains(IDTR=10, update=full_update)
    stainsvar = create_full_strainsvar(IDR=10, update=full_update)
    sraspr = create_full_srasprp(IDR=10, update=full_update)
    scalendar = create_full_scalendar(IDTR=10, update=full_update)

    apply_full_update(full_update)

    assert Current_STRAINS.objects.get().IDTR == strain.IDTR == 10
    assert Current_STRAINSVAR.objects.get().IDR == stainsvar.IDR == 10
    assert Current_SRASPRP.objects.get().IDR == sraspr.IDR == 10
    assert Current_SCALENDAR.objects.get().IDTR == scalendar.IDTR == 10


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.get_stations')
def test_reload_stations(m_get_stations):
    create_ic00_stan(NAME=u'OLD STAN IN DATABASE')
    stan_data = [
        IC00_STAN_Factory.make_rzd_db_row(NAME=u'STAN FIRST'),
        IC00_STAN_Factory.make_rzd_db_row(NAME=u'STAN SECOND')
    ]

    m_get_stations.return_value = stan_data

    reload_stations()
    m_get_stations.assert_called_once_with()

    assert IC00_STAN.objects.count() == 2
    assert {u'STAN FIRST', u'STAN SECOND'} == {i.NAME for i in IC00_STAN.objects.all()}


@pytest.mark.dbuser
@replace_now('2015-01-01 00:00:00')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.get_trains_related_data')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.get_gdpprbase_strains_for_region')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_full.get_gdpprbase_regions', return_value=[{'CREG': 111, 'NNAPR': u'1 REG'}])
def test_update_rzd_tables(m_get_gdpprbase_regions, m_get_gdpprbase_strains_for_region, m_get_trains_related_data):
    m_get_gdpprbase_strains_for_region.return_value = [STRAINS_Factory.make_rzd_db_row(IDTR=20)]

    def get_trains_related_date_mock(train_ids):
        data = {
            'STRAINSVAR': [STRAINSVAR_Factory.make_rzd_db_row(IDTR=20, IDR=30)],
            'SRASPRP': [SRASPRP_Factory.make_rzd_db_row(IDTR=20, IDR=30)],
            'SCALENDAR': [SCALENDAR_Factory.make_rzd_db_row(IDTR=20, IDR=30)],
            'SDOCS': [SDOCS_Factory.make_rzd_db_row(IDDOC=10)],
        }

        yield data

    m_get_trains_related_data.side_effect = get_trains_related_date_mock

    update = create_update(action_type=Update.FULL_UPDATE)
    update_rzd_tables(update)

    assert Full_STRAINS.objects.filter(update=update).get().IDTR == 20
    assert Full_STRAINSVAR.objects.filter(update=update).get().IDTR == 20
    assert Full_STRAINSVAR.objects.filter(update=update).get().IDR == 30
    assert Full_SRASPRP.objects.filter(update=update).get().IDTR == 20
    assert Full_SRASPRP.objects.filter(update=update).get().IDR == 30
    assert Full_SCALENDAR.objects.filter(update=update).get().IDTR == 20
    assert Full_SCALENDAR.objects.filter(update=update).get().IDR == 30
    assert Full_SDOCS.objects.filter(update=update).get().IDDOC == 10
    m_get_gdpprbase_regions.assert_called_once_with()
    m_get_gdpprbase_strains_for_region.assert_called_once_with(111, date(2015, 1, 1))
    m_get_trains_related_data.assert_called_once_with([20])
