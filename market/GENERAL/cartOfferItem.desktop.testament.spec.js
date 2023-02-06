import {screen, fireEvent, waitFor} from '@testing-library/dom';

import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {buildUrl} from '@self/root/src/utils/router';

import {pricesValue} from '@self/root/src/entities/price';

import {
    baseMockFunctionality,
    mockFullCartPageStateWithSimpleOffer,
} from '../../__spec__/mocks/mockFunctionality';

import {
    visibleStrategyId,
    baseCartItem,
    offerId,
} from '../../__spec__/mocks/mockData';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

const widgetPath = '..';

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    // Убираем все слоты
    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/cart/CartDeliveryTermsNotifier'),
        () => ({create: () => Promise.resolve(null)})
    );
    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/cart/YandexGoCartEmpty'),
        () => ({create: () => Promise.resolve(null)})
    );
    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/m2b/M2BNoticeProductSearch'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.backend.runCode(baseMockFunctionality, []);
});

beforeEach(async () => {
    await mandrelLayer.initContext();
});

afterAll(() => mirror.destroy());

describe('Элемент корзины.', () => {
    describe('Один оффер в корзине, 1 штука.', () => {
        beforeEach(async () => {
            await jestLayer.backend.runCode(mockFullCartPageStateWithSimpleOffer, [{count: 1, availableCount: 3}]);
            await apiaryLayer.mountWidget(widgetPath, {props: {isEda: false, visibleStrategyId}});
        });

        it('Показывается корзинный оффер.', async () => {
            const cartOffer = screen.getByTestId('CartItem');

            expect(cartOffer).toBeVisible();
        });

        it('Отображается изображение.', async () => {
            const cartOfferPicture = screen.getByTestId('offerPicture');

            expect(cartOfferPicture).toBeVisible();
        });

        it('Отображает название и оно совпадает с названием из репорта.', async () => {
            const cartOfferName = screen.getByRole('link', {
                name: baseCartItem.name,
            });

            expect(cartOfferName.textContent).toEqual(baseCartItem.name);
        });

        it('Отображает название и оно является ссылкой на карточку КМ.', async () => {
            const cartOfferName = screen.getByRole('link', {
                name: baseCartItem.name,
            });

            const expetedUrl = buildUrl('market:product', {
                offerid: offerId,
                productId: baseCartItem.productId,
                sku: baseCartItem.marketSku,
                skuId: baseCartItem.marketSku,
                slug: baseCartItem.slug,
                track: 'cart',
            });

            expect(cartOfferName).toHaveAttribute('href', expect.stringMatching(expetedUrl));
        });

        it('Отображает цену и она совпадает с ценой из репорта.', async () => {
            const cartOfferName = screen.getByText(pricesValue(baseCartItem.price.value));

            expect(cartOfferName).toBeVisible();
        });


        it('При удалении элемент удаляется из корзины.', async () => {
            const cartOffer = screen.getByTestId('CartItem');
            const cartOfferRemoveButton = screen.getByRole('button', {name: 'Удалить'});
            fireEvent.click(cartOfferRemoveButton);

            await waitFor(() => {
                expect(cartOffer).not.toBeVisible();
            });
        });

        it('Кнопка уменьшить кол-во элементов не доступна', async () => {
            const cartOfferMinusButton = screen.getByRole('button', {
                name: /уменьшить/i,
            });

            expect(cartOfferMinusButton).toHaveAttribute('disabled');
        });

        it('При клике по кнопке плюс количество элементов увеличивается.', async () => {
            const offerAmountCounter = screen.getByTestId('offerAmountCounter');
            const cartOfferPlusButton = screen.getByRole('button', {
                name: /увеличить/i,
            });
            fireEvent.click(cartOfferPlusButton);

            await waitFor(() => {
                expect(offerAmountCounter.textContent).toEqual('2');
            });
        });

        it('При многократном клике по кнопке плюс количество в корзине увеличивается', async () => {
            const offerAmountCounter = screen.getByTestId('offerAmountCounter');
            const cartOfferPlusButton = screen.getByRole('button', {
                name: /увеличить/i,
            });
            fireEvent.click(cartOfferPlusButton);
            fireEvent.click(cartOfferPlusButton);

            await waitFor(() => {
                expect(offerAmountCounter.textContent).toEqual('3');
            });
        });

        it('Удаление имеет текст для незрячих.', async () => {
            const cartOfferRemoveButton = screen.getByRole('button', {name: 'Удалить'});

            expect(cartOfferRemoveButton).toHaveAccessibleName('Удалить');
        });

        it('Плюс имеет текст для незрячих.', async () => {
            const cartOfferPlusButton = screen.getByRole('button', {
                name: /увеличить/i,
            });

            expect(cartOfferPlusButton).toHaveAccessibleName('Увеличить');
        });

        it('Минус имеет текст для незрячих.', async () => {
            const cartOfferMinusButton = screen.getByRole('button', {
                name: /уменьшить/i,
            });

            expect(cartOfferMinusButton).toHaveAccessibleName('Уменьшить');
        });
    });

    describe('Один оффер в корзине, 3 штуки.', () => {
        beforeEach(async () => {
            await jestLayer.backend.runCode(mockFullCartPageStateWithSimpleOffer, [{count: 3, availableCount: 4}]);
            await apiaryLayer.mountWidget(widgetPath, {props: {isEda: false, visibleStrategyId}});
        });

        it('Кнопка уменьшить кол-во элементов доступна', async () => {
            const cartOfferMinusButton = screen.getByRole('button', {
                name: /уменьшить/i,
            });

            expect(cartOfferMinusButton).not.toHaveAttribute('disabled');
        });

        it('При клике по кнопке минус количество элементов уменьшается.', async () => {
            const offerAmountCounter = screen.getByTestId('offerAmountCounter');
            const cartOfferMinusButton = screen.getByRole('button', {
                name: /уменьшить/i,
            });
            fireEvent.click(cartOfferMinusButton);

            await waitFor(() => {
                expect(offerAmountCounter.textContent).toEqual('2');
            });
        });

        it('При многократном клике по кнопке минус количество в корзине уменьшается.', async () => {
            const offerAmountCounter = screen.getByTestId('offerAmountCounter');
            const cartOfferMinusButton = screen.getByRole('button', {
                name: /уменьшить/i,
            });
            fireEvent.click(cartOfferMinusButton);
            fireEvent.click(cartOfferMinusButton);

            await waitFor(() => {
                expect(offerAmountCounter.textContent).toEqual('1');
            });
        });

        it('При клике по кнопке плюс количество элементов увеличивается.', async () => {
            const offerAmountCounter = screen.getByTestId('offerAmountCounter');
            const cartOfferPlusButton = screen.getByRole('button', {
                name: /увеличить/i,
            });
            fireEvent.click(cartOfferPlusButton);

            await waitFor(() => {
                expect(offerAmountCounter.textContent).toEqual('4');
            });
        });
    });
});
