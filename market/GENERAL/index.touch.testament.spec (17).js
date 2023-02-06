import {makeMirror} from '@self/platform/helpers/testament';
import {
    INDEX_PAGE_TITLE,
    INDEX_PAGE_SHORT_TITLE,
    INDEX_PAGE_DESCRIPTION,
} from '@self/root/src/constants/meta';

let mirror;
let jestLayer;
let mandrelLayer;
let kadavrLayer;

async function makeContext({pageId}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
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

    await jestLayer.runCode((
        CmsLayout,
        BubbleCategories,
        Similar,
        DynamicPageParams,
        FashionOnboardingPopup,
        MboLayout
    ) => {
        jest.doMock(
            CmsLayout,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            BubbleCategories,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            Similar,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            DynamicPageParams,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            FashionOnboardingPopup,
            () => ({create: () => Promise.resolve(null)})
        );
        jest.doMock(
            MboLayout,
            () => ({create: () => Promise.resolve(null)})
        );
    }, [
        require.resolve('@self/platform/widgets/layouts/CmsLayout'),
        require.resolve('@self/platform/widgets/parts/BubbleCategories'),
        require.resolve('@self/root/src/widgets/content/Similar'),
        require.resolve('@self/root/src/widgets/content/DynamicPageParams'),
        require.resolve('@self/root/src/widgets/content/FashionOnboardingPopup'),
        require.resolve('@self/platform/widgets/layouts/MboLayout'),
    ]);

    await makeContext({pageId: 'touch:index'});
});

afterAll(() => {
    mirror.destroy();
});

// SKIPPED MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: IndexPage', () => {
    it('SEO джобы отдают нужные данные', async () => {
        const {title, metaTitle, metaDescription, metaCanonical} = await jestLayer.backend.runCode(async () => {
            const {props} = require('@yandex-market/promise-helpers');

            const controllerModule = require('../controller').default;
            const inputOptions = {};
            // eslint-disable-next-line
            const ctx = getBackend('mandrel')?.getContext();
            const controller = controllerModule(ctx, inputOptions);

            const meta = controller.jobs.find(j => j.name === 'meta');
            const title = controller.jobs.find(j => j.name === 'title');

            return props({
                title: title.payload,
                metaTitle: meta.payload.title,
                metaDescription: meta.payload.description,
                metaCanonical: meta.payload.canonical,
            });
        }, []);

        expect(title).toBe(INDEX_PAGE_TITLE);
        expect(metaTitle).toBe(INDEX_PAGE_SHORT_TITLE);
        expect(metaDescription).toBe(INDEX_PAGE_DESCRIPTION);
        expect(metaCanonical).toBe('http://market.yandex.ru/');
    });
});
