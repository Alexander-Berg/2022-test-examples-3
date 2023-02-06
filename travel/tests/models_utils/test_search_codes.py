# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from travel.avia.library.python.common.models.geo import CodeSystem, Station2Settlement
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.models_utils.search_codes import PointReason, get_codes_for_search
from travel.avia.library.python.tester.factories import (
    create_settlement, create_station, create_station_code
)
from travel.avia.library.python.tester.testcase import TestCase


class GetCodeForSearchTestCase(TestCase):
    def setUp(self):
        reset_all_caches()

        self.iata_code_system, _ = CodeSystem.objects.get_or_create(code='iata')


class TestIATAStationPointKeys(GetCodeForSearchTestCase):
    def test_station_without_code(self):
        """
        Для станции берется iata-код связанного напрямую с ней города, если у станции не проставлен код
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)

        assert [('c1', (PointReason.STATION_HAS_NO_CODE_FOR_STATION_SEARCH,))] == get_codes_for_search('s3')

    def test_without_settlement_iata(self):
        """
        Для города у которого есть аэропорт,
         у города не заполнена iata
         возвращает код аэропорта города

        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        create_station_code(code='TMP', system_id=self.iata_code_system.id, station_id=3)

        assert [('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH,))] == get_codes_for_search('s3')

    def test_with_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города заполнена iata
         возвращает код первого попавшегося аэропорта города
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert [('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH,))] == get_codes_for_search('s3')

    def test_no_codes(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        assert get_codes_for_search('s3') is None

    def test_unknown_station(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        assert get_codes_for_search('s4') is None


class TestIATASettlementPointKeys(GetCodeForSearchTestCase):
    def test_1_airport_station_with_code(self):
        """
        Для города у которого есть 1 аэропорт,
        возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert [('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_1_airport_station_without_code(self):
        """
        Даже, если у города нет станций с кодами, всё-равно будем искать до города
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)

        assert [('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_1_airport_no_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города не заполнена iata
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        create_station_code(code='TMP', system_id=self.iata_code_system.id, station_id=3)

        assert [('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_2_airports_station_with_code(self):
        """
        Для города у которого есть 2 аэропорта, код совпадает с городом
         возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)
        create_station(id=4, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)
        create_station_code(code='ZZZ', system_id=self.iata_code_system.id, station_id=4)

        assert [('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_2_airports_settlement_without_code(self):
        """
        Для города у которого есть 2 аэропорта, но не заполнен код города
         возвращает код первого попавшегося аэропорта
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)
        create_station(id=4, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)
        create_station_code(code='ZZZ', system_id=self.iata_code_system.id, station_id=4)

        assert {
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,))
        } == set(get_codes_for_search('c1'))

    def test_2_airports_settlement_without_code_hidden_airport(self):
        """
        Для города у которого есть 2 аэропорта, но не заполнен код города
         возвращает код первого попавшегося аэропорта, после сортировки по скрытости
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, hidden=True)
        create_station(id=4, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)
        create_station_code(code='ZZZ', system_id=self.iata_code_system.id, station_id=4)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,))
        ] == get_codes_for_search('c1')

    def test_s2s(self):
        """
        Привязка к другому городу через s2s
        """
        create_settlement(id=1)
        create_settlement(id=2, iata='TMP')
        create_station(id=3, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)
        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert [
            ('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_airport(self):
        """
        Привязка к другому городу через s2s, город без iata, берется iata аэропорта
        """
        create_settlement(id=1)
        create_settlement(id=2)
        create_station(id=3, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)
        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert [
            ('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch(self):
        """
        Город с iata, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1, iata='MIL')
        create_settlement(id=2, iata='BER')
        create_station(id=3, settlement_id=1)
        create_station(id=4, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)
        create_station_code(code='MIA', system_id=self.iata_code_system.id, station_id=4)
        create_station_code(code='BGY', system_id=self.iata_code_system.id, station_id=3)

        assert [
            ('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch_native_airport(self):
        """
        Город без iata, у которого есть свой аэропорт и дополнительная привязка по s2s
        к аэропорту, у города которого есть iata
        """
        create_settlement(id=1)
        create_settlement(id=2, iata='BER')
        create_station(id=3, settlement_id=1)
        create_station(id=4, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)
        create_station_code(code='MIA', system_id=self.iata_code_system.id, station_id=3)
        create_station_code(code='BGY', system_id=self.iata_code_system.id, station_id=4)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch_airport(self):
        """
        Город без iata, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1)
        create_settlement(id=2)
        create_station(id=3, settlement_id=1)
        create_station(id=4, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)
        create_station_code(code='MIA', system_id=self.iata_code_system.id, station_id=3)
        create_station_code(code='BGY', system_id=self.iata_code_system.id, station_id=4)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch_settlement_code_only(self):
        """
        Город без iata, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1)
        create_settlement(id=2, iata='BER')
        create_station(id=3, settlement_id=1)
        create_station(id=4, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)
        create_station_code(code='MIA', system_id=self.iata_code_system.id, station_id=3)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('c2', (PointReason.STATION_HAS_NO_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')


class TestSirenaStationPointKeys(GetCodeForSearchTestCase):
    def test_station_without_code(self):
        """
        Для станции берется sirena-код связанного напрямую с ней города, если у станции не проставлен код
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1)

        assert [('c1', (PointReason.STATION_HAS_NO_CODE_FOR_STATION_SEARCH,))] == get_codes_for_search('s3')

    def test_without_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт,
         у города не заполнена sirena
         возвращает код первого попавшегося аэропорта города

        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='ОМГ')

        assert [('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH,))] == get_codes_for_search('s3')

    def test_with_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города заполнена sirena
         возвращает код первого попавшегося аэропорта города
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1, sirena_id='ОМГ')

        assert [('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH,))] == get_codes_for_search('s3')

    def test_no_codes(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        assert get_codes_for_search('s3') is None

    def test_unknown_station(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        assert get_codes_for_search('s4') is None


class TestSirenaSettlementPointKeys(GetCodeForSearchTestCase):
    def test_1_airport_station_with_code(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1, sirena_id='ОМГ')

        assert [('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_1_airport_station_without_code(self):
        """
        Даже, если у города нет станций с кодами, всё-равно будем искать до города
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1)

        assert [('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_1_airport_no_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города не заполнена sirena_id
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='ТМП')

        assert [('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_2_airports_station_with_code(self):
        """
        Для города у которого есть 2 аэропорта, код совпадает с городом
         возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1, sirena_id='AAA')
        create_station(id=4, settlement_id=1, sirena_id='БББ')

        assert [('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,))] == get_codes_for_search('c1')

    def test_2_airports_settlement_without_code(self):
        """
        Для города у которого есть 2 аэропорта, но не заполнен код города
         возвращает код первого попавшегося аэропорта
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='AAA')
        create_station(id=4, settlement_id=1, sirena_id='БББ')

        assert {
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,))
        } == set(get_codes_for_search('c1'))

    def test_2_airports_settlement_without_code_hidden_airport(self):
        """
        Для города у которого есть 2 аэропорта, но не заполнен код города
         возвращает код первого попавшегося аэропорта после сортировки по скрытости
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='AAA', hidden=True)
        create_station(id=4, settlement_id=1, sirena_id='БББ')

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,))
        ] == get_codes_for_search('c1')

    def test_s2s(self):
        """
        Привязка к другому городу через s2s
        """
        create_settlement(id=1)
        create_settlement(id=2, sirena_id='ТМП')
        create_station(id=3, settlement_id=2, sirena_id='ОМГ')
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)

        assert [
            ('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_airport(self):
        """
        Привязка к другому городу через s2s, город без sirena, берется sirena аэропорта
        """
        create_settlement(id=1)
        create_settlement(id=2)
        create_station(id=3, settlement_id=2, sirena_id='ОМГ')
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)

        assert [
            ('s3', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch(self):
        """
        Город с sirena, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1, sirena_id='МИЛ')
        create_station(id=3, settlement_id=1, sirena_id='МИА')
        create_settlement(id=2, sirena_id='БЕР')
        create_station(id=4, settlement_id=2, sirena_id='БРГ')
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)

        assert [
            ('c1', (PointReason.SETTLEMENT_HAS_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch_native_airport(self):
        """
        Город без sirena, у которого есть свой аэропорт и дополнительная привязка по s2s
        к аэропорту, у города которого есть sirena
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='МИА')
        create_settlement(id=2, sirena_id='БЕР')
        create_station(id=4, settlement_id=2, sirena_id='БРГ')
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch_airport(self):
        """
        Город без sirena, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='МИА')
        create_settlement(id=2)
        create_station(id=4, settlement_id=2, sirena_id='БРГ')
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('s4', (PointReason.STATION_HAS_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')

    def test_s2s_multisearch_settlement_code_only(self):
        """
        Город без iata, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1)
        create_settlement(id=2, sirena_id='BER')
        create_station(id=3, settlement_id=1, sirena_id='MIA')
        create_station(id=4, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)

        assert [
            ('s3', (PointReason.SETTLEMENT_HAS_NO_CODE_FOR_SETTLEMENT_SEARCH,)),
            ('c2', (PointReason.STATION_HAS_NO_CODE_FOR_STATION_SEARCH, PointReason.SETTLEMENT_S2S_SEARCH)),
        ] == get_codes_for_search('c1')
