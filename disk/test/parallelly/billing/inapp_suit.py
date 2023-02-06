# -*- coding: utf-8 -*-
"""
Тест кейсы инапов

Эти тест кейсы очень жестко завязаны на константы из test/fixtures/json/trust_verify_response.json, поэтому менять
константы или fixture надо с особой осторожностью
"""

import json
import copy
from datetime import datetime

import mock

from mpfs.common.errors.billing import BigBillingBadResult
from mpfs.common.static.codes import BILLING_IN_APP_INVALID_VERIFICATION
from mpfs.common.static.tags.billing import APPLE_APPSTORE, STATE, INAPP_GRACE_PERIOD, INAPP_ACTIVE, GOOGLE_PLAY
from mpfs.common.util import from_json
from mpfs.core.billing import ServiceList, look_for_order, ArchiveOrder, Product, Client, ServiceListHistory
from mpfs.core.billing.constants import PRODUCT_INITIAL_10GB_ID
from mpfs.core.billing.inapp.app_store_parser import app_store_response_parser
from mpfs.core.billing.inapp.dao.unprocessed_receipt import UnprocessedReceiptDAO
from mpfs.core.billing.inapp.sync_logic import process_trust_sub, TrustInAppSubscription
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.billing.processing.common import provide_paid_service_to_client
from mpfs.core.metastorage.control import billing_orders_history
from test.base import time_machine
from test.parallelly.billing.base import BillingTestCaseMixin, BaseBillingTestCase


