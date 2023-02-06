# -*- coding: utf-8 -*-

from datetime import date

import mock
import pytest

from travel.rasp.suburban_tasks.suburban_tasks.models import Update, Change_SPEC_BUF, Current_STRAINS, Current_STRAINSVAR, Current_SRASPRP, Current_SCALENDAR
from travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes import (
    apply_changes_update, apply_strains, Bad_SPEC_BUF_KOP, apply_strainsvar, apply_srasprp, apply_scalendar
)
from travel.rasp.suburban_tasks.tests.factories import (
    create_update, create_spec_buf, create_strains_buf, create_current_strains, create_strainsvar_buf,
    create_current_strainsvar, create_srasprp_buf, create_current_srasprp, create_scalendar_buf,
    create_current_scalendar
)

BAD_KOP_CODE_EXAMPLE = -50


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_strains')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_strainsvar')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_srasprp')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_scalendar')
def test_apply_changes_update(
        m_apply_scalendar,
        m_apply_srasprp,
        m_apply_strainsvar,
        m_apply_strains
):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf_3_15 = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15')
    spec_buf_4_15 = create_spec_buf(IDTR=30, update=update, DATE_GVC='2015-01-01 04:15')
    spec_buf_2014 = create_spec_buf(IDTR=30, update=update, DATE_GVC='2014-01-01 05:15')
    call_seq = [
        mock.call(spec_buf_2014),
        mock.call(spec_buf_3_15),
        mock.call(spec_buf_4_15)
    ]
    apply_changes_update(update)
    m_apply_scalendar.assert_has_calls(call_seq)
    m_apply_srasprp.assert_has_calls(call_seq)
    m_apply_strainsvar.assert_has_calls(call_seq)
    m_apply_strains.assert_has_calls(call_seq)


