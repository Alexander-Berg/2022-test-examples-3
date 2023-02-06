package ru.yandex.travel.api.services.avia.fares;

public class AviaAeroflotFareFamilyReferenceMetaTest extends AviaAeroflotFareFamilyReferenceTest {
    /**
     * используем файл тарифной сетки из мета-поиска (travel/avia/ticket_daemon_api/data)
     * для того чтобы копирование из меты в BoY проходило безболезненно RASPTICKETS-21469
     */
    @Override
    protected AviaFareFamilyProperties Properties() {
        return new AviaFareFamilyProperties()
                .setFareFamiliesFile("fare_families/26_aeroflot.json")
                .setExternalExpressionsFile("fare_families_expressions/26_aeroflot.xml");
    }
}
