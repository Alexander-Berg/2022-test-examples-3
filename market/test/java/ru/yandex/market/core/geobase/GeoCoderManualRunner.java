package ru.yandex.market.core.geobase;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.client.GeoClientBuilder;


/**
 * Класс для проверки вручную адреса, когда он не находится
 */
public class GeoCoderManualRunner {

    public static void main(String[] args) {
        GeoClient client = GeoClientBuilder.newBuilder()
        .withApiBaseUrl("http://addrs-testing.search.yandex.net/search/stable/yandsearch?origin=market-mbi-admin&text=")
        .build();

        // проверяем доступность сервиса и наш формат
        if(client.findFirst("Биробиджан, Пионерская, д. 66, строение лит.Б").isPresent()){
            // проверям переданную адресную строку
            if (args.length > 0) {
                client.findFirst(args[0]).ifPresent(System.out::print);
            }
        }
    }
}
