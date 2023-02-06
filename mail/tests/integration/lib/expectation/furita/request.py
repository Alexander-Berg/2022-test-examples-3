from hamcrest import (
    equal_to,
    has_entries,
    has_entry
)
from library.python.testing.pyremock.lib.pyremock import MatchRequest


def make_blackwhitelist_request(uid):
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/api/blackwhitelist"),
        params=has_entries(
            uid=equal_to([str(uid).encode()])
        )
    )


def make_get_request(org_id):
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/v1/domain/rules/get"),
        params=has_entry("orgid", [org_id.encode()])
    )


def make_list_request(uid):
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/api/list.json"),
        params=has_entry("uid", [str(uid).encode()])
    )
