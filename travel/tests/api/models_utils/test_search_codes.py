# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from travel.avia.library.python.common.models.geo import CodeSystem, Station2Settlement
from travel.avia.library.python.tester.factories import (
    create_settlement, create_station, create_station_code
)
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches

from travel.avia.ticket_daemon.ticket_daemon.api.models_utils.search_codes import (
    get_iata_code_for_search, get_sirena_code_for_search,
    get_iata_codes_for_search, get_sirena_codes_for_search)


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

        assert 'TMP' == get_iata_code_for_search('s3')
        assert ['TMP'] == get_iata_codes_for_search('s3')

    def test_without_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города не заполнена iata
         возвращает код первого попавшегося аэропорта города

        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        create_station_code(code='TMP', system_id=self.iata_code_system.id, station_id=3)
        assert 'TMP' == get_iata_code_for_search('s3')
        assert ['TMP'] == get_iata_codes_for_search('s3')

    def test_with_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города заполнена iata
         возвращает код первого попавшегося аэропорта города
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert 'WWW' == get_iata_code_for_search('s3')
        assert ['WWW'] == get_iata_codes_for_search('s3')

    def test_no_codes(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)
        assert get_iata_code_for_search('s3') is None
        assert get_iata_codes_for_search('s3') is None

    def test_unknown_station(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)
        assert get_iata_code_for_search('s4') is None
        assert get_iata_codes_for_search('s4') is None


class TestIATASettlementPointKeys(GetCodeForSearchTestCase):
    def test_1_airport_station_with_code(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, iata='TMP')
        create_station(id=3, settlement_id=1)

        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert 'TMP' == get_iata_code_for_search('c1')
        assert ['TMP'] == get_iata_codes_for_search('c1')

    def test_1_airport_no_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города не заполнена iata
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)

        create_station_code(code='TMP', system_id=self.iata_code_system.id, station_id=3)

        assert 'TMP' == get_iata_code_for_search('c1')
        assert ['TMP'] == get_iata_codes_for_search('c1')

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

        assert 'TMP' == get_iata_code_for_search('c1')
        assert ['TMP'] == get_iata_codes_for_search('c1')

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

        assert get_iata_code_for_search('c1') in {'WWW', 'ZZZ'}
        assert {'WWW', 'ZZZ'} == set(get_iata_codes_for_search('c1'))

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

        assert 'ZZZ' == get_iata_code_for_search('c1')
        assert ['ZZZ', 'WWW'] == get_iata_codes_for_search('c1')

    def test_s2s(self):
        """
        Привязка к другому городу через s2s
        """
        create_settlement(id=1)
        create_settlement(id=2, iata='TMP')
        create_station(id=3, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)
        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert 'TMP' == get_iata_code_for_search('c1')
        assert ['TMP'] == get_iata_codes_for_search('c1')

    def test_s2s_airport(self):
        """
        Привязка к другому городу через s2s, город без iata, берется iata аэропорта
        """
        create_settlement(id=1)
        create_settlement(id=2)
        create_station(id=3, settlement_id=2)
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)
        create_station_code(code='WWW', system_id=self.iata_code_system.id, station_id=3)

        assert 'WWW' == get_iata_code_for_search('c1')
        assert ['WWW'] == get_iata_codes_for_search('c1')

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

        assert 'MIL' == get_iata_code_for_search('c1')
        assert ['MIL', 'BER'] == get_iata_codes_for_search('c1')

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

        assert 'MIA' == get_iata_code_for_search('c1')
        assert ['MIA', 'BER'] == get_iata_codes_for_search('c1')

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

        assert 'MIA' == get_iata_code_for_search('c1')
        assert ['MIA', 'BGY'] == get_iata_codes_for_search('c1')


