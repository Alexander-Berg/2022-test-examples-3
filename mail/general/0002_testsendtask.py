from django.db import migrations, models
import fan.db.fields.separatedvaluesfield


class Migration(migrations.Migration):

    dependencies = [
        ("fan", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="TestSendTask",
            fields=[
                (
                    "id",
                    models.AutoField(
                        verbose_name="ID", serialize=False, auto_created=True, primary_key=True
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("modified_at", models.DateTimeField(auto_now=True)),
                (
                    "recipients",
                    fan.db.fields.separatedvaluesfield.SeparatedValuesField(max_length=1024),
                ),
                ("campaign", models.ForeignKey(related_name="test_send_tasks", to="fan.Campaign")),
            ],
            options={
                "verbose_name_plural": "\u0417\u0430\u0434\u0430\u0447\u0430 \u0442\u0435\u0441\u0442\u043e\u0432\u043e\u0439 \u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0438",
            },
        ),
    ]
