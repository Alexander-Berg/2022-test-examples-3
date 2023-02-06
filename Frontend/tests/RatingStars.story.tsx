import React from 'react';
import { number, select } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { RatingStars } from '..';

function Wrapper({ children }: React.PropsWithChildren<{}>) {
    return (
        <div style={{ display: 'inline-block' }} className="story-wrapper">
            {React.Children.map(children, item => (
                <div style={{ margin: '2px 0' }}>
                    {item}
                </div>
            ))}
        </div>
    );
}

createPlatformStories('Tests/RatingStars', RatingStars, stories => {
    stories
        .add('plain', RatingStars => {
            return (
                <RatingStars
                    rating={number('rating', 3)}
                    step={number('step', 0.5)}
                    max={number('max', 5)}
                    size={select('size', ['s', 'm'], 's')}
                />
            );
        })
        .add('showcase', RatingStars => (
            <Wrapper>
                <RatingStars rating={2.5} step={1} />
                <RatingStars rating={2.5} step={0.5} />
                <RatingStars rating={7} step={1} max={10} />

                <RatingStars rating={2.5} step={1} size="m" />
                <RatingStars rating={2.5} step={0.5} size="m" />
                <RatingStars rating={7} step={1} max={10} size="m" />
            </Wrapper>
        ));
});
