import pytest

pytest_plugins = [
    "crypta.api.test_helpers.fixtures",
]


@pytest.fixture
def init_db(postgres):
    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute("""
                    INSERT INTO api_segments (id, type, scope, ticket, author, parent_id) VALUES
                        ('root', 'GROUP', 'EXTERNAL', 'CRYPTA-1564', 'dima-utyuz', 'root'),
                        ('root-users', 'GROUP', 'EXTERNAL', 'CRYPTA-1564', 'dima-utyuz', 'root'),
                        ('segment-test', 'CRYPTA_SEGMENT', 'INTERNAL', 'CRYPTA-1564', 'dima-utyuz', 'root');
                """)

    yield

    with postgres.connect() as connection, connection.cursor() as cursor:
        cursor.execute("""
            DELETE FROM api_responsibles;
            DELETE FROM api_segment_exports;
            DELETE FROM api_segments;
        """)
