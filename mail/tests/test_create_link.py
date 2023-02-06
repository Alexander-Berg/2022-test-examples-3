import pytest
from psycopg2.errors import UniqueViolation, CheckViolation
from hamcrest import assert_that, contains
from .utils import exec_procedure, all_links


@pytest.fixture(autouse=True)
def clean_links(db_cursor):
    db_cursor.execute("DELETE FROM botdb.links")


@pytest.fixture()
def link(botpeer, mail_account):
    return {**botpeer, **mail_account, "extra": {"context_id": "o3d71r6LK8c1"}}


@pytest.fixture()
def second_link(botpeer, mail_account2):
    return {**botpeer, **mail_account2, "extra": {"context_id": "n3dQuq6LSOs1"}}


def create_link(cur, link):
    exec_procedure(cur, "code.create_link", **link)


def test_create_link(db_cursor, link):
    create_link(db_cursor, link)
    assert_that(all_links(db_cursor), contains(link))


def test_cannot_create_duplicate_link(db_cursor, link):
    create_link(db_cursor, link)
    with pytest.raises(UniqueViolation):
        create_link(db_cursor, link)


def test_cannot_create_multiple_links_for_one_botpeer(db_cursor, link, second_link):
    create_link(db_cursor, link)
    with pytest.raises(UniqueViolation):
        create_link(db_cursor, second_link)


def test_cannot_create_link_with_empty_uid(db_cursor, link):
    link["uid"] = ""
    with pytest.raises(CheckViolation):
        create_link(db_cursor, link)


def test_cannot_create_link_with_empty_email(db_cursor, link):
    link["email"] = ""
    with pytest.raises(CheckViolation):
        create_link(db_cursor, link)
