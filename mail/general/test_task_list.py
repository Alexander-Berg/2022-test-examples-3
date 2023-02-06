from .common import *
from fan.send.test_send import get_tasks
from fan_ui.api.serializers.test_send_task import TestSendTaskSerializerV1
from fan_ui.api.query_params import pass_count_param


class TestSendTaskListEndpoint(Endpoint):
    permission_classes = ()

    @method_decorator(pass_count_param)
    def get(self, request, count):
        tasks = get_tasks(count)
        return make_ok_response(tasks, TestSendTaskSerializerV1)
