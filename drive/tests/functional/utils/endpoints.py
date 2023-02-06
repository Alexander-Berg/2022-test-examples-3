import validators


def get_endpoint(endpoint):
    if endpoint == "testing":
        return "https://testing.carsharing.yandex.net"
    if endpoint == "qa":
        return "https://testing.carsharing.yandex.net?backend_cluster=qa"
    if endpoint == "prestable":
        return "https://prestable.carsharing.yandex.net"
    if endpoint == "stable":
        return "https://stable.carsharing.yandex.net"
    else:
        url = validators.url(endpoint)
        if url:
            return endpoint
        TypeError("Endpoint is not url")
