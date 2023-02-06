import {screen, waitFor} from '@testing-library/dom';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {
    defaultUser,
    defaultQuestion,
    USER_PROFILE_CONFIG,
} from './__mocks__/helpers';
import {
    productWithPicture,
    productSlug,
    productId,
    questionSlug,
    questionId,
} from './__mocks__';

const widgetPath = '../';
const user = defaultUser({uid: USER_PROFILE_CONFIG.uid, publicId: USER_PROFILE_CONFIG.publicId});

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext(auth) {
    const userData = auth ? user : {isAuth: false};
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId(true)};
    return mandrelLayer.initContext({
        user: userData,
        request: {
            cookie,
            params: {
                productSlug,
                productId,
                questionSlug,
                questionId,
            },
        },
    });
}
beforeAll(async () => {
    mockLocation();

    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});
afterAll(() => {
    mirror.destroy();
});
describe('Блок снипета вопроса', () => {
    beforeEach(async () => {
        await makeContext(true);
    });
    test('Когда вопрос можно удалить, по умолчанию кнопка удалить отображается', async () => {
        await kadavrLayer.setState('schema', {
            users: [user],
            modelQuestions: [defaultQuestion({userUid: USER_PROFILE_CONFIG.uid, canDelete: true})],
        });
        await kadavrLayer.setState('report', productWithPicture);
        await apiaryLayer.mountWidget(widgetPath, {questionId});
        await waitFor(async () => {
            expect(screen.queryByRole('button', {name: /удалить/i})).toBeVisible();
        });
    });
    test('Когда вопрос нельзя удалить, по умолчанию кнопка удалить не видна', async () => {
        await kadavrLayer.setState('schema', {
            users: [user],
            modelQuestions: [defaultQuestion({userUid: USER_PROFILE_CONFIG.uid, canDelete: false})],
        });
        await kadavrLayer.setState('report', productWithPicture);
        await apiaryLayer.mountWidget(widgetPath, {questionId});
        await waitFor(async () => {
            expect(screen.queryByRole('button', {name: /удалить/i})).toBeNull();
        });
    });
});
