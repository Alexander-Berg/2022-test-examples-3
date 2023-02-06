import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {fireEvent, getByTestId, getByText, screen} from '@testing-library/dom';

import {
    usersMock,
    relatedCommentaryMock,
    mockResolveCurrentUserDenormalizedSync,
} from './__mocks__';

const widgetPath = '../';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext() {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID: usersMock.uid,
        },
        request: {
            cookie,
            params: {
                // Параметр который ждет контроллер
                productId: 1,
            },
        },
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    mockLocation();

    window.scrollTo = jest.fn();
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
});

beforeAll(async () => {
    await jestLayer.doMock(
        require.resolve('@self/platform/widgets/content/BelarusBoundPhoneDialog'),
        () => ({create: () => Promise.resolve(null)})
    );

    // note: mandrelLayer.initContext не поддерживает мок StoutUser.publicId
    await jestLayer.backend.runCode(mockResolveCurrentUserDenormalizedSync => {
        jest.spyOn(require('@self/root/src/resolvers/user'), 'resolveCurrentUserDenormalizedSync')
            .mockReturnValue(mockResolveCurrentUserDenormalizedSync);
    }, [mockResolveCurrentUserDenormalizedSync]);
});

afterAll(() => {
    mirror.destroy();
});

describe('Комментарии с двумя уровнями.', () => {
    beforeEach(async () => {
        await makeContext();
    });
    describe('2ой уровень', () => {
        beforeEach(async () => {
            await kadavrLayer.setState('schema', {users: usersMock});
            await kadavrLayer.setState('storage', {commentary: relatedCommentaryMock});
        });
        describe('Список комментариев, последнего уровня глубиной 2', () => {
            test('В блоке раскрывания на первом уровне по умолчанию отображается корректное число потомков', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                return expect(screen.queryByText('1 комментарий')).toBeTruthy();
            });
            test('При удаленном комментарии 1го уровня он заменяется на плейсхолдер', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                screen.getByTestId('commentaries-more-actions-button').click();
                (await screen.findByText('Удалить')).click();
                (await screen.findByText('Удалить')).click();
                return expect(screen.findByText('Комментарий удалён')).resolves.toBeTruthy();
            });
            test('При удаленном комментарии 1го уровня форма отправки комментария отображается', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                screen.getByTestId('commentaries-more-actions-button').click();
                (await screen.findByText('Удалить')).click();
                (await screen.findByText('Удалить')).click();

                const expandButton = await screen.findByRole('button', {name: '1 комментарий'});

                expandButton.click();

                const smallForm = await screen.findByTestId('commentaries-small-form');
                const textArea = smallForm.querySelector('textarea');

                return expect(textArea).toBeTruthy();
            });
            test('При отправке ответа на комментарий 1го уровня он отображается в списке', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });

                const expandButton = await screen.findByRole('button', {name: '1 комментарий'});

                expandButton.click();

                const smallForm = await screen.findByTestId('commentaries-small-form');
                const textArea = smallForm.querySelector('textarea');

                fireEvent.change(textArea, {target: {value: 'check please!'}});
                const sendButton = getByText(smallForm, 'Отправить');
                sendButton.click();

                return expect(screen.findByText('check please!')).resolves.toBeTruthy();
            });
            test('При отправке ответа на комментарий с обращением 1го уровня он отображается в списке с обращением', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                const expandButton = await screen.findByRole('button', {name: '1 комментарий'});

                expandButton.click();

                const smallForm = await screen.findByTestId('commentaries-small-form');

                const name = getByTestId(smallForm, 'public-user-info').textContent;
                const textArea = smallForm.querySelector('textarea');

                fireEvent.change(textArea, {target: {value: `${name}check please!`}});

                const sendButton = getByText(smallForm, 'Отправить');
                sendButton.click();

                return expect(screen.findByText(`${name}check please!`)).resolves.toBeTruthy();
            });
            test('При удаленном комментарии 1го уровня форма отправки комментария не содержит обращения', async () => {
                await apiaryLayer.mountWidget(widgetPath, {
                    entity: 'productReviewComment',
                    entityId: 1,
                });
                screen.getByTestId('commentaries-more-actions-button').click();
                (await screen.findByText('Удалить')).click();
                (await screen.findByText('Удалить')).click();

                const expandButton = await screen.findByRole('button', {name: '1 комментарий'});

                expandButton.click();

                const smallForm = await screen.findByTestId('commentaries-small-form');
                const textArea = smallForm.querySelector('textarea');

                return expect(textArea.value).toBeFalsy();
            });
        });
    });
});
