import pytest

from mail.ipa.ipa.core.entities.enums import UserImportError

unknown_error_params = pytest.mark.parametrize('user_error,collector_status', (
    (None, None),
    ('some error', None),
    (None, 'some error'),
    ('some error', 'some error'),
))


ok_params = pytest.mark.parametrize('user_error,collector_status', (
    (None, 'ok'),
))


class TestUserImportError:
    unknown_error_params = pytest.mark.parametrize('user_error,collector_status', (
        (None, None),
        ('some error', None),
        (None, 'some error'),
        ('some error', 'some error'),
    ))

    ok_params = pytest.mark.parametrize('user_error,collector_status', (
        (None, 'ok')
    ))

    class TestGetError:
        @pytest.fixture
        def returned(self, user_error, collector_status):
            return UserImportError.get_error(
                user_error=user_error,
                collector_status=collector_status,
            )

        @unknown_error_params
        def test_get_error_unknown_error(self, returned):
            assert returned == UserImportError.UNKNOWN_ERROR

        @ok_params
        def test_get_error_ok(self, returned):
            assert returned is None

    class TestGetErrorStr:
        @pytest.fixture
        def returned(self, user_error, collector_status):
            return UserImportError.get_error_str(
                user_error=user_error,
                collector_status=collector_status,
            )

        @unknown_error_params
        def test_get_error_str_unknown_error(self, returned):
            assert returned == 'unknown_error'

        @ok_params
        def test_get_error_str_ok(self, returned):
            assert returned == ''
