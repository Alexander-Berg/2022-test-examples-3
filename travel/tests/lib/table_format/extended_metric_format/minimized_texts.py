import datetime

from travel.avia.country_restrictions.lib.table_format.extended_metrics_format import BannerColorEnum, \
    CLOSED_COUNTRY_SIMPLE_MORE_TEXT, ExtendedMetricsFormat, MinimizedTextsInfo, NO_TOURISM_INFO_MORE_TEXT
from travel.avia.country_restrictions.lib.types.metric_type import FLIGHTS_AVAILABILITY_V2, FlightAvailabilityV2Enum, \
    IS_SPUTNIK_APPROVED, TOURISM_AVAILABILITY, TOURISM_AVAILABLE_FROM_METRIC_TYPE, VISA_REQUIRED
from travel.avia.country_restrictions.tests.mocks.geo_format_manager import default_mock_geo_format_manager


def test_no_tourism():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(None),
    }
    assert ExtendedMetricsFormat('', []).get_minimized_texts(
        'l1',
        data,
        BannerColorEnum.GRAY,
        default_mock_geo_format_manager,
    ) == MinimizedTextsInfo(
        title_text='Банания: нет информации о туризме',
        desktop_texts_info=NO_TOURISM_INFO_MORE_TEXT,
        mobile_texts_info=NO_TOURISM_INFO_MORE_TEXT,
    )


def test_closed_country():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
    }
    assert ExtendedMetricsFormat('', []).get_minimized_texts(
        'l3',
        data,
        BannerColorEnum.RED,
        default_mock_geo_format_manager,
    ) == MinimizedTextsInfo(
        title_text='Для туристов закрыто',
        desktop_texts_info=CLOSED_COUNTRY_SIMPLE_MORE_TEXT,
        mobile_texts_info=CLOSED_COUNTRY_SIMPLE_MORE_TEXT,
    )


def test_closed_country_with_open_date():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
        TOURISM_AVAILABLE_FROM_METRIC_TYPE.name: TOURISM_AVAILABLE_FROM_METRIC_TYPE.generate_metric(
            datetime.date(year=2040, month=5, day=5),
        )
    }
    assert ExtendedMetricsFormat('', []).get_minimized_texts(
        'l3',
        data,
        BannerColorEnum.RED,
        default_mock_geo_format_manager,
    ) == MinimizedTextsInfo(
        title_text='Для туристов закрыто',
        desktop_texts_info=['Закрыто для путешествий до 5 мая 2040'],
        mobile_texts_info=['Закрыто для путешествий до 5 мая 2040'],
    )


def test_case_1():
    data = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
        FLIGHTS_AVAILABILITY_V2.name: FLIGHTS_AVAILABILITY_V2.generate_metric(FlightAvailabilityV2Enum.DIRECT_FLIGHTS),
        IS_SPUTNIK_APPROVED.name: IS_SPUTNIK_APPROVED.generate_metric(True),
        VISA_REQUIRED.name: VISA_REQUIRED.generate_metric(False),
    }
    assert ExtendedMetricsFormat('', []).get_minimized_texts(
        'l2',
        data,
        BannerColorEnum.GREEN,
        default_mock_geo_format_manager,
    ) == MinimizedTextsInfo(
        title_text='Финикия: для путешествий открыто',
        desktop_texts_info=['Есть прямые рейсы', 'Можно со Спутником-V', 'Виза не нужна'],
        mobile_texts_info=['Есть прямые рейсы', 'Можно со Спутником-V'],
    )
