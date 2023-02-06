import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopButton } from '../Button@desktop';
import { TouchButton } from '../Button@touch';
import { TButton } from '../Button.types';

const datas: TButton[] = [
    {
        label: 'Попробовать демо',
        theme: 'dark',
    },
    {
        label: 'Попробовать демо',
        theme: 'dark',
        disabled: true,
    },
    {
        label: 'Получить доступ',
        theme: 'dark',
        type: 'link',
    },
    {
        label: 'Получить доступ',
        theme: 'dark',
        type: 'link',
        disabled: true,
    },
    {
        label: 'Скачать',
        theme: 'dark',
        type: 'download',
    },
    {
        label: 'Скачать',
        theme: 'dark',
        type: 'download',
        disabled: true,
    },
    {
        label: 'Аналитика доходности',
        theme: 'dark',
        type: 'tabs',
    },
    {
        label: '',
        theme: 'dark',
        type: 'back',
    },
    {
        label: '',
        theme: 'dark',
        type: 'back',
        disabled: true,
    },
    {
        label: '',
        theme: 'dark',
        type: 'up',
    },
    {
        label: '',
        theme: 'dark',
        type: 'up',
        disabled: true,
    },
    {
        label: '',
        theme: 'dark',
        type: 'menu-bar',
        disabled: true,
    },
];
const customStyle = {
    display: 'grid',
    gridTemplateColumns: ' 200rem 450rem',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#332487',
    padding: '20rem',
    color: '#fff',
};

new ComponentStories(module, 'Tests|HelpComponent', { desktop: DesktopButton })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('ButtonDark', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={`${data.theme}-${data.type}-${data.disabled}`}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                            <p> <b>Тип кнопки:</b> {data.type}</p>
                            <p> <b>Disabled:</b> {data.disabled ? 'Да' : 'Нет'}</p>
                        </div>
                        <Component {...data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));

new ComponentStories(module, 'Tests|HelpComponent', { 'touch-phone': TouchButton })
    .addDecorator(withPlatform(Platform.Touch))
    .add('ButtonDark', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={`${data.theme}-${data.type}-${data.disabled}`}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                            <p> <b>Тип кнопки:</b> {data.type}</p>
                            <p> <b>Disabled:</b> {data.disabled ? 'Да' : 'Нет'}</p>
                        </div>
                        <Component {...data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));
