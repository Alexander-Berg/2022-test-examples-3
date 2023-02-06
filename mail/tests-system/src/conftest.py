from .fake_blackbox import FakeBlackbox
from .fake_telegram import FakeTelegram
from .utils import DB, wait_webservice_is_ready, wait_db_is_ready, otp, link
import psycopg2
import psycopg2.extras
import pytest


@pytest.fixture(autouse=True)
def fake_blackbox():
    fake = FakeBlackbox(host="localhost", port=8082)
    fake.start()
    yield fake
    fake.fini()


@pytest.fixture(autouse=True)
def fake_telegram():
    fake = FakeTelegram(
        host="localhost", port=8081, bind_path="http://localhost:4080/fake_telegram_gate"
    )
    fake.start()
    yield fake
    fake.fini()


@pytest.fixture()
def botpeer():
    return {"chat_id": "833782299", "bot_id": "fake_bot_id", "bot_platform": "telegram"}


@pytest.fixture()
def mail_account(fake_blackbox):
    account = {"uid": "100500", "email": "yabottest@yandex.ru"}
    fake_blackbox.set_response(int(account["uid"]))
    return account


@pytest.fixture()
def mail_account_with_bb_error(fake_blackbox):
    account = {"uid": "109876", "email": "error@notyandex.ru"}
    fake_blackbox.set_response_error("db query error")
    return account


@pytest.fixture()
def mail_account_with_empty_bb_response(fake_blackbox):
    account = {"uid": "543210", "email": "empty@notyandex.ru"}
    return account


@pytest.fixture()
def botpeer_mail_account_link(botpeer, mail_account, db):
    db.create_link(link(botpeer, mail_account))


@pytest.fixture()
def botpeer_mail_account_otp(botpeer, mail_account, db):
    db.create_otp(otp(botpeer, mail_account))


@pytest.fixture(scope="session", autouse=True)
def mocks_ready(db_connstring):
    wait_db_is_ready(db_connstring)
    wait_webservice_is_ready("http://localhost:8080/ping")


@pytest.fixture(autouse=True)
def clean_db(db):
    db.clean_links()
    db.clean_otps()


@pytest.fixture()
def db(db_cursor):
    return DB(db_cursor)


@pytest.fixture()
def db_cursor(db_connection):
    with db_connection.cursor(cursor_factory=psycopg2.extras.DictCursor) as cur:
        yield cur


@pytest.fixture(scope="session")
def db_connection(db_connstring):
    with psycopg2.connect(db_connstring) as conn:
        conn.autocommit = True
        yield conn


@pytest.fixture(scope="session")
def db_connstring():
    return "host=localhost port=5432 dbname=postgres user=bot_user"
