from datetime import datetime, timezone

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries, has_item, has_properties

from mail.payments.payments.core.entities.enums import PAY_METHODS, MerchantRole, TaskType
from mail.payments.payments.core.entities.report import Report
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def pay_method(randitem):
    return randitem(PAY_METHODS)


@pytest.fixture
def report_data(randn, pay_method):
    x = randn()
    y = randn()

    return {
        'lower_dt': datetime.fromtimestamp(x).astimezone(timezone.utc).isoformat(),
        'upper_dt': datetime.fromtimestamp(x + y).astimezone(timezone.utc).isoformat(),
        'pay_method': pay_method
    }


class TestPostReport(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    async def response(self, client, merchant, report_data, tvm):
        r = await client.post(f'/v1/report/{merchant.uid}', params=report_data)
        assert r.status == 200
        return await r.json()

    @pytest.mark.asyncio
    async def test_required_params(self, client, merchant):
        r = await client.post(f'/v1/report/{merchant.uid}')
        assert r.status == 400

    @pytest.mark.asyncio
    async def test_report_params(self, response, pay_method, merchant, report_data):
        report_returned = response['data']
        assert_that(report_returned, has_entries({
            'uid': merchant.uid,
            'data': has_entries({
                'lower_dt': report_data['lower_dt'],
                'upper_dt': report_data['upper_dt'],
                'pay_method': pay_method
            })
        }))

    @pytest.mark.asyncio
    async def test_returned_report(self, response, storage, merchant, report_data, pay_method):
        report_returned = response['data']
        db_report = await storage.report.get(report_returned['report_id'])
        assert_that(report_returned, has_entries({
            'data': has_entries({
                'lower_dt': db_report.data['lower_dt'].isoformat(),
                'upper_dt': db_report.data['upper_dt'].isoformat(),
                'pay_method': pay_method,
            }),
            'uid': db_report.uid,
            'mds_path': db_report.mds_path
        }))

    def test_task_params(self, response, tasks, merchant, report_data):
        assert_that(
            tasks,
            has_item(has_properties({
                'params': has_entries(
                    action_kwargs={
                        'uid': merchant.uid,
                        'report_id': response['data']['report_id']
                    }
                ),
                'action_name': 'create_report_action',
                'task_type': TaskType.RUN_ACTION,
            }))
        )


class TestDownloadReport(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def content_type(self):
        return 'test-download-report-content-type'

    @pytest.fixture
    def created(self):
        return utcnow()

    @pytest.fixture
    def chunks(self):
        return [b'aaa', b'bbb', b'ccc']

    @pytest.fixture(autouse=True)
    def mds_download_mock(self, mds_client_mocker, content_type, chunks):
        async def download():
            for chunk in chunks:
                yield chunk

        with mds_client_mocker('download', (content_type, download())) as mock:
            yield mock

    @pytest.fixture
    def path(self):
        return 'test-report-download-path'

    @pytest.fixture
    async def report_entity(self, storage, path, merchant, created):
        return Report(mds_path=path, uid=merchant.uid, created=created)

    @pytest.fixture
    async def report(self, storage, report_entity):
        return await storage.report.create(report_entity)

    @pytest.fixture
    async def not_uploaded_report(self, storage, merchant):
        not_uploaded_report_entity = Report(mds_path='', uid=merchant.uid)
        return await storage.report.create(not_uploaded_report_entity)

    @pytest.fixture
    def report_id(self, report):
        return report.report_id

    @pytest.fixture
    async def download_response(self, client, merchant, report_id, report, tvm):
        return await client.get(f'/v1/report/{merchant.uid}/download/{report_id}')

    @pytest.fixture
    async def response_data(self, download_response):
        return await download_response.read()

    @pytest.mark.asyncio
    async def test_returns_joined_chunks(self, chunks, response_data):
        assert response_data == b''.join(chunks)

    @pytest.mark.asyncio
    async def test_not_found(self, client, merchant, rands, report, tvm):
        report_id = rands()
        response = await client.get(f'/v1/report/{merchant.uid}/download/{report_id}')
        assert response.status == 404

    @pytest.mark.asyncio
    async def test_not_uploaded(self, client, merchant, not_uploaded_report, tvm):
        report_id = not_uploaded_report.report_id
        response = await client.get(f'/v1/report/{merchant.uid}/download/{report_id}')
        assert response.status == 400


class TestReportsList(BaseTestMerchantRoles):  # TODO: make this test able to run multiple times
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def mds_path(self):
        return 'test-reports-list-path'

    @pytest.fixture(autouse=True)
    def mds_upload_mock(self, mds_client_mocker, mds_path):
        with mds_client_mocker('upload', mds_path) as mock:
            yield mock

    @pytest.fixture
    def created(self):
        return utcnow()

    @pytest.fixture
    def report_entities(self, storage, merchant, mds_path, created, rands):
        return [
            Report(report_id=rands(), mds_path=mds_path, uid=merchant.uid, created=created)
            for _ in range(3)
        ]

    @pytest.fixture
    def reports_data(self, report_entities):
        return [
            {
                'report_id': report.report_id,
                'mds_path': report.mds_path,
                'uid': report.uid,
                'created': report.created
            }
            for report in report_entities
        ]

    @pytest.fixture
    async def reports(self, storage, report_entities):
        return [
            await storage.report.create(entity)
            for entity in report_entities
        ]

    @pytest.fixture
    async def reports_list_response(self, client, merchant, tvm):
        r = await client.get(f'/v1/report/{merchant.uid}')
        assert r.status == 200
        return (await r.json())['data']

    def test_reports_match(self, reports, reports_list_response):
        assert_that(
            [
                {
                    'uid': ret_report['uid'],
                    'created': ret_report['created'],
                    'mds_path': ret_report['mds_path'],
                    'data': ret_report['data']
                }
                for ret_report in reports_list_response
            ],
            contains_inanyorder(*[
                {
                    'uid': report.uid,
                    'created': report.created.astimezone(tz=timezone.utc).isoformat(),
                    'mds_path': report.mds_path,
                    'data': report.data
                }
                for report in reports
            ])
        )
