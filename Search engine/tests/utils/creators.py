import typing

from search.martylib.http.exceptions import BadRequest
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import metric_pb2, component_pb2
from search.mon.warden.src.services.model import Warden


@TestCase.mock_auth(login='test-user', roles=['warden/admin'])
def create_components(*args):
    for component in args:
        try:
            response = Warden().create_component(request=component, context=None)
        except Exception as e:
            raise e
        if response.error:
            raise BadRequest(f'Error with component "{component.component.slug}" creation: {response.error}')


@TestCase.mock_auth(login='test-user', roles=['warden/admin'])
def create_alerts(*args) -> typing.List[str]:
    create_alerts_list = []
    for alert in args:
        try:
            response = Warden().add_alert(request=alert, context=None)
            create_alerts_list.append(response.alert_id)
        except Exception as e:
            raise e
    return create_alerts_list


@TestCase.mock_auth(login='test-user', roles=['warden/admin'])
def create_functionalities(*args):
    created_functionality_list = []
    for funct in args:
        try:
            response = Warden().add_functionality(request=funct, context=None)
            created_functionality_list.append(response.functionality_id)
        except Exception as e:
            raise e
    return created_functionality_list


@TestCase.mock_auth(login='test-user', roles=['warden/admin'])
def create_metric_types(metric_types: typing.Iterable[metric_pb2.MetricType]):
    for metric_type in metric_types:
        Warden().create_metric_type(request=metric_type, context=None)


@TestCase.mock_auth(login='test-user', roles=['warden/admin'])
def create_metrics(metrics: typing.Iterable[metric_pb2.Metric]):
    for metric in metrics:
        Warden().create_metric(request=metric, context=None)


@TestCase.mock_auth(login='test-user', roles=['warden/admin'])
def create_panels(panels: typing.Iterable[component_pb2.AddPanelRequest]):
    for panel in panels:
        Warden().add_panel(request=panel, context=None)
