import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {REGISTRATION_STATUS, REGISTRATION_MESSAGE} from './__mocks__/constants.mock';
import {ERROR_MESSAGE_TEST_ID} from '../constants';

/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {Mirror} */
let mirror;

const WIDGET_PATH = require.resolve('@self/root/src/widgets/content/m2b/M2BFastRegistrationInProcessPopup');

async function makeContext() {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            params: {},
            cookie,
        },
    });
}

describe('Widget: M2BFastRegistrationInProcessPopup.', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirrorDesktop({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');
        kadavrLayer = mirror.getLayer('kadavr');
        mandrelLayer = mirror.getLayer('mandrel');

        await jestLayer.backend.runCode((resolveBusinessUserInProgressStatus, mock) => {
            jest.doMock(
                resolveBusinessUserInProgressStatus,
                () => ({
                    resolveBusinessUserInProgressStatus: jest.fn().mockResolvedValue(mock),
                })
            );
        }, [
            require.resolve('@self/root/src/resolvers/businessUserInProgressStatus'),
            {
                status: REGISTRATION_STATUS,
                message: REGISTRATION_MESSAGE,
            },
        ]);
    });

    afterAll(() => {
        mirror.destroy();
    });

    test('Проверка текста сообщения об ошибки', async () => {
        // Arrange
        await makeContext();

        // Act
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {isOpen: true});
        // Assert
        const selector = `[data-auto="${ERROR_MESSAGE_TEST_ID}"]`;
        const subtitle = container.querySelector(selector);
        expect(subtitle.textContent).toEqual(REGISTRATION_MESSAGE);
    });
});
