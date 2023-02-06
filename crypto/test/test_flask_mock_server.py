from crypta.lib.python.test_utils.flask_mock_server import FlaskMockServer


def create_mock_server(name):
    mock = FlaskMockServer(name)

    @mock.app.route("/submit/<int:num>")
    def submit(num):
        return "Got {}".format(num)

    return mock


def response_to_tuple(response):
    return response.status_code, response.text


def test_server_restart():
    result = []

    mock_server = create_mock_server("mock_server")
    for i in range(2):
        assert not mock_server.is_up()

        with mock_server:
            assert mock_server.is_up()
            responses = [
                response_to_tuple(mock_server.get(path, timeout=5))
                for path in ["/submit/1", "/submit/10"]
            ]

            result.append({
                "responses": responses,
                "dump_requests": mock_server.dump_requests(),
            })

    assert not mock_server.is_up()
    return result


def test_multiple_servers():
    mock_server = create_mock_server("mock_server")
    mock_server2 = create_mock_server("mock_server2")

    assert not mock_server.is_up()
    assert not mock_server2.is_up()

    with mock_server:
        assert mock_server.is_up()
        assert not mock_server2.is_up()

        with mock_server2:
            assert mock_server.is_up()
            assert mock_server2.is_up()
            mock_server.get("/submit/1", timeout=5)
            mock_server2.get("/submit/2", timeout=5)

            requests1 = mock_server.dump_requests()
            requests2 = mock_server2.dump_requests()

        assert mock_server.is_up()
        assert not mock_server2.is_up()

    assert not mock_server.is_up()
    assert not mock_server2.is_up()

    return {
        "server1_requests": requests1,
        "server2_requests": requests2,
    }
