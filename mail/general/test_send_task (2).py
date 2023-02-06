import rest_framework.serializers as rf_serializers
from fan.models import TestSendTask


class TestSendTaskSerializerV1(rf_serializers.ModelSerializer):
    account_slug = rf_serializers.CharField(source="campaign.account.name")
    campaign_slug = rf_serializers.CharField(source="campaign.slug")
    from_email = rf_serializers.CharField(source="campaign.default_letter.from_email")
    recipients = rf_serializers.SerializerMethodField()
    user_template_variables = rf_serializers.JSONField()

    class Meta:
        model = TestSendTask
        fields = (
            "id",
            "created_at",
            "recipients",
            "user_template_variables",
            "account_slug",
            "campaign_slug",
            "from_email",
        )

    # XXX serialize recipients by hands to prevent incorrect escaping
    def get_recipients(self, task):
        return task.recipients
