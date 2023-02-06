# -*- coding: utf-8 -*-
import time
from datetime import datetime

import mock
from contextlib import contextmanager

from mpfs.common.errors.billing import BillingTrustUIDSyncError
from mpfs.common.static.codes import BILLING_TRUST_INVALID_VERIFICATION
from mpfs.common.static.tags.billing import APPLE_APPSTORE, STATE, ServiceState, GOOGLE_PLAY
from mpfs.common.util import from_json
from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.core.billing import ServiceList, Product, Client, ServiceListHistory
from mpfs.core.billing.constants import PRODUCT_INITIAL_10GB_ID
from mpfs.core.billing.inapp.dao.unprocessed_receipt import UnprocessedReceiptDAO
from mpfs.core.billing.processing.common import provide_paid_service_to_client
from mpfs.core.billing.processing.marketing import get_product_billing_representation
from mpfs.core.billing.inapp.sync_logic import (
    process_trust_sub,
    get_service_by_inapp_subs_id,
    TrustInAppSubscription,
    InAppResyncManager,
)
from mpfs.core.metastorage.control import billing_orders_history
from test.base import time_machine
from test.parallelly.billing.base import BillingTestCaseMixin, BaseBillingTestCase


class InAppRegularTasksTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    def create_in_app_service(self, btime, state):
        client = Client(self.uid)
        product = Product(pid="1tb_1m_apple_appstore_2019")
        provide_paid_service_to_client(
            client,
            product,
            product.store_id,
            auto=True,
            bb_time=btime,
            receipt="123",
            original_transaction_id=1,
            state=state,
        )

    def get_sync_mock(self):
        return mock.patch("mpfs.core.billing.inapp.sync_logic.process_trust_sub")

    def get_trust_mock(self):
        subs = lambda *args, **kwargs: {
            "status": "success",
            "uid": "-",
            "sync_dt": "2019-08-01T12:19:17+03:00",
            "store_expiration_dt": "2037-07-30T12:19:17+03:00",
            "state": "ACTIVE",
            "product_id": "1tb_1m_apple_appstore_2019",
            "subs_until_dt": "2027-12-06T12:19:17+03:00",
            "store_subscription_id": "4277b712e8e84923a4f22208e17240f0",
            "id": 1,
            "store_id": APPLE_APPSTORE,
        }
        return mock.patch("mpfs.core.services.trust_payments.TrustPaymentsService.request", new=subs)

    def past_ts(self):
        return int(time.time()) - 1000

    def future_ts(self):
        return int(time.time()) + 1000

    def create_service_pack(self):
        # normal - don't resync
        self.create_in_app_service(self.future_ts(), ServiceState.active)
        # active expired + 2
        self.create_in_app_service(self.past_ts(), ServiceState.active)
        self.create_in_app_service(self.past_ts(), ServiceState.in_grace)
        # in grace + 1
        self.create_in_app_service(self.future_ts(), ServiceState.in_grace)
        # on hold +3
        self.create_in_app_service(self.past_ts(), ServiceState.on_hold)
        self.create_in_app_service(self.future_ts(), ServiceState.on_hold)
        self.create_in_app_service(self.future_ts(), ServiceState.on_hold)

    def test_regular_resync_jobs(self):
        self.create_service_pack()
        with self.get_sync_mock() as sync_stub, self.get_trust_mock():
            InAppResyncManager.regular_resync_jobs()
            sync_stub.call_count == 6

    def test_resync_all_expired(self):
        self.create_service_pack()
        with self.get_sync_mock() as sync_stub, self.get_trust_mock():
            InAppResyncManager.resync_all_expired()
            sync_stub.call_count == 2

    def test_resync_all_on_hold(self):
        self.create_service_pack()
        with self.get_sync_mock() as sync_stub, self.get_trust_mock():
            InAppResyncManager.resync_all_on_hold()
            sync_stub.call_count == 3

    def test_resync_all_in_grace(self):
        self.create_service_pack()
        with self.get_sync_mock() as sync_stub, self.get_trust_mock():
            InAppResyncManager.resync_all_in_grace()
            sync_stub.call_count == 1


class TrustSubscriptionTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    """
    Тест кейсы инапов

    Эти тест кейсы очень жестко завязаны на константы из test/fixtures/json/trust_verify_response.json, поэтому менять
    константы или fixture надо с особой осторожностью
    """

    verify_response = None

    def setup_method(self, method):
        super(TrustSubscriptionTestCase, self).setup_method(method)
        with open("fixtures/json/trust_subscription_response.json") as subscription_file, open(
            "fixtures/json/trust_verify_response.json"
        ) as verify_file:
            self.verify_response = from_json(subscription_file.read())
            self.verify_response["items"][0]["subscription"]["uid"] = self.uid
            self.verify_response["items"][1]["subscription"]["uid"] = self.uid
            self.receipt = from_json(verify_file.read())["latest_receipt"]

    def test_verification_has_to_create_serivces(self):
        client = Client(self.uid)
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid), time_machine(datetime(year=2019, month=7, day=15)):
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

    def test_verification_has_to_change_pid(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1m_apple_appstore_2019")
        otid = 53
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
        ), enable_experiment_for_uid("inapp_v2", self.uid), time_machine(datetime(year=2019, month=7, day=15)):
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
        assert archived_services[0]["original_transaction_id"] == otid
        assert len(list(billing_orders_history.find({"uid": self.uid}))) == 1

    def test_verification_has_to_change_btime(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1y_apple_appstore_2019")
        provide_paid_service_to_client(
            client,
            product,
            product.store_id,
            auto=True,
            bb_time=123,
            state=ServiceState.in_grace,
            original_transaction_id=53,
        )

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid), time_machine(datetime(year=2019, month=7, day=15)):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_ok("process_receipt", qs, json=body)

        service = ServiceList(client=Client(self.uid), product="1tb_1y_apple_appstore_2019")[0]
        assert service["btime"] == 2112167957
        assert len(list(billing_orders_history.find({"uid": self.uid}))) == 1

    def test_verification_has_to_change_btime_and_pid(self):
        client = Client(self.uid)
        product = Product(pid="1tb_1m_apple_appstore_2019")
        otid = "1000000543251951"
        incorrect_btime = 2130837298
        correct_btime = 2112167957
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
        ), enable_experiment_for_uid("inapp_v2", self.uid):
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
        with enable_experiment_for_uid("inapp_v2", self.uid), mock.patch(
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
            self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", "1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])
            self.verify_response["items"][1]["subscription"]["state"] = "IN_GRACE"

            self.billing_ok("process_receipt", qs, json=body)
            self.verify_response["items"][1]["state"] = "ACTIVE"
            service = ServiceList(client=Client(self.uid), product="1tb_1m_apple_appstore_2019")[0]
            assert service[STATE] == ServiceState.in_grace

    def test_deleting_expired_service(self):
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid):
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
            self.verify_response["items"][0]["subscription"]["state"] = "FINISHED"
            self.verify_response["items"][1]["subscription"]["state"] = "FINISHED"

            self.billing_ok("process_receipt", qs, json=body)
            self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])
            self.verify_response["items"][0]["subscription"]["state"] = "ACTIVE"
            self.verify_response["items"][1]["subscription"]["state"] = "ACTIVE"

    def test_wont_create_new_service_if_its_already_expired_or_in_grace_period(self):
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.verify_response["items"][0]["subscription"]["state"] = "FINISHED"
            self.verify_response["items"][1]["subscription"]["state"] = "FINISHED"
            self.billing_ok("process_receipt", qs, json=body)
            self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])
            self.verify_response["items"][0]["subscription"]["state"] = "ACTIVE"
            self.verify_response["items"][1]["subscription"]["state"] = "ACTIVE"

    def test_inapp_services_are_shown_in_service_list_enpoint_response(self):
        client = Client(self.uid)
        pid = "1tb_1y_apple_appstore_2019"
        product = Product(pid=pid)
        now_ts = 1563441669
        btime = 1563442000
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid), time_machine(datetime.fromtimestamp(now_ts)):

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
        assert all(x["payment_method"] == APPLE_APPSTORE
                   for x in services
                   if x['name'] != PRODUCT_INITIAL_10GB_ID)

    def test_inapp_products(self):
        prod = Product("1tb_1y_apple_appstore_2019")
        with enable_experiment_for_uid("inapp_v2", self.uid):
            res = self.billing_ok("inapp_products", {"uid": self.uid, "store_id": APPLE_APPSTORE})
            assert {"100gb_1y_apple_appstore_2019", "100gb_1m_apple_appstore_2019"} == {
                x["product_id"] for x in res["items"]
            }
            assert all(
                [
                    {"product_id", "display_space", "display_space_units", "space", "is_yandex_plus", "is_best_offer"}
                    == set(x.keys())
                    for x in res["items"]
                ]
            )

    def test_error_occurred_on_service_providing(self):
        now_ts = 1563441669

        from mpfs.core.billing.inapp.sync_logic import process_trust_sub

        def fake_process_trust_sub(trust_subscription):
            if trust_subscription.get_product_id() == "1tb_1y_apple_appstore_2019":
                raise Exception("Exception from mock")
            process_trust_sub(trust_subscription)

        with mock.patch("mpfs.core.billing.inapp.sync_logic.process_trust_sub", fake_process_trust_sub), mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid), time_machine(datetime.fromtimestamp(now_ts)):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1m_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_error("process_receipt", qs, json=body)

        self.assertUserHasExactServices(self.uid, ["1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])

        result = list(UnprocessedReceiptDAO().find({"uid": self.uid}))
        assert len(result) == 1
        assert result[0]["syncronization_datetime"] == now_ts
        assert result[0]["uid"] == self.uid

    def test_invalid_receipts_are_not_processed(self):
        invalid_verify_response = {"status": "error", "items": []}

        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=invalid_verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid):
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "100gb_1m_apple_appstore_2019",
            }
            body = {"receipt": self.receipt}
            self.billing_error("process_receipt", qs, json=body, code=BILLING_TRUST_INVALID_VERIFICATION)

        self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])

    def test_inapp_callback(self):
        body = {
            "store_id": APPLE_APPSTORE,
            "product_id": "1tb_1m_apple_appstore_2019",
            "product_type": "inapp_subs",
            "sync_dt": "2019-08-01T12:19:17+03:00",
            "subscription_id": 1,
        }
        subscription = {
            "status": "success",
            "uid": self.uid,
            "sync_dt": "2019-08-01T12:19:17+03:00",
            "store_expiration_dt": "2037-07-30T12:19:17+03:00",
            "state": "ACTIVE",
            "product_id": "1tb_1m_apple_appstore_2019",
            "subs_until_dt": "2027-12-06T12:19:17+03:00",
            "store_subscription_id": "4277b712e8e84923a4f22208e17240f0",
            "id": 1,
            "store_id": APPLE_APPSTORE,
        }

        with mock.patch("mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=subscription):
            self.billing_ok("inapp_callback", {}, json=body)

        self.assertUserHasExactServices(self.uid, ["1tb_1m_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])

    def test_export_inapp_product_to_trust(self):
        pid = "1tb_1m_apple_appstore_2019"
        product = Product(pid)
        result = get_product_billing_representation(product, 0)
        assert isinstance(result, list)
        assert len(result) == 1
        trust_product = result[0]
        assert trust_product["product_type"] == "inapp_subs"
        assert trust_product["product_id"] == pid
        assert trust_product["name"] == pid
        assert trust_product["package_name"] == "ru.yandex.disk"
        assert "subs_grace_period" in trust_product
        assert "local_names" in trust_product
        assert "prices" in trust_product

    def test_service_list_with_inapp_service(self):
        with mock.patch(
            "mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self.verify_response
        ), enable_experiment_for_uid("inapp_v2", self.uid), time_machine(datetime(year=2019, month=7, day=15)):
            body = {"receipt": self.receipt}
            qs = {
                "uid": self.uid,
                "store_id": APPLE_APPSTORE,
                "package_name": "123",
                "store_product_id": "1tb_1y_apple_appstore_2019",
                "receipt": self.receipt,
            }
            self.billing_ok("process_receipt", qs, json=body)
        result = self.billing_ok("service_list", {"uid": self.uid, "ip": "1"})
        result = [service
                  for service in result
                  if service['name'] != PRODUCT_INITIAL_10GB_ID]
        assert len(result) == 2
        for service in result:
            assert service["payment_method"] == "APPLE_APPSTORE"
            assert service["state"] == "ACTIVE"

    @staticmethod
    def _build_full_resp(trust_subs_dicts):
        return {
            "status": "success",
            "items": [
                {"sync_status": "success", "subscription": s}
                for s in trust_subs_dicts
            ]
        }
    def test_google_play(self):
        trust_subs_dict = {
            "state": "ACTIVE",
            "is_production": True,
            "uid": int(self.uid),
            "subs_until_dt": "2019-10-13T16:02:25+03:00",
            "store_id": "GOOGLE_PLAY",
            "store_subscription_id": "0a16253dbb5247de84124d16bbcf3c7b",
            "sync_dt": "2019-11-12T16:02:26+03:00",
            "store_expiration_dt": "2019-11-13T16:02:25+03:00",
            "id": 18,
            "product_id": "1tb_1m_google_play_2019"
        }
        with mock.patch("mpfs.core.services.trust_payments.TrustPaymentsService.request", return_value=self._build_full_resp([trust_subs_dict])):
            body = {"receipt": "fake_receipt"}
            qs = {
                "uid": self.uid,
                "store_id": GOOGLE_PLAY,
                "package_name": "123",
                "store_product_id": "1tb_1m_google_play_2019",
                "currency": "USD",
            }
            self.billing_ok("process_receipt", qs, json=body)
        result = self.billing_ok("service_list", {"uid": self.uid, "ip": "1"})
        result = [service
                  for service in result
                  if service['name'] != PRODUCT_INITIAL_10GB_ID]
        assert len(result) == 1
        subs = result[0]

        assert subs['payment_method'] == 'GOOGLE_PLAY'
        assert subs['name'] == '1tb_1m_google_play_2019'
        assert subs['state'] == 'ACTIVE'


class ProcessTrustSubTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    one_tb = 1099511627776
    otid = 53

    def create_trust_dict(self):
        return {
            "uid": self.uid,
            "sync_dt": "2019-08-01T12:19:17+03:00",
            "store_expiration_dt": "2037-07-30T12:19:17+03:00",
            "state": "ACTIVE",
            "product_id": "1tb_1y_apple_appstore_2019",
            "subs_until_dt": "2027-12-06T12:19:17+03:00",
            "store_subscription_id": "4277b712e8e84923a4f22208e17240f0",
            "id": self.otid,
            "store_id": "APPLE_APPSTORE",
        }

    def get_limit_counter(self, uid=None):
        if uid is None:
            uid = self.uid
        resp = self.json_ok("user_info", {"uid": self.uid})
        return resp["space"]["limit"]

    @contextmanager
    def assert_counter_delta(self, expected_delta):
        before_limit = self.get_limit_counter()
        try:
            yield
        finally:
            after_limit = self.get_limit_counter()
        assert after_limit - before_limit == expected_delta

    def test_new_active_subscription(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.active)

    def test_new_in_grace_state(self):
        trust_subscription = self.create_trust_dict()
        trust_subscription["state"] = ServiceState.in_grace
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.in_grace)

    def test_new_hold_state(self):
        trust_subscription = self.create_trust_dict()
        trust_subscription["state"] = ServiceState.on_hold
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.on_hold)

    def test_new_finish_state(self):
        trust_subscription = self.create_trust_dict()
        trust_subscription["state"] = ServiceState.finished
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])

    def test_subscription_prolongation(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        trust_subscription = self.create_trust_dict()
        trust_subscription["subs_until_dt"] = "2028-12-06T12:19:17+03:00"
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.btime, 1859707157)

    def test_subscription_change_state_to_grace(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        trust_subscription["state"] = ServiceState.in_grace
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.in_grace)

    def test_subscription_change_state_to_hold(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        trust_subscription["state"] = ServiceState.on_hold
        with self.assert_counter_delta(-self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.on_hold)

    def test_change_state_from_hold_to_active(self):
        trust_subscription = self.create_trust_dict()
        trust_subscription["state"] = ServiceState.on_hold
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertUserHasExactServices(self.uid, ["1tb_1y_apple_appstore_2019", PRODUCT_INITIAL_10GB_ID])
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.on_hold)

        trust_subscription["state"] = ServiceState.active
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertEqual(service.state, ServiceState.active)

    def test_subscription_change_state_to_finished(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        trust_subscription["state"] = ServiceState.finished
        with self.assert_counter_delta(-self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertUserHasExactServices(self.uid, [PRODUCT_INITIAL_10GB_ID])
        service = get_service_by_inapp_subs_id(self.otid)
        self.assertIsNone(service)

    def test_update_btime(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        old_btime = get_service_by_inapp_subs_id(self.otid).btime
        trust_subscription["subs_until_dt"] = "2028-12-06T12:19:17+03:00"
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        new_btime = get_service_by_inapp_subs_id(self.otid).btime
        self.assertGreater(new_btime, old_btime)

    def test_change_pid(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        trust_subscription["product_id"] = "1tb_1m_apple_appstore_2019"
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        self.assertEqual(get_service_by_inapp_subs_id(self.otid).pid, "1tb_1m_apple_appstore_2019")

    def test_change_pid_update_btime(self):
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        old_btime = get_service_by_inapp_subs_id(self.otid).btime

        trust_subscription["product_id"] = "1tb_1m_apple_appstore_2019"
        trust_subscription["subs_until_dt"] = "2028-12-06T12:19:17+03:00"
        with self.assert_counter_delta(0):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        new_btime = get_service_by_inapp_subs_id(self.otid).btime
        self.assertEqual(get_service_by_inapp_subs_id(self.otid).pid, "1tb_1m_apple_appstore_2019")
        self.assertGreater(new_btime, old_btime)

    def test_change_user(self):
        self.create_user(self.user_3.uid)
        trust_subscription = self.create_trust_dict()
        with self.assert_counter_delta(self.one_tb):
            process_trust_sub(TrustInAppSubscription(trust_subscription))
        trust_subscription["uid"] = self.user_3.uid
        self.assertRaises(BillingTrustUIDSyncError, process_trust_sub, TrustInAppSubscription(trust_subscription))
