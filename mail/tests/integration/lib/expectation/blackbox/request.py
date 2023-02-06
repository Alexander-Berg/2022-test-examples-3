from hamcrest import equal_to, has_entries

from library.python.testing.pyremock.lib.pyremock import MatchRequest


def make_params_matcher(user):
    if user.is_mailish:
        return has_entries(method=[b"userinfo"], uid=[str(user.uid).encode()], attributes=[b"1031"])
    else:
        return has_entries(method=[b"userinfo"], login=[user.email.encode()], attributes=[b"1031"])


def make_request(user):
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/blackbox"),
        params=make_params_matcher(user)
    )
