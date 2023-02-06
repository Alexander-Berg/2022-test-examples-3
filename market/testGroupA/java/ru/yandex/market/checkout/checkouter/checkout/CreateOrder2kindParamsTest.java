package ru.yandex.market.checkout.checkouter.checkout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateOrder2kindParamsTest extends AbstractWebTestBase {

    private static final String FILTER_KIND2_TYPE_NUMBER = "[ {  'id': '5085113',  'type': 'number',  'name': 'Размер" +
            " экрана',  'xslname': 'DimenScreen',  'subType': '',  'kind': 2,  'unit': '\"',  'position': 5,  " +
            "'noffers': 1,  'precision': 2,  'values': [  {  'max': '15.6',  'initialMax': '15.6',  'initialMin': '15" +
            ".6',  'min': '15.6',  'id': 'found'  }  ], 'marks': { 'specifiedForOffer': true } } ]";


    @SuppressWarnings("checkstyle:MethodLength")
    public static Stream<Arguments> parameterizedTestData() {
        ItemParameter itemParameterNumber = new ItemParameter();
        itemParameterNumber.setType("number");
        itemParameterNumber.setName("Размер экрана");
        itemParameterNumber.setSubType("");
        itemParameterNumber.setUnit("\"");
        itemParameterNumber.setValue("15.6");
        itemParameterNumber.setSpecifiedForOffer(true);
        List<ItemParameter> itemParameterNumberList = new ArrayList<>();
        itemParameterNumberList.add(itemParameterNumber);

        ItemParameter itemParameterBoolean = new ItemParameter();
        itemParameterBoolean.setType("boolean");
        itemParameterBoolean.setName("Wi-Fi");
        itemParameterBoolean.setSubType("");
        itemParameterBoolean.setValue("1");
        itemParameterBoolean.setSpecifiedForOffer(true);
        List<ItemParameter> itemParameterBooleanList = new ArrayList<>();
        itemParameterBooleanList.add(itemParameterBoolean);

        ItemParameter itemParameterEnum = new ItemParameter();
        itemParameterEnum.setType("enum");
        itemParameterEnum.setName("Количество ядер процессора");
        itemParameterEnum.setSubType("");
        itemParameterEnum.setValue("2");
        itemParameterEnum.setSpecifiedForOffer(true);
        List<ItemParameter> itemParameterEnumList = new ArrayList<>();
        itemParameterEnumList.add(itemParameterEnum);
        return Arrays.asList(new Object[][]{
                {
//                        checkouter-51: Создание заказа без параметров второго типа
//                        @link https://testpalm.yandex-team.ru/testcase/checkouter-51
                        "",
                        Color.BLUE,
                        null,
                        null
                },
                {

//                        checkouter-51: Создание заказа без параметров второго типа
//                        @link https://testpalm.yandex-team.ru/testcase/checkouter-51
                        "[  {  'id': '7893318',  'type': 'enum',  'name': 'Производитель',  'xslname': 'vendor',  " +
                                "'subType': '',  'kind': 1,  'position': 1,  'noffers': 1,  'values': [  {  " +
                                "'initialFound': 1,  'found': 1,  'value': 'HP',  'vendor': {  'name': 'HP',  " +
                                "'entity': 'vendor',  'id': 152722  },  'id': '152722'  }  ],  'valuesGroups': [  {  " +
                                "'type': 'all',  'valuesIds': [  '152722'  ]  }  ]  },  {  'id': '12782797',  'type':" +
                                " 'enum',  'name': 'Линейка',  'xslname': 'vendor_line',  'subType': '',  'kind': 1, " +
                                " 'position': 2,  'noffers': 1,  'values': [  {  'initialFound': 1,  'found': 1,  " +
                                "'value': 'ProBook',  'vendor': {  'name': 'HP',  'entity': 'vendor',  'id': 152722  " +
                                "},  'id': '13730030'  }  ],  'valuesGroups': [  {  'type': 'all',  'valuesIds': [  " +
                                "'13730030'  ]  }  ]  } ]",
                        Color.BLUE,
                        null,
                        new ArrayList<ItemParameter>()
                },
                {
//                      В данном кейсе найдено 0 параметров с обоими вариантами значений, такой параметр должен быть
//                      пропущен
                        "[ {  'id': '7911932',  'type': 'boolean',  'name': 'Ultrabook',  'xslname': 'Ultrabook',  " +
                                "'subType': '',  'kind': 2,  'position': 4,  'noffers': 1,  'values': [  {  " +
                                "'initialFound': 0,  'found': 0,  'value': '1',  'id': '1'  },  {  'initialFound': 0," +
                                "  'found': 0,  'value': '0',  'id': '0'  }  ]  }  ]",
                        Color.BLUE,
                        null,
                        new ArrayList<ItemParameter>()
                },
                {
//                        checkouter-52: Создание заказа с параметрами второго типа
//                        @link https://testpalm.yandex-team.ru/testcase/checkouter-52
//                        checkouter-53: Создание заказа с параметром второго типа с "type": "number"
//                        @link https://testpalm.yandex-team.ru/testcase/checkouter-53
                        FILTER_KIND2_TYPE_NUMBER,
                        Color.BLUE,
                        null,
                        itemParameterNumberList
                },
                {
//                        checkouter-54: Создание заказа с параметром второго типа с "type": "boolean"
//                        @link https://testpalm.yandex-team.ru/testcase/checkouter-54
                        "[ {  'id': '5085139',  'type': 'boolean',  'name': 'Wi-Fi',  'xslname': 'Home80211',  " +
                                "'subType': '',  'kind': 2,  'position': 21,  'noffers': 1,  'values': [  {  " +
                                "'initialFound': 1,  'found': 1,  'value': '1',  'id': '1'  },  {  'initialFound': 0," +
                                "  'found': 0,  'value': '0',  'id': '0'  }  ], 'marks': { 'specifiedForOffer': true " +
                                "}  } ]",
                        Color.BLUE,
                        null,
                        itemParameterBooleanList
                },
                {
//                        checkouter-55: Создание заказа с параметром второго типа с "type": "enum"
//                        @link https://testpalm.yandex-team.ru/testcase/checkouter-55
                        "[ {  'id': '6068613',  'type': 'enum',  'name': 'Количество ядер процессора',  'xslname': " +
                                "'ProcCoreNum',  'subType': '',  'kind': 2,  'position': 10,  'noffers': 1,  " +
                                "'values': [  {  'initialFound': 1,  'found': 1,  'value': '2',  'id': '12106928'  } " +
                                " ],  'valuesGroups': [  {  'type': 'all',  'valuesIds': [  '12106928'  ] } ], " +
                                "'marks': { 'specifiedForOffer': true }  }  ]",
                        Color.BLUE,
                        null,
                        itemParameterEnumList
                }, {
                //checkouter-59. Проверка размеров
                //@link https://testpalm.yandex-team.ru/testcase/checkouter-59
                "[{" +
                        "      'id': '14474261'," +
                        "      'type': 'enum'," +
                        "      'name': 'Обхват под грудью'," +
                        "      'xslname': 'girth_chest_down'," +
                        "      'subType': 'size'," +
                        "      'kind': 2," +
                        "      'position': 3," +
                        "      'noffers': 2," +
                        "      'defaultUnit': 'RU'," +
                        "       'marks': {" +
                        "        'specifiedForOffer': true" +
                        "      }," +
                        "      'units': [" +
                        "        {" +
                        "          'values': [" +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'EU'," +
                        "              'found': 2," +
                        "              'value': '60'," +
                        "              'id': '14497242'," +
                        "              'isShopValue': true" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'EU'," +
                        "              'found': 2," +
                        "              'value': '65'," +
                        "              'id': '14497250'," +
                        "              'isShopValue': true" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'EU'," +
                        "              'found': 2," +
                        "              'value': '70'," +
                        "              'id': '14497258'," +
                        "              'isShopValue': false" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'EU'," +
                        "              'found': 2," +
                        "              'value': '75'," +
                        "              'id': '14497266'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'EU'," +
                        "              'found': 2," +
                        "              'value': '80'," +
                        "              'id': '14497274'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'EU'," +
                        "              'found': 2," +
                        "              'value': '85'," +
                        "              'id': '14497282'" +
                        "            }" +
                        "          ]," +
                        "          'unitId': 'EU'," +
                        "          'id': '14497239'" +
                        "        }," +
                        "        {" +
                        "          'values': [" +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '60'," +
                        "              'id': '14496691'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '65'," +
                        "              'id': '14496699'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '70'," +
                        "              'id': '14496707'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '75'," +
                        "              'id': '14496715'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '80'," +
                        "              'id': '14496723'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '42'," +
                        "              'id': '14496820'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'INT'," +
                        "              'found': 2," +
                        "              'value': '85'," +
                        "              'id': '14496731'" +
                        "            }" +
                        "          ]," +
                        "          'unitId': 'INT'," +
                        "          'id': '14496688'" +
                        "        }," +
                        "        {" +
                        "          'values': [" +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'RU'," +
                        "              'found': 2," +
                        "              'value': '60'," +
                        "              'id': '14497134'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'RU'," +
                        "              'found': 2," +
                        "              'value': '65'," +
                        "              'id': '14497141'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'RU'," +
                        "              'found': 2," +
                        "              'value': '70'," +
                        "              'id': '14497148'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'RU'," +
                        "              'found': 2," +
                        "              'value': '75'," +
                        "              'id': '14497155'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'RU'," +
                        "              'found': 2," +
                        "              'value': '80'," +
                        "              'id': '14497162'" +
                        "            }," +
                        "            {" +
                        "              'initialFound': 2," +
                        "              'unit': 'RU'," +
                        "              'found': 2," +
                        "              'value': '85'," +
                        "              'id': '14497169'" +
                        "            }" +
                        "          ]," +
                        "          'unitId': 'RU'," +
                        "          'id': '14497127'" +
                        "        }" +
                        "      ]" +
                        "    }]",
                Color.BLUE,
                // переданные параметры игнорируются
                itemParameterEnumList,
                createItemParameterEnumListWithSizeUnit()
        }
        }).stream().map(Arguments::of);
    }

    private static List<ItemParameter> createItemParameterEnumListWithSizeUnit() {
        UnitValue euSize = new UnitValue();
        euSize.setUnitId("EU");
        euSize.setDefaultUnit(false);
        euSize.setValues(Arrays.asList("60", "65", "70", "75", "80", "85"));
        euSize.setShopValues(Arrays.asList("60", "65"));

        UnitValue intSize = new UnitValue();
        intSize.setUnitId("INT");
        intSize.setDefaultUnit(false);
        intSize.setValues(Arrays.asList("60", "65", "70", "75", "80", "42", "85"));

        UnitValue ruSize = new UnitValue();
        ruSize.setUnitId("RU");
        ruSize.setDefaultUnit(true);
        ruSize.setValues(Arrays.asList("60", "65", "70", "75", "80", "85"));

        ItemParameter parameter = new ItemParameter();
        parameter.setType("enum");
        parameter.setSubType("size");
        parameter.setName("Обхват под грудью");
        parameter.setSpecifiedForOffer(true);
        parameter.setUnits(Arrays.asList(euSize, intSize, ruSize));
        return Collections.singletonList(parameter);
    }


    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createOrder2kindParamsTest(String filterString,
                                           Color color,
                                           List<ItemParameter> kind2ParamsFromRequest,
                                           List<ItemParameter> expectedItemParameterList) throws Exception {
        Parameters parameters;
        if (color == Color.BLUE) {
            parameters = BlueParametersProvider.defaultBlueOrderParameters();
        } else {
            parameters = new Parameters();
            parameters.setColor(color);
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
        }
        parameters.getReportParameters().setReportFiltersValue(filterString);
        parameters.getOrder().getItems().forEach(oi -> {
            oi.setKind2Parameters(kind2ParamsFromRequest);
        });

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getId(), notNullValue());
        assertEquals(expectedItemParameterList,
                ((OrderItem) createdOrder.getItems().toArray()[0]).getKind2Parameters());
    }
}
