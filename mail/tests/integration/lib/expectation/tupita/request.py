from hamcrest import (
    anything,
    equal_to,
    is_,
    has_entries
)
from library.python.testing.pyremock.lib.pyremock import HttpMethod, MatchRequest


def make_check_request(uid):
    return MatchRequest(
        method=equal_to("post"),
        path=equal_to("/check"),
        params=has_entries(uid=[str(uid).encode()], reqid=anything())
    )


def make_conditions_convert_request(org_id):
    return MatchRequest(
        method=is_(HttpMethod.POST),
        path=equal_to("/api/mail/conditions/convert"),
        params=has_entries(org_id=anything())
    )
