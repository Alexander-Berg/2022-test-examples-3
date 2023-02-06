from mail.payments.payments.api.handlers.testing import DeleteUserHandler
from mail.payments.payments.api.routes.base import PrefixedUrl


class Url(PrefixedUrl):
    PREFIX = "/testing"


TESTING_ROUTES = (Url(r"/users/{uid:\d+}", DeleteUserHandler, name="testing_delete_user"),)
