import json

from typing import Mapping


def make_success_body_response(settings: Mapping[str, str]) -> str:
    return json.dumps(dict(
        settings=dict(
            single_settings=settings
        )
    ))


def make_server_error_body_response():
    return "msettings error some text"
