from juggler_sdk import Check, Child, NotificationOptions


def get_checks(responsibles=["dskut"]):
    agg_kwargs = {
        "unreach_service": [{"check": "yasm_alert:virtual-meta"}],
        "nodata_mode": "force_ok",
        "unreach_mode": "force_ok",
    }
    meta = {
        "yasm-alert_name": "dskut-dev-cpu-alert",
        "urls": [{
            "url": "https://yasm.yandex-team.ru/chart-alert/alerts=dskut-dev-cpu-alert;",
            "type": "yasm_alert",
            "title": "Golovan alert url"
        }]
    }
    children = [Child(
        host="yasm_alert",
        service="dskut-dev-cpu-alert",
    )]
    notifications = [NotificationOptions(
        template_name="on_status_change",
        template_kwargs={
            "status": [
                {"from": "OK", "to": "CRIT"},
                {"from": "CRIT", "to": "OK"},
                {"from": "WARN", "to": "CRIT"},
                {"from": "CRIT", "to": "WARN"},
            ],
            "login": responsibles,
            "method": ["telegram"],
        }
    )]
    check = Check(
        namespace="dskut",
        host="dskut-dev",
        service="cpu-usage",
        refresh_time=10,
        aggregator="logic_or",
        aggregator_kwargs=agg_kwargs,
        meta=meta,
        children=children,
        notifications=notifications,
    )
    yield check