@pytest.mark.dbuser
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_strains')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_strainsvar')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_srasprp')
@mock.patch('travel.rasp.suburban_tasks.suburban_tasks.scripts.update_changes.apply_scalendar')
def test_apply_changes_update2(
        m_apply_scalendar,
        m_apply_srasprp,
        m_apply_strainsvar,
        m_apply_strains
):
    call_seq = []
    m_apply_strains.side_effect = lambda x: call_seq.append('strains')
    m_apply_scalendar.side_effect = lambda x: call_seq.append('scalendar')
    m_apply_srasprp.side_effect = lambda x: call_seq.append('srasprp')
    m_apply_strainsvar.side_effect = lambda x: call_seq.append('strainsvar')

    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15')
    apply_changes_update(update)
    m_apply_scalendar.assert_called_once_with(spec_buf)
    m_apply_srasprp.assert_called_once_with(spec_buf)
    m_apply_strainsvar.assert_called_once_with(spec_buf)
    m_apply_strains.assert_called_once_with(spec_buf)

    assert call_seq == ['strains', 'strainsvar', 'srasprp', 'scalendar']


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_strains_kop_create_update_new(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_strains_buf(IDTR=20, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    apply_strains(spec_buf)
    assert Current_STRAINS.objects.get().IDTR == 20
    apply_strains(spec_buf)
    assert Current_STRAINS.objects.get().IDTR == 20


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_strains_kop_create_update_existed(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_strains_buf(IDTR=20, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update, NAME=u'after')
    create_current_strains(IDTR=20, NAME=u'before')

    apply_strains(spec_buf)
    assert Current_STRAINS.objects.get().NAME == u'after'
    apply_strains(spec_buf)
    assert Current_STRAINS.objects.get().NAME == u'after'


@pytest.mark.dbuser
def test_apply_strains_kop_delete():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=Change_SPEC_BUF.KOP_DELETE_MODE, ID_XML=90)
    create_strains_buf(IDTR=20, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_current_strains(IDTR=20)

    apply_strains(spec_buf)
    assert not Current_STRAINS.objects.all().exists()
    apply_strains(spec_buf)
    assert not Current_STRAINS.objects.all().exists()


@pytest.mark.dbuser
def test_apply_strains_kop_other():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=BAD_KOP_CODE_EXAMPLE, ID_XML=90)
    create_strains_buf(IDTR=20, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    with pytest.raises(Bad_SPEC_BUF_KOP):
        apply_strains(spec_buf)


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_strainsvar_kop_create_update_new(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_strainsvar_buf(IDTR=20, IDR=50, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    apply_strainsvar(spec_buf)
    assert Current_STRAINSVAR.objects.get().IDR == 50
    apply_strainsvar(spec_buf)
    assert Current_STRAINSVAR.objects.get().IDR == 50


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_strainsvar_kop_create_update_existed(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_strainsvar_buf(IDTR=20, IDR=50, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update, RP1=30)
    create_current_strainsvar(IDTR=20, RP1=10, IDR=50)

    apply_strainsvar(spec_buf)
    assert Current_STRAINSVAR.objects.get().RP1 == 30
    apply_strainsvar(spec_buf)
    assert Current_STRAINSVAR.objects.get().RP1 == 30


@pytest.mark.dbuser
def test_apply_strainsvar_kop_delete():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=Change_SPEC_BUF.KOP_DELETE_MODE, ID_XML=90)
    create_strainsvar_buf(IDTR=20, IDR=50, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_current_strainsvar(IDTR=20, IDR=50)

    apply_strainsvar(spec_buf)
    assert not Current_STRAINSVAR.objects.all().exists()
    apply_strainsvar(spec_buf)
    assert not Current_STRAINSVAR.objects.all().exists()


@pytest.mark.dbuser
def test_apply_strainsvar_kop_other():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=BAD_KOP_CODE_EXAMPLE, ID_XML=90)
    create_strainsvar_buf(IDTR=20, IDR=50, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    with pytest.raises(Bad_SPEC_BUF_KOP):
        apply_strainsvar(spec_buf)


def assert_srasprp_list(srasprp_list, result):
    converted_srasprp_list = [
        [s.IDTR, s.IDR, s.SEQ, s.IDRP] for s in srasprp_list
        ]

    assert converted_srasprp_list == result


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_srasprp_kop_create_update_new(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=0, IDRP=100, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=1, IDRP=101, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    apply_srasprp(spec_buf)
    assert_srasprp_list(Current_SRASPRP.objects.order_by('SEQ'), [
        [20, 50, 0, 100],
        [20, 50, 1, 101]
    ])
    current_ids = list(Current_SRASPRP.objects.order_by('SEQ').values_list('id', flat=True))

    apply_srasprp(spec_buf)
    assert_srasprp_list(Current_SRASPRP.objects.order_by('SEQ'), [
        [20, 50, 0, 100],
        [20, 50, 1, 101]
    ])
    assert current_ids == list(Current_SRASPRP.objects.order_by('SEQ').values_list('id', flat=True))


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_srasprp_kop_create_update_existed(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=0, IDRP=100, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=1, IDRP=101, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_current_srasprp(IDTR=20, IDR=50, SEQ=0, IDRP=200)
    create_current_srasprp(IDTR=20, IDR=50, SEQ=1, IDRP=201)

    apply_srasprp(spec_buf)
    assert_srasprp_list(Current_SRASPRP.objects.order_by('SEQ'), [
        [20, 50, 0, 100],
        [20, 50, 1, 101]
    ])
    current_ids = list(Current_SRASPRP.objects.order_by('SEQ').values_list('id', flat=True))

    apply_srasprp(spec_buf)
    assert_srasprp_list(Current_SRASPRP.objects.order_by('SEQ'), [
        [20, 50, 0, 100],
        [20, 50, 1, 101]
    ])
    assert current_ids == list(Current_SRASPRP.objects.order_by('SEQ').values_list('id', flat=True))


@pytest.mark.dbuser
def test_apply_srasprp_kop_delete():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=Change_SPEC_BUF.KOP_DELETE_MODE, ID_XML=90)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=0, IDRP=100, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=1, IDRP=101, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_current_srasprp(IDTR=20, IDR=50, SEQ=0, IDRP=100)
    create_current_srasprp(IDTR=20, IDR=50, SEQ=1, IDRP=101)

    apply_srasprp(spec_buf)
    assert not Current_SRASPRP.objects.all().exists()
    apply_srasprp(spec_buf)
    assert not Current_SRASPRP.objects.all().exists()


@pytest.mark.dbuser
def test_apply_srasprp_kop_other():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=BAD_KOP_CODE_EXAMPLE, ID_XML=90)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=0, IDRP=100, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_srasprp_buf(IDTR=20, IDR=50, SEQ=1, IDRP=101, DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    with pytest.raises(Bad_SPEC_BUF_KOP):
        apply_srasprp(spec_buf)


def assert_attrs_equal(obj, attrs, result):
    assert [getattr(obj, attr) for attr in attrs] == result


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_scalendar_kop_create_update_new(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_scalendar_buf(IDTR=20, IDR=50, CDATE='2015-01-10', DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    apply_scalendar(spec_buf)
    assert_attrs_equal(Current_SCALENDAR.objects.get(), ['IDTR', 'IDR', 'CDATE'], [20, 50, date(2015, 1, 10)])
    apply_scalendar(spec_buf)
    assert_attrs_equal(Current_SCALENDAR.objects.get(), ['IDTR', 'IDR', 'CDATE'], [20, 50, date(2015, 1, 10)])


@pytest.mark.dbuser
@pytest.mark.parametrize('kop', [Change_SPEC_BUF.KOP_INSERT_MODE, Change_SPEC_BUF.KOP_UPDATE_MODE])
def test_apply_scalendar_kop_create_update_existed(kop):
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15', KOP=kop, ID_XML=90)
    create_scalendar_buf(IDTR=20, IDR=50, CDATE='2015-01-10', DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_current_scalendar(IDTR=20, IDR=30, CDATE='2015-01-10')

    apply_scalendar(spec_buf)
    assert_attrs_equal(Current_SCALENDAR.objects.get(), ['IDTR', 'IDR', 'CDATE'], [20, 50, date(2015, 1, 10)])
    apply_scalendar(spec_buf)
    assert_attrs_equal(Current_SCALENDAR.objects.get(), ['IDTR', 'IDR', 'CDATE'], [20, 50, date(2015, 1, 10)])


@pytest.mark.dbuser
def test_apply_scalendar_kop_delete():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=Change_SPEC_BUF.KOP_DELETE_MODE, ID_XML=90)
    create_scalendar_buf(IDTR=20, IDR=50, CDATE='2015-01-10', DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)
    create_current_scalendar(IDTR=20, IDR=50, CDATE='2015-01-10')

    apply_scalendar(spec_buf)
    assert not Current_SCALENDAR.objects.all().exists()
    apply_scalendar(spec_buf)
    assert not Current_SCALENDAR.objects.all().exists()


@pytest.mark.dbuser
def test_apply_scalendar_kop_other():
    update = create_update(action_type=Update.CHANGES_UPDATE, last_gvc_date='2015-01-01', is_fake_gvc_date=False,
                           query_from_dt='2015-01-01', query_to_dt='2015-01-01 01')
    spec_buf = create_spec_buf(IDTR=20, update=update, DATE_GVC='2015-01-01 03:15',
                               KOP=BAD_KOP_CODE_EXAMPLE, ID_XML=90)
    create_scalendar_buf(IDTR=20, IDR=50, CDATE='2015-01-10', DATE_GVC=spec_buf.DATE_GVC, ID_XML=90, update=update)

    with pytest.raises(Bad_SPEC_BUF_KOP):
        apply_scalendar(spec_buf)
