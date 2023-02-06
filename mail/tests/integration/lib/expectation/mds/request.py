import traceback

from hamcrest import equal_to, has_entries
from hamcrest.core.base_matcher import BaseMatcher
from email import message_from_string

from library.python.testing.pyremock.lib.pyremock import MatchRequest

from mail.notsolitesrv.tests.integration.lib.util.headers import Header


__MESSAGES_STORAGE__ = dict()


def make_get_request(stid):
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/gate/get/" + stid),
        params=has_entries(
            elliptics=equal_to([b"1"]),
            service=equal_to([b"nsls"])
        )
    )


def make_put_request_params_matcher(unit_type, ns):
    pairs = {
        "elliptics": [b"1"],
        "service": [b"nsls"]
    }
    if (unit_type is not None):
        pairs["unit_type"] = [str.encode(unit_type)]
    if (ns is not None):
        pairs["ns"] = [ns]
    return has_entries(pairs)


class PutBodyMatcher(BaseMatcher):
    def __init__(
            self,
            expected_existing_headers,
            expected_not_existing_headers,
            expected_equal_headers
    ):
        self.expected_existing_headers = expected_existing_headers
        self.expected_not_existing_headers = expected_not_existing_headers
        self.expected_equal_headers = expected_equal_headers

    def matches(self, item, mismatch_description=None):
        try:
            msg = message_from_string(item)
            __MESSAGES_STORAGE__[msg[Header.MESSAGE_ID]] = msg

            if self.expected_existing_headers:
                for hdr in self.expected_existing_headers:
                    if hdr not in msg:
                        if mismatch_description:
                            mismatch_description.append_text("hdr not found: {}\r\n".format(hdr))
                        return False

            if self.expected_not_existing_headers:
                for hdr in self.expected_not_existing_headers:
                    if hdr in msg:
                        if mismatch_description:
                            mismatch_description.append_text("found hdr: {}\r\n".format(hdr))
                        return False

            if self.expected_equal_headers:
                for hdr, value in self.expected_equal_headers.items():
                    if hdr not in msg:
                        if mismatch_description:
                            mismatch_description.append_text("not found hdr: {}\r\n".format(hdr))
                        return False
                    if value != msg[hdr]:
                        if mismatch_description:
                            mismatch_description.append_text("expect {}: {}\r\n".format(hdr, value))
                            mismatch_description.append_text("actual {}: {}\r\n".format(hdr, msg[hdr]))
                        return False

            return True
        except Exception:
            if mismatch_description:
                mismatch_description.append_text("request matching failed with exception:\r\n")
                mismatch_description.append_text("{}\r\n".format(traceback.format_exc()))
            return False

    def describe_to(self, description):
        pass


def put_body_matcher(expected_existing_headers, expected_not_existing_headers, expected_equal_headers):
    return PutBodyMatcher(
        expected_existing_headers=expected_existing_headers,
        expected_not_existing_headers=expected_not_existing_headers,
        expected_equal_headers=expected_equal_headers)


def make_put_request(
        stid_prefix,
        unit_type="ham",
        ns=b"mail",
        expected_existing_headers: list = None,
        expected_not_existing_headers: list = None,
        expected_equal_headers: dict = None
):
    matcher = MatchRequest(
        method=equal_to("post"),
        path=equal_to("/gate/put/" + stid_prefix),
        params=make_put_request_params_matcher(unit_type, ns)
    )
    if expected_existing_headers or expected_not_existing_headers or expected_equal_headers:
        matcher.body=put_body_matcher(
            expected_existing_headers=expected_existing_headers,
            expected_not_existing_headers=expected_not_existing_headers,
            expected_equal_headers=expected_equal_headers
        )
    return matcher
