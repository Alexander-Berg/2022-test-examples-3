// @flow
// flowlint-next-line untyped-import: off
import {waitFor, screen, getByTestId, queryByTestId, getByText, fireEvent} from '@testing-library/dom';
/* flowlint-next-line untyped-import: off */ /* eslint-disable-next-line import/order*/
import getImageIdFromUrlHelper from '@self/platform/spec/page-objects/helpers/getImageIdFromUrl';
import {makeMirror} from '@self/platform/helpers/testament';

import {
    WIDGET_PATH,
    GALLERY_PHOTO_TEST_ID,
    GALLERY_MODAL_TEST_ID,
    GALLERY_MODAL_PHOTO_TEST_ID,
    GALLERY_NAV_TEST_ID,
    GALLERY_CLOSE_TEST_ID,
    ACTIVE_THUMB_TEST_ID,
    NEXT_BUTTON_TEST_ID,
    IMAGE_GALLERY_MORE_TEST_ID,
    HYPEBADGE_LIST_TEST_ID,
    HYPE_BADGE_TYPES,
} from './constants';
import {productId} from './fixtures/productsInfo';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function initContext() {
    await mandrelLayer.initContext();
}

beforeAll(async () => {
    mirror = await makeMirror(__filename, jest);
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.runCode(() => {
        // flowlint-next-line untyped-import: off
        require('@self/project/src/spec/unit/mocks/yandex-market/mandrel/resolver');
        const {unsafeResource} = require('@yandex-market/mandrel/resolver');
        const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
        const topOffersForProduct = require('./fixtures/topOffersForProduct');
        const {mockIntersectionObserver, mockLocation, mockScrollBy} = require('@self/root/src/helpers/testament/mock');
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        const {productsInfo} = require('./fixtures/productsInfo');

        mockLocation();
        mockRouter();
        mockScrollBy();
        mockIntersectionObserver();

        jest.doMock('@self/root/src/entities/platform/utils', () => ({
            getPlatform: () => 'desktop',
            isApiPlatform: () => false,
        }));

        // $FlowFixMe
        unsafeResource.mockImplementation(
            createUnsafeResourceMockImplementation({
                'report.getProductsInfo': () => Promise.resolve(productsInfo),
                'report.getTopOffersForProduct': () => Promise.resolve(topOffersForProduct),
            })
        );
    }, []);
});

afterAll(() => mirror.destroy());
beforeEach(initContext);

const widgetOptions = {
    props: {productId},
};

describe('Бейджи', () => {
    beforeAll(async () => {
        const {productsInfo, productId} = require('./fixtures/productsInfoWithBadges');
        widgetOptions.props.productId = productId;

        await jestLayer.runCode(productsInfo => {
            const {unsafeResource} = require('@yandex-market/mandrel/resolver');
            const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
            const topOffersForProduct = require('./fixtures/topOffersForProduct');

            // $FlowFixMe
            unsafeResource.mockImplementation(
                createUnsafeResourceMockImplementation({
                    'report.getProductsInfo': () => Promise.resolve(productsInfo),
                    'report.getTopOffersForProduct': () => Promise.resolve(topOffersForProduct),
                })
            );
        }, [productsInfo]);
    });

    test('при наличии флага isNew отображается бейдж "новинка"', async () => {
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

        const galleryImage = getByTestId(container, GALLERY_PHOTO_TEST_ID);
        fireEvent.load(galleryImage);
        const hypeBadgeList = getByTestId(container, HYPEBADGE_LIST_TEST_ID);

        await step('Контейнер с бейджами должен загрузиться', async () => {
            expect(hypeBadgeList).not.toBeNull();
        });

        expect(getByText(hypeBadgeList, HYPE_BADGE_TYPES.NOVICE)).not.toBeNull();
    });

    test('при наличии флага isExclusive отображается бейдж "эксклюзив"', async () => {
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

        const galleryImage = getByTestId(container, GALLERY_PHOTO_TEST_ID);
        fireEvent.load(galleryImage);
        const hypeBadgeList = getByTestId(container, HYPEBADGE_LIST_TEST_ID);

        await step('Контейнер с бейджами должен загрузиться', async () => {
            expect(hypeBadgeList).not.toBeNull();
        });

        expect(getByText(hypeBadgeList, HYPE_BADGE_TYPES.EXCLUSIVE)).not.toBeNull();
    });
});

