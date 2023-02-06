from ...core.user_setup import UserSetup


class StubUserSetup(UserSetup):

    @classmethod
    def phone_from_request(cls, request):
        if request.user.phone:
            phone = request.user.phone.as_e164
        else:
            phone = '+71234567890'
        return phone, False
