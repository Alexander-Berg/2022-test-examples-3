import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import AgeConfirmation from '@self/platform/spec/page-objects/widgets/parts/AgeConfirmation';
import GallerySlider from '@self/platform/spec/page-objects/components/Gallery/GallerySlider';

import {setCookies} from '@yandex-market/gemini-extended-actions';
import deleteCookie from '@yandex-market/gemini-extended-actions/actions/deleteCookie';
import {DEFAULT_COOKIES, setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {
    createOffer,
    createProduct,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';

import {offerMock, adultWarnings} from './mocks/offer.mock';

const catalogerMock = makeCatalogerTree('Название категории в Хлебных Крошках', 138608, 54726, {categoryType: 'guru'});

const mainProduct = {
    type: 'model',
    titles: {
        raw: 'Товар с промокодом',
    },
    offers: {
        items: [offerMock.wareId],
    },
    pictures: offerMock.pictures,
    isNew: true,
};

// Кадавр вернёт все доступные КМ - т.е. carouselProducts + mainProduct
const carouselProducts = [
    createProduct({
        titles: {raw: 'Товар без офферов'},
        pictures: offerMock.pictures,
    }),
    createProduct({
        titles: {raw: 'No offers'},
        pictures: offerMock.pictures,
    }),
];

export default {
    suiteName: 'OfferCard [KADAVR]',
    url: `/offer/${offerMock.wareId}`,
    childSuites: [
        {
            suiteName: 'OfferWithoutProduct',
            selector: MainSuite.selector,
            before(actions) {
                const customOfferMock = {
                    ...offerMock,
                    titles: {
                        raw: 'Оффер без привязки к КМ M357/OF2176/QQ1337/ASD1234/DF8103/MEM',
                    },
                    description: 'Замоканый оффер.\nКартинка может протухнуть.\nОффер без привязки к КМ',
                    // Нужно чтобы кадавр не пытался замокать эти значения рандомно
                    productId: null,
                    model: {
                        id: null,
                    },
                };

                createSession.call(actions);
                setState.call(actions, 'Cataloger.tree', catalogerMock);
                setState.call(actions, 'report', mergeState([
                    createProduct(mainProduct, 123),
                    ...carouselProducts,
                    createOffer(customOfferMock, customOfferMock.wareId),
                ]));

                setDefaultGeminiCookies(actions);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            capture(actions) {
                // Ждём появления картинки - чтобы получить падение если страница не появится
                actions.waitForElementToShow(GallerySlider.root, 3000);
            },
        },
        {
            suiteName: 'OfferWithProduct',
            selector: MainSuite.selector,
            before(actions) {
                const customOfferMock = {
                    ...offerMock,
                    titles: {
                        raw: 'Оффер с привязкой к КМ M357/OF2176/QQ1337/ASD1234/DF8103/MEM',
                    },
                    description: 'Замоканый оффер.\nКартинка может протухнуть.\nОффер с привязкой к КМ',
                    // Нужно для отображения карусели "С этим товаром смотрят"
                    productId: 123,
                    model: {
                        id: 123,
                    },
                };

                createSession.call(actions);
                setState.call(actions, 'Cataloger.tree', catalogerMock);
                setState.call(actions, 'report', mergeState([
                    createProduct(mainProduct, 123),
                    ...carouselProducts,
                    createOffer(customOfferMock, customOfferMock.wareId),
                ]));

                setDefaultGeminiCookies(actions);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            capture(actions) {
                // Ждём появления картинки - чтобы получить падение если страница не появится
                actions.waitForElementToShow(GallerySlider.root, 3000);
            },
        },
        {
            suiteName: 'AdultCardWarning',
            selector: MainSuite.selector,
            before(actions) {
                const customOfferMock = {
                    ...offerMock,
                    titles: {
                        raw: 'Этот оффер не должно быть видно. Должно быть предупреждение 18+',
                    },
                    description: 'Замоканый оффер.\nЭтот оффер не должно быть видно. Должно быть предупреждение 18+',
                    // Нужно для отображения карусели "С этим товаром смотрят"
                    productId: 123,
                    model: {
                        id: 123,
                    },
                };

                createSession.call(actions);
                setState.call(actions, 'Cataloger.tree', catalogerMock);
                setState.call(actions, 'report', mergeState([
                    createProduct(mainProduct, 123),
                    ...carouselProducts,
                    createOffer(customOfferMock, customOfferMock.wareId),
                    {
                        data: {
                            search: {
                                adult: true,
                            },
                        },
                    },
                ]));

                deleteCookie.call(actions, 'adult');
                setDefaultGeminiCookies(actions);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            capture(actions) {
                // Ждём появления картинки - чтобы получить падение если ворнинг не появится
                actions.waitForElementToShow(AgeConfirmation.root, 3000);
            },
        },
        {
            suiteName: 'AdultOfferCard',
            selector: MainSuite.selector,
            before(actions) {
                const customOfferMock = {
                    ...offerMock,
                    titles: {
                        raw: 'Оффер 18+. У пользователя стоит кука, оффер должен быть виден.',
                    },
                    description: 'Замоканый оффер.\nОффер 18+. У пользователя стоит кука, оффер должен быть виден.',
                    // Нужно для отображения карусели "С этим товаром смотрят"
                    productId: 123,
                    model: {
                        id: 123,
                    },
                    warnings: adultWarnings,
                };

                const adultMainProduct = {
                    ...mainProduct,
                    warnings: adultWarnings,
                };

                createSession.call(actions);
                setState.call(actions, 'Cataloger.tree', catalogerMock);
                setState.call(actions, 'report', mergeState([
                    createProduct(adultMainProduct, 123),
                    ...carouselProducts,
                    createOffer(customOfferMock, customOfferMock.wareId),
                    {
                        data: {
                            search: {
                                adult: true,
                            },
                        },
                    },
                ]));

                setCookies.setCookies.call(actions, [
                    ...DEFAULT_COOKIES,
                    {
                        name: 'adult',
                        value: '1:1:ADULT',
                    },
                ]);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            capture(actions) {
                // Ждём появления картинки - чтобы получить падение если страница не появится
                actions.waitForElementToShow(GallerySlider.root, 3000);
            },
        },
    ],
};
