package ru.yandex.market.tpl.common.covid.external;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TplCovidApiResolverTest {

    @Test
    void resolve_GOSUSLUGI_CERT_NUMBER() {
        //given
        String query = "0000659887469988";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.GOSUSLUGI_CERT_NUMBER, result.get());
    }

    @Test
    void resolve_with_whites_GOSUSLUGI_CERT_NUMBER() {
        //given
        String query = "0000 6598 8746 9988";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.GOSUSLUGI_CERT_NUMBER, result.get());
    }

    @Test
    void resolve_GOSUSLUGI_V1() {
        //given
        String query = "https://www.gosuslugi.ru/api/vaccine/v1/cert/verify/9780000032520297";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.GOSUSLUGI_V1, result.get());
    }

    @Test
    void resolve_GOSUSLUGI_V3() {
        //given
        String query = "https://www.gosuslugi.ru/covid-cert/verify/9780000032520297" +
                "?lang=ru&ck=e32de298c13b59dc872894fb92b0e5d6";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.GOSUSLUGI_V3, result.get());
    }

    @Test
    void resolve_GOSUSLUGI_V2() {
        //given
        String query = "https://www.gosuslugi.ru/covid-cert/status/11x1111x-2xx6-4xx8-8x39-44x7777xx555" +
                "?lang=ru";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.GOSUSLUGI_V2, result.get());
    }

    @Test
    void resolve_GOSUSLUGI_V1_UNRZ() {
        //given
        String query = "https://www.gosuslugi.ru/api/vaccine/v1/cert/verify/unrz/9780000032520297";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.GOSUSLUGI_V1_UNRZ, result.get());
    }

    @Test
    void resolve_MOSRU() {
        //given
        String query = "https://immune.mos.ru/qr?id=12345QWERTY67890";
        //when
        Optional<TplCovidApiType> result = TplCovidApiResolver.resolve(query);

        //then
        assertTrue(result.isPresent());
        assertEquals(TplCovidApiType.MOSRU, result.get());
    }
}
