import React from 'react';
import type { StaticRouterProps } from 'react-router-dom/server';
import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import type { IInternalState } from '@src/store/services/internal/types';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { NBSP } from '@src/constants/char';
import { AliceHeader } from '../index';
import { getAliceGuruProps } from '../hooks/getAliceGuruProps';
import { getAliceGiftsProps } from '../hooks/getAliceGiftsProps';

const internal: IInternalState = {
    nonce: '',
    baseUrl: '',
    canonicalUrl: '',
    isYandexNet: true,
    isYandexApp: false,
    isYandexAppWebVerticalsExp: false,
    isYandexAppUAExp: false,
    expFlags: {},
    origin: '',
    project: 'products',
    currentYear: 2022,
};

const routerProps: Partial<StaticRouterProps> = {
    location: {
        search: '?text=холодильник',
    },
};

createPlatformStories('Tests/AliceHeader', AliceHeader, stories => {
    stories
        .addDecorator(withStaticRouter(routerProps))
        .add('plain', Component => {
            return (
                <StubReduxProvider stub={{ internal }}>
                    <Component
                        title="Алиса"
                        text={`Вот, нашла популярные товары по${NBSP}вашему запросу и цены на${NBSP}них в${NBSP}разных магазинах`}
                    />
                </StubReduxProvider>
            );
        })
        .add('redesign', Component => {
            return <StubReduxProvider stub={{ internal }}><Component redesign /></StubReduxProvider>;
        })
        .add('guru', Component => {
            const props = getAliceGuruProps(100);

            if (props) {
                props.returnURL = 'https://yandex.ru';
                return <StubReduxProvider stub={{ internal }}><Component {...props} title="Алиса" /></StubReduxProvider>;
            }

            return null;
        })
        .add('gifts', Component => {
            const props = getAliceGiftsProps(100, 'yandex.ru', 'холодильник');
            return props && <StubReduxProvider stub={{ internal }}><Component {...props} title="Алиса" /></StubReduxProvider>;
        });
});
