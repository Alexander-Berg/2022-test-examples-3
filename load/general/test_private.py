from load.projects.cloud.loadtesting.server.obfuscator.base import Obfuscator
from yandex.cloud.priv.loadtesting.v2 import test_pb2 as test_private_pb2
from yandex.cloud.priv.loadtesting.v2 import test_service_pb2 as test_private_service_pb2


class MonitoringReport(Obfuscator):
    target_class = test_private_pb2.MonitoringReport

    def data(self) -> dict:
        report: test_private_pb2.MonitoringReport = self.original

        return {
            'test_id': report.test_id,
            'charts': len(report.charts),
            'finished': report.finished,
            'imbalance_point': report.imbalance_point,
            'imbalance_ts': report.imbalance_ts,
        }


class MonitoringChart(Obfuscator):
    target_class = test_private_pb2.MonitoringChart

    def data(self) -> dict:
        chart: test_private_pb2.MonitoringChart = self.original

        return {
            'monitored_host': chart.monitored_host,
            'test_id': chart.test_id,
            'name': chart.name,
            'description': chart.description,
        }


class GetMonitoringReportRequest(Obfuscator):
    target_class = test_private_service_pb2.GetMonitoringReportRequest

    def data(self) -> dict:
        request: test_private_service_pb2.GetMonitoringReportRequest = self.original

        return {
            'test_id': request.test_id,
        }
