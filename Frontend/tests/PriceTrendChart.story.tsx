import React from 'react';
import { withKnobs, number, button } from '@storybook/addon-knobs';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';

import type { IPriceTrendChartProps } from '../PriceTrendChart.typings';
import { PriceTrendChart } from '..';

import './PriceTrendChart.story.scss';

const defaultProps: IPriceTrendChartProps = {
    className: 'PriceTrendChartStory',
    data: [
        { date: '2021-03-28', price: 525013 },
        { date: '2021-03-30', price: 620000 },
        { date: '2021-04-01', price: 700000 },
        { date: '2021-04-04', price: 750000 },
        { date: '2021-04-08', price: 730000 },
        { date: '2021-04-16', price: 560000 },
        { date: '2021-04-20', price: 489855 },
        { date: '2021-04-24', price: 520000 },
        { date: '2021-04-28', price: 600000 },
        { date: '2021-05-02', price: 650000 },
        { date: '2021-05-06', price: 685000 },
        { date: '2021-05-10', price: 670000 },
        { date: '2021-05-14', price: 660000 },
        { date: '2021-05-18', price: 700000 },
        { date: '2021-05-22', price: 750000 },
        { date: '2021-05-24', price: 820013 },
    ],
};

function random(min: number, max: number) {
    return Math.floor(Math.random() * (max - min)) + min;
}

function randomizeData(min: number, max: number) {
    defaultProps.data = defaultProps.data?.map(({ date }) => ({
        date, price: Math.trunc(random(min, max)),
    }));
}

createPlatformStories('Tests/PriceTrendChart', PriceTrendChart, stories => {
    stories
        .addDecorator(withKnobs)
        .addDecorator(withStaticRouter())
        .add('plain', Component => {
            const min = number('min', 500000);
            const max = number('max', 800000);
            const width = number('width', 0) || undefined;
            const height = number('height', 0) || undefined;

            button('Randomize', () => randomizeData(min, max));

            const props = {
                ...defaultProps,
                width,
                height,
                isXAxisLabelsEnabled: true,
                isYAxisLabelsEnabled: true,
            };

            return (
                <Component {...props} />
            );
        });
});
