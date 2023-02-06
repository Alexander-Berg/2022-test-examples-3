import React from 'react';
import { text, number } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Review } from '..';

createPlatformStories('Tests/Reviews/Review', Review, stories => {
    stories
        .add('plain', Review => {
            return (
                <div style={{ width: 300 }}>
                    <Review
                        name={text('name', 'Константин Константинович Константинопольский')}
                        avatarId={text('avatarId', '51381/PcHGoxHajI6zo1peepYSpBu6TI-1')}
                        time={number('time', 1651055627000)}
                        source={text('source', 'Яндекс.Маркет')}
                        rating={{
                            val: number('rating', 3),
                            max: 5,
                        }}
                        text={text('text', `Достоинства: • Товары надёжно упакованы
                            • Пункт выдачи легко найти
                            • Было просто забрать заказ
                            • Заказ был в пункте выдачи в назначенный день

                            Комментарий: Отлично упакован. Всё соответствует. Большое спасибо`)}
                    />
                </div>
            );
        });
});
