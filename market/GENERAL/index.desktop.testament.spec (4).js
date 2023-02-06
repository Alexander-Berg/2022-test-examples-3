// @flow

// flowlint-next-line untyped-import: off
import {getByText, findAllByText, waitFor} from '@testing-library/dom';
import {makeMirror} from '@self/platform/helpers/testament';

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
        require('./mock');
    }, []);
});

afterAll(() => {
    mirror.destroy();
});

describe('Виджет BrandsListByChar', () => {
    test('Клик на "Показать ещё" загружает список вендоров', async () => {
        await initContext();
        const {container} = await apiaryLayer.mountWidget('../', {
            hid: 123,
            char: 'a',
        });

        await expect(findAllByText(container, 'Vendor')).resolves.toHaveLength(200);
        getByText(container, 'Показать ещё').click();
        await waitFor(async () => {
            await expect(findAllByText(container, 'Vendor')).resolves.toHaveLength(400);
        });
    });
});
