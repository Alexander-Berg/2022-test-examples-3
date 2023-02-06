import {makeMirror} from '@self/platform/helpers/testament';
import {TITLE, ID, SEO, SEO_URL, IMAGE_URL} from './__mock__/product';

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


    await jestLayer.backend.doMock(
        require.resolve('@self/platform/resolvers/productPage/resolvePageDefault'),
        () => {
            const {PRODUCT, SEO} = require('./__mock__/product');
            return {
                __esModule: true,
                resolvePageDefault: () => Promise.resolve({
                    productPromise: Promise.resolve(PRODUCT),
                    seoUrlsPromise: Promise.resolve(SEO),
                }),
            };
        }
    );

    await makeContext({pageId: 'market:product-reviews-add', requestParams: {productId: ID}});
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: ProductReviewsNewPage', () => {
    it('SEO-джобы отдают нужные данные', async () => {
        const {
            title,
            metaDescription,
            metaTitle,
            metaCanonical,
            metaAlternateLang,
            metaImages,
        } = await jestLayer.backend.runCode(async () => {
            const {props} = require('@yandex-market/promise-helpers');
            const controllerModule = require('../controller').default;
            // eslint-disable-next-line
            const ctx = getBackend('mandrel')?.getContext();

            const {jobs} = controllerModule(ctx);
            const title = jobs.find(j => j.name === 'title');
            const meta = jobs.find(j => j.name === 'meta');

            return props({
                title: title.payload,
                metaTitle: meta.payload.title,
                metaDescription: meta.payload.description,
                metaCanonical: meta.payload.canonical,
                metaAlternateLang: meta.payload.alternateLang,
                metaImages: meta.payload.images,
            });
        }, []);

        expect(title)
            .toEqual('Добавление отзывов — Яндекс Маркет');
        expect(metaTitle)
            .toEqual('Добавление отзывов — Яндекс Маркет');
        expect(metaDescription)
            .toEqual(`Страница добавления отзывов на ${TITLE}. ` +
                'Вы можете оценить товар по нескольким характеристикам, а также поставить общий балл.');
        expect(metaCanonical).toEqual(SEO_URL);
        expect(metaAlternateLang).toEqual(SEO.alternateLang);
        expect(metaImages).toEqual([IMAGE_URL]);
    });
});
