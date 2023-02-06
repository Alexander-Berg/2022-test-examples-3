// @flow

// flowlint-next-line untyped-import: off
import {screen, within} from '@testing-library/dom';
// flowlint-next-line untyped-import: off
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {makeMirror} from '@self/platform/helpers/testament';

import {NBSP, SMALL_SPACE_CHAR} from '@self/root/src/constants/string';
// flowlint-next-line untyped-import: off
import SearchSnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';

// mocks
// flowlint-next-line untyped-import: off
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';
import {createWishlistItem} from './mocks';

// путь к виджету который тестируем
const WIDGET_PATH = '../';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext() {
    return mandrelLayer.initContext({
        request: {
            cookie: {
                kadavr_session_id: await kadavrLayer.getSessionId(),
            },
        },
    });
}

beforeAll(async () => {
    mirror = await makeMirror({
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
});

afterAll(() => mirror.destroy());

describe('Вишлист.', () => {
    const setReportState = async offerData => {
        const state = mergeState([
            createOffer(offerData, offerData.id),
            {
                data: {
                    search: {
                        total: 1,
                        totalOffers: 1,
                    },
                },
            },
        ]);

        await kadavrLayer.setState('report', state);
        await kadavrLayer.setState('persBasket', {
            ...createWishlistItem(offerData.id),
            hasMore: false,
            token: 'token',
        });
    };

    beforeEach(async () => {
        await makeContext();
    });

    describe('Выдача. Гридовый список сниппетов.', () => {
        describe('DSBS-оффер. Снипет DSBS товара', () => {
            beforeEach(async () => {
                await setReportState(offerDSBSMock);
            });

            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            test.skip('Должен отображаться', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                expect(container.querySelector(SearchSnippetCell.root)).toBeVisible();
            });

            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            test.skip('Должен отображать заголовок с ожидаемым текстом', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const title = container.querySelector(SearchSnippetCell.title);

                expect(title.textContent).toContain(offerDSBSMock.titles.raw);
            });

            test('Должен отображать ожидаемую цену', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const price = container.querySelector(SearchSnippetCell.mainPrice);

                expect(price.textContent.replace(/\D/g, '')).toContain(offerDSBSMock.prices.value);
            });

            test('Фото должно быть видно', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);

                const article = screen.getByRole('article');
                const img = within(article).getByRole('img');

                expect(img).toBeVisible();
            });

            test('Название товара должно содержать ожидаемый текст', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);

                const shopInfo = screen.getByRole('link', {name: offerDSBSMock.shop.name});

                expect(shopInfo.textContent).toContain(offerDSBSMock.shop.name);
            });

            test('Текст о количестве отзывов должен содержать ожидаемый текст', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);

                const reviews = screen.getByRole('link', {name: `3${SMALL_SPACE_CHAR}219${NBSP}отзывов`});

                expect(reviews.textContent).toContain(`3${SMALL_SPACE_CHAR}219${NBSP}отзывов`);
            });

            test('Текст о доставке должен содержать "доставит продавец"', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const deliveryText = container.querySelector('[data-autotest-id="text"]');

                expect(deliveryText.textContent).toContain('доставит продавец');
            });

            test('Заголовок должен содержать верную ссылку и атрибут target', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const titleTargetAttribute = container.querySelector(SearchSnippetCell.titleLink).getAttribute('target');
                const titleHref = container.querySelector(SearchSnippetCell.titleLink).getAttribute('href');

                expect(titleTargetAttribute).toBe('_blank');
                expect(titleHref).toContain(offerDSBSMock.urls.offercard);
            });

            test('Цена должна содержать верную ссылку и атрибут target', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const priceTargetAttribute = container.querySelector(SearchSnippetCell.mainPrice).getAttribute('target');
                const priceHref = container.querySelector(SearchSnippetCell.mainPrice).getAttribute('href');

                expect(priceTargetAttribute).toBe('_blank');
                expect(priceHref).toContain(offerDSBSMock.urls.offercard);
            });

            test('Название магазина должно содержать верную ссылку и атрибут target', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);

                const shopInfo = screen.getByRole('link', {name: offerDSBSMock.shop.name});
                const shopInfoTargetAttribute = shopInfo.getAttribute('target');
                const shopInfoHref = shopInfo.getAttribute('href');

                expect(shopInfoTargetAttribute).toBe('_blank');
                expect(shopInfoHref).toContain(offerDSBSMock.urls.offercard);
            });

            test('Картинка должна содержать верную ссылку и атрибут target', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);

                const article = screen.getByRole('article');
                const img = within(article).getByRole('img');
                const link = img.parentElement;

                const linkTargetAttribute = link.getAttribute('target');
                const linkHref = link.getAttribute('href');

                expect(linkTargetAttribute).toBe('_blank');
                expect(linkHref).toContain(offerDSBSMock.urls.offercard);
            });
        });
    });
});
