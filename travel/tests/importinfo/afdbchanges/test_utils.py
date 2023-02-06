# -*- coding: utf-8 -*-

import gzip

import pytest
from io import BytesIO
from lxml import etree

from django.core.files.base import ContentFile

from common.maintenance.models import Flag
from common.models.geo import Settlement
from common.settings import WorkInstance
from travel.rasp.admin.importinfo.afdbchanges.utils import run_actions, apply_af_db_changes, run_apply_af_db_changes_as_task
from travel.rasp.admin.importinfo.models.af import AFDBChangesFile
from tester.factories import create_settlement
from tester.utils.replace_setting import replace_setting


settlement_change_xml = u'''
    <dbchanges>
        <dbchange model="www.settlement" type="title" object_id="10000" value="AAAA"/>
    </dbchanges>
'''


def assert_settlement_change_applied():
    settlement = Settlement.objects.get(pk=10000)
    assert settlement.title == u'AAAA'


create_test_settlement = create_settlement.mutate(id=10000, title=u'Старое название города')


@pytest.mark.dbuser
def test_run_actions():
    create_test_settlement()
    run_actions(etree.fromstring(settlement_change_xml))
    assert_settlement_change_applied()


@pytest.mark.dbuser
def test_run_actions_bad_xml():
    # Проверяем, что исключение не кидается
    run_actions(etree.fromstring('<dbchanges><dbchange model="www.settlement1" /></dbchanges>'))


@pytest.mark.dbuser
def test_apply_af_db_changes():
    create_test_settlement()

    db_changes = AFDBChangesFile()
    db_changes.changes_file = ContentFile(name='1.xml', content=settlement_change_xml)
    db_changes.save()

    apply_af_db_changes(db_changes.id)

    assert_settlement_change_applied()


@pytest.mark.dbuser
def test_apply_af_db_changes_with_gz_file():
    create_test_settlement()

    buf = BytesIO()
    with gzip.GzipFile(fileobj=buf, mode='w') as gzf:
        gzf.write(settlement_change_xml)

    db_changes = AFDBChangesFile()
    db_changes.changes_file = ContentFile(name='1.xml.gz', content=buf.getvalue())
    db_changes.save()

    apply_af_db_changes(db_changes.id)

    assert_settlement_change_applied()


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_run_apply_af_db_changes_as_task(tmpdir):
    create_test_settlement()

    db_changes = AFDBChangesFile()
    db_changes.changes_file = ContentFile(name='1.xml', content=settlement_change_xml)
    db_changes.save()

    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}):
        run_apply_af_db_changes_as_task(db_changes)

    assert_settlement_change_applied()


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_run_apply_af_db_changes_as_task_maintenance_flag(tmpdir):
    Flag.objects.filter(name='maintenance').update(state=1)

    create_test_settlement()

    db_changes = AFDBChangesFile()
    db_changes.changes_file = ContentFile(name='1.xml', content=settlement_change_xml)
    db_changes.save()

    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}):
        run_apply_af_db_changes_as_task(db_changes)

    settlement = Settlement.objects.get(pk=10000)
    assert settlement.title == u'Старое название города'
