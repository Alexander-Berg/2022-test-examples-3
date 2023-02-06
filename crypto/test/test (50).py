import requests


def test_server(resource_service, cluster_envs):
    url_templates = [
        "/version/{dc}/{resource}",
        "/get/{dc}/{resource}/0",
        "/get/{dc}/{resource}/1",
        "/report_ok/{dc}/{resource}/0",
        "/get/{dc}/{resource}/0",
        "/report_ok/{dc}/{resource}/1",
        "/get_report_counts/{dc}/{resource}/0",
        "/get_report_counts/{dc}/{resource}/1",
    ]
    requested_resources = ["present", "not_found", "unknown"]
    urls = [
        url.format(dc=dc, resource=resource)
        for dc in cluster_envs.keys()
        for resource in requested_resources
        for url in url_templates
    ]

    responses = [requests.get(resource_service.url_prefix + url) for url in urls]
    stats = resource_service.get_sensors()
    stats["metrics"] = sorted([sensor for sensor in stats["metrics"] if sensor["labels"]["cmd"] != "ping"])
    return {
        "responses": [(url, r.status_code, r.text) for url, r in zip(urls, responses)],
        "stats": stats,
    }