describe('При клике', () => {
    test('открывается большая галерея с сохранением активной миниатюры и изображения соответственно', async () => {
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
        const activeThumb = 1;

        await step(`Кликаем на миниатюру с индексом ${activeThumb}`, async () => {
            getByTestId(container, GALLERY_NAV_TEST_ID).children[activeThumb].click();
        });

        step(`Миниатюра с индексом ${activeThumb} стала активной`, () => {
            expect(getByTestId(container, GALLERY_NAV_TEST_ID).children[activeThumb]).toHaveClass('active');
        });

        await step('Открываем попап с галереей', async () => {
            await waitFor(() => {
                getByTestId(container, GALLERY_PHOTO_TEST_ID).click();
            });
        });

        await step('Попап с галереей должна быть виден', async () => {
            expect(screen.queryByTestId(GALLERY_MODAL_TEST_ID)).not.toBeNull();
        });

        await step(`Миниатюра с индексом ${activeThumb} активна в попапе`, async () => {
            const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
            expect(getByTestId(popup, GALLERY_NAV_TEST_ID).children[activeThumb]).toHaveClass('active');
        });

        await step('Изображение в попапе соответствует изображению в галерее', async () => {
            const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
            const popupImage = getByTestId(popup, GALLERY_MODAL_PHOTO_TEST_ID);
            const galleryImage = getByTestId(container, GALLERY_PHOTO_TEST_ID);

            expect(popupImage.src).toBe(galleryImage.src);
        });

        await step('Кликаем на иконку закрытия попапа', async () => {
            const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
            getByTestId(popup, GALLERY_CLOSE_TEST_ID).click();
        });

        await step('Попапа с галереей не должно быть видно', async () => {
            await waitFor(() => {
                expect(screen.queryByTestId(GALLERY_MODAL_TEST_ID)).toBeNull();
            });
        });
    });
});

describe('Переключения.', () => {
    test('При клике на вторую миниатюру меняется активная миниатюра и изображение соответственно', async () => {
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

        await step('Кликаем на миниатюру с индексом 1', async () => {
            getByTestId(container, GALLERY_NAV_TEST_ID).children[1].click();
        });

        step('Миниатюра с индексом 1 стала активной', () => {
            expect(getByTestId(container, GALLERY_NAV_TEST_ID).children[1]).toHaveClass('active');
        });

        await step('Изображение должно соответствовать миниатюре', async () => {
            await waitFor(() => {
                const thumbImg = getByTestId(container, ACTIVE_THUMB_TEST_ID).children[0];
                const galleryImage = getByTestId(container, GALLERY_PHOTO_TEST_ID);

                expect(getImageIdFromUrlHelper(thumbImg.src)).toBe(getImageIdFromUrlHelper(galleryImage.src));
            });
        });
    });
});

