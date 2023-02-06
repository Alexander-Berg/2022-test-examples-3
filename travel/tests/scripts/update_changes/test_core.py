# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
from operator import attrgetter

import mock
import pytest
from freezegun import freeze_time

from travel.rasp.suburban_tasks.suburban_tasks.models import (
    Update, Change_SPEC_BUF, Change_STRAINS_BUF, Change_STRAINSVAR_BUF, Change_SRASPRP_BUF, Change_SCALENDAR_BUF, Change_SDOCS_BUF
)
from travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes import (
    HasNoFullUpdate, load_new_spec_bufs, load_changes_update, load_changes_for_update,
    filter_spec_buf_rows, get_load_new_spec_bufs_time_range
)
from travel.rasp.suburban_tasks.tests.factories import (
    create_update, Change_SPEC_BUF_Factory, create_spec_buf, STRAINSVAR_BUF_Factory, SRASPRP_BUF_Factory,
    SCALENDAR_BUF_Factory, SDOCS_BUF_Factory, STRAINS_BUF_Factory
)

from common.tester.utils.replace_setting import replace_dynamic_setting
from common.tester.utils.datetime import replace_now


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.load_new_spec_bufs', side_effect=HasNoFullUpdate(u'Error'))
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.load_changes_for_update')
def test_load_changes_update__fail_if_has_no_full_update(m_load_changes_for_update, m_load_new_spec_bufs):
    with pytest.raises(HasNoFullUpdate):
        load_changes_update()

    m_load_new_spec_bufs.assert_called_once_with()
    m_load_changes_for_update.assert_not_called()


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.load_new_spec_bufs')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.load_changes_for_update')
def test_load_changes_update(m_load_changes_for_update, m_load_new_spec_bufs):
    update = create_update(action_type=Update.CHANGES_UPDATE)
    m_load_new_spec_bufs.return_value = update

    assert load_changes_update() == update

    m_load_new_spec_bufs.assert_called_once_with()
    m_load_changes_for_update.assert_called_once_with(update)


@pytest.mark.dbuser
@replace_now('2015-01-01 01:42:00')
def test_get_load_new_spec_bufs_time_range():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2014-12-30 14:28')

    # ограничение по часам есть, но мы в него влезаем
    with replace_dynamic_setting('RZD_CHANGES_MAX_HOURS_TO_FETCH', 36):
        query_from_dt, query_to_dt = get_load_new_spec_bufs_time_range(update)
        assert query_from_dt == datetime(2014, 12, 30, 14, 28)
        assert query_to_dt == datetime(2015, 1, 1, 1, 42, 0)

    # ограничения по часам нет
    with replace_dynamic_setting('RZD_CHANGES_MAX_HOURS_TO_FETCH', 0):
        query_from_dt, query_to_dt = get_load_new_spec_bufs_time_range(update)
        assert query_from_dt == datetime(2014, 12, 30, 14, 28)
        assert query_to_dt == datetime(2015, 1, 1, 1, 42, 0)

    # ограничение по часам есть, и мы в него не влезаем
    with replace_dynamic_setting('RZD_CHANGES_MAX_HOURS_TO_FETCH', 34):
        query_from_dt, query_to_dt = get_load_new_spec_bufs_time_range(update)
        assert query_from_dt == datetime(2014, 12, 30, 14, 28)
        assert query_to_dt == datetime(2015, 1, 1, 0, 28, 0)


@pytest.mark.dbuser
def test_load_new_spec_bufs__fail_if_has_no_full_update():
    create_update(action_type=Update.CHANGES_UPDATE)
    with pytest.raises(HasNoFullUpdate):
        load_new_spec_bufs()


@pytest.mark.dbuser
@freeze_time('2015-01-01 00:00:00')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.get_changes_spec_buf_rows')
@replace_dynamic_setting('RZD_CHANGES_MAX_HOURS_TO_FETCH', 60)
def test_load_new_spec_bufs__has_new_spec_bufs(m_get_changes_spec_buf_rows):
    full_update = create_update(action_type=Update.FULL_UPDATE, last_gvc_date='2014-12-30')
    m_get_changes_spec_buf_rows.return_value = [
        Change_SPEC_BUF_Factory.make_rzd_db_row(IDTR=10, DATE_GVC=datetime(2014, 12, 31))
    ]

    update = load_new_spec_bufs()

    spec_buf = Change_SPEC_BUF.objects.get()

    assert spec_buf.IDTR == 10
    assert spec_buf.DATE_GVC == datetime(2014, 12, 31)
    assert update.last_gvc_date == spec_buf.DATE_GVC
    assert not update.is_fake_gvc_date
    assert update.query_from_dt == full_update.last_gvc_date
    assert update.query_to_dt == datetime(2015, 1, 1, 3)  # Т.к. мы спрашиваем по Москве, а Москва UTC+3
    assert update.updated_at == datetime(2015, 1, 1)
    m_get_changes_spec_buf_rows.assert_called_once_with(update.query_from_dt, update.query_to_dt)


