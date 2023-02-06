import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopBlockCard } from '../BlockCard@desktop';
import { TouchBlockCard } from '../BlockCard@touch';
import { TBlockCardPropsData } from '../BlockCard.types';

const datas: TBlockCardPropsData[] = [
    {
        id: '1',
        type: 'block',
        theme: 'light',
        title: 'Маркетинг',
        description: 'Оценивайте эффект рекламных кампаний в целевых метриках приложения, такие как покупки, удержание и вовлеченность новых пользователей, совершение целевых действий или прохождение онбордингов',
        buttons: [
            {
                label: 'Products scales',
                type: 'link',
            },
        ],
    },
    {
        id: '2',
        type: 'block',
        theme: 'light',
        tags: [
            {
                label: 'Customer Stories',
                theme: 'blue',
            },
        ],
        imgLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/114583/2a000001803bb5d7d7a9c424a7f390ac209a/orig',
        },
        title: 'Как мы помогли Yandex Cloud стать лучше',
        date: '23 сентября 2021',
        description: 'С помощью AppMetrica настроили отправку данных из приложения MyBook в Яндекс.Метрику, чтобы проанализировать трафик, который приходит в приложение через сайт',
        buttons: [
            {
                label: 'Products scales',
                type: 'link',
            },
        ],
    },
    {
        id: '3',
        type: 'block',
        theme: 'dark',
        tags: [
            {
                label: 'Customer Stories',
                theme: 'blue',
            },
        ],
        imgLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/114583/2a000001803bb5d7d7a9c424a7f390ac209a/orig',
        },
        title: 'Как мы помогли Yandex Cloud стать лучше',
        date: '23 сентября 2021',
        description: 'С помощью AppMetrica настроили отправку данных из приложения MyBook в Яндекс.Метрику, чтобы проанализировать трафик, который приходит в приложение через сайт',
        buttons: [
            {
                label: 'Products scales',
                type: 'link',
            },
        ],
    },
    {
        id: '4',
        type: 'features',
        theme: 'dark',
        imgLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/95093/2a0000018038a0ebfb67c6c05572d79df152/orig',
        },
        title: '>10 готовых отчетов',
        description: 'Используйте готовые отчеты AppMetrica чтобы находить инсайты и всю важную аналитику буквально в несколько кликов',
        buttons: [
            {
                label: 'Segmentation',
            },
        ],
    },
    {
        id: '5',
        type: 'features',
        theme: 'light',
        imgLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/114583/2a000001803c3cdbb0617000d0a05c250b59/orig',
        },
        title: 'Анализ покупок',
        description: 'Отчёт “Анализ покупкам” помогает анализировать встроенные покупки и находить драйверы роста продаж. Оценивайте ключевые метрики доходности приложения в разных срезах и сегментах: популярные товары, география покупок, ARPU, ARPPU',
        buttons: [
            {
                label: 'Документация',
                type: 'primary',
            },
            {
                label: 'Перейти в интерфейс',
            },
        ],
        footerText: 'Рост продаж на 30% с инсайтами после анализа покупок',
        footerButton: {
            label: 'Читать кейс',
            type: 'link',
        },
    },
    {
        id: '6',
        type: 'partners',
        theme: 'dark',
        title: 'Insuring the best user experience',
        imgLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/95093/2a00000180389f09010e50f8ffe987230218/orig',
        },
        logoLinks: {
            imgLink: 'https://avatars.mds.yandex.net/get-adv/42259/2a0000018038aaf6a5fa2f50247a7c2ffc8a/orig',
        },
        description: 'Customer-first experience: 4.9 star app Data-informed decisions: 95% of employees use analytics Business results: 500% increase in policy holders.',
        buttons: [
            {
                label: 'Read Full Story',
                type: 'link',
                theme: 'dark',
            },
        ],
    },

];

const customStyle = {
    display: 'grid',
    gridTemplateColumns: ' 200rem 450rem',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#fff',
    padding: '20rem',
};

new ComponentStories(module, 'Tests|Card', { desktop: DesktopBlockCard })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('BlockCard', Component => (
        <div style={customStyle} className="test">
            {
            datas.map(data => (
                <React.Fragment key={`${data.theme}-${data.type}`}>
                    <div>
                        <p> <b>Тема:</b> {data.theme}</p>
                        <p> <b>Тип блока:</b> {data.type}</p>
                    </div>
                    <Component data={data} />
                </React.Fragment>
            ))
        }
        </div>
    ));

new ComponentStories(module, 'Tests|Card', { 'touch-phone': TouchBlockCard })
    .addDecorator(withPlatform(Platform.Touch))
    .add('BlockCard', Component => (
        <div style={customStyle} className="test">
            {
            datas.map(data => (
                <React.Fragment key={`${data.theme}-${data.type}`}>
                    <div>
                        <p> <b>Тема:</b> {data.theme}</p>
                        <p> <b>Тип блока:</b> {data.type}</p>
                    </div>
                    <Component data={data} />
                </React.Fragment>
            ))
        }
        </div>
    ));
