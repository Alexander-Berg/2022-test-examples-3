import {screen} from '@testing-library/dom';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

const DEFAULT_NID = 555;
const DEFAULT_HID = 198119;
const PRODUCT_ID = 41224;

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
let kadavrLayer;

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await Promise.all([
        jestLayer.doMock(
            require.resolve('@self/platform/widgets/parts/AuthSuggestionPopup'),
            () => ({create: () => Promise.resolve(null)})
        ),
        jestLayer.doMock(
            require.resolve('@self/platform/widgets/parts/VendorPromoBadgePopup'),
            () => ({create: () => Promise.resolve(null)})
        ),
        jestLayer.doMock(
            require.resolve('@self/platform/widgets/parts/ProductCardLinksCompact'),
            () => ({create: () => Promise.resolve(null)})
        ),
        jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/ArLink'),
            () => ({create: () => Promise.resolve(null)})
        ),
    ]);
});

afterAll(() => {
    mirror.destroy();
});

async function initContext(exps = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    await mandrelLayer.initContext({
        request: {
            params: {skuId: '123'},
            cookie,
            abt: {expFlags: exps || {}},
        },
    });
}

describe('Галлерея на КМ.', () => {
    beforeEach(async () => {
        const category = {
            id: DEFAULT_NID,
            hid: DEFAULT_HID,
            nid: DEFAULT_NID,
        };
        const productWithPictureState = createProduct({
            id: PRODUCT_ID,
            filters: [],
            category,
            categoryIds: [DEFAULT_HID],
            pictures: [{
                original: {
                    url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/orig',
                },
                thumbnails: [{
                    containerWidth: 500,
                    containerHeight: 500,
                    url: '//avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/9hq',
                    width: 500,
                    height: 500,
                }],
            }],
        }, PRODUCT_ID);

        const pageState = {
            pageId: 'touch:product',
            offers: {filters: [], search: {total: 0}},
            reviewFilters: {},
            filtersState: {},
            filtersOrder: [],
            filterLists: {},
        };

        await jestLayer.backend.runCode(mock => {
            jest.spyOn(require('@self/platform/resolvers/productPage/resolvePageDefault'),
                'resolvePageDefault').mockResolvedValue(Promise.resolve(mock));

            jest.spyOn(require('@self/root/src/resolvers/wishlist'),
                'resolveWishlistItems').mockResolvedValue(Promise.resolve({collections: {}}));

            jest.spyOn(require('@self/platform/app/node_modules/modules/compass'),
                'getCompassInfo').mockResolvedValue(Promise.resolve({}));
        }, [{
            productDataNormalizedPromise: {
                sku: {},
                pageState: {
                    current: pageState,
                    applied: pageState,
                    confirmed: pageState,
                },
                productId: PRODUCT_ID,
                category: {[DEFAULT_HID]: category},
                filter: {},
                ...productWithPictureState.collections,
            },
        }]);
    });

    test('По умолчанию слайды галлереи отображаются с дефолтными классами', async () => {
        await initContext();
        await apiaryLayer.mountWidget('..');

        expect(screen.getByTestId('galleryPicture')).toBeVisible();
        expect(screen.getByTestId('galleryPicture').className).not.toContain('overlay');
    });

    /**
     * @expFlag touch_km_gallery-redesign
     * @ticket MARKETFRONT-97718
     * @start
     */
    test('В эксперименте слайды галлереи отображаются с нужными классами', async () => {
        const exps = {'touch_km_gallery-redesign': true};
        await initContext(exps);
        await apiaryLayer.mountWidget('..');

        expect(screen.getByTestId('galleryPicture')).toBeVisible();
        expect(screen.getByTestId('galleryPicture').className).toContain('overlay');
    });
    /**
     * @expFlag touch_km_gallery-redesign
     * @ticket MARKETFRONT-97718
     * @end
     */
});
