import string

import pytest
import yatest.common

from crypta.buchhalter.services.main.lib.common.helpers import indexes
from crypta.lib.python.identifiers import identifiers


NAME_ALPHABET = string.ascii_lowercase + ".-_"


@pytest.fixture
def index():
    index_path = yatest.common.source_path("crypta/buchhalter/services/main/config/shadow_dmp_index/shadow_dmp_index.yaml")
    return indexes.get_shadow_dmp_index(index_path)


def test_copy_paste(index):
    audience_logins = [item.AudienceLogin for item in index]
    names = [item.Name for item in index]
    emails = sum([list(item.Emails) for item in index], [])

    assert len(audience_logins) == len(set(audience_logins)), "Same audience login in two or more entries"
    assert len(names) == len(set(names)), "Same name in two or more entries"
    assert len(emails) == len(set(emails)), "Same email in two or more entries"


def test_basic(index):
    for sdmp in index:
        login = identifiers.Login(sdmp.AudienceLogin)
        assert login.is_valid(), "Invalid login '{}'".format(sdmp.AudienceLogin)
        assert login.normalize == sdmp.AudienceLogin, "Denormalized login '{}'".format(sdmp.AudienceLogin)
        assert all(c in NAME_ALPHABET for c in sdmp.Name), "Invalid name '{}'".format(sdmp.Name)

        for email in sdmp.Emails:
            assert identifiers.Email(email).is_valid(), "Invalid email '{}'".format(email)
