def test_ping(context):
    response = context.furita_api.ping()
    assert response.status_code == 200
    assert response.text == "pong"
