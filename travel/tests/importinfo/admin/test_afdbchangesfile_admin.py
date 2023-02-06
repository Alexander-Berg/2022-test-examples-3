# coding: utf-8

from collections import defaultdict
from io import BytesIO

import pytest
from django.core.files.base import ContentFile
from django.core.urlresolvers import reverse

from common.models.geo import Settlement
from common.settings import WorkInstance
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


def assert_settlement_change_not_applied():
    settlement = Settlement.objects.get(pk=10000)
    assert settlement.title == u'Старое название города'


create_test_settlement = create_settlement.mutate(id=10000, title=u'Старое название города')


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_afdbchanges_add(superuser_client, tmpdir):
    create_test_settlement()

    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}):
        resp = superuser_client.post('/importinfo/afdbchangesfile/add/', {
            'changes_file': BytesIO(settlement_change_xml.encode('utf8'))
        })

    assert resp.status_code == 302
    assert_settlement_change_applied()


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_afdbchanges_save(superuser_client, tmpdir):
    create_test_settlement()

    db_changes = AFDBChangesFile()
    db_changes.changes_file = ContentFile(name='1.xml', content=settlement_change_xml)
    db_changes.save()

    resp = superuser_client.post('/importinfo/afdbchangesfile/{}/'.format(db_changes.id))

    assert resp.status_code == 302
    assert_settlement_change_not_applied()


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_afdbchanges_reimport(superuser_client, tmpdir):
    create_test_settlement()

    db_changes = AFDBChangesFile()
    db_changes.changes_file = ContentFile(name='1.xml', content=settlement_change_xml)
    db_changes.save()

    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}):
        resp = superuser_client.post('/importinfo/afdbchangesfile/{}/reimport/'.format(db_changes.id))

    assert resp.status_code == 302
    assert_settlement_change_applied()


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_afdbchanges_change_form(superuser_client, tmpdir):
    create_test_settlement()

    with replace_setting('LOG_PATHS', {WorkInstance.code: str(tmpdir)}):
        superuser_client.post('/importinfo/afdbchangesfile/add/', {
            'changes_file': BytesIO(settlement_change_xml.encode('utf8'))
        })

    dbchanges = AFDBChangesFile.objects.get()

    with replace_setting('LOG_PATHS', defaultdict(lambda: str(tmpdir))):
        resp = superuser_client.get(
            reverse('admin:%s_%s_%s' % (dbchanges._meta.app_label, dbchanges._meta.model_name, 'change'),
                    args=(dbchanges.id,))
        )

    assert resp.status_code == 200
    task_log_block = resp.context_data['task_log_blocks'][0]
    assert task_log_block['title'] == u'Рабочие логи'
    assert len(task_log_block['logs']) == 1
    assert task_log_block['logs'][0].name == 'importinfo/afdbchangesfile/{}/import'.format(dbchanges.id)
