# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, timedelta

import pytest
from dateutil.relativedelta import relativedelta
from django.conf import settings
from django.core import mail

from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.content_manager import ContentManager
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from tester.factories import create_supplier
from travel.rasp.admin.scripts.notify_bus_suppliers import (need_to_send_email, get_package_end_date,
                                          NUMBER_OF_REMAINING_DAYS_TO_START_SEND_EMAILS, send_notifications)
from tester.utils.replace_setting import replace_setting

today = date(2015, 11, 1)
month_ago = today - relativedelta(months=1)


@pytest.mark.dbuser
@pytest.mark.parametrize("last_import_date, last_mask_date, result", [
    (None, None, False),
    (month_ago, None, False),
    (month_ago, today, True),
    (month_ago, today + timedelta(days=NUMBER_OF_REMAINING_DAYS_TO_START_SEND_EMAILS + 1), False),
    (month_ago, today + timedelta(days=NUMBER_OF_REMAINING_DAYS_TO_START_SEND_EMAILS), False),
    (month_ago, today + timedelta(days=NUMBER_OF_REMAINING_DAYS_TO_START_SEND_EMAILS - 1), True),
])
def test_need_to_send_email(last_import_date, last_mask_date, result):
    package = create_tsi_package(
        title="test_need_to_send_email",
        supplier=create_supplier(),
        last_import_date=last_import_date,
        last_mask_date=last_mask_date,
        notify_supplier=True,
        supplier_email="aaa@example.com",
    )
    assert result == need_to_send_email(package, today, get_package_end_date(package))


@pytest.mark.dbuser
def test_need_to_send_email_notify_supplier_false():
    last_import_date, last_mask_date = month_ago, today
    package = create_tsi_package(
        title="test_need_to_send_email",
        supplier=create_supplier(),
        last_import_date=last_import_date,
        last_mask_date=last_mask_date,
        notify_supplier=False,
        supplier_email="aaa@example.com",
    )
    assert not need_to_send_email(package, today, get_package_end_date(package))


@pytest.mark.dbuser
def test_need_to_send_email_no_email():
    last_import_date, last_mask_date = month_ago, today
    package = create_tsi_package(
        title="test_need_to_send_email",
        supplier=create_supplier(),
        last_import_date=last_import_date,
        last_mask_date=last_mask_date,
        notify_supplier=True,
        supplier_email=None,
    )
    assert not need_to_send_email(package, today, get_package_end_date(package))


@pytest.mark.dbuser
@replace_setting("SEND_MAIL_TO_PARTNERS", True)
@pytest.mark.parametrize("package_type", [pt[0] for pt in TwoStageImportPackage.PACKAGE_TYPE_CHOICES])
def test_send_notifications_to_partner_only(package_type):
    # Перед отправкой сообщений проверим, что реальной отправки не будут
    assert settings.EMAIL_BACKEND == "django.core.mail.backends.locmem.EmailBackend"

    supplier_email = "supplier@example.com"

    package = create_tsi_package(
        title="test_need_to_send_email",
        supplier=create_supplier(title="supplier title"),
        package_type=package_type,
        last_import_date=month_ago,
        last_mask_date=today,
        notify_supplier=True,
        supplier_email=supplier_email,
        notify_content_manager=False,
    )

    send_notifications()

    assert len(mail.outbox) == 1
    email = mail.outbox[0]
    assert email.recipients() == [supplier_email]
    assert email.from_email == "Яндекс.Расписания <rasp-info@yandex-team.ru>"
    assert "" in email.body
    assert "Если Вы хотите продолжить сотрудничество с нашим сервисом, то Вам необходимо прислать на адрес rasp-info@yandex-team.ru" in email.alternatives[0][0]
    assert "text/html" in email.alternatives[0][1]
    assert package.title not in email.subject
    assert package.supplier.title not in email.subject


@pytest.mark.dbuser
@replace_setting("SEND_MAIL_TO_PARTNERS", True)
@pytest.mark.parametrize("package_type", [pt[0] for pt in TwoStageImportPackage.PACKAGE_TYPE_CHOICES])
def test_send_notifications_to_partner_and_content_manager(package_type):
    # Перед отправкой сообщений проверим, что реальной отправки не будут
    assert settings.EMAIL_BACKEND == "django.core.mail.backends.locmem.EmailBackend"

    supplier_email = "supplier@example.com"
    content_manager_email = "content_manager@example.com"

    content_manager = ContentManager.objects.create(name="Контент Менеджер", email=content_manager_email)

    package = create_tsi_package(
        title="test_need_to_send_email",
        supplier=create_supplier(title="supplier title"),
        package_type=package_type,
        last_import_date=month_ago,
        last_mask_date=today,
        notify_supplier=True,
        supplier_email=supplier_email,
        notify_content_manager=True,
        content_manager=content_manager,
    )

    send_notifications()

    assert len(mail.outbox) == 2
    email_to_supplier = mail.outbox[0]
    email_to_cm = mail.outbox[1]

    if email_to_supplier.recipients() != [supplier_email]:
        email_to_supplier, email_to_cm = email_to_cm, email_to_supplier

    assert email_to_supplier.recipients() == [supplier_email]
    assert email_to_supplier.from_email == "Яндекс.Расписания <rasp-info@yandex-team.ru>"
    assert "" in email_to_supplier.body
    assert "Если Вы хотите продолжить сотрудничество с нашим сервисом, то Вам необходимо прислать на адрес rasp-info@yandex-team.ru" in email_to_supplier.alternatives[0][0]
    assert package.title not in email_to_supplier.subject
    assert package.supplier.title not in email_to_supplier.subject

    assert email_to_cm.recipients() == [content_manager_email]
    assert email_to_cm.from_email == "Яндекс.Расписания <rasp-info@yandex-team.ru>"
    assert "Напоминание об истечении срока размещения на сервисе отправлено поставщику." in email_to_cm.body
    assert package.title in email_to_cm.subject
    assert package.supplier.title in email_to_cm.subject
