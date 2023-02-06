import pytest


pytest_plugins = [
    "crypta.api.test_helpers.fixtures",
]


@pytest.fixture
def init_db(postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute("""
        INSERT INTO api_constructor_rules (id, author, name, days, min_days, created, modified)
        VALUES('rule-26abed05', 'robot-secretary', 'Kaptur', 35, 2, 1561046025, 1561468323)
        """)
