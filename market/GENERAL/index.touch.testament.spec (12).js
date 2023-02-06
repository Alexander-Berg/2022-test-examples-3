// @flow

import {waitFor} from '@testing-library/dom';
import {makeMirror} from '@self/platform/helpers/testament';

// page objects
import GalleryNewStoryItemPO from '@self/platform/components/Gallery/GalleryNewStoryItem/__pageObject';
import UgcMediaGalleryZeroStatePO from '@self/platform/components/Gallery/GalleryZeroState/__pageObject';
import UgcMediaGalleryModalPO from '@self/platform/components/Gallery/GalleryNewStoryItemPopup/__pageObject';

// mocks
import {
    reportProduct,
    schemaStateWithPhotos,
    schemaStateWithoutPhotos,
    widgetOptions,
    expectedPhotoLink,
    expectedVideoLink,
} from './mocks';

const getButtonLink = button => {
    if (button) {
        const anchors = button.getElementsByTagName('a');
        if (anchors && anchors.length > 0) {
            return anchors[0].href;
        }
    }
    return '';
};

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
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
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    kadavrLayer = mirror.getLayer('kadavr');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.backend.runCode((resolver, mock) => {
        jest.doMock(
            resolver,
            () => jest.fn().mockResolvedValue(mock)
        );
    }, [require.resolve('@self/platform/app/resource/social'), []]);

    await jestLayer.backend.runCode((resolver, mock) => {
        jest.doMock(
            resolver,
            () => jest.fn().mockResolvedValue(mock)
        );
    }, [require.resolve('@self/platform/app/resource/buker'), []]);

    await kadavrLayer.setState('report', reportProduct);
});

afterAll(() => mirror.destroy());

describe('Слайдер UGC медиа галереи с фото и видео.', () => {
    beforeEach(async () => {
        await makeContext();
        await kadavrLayer.setState('schema', schemaStateWithPhotos);
    });

    test('Открывается окно добавления медиа', async () => {
        const {container} = await apiaryLayer.mountWidget('../', widgetOptions);

        await step('Кликаем на кнопку добавления медиа', async () => {
            container.querySelector(GalleryNewStoryItemPO.root).click();
        });
        await step('Диалоговое окно добавления медиа должно быть видно', async () => {
            await waitFor(() => {
                expect(document.querySelector(UgcMediaGalleryModalPO.root)).toBeVisible();
            });
        });
    });

    test('"Добавить фото" ведет на корректную страницу', async () => {
        const {container} = await apiaryLayer.mountWidget('../', widgetOptions);

        await step('Кликаем на кнопку добавления медиа', async () => {
            container.querySelector(GalleryNewStoryItemPO.root).click();
        });

        await step('Открылось окно с кнопкой "добавить фото"', async () => {
            await waitFor(() => {
                expect(document.querySelector(UgcMediaGalleryModalPO.root)).toBeVisible();
                expect(document.querySelector(UgcMediaGalleryModalPO.addPhotoButton)).toBeVisible();
            });
        });

        const actualLink = getButtonLink(document.querySelector(UgcMediaGalleryModalPO.addPhotoButton));
        await step('Проверяем ссылку кнопки', async () => {
            await waitFor(() => {
                expect(actualLink).toBe(expectedPhotoLink);
            });
        });
    });

    test('"Добавить видео" ведет на корректную страницу', async () => {
        const {container} = await apiaryLayer.mountWidget('../', widgetOptions);

        await step('Кликаем на кнопку добавления медиа', async () => {
            container.querySelector(GalleryNewStoryItemPO.root).click();
        });

        await step('Открылось окно с кнопкой "добавить фото"', async () => {
            await waitFor(() => {
                expect(document.querySelector(UgcMediaGalleryModalPO.root)).toBeVisible();
                expect(document.querySelector(UgcMediaGalleryModalPO.addVideoButton)).toBeVisible();
            });
        });

        const actualLink = getButtonLink(document.querySelector(UgcMediaGalleryModalPO.addVideoButton));
        await step('Проверяем ссылку кнопки', async () => {
            await waitFor(() => {
                expect(actualLink).toBe(expectedVideoLink);
            });
        });
    });
});
describe('Слайдер UGC медиа галереи без фото и видео.', () => {
    beforeEach(async () => {
        await makeContext();
        await kadavrLayer.setState('schema', schemaStateWithoutPhotos);
    });
    test('При клике отображается окно добавления медиа', async () => {
        const {container} = await apiaryLayer.mountWidget('../', widgetOptions);

        await step('Кликаем на кнопку добавления медиа', async () => {
            container.querySelector(UgcMediaGalleryZeroStatePO.root).click();
        });

        await step('Диалоговое окно добавления медиа должно быть видно', async () => {
            await waitFor(() => {
                expect(document.querySelector(UgcMediaGalleryModalPO.root)).toBeVisible();
            });
        });
    });
});