EMPTY_RECEIPT_JSON = """
{
  "status": "0",
  "environment": "Production",
  "receipt": {
    "download_id": null,
    "adam_id": "553266487",
    "request_date": "2019-09-18 11:52:28 Etc/GMT",
    "app_item_id": "553266487",
    "original_purchase_date_pst": "2017-04-03 19:17:36 America/Los_Angeles",
    "version_external_identifier": "832775961",
    "receipt_creation_date": "2019-09-18 11:47:43 Etc/GMT",
    "in_app": [],
    "original_purchase_date_ms": "1491272256000",
    "original_application_version": "7792",
    "original_purchase_date": "2017-04-04 02:17:36 Etc/GMT",
    "request_date_ms": "1568807548305",
    "bundle_id": "ru.yandex.disk",
    "receipt_creation_date_pst": "2019-09-18 04:47:43 America/Los_Angeles",
    "application_version": "18303",
    "request_date_pst": "2019-09-18 04:52:28 America/Los_Angeles",
    "receipt_creation_date_ms": "1568807263000",
    "receipt_type": "Production"
  }
}
"""
RAW_RECEIPT_WITH_CANCELLATION_DATE = """
{
  "status": 0,
  "receipt": {
    "download_id": 64006492451917,
    "adam_id": 553266487,
    "request_date": "2020-07-21 12:00:50 Etc/GMT",
    "app_item_id": 553266487,
    "original_purchase_date_pst": "2014-01-23 22:47:47 America/Los_Angeles",
    "version_external_identifier": 835656663,
    "receipt_creation_date": "2020-05-09 20:55:45 Etc/GMT",
    "in_app": [
      {
        "is_trial_period": "false",
        "purchase_date_pst": "2020-05-09 13:55:43 America/Los_Angeles",
        "is_in_intro_offer_period": "false",
        "expires_date": "2020-06-09 20:55:43 Etc/GMT",
        "product_id": "1tb_1m_apple_appstore_2019",
        "original_transaction_id": "240000749927149",
        "original_purchase_date_pst": "2020-05-09 13:55:45 America/Los_Angeles",
        "expires_date_pst": "2020-06-09 13:55:43 America/Los_Angeles",
        "expires_date_ms": "1591736143000",
        "original_purchase_date": "2020-05-09 20:55:45 Etc/GMT",
        "original_purchase_date_ms": "1589057745000",
        "purchase_date": "2020-05-09 20:55:43 Etc/GMT",
        "web_order_line_item_id": "240000285688381",
        "purchase_date_ms": "1589057743000",
        "transaction_id": "240000749927149",
        "quantity": "1"
      }
    ],
    "original_purchase_date_ms": "1390546067000",
    "original_application_version": "693",
    "original_purchase_date": "2014-01-24 06:47:47 Etc/GMT",
    "request_date_ms": "1595332850170",
    "bundle_id": "ru.yandex.disk",
    "receipt_creation_date_pst": "2020-05-09 13:55:45 America/Los_Angeles",
    "application_version": "19590",
    "request_date_pst": "2020-07-21 05:00:50 America/Los_Angeles",
    "receipt_creation_date_ms": "1589057745000",
    "receipt_type": "Production"
  },
  "latest_receipt_info": [
    {
      "is_trial_period": "false",
      "purchase_date_pst": "2020-06-09 10:28:31 America/Los_Angeles",
      "is_in_intro_offer_period": "false",
      "expires_date": "2021-06-09 17:28:31 Etc/GMT",
      "product_id": "1tb_1y_apple_appstore_2019",
      "original_transaction_id": "240000749927149",
      "original_purchase_date_pst": "2020-05-09 13:55:45 America/Los_Angeles",
      "expires_date_pst": "2021-06-09 10:28:31 America/Los_Angeles",
      "expires_date_ms": "1623259711000",
      "original_purchase_date": "2020-05-09 20:55:45 Etc/GMT",
      "original_purchase_date_ms": "1589057745000",
      "purchase_date": "2020-06-09 17:28:31 Etc/GMT",
      "subscription_group_identifier": "20542981",
      "web_order_line_item_id": "240000294425564",
      "purchase_date_ms": "1591723711000",
      "transaction_id": "240000764573178",
      "quantity": "1"
    },
    {
      "purchase_date_pst": "2020-06-09 13:55:43 America/Los_Angeles",
      "expires_date_pst": "2020-07-09 13:55:43 America/Los_Angeles",
      "cancellation_date_ms": "1591752596000",
      "web_order_line_item_id": "240000285688382",
      "cancellation_date_pst": "2020-06-09 18:29:56 America/Los_Angeles",
      "subscription_group_identifier": "20542981",
      "purchase_date": "2020-06-09 20:55:43 Etc/GMT",
      "purchase_date_ms": "1591736143000",
      "transaction_id": "240000764470830",
      "is_trial_period": "false",
      "original_purchase_date_pst": "2020-05-09 13:55:45 America/Los_Angeles",
      "expires_date": "2020-07-09 20:55:43 Etc/GMT",
      "product_id": "1tb_1m_apple_appstore_2019",
      "original_transaction_id": "240000749927149",
      "expires_date_ms": "1594328143000",
      "original_purchase_date": "2020-05-09 20:55:45 Etc/GMT",
      "cancellation_date": "2020-06-10 01:29:56 Etc/GMT",
      "original_purchase_date_ms": "1589057745000",
      "is_upgraded": "true",
      "cancellation_reason": "0",
      "is_in_intro_offer_period": "false",
      "quantity": "1"
    },
    {
      "is_trial_period": "false",
      "purchase_date_pst": "2020-05-09 13:55:43 America/Los_Angeles",
      "is_in_intro_offer_period": "false",
      "expires_date": "2020-06-09 20:55:43 Etc/GMT",
      "product_id": "1tb_1m_apple_appstore_2019",
      "original_transaction_id": "240000749927149",
      "original_purchase_date_pst": "2020-05-09 13:55:45 America/Los_Angeles",
      "expires_date_pst": "2020-06-09 13:55:43 America/Los_Angeles",
      "expires_date_ms": "1591736143000",
      "original_purchase_date": "2020-05-09 20:55:45 Etc/GMT",
      "original_purchase_date_ms": "1589057745000",
      "purchase_date": "2020-05-09 20:55:43 Etc/GMT",
      "subscription_group_identifier": "20542981",
      "web_order_line_item_id": "240000285688381",
      "purchase_date_ms": "1589057743000",
      "transaction_id": "240000749927149",
      "quantity": "1"
    }
  ],
  "latest_receipt": "...",
  "pending_renewal_info": [
    {
      "auto_renew_status": "1",
      "auto_renew_product_id": "1tb_1y_apple_appstore_2019",
      "product_id": "1tb_1y_apple_appstore_2019",
      "original_transaction_id": "240000749927149"
    }
  ],
  "environment": "Production"
}
"""

class InappTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    """
    Тест кейсы инапов

    Эти тест кейсы очень жестко завязаны на константы из test/fixtures/json/trust_verify_response.json, поэтому менять
    константы или fixture надо с особой осторожностью
    """

    verify_response = None

    def setup_method(self, method):
        super(InappTestCase, self).setup_method(method)
        with open("fixtures/json/trust_verify_response.json") as fix_file:
            self.verify_response = {
                "result": {"receipt_info": from_json(fix_file.read()), "receipt_check_status": "valid"}
            }
        self.receipt = self.verify_response["result"]["receipt_info"]["latest_receipt"]

    def test_parse_receipt_with_cancellation_dt(self):
        result = app_store_response_parser(json.loads(RAW_RECEIPT_WITH_CANCELLATION_DATE))
        assert len(result) == 1
        subscription = result[0]
        assert len(subscription.purchase_receipts) == 2
        assert len(subscription.cancellation_receipts) == 1
        latest_transaction = subscription.get_latest_purchase_receipt()
        assert latest_transaction.product_id == '1tb_1y_apple_appstore_2019'

    def test_parse_empty_receipt(self):
        result = app_store_response_parser(json.loads(EMPTY_RECEIPT_JSON))
        assert result == []

    def test_verification_has_to_create_serivces(self):
        client = Client(self.uid)
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime(year=2019, month=7, day=15)):
            body = {"receipt": self.receipt}
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
                "receipt": self.receipt,
            }
            self.billing_ok("process_receipt", qs, json=body)
        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", "1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])

        service = ServiceList(client=client, product="1tb_1m_apple_appstore_2019")[0]
        order = look_for_order(service["order"])
        assert isinstance(order, ArchiveOrder)
        assert order.payment_method == APPLE_APPSTORE
        assert order.receipt == self.receipt
        assert service["btime"] == 2099312099

        service = ServiceList(client=client, product="1tb_1y_apple_appstore_2019")[0]
        order = look_for_order(service["order"])
        assert isinstance(order, ArchiveOrder)
        assert order.payment_method == APPLE_APPSTORE
        assert order.receipt == self.receipt
        assert service["btime"] == 2130837299

    def test_verification_has_to_change_pid(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1m_apple_appstore_2019")
        otid = "1000000543251951"
        provide_paid_service_to_client(
            client,
            product,
            product.store_id,
            auto=True,
            bb_time=2130837299,
            receipt="123",
            original_transaction_id=otid,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime(year=2019, month=7, day=15)):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_ok("process_receipt", qs, json=body)

        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", "1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])
        archived_services = ServiceListHistory(client=Client(self.uid), product=product.pid)
        assert len(archived_services) == 1
        assert str(archived_services[0]["original_transaction_id"]) == otid
        assert len(list(billing_orders_history.find({"uid": self.uid}))) == 3

    def test_uid_mismatch(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1y_apple_appstore_2019")
        provide_paid_service_to_client(
            client,
            product,
            product.store_id,
            auto=True,
            bb_time=123,
            receipt="123",
            original_transaction_id="1000000543251951",
        )

        self.run_000_user_check(self.user_3.uid)

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime(year=2019, month=7, day=15)):
            qs = {
                "uid": self.user_3.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            # bug in billing_ok - it's not ok
            resp = self.billing_ok("process_receipt", qs, json=body)
            assert resp["code"] == 272
            assert resp["response"] == 400

    def test_verification_has_to_change_btime(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1y_apple_appstore_2019")
        provide_paid_service_to_client(
            client,
            product,
            product.store_id,
            auto=True,
            bb_time=123,
            receipt="123",
            original_transaction_id="1000000543251951",
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime(year=2019, month=7, day=15)):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_ok("process_receipt", qs, json=body)

        service = ServiceList(client=Client(self.uid), product="1tb_1y_apple_appstore_2019")[0]
        assert service["btime"] == 2130837299
        assert len(list(billing_orders_history.find({"uid": self.uid}))) == 2

    def test_verification_has_to_change_btime_and_pid(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1m_apple_appstore_2019")
        otid = "1000000543251951"
        incorrect_btime = 2130837298
        correct_btime = 2130837299
        provide_paid_service_to_client(
            client,
            product,
            product.store_id,
            auto=True,
            bb_time=incorrect_btime,
            receipt="123",
            original_transaction_id=otid,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_ok("process_receipt", qs, json=body)

        service = ServiceList(client=Client(self.uid), product="1tb_1y_apple_appstore_2019")[0]
        assert service["btime"] == correct_btime

    def test_moving_service_to_grace_period(self):
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}

            with time_machine(datetime(year=2019, month=7, day=15)):
                self.billing_ok("process_receipt", qs, json=body)
            self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", "1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])

            with time_machine(datetime(year=2036, month=7, day=17, hour=10, minute=41)):
                self.billing_ok("process_receipt", qs, json=body)

            service = ServiceList(client=Client(self.uid), product="1tb_1m_apple_appstore_2019")[0]
            assert service[STATE] == INAPP_GRACE_PERIOD

    def test_bad_google_play_receipt(self):
        error = BigBillingBadResult({
            'status': 'error',
            'status_code': 'unknown_error',
            'method': 'yandex_balance_simple.put_inapp_receipt',
            'status_desc': 'RuntimeError: Error due to google api call 400 {"error":{"code":400,"message":"Invalid Value","errors":[{"message":"Invalid Value","domain":"global","reason":"invalid"}]}}'
        })
        with mock.patch("mpfs.core.services.trust_payments.TrustPaymentsService.request", side_effect=error):
            qs = {
                "uid": self.uid,
                "store_id": GOOGLE_PLAY,
                "package_name": "123",
                "store_product_id": "1tb_1y_google_play_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_error("process_receipt", qs, json=body, code=BILLING_IN_APP_INVALID_VERIFICATION)

    def test_deleting_expired_service(self):
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}

            with time_machine(datetime(year=2019, month=7, day=15)):
                self.billing_ok("process_receipt", qs, json=body)
            self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", "1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])

            with time_machine(datetime(year=2037, month=7, day=17, hour=15, minute=41)):
                self.billing_ok("process_receipt", qs, json=body)
            self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])

    def test_wont_create_new_service_if_its_already_expired_or_in_grace_period(self):
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime(year=2037, month=7, day=15, hour=10, minute=41)):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_ok("process_receipt", qs, json=body)
            self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])

    def test_inapp_services_are_shown_in_service_list_enpoint_response(self):
        client = Client(self.uid)
        pid = "1tb_1y_apple_appstore_2019"
        product = Product(pid=pid)
        now_ts = 1563441669
        btime = 1563442000
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):

            provide_paid_service_to_client(
                client,
                product,
                APPLE_APPSTORE,
                auto=True,
                bb_time=btime,
                receipt="123",
                original_transaction_id="1224444",
            )
        services = self.billing_ok("service_list", {"uid": self.uid, "ip": "127.0.0.1"})
        assert {"1tb_1y_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID} == {x["name"] for x in services}
        for i in xrange(len(services)):
            if services[i]["name"] == PRODUCT_INITIAL_10GB_ID:
                services.pop(i)
                break
        assert all(x["payment_method"] == APPLE_APPSTORE
                   for x in services)

    def test_inapp_products(self):
        res = self.billing_ok("inapp_products", {"uid": self.uid, "store_id": APPLE_APPSTORE})
        assert {"100gb_1m_apple_appstore_2019", "100gb_1y_apple_appstore_2019"} == {x["product_id"] for x in res["items"]}
        assert all(
            [
                {"product_id", "display_space", "display_space_units", "space", "is_yandex_plus", "is_best_offer"}
                == set(x.keys())
                for x in res["items"]
            ]
        )

    def test_error_occurred_on_service_providing(self):
        now_ts = 1563441669

        from mpfs.core.billing.inapp.mvp1_sync_logic import process_app_store_sub

        def fake_process_app_store_sub(client, app_store_sub, receipt, store_id):
            if app_store_sub.get_latest_purchase_receipt().product_id == "1tb_1y_apple_appstore_2019":
                raise Exception("Exception from mock")
            process_app_store_sub(client, app_store_sub, receipt, store_id)

        with mock.patch(
            "mpfs.core.billing.inapp.mvp1_sync_logic.process_app_store_sub", fake_process_app_store_sub
        ), mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(
            datetime.fromtimestamp(now_ts)
        ):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1m_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_ok("process_receipt", qs, json=body)

        self.assertUserHasExactServices(self.uid, ["1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])

        result = list(UnprocessedReceiptDAO().find({"uid": self.uid}))
        assert len(result) == 1
        assert result[0]["syncronization_datetime"] == now_ts
        assert result[0]["uid"] == self.uid

    def test_invalid_receipts_are_not_processed(self):
        invalid_verify_response = {"result": {"receipt_info": {}, "receipt_check_status": "invalid"}}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=invalid_verify_response
        ):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "100gb_1m_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_error("process_receipt", qs, json=body, code=BILLING_IN_APP_INVALID_VERIFICATION)

        self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])


class InappBtimeRoutineTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    verify_response = None

    def setup_method(self, method):
        super(InappBtimeRoutineTestCase, self).setup_method(method)
        with open("fixtures/json/trust_verify_response_single_app_sub.json") as fix_file:
            self.verify_response = {
                "result": {"receipt_info": from_json(fix_file.read()), "receipt_check_status": "valid"}
            }
        self.receipt = self.verify_response["result"]["receipt_info"]["latest_receipt"]

        self.client = Client(self.uid)
        self.pid = "1tb_1y_apple_appstore_2019"
        self.product = Product(pid=self.pid)
        self.otid = "1000000543251951"

    def test_now_btime_expires_changes_btime(self):
        now_ts = 1561993680  # 1 July 2019 г., 18:08:00
        btime_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] != INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts

    def test_btime_now_expires_changes_btime(self):
        btime_ts = 1561993680  # 1 July 2019 г., 18:08:00
        now_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] != INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts

    def test_service_expires_and_app_store_stop_trying_to_renew(self):
        # service shoud be deleted
        btime_ts = 1562080080  # 2 July 2019 г., 18:08:00
        now_ts = 1562252880  # 4 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}
        verify_response = copy.deepcopy(self.verify_response)
        for info in verify_response["result"]["receipt_info"]["pending_renewal_info"]:
            info["is_in_billing_retry_period"] = "0"
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 0
        assert len(list(billing_orders_history.find({"uid": self.uid}))) == 1

    def test_btime_expires_now_puts_to_grace_and_change_btime(self):
        btime_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        now_ts = 1562252880  # 4 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] == INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts

    def test_now_expires_btime_does_nothing(self):
        now_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        btime_ts = 1562252880  # 4 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] != INAPP_GRACE_PERIOD
        assert services[0]["btime"] == btime_ts

    def test_expires_now_btime_does_nothing(self):
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        now_ts = 1562252880  # 4 July 2019 г., 18:08:00
        btime_ts = 1562339280  # 5 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] != INAPP_GRACE_PERIOD
        assert services[0]["btime"] == btime_ts

    def test_expires_btime_now_puts_to_grace(self):
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        btime_ts = 1562252880  # 4 July 2019 г., 18:08:00
        now_ts = 1562339280  # 5 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] == INAPP_GRACE_PERIOD
        assert services[0]["btime"] == btime_ts

    def test_btime_expires_now_puts_to_grace_and_change_btime_for_different_pids(self):
        btime_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        now_ts = 1562252880  # 4 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] == INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts

    def test_now_btime_expires_changes_btime_for_different_pids(self):
        now_ts = 1561993680  # 1 July 2019 г., 18:08:00
        btime_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=btime_ts,
            receipt="123",
            original_transaction_id=self.otid,
        )

        qs = {"uid": self.uid, "store_id": APPLE_APPSTORE, "package_name": "123", "store_product_id": self.pid}
        body = {"receipt": self.receipt}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), time_machine(datetime.fromtimestamp(now_ts)):
            self.billing_ok("process_receipt", qs, json=body)

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] != INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts


class InappCronTasksTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    def setup_method(self, method):
        super(InappCronTasksTestCase, self).setup_method(method)
        with open("fixtures/json/trust_verify_response_single_app_sub.json") as fix_file:
            self.verify_response_1_serivce = {
                "result": {"receipt_info": from_json(fix_file.read()), "receipt_check_status": "valid"}
            }
        self.receipt_1_serivce = self.verify_response_1_serivce["result"]["receipt_info"]["latest_receipt"]

        with open("fixtures/json/trust_verify_response.json") as fix_file:
            self.verify_response_2_serivces = {
                "result": {"receipt_info": from_json(fix_file.read()), "receipt_check_status": "valid"}
            }
        self.receipt_2_serivces = self.verify_response_2_serivces["result"]["receipt_info"]["latest_receipt"]

        self.client = Client(self.uid)
        self.pid = "1tb_1y_apple_appstore_2019"
        self.product = Product(pid=self.pid)
        self.otid = "1000000543251951"

    def test_inapp_service_put_to_grace(self):
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        now_ts = 1562252880  # 4 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=expires_ts,
            receipt="123",
            original_transaction_id=self.otid,
            state=INAPP_ACTIVE,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request",
            return_value=self.verify_response_1_serivce,
        ), time_machine(datetime.fromtimestamp(now_ts)):
            push_billing_commands()

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] == INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts

    def test_inapp_service_removed(self):
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        now_ts = 1563127680  # 14 July 2019 г., 18:08:00

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=expires_ts,
            receipt="123",
            original_transaction_id=self.otid,
            state=INAPP_ACTIVE,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request",
            return_value=self.verify_response_1_serivce,
        ), time_machine(datetime.fromtimestamp(now_ts)):
            push_billing_commands()

        self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])

    def test_inapp_service_pid_changed(self):
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57
        now_ts = 1562166538  # 3 July 2019 г., 18:08:58

        old_product = Product("1tb_1m_apple_appstore_2019")
        provide_paid_service_to_client(
            self.client,
            old_product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=expires_ts,
            receipt="123",
            original_transaction_id=self.otid,
            state=INAPP_ACTIVE,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request",
            return_value=self.verify_response_1_serivce,
        ), time_machine(datetime.fromtimestamp(now_ts)):
            push_billing_commands()

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] == INAPP_GRACE_PERIOD
        assert services[0]["btime"] == expires_ts
        assert services[0]["pid"] == self.pid

    def test_inapp_service_btime_change(self):
        now_ts = 1562080080  # 2 July 2019 г., 18:08:00
        expires_ts = 1562166537  # 3 July 2019 г., 18:08:57

        provide_paid_service_to_client(
            self.client,
            self.product,
            APPLE_APPSTORE,
            auto=True,
            bb_time=now_ts - 1,
            receipt="123",
            original_transaction_id=self.otid,
            state=INAPP_ACTIVE,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request",
            return_value=self.verify_response_1_serivce,
        ), time_machine(datetime.fromtimestamp(now_ts)):
            push_billing_commands()

        services = ServiceList(client=Client(self.uid), product=self.pid)
        assert len(services) == 1
        assert services[0][STATE] == INAPP_ACTIVE
        assert services[0]["btime"] == expires_ts

    def test_cron_task_creates_new_serivces(self):
        first_service_btime = 2099312099
        first_service_otid = "1000000543677750"
        provide_paid_service_to_client(
            self.client,
            Product("1tb_1m_apple_appstore_2019"),
            APPLE_APPSTORE,
            auto=True,
            bb_time=first_service_btime,
            receipt="123",
            original_transaction_id=first_service_otid,
            state=INAPP_ACTIVE,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request",
            return_value=self.verify_response_2_serivces,
        ), time_machine(datetime.fromtimestamp(first_service_btime + 1)):
            push_billing_commands()

        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", "1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])


