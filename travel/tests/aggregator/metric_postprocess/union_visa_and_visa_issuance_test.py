from travel.avia.country_restrictions.lib.types.metric_type import VISA_ISSUANCE, VISA_REQUIRED
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text
from travel.avia.country_restrictions.aggregator.metric_postprocess.union_visa_and_visa_issuance import processor


def test_all_true():
    initial = {
        VISA_REQUIRED.name: VISA_REQUIRED.generate_metric(True),
        VISA_ISSUANCE.name: VISA_ISSUANCE.generate_metric(True),
    }

    result = processor('l1', initial, None)

    expected = {
        VISA_REQUIRED.name: VISA_REQUIRED.generate_metric(True),
    }

    assert result == expected


def test_no_issuance_and_need_visa():
    initial = {
        VISA_REQUIRED.name: VISA_REQUIRED.generate_metric(True),
        VISA_ISSUANCE.name: VISA_ISSUANCE.generate_metric(False),
    }

    result = processor('l1', initial, None)

    visa_metric = VISA_REQUIRED.generate_metric(True)
    visa_metric.text = new_rich_text(
        'Для въезда нужна виза, но на данный момент выдача виз гражданам России приостановлена',
    )
    expected = {
        VISA_REQUIRED.name: visa_metric,
    }

    assert result == expected
