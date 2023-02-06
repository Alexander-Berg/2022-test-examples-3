import pytest

pytest_plugins = [
    "crypta.idm.test_helpers.fixtures",
]


@pytest.fixture
def clear_db(idm_api, postgres):
    yield

    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute("""
            DELETE FROM api_idm_login_roles;
        """)
        cursor.execute("""
            DELETE FROM api_idm_logins;
        """)
