import pytest
import psycopg2
import psycopg2.extras


def pytest_addoption(parser):
    parser.addoption(
        "--connstring",
        default="host=localhost port=5432 dbname=postgres user=bot_user",
        help="postgresql db connection string",
    )


@pytest.fixture(scope="session")
def db_connstring(request):
    return request.config.getoption("--connstring")


@pytest.fixture(scope="session")
def db_connection(db_connstring):
    with psycopg2.connect(db_connstring) as conn:
        conn.autocommit = True
        yield conn


@pytest.fixture()
def db_cursor(db_connection):
    with db_connection.cursor(cursor_factory=psycopg2.extras.DictCursor) as cur:
        yield cur


@pytest.fixture()
def botpeer():
    return {"chat_id": "833782299", "bot_id": "rtec_test_bot", "bot_platform": "telegram"}


@pytest.fixture()
def mail_account():
    return {"uid": "100500", "email": "yabottest@yandex.ru"}


@pytest.fixture()
def mail_account2():
    return {"uid": "200500", "email": "yasecondmail@yandex.ru"}
