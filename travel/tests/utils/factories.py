import factory

from travel.rasp.pathfinder_proxy.const import TTransport
from travel.rasp.pathfinder_proxy.services.interline_service import InterlineService
from travel.rasp.pathfinder_proxy.services.ticket_daemon_service import TicketDaemonService
from travel.rasp.pathfinder_proxy.services.train_api_service import TrainApiService
from travel.rasp.pathfinder_proxy.settings import Settings
from travel.rasp.pathfinder_proxy.tariff_collectors.interline_collector import InterlineCollector
from travel.rasp.pathfinder_proxy.tariff_collectors.ticket_daemon_collector import TicketDaemonCollector
from travel.rasp.pathfinder_proxy.tariff_collectors.train_api_collector import TrainApiCollector
from travel.rasp.pathfinder_proxy.views import Handler


class HandlerFactory(factory.Factory):
    class Meta:
        model = Handler

    morda_backend_client = None
    train_api_client = None
    ticket_daemon_client = None
    train_fee_service = None
    cache = None
    settings = Settings()


class TrainApiCollectorFactory(factory.Factory):
    class Meta:
        model = TrainApiCollector

    client = None
    cache = None
    transport_code = TTransport.get_name(TTransport.TRAIN)


class TrainApiServiceFactory(factory.Factory):
    class Meta:
        model = TrainApiService

    client = None
    cache = None
    settings = Settings()


class TicketDaemonCollectorFactory(factory.Factory):
    class Meta:
        model = TicketDaemonCollector

    client = None
    cache = None
    transport_code = TTransport.get_name(TTransport.PLANE)


class TicketDaemonServiceFactory(factory.Factory):
    class Meta:
        model = TicketDaemonService

    client = None
    cache = None


class InterlineCollectorFactory(factory.Factory):
    class Meta:
        model = InterlineCollector

    client = None
    cache = None
    transport_code = TTransport.get_name(TTransport.PLANE)


class InterlineServiceFactory(factory.Factory):
    class Meta:
        model = InterlineService

    client = None
    cache = None
