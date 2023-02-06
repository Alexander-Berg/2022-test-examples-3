from .common import *
from django.http import HttpResponse
from fan.message.render import render_test_eml
from fan_ui.api.query_params import pass_test_send_task_object, pass_recipient_param


class TestSendTaskEMLEndpoint(Endpoint):
    permission_classes = ()

    @method_decorator(pass_test_send_task_object)
    @method_decorator(pass_recipient_param)
    def get(self, request, task, recipient, **kwargs):
        email = render_test_eml(
            task.campaign, recipient, user_template_variables=task.user_template_variables
        )
        return HttpResponse(email)
