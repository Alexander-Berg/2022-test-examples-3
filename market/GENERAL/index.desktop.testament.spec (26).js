import dayjs from 'dayjs';
import {waitFor} from '@testing-library/dom';

import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import promoMock from '@self/root/src/spec/hermione/kadavr-mock/loyalty/promos';
import customParseFormat from 'dayjs/plugin/customParseFormat';

import CoinPO from '@self/root/src/uikit/components/Coin/__pageObject';
import BindBonusPO, {LegalLink as LegalLinkPO, PromoEnd as PromoEndPO} from '@self/root/src/components/BindBonus/__pageObject';
import {Button as ButtonPO} from '@self/root/src/uikit/components/Button/__pageObject';
import LinkPO from '@self/root/src/components/Link/__pageObject';
import ImagePO from '@self/root/src/uikit/components/Image/__pageObject';
import {DEFAULT_DATE_FORMAT} from '@self/root/src/constants/date';

dayjs.extend(customParseFormat);

const promoMockDateFormat = DEFAULT_DATE_FORMAT;
const promoTextDateFormat = 'DD.MM.YYYY';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

async function makeContext(user = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user,
        request: {
            cookie,
        },
    });
}

describe('Widget: BindBonus', () => {
    const widgetPath = '@self/root/src/widgets/content/BindBonus';

    const setLoyaltyState = async (promo = promoMock.default) => {
        await kadavrLayer.setState('Loyalty.collections.promos', [promo]);
    };

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirrorDesktop({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });

        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        await jestLayer.doMock(
            require.resolve('@self/root/src/resolvers/cms'),
            () => ({
                resolveDailyBonusesConfig: jest.fn().mockResolvedValue({result: {}}),
            })
        );
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Купон', () => {
        const promo = promoMock.default;
        const widgetOptions = {
            token: promo.id,
            source: promo.promoCode,
        };

        beforeEach(async () => {
            await setLoyaltyState();
            await makeContext({isAuth: true});
        });

        test('Купон должен быть видимым', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const coupon = container.querySelector(`${BindBonusPO.root} ${CoinPO.root}`);

            await waitFor(() => expect(coupon).toBeVisible());
        });

        test('Купон должен содержать корректный заголовок', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const couponTitle = container.querySelector(`${BindBonusPO.root} ${CoinPO.root} ${CoinPO.title}`);

            expect(couponTitle).toHaveTextContent(promo.title);
        });

        test('Купон должен отображаться с корректной картинкой', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const couponImg = container.querySelector(`${BindBonusPO.root} ${CoinPO.root} ${ImagePO.img}`);

            expect(couponImg).toHaveAttribute('src', expect.stringMatching(promo.images.standard));
        });
    });

    describe('Ссылка на условия акции', () => {
        const promo = promoMock.default;
        const widgetOptions = {
            token: promo.id,
            source: promo.promoCode,
        };

        beforeEach(async () => {
            await setLoyaltyState();
            await makeContext({isAuth: true});
        });

        test('Ссылка должна быть видима', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const couponLink = container.querySelector(`${BindBonusPO.root} ${LinkPO.root}`);

            await waitFor(() => expect(couponLink).toBeVisible());
        });

        test('Блок с ссылкой должен содержать корректный текст', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const couponLinkBlock = container.querySelector(`${BindBonusPO.root} ${LegalLinkPO.root}`);

            expect(couponLinkBlock).toHaveTextContent('Подробнее об условиях');
        });

        test('Ссылка должна содержать корректный текст', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const couponLink = container.querySelector(`${BindBonusPO.root} ${LinkPO.root}`);

            expect(couponLink).toHaveTextContent('условиях');
        });

        test('Ссылка должна содержать корректный путь', async () => {
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const couponLink = container.querySelector(`${BindBonusPO.root} ${LinkPO.root}`);

            expect(couponLink).toHaveAttribute('href', expect.stringMatching(promo.promoOfferAndAcceptance));
        });
    });

    describe('Дата сгорания', () => {
        beforeEach(async () => {
            await makeContext({isAuth: true});
        });

        describe('В будущем', () => {
            const promo = promoMock.active;
            const widgetOptions = {
                token: promo.id,
                source: promo.promoCode,
            };

            beforeEach(async () => {
                await setLoyaltyState(promo);
            });

            test('Блок должен содержать корректный текст', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const promoEnd = container.querySelector(`${BindBonusPO.root} ${PromoEndPO.root}`);

                const dateAsText = dayjs(promo.promoEndDate, promoMockDateFormat)
                    .format(promoTextDateFormat);

                expect(promoEnd).toHaveTextContent(dateAsText);
            });
        });

        describe('В прошлом', () => {
            const promo = promoMock.expired;
            const widgetOptions = {
                token: promo.id,
                source: promo.promoCode,
            };

            beforeEach(async () => {
                await setLoyaltyState(promo);
            });


            test('Блок должен содержать корректный текст', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const promoEnd = container.querySelector(`${BindBonusPO.root} ${PromoEndPO.root}`);

                expect(promoEnd).toHaveTextContent('Акция закончилась');
            });
        });
    });

    describe('Кнопка прикрепления купона', () => {
        describe('Авторизованный пользователь', () => {
            beforeEach(async () => {
                await makeContext({isAuth: true});
            });

            describe('По умолчанию', () => {
                beforeEach(async () => {
                    await setLoyaltyState();
                });

                const promo = promoMock.default;
                const widgetOptions = {
                    token: promo.id,
                    source: promo.promoCode,
                };

                test('Кнопка прикрепления купона должна быть видимой', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                    const couponButton = container.querySelector(`${BindBonusPO.root} ${ButtonPO.root}`);

                    await waitFor(() => expect(couponButton).toBeVisible());
                });

                test('Блок с ссылкой должен содержать корректный текст', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                    const couponButton = container.querySelector(`${BindBonusPO.root} ${ButtonPO.root}`);

                    expect(couponButton).toHaveTextContent('Получить купон');
                });
            });

            describe('Акция закончилась', () => {
                const promo = promoMock.expired;
                const widgetOptions = {
                    token: promo.id,
                    source: promo.promoCode,
                };

                beforeEach(async () => {
                    await setLoyaltyState(promo);
                });

                test('Кнопка прикрепления купона не должна быть видимой', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                    const couponButton = container.querySelector(`${BindBonusPO.root} ${ButtonPO.root}`);

                    await waitFor(() => expect(couponButton).not.toBeInTheDocument());
                });
            });
        });

        describe('Неавторизованный пользователь', () => {
            beforeEach(async () => {
                await makeContext({isAuth: false});
            });

            describe('По умолчанию', () => {
                beforeEach(async () => {
                    await setLoyaltyState();
                });

                const promo = promoMock.default;
                const widgetOptions = {
                    token: promo.id,
                    source: promo.promoCode,
                };

                test('Кнопка прикрепления купона должна быть видимой', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                    const couponButton = container.querySelector(`${BindBonusPO.root} ${ButtonPO.root}`);

                    await waitFor(() => expect(couponButton).toBeVisible());
                });

                test('Блок с ссылкой должен содержать корректный текст', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                    const couponButton = container.querySelector(`${BindBonusPO.root} ${ButtonPO.root}`);

                    expect(couponButton).toHaveTextContent('Войти и получить купон');
                });
            });

            describe('Акция закончилась', () => {
                const promo = promoMock.expired;
                const widgetOptions = {
                    token: promo.id,
                    source: promo.promoCode,
                };

                beforeEach(async () => {
                    await setLoyaltyState(promo);
                });

                test('Кнопка прикрепления купона не должна быть видимой', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                    const couponButton = container.querySelector(`${BindBonusPO.root} ${ButtonPO.root}`);

                    await waitFor(() => expect(couponButton).not.toBeInTheDocument());
                });
            });
        });
    });
});
