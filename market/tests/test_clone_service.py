from market.sre.tools.rtc.nanny.models.service import Service  # noqa
from market.sre.tools.rtc.nanny.scenarios.clone_service import CloneService  # noqa


def test_step_update_tickets_integration(
    nanny_service,  # type: Service
    clone_service,  # type: CommonService
):  # type: (...) -> None
    tickets_integration_before = nanny_service.info_attrs.tickets_integration.content
    clone_service._step_update_tickets_integration(nanny_service)
    tickets_integration_after = nanny_service.info_attrs.tickets_integration.content
    assert tickets_integration_before == tickets_integration_after


def test_get_datasources_resource(
    nanny_service,  # type: Service
    clone_service,  # type: CommonService
):  # type: (...) -> None
    assert clone_service._get_datasources_resource(nanny_service) is None
