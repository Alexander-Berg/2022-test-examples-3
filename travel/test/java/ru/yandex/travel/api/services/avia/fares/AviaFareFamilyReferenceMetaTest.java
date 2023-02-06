package ru.yandex.travel.api.services.avia.fares;

public class AviaFareFamilyReferenceMetaTest extends AviaFareFamilyReferenceTest {
    /**
     * используем файл тарифной сетки из мета-поиска (travel/avia/ticket_daemon_api/data)
     * для того чтобы копирование из меты в BoY проходило безболезненно RASPTICKETS-21469
     */
    @Override
    protected AviaFareFamilyProperties TestResources() {
        return new AviaFareFamilyProperties()
                .setFareFamiliesFile("avia/fare_families_samples/aeroflot/SU_ff_v2_test.json")
                .setExternalExpressionsFile("fare_families_expressions/26_aeroflot.xml");
    }
}