@pytest.mark.dbuser
@pytest.mark.parametrize('is_fake_gvc_date', [True, False])
@freeze_time('2015-01-01 02:00:00')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.get_changes_spec_buf_rows')
@replace_dynamic_setting('RZD_CHANGES_MAX_HOURS_TO_FETCH', 60)
def test_load_new_spec_bufs__has_no_spec_bufs(m_get_changes_spec_buf_rows, is_fake_gvc_date):
    full_update = create_update(action_type=Update.FULL_UPDATE, last_gvc_date='2014-12-30',
                                is_fake_gvc_date=is_fake_gvc_date)
    m_get_changes_spec_buf_rows.return_value = []

    update = load_new_spec_bufs()

    assert not Change_SPEC_BUF.objects.exists()
    assert update.last_gvc_date == full_update.last_gvc_date
    assert update.is_fake_gvc_date == full_update.is_fake_gvc_date
    assert update.query_from_dt == full_update.last_gvc_date
    assert update.query_to_dt == datetime(2015, 1, 1, 5)  # Т.к. мы спрашиваем по Москве, а Москва UTC+3
    assert update.updated_at == datetime(2015, 1, 1, 2)
    m_get_changes_spec_buf_rows.assert_called_once_with(update.query_from_dt, update.query_to_dt)


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.get_train_changes_related_data')
def test_load_changes_for_update(m_get_train_changes_related_data):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15')
    m_get_train_changes_related_data.return_value = {
        'STRAINSVAR': [STRAINSVAR_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC)],
        'SRASPRP': [SRASPRP_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC)],
        'SCALENDAR': [SCALENDAR_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC)],
        'SDOCS': [SDOCS_BUF_Factory.make_rzd_db_row(IDDOC=10, DATE_GVC=spec_buf.DATE_GVC)],
        'STRAINS': [STRAINS_BUF_Factory.make_rzd_db_row(IDTR=20, DATE_GVC=spec_buf.DATE_GVC)],
    }

    load_changes_for_update(update)

    assert Change_STRAINS_BUF.objects.filter(update=update).get().IDTR == 20
    assert Change_STRAINSVAR_BUF.objects.filter(update=update).get().IDTR == 20
    assert Change_STRAINSVAR_BUF.objects.filter(update=update).get().IDR == 30
    assert Change_SRASPRP_BUF.objects.filter(update=update).get().IDTR == 20
    assert Change_SRASPRP_BUF.objects.filter(update=update).get().IDR == 30
    assert Change_SCALENDAR_BUF.objects.filter(update=update).get().IDTR == 20
    assert Change_SCALENDAR_BUF.objects.filter(update=update).get().IDR == 30
    assert Change_SDOCS_BUF.objects.filter(update=update).get().IDDOC == 10
    m_get_train_changes_related_data.assert_called_once_with([20], datetime(2015, 1, 1), datetime(2015, 1, 1, 3))


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.get_train_changes_related_data')
def test_load_changes_for_update_drop_extra_rows(m_get_train_changes_related_data):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', ID_XML=200)
    m_get_train_changes_related_data.return_value = {
        'STRAINSVAR': [
            STRAINSVAR_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC, ID_XML=200),
            STRAINSVAR_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC, ID_XML=220)
        ],
        'SRASPRP': [
            SRASPRP_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC + timedelta(seconds=1),
                                                ID_XML=200),
            SRASPRP_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC, ID_XML=200)
        ],
        'SCALENDAR': [
            SCALENDAR_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC, ID_XML=200),
            SCALENDAR_BUF_Factory.make_rzd_db_row(IDTR=20, IDR=30, DATE_GVC=spec_buf.DATE_GVC, ID_XML=220)
        ],
        'SDOCS': [
            SDOCS_BUF_Factory.make_rzd_db_row(IDDOC=10, DATE_GVC=spec_buf.DATE_GVC, ID_XML=200)
        ],
        'STRAINS': [
            STRAINS_BUF_Factory.make_rzd_db_row(IDTR=20, DATE_GVC=spec_buf.DATE_GVC, ID_XML=200),
            STRAINS_BUF_Factory.make_rzd_db_row(IDTR=20, DATE_GVC=spec_buf.DATE_GVC + timedelta(seconds=1), ID_XML=220)
        ],
    }

    load_changes_for_update(update)

    strains_buf = Change_STRAINS_BUF.objects.filter(update=update).get()
    strainsvar_buf = Change_STRAINSVAR_BUF.objects.filter(update=update).get()
    srasprp_buf = Change_SRASPRP_BUF.objects.filter(update=update).get()
    scalendar_buf = Change_SCALENDAR_BUF.objects.filter(update=update).get()
    sdocs_buf = Change_SDOCS_BUF.objects.filter(update=update).get()

    assert attrgetter('IDTR', 'ID_XML', 'DATE_GVC')(strains_buf) == (20, 200, spec_buf.DATE_GVC)
    assert attrgetter('IDTR', 'IDR', 'ID_XML', 'DATE_GVC')(strainsvar_buf) == (20, 30, 200, spec_buf.DATE_GVC)
    assert attrgetter('IDTR', 'IDR', 'ID_XML', 'DATE_GVC')(srasprp_buf) == (20, 30, 200, spec_buf.DATE_GVC)
    assert attrgetter('IDTR', 'IDR', 'ID_XML', 'DATE_GVC')(scalendar_buf) == (20, 30, 200, spec_buf.DATE_GVC)
    assert attrgetter('IDDOC', 'ID_XML', 'DATE_GVC')(sdocs_buf) == (10, 200, spec_buf.DATE_GVC)
    m_get_train_changes_related_data.assert_called_once_with([20], datetime(2015, 1, 1), datetime(2015, 1, 1, 3))


