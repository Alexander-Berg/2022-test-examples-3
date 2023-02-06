import logging
from django.core.urlresolvers import reverse

from fan.links.unsubscribe import encode_unsubscribe_code2
from fan_feedback.typed_log import set_typed_log_handler
from tskv_logging.formatters import TSKVFormatter


class MockTypedLoggingHandler(logging.Handler):
    def __init__(self, *args, **kwargs):
        self.messages = []
        self.formatter = TSKVFormatter(tskv_format="test")
        logging.Handler.__init__(self, *args, **kwargs)

    def emit(self, record):
        message = self.formatter.record_to_data(record)
        self.messages.append(message)


def init_fake_typed_log_handler():
    handler = MockTypedLoggingHandler()
    set_typed_log_handler(handler)
    return handler


# TODO move to test_unsubscribe
def get_unsubscribe_link(letter, email):
    code = encode_unsubscribe_code2(
        campaign_id=letter.campaign.id, email=email, letter_id=letter.id
    )
    return reverse("unsubscribe", kwargs={"path": code})
