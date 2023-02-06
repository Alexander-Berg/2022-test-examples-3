from mock import MagicMock

import token_utils

METRICS_DUMMY_TOKEN = "MY-M$TRIC$-T0KEN"

get_metrics_token_mock = MagicMock(return_value=METRICS_DUMMY_TOKEN)

get_wrong_metrics_token_mock = MagicMock(return_value="WR0NG_T0K3N")


def get_headers_stub(stub=METRICS_DUMMY_TOKEN):
    return token_utils.get_metrics_oauth_headers(stub)
