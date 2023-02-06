from hamcrest import anything, equal_to, has_entries

from library.python.testing.pyremock.lib.pyremock import MatchRequest


def make_request(request_body_matcher=anything()):
    return MatchRequest(
        method=equal_to("post"),
        path=equal_to("/1/save"),
        params=has_entries(service=[b"nsls"], session_id=anything()),
        body=request_body_matcher
    )
