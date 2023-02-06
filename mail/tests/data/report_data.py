from itertools import chain

import pytest


@pytest.fixture
def report_data():
    return [
        {  # User creation failed: user with error, no collector
            'login': 'some_login1',
            'error': 'some user error',
        },
        {
            # User created, collector error
            'login': 'some_login1.5',
            'collectors': [{
                'status': 'some collector error',
                'errors': 0,
                'collected': 0,
                'total': 0,
            }],
        },
        {  # No errors, single collector
            'login': 'some_login2',
            'collectors': [
                {'errors': 5, 'collected': 6, 'total': 7},
            ],
        },
        {  # No errors, multiple collectors
            'login': 'some_login3',
            'collectors': [
                {'errors': 5, 'collected': 6, 'total': 8},
                {'errors': 6, 'collected': 8, 'total': 3},
                {'errors': 7, 'collected': 9, 'total': 1},
            ],
        }
    ]


@pytest.fixture
async def setup_report_data(org_id, create_user, create_collector, report_data):
    for user_data in report_data:
        user = await create_user(org_id=org_id, login=user_data['login'], error=user_data.get('error'))
        for collector_data in user_data.get('collectors', []):
            await create_collector(
                user_id=user.user_id,
                errors=collector_data['errors'],
                collected=collector_data['collected'],
                total=collector_data['total'],
                status=collector_data.get('status', 'ok'),
            )


@pytest.fixture
def expected_report_response(report_data):
    from mail.ipa.ipa.core.entities.enums import UserImportError
    return '\r\n'.join(chain(
        ['login,error,collected,total,errors'],
        [
            ','.join([
                user_data['login'],
                UserImportError.get_error(
                    user_error=user_data.get('error'),
                    collector_status=collector_data.get('status'),
                ).value,
                str(collector_data.get('collected', '')),
                str(collector_data.get('total', '')),
                str(collector_data.get('errors', '')),
            ])
            for user_data in report_data
            for collector_data in user_data.get('collectors', [{}])
            if user_data.get('error') is not None or collector_data.get('status', 'ok') != 'ok'
        ],
        [''],
    ))
