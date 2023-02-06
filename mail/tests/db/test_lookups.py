import pytest

from fan.models import UnsubscribeListElement

pytestmark = pytest.mark.django_db


class TestUpperInLookup:
    @pytest.fixture
    def general_email(self, unsub_list_general):
        email = "general@example.com"
        unsub_list_general.upsert_element(email)
        return email

    @pytest.fixture
    def unsubscribed_email(self, unsub_list):
        email = "unsubscribed@example.com"
        unsub_list.upsert_element(email)
        return email

    @pytest.fixture(params=[0, 1])
    def lookup_to_expected_result(
        self, request, unsub_list_general, unsub_list, general_email, unsubscribed_email
    ):
        assert general_email != unsubscribed_email
        assert unsubscribed_email not in unsub_list_general.elements.all().values_list(
            "email", flat=True
        )
        assert general_email not in unsub_list.elements.all().values_list("email", flat=True)

        return [
            {
                "lookup": {
                    "list_id__in": [unsub_list_general.id, unsub_list.id],
                    "email__upperin": [general_email, unsubscribed_email],
                },
                "result": [unsubscribed_email, general_email],
            },
            {
                "lookup": {
                    "list_id__in": [unsub_list_general.id, unsub_list.id],
                    "email__upperin": [],
                },
                "result": [],
            },
        ][request.param]

    def test(self, lookup_to_expected_result):
        emails = set(
            UnsubscribeListElement.objects.filter(
                **lookup_to_expected_result["lookup"]
            ).values_list("email", flat=True)
        )
        assert emails == set(lookup_to_expected_result["result"])
