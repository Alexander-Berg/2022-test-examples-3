import json

from dataclasses import dataclass
from typing import List, Union


@dataclass
class SubscriptionStatus:
    uid: str
    email: str
    status: str


def make_success_body_response(status: Union[SubscriptionStatus, List[SubscriptionStatus]]) -> str:
    return json.dumps(dict(
        subscriptions=[
            dict(uid=s.uid, email=s.email, status=s.status)
            for s in (status if isinstance(status, list) else [status])
        ]
    ))


def make_server_error_body_response():
    return "msearch error some text"
