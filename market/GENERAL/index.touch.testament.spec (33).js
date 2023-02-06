import {fireEvent, screen, within} from '@testing-library/dom';

import {mockScrollBy, mockYaRum} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {
    userAddressId,
    checkoutCartId,
    jeweleryMockParams,
} from '@self/root/src/widgets/content/checkout/common/__spec__/mockData/jewelery';
import {
    defaultParams,
    dsbsKgtMockParams,
    dsbsMockParams,
} from '@self/root/src/widgets/content/checkout/common/__spec__/mockData/dsbs';
import {
    outletId,
    fashionMockParams,
} from '@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion';
import {baseMockFunctionality} from './mockFunctionality';
import {
    checkActiveTab,
    hasExpectedTitle,
    hasTryingInformer,
    hasExpectedWarning,
    hasSelectedAddress,
    containsUniqueOffers,
    hasWithoutTryingIcon,
    hasExpectedTryingText,
    checkLiftSelectedParams,
    hasSupplierDeliveryInfo,
    containsEstimatedOffers,
    hasTryingPresetInfoTouch,
    hasWithoutTryingPresetInfoTouch,
} from './testCases';
import {emitBrowserEventActualizeSuccess} from './utils';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext({cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

beforeAll(async () => {
    mockScrollBy();
    mockYaRum();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await makeContext({});
});

afterAll(() => {
    mirror.destroy();
});

const widgetPath = '@self/root/src/widgets/content/checkout/common/CheckoutParcel';

describe('CheckoutParcel', () => {
    // SKIPPED MARKETFRONT-96354
    // заменить на test.each при расскипе
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Должен содержать ожидаемые данные для DSBS офера с КГТ', () =>
        dsbsKgtMockParams.forEach(dsbsKgtMockParam =>
            test(dsbsKgtMockParam.message, async () => {
                const {
                    liftingType,
                    price,
                    prefix,
                } = dsbsKgtMockParam;
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/dsbs', {
                    ...defaultParams,
                    liftingType,
                }]);
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();
                await checkLiftSelectedParams(container, price, prefix);
            })
        )
    );
    // MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Должен содержать ожидаемые данные для офера с ювелиркой [marketfront-5078]', () => {
        let widgetContainer;
        beforeAll(async () => {
            await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/jewelery']);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {
                visibleCheckoutCartId: checkoutCartId,
            });
            await makeContext({});
            widgetContainer = container;
            emitBrowserEventActualizeSuccess();
        });
        const {
            parcelTitle,
            selectedAddress,
            delivery,
            warningText,
        } = jeweleryMockParams;
        test('содержит правильный заголовок', async () => {
            await hasExpectedTitle(widgetContainer, parcelTitle);
        });
        test('содержит выбранный адрес', async () => {
            await hasSelectedAddress(widgetContainer, selectedAddress);
        });
        test(`содержит правильную информацию о продавце ${delivery}`, async () => {
            await hasSupplierDeliveryInfo(widgetContainer, delivery);
        });
        test('содержит ожидаемое предупреждение', async () => {
            await hasExpectedWarning(widgetContainer, warningText);
        });
    });
    // MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Должен содержать ожидаемые данные для офера', () => {
        let widgetContainer;
        beforeAll(async () => {
            await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/dsbs']);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {
                visibleCheckoutCartId: checkoutCartId,
            });
            await makeContext({});
            widgetContainer = container;
            emitBrowserEventActualizeSuccess();
        });
        const {
            parcelTitle,
            selectedAddress,
            delivery,
            warningText,
        } = dsbsMockParams;
        test('содержит правильный заголовок', async () => {
            await hasExpectedTitle(widgetContainer, parcelTitle);
        });
        test('содержит выбранный адрес', async () => {
            await hasSelectedAddress(widgetContainer, selectedAddress);
        });
        test(`содержит правильную информацию о продавце ${delivery}`, async () => {
            await hasSupplierDeliveryInfo(widgetContainer, delivery);
        });
        test('содержит ожидаемое предупреждение', async () => {
            await hasExpectedWarning(widgetContainer, warningText);
        });
    });
    // MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Признак примерки у FBS товара', () => {
        const {
            withTryingText,
            withoutTryingText,
        } = fashionMockParams;
        test('с примеркой [marketfront-6145]', async () => {
            await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                isMarketBranded: true,
            }]);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {
                visibleCheckoutCartId: checkoutCartId,
            });
            await makeContext({});
            emitBrowserEventActualizeSuccess();

            await hasExpectedTryingText(container, withTryingText);
        });
        test('без примерки [marketfront-6146]', async () => {
            await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                deliveryType: 'DELIVERY',
                isTryingAvailable: false,
            }]);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {
                visibleCheckoutCartId: checkoutCartId,
            });
            await makeContext({});
            emitBrowserEventActualizeSuccess();

            await hasExpectedTryingText(container, withoutTryingText);
            await hasWithoutTryingIcon();
        });
    });

    describe('Информер примерки.', () => {
        describe('Нет сохранённых БПВЗ.', () => {
            test('FBY товар с примеркой. [marketfront-6147]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isFBS: false,
                    deliveryType: 'DELIVERY',
                    hasPickupPresets: false,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('delivery');

                await step('Информер примерки не отображается.', () => {
                    const tryingInformer = screen.queryByTestId('tryingInformer');
                    expect(tryingInformer).not.toBeInTheDocument();
                });

                await step('Перейти на таб "Самовывоз".', () => {
                    const pickup = screen.getByRole('radio', {name: /самовывоз/i});
                    fireEvent.click(pickup);
                });

                await hasTryingInformer();
            });
            test('FBS товар с примеркой [marketfront-6148]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isMarketBranded: false,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('pickup');

                await hasTryingInformer();

                await step('Перейти на таб "Курьер".', () => {
                    const courier = screen.getByRole('radio', {name: /курьер/i});
                    fireEvent.click(courier);
                    expect(courier).toBeChecked();
                });

                await hasTryingInformer();
            });
        });
        describe('Есть сохранённые БПВЗ.', () => {
            test('FBY товар с примеркой. [marketfront-6150]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isFBS: false,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('pickup');

                await step('Информер примерки не отображается.', () => {
                    const tryingInformer = screen.queryByTestId('tryingInformer');
                    expect(tryingInformer).not.toBeInTheDocument();
                });
            });
            test('FBS товар с примеркой. [marketfront-6151]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    deliveryType: 'DELIVERY',
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('delivery');

                await hasTryingInformer();

                await step('Перейти на таб "Самовывоз".', () => {
                    const pickup = screen.getByRole('radio', {name: /самовывоз/i});
                    fireEvent.click(pickup);
                });

                await step('Информер примерки не отображается.', () => {
                    const tryingInformer = screen.queryByTestId('tryingInformer');
                    expect(tryingInformer).not.toBeInTheDocument();
                });
            });
        });
        describe('Нет сохранённых БПВЗ, ПВЗ и постаматов.', () => {
            test('Фешн товар без примерки. [marketfront-6149]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    deliveryType: 'DELIVERY',
                    hasPickupPresets: false,
                    isTryingAvailable: false,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('delivery');

                await step('Информер примерки не отображается.', () => {
                    const tryingInformer = screen.queryByTestId('tryingInformer');
                    expect(tryingInformer).not.toBeInTheDocument();
                });

                await step('Перейти на таб "Самовывоз".', () => {
                    const pickup = screen.getByRole('radio', {name: /самовывоз/i});
                    fireEvent.click(pickup);
                });

                await step('Информер примерки не отображается.', () => {
                    const tryingInformer = screen.queryByTestId('tryingInformer');
                    expect(tryingInformer).not.toBeInTheDocument();
                });
            });
        });
    });

    describe('Признак примерки на пресете.', () => {
        describe('Есть сохранённые БПВЗ.', () => {
            test('FBY товар с примеркой. [marketfront-6152]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isFBS: false,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('pickup');
                await hasTryingPresetInfoTouch(outletId);
            });
            test('FBS товар с примеркой. [marketfront-6153]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isTryingAvailable: true,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('pickup');
                await hasTryingPresetInfoTouch(outletId);
            });
        });
        describe('Есть сохранённые БПВЗ и ПВЗ.', () => {
            test('Фешн товар без примерки. [marketfront-6154]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isTryingAvailable: false,
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('pickup');

                const popup = screen.queryByTestId('addressBottomDrawer');
                const address = within(popup).queryByTestId(outletId);
                const tryingInfo = within(address).queryByTestId('tryingInfo');
                const icon = within(address).queryByRole('img', {name: /без примерки/i});
                expect(tryingInfo).not.toBeInTheDocument();
                expect(icon).not.toBeInTheDocument();
            });
        });
        describe('Есть сохранённые адреса доставки курьером.', () => {
            test('FBS товар с примеркой. [marketfront-6156]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isTryingAvailable: true,
                    deliveryType: 'DELIVERY',
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});
                emitBrowserEventActualizeSuccess();

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('delivery');
                await hasWithoutTryingPresetInfoTouch(userAddressId);
            });
            test('FBY товар с примеркой. [marketfront-6155]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isFBS: false,
                    deliveryType: 'DELIVERY',
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('delivery');
                await hasTryingPresetInfoTouch(userAddressId);
            });
            test('Фешн товар без примерки. [marketfront-6157]', async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/fashion', {
                    isTryingAvailable: false,
                    deliveryType: 'DELIVERY',
                }]);
                await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });
                await makeContext({});

                await step('Нажать кнопку "Изменить" в блоке информации о доставке.', () => {
                    const btn = screen.getByRole('button', {name: /изменить/i});
                    fireEvent.click(btn);
                });

                await checkActiveTab('delivery');

                const popup = screen.queryByTestId('addressBottomDrawer');
                const address = within(popup).queryByTestId(userAddressId);
                const tryingInfo = within(address).queryByTestId('tryingInfo');
                const icon = within(address).queryByRole('img', {name: /без примерки/i});
                expect(tryingInfo).not.toBeInTheDocument();
                expect(icon).not.toBeInTheDocument();
            });
        });
    });

    describe('В посылке содержатся', () => {
        test('товары на заказ',
            async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, [
                    '@self/root/src/widgets/content/checkout/common/__spec__/mockData/uniqueOrder', {
                        isUniqueOrder: true,
                        isEstimatedOrder: true,
                    },
                ]);

                await makeContext({});
                emitBrowserEventActualizeSuccess();

                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });

                await containsUniqueOffers(container);
            }
        );

        test('товары с долгим сроком доставки ',
            async () => {
                await jestLayer.backend.runCode(baseMockFunctionality, [
                    '@self/root/src/widgets/content/checkout/common/__spec__/mockData/uniqueOrder', {
                        isEstimatedOrder: true,
                    },
                ]);

                await makeContext({});
                emitBrowserEventActualizeSuccess();

                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    visibleCheckoutCartId: checkoutCartId,
                });

                await containsEstimatedOffers(container);
            }
        );
    });
});
