from psycopg2 import errors
from sqlalchemy.exc import IntegrityError

from search.martylib.db_utils import session_scope, to_model
from search.martylib.test_utils import TestCase

from search.mon.warden.src import utils
from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.src.services.model import Warden


WARDEN_CLIENT = Warden()

PRIVATE_METRIC_TYPE = 'private-type'
PRIVATE_METRIC_KEY = 'private-metric'
ALLOWED_METRIC_KEY = 'allowed-metric'
METRIC_VALUES = {
    PRIVATE_METRIC_KEY: 30,
    ALLOWED_METRIC_KEY: 35,
}


def setup_metrics(default_metrics: bool = True):
    try:
        with session_scope() as session:
            if default_metrics:
                utils.setup_metrics()
            private_metric_type = to_model(
                metric_pb2.MetricType(
                    key=PRIVATE_METRIC_TYPE,
                    name='Private Metric Type',
                    owners=metric_pb2.MetricOwners(logins=['owner-user']),
                    is_private=True,
                    is_additive=True,
                )
            )
            private_metrics = [
                to_model(
                    metric_pb2.Metric(
                        type=PRIVATE_METRIC_TYPE,
                        key=PRIVATE_METRIC_KEY,
                        name='Private Metric',
                        owners=metric_pb2.MetricOwners(logins=['owner-user'], abc=['owner-abc']),
                    )
                ),
                to_model(
                    metric_pb2.Metric(
                        type=PRIVATE_METRIC_TYPE,
                        key=ALLOWED_METRIC_KEY,
                        name='Allowed Private Metric',
                        owners=metric_pb2.MetricOwners(logins=['test-user'], abc=['test-abc']),
                    )
                )
            ]
            session.add(private_metric_type)
            session.add_all(private_metrics)
    except IntegrityError as e:
        if not isinstance(e.orig, errors.UniqueViolation):
            raise


@TestCase.mock_auth(login='login', roles=['warden/admin'])
def setup_metric_values(incident_key='INCIDENT-WITH-METRIC-VALUES'):
    setup_metrics()
    for metric_key in (PRIVATE_METRIC_KEY, ALLOWED_METRIC_KEY):
        for value in (10, 20, METRIC_VALUES[metric_key]):
            WARDEN_CLIENT.add_metric_value(
                request=metric_pb2.AddMetricValueRequest(
                    incident_key=incident_key,
                    metric_key=metric_key,
                    value=value,
                ),
                context=None,
            )


@TestCase.mock_auth(login='login', roles=['warden/admin'])
def setup_common_metric_values(incident_key='INCIDENT-WITH-METRIC-VALUES'):
    setup_metrics()
    for value in (10, 20):
        WARDEN_CLIENT.add_metric_value(
            request=metric_pb2.AddMetricValueRequest(
                incident_key=incident_key,
                metric_key='ydt',
                value=value,
            ),
            context=None,
        )
