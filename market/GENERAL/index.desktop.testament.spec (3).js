import {screen} from '@testing-library/dom';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import SharedActionEmitter from '@yandex-market/apiary/client/sharedActionEmitter';
import {BOUND_PHONE_DIALOG_OPEN} from '@self/platform/actions/boundPhoneDialog';

const widgetPath = '../';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
});

afterAll(() => {
    mirror.destroy();
});

describe('Диалог подтверждения телефона для домена .by.', () => {
    beforeEach(async () => {
        await mandrelLayer.initContext();
    });
    test('Отображается', async () => {
        await apiaryLayer.mountWidget(widgetPath);
        const globalSharedActionEmitter = new SharedActionEmitter();
        globalSharedActionEmitter.dispatch({
            type: BOUND_PHONE_DIALOG_OPEN,
        });
        return expect(screen.findByText('Привязать номер')).toBeTruthy();
    });
});
