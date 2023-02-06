import yatest.common
import json
import numbers

from sandbox.projects.woland.WolandAlerting import post_alerts

panels_path = yatest.common.source_path('search/tools/woland/panels')
binary_path = yatest.common.build_path('search/tools/woland/woland')

WARN_LEVEL = 'warn'
CRIT_LEVEL = 'crit'


def _check_non_ascii_symbols(string: str, entity_name: str) -> None:
    try:
        # Yasm/Juggler wants ASCII from us
        string.encode('ascii')
    except UnicodeEncodeError as e:
        raise AssertionError(f'{entity_name} {string} contans non ASCII symbols: {e}')


def test_alerts_generation():
    logs = []
    checks_list = post_alerts.prepare_alerts(binary_path, panels_path, logs)
    for prefix, alerts in checks_list.items():
        _check_non_ascii_symbols(prefix, 'Alerts perfix')

        for alert in alerts:
            signal_name = alert['signal']

            # Check all flaps times is numeric
            if "juggler_check" in alert:
                if "flaps" in alert["juggler_check"]:
                    for _, v in alert["juggler_check"]["flaps"].items():
                        assert isinstance(v, numbers.Number), f"Alert {prefix}/{signal_name} has non-numeric flaps config value {v}"

            _check_non_ascii_symbols(json.dumps(alert), 'Alert')

            # Check low and high thresholds for every level
            for level in (WARN_LEVEL, CRIT_LEVEL):
                lower = alert[level][0]
                higher = alert[level][1]
                if lower is not None and higher is not None:
                    assert lower <= higher, f'Alert {prefix}/{signal_name} have incosistent thresholds: {alert[level]} left threshold shouldn\t be greater than right one'

            if alert[WARN_LEVEL][1] is not None and alert[CRIT_LEVEL][0] is not None:
                assert alert[WARN_LEVEL][1] <= alert[CRIT_LEVEL][0], 'Higher warn threshold shouldn\'t be greater than lower crit threshold'
