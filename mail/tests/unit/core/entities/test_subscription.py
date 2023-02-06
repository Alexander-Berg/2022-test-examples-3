import pytest

from mail.payments.payments.core.entities.enums import PeriodUnit


class TestSubscription:
    @pytest.mark.parametrize('field', ('period', 'trial_period'))
    def test_period(self, field, subscription):
        amount = getattr(subscription, f'{field}_amount')
        units = getattr(subscription, f'{field}_units')
        assert f'{amount}{units.value}' == getattr(subscription, field)

    @pytest.mark.parametrize('field_set_none', ('amount', 'units'))
    def test_without_trial(self, subscription, field_set_none):
        setattr(subscription, f'trial_period_{field_set_none}', None)
        assert subscription.trial_period is None

    def test_product_id(self, subscription):
        target = f'subs-{subscription.uid}-{subscription.subscription_id}-{subscription.product_uuid}'
        assert subscription.product_id == target

    @pytest.mark.parametrize('unit', list(PeriodUnit))
    def test_approx_period_seconds(self, subscription, unit):
        subscription.period_amount = 1
        subscription.period_units = unit

        answers = {
            PeriodUnit.SECOND: 1,
            PeriodUnit.DAY: 86400,  # 60 * 60 * 24
            PeriodUnit.WEEK: 604800,  # 60 * 60 * 24 * 7
            PeriodUnit.MONTH: 2629800,  # 60 * 60 * 24 * 30.4375
            PeriodUnit.YEAR: 31557600  # 60 * 60 * 24 * 30.4375 * 12
        }

        assert subscription.approx_period_seconds == answers[unit]
