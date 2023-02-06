import pytest
from fan.lists.maillist import retrieve_maillist_data


pytestmark = pytest.mark.django_db


def test_data_is_correct(maillist, maillist_csv_content):
    data = retrieve_maillist_data(maillist)
    assert isinstance(data, str)
    assert data == maillist_csv_content
