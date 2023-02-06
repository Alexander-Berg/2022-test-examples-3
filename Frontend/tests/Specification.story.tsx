import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopSimpleCard } from '../SimpleCard@desktop';
import { TouchSimpleCard } from '../SimpleCard@touch';
import { TSimpleCardPropsData } from '../SimpleCard.types';

const dataList: TSimpleCardPropsData[] = [
    {
        size: 'large',
        type: 'specification',
        theme: 'light',
        title: 'In-App',
        description: 'Комбинируйте автоматический трекинг с ручным, чтобы отслеживать покупки вне приложения,  к примеру оформление подписки на сайте',
    },
    {
        size: 'medium',
        type: 'specification',
        theme: 'light',
        title: 'In-App',
        description: 'Комбинируйте автоматический трекинг с ручным, чтобы отслеживать покупки вне приложения,  к примеру оформление подписки на сайте',
    },
    {
        size: 'small',
        type: 'specification',
        theme: 'light',
        title: 'In-App',
        description: 'Комбинируйте автоматический трекинг с ручным, чтобы отслеживать покупки вне приложения,  к примеру оформление подписки на сайте',
    },
];

const customStyleDark = {
    display: 'grid',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#1B202B',
    color: '#fff',
    padding: '20rem',
};

const customStyleLight = {
    display: 'grid',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#fff',
    color: 'black',
    padding: '20rem',
};

new ComponentStories(module, 'SimpleCard', { desktop: DesktopSimpleCard })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('Specification', Component => (
        <div>
            <div style={customStyleDark} className="test">
                {
                    dataList.map(data => {
                        const theme = 'dark';
                        return (
                            <div key={`${data.size}-${data.title}`}>
                                <p> <b>size:</b> {data.size}</p>
                                <React.Fragment>
                                    <Component data={{ ...data, theme }} />
                                </React.Fragment>
                            </div>
                        );
                    })
                }
            </div>
            <div style={customStyleLight} className="test">
                {
                    dataList.map(data => {
                        data.theme = 'light';
                        return (
                            <div key={`${data.size}-${data.title}`}>
                                <p> <b>size:</b> {data.size}</p>
                                <React.Fragment>
                                    <Component data={data} />
                                </React.Fragment>
                            </div>
                        );
                    })
                }
            </div>
        </div>
    ));

new ComponentStories(module, 'SimpleCard', { 'touch-phone': TouchSimpleCard })
    .addDecorator(withPlatform(Platform.Touch))
    .add('Specification', Component => (
        <div>
            <div style={customStyleDark} className="test">
                {
                    dataList.map(data => {
                        const theme = 'dark';
                        return (
                            <div key={`${data.size}-${data.title}`}>
                                <p> <b>size:</b> {data.size}</p>
                                <React.Fragment>
                                    <Component data={{ ...data, theme }} />
                                </React.Fragment>
                            </div>
                        );
                    })
                }
            </div>
            <div style={customStyleLight} className="test">
                {
                    dataList.map(data => {
                        const theme = 'light';
                        return (
                            <div key={`${data.size}-${data.title}`}>
                                <p> <b>size:</b> {data.size}</p>
                                <React.Fragment>
                                    <Component data={{ ...data, theme }} />
                                </React.Fragment>
                            </div>
                        );
                    })
                }
            </div>
        </div>
    ));
