package ru.yandex.market.checkout.test.providers;

import java.util.List;

import ru.yandex.market.common.report.model.json.common.Region;
import ru.yandex.market.common.report.model.json.common.delivery.DeliveryLingua;

import static java.util.Arrays.asList;

public class RegionProvider {

    private static final int MOSCOW_REGION_ID = 213;
    private static final int NOVOSIBIRSK_REGION_ID = 65;

    private RegionProvider() {

    }

    public static Region getMoscowRegion() {
        return new Region() {{
            setId(MOSCOW_REGION_ID);
            setName("Москва");
            setLingua(new DeliveryLingua() {{
                setName(new Name() {{
                    setGenitive("Москвы");
                    setPreposition("в");
                    setPrepositional("Москве");
                }});
            }});
        }};
    }

    public static List<Region> getManufacturerCountries() {
        return asList(
                getMoscowRegion(),
                new Region() {{
                    setId(NOVOSIBIRSK_REGION_ID);
                    setName("Новосибирск");
                }}
        );
    }
}
