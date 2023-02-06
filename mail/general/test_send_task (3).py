from django.db import models
from django.utils.translation import ugettext_lazy as _
from jsonfield import JSONField
from fan.db.fields.separatedvaluesfield import SeparatedValuesField
from fan.models.common import TimestampsMixin


class TestSendTask(TimestampsMixin, models.Model):
    campaign = models.ForeignKey("Campaign", related_name="test_send_tasks")
    recipients = SeparatedValuesField(max_length=1024, null=False, blank=False)
    user_template_variables = JSONField(default=dict)

    class Meta:
        verbose_name_plural = _("Задача тестовой отправки")

    def __str__(self):
        return "Тестовая отправка для " + self.campaign.title