@pytest.mark.dbuser
def test_filter_spec_buf_rows_last_update_is_full():
    last_update = create_update(action_type=Update.FULL_UPDATE, last_gvc_date=datetime(2015, 1, 10, 10))
    change_update = create_update(action_type=Update.CHANGES_UPDATE)
    create_spec_buf(ID_XML=9, DATE_GVC=datetime(2015, 1, 10, 9), update=change_update)
    create_spec_buf(ID_XML=11, DATE_GVC=datetime(2015, 1, 10, 11), update=change_update)

    spec_buf_rows = [
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=8,
                                                DATE_GVC=datetime(2015, 1, 10, 8, 0, 0, 333333)),  # Must not be <
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=8, DATE_GVC=datetime(2015, 1, 10, 9)),    # Must not be <
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=9, DATE_GVC=datetime(2015, 1, 10, 9)),    # Must not be <
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=22, DATE_GVC=datetime(2015, 1, 10, 10)),  # Must not be <=
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=11, DATE_GVC=datetime(2015, 1, 10, 11)),  # Must not be existed
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=12, DATE_GVC=datetime(2015, 1, 10, 12)),  # Must be
    ]

    filtered = filter_spec_buf_rows(spec_buf_rows, last_update)

    assert len(filtered) == 1
    assert (filtered[0]['ID_XML'], filtered[0]['DATE_GVC']) == (12, datetime(2015, 1, 10, 12))


@pytest.mark.dbuser
def test_filter_spec_buf_rows_last_update_is_changes():
    full_update = create_update(action_type=Update.FULL_UPDATE, last_gvc_date=datetime(2015, 1, 9, 10))
    change_update = create_update(action_type=Update.CHANGES_UPDATE, updated_at=full_update.updated_at + timedelta(10))
    last_update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date=datetime(2015, 1, 10, 10),
                                updated_at=change_update.updated_at + timedelta(10))
    create_spec_buf(ID_XML=9, DATE_GVC=datetime(2015, 1, 10, 9), update=change_update)
    create_spec_buf(ID_XML=11, DATE_GVC=datetime(2015, 1, 10, 11), update=change_update)

    spec_buf_rows = [
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=9,
                                                DATE_GVC=datetime(2015, 1, 9, 10, 0, 0, 3333)),  # Must not be == full_update_date
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=8, DATE_GVC=datetime(2015, 1, 10, 9, 0, 0, 333333)),    # Must be
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=9, DATE_GVC=datetime(2015, 1, 10, 9)),    # Must not be existed
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=22, DATE_GVC=datetime(2015, 1, 10, 10)),  # Must be
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=11, DATE_GVC=datetime(2015, 1, 10, 11)),  # Must not be existed
        Change_SPEC_BUF_Factory.make_rzd_db_row(ID_XML=12, DATE_GVC=datetime(2015, 1, 10, 12)),  # Must be
    ]

    filtered = filter_spec_buf_rows(spec_buf_rows, last_update)

    assert len(filtered) == 3
    assert (filtered[0]['ID_XML'], filtered[0]['DATE_GVC']) == (8, datetime(2015, 1, 10, 9, 0, 0, 333333))
    assert (filtered[1]['ID_XML'], filtered[1]['DATE_GVC']) == (22, datetime(2015, 1, 10, 10))
    assert (filtered[2]['ID_XML'], filtered[2]['DATE_GVC']) == (12, datetime(2015, 1, 10, 12))
