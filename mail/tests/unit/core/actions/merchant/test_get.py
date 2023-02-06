import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.merchant.get import GetMerchantAction
from mail.payments.payments.core.entities.enums import FunctionalityType
from mail.payments.payments.core.entities.functionality import Functionalities


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestGetMerchant:
    @pytest.fixture
    def action(self, params):
        return GetMerchantAction(**params)

    @pytest.fixture
    def merchant_by_uid(self, merchant):
        return {merchant.uid: merchant}

    @pytest.fixture
    def params(self, merchant):
        return {'uid': merchant.uid}

    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    def test_returns_merchant(self, merchant, returned):
        returned.moderation = returned.moderations = None
        merchant.oauth = []
        assert returned == merchant

    class TestTokenParam:
        @pytest.fixture
        def params(self, merchant):
            return {'token': merchant.token}

        @pytest.mark.asyncio
        async def test_can_get_by_token(self, merchant, action):
            by_token = await action.run()
            by_token.moderations = by_token.moderation = None
            merchant.oauth = []
            assert by_token == merchant

    @pytest.mark.parametrize('moderations_data,approved,reasons,has_ongoing', (
        pytest.param([{'approved': True}], True, [], False, id='approved'),
        pytest.param([{'approved': False, 'reasons': [1, 2, 3]}], False, [1, 2, 3], False, id='disapproved'),
        pytest.param(
            [{'approved': False, 'reasons': [1, 2, 3]}, {'approved': None}], False, [1, 2, 3], True,
            id='disapproved-ongoing',
        ),
    ))
    def test_moderation(self, moderations, returned, approved, reasons, has_ongoing):
        moderation = returned.moderation
        assert all([
            moderation.approved == approved,
            moderation.reasons == reasons,
            moderation.has_ongoing == has_ongoing,
        ])

    @pytest.mark.parametrize('moderations_data,expected_payments,expected_yandexpay', (
        pytest.param(
            [
                {'approved': True, 'functionality_type': FunctionalityType.PAYMENTS},
                {'approved': False, 'functionality_type': FunctionalityType.YANDEX_PAY},
            ],
            has_properties({'approved': True, 'has_ongoing': False, 'has_moderation': True}),
            has_properties({'approved': False, 'has_ongoing': False, 'has_moderation': True}),
            id='both'
        ),
        pytest.param(
            [
                {'approved': True, 'functionality_type': FunctionalityType.PAYMENTS},
            ],
            has_properties({'approved': True, 'has_ongoing': False, 'has_moderation': True}),
            has_properties({'approved': False, 'has_ongoing': False, 'has_moderation': False}),
            id='yandexpay-absent'
        ),
    ))
    def test_moderations(self, moderations, returned, expected_payments, expected_yandexpay):
        moderations = returned.moderations
        assert_that(moderations[FunctionalityType.PAYMENTS], expected_payments)
        assert_that(moderations[FunctionalityType.YANDEX_PAY], expected_yandexpay)

    class TestGetsMerchantWithPreregistration:
        @pytest.fixture
        def merchant_preregistered_uid(self, merchant_uid):
            return merchant_uid

        @pytest.fixture
        def params(self, merchant, merchant_preregistration):
            return {
                'uid': merchant.uid,
                'skip_preregistration': False,
            }

        def test_returns_merchant_with_registration(self, returned, merchant_preregistration):
            assert returned.preregistration == merchant_preregistration

    def test_oauth(self, merchant_oauth, returned):
        assert returned.oauth == [merchant_oauth]

    class TestParent:
        @pytest.fixture
        def moderations_data(self, merchant_with_parent):
            return [
                {'uid': merchant_with_parent.uid, 'approved': False, 'revision': merchant_with_parent.revision - 1},
                {'uid': merchant_with_parent.uid, 'approved': None, 'revision': merchant_with_parent.revision},
                {'uid': merchant_with_parent.parent_uid, 'approved': True},
            ]

        @pytest.fixture
        def params(self, merchant_with_parent, moderations):
            return {'uid': merchant_with_parent.uid}

        @pytest.fixture
        async def parent_oauth(self, storage, merchant_with_parent, create_merchant_oauth):
            parent = await storage.merchant.get(merchant_with_parent.parent_uid)
            return await create_merchant_oauth(uid=parent.uid)

        @pytest.fixture
        async def self_oauth(self, storage, merchant_with_parent, create_merchant_oauth):
            return await create_merchant_oauth(uid=merchant_with_parent.uid)

        def test_returns_with_parent(self, merchant_with_parent, returned):
            returned.moderations = returned.moderation = None
            merchant_with_parent.oauth = []
            returned.parent.oauth = []  # FIXME: нужно ли загружать oauth у parent'а?
            returned.parent.functionalities = Functionalities()
            assert returned == merchant_with_parent

        def test_returns_parent_moderation(self, merchant_with_parent, returned):
            moderation = returned.moderation
            assert moderation.approved and not moderation.has_ongoing

        def test_not_returns_parent_oauth(self, self_oauth, returned):
            assert returned.oauth == [self_oauth]
