import requests


def test_list_all_rules(api, init_db):
    return requests.get(f"http://localhost:{api.port}/lab/constructor/rule").json()
