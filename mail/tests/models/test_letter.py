import pytest
from fan.models import letter as letter_module
from fan.testutils.matchers import is_subdict

pytestmark = pytest.mark.django_db


class TestLetterVerification:
    @pytest.fixture
    def valid_letter(self, letter):
        letter.template_meta = {
            "parsed": True,
            "extensions:stats": {},
        }
        return letter

    @pytest.fixture
    def attachment(self, valid_letter):
        att = letter_module.LetterAttachment(letter=valid_letter)
        att.save()
        return att

    def test_valid(self, valid_letter):
        assert valid_letter.verify_template_meta()["result"]

    def test_unparsed(self, valid_letter):
        valid_letter.template_meta["parsed"] = False
        valid_letter.template_meta["parse:error:message"] = "error"
        assert valid_letter.verify_template_meta()["result"] == False
        assert valid_letter.verify_template_meta()["template_error"] == "error"
        assert valid_letter.verify_template_meta()["error_line_no"] == None

    def test_allowed_domains(self, valid_letter):
        valid_letter.allowed_links = [
            "https://example.com",
            "http://example.ru/some/path",
        ]

        valid_letter.allowed_domains = ["yandex.ru", "facebook.com"]
        for allowed_domain in ["example.com", "example.ru", "yandex.ru", "facebook.com"]:
            assert allowed_domain in valid_letter.verify_template_meta()["allowed_domains"]

    def test_attachments(self, valid_letter, attachment):
        valid_letter.template_meta["extensions:stats"]["attachments"] = ["existent", "nonexistent"]
        attachment.uri = "existent"
        attachment.save()

        actual = valid_letter.verify_template_meta()["attachments"]
        assert is_subdict({"used": True, "uploaded": True, "error": False}, actual["existent"])
        assert is_subdict({"used": True, "uploaded": False, "error": True}, actual["nonexistent"])

    def test_pixel(self, valid_letter):
        valid_letter.template_meta["extensions:stats"]["pixel_usage"] = 2

        actual = valid_letter.verify_template_meta()
        assert is_subdict({"result": False, "pixel_used": 2}, actual)
