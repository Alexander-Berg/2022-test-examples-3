import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopMainMenu } from '../MainMenu@desktop';
import { TouchMainMenu } from '../MainMenu@touch';
import { TMainMenuPropsData } from '../MainMenu.types';

const customStyleDesktop = {
    display: 'grid',
    backgroundColor: '#D9E0EC',
    padding: '80rem',
    height: '1000rem',
};

const data: TMainMenuPropsData = {
    firstLevelItems: [
        {
            id: 3,
            href: '/solutions',
            title: 'Решения',
            secondLevelItems: [
                { id: 7, href: '/blogs/1', title: 'Продуктовая аналитика' },
                { id: 8, href: '/blogs/2', title: 'Аналитика доходности' },
                { id: 9, href: '/blogs/1', title: 'Обзор аудитории' },
                { id: 10, href: '/blogs/1', title: 'Трекер' },
                { id: 11, href: '/blogs/2', title: 'Мониторинг стабильности' },
                { id: 12, href: '/blogs/1', title: 'Пуш-уведомления' },
                { id: 13, href: '/blogs/2', title: 'Дашборды и экспорт' },
                { id: 14, href: '/blogs/2', title: 'API' },
                { id: 15, href: '/blogs/2', title: 'No-code' },
                { id: 16, href: '/blogs/2', title: 'Сегментация' },
            ] },
        {
            id: 4,
            href: '/industrii',
            title: 'Индустрии',
            secondLevelItems: [
                { id: 17, href: '/blogs/1', title: 'Games' },
                { id: 18, href: '/blogs/1', title: 'E-Commerce' },
                { id: 19, href: '/blogs/1', title: 'Подписки' },
            ],
        },
        {
            id: 5,
            href: '/resurce',
            title: 'Ресуры',
            secondLevelItems: [
                { id: 20, href: '/blogs/1', title: 'Интеграция' },
                { id: 21, href: '/blogs/1', title: 'Документация' },
                { id: 22, href: '/blogs/1', title: 'Блог' },
                { id: 23, href: '/blogs/1', title: 'Кейсы' },
                { id: 24, href: '/blogs/1', title: 'Быстрый старт' },
                { id: 25, href: '/blogs/1', title: 'Технологии' },
            ],
        },
        {
            id: 6,
            href: '/about_us',
            title: 'О нас',
            secondLevelItems: [
                { id: 26, href: '/blogs/1', title: 'Поддержка и контакты' },
                { id: 27, href: '/blogs/1', title: 'Партнёры' },
                { id: 28, href: '/blogs/1', title: 'Глоссарий' },
                { id: 29, href: '/blogs/1', title: 'Список фичей' },
            ],
        },
    ],
    theme: 'light',
};

new ComponentStories(module, 'Tests|ManegeComponent', { desktop: DesktopMainMenu })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('MainMenuLight', Component => (
        <div style={customStyleDesktop} className="test">
            <Component data={data} />
        </div>
    ));

const customStyleTouch = {
    width: '375rem',
    backgroundColor: '#D9E0EC',
    height: '500rem',
    padding: '20rem',
};

new ComponentStories(module, 'Tests|ManegeComponent', { 'touch-phone': TouchMainMenu })
    .addDecorator(withPlatform(Platform.Touch))
    .add('MainMenuLight', Component => (
        <div style={customStyleTouch} className="test">
            <Component data={data} />
        </div>
    ));