DEFAULT_APPLE_APPSTORE_CARD = """
{
  "payment_method": "APPLE_APPSTORE",
  "items": [
    {
      "is_best_offer": false,
      "periods": {
        "month": {
          "product_id": "100gb_1m_apple_appstore_2019"
        },
        "year": {
          "product_id": "100gb_1y_apple_appstore_2019"
        }
      },
      "id": "100gb_apple_appstore_2019"
    },
    {
      "is_best_offer": true,
      "periods": {
        "month": {
          "product_id": "1tb_1m_apple_appstore_2019"
        },
        "year": {
          "product_id": "1tb_1y_apple_appstore_2019"
        }
      },
      "id": "1tb_apple_appstore_2019"
    },
    {
      "is_best_offer": false,
      "periods": {
        "month": {
          "product_id": "3tb_1m_apple_appstore_2019"
        },
        "year": {
          "product_id": "3tb_1y_apple_appstore_2019"
        }
      },
      "id": "3tb_apple_appstore_2019"
    }
  ]
}
"""
DEFAULT_GOOGLE_PLAY_CARD = """
{
  "payment_method": "GOOGLE_PLAY",
  "items": [
    {
      "is_best_offer": false,
      "periods": {
        "month": {
          "product_id": "100gb_1m_google_play_2019"
        },
        "year": {
          "product_id": "100gb_1y_google_play_2019"
        }
      },
      "id": "100gb_google_play_2019"
    },
    {
      "is_best_offer": true,
      "periods": {
        "month": {
          "product_id": "1tb_1m_google_play_2019"
        },
        "year": {
          "product_id": "1tb_1y_google_play_2019"
        }
      },
      "id": "1tb_google_play_2019"
    },
    {
      "is_best_offer": false,
      "periods": {
        "month": {
          "product_id": "3tb_1m_google_play_2019"
        },
        "year": {
          "product_id": "3tb_1y_google_play_2019"
        }
      },
      "id": "3tb_google_play_2019"
    }
  ]
}
"""


class InAppProductCardTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    def provide_inapp_product(self, pid, store_id):
        raw_trust_subs = {
            "uid": self.uid,
            "sync_dt": "2019-08-01T12:19:17+03:00",
            "store_expiration_dt": "2037-07-30T12:19:17+03:00",
            "state": "ACTIVE",
            "product_id": pid,
            "subs_until_dt": "2027-12-06T12:19:17+03:00",
            "store_subscription_id": "1",
            "id": 1,
            "store_id": store_id,
        }
        process_trust_sub(TrustInAppSubscription(raw_trust_subs))
        self.assertUserHasExactServices(self.uid, [pid, PRODUCT_INITIAL_10GB_ID])

    def test_default_card_appstore(self):
        result = self.billing_ok("inapp_product_card", {"uid": self.uid, "store_id": APPLE_APPSTORE})
        assert result == json.loads(DEFAULT_APPLE_APPSTORE_CARD)

    def test_default_card_google_play(self):
        result = self.billing_ok("inapp_product_card", {"uid": self.uid, "store_id": GOOGLE_PLAY})
        assert result == json.loads(DEFAULT_GOOGLE_PLAY_CARD)

    def test_current_product_year(self):
        self.provide_inapp_product("1tb_1y_apple_appstore_2019", APPLE_APPSTORE)
        result = self.billing_ok("inapp_product_card", {"uid": self.uid, "store_id": APPLE_APPSTORE})
        assert result["current_product"]["product_id"] == "1tb_1y_apple_appstore_2019"
        assert len(result["items"]) == 1
        assert result["items"][0]["periods"]["month"]["product_id"] == "3tb_1m_apple_appstore_2019"
        assert result["items"][0]["periods"]["year"]["product_id"] == "3tb_1y_apple_appstore_2019"

    def test_current_product_month(self):
        self.provide_inapp_product("1tb_1m_apple_appstore_2019", APPLE_APPSTORE)
        result = self.billing_ok("inapp_product_card", {"uid": self.uid, "store_id": APPLE_APPSTORE})
        assert result["current_product"]["product_id"] == "1tb_1m_apple_appstore_2019"
        assert len(result["items"]) == 2
        assert result["items"][0]["periods"]["month"]["product_id"] == "1tb_1m_apple_appstore_2019"
        assert result["items"][0]["periods"]["year"]["product_id"] == "1tb_1y_apple_appstore_2019"
        assert result["items"][1]["periods"]["month"]["product_id"] == "3tb_1m_apple_appstore_2019"
        assert result["items"][1]["periods"]["year"]["product_id"] == "3tb_1y_apple_appstore_2019"

    def test_current_product_most_expensive(self):
        self.provide_inapp_product("3tb_1y_apple_appstore_2019", APPLE_APPSTORE)
        result = self.billing_ok("inapp_product_card", {"uid": self.uid, "store_id": APPLE_APPSTORE})
        assert result["current_product"]["product_id"] == "3tb_1y_apple_appstore_2019"
        assert len(result["items"]) == 0

    def test_ios_experiment(self):
        uaas_value = [
            {
                "HANDLER": "DISK",
                "CONTEXT": {
                    "DISK": {"testid": ["186456"]}
                }
            },
            {
                "HANDLER": "DISK",
                "CONTEXT": {
                    "DISK": {
                        "testid": ["155462"],
                        "data": ["100gb_apple_appstore_2019", "1tb_apple_appstore_2019", "3tb_apple_appstore_2019"],
                        "flags": ["disk_tariff_ios_all"]
                    }
                }
            },
        ]
        with mock.patch('mpfs.core.services.uaas_service.NewUAAS.get_disk_experiments') as stub:
            stub.return_value = uaas_value
            result = self.billing_ok("inapp_product_card", {"uid": self.uid, "store_id": APPLE_APPSTORE})
            assert len(result["items"]) == 3
