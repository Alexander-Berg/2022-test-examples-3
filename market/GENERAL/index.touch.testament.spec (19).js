import {makeMirror} from '@self/platform/helpers/testament';

let mirror;
let jestLayer;
let mandrelLayer;
let kadavrLayer;

async function makeContext({pageId, requestParams}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
            params: requestParams,
        },
        route: {name: pageId},
    });
}

const ID = '1';
const NAME = 'Тест';
const SLUG = 'test';

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.runCode((
        CmsLayout,
        ShopCatalog,
        ShopJurInfo,
        ShopWarranty,
        AgitationScrollBox,
        shopPage,
        shopsInfo,
        shops
    ) => {
        const ID = '1';
        const NAME = 'Тест';

        jest.doMock(
            CmsLayout,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopCatalog,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopJurInfo,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            ShopWarranty,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            AgitationScrollBox,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            shopPage,
            () => ({resolveShopPage: () => Promise.resolve(null)})
        );
        jest.doMock(
            shopsInfo,
            () => ({
                getShopInfoWithBrand: () => Promise.resolve({
                    id: ID,
                    shopBrandName: NAME,
                }),
            })
        );
        jest.doMock(
            shops,
            () => ({
                resolveShopsInfo: () => Promise.resolve({
                    result: [Number(ID)],
                    collections: {
                        shop: {
                            [ID]: {
                                id: ID,
                                shopBrandName: NAME,
                            },
                        },
                    },
                }),
            })
        );
    }, [
        require.resolve('@self/platform/widgets/layouts/CmsLayout'),
        require.resolve('@self/platform/widgets/content/ShopCatalog'),
        require.resolve('@self/platform/widgets/content/ShopJurInfo'),
        require.resolve('@self/platform/widgets/content/ShopWarranty'),
        require.resolve('@self/root/src/widgets/content/AgitationScrollBox'),
        require.resolve('@self/platform/resolvers/cms'),
        require.resolve('@self/platform/resolvers/shops/info'),
        require.resolve('@self/project/src/resolvers/shop'),
    ]);

    await makeContext({pageId: 'touch:shop', requestParams: {shopId: '1', slug: 'shop'}});
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: ShopPage', () => {
    it('SEO джобы отдают нужные данные', async () => {
        const {title, metaTitle, metaDescription, metaCanonical} = await jestLayer.backend.runCode(async () => {
            const {props} = require('@yandex-market/promise-helpers');

            const controllerModule = require('../controller').default;
            // eslint-disable-next-line
            const ctx = getBackend('mandrel')?.getContext();
            const controller = controllerModule(ctx);

            const meta = controller.jobs.find(j => j.name === 'meta');
            const title = controller.jobs.find(j => j.name === 'title');

            return props({
                title: title.payload,
                metaTitle: meta.payload.title,
                metaDescription: meta.payload.description,
                metaCanonical: meta.payload.canonical,
            });
        }, []);

        expect(title)
            .toBe(`${NAME} — информация об интернет-магазине — Яндекс Маркет`);
        expect(metaTitle)
            .toBe(`${NAME} — информация об интернет-магазине — Яндекс Маркет`);
        expect(metaDescription)
            .toBe(`Информация об интернет-магазине ${NAME} на Яндекс Маркете: ` +
                'контактные данные, юридическая информация. ' +
                `Каталог товаров ${NAME}: популярные товары и категории, товары со скидками. ` +
                'Способы получения товаров, сроки и стоимость доставки. ' +
                `Рейтинг магазина ${NAME} и отзывы о нём на Яндекс Маркете.`);
        expect(metaCanonical)
            .toBe(`https://market.yandex.ru/shop--${SLUG}/${ID}`);
    });
});
