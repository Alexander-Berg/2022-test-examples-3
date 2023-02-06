from typing import List, Union
from urllib.parse import unquote
from hamcrest import equal_to, has_entries, contains_inanyorder

from library.python.testing.pyremock.lib.pyremock import MatchRequest

from mail.notsolitesrv.tests.integration.lib.expectation.matchers import transformed_by


Uids = Union[str, int, List[str], List[int]]
Emails = Union[str, List[str]]


def make_request(uids: Uids, emails: Emails, opt_in_uids: Uids = []):
    def split_param_values(values):
        return unquote(values[0].decode()).split(",") if values and values[0] else []

    def make_uids_list(uids: Uids):
        return [str(uid) for uid in (uids if isinstance(uids, list) else [uids])]

    uids = make_uids_list(uids)
    opt_in_uids = make_uids_list(opt_in_uids)
    emails = emails if isinstance(emails, list) else [emails]
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/api/async/mail/subscriptions/status"),
        params=has_entries(
            uid=transformed_by(split_param_values, contains_inanyorder(*uids)),
            opt_in_subs_uid=transformed_by(split_param_values, contains_inanyorder(*opt_in_uids)),
            email=transformed_by(split_param_values, contains_inanyorder(*emails)),
        )
    )
