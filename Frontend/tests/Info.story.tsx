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
        type: 'info',
        theme: 'dark',
        title: 'Без привлечения разработчиков',
        description: 'AppMetrica поможет вам с автоматизацией',
    },
    {
        size: 'medium',
        type: 'info',
        theme: 'dark',
        title: 'Без привлечения разработчиков',
        description: 'AppMetrica поможет вам с автоматизацией',
    },
    {
        size: 'small',
        type: 'info',
        theme: 'dark',
        description: 'Находите новые способы монетизации',
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
    .add('Info', Component => (
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

new ComponentStories(module, 'SimpleCard', { 'touch-phone': TouchSimpleCard })
    .addDecorator(withPlatform(Platform.Touch))
    .add('Info', Component => (
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
