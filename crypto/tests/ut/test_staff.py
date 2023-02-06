from __future__ import unicode_literals
import pytest

from mock import patch, Mock

from crypta.graph.staff.lib.staff_exporter import IdType, StaffLoader


class FakeResponse(Mock):

    """Retrun mocked response from http"""

    url = "fake://foo.boo.koo/path?args"

    def __init__(self, content=None, num_pages=None, *args, **kwargs):
        super(FakeResponse, self).__init__(*args, **kwargs)
        self.content = content
        self.num_pages = num_pages

    def __call__(self, *args, **kwargs):
        super(FakeResponse, self).__call__(*args, **kwargs)
        return self

    def json(self):
        return {"pages": self.num_pages, "result": self.content}


class TestIdType(object):

    """Check is id type correct map values"""

    @pytest.mark.parametrize(
        "value",
        (
            "another_work_email",
            "apple_id",
            "email",
            "gmail",
            "home_email",
            "personal_email",
            "play_market_id",
        ),
    )
    def test_emails(self, value):
        """Should check is all email mapped correct"""
        assert IdType.clean_item(value) == IdType.EMAIL

    @pytest.mark.parametrize(
        "value, etalon",
        (
            ("home_page", "site"),
            ("login_mk", "moi_krug"),
            ("mk", "moi_krug"),
            ("login_lj", "blog"),
            ("login_skype", "skype"),
        ),
    )
    def test_extra_keys(self, value, etalon):
        """Should check is extra keys maped correct"""
        assert IdType.clean_item(value) == etalon
        assert value != etalon

    @pytest.mark.parametrize(
        "value",
        (
            IdType.EMAIL,
            IdType.PHONE,
            IdType.JABBER,
            IdType.ICQ,
            IdType.SKYPE,
            IdType.TWITTER,
            IdType.MOI_KRUG,
            IdType.PERSONAL_SITE,
            IdType.LIVEJOURNAL,
            IdType.GITHUB,
            IdType.FACEBOOK,
            IdType.VKONTAKTE,
            IdType.HABRAHABR,
            IdType.INSTAGRAM,
            IdType.FLICKR,
            IdType.TUMBLR,
            IdType.BLOGSPOT,
            IdType.TELEGRAM,
            IdType.YAMB,
            IdType.ASSISTENT,
            IdType.SITE,
            IdType.BLOG,
            IdType.STAFF,
            IdType.STAFF_LOGIN,
            IdType.PASSPORT_LOGIN,
            IdType.UNKNOWN,
        ),
    )
    def test_as_is(self, value):
        """Each value should map the same"""
        assert IdType.clean_item(value) == value

    def test_as_is_known_keys(self):
        """Should chek is known keys correct"""
        for key in IdType.KNOWN_KEYS:
            assert IdType.clean_item(key) == key


class TestStaffLoader(object):

    """Staff loader class test cases"""

    raw_user = {
        "accounts": [
            {"id": 12345, "private": False, "type": "gmail", "value": "FakeUser@gmail.com"},
            {"id": 12345, "private": False, "type": "telegram", "value": "FakeUser"},
            {"id": 12345, "private": False, "type": "github", "value": "FakeUser"},
        ],
        "contacts": [],
        "emails": [
            {"address": "jhon.smith@yandex-team.ru", "id": 123458, "source_type": "staff"},
            {"address": "FakeUser@gmail.com", "id": 123459, "source_type": "staff"},
            {"address": "jhon.smith@yandex-team.com", "id": 123452, "source_type": "passport"},
            {"address": "jhon.smith@yandex-team.com.tr", "id": 123453, "source_type": "passport"},
            {"address": "jhon.smith@yandex-team.com.ua", "id": 123454, "source_type": "passport"},
            {"address": "jhon.smith@yandex-team.ru", "id": 123455, "source_type": "passport"},
            {"address": "FakeUser@ya.ru", "id": 123450, "source_type": "staff"},
            {"address": "FakeUser@yandex.by", "id": 123451, "source_type": "staff"},
            {"address": "FakeUser@yandex.com", "id": 123452, "source_type": "staff"},
            {"address": "FakeUser@yandex.kz", "id": 123453, "source_type": "staff"},
            {"address": "FakeUser@yandex.ru", "id": 123454, "source_type": "staff"},
            {"address": "FakeUser@yandex.ua", "id": 123455, "source_type": "staff"},
        ],
        "id": 12345,
        "is_deleted": False,
        "login": "jhon.smith",
        "official": {
            "affiliation": "yandex",
            "contract_ended_at": None,
            "duties": {"en": "", "ru": ""},
            "employment": "full",
            "has_byod_access": False,
            "is_dismissed": False,
            "is_homeworker": False,
            "is_robot": False,
            "join_at": "2018-01-01",
            "nda_ended_at": None,
            "quit_at": "2018-09-19",
        },
        "name": {
            "first": "fake",
            "last": "user",
        },
        "personal": {
            "gender": "male",
            "birthday": "1990-01-01",
        },
        "phones": [
            {
                "description": "",
                "id": 12345,
                "is_main": True,
                "kind": "common",
                "number": "+79001234567",
                "protocol": "all",
                "type": "mobile",
            }
        ],
        "work_email": "jhon.smith@yandex-team.ru",
        "work_phone": 12345,
        "yandex": {"login": "FakeUser"},
        "gender": "male",
    }

    parsed_user = {
        "passport_login": "fakeuser",
        "staff_login": "jhon.smith",
        "name": {"last": "user", "first": "fake"},
        "gender": "male",
        "dt": "1990-01-01",
        "data": {
            "github": ["fakeuser"],
            "telegram": ["fakeuser"],
            "phone": ["+712345", "+79001234567"],
            "email": ["fakeuser@gmail.com", "fakeuser@yandex.ru", "jhon.smith@yandex-team.ru"],
        },
    }

    @patch.object(StaffLoader, "parse")
    @patch.object(StaffLoader, "fetch")
    def test_staff_loader_process(self, mocked_fetch, mocked_parse):
        """Should check is staff loader process call fetch and parse from process"""
        mocked_fetch.return_value = Mock()
        mocked_parse.return_value = Mock()
        staff_loader = StaffLoader()
        staff_loader.process()
        mocked_fetch.assert_called_once()
        mocked_parse.assert_called_once_with(mocked_fetch.return_value)

    @patch("requests.Session.get", FakeResponse("some test content", 3))
    def test_fetch_method(self):
        """Should check is fetch correct save yield staff data"""
        staff_loader = StaffLoader()
        parts = tuple(staff_loader.fetch())
        assert len(parts) == 3
        assert parts == ({"pages": 3, "result": "some test content"},) * 3

    def test_parse(self):
        """Should check is staff loader correct parse data"""
        staff_loader = StaffLoader()
        data = staff_loader.parse([{"result": [self.raw_user]}])
        assert data == [self.parsed_user]
