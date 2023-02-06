# coding=utf-8
from mock import Mock

from travel.avia.library.python.avia_data.models import AviaCompany
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_company, create_aviacompany
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon.ticket_daemon.api.flights import Variant, IATAFlight
from travel.avia.ticket_daemon.ticket_daemon.lib.baggage import Baggage, SourceInt
from travel.avia.ticket_daemon.ticket_daemon.lib.baggage_merger import BaggageMerger


class BaggageMergerTest(TestCase):
    def setUp(self):
        self._logger = Mock()
        self._yt_logger = Mock()

        self._merger = BaggageMerger(
            logger=self._logger,
            yt_logger=self._yt_logger
        )

    def _test(self, expected_baggage, cost_type,
              partner_baggage, default_baggage, has_company=True, is_charter=False):
        # type: (Baggage, str, Baggage, Baggage, bool, bool) -> None
        company = create_company(
            t_type_id=TransportType.PLANE_ID,
            iata='zzz'
        )
        airline = None
        if cost_type != 'unknown_airline':
            airline = create_aviacompany(
                cost_type=cost_type,
                rasp_company=company
            )
        if not has_company:
            company = None

        actual_baggage = self._merger.merge(
            partner_code='zzz',
            flight_number='ЮГ 123',
            airline=airline,
            default_baggage=default_baggage,
            partner_baggage=partner_baggage,
            company=company,
            is_charter=is_charter,
        )

        for field in ['pieces', 'weight', 'included']:
            expected = getattr(expected_baggage, field)
            actual = getattr(actual_baggage, field)

            assert expected == actual, \
                '{field} must be {expected}, but {actual}'.format(
                    field=field,
                    expected=expected,
                    actual=actual
                )
            if expected is not None:
                assert expected.source == actual.source, \
                    'source of {field} must be {expected}, but {actual}'.format(
                        field=field,
                        expected=expected.source,
                        actual=actual.source
                    )

        assert self._yt_logger.exception.call_count == 0

    def test_low_cost(self):
        """
        Для лоукостеров всегда возвращаем отсутвие багажа
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(0, source='db'),
                pieces=SourceInt(0, source='db'),
                weight=SourceInt(0, source='db'),
            ),
            cost_type=AviaCompany.LOW_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(1, source='partner'),
                weight=SourceInt(10, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(2, source='db'),
                weight=SourceInt(20, source='db'),
            )
        )

    def test_charter_true(self):
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(1, source='partner'),
                weight=SourceInt(10, source='partner'),
            ),
            cost_type=AviaCompany.LOW_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(1, source='partner'),
                weight=SourceInt(10, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(2, source='db'),
                weight=SourceInt(20, source='db'),
            ),
            is_charter=True
        )

    def test_charter_false(self):
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(0, source='db'),
                pieces=SourceInt(0, source='db'),
                weight=SourceInt(0, source='db'),
            ),
            cost_type=AviaCompany.LOW_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(1, source='partner'),
                weight=SourceInt(10, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(2, source='db'),
                weight=SourceInt(20, source='db'),
            ),
            is_charter=False
        )

    def test_unknown_airline(self):
        """
        Если информация о багаже неизвестная, то всегда наличие багажа неизвестно
        """
        self._test(
            cost_type='unknown_airline',
            default_baggage=Baggage.create_empty(),
            expected_baggage=Baggage.create_empty(),
            partner_baggage=Baggage.create_empty(),
        )

    def test_unknown_company(self):
        """
        Если авиакомпания неизвестная, то всегда наличие багажа неизвестно
        """
        self._test(
            cost_type='unknown_airline',
            default_baggage=Baggage.create_empty(),
            expected_baggage=Baggage.create_empty(),
            partner_baggage=Baggage.create_empty(),
            has_company=False
        )

    def test_for_normal_airline__first_case(self):
        """
        Если компания нормальная, то берем всю информацию из базы.
        НО если в базе багаж разрешен и партнер вернул количество мест,
            то берем места из партнера,
            иначе берем из базы
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='partner'),
                weight=SourceInt(9, source='db'),
            ),
            cost_type=AviaCompany.NORMAL_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(1, source='partner'),
                weight=SourceInt(99, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(9, source='db'),
            )
        )

    def test_for_normal_airline__second_case(self):
        """
        Если компания нормальная, то берем всю информацию из базы.
        НО если в базе багаж разрешен и партнер вернул количество мест,
            то берем места из партнера,
            иначе берем из базы
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(2, source='partner'),
                weight=SourceInt(9, source='db'),
            ),
            cost_type=AviaCompany.NORMAL_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(2, source='partner'),
                weight=SourceInt(99, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(9, source='db'),
            )
        )

    def test_for_normal_airline__third_case(self):
        """
        Если компания нормальная, то берем всю информацию из базы.
        НО если в базе багаж разрешен и партнер вернул количество мест,
            то берем места из партнера,
            иначе берем из базы
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(0, source='db'),
                pieces=SourceInt(0, source='db'),
                weight=None,
            ),
            cost_type=AviaCompany.NORMAL_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(2, source='partner'),
                weight=SourceInt(99, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(0, source='db'),
                pieces=SourceInt(0, source='db'),
                weight=None,
            )
        )

    def test_for_normal_airline__fourth_case(self):
        """
        Если компания нормальная, то берем всю информацию из базы.
        НО если в базе багаж разрешен и партнер вернул количество мест,
            то берем места из партнера,
            иначе берем из базы
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(30, source='db'),
            ),
            cost_type=AviaCompany.NORMAL_COST,
            partner_baggage=Baggage(
                included=None,
                pieces=None,
                weight=None,
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(30, source='db'),
            )
        )

    def test_for_normal_airline__fifth_case(self):
        """
        RASPTICKETS-13279: Если компания нормальная, но информации в базе нет, то все равно ставим included=True
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=None,
                weight=None,
            ),
            cost_type=AviaCompany.NORMAL_COST,
            partner_baggage=Baggage(
                included=None,
                pieces=None,
                weight=None,
            ),
            default_baggage=Baggage(
                included=None,
                pieces=None,
                weight=None,
            )
        )

    def test_for_hybrid_airline__first_case(self):
        """
        Если компания гибридная и партнер говорит багажа есть!
        То берем всю информацию о багаже из партнера, кроме веса.
        Вес всегда берем из базы
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(2, source='partner'),
                weight=SourceInt(9, source='db'),
            ),
            cost_type=AviaCompany.HYBRID_COST,
            partner_baggage=Baggage(
                included=SourceInt(1, source='partner'),
                pieces=SourceInt(2, source='partner'),
                weight=SourceInt(99, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(0, source='db'),
                pieces=SourceInt(0, source='db'),
                weight=SourceInt(9, source='db'),
            )
        )

    def test_for_hybrid_airline__second_case(self):
        """
        Если компания гибридная и партнер говорит багажа нет!
        То значит багажа нет
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(0, source='partner'),
                pieces=SourceInt(0, source='partner'),
                weight=SourceInt(0, source='partner'),
            ),
            cost_type=AviaCompany.HYBRID_COST,
            partner_baggage=Baggage(
                included=SourceInt(0, source='partner'),
                pieces=SourceInt(0, source='partner'),
                weight=SourceInt(0, source='partner'),
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(9, source='db'),
            )
        )

    def test_for_hybrid_airline__third_case(self):
        """
        Если компания гибридная и партнер не знает ничего о багаже, то:
        все берем из базы
        """
        self._test(
            expected_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(9, source='db'),
            ),
            cost_type=AviaCompany.HYBRID_COST,
            partner_baggage=Baggage(
                included=None,
                pieces=None,
                weight=None,
            ),
            default_baggage=Baggage(
                included=SourceInt(1, source='db'),
                pieces=SourceInt(1, source='db'),
                weight=SourceInt(9, source='db'),
            )
        )


