from .common import *
from fan.send.test_send import complete
from fan_ui.api.query_params import pass_test_send_task_object


class TestSendTaskEndpoint(Endpoint):
    permission_classes = ()

    @method_decorator(pass_test_send_task_object)
    def delete(self, request, task):
        complete(task)
        return Response(None, status=status.HTTP_200_OK)
