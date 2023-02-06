import * as React from 'react';
import { shallow } from 'enzyme';
import { IProps } from '../YandexComments.types';
import { NO_COMMENTS_TEXT, NO_COMMENTS_ICON } from '../YandexComments';

const apiKey = 'some_api_key';
const entityId = 'some_entity_id';
const uniqId = 'testUniqId';

// @ts-ignore
global.window.Ya = {};

jest.mock('@yandex-turbo/core/ua', () => ({
    __esModule: true,
    default: {
        BrowserName: 'YandexSearch',
    },
}));

describe('YandexComments component', () => {
    let YandexComments: React.ReactType<IProps>;

    beforeEach(() => {
        document.body.innerHTML = '';
        // Чтобы сбросились статические свойства
        jest.resetModules();
        ({ YandexComments } = require('../YandexComments'));
    });

    it('Компонент рендерится без ошибок', () => {
        const wrapper = shallow(
            <YandexComments
                uniqId={uniqId}
                theme="dark"
                lang="ru"
                params={{ entityId, apiKey }}
           />
        );

        expect(wrapper.find('.yandex-comments')).toHaveLength(1);
    });

    it('Вызывает метод выхода из fullScreen, если создается в ПП на странице, которая не в iframe', async() => {
        // @ts-ignore
        global.window.Ya.isInFrame = () => false;
        // @ts-ignore
        global.window.YandexApplicationsAPIBackend = { closeFullScreen: jest.fn(() => { return }) };

        shallow(
            <YandexComments
                uniqId={uniqId}
                theme="dark"
                lang="ru"
                params={{ entityId, apiKey }}
           />
        );

        await new Promise(r => setTimeout(r, 400));
        // @ts-ignore
        expect(global.window.YandexApplicationsAPIBackend.closeFullScreen).toHaveBeenCalledTimes(1);
    });

    it('Должен передавать правильный набор параметров при дефолтном наборе пропсов', async() => {
        shallow(
            <YandexComments
                uniqId={uniqId}
                theme="dark"
                lang="ru"
                params={{ entityId, apiKey }}
                scriptSrc="https://test.ru"
            />
        );

        // @ts-ignore
        global.window.Ya.Cmnt.init = jest.fn();
        // @ts-ignore
        global.window.Ya.Cmnt.on = jest.fn();

        const script = document.body.querySelector('script[src="https://test.ru"]') as HTMLScriptElement;
        script.onload!({} as Event);

        // Чтобы не городить всякие setImmediate или process.nextTick
        // @ts-ignore
        await YandexComments.onLoadCommentsPromise;

        // @ts-ignore
        expect(global.window.Ya.Cmnt.init).toHaveBeenCalledTimes(1);
        // @ts-ignore
        expect(global.window.Ya.Cmnt.init).toHaveBeenCalledWith(
            'testUniqId_ya-comments',
            { entityId, apiKey, iframe: false }
        );
    });

    it('Должен добавлять верные параметры noComments при переданном setNoComments в пропсах и не залогиненном пользователе', async() => {
        shallow(
            <YandexComments
                uniqId={uniqId}
                theme="dark"
                lang="ru"
                params={{ entityId, apiKey }}
                scriptSrc="https://test.ru"
                setNoComments
                isLoggedIn={false}
            />
        );

        // @ts-ignore
        global.window.Ya.Cmnt.init = jest.fn();
        // @ts-ignore
        global.window.Ya.Cmnt.on = jest.fn();

        const script = document.body.querySelector('script[src="https://test.ru"]') as HTMLScriptElement;
        script.onload!({} as Event);

        // Чтобы не городить всякие setImmediate или process.nextTick
        // @ts-ignore
        await YandexComments.onLoadCommentsPromise;

        // @ts-ignore
        expect(global.window.Ya.Cmnt.init).toHaveBeenCalledTimes(1);
        // @ts-ignore
        expect(global.window.Ya.Cmnt.init).toHaveBeenCalledWith(
            'testUniqId_ya-comments',
            {
                entityId,
                apiKey,
                iframe: false,
                noComments: 'none',
            }
        );
    });

    it('Должен добавлять верные параметры noComments при переданном setNoComments в пропсах и залогиненном пользователе', async() => {
        shallow(
            <YandexComments
                uniqId={uniqId}
                theme="dark"
                lang="ru"
                params={{ entityId, apiKey }}
                scriptSrc="https://test.ru"
                setNoComments
                isLoggedIn
            />
        );

        // @ts-ignore
        global.window.Ya.Cmnt.init = jest.fn();
        // @ts-ignore
        global.window.Ya.Cmnt.on = jest.fn();

        const script = document.body.querySelector('script[src="https://test.ru"]') as HTMLScriptElement;
        script.onload!({} as Event);

        // Чтобы не городить всякие setImmediate или process.nextTick
        // @ts-ignore
        await YandexComments.onLoadCommentsPromise;

        // @ts-ignore
        expect(global.window.Ya.Cmnt.init).toHaveBeenCalledTimes(1);
        // @ts-ignore
        expect(global.window.Ya.Cmnt.init).toHaveBeenCalledWith(
            'testUniqId_ya-comments',
            {
                entityId,
                apiKey,
                iframe: false,
                noComments: {
                    text: NO_COMMENTS_TEXT,
                    icon: NO_COMMENTS_ICON,
                },
            }
        );
    });
});
