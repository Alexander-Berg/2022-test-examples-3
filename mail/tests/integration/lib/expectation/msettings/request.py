from typing import Union
from urllib.parse import unquote
from hamcrest import equal_to, has_entries, contains_inanyorder

from library.python.testing.pyremock.lib.pyremock import MatchRequest

from mail.notsolitesrv.tests.integration.lib.expectation.matchers import transformed_by


def make_request(uid: Union[str, int]):
    def split_param_values(values):
        return unquote(values[0].decode()).split("\r") if values and values[0] else []

    params = ["mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled"]

    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/get_params"),
        params=has_entries(
            uid=[str(uid).encode()],
            settings_list=transformed_by(split_param_values, contains_inanyorder(*params)),
            service=[b"nsls"]
        )
    )


def make_profile_request(uid: Union[str, int]):
    return MatchRequest(
        method=equal_to("get"),
        path=equal_to("/get_profile"),
        params=has_entries(
            uid=[str(uid).encode()],
            settings_list=[b"from_name"],
            service=[b"nsls"],
            ask_validator=[b"y"],
        )
    )
