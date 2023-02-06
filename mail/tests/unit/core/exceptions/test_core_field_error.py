from marshmallow.validate import Email

from mail.payments.payments.core.exceptions import CoreFieldError


class TestCoreFieldError:
    def test_fields(self):
        try:
            raise CoreFieldError(
                fields={
                    "name": "invalid",
                    "persons.ceo.email": Email.default_message
                }
            )
        except CoreFieldError as e:
            assert e.fields == {
                "persons": {
                    "ceo": {
                        "email": [Email.default_message]
                    }
                },
                "name": ["invalid"]
            }
