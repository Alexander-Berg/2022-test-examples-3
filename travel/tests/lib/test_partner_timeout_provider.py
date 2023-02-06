from logging import Logger

import pytest
from mock import Mock
from typing import cast

from travel.avia.library.python.common.models.partner import Partner, DohopVendor
from travel.avia.ticket_daemon_api.jsonrpc.lib.partner_timeout_provider import (
    PartnerRedirectTimeoutProvider, UnknownPartnerError, DisabledPartnerError
)
from travel.avia.library.python.tester.testcase import TestCase


class PartnerRedirectTimeoutProviderTest(TestCase):
    def setUp(self):
        self._default_timeout = Mock()
        self._dohop_timeout = Mock()
        self._custom_timeout_for_custom = Mock()

        self._fake_logger = Mock()
        self._fake_partners_utils = Mock()

        self._provider = PartnerRedirectTimeoutProvider(
            logger=cast(Logger, self._fake_logger),
            partners_utils=self._fake_partners_utils,
            default_timeout=self._default_timeout,
            dohop_timeout=self._dohop_timeout,
            custom_timeout={
                'custom': self._custom_timeout_for_custom
            }
        )

    def test_if_partner_is_unknown_that_throw_exception(self):
        self._fake_partners_utils.get_partner_by_code = Mock(
            return_value=None
        )

        with pytest.raises(UnknownPartnerError):
            self._provider.get(
                partner_code='not_found'
            )

    def test_if_partner_is_disabled_that_throw_exception(self):
        partner = Partner()
        partner.disabled = True

        self._fake_partners_utils.get_partner_by_code = Mock(
            return_value=partner
        )

        with pytest.raises(DisabledPartnerError):
            self._provider.get(
                partner_code='not_found'
            )

    def test_if_vendor_is_disabled_that_throw_exception(self):
        partner = DohopVendor()
        partner.enabled = False

        self._fake_partners_utils.get_partner_by_code = Mock(
            return_value=partner
        )

        with pytest.raises(DisabledPartnerError):
            self._provider.get(
                partner_code='not_found'
            )

    def test_if_vendor_is_enabled_that_timeout_is_default_for_vendor(self):
        partner = DohopVendor()
        partner.enabled = True

        self._fake_partners_utils.get_partner_by_code = Mock(
            return_value=partner
        )
        assert self._provider.get(
            partner_code='not_found'
        ) == self._dohop_timeout

    def test_if_partner_is_enabled_and_regular_that_timeout_is_default(self):
        partner = Partner(
            code='default'
        )
        partner.disabled = False

        self._fake_partners_utils.get_partner_by_code = Mock(
            return_value=partner
        )
        assert self._provider.get(
            partner_code='default'
        ) == self._default_timeout

    def test_if_partner_is_enabled_and_custom_that_timeout_is_custom(self):
        partner = Partner(
            code='custom'
        )
        partner.disabled = False

        self._fake_partners_utils.get_partner_by_code = Mock(
            return_value=partner
        )
        assert self._provider.get(
            partner_code='custom'
        ) == self._custom_timeout_for_custom
