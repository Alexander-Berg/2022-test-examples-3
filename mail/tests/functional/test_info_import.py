from datetime import datetime, timedelta

import pytest

from mail.ipa.ipa.core.entities.enums import UserImportError


class TestInfoImport:
    @pytest.fixture(autouse=True)
    def func_now(self, mocker):
        delta = timedelta()
        now = datetime.now()

        def dummy_now():
            nonlocal delta
            delta += timedelta(seconds=1)
            return now + delta

        mocker.patch('mail.ipa.ipa.storage.mappers.user.mapper.func.now', dummy_now)
        mocker.patch('mail.ipa.ipa.storage.mappers.collector.mapper.func.now', dummy_now)

    @pytest.fixture(autouse=True)
    async def users(self, func_now, org_id, create_user):
        return [
            await create_user(org_id=org_id, error=error)
            for error in (None, None, 'password.weak', 'login.prohobitedsymbols')
        ]

    @pytest.fixture(autouse=True)
    async def collectors(self, func_now, create_collector, users):
        return [
            await create_collector(user_id=user.user_id, status=status)
            for user in users
            if user.error is None
            for status in ('ok', 'auth_error')
        ]

    @pytest.fixture
    def expected(self, users, collectors):
        users_by_id = {u.user_id: u for u in users}

        data_collectors = []
        for user in users:
            if user.error is not None:
                data_collectors.append({
                    'uid': user.uid,
                    'login': user.login,
                    'error': UserImportError.get_error(user_error=user.error).value,
                })
        for collector in collectors:
            user = users_by_id[collector.user_id]
            error = UserImportError.get_error(
                user_error=user.error,
                collector_status=collector.status,
            )
            data_collectors.append({
                'uid': user.uid,
                'login': user.login,
                'error': error and error.value,
                'collected': collector.collected,
                'total': collector.total,
                'errors': collector.errors,
                'params': {
                    'delete_msgs': collector.params.delete_msgs,
                    'imap': collector.params.imap,
                    'mark_archive_read': collector.params.mark_archive_read,
                    'port': collector.params.port,
                    'server': collector.params.server,
                    'ssl': collector.params.ssl,
                    'src_login': collector.params.src_login,
                }
            })

        return {
            'code': 200,
            'status': 'success',
            'data': {
                'has_more': False,
                'collectors': list(reversed(data_collectors)),
            }
        }

    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/import/{org_id}/')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_status(self, response):
        assert response.status == 200

    def test_response(self, response_json, expected):
        assert response_json == expected