describe('Большая галерея.', () => {
    test('При использовании миниатюр и стрелок ' +
        'изображение и соответствующая активная миниатюра меняются', async () => {
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
        const activeThumbIndex = 1;
        const nextThumbIndex = activeThumbIndex + 1;

        await step('Открываем попап с галереей', async () => {
            getByTestId(container, GALLERY_PHOTO_TEST_ID).click();
        });

        await step(`Кликаем на миниатюру с индексом ${activeThumbIndex} в попапе`, async () => {
            const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
            getByTestId(popup, GALLERY_NAV_TEST_ID).children[activeThumbIndex].click();
        });

        await step('Изображение должно соответствовать миниатюре', async () => {
            await waitFor(() => {
                const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
                const popupImage = getByTestId(popup, GALLERY_MODAL_PHOTO_TEST_ID);
                const thumbImg = getByTestId(popup, GALLERY_NAV_TEST_ID).children[activeThumbIndex].children[0];

                expect(getImageIdFromUrlHelper(thumbImg.src)).toBe(getImageIdFromUrlHelper(popupImage.src));
            });
        });

        await step('Кликаем на стрелку вниз', async () => {
            const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
            getByTestId(popup, NEXT_BUTTON_TEST_ID).click();
        });

        await step(`Изображение должно соответствовать миниатюре с индексом ${nextThumbIndex}`, async () => {
            await waitFor(() => {
                const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
                const galleryImage = getByTestId(popup, GALLERY_MODAL_PHOTO_TEST_ID);
                const thumbImg = getByTestId(popup, GALLERY_NAV_TEST_ID).children[nextThumbIndex].children[0];

                expect(getImageIdFromUrlHelper(thumbImg.src)).toBe(getImageIdFromUrlHelper(galleryImage.src));
            });
        });

        await step('Закрываем попап', async () => {
            const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
            getByTestId(popup, GALLERY_CLOSE_TEST_ID).click();
        });

        await step('Изображение и активная миниатюра должны сохраняться', async () => {
            const thumbImg = getByTestId(container, GALLERY_NAV_TEST_ID).children[nextThumbIndex].children[0];
            const galleryImage = getByTestId(container, GALLERY_PHOTO_TEST_ID);

            expect(getByTestId(container, GALLERY_NAV_TEST_ID).children[nextThumbIndex]).toHaveClass('active');
            expect(getImageIdFromUrlHelper(thumbImg.src)).toBe(getImageIdFromUrlHelper(galleryImage.src));
        });
    });
});

describe('Ссылка Еще N.', () => {
    describe('5 и меньше изображений', function () {
        test('Не отображается', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
            expect(queryByTestId(container, IMAGE_GALLERY_MORE_TEST_ID)).toBeNull();
        });
    });

    describe('Больше 5 изображений', () => {
        const widgetOptions = {
            props: {},
        };

        beforeAll(async () => {
            const {productsInfo, productId} = require('./fixtures/productsInfoWithMoreButton');
            widgetOptions.props.productId = productId;

            await jestLayer.runCode(productsInfo => {
                const {unsafeResource} = require('@yandex-market/mandrel/resolver');
                const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
                const topOffersForProduct = require('./fixtures/topOffersForProduct');

                // $FlowFixMe
                unsafeResource.mockImplementation(
                    createUnsafeResourceMockImplementation({
                        'report.getProductsInfo': () => Promise.resolve(productsInfo),
                        'report.getTopOffersForProduct': () => Promise.resolve(topOffersForProduct),
                    })
                );
            }, [productsInfo]);
        });

        test('При клике открывается большая галерея, первая миниатюра активна и соответствующая картинка', async () => {
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

            await step('Кликаем на ссылку еще', async () => {
                getByTestId(container, IMAGE_GALLERY_MORE_TEST_ID).click();
            });

            await step('Попап с галереей должна быть виден', async () => {
                await waitFor(() => {
                    expect(screen.queryByTestId(GALLERY_MODAL_TEST_ID)).not.toBeNull();
                });
            });

            await step('Миниатюра с индексом 0 активна в попапе', async () => {
                const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
                expect(getByTestId(popup, GALLERY_NAV_TEST_ID).children[0]).toHaveClass('active');
            });

            await step('Изображение должно соответствовать миниатюре с индексом 0', async () => {
                await waitFor(() => {
                    const popup = screen.getByTestId(GALLERY_MODAL_TEST_ID);
                    const galleryImage = getByTestId(popup, GALLERY_MODAL_PHOTO_TEST_ID);
                    const thumbImg = getByTestId(popup, GALLERY_NAV_TEST_ID).children[0].children[0];

                    expect(getImageIdFromUrlHelper(thumbImg.src)).toBe(getImageIdFromUrlHelper(galleryImage.src));
                });
            });
        });
    });
});
