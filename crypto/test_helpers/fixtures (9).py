import pytest
from yatest.common import network

from crypta.lib.python.smtp.test_helpers import mail_formatters
from crypta.lib.python.smtp.test_helpers.fs_smtp_server import FsSmtpServer
from crypta.lib.python.smtp.test_helpers.local_smtp_server import LocalSmtpServer


@pytest.fixture
def local_smtp_server():
    pm = network.PortManager()
    yield LocalSmtpServer(FsSmtpServer(("localhost", pm.get_port()), None, "mail_{count}.txt", formatter=mail_formatters.replace_boundaries))
    pm.release()
