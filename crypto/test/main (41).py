from crypta.lib.python.juggler.juggler_client import JugglerClient


def test_basic(mock_juggler_server):
    juggler_client = JugglerClient(mock_juggler_server.host, mock_juggler_server.port, "CRYPTA")
    juggler_client.send_ok("OK.local", "OK_service", "OK_description")
    juggler_client.send_warn("WARN.local", "WARN_service", "WARN_description")
    juggler_client.send_crit("CRIT.local", "CRIT_service", "CRIT_description")
    return mock_juggler_server.dump_events_requests()
