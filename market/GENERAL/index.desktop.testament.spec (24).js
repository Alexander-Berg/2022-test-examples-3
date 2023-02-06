
import {getNodeText, screen} from '@testing-library/dom';
import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror.js';
import {buildFormattedPriceOnly} from '@self/root/src/entities/price';

import {MOCK, PRICE} from './mocks';

const WIDGET_PATH = '@self/project/src/widgets/content/OfferSet';
const WIDGET_PROPS = {
    offerId: 1,
    cpc: 1,
    isMultiPromoComplectExp: true,
};

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

const makeContext = async () => mandrelLayer.initContext({});

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    jestLayer = mirror.getLayer('jest');
    apiaryLayer = mirror.getLayer('apiary');

    await Promise.all([
        jest.mock('@self/project/src/resolvers/experiments', () => ({
            resolveExpFlagValueSync: jest.fn().mockResolvedValue(undefined),
        })),
        jestLayer.backend.runCode(mock => {
            jest.spyOn(require('@self/root/src/resolvers/s3mds/resolveConfigBlueSetExp'), 'resolveConfigBlueSetExp').mockResolvedValue(mock);
        }, [MOCK.resolveConfigBlueSetExp]),
        jest.mock('@self/project/src/legacy/resolvers/cartItems', () => ({
            resolveCartItemsWidget: jest.fn().mockResolvedValue({result: [], collections: {cartItem: {}}}),
        })),
        jestLayer.backend.runCode(mock => {
            jest.spyOn(require('@self/project/src/resolvers/offer/resolveOffersByIds'), 'resolveOffersByIds').mockResolvedValue(mock);
        }, [MOCK.resolveOffersByIds]),
    ]);
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: OfferSet', () => {
    beforeEach(async () => {
        await makeContext();
    });

    describe('Кнопка "Комплект в корзину"', () => {
        test('показывает правильный текст', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PROPS);

            expect(screen.queryAllByRole('button', {name: 'Комплект в корзину'})[0]).not.toBeNull();
        });
    });

    const selectTotalPrice = () => getNodeText(screen.queryAllByRole('total-price')[0].querySelector('h2'));

    describe('Заголовок', () => {
        test('показывает корректную цену', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PROPS);

            const titlePrice = buildFormattedPriceOnly(PRICE);

            // Берем массив тк, изза отсутствия css, тестамент рендерит два варианта комплекта: мини и дефолт
            // функционально они абсолютно похожи поэтому берем любой.
            expect(selectTotalPrice()).toBe(titlePrice);
        });
    });

    describe('Суммарная цена', () => {
        test('равна сумме комплектующих', async () => {
            await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_PROPS);

            const selectSnippet = snippet =>
                Number(getNodeText(
                    snippet
                        .querySelector('.currentPrice > span'))
                    .replace(/\s/g, '')
                );

            const snippets = screen.queryAllByRole('snippet');
            const summaryPrice = Number(selectTotalPrice().replace(/\s/g, ''));
            const snippet1 = selectSnippet(snippets[0]);
            const snippet2 = selectSnippet(snippets[1]);

            expect(summaryPrice).toBe(snippet1 + snippet2);
        });
    });
});