class MergeVariantBaggage(TestCase):
    def setUp(self):
        self._logger = Mock()
        self._yt_logger = Mock()

        self._merger = BaggageMerger(
            logger=self._logger,
            yt_logger=self._yt_logger
        )

    def _create_variant(self, forward_baggage_list, backward_baggage_list):
        def fill_baggage(segments, baggage_list):
            for baggage in baggage_list:
                f = IATAFlight()
                f.baggage = baggage
                segments.append(
                    f
                )

        v = Variant()
        fill_baggage(v.forward.segments, forward_baggage_list)
        fill_baggage(v.backward.segments, backward_baggage_list)

        return v

    def _test(self, v, expected):
        result = self._merger.merge_variant_baggage(v)

        assert result.included == expected.included
        assert result.pieces == expected.pieces
        assert result.weight == expected.weight

    def test_direct_flight_with_baggage(self):
        v = self._create_variant([
            Baggage(
                included=SourceInt(1),
                pieces=SourceInt(1),
                weight=SourceInt(10),
            )
        ], [])

        self._test(v, Baggage(
            included=SourceInt(1),
            pieces=SourceInt(1),
            weight=SourceInt(10),
        ))

    def test_direct_flight_without_baggage(self):
        v = self._create_variant([
            Baggage(
                included=SourceInt(0),
                pieces=SourceInt(0),
                weight=SourceInt(0),
            )
        ], [])

        self._test(v, Baggage(
            included=SourceInt(0),
            pieces=SourceInt(0),
            weight=SourceInt(0),
        ))

    def test_direct_flight_with_unknown(self):
        v = self._create_variant([
            Baggage(
                included=None,
                pieces=SourceInt(0),
                weight=SourceInt(0),
            )
        ], [])

        self._test(v, Baggage(
            included=SourceInt(0),
            pieces=SourceInt(0),
            weight=SourceInt(0),
        ))

    def test_direct_and_backward_flight_with_baggage(self):
        v = self._create_variant([
            Baggage(
                included=SourceInt(1),
                pieces=SourceInt(1),
                weight=SourceInt(10),
            )
        ], [
            Baggage(
                included=SourceInt(1),
                pieces=SourceInt(2),
                weight=SourceInt(20),
            )
        ])

        other_v = self._create_variant([
            Baggage(
                included=SourceInt(1),
                pieces=SourceInt(3),
                weight=SourceInt(30),
            )
        ], [
            Baggage(
                included=SourceInt(1),
                pieces=SourceInt(2),
                weight=SourceInt(20),
            )
        ])

        self._test(v, Baggage(
            included=SourceInt(1),
            pieces=SourceInt(1),
            weight=SourceInt(10),
        ))
        self._test(other_v, Baggage(
            included=SourceInt(1),
            pieces=SourceInt(2),
            weight=SourceInt(20),
        ))

    def test_direct_and_backward_flight_without_baggage(self):
        v = self._create_variant([
            Baggage(
                included=SourceInt(0),
                pieces=SourceInt(0),
                weight=SourceInt(0),
            )
        ], [
            Baggage(
                included=SourceInt(0),
                pieces=SourceInt(0),
                weight=SourceInt(0),
            )
        ])

        self._test(v, Baggage(
            included=SourceInt(0),
            pieces=SourceInt(0),
            weight=SourceInt(0),
        ))

    def test_direct_and_backward_flight_unknown_baggage(self):
        v = self._create_variant([
            Baggage(
                included=None,
                pieces=None,
                weight=None,
            )
        ], [
            Baggage(
                included=None,
                pieces=None,
                weight=None,
            )
        ])

        self._test(v, Baggage(
            included=SourceInt(0),
            pieces=SourceInt(0),
            weight=SourceInt(0),
        ))

    def test_None_in_baggage(self):
        v = self._create_variant([
            None
        ], [
            None
        ])

        self._test(v, Baggage(
            included=SourceInt(0),
            pieces=SourceInt(0),
            weight=SourceInt(0),
        ))