class TestSirenaStationPointKeys(GetCodeForSearchTestCase):
    def test_station_without_code(self):
        """
        Для станции берется sirena-код связанного напрямую с ней города, если у станции не проставлен код
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1)

        assert 'ТМП' == get_sirena_code_for_search('s3')
        assert ['ТМП'] == get_sirena_codes_for_search('s3')

    def test_without_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города не заполнена sirena
         возвращает код первого попавшегося аэропорта города

        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='ОМГ')

        assert 'ОМГ' == get_sirena_code_for_search('s3')
        assert ['ОМГ'] == get_sirena_codes_for_search('s3')

    def test_with_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города заполнена sirena
         возвращает код первого попавшегося аэропорта города
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1, sirena_id='ОМГ')

        assert 'ОМГ' == get_sirena_code_for_search('s3')
        assert ['ОМГ'] == get_sirena_codes_for_search('s3')

    def test_no_codes(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)
        assert get_sirena_code_for_search('s3') is None
        assert get_sirena_codes_for_search('s3') is None

    def test_unknown_station(self):
        create_settlement(id=1)
        create_station(id=3, settlement_id=1)
        assert get_sirena_code_for_search('s4') is None
        assert get_sirena_codes_for_search('s4') is None


class TestSirenaSettlementPointKeys(GetCodeForSearchTestCase):
    def test_1_airport_station_with_code(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1, sirena_id='ОМГ')

        assert 'ТМП' == get_sirena_code_for_search('c1')
        assert ['ТМП'] == get_sirena_codes_for_search('c1')

    def test_1_airport_station_without_code(self):
        """
        Ничего не найдем, если у аэропорта, связанного с городом не заполнена sirena

        TODO: верна ли задумка?
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1)

        assert get_sirena_code_for_search('c1') is None
        assert get_sirena_codes_for_search('c1') is None

    def test_1_airport_no_settlement_iata(self):
        """
        Для города у которого есть 1 аэропорт, код совпадает с городом
         у города не заполнена sirena_id
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='ТМП')

        assert 'ТМП' == get_sirena_code_for_search('c1')
        assert ['ТМП'] == get_sirena_codes_for_search('c1')

    def test_2_airports_station_with_code(self):
        """
        Для города у которого есть 2 аэропорта, код совпадает с городом
         возвращает код города если в нём есть аэропорты
        """
        create_settlement(id=1, sirena_id='ТМП')
        create_station(id=3, settlement_id=1, sirena_id='AAA')
        create_station(id=4, settlement_id=1, sirena_id='БББ')

        assert 'ТМП' == get_sirena_code_for_search('c1')
        assert ['ТМП'] == get_sirena_codes_for_search('c1')

    def test_2_airports_settlement_without_code(self):
        """
        Для города у которого есть 2 аэропорта, но не заполнен код города
         возвращает код первого попавшегося аэропорта
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='AAA')
        create_station(id=4, settlement_id=1, sirena_id='БББ')

        assert get_sirena_code_for_search('c1') in {'AAA', 'БББ'}
        assert set(get_sirena_codes_for_search('c1')) == {'AAA', 'БББ'}

    def test_2_airports_settlement_without_code_hidden_airport(self):
        """
        Для города у которого есть 2 аэропорта, но не заполнен код города
         возвращает код первого попавшегося аэропорта после сортировки по скрытости
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='AAA', hidden=True)
        create_station(id=4, settlement_id=1, sirena_id='БББ')

        assert 'БББ' == get_sirena_code_for_search('c1')
        assert ['БББ', 'AAA'] == get_sirena_codes_for_search('c1')

    def test_s2s(self):
        """
        Привязка к другому городу через s2s
        """
        create_settlement(id=1)
        create_settlement(id=2, sirena_id='ТМП')
        create_station(id=3, settlement_id=2, sirena_id='ОМГ')
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)

        assert 'ТМП' == get_sirena_code_for_search('c1')
        assert ['ТМП'] == get_sirena_codes_for_search('c1')

    def test_s2s_airport(self):
        """
        Привязка к другому городу через s2s, город без sirena, берется sirena аэропорта
        """
        create_settlement(id=1)
        create_settlement(id=2)
        create_station(id=3, settlement_id=2, sirena_id='ОМГ')
        Station2Settlement.objects.get_or_create(station_id=3, settlement_id=1)

        assert 'ОМГ' == get_sirena_code_for_search('c1')
        assert ['ОМГ'] == get_sirena_codes_for_search('c1')

    def test_s2s_multisearch(self):
        """
        Город с sirena, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1, sirena_id='МИЛ')
        create_station(id=3, settlement_id=1, sirena_id='МИА')
        create_settlement(id=2, sirena_id='БЕР')
        create_station(id=4, settlement_id=2, sirena_id='БРГ')
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)

        assert 'МИЛ' == get_sirena_code_for_search('c1')
        assert ['МИЛ', 'БЕР'] == get_sirena_codes_for_search('c1')

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

        assert 'МИА' == get_sirena_code_for_search('c1')
        assert ['МИА', 'БЕР'] == get_sirena_codes_for_search('c1')

    def test_s2s_multisearch_airport(self):
        """
        Город без sirena, у которого есть свой аэропорт и дополнительная привязка по s2s
        """
        create_settlement(id=1)
        create_station(id=3, settlement_id=1, sirena_id='МИА')
        create_settlement(id=2)
        create_station(id=4, settlement_id=2, sirena_id='БРГ')
        Station2Settlement.objects.get_or_create(station_id=4, settlement_id=1)

        assert 'МИА' == get_sirena_code_for_search('c1')
        assert ['МИА', 'БРГ'] == get_sirena_codes_for_search('c1')
