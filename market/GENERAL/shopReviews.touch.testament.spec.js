import {screen} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';
import {
    shopInfo,
    shopReview,
    publicDisplayName,
    socialUserShopSchema,
    passportUserShopSchema,
} from '@self/root/src/spec/testament/review/mocks';

// путь к виджету который тестируем
const WIDGET_PATH = '@self/platform/widgets/pages/ShopReviewsPage';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext({pageId, requestParams}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
            params: requestParams,
        },
        page: {
            pageId,
        },
    });
}

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.runCode((
        AgitationScrollBox,
        UserExpertisePopup,
        ShopRatingWarning,
        ShopCard,
        ShopSummaryFactors,
        ShopReviewsSummary,
        ShopReviewsHeader
    ) => {
        jest.doMock(
            AgitationScrollBox,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            UserExpertisePopup,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopRatingWarning,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopCard,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopSummaryFactors,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopReviewsSummary,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopReviewsHeader,
            () => ({create: () => Promise.resolve(null)})
        );
    }, [
        require.resolve('@self/root/src/widgets/content/AgitationScrollBox'),
        require.resolve('@self/platform/widgets/content/UserExpertisePopup'),
        require.resolve('@self/platform/widgets/content/ShopRatingWarning'),
        require.resolve('@self/platform/widgets/content/ShopCard'),
        require.resolve('@self/platform/widgets/content/ShopSummaryFactors'),
        require.resolve('@self/platform/widgets/content/ShopReviewsSummary'),
        require.resolve('@self/platform/widgets/content/ShopReviewsHeader'),
    ]);
});

afterAll(() => {
    mirror.destroy();
});

const expectedUserName = publicDisplayName;

describe('Widget: ShopReviewsPage', () => {
    describe('Рекомендованные отзывы.', () => {
        beforeEach(async () => {
            await kadavrLayer.setState('report', shopInfo);
            await kadavrLayer.setState('ShopInfo.collections', {shopNames: []});
            await makeContext({
                pageId: 'touch:shop-reviews',
                requestParams: {
                    shopId: shopReview.shop.id,
                    slug: 'test-shop',
                },
            });
        });
        describe('Пользователь зарегистрирован через соцсеть.', () => {
            beforeEach(async () => {
                await kadavrLayer.setState('schema', socialUserShopSchema);
                await apiaryLayer.mountWidget(WIDGET_PATH);
            });
            describe('Имя пользователя в заголовке отзыва.', () => {
                it('По умолчанию должно быть корректным.', async () => {
                    const userName = screen.getByRole('button', {name: expectedUserName});
                    expect(userName.textContent).toEqual(expectedUserName);
                });
            });
        });
        describe('Отображаемое имя задано в паспорте.', () => {
            beforeEach(async () => {
                await kadavrLayer.setState('schema', passportUserShopSchema);
                await apiaryLayer.mountWidget(WIDGET_PATH);
            });
            describe('Имя пользователя в заголовке отзыва.', () => {
                it('По умолчанию должно быть корректным.', async () => {
                    const userName = screen.getByRole('button', {name: expectedUserName});
                    expect(userName.textContent).toEqual(expectedUserName);
                });
            });
        });
    });
});
