import os
import json
import pytest
import yatest

from _pytest.monkeypatch import MonkeyPatch  # noqa

from time import clock

from typing import Callable, List  # noqa

from market.sre.tools.rtc.nanny.manager import Dashboard, Service

from market.front.tools.service_updater.lib import settings


@pytest.fixture(scope="module")
def fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path("market/front/tools/service_updater/lib/tests/fixtures")


@pytest.fixture(scope="module")
def dashboard_fixture(fixtures_dir):  # type: (unicode) -> dict
    with open(os.path.join(fixtures_dir, "dashboard.json")) as f:
        return json.load(f)


@pytest.fixture(scope="module")
def services_fixture(fixtures_dir):  # type: (unicode) -> dict
    with open(os.path.join(fixtures_dir, "services.json")) as f:
        return json.load(f)


@pytest.fixture(scope="module")
def instances_fixture(fixtures_dir):  # type: (unicode) -> dict
    with open(os.path.join(fixtures_dir, "instances.json")) as f:
        return json.load(f)


@pytest.fixture(autouse=True)
def clients_fixture(
        monkeypatch, dashboard_fixture, services_fixture, instances_fixture
):  # type: (MonkeyPatch, dict, dict, dict) -> None
    monkeypatch.setattr(
        'market.front.tools.service_updater.lib.Clients._token',
        lambda _: 'token'
    )

    monkeypatch.setattr(
        'market.sre.tools.rtc.nanny.manager.ServiceRepoManager.get_dashboard',
        lambda _, dash_id: Dashboard.from_dict(dashboard_fixture)
    )

    monkeypatch.setattr(
        'market.sre.tools.rtc.nanny.manager.ServiceRepoManager.get_service',
        lambda _, service_id: Service.from_dict(services_fixture[service_id])
    )

    monkeypatch.setattr(
        'market.sre.tools.rtc.nanny.manager.ServiceRepoManager.update_service',
        lambda _, service, desc: service
    )

    monkeypatch.setattr(
        'market.sre.tools.rtc.nanny.manager.ServiceRepoManager.activate_service',
        lambda _, service: service
    )

    monkeypatch.setattr(
        'infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.get_current_state_instances',
        lambda self, service_id: {'result': instances_fixture[service_id]}
    )

    monkeypatch.setattr('time.sleep', lambda _: None)


@pytest.fixture()
def service_status_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> Callable[[Callable[[unicode], unicode]], None]
    data = {'status': lambda service_id: 'online'}

    monkeypatch.setattr(
        'infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient.get_current_state',
        lambda self, service_id: {'content': {'summary': {'value': data['status'](service_id)}}}
    )

    def set_status(data, value):
        data['status'] = value

    return lambda value: set_status(data, value)


@pytest.fixture()
def cli_read_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> Callable[[unicode], None]
    data = {'readline': 'user data'}

    monkeypatch.setattr(
        'market.front.tools.service_updater.lib.io.__IO.read',
        lambda _: data['readline']
    )

    def set_readline(data, result):
        data['readline'] = result

    return lambda result: set_readline(data, result)


@pytest.fixture()
def capture_output_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> Callable[[], List[unicode]]
    result = ['']

    def write(msg):
        result[len(result) - 1] += msg

    def writeln(msg):
        write(msg)
        result.append('')

    def read():
        out = [r for r in result]
        result.__delslice__(0, len(result))
        result.append('')
        return out

    monkeypatch.setattr(
        'market.front.tools.service_updater.lib.io.__IO.write',
        lambda _, msg: write(msg)
    )

    monkeypatch.setattr(
        'market.front.tools.service_updater.lib.io.__IO.writeln',
        lambda _, msg: writeln(msg)
    )

    return lambda: read()


@pytest.fixture()
def interactive_input_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> None
    monkeypatch.setitem(settings, 'interactive', True)


@pytest.fixture()
def non_interactive_input_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> None
    monkeypatch.setitem(settings, 'interactive', False)


@pytest.fixture()
def dry_run_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> None
    monkeypatch.setitem(settings, 'dry', True)


@pytest.fixture()
def no_dry_run_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> None
    monkeypatch.setitem(settings, 'dry', False)


@pytest.fixture()
def clock_fixture(
        monkeypatch
):  # type: (MonkeyPatch) -> Callable[[int], None]
    data = {'clock': lambda: clock()}

    monkeypatch.setattr('time.clock', lambda: data['clock']())

    def set_clock(data, value):
        data['clock'] = value

    return lambda value: set_clock(data, value)
