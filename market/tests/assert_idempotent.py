from typing import Callable, Any  # noqa

from market.sre.tools.rtc.nanny.models.service import Service as RTCService  # noqa


def assert_idempotent(service, mutator):  # type: (RTCService, Callable[[], Any]) -> None
    mutator()
    first_runtime_diff = service.runtime_attrs.diff
    first_info_diff = service.info_attrs.diff

    mutator()
    assert first_runtime_diff == service.runtime_attrs.diff
    assert first_info_diff == service.info_attrs.diff
