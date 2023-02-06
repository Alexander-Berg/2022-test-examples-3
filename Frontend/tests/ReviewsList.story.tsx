import React from 'react';
import { action } from '@storybook/addon-actions';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { ESortOption } from '../../ReviewsSortingSelect/ReviewsSortingSelect.const';
import { ReviewsList } from '..';

createPlatformStories('Tests/Reviews/ReviewsList', ReviewsList, stories => {
    stories
        .add('initializing', ReviewsList => {
            return (
                <div style={{ width: 300 }}>
                    <ReviewsList
                        state="loading"
                        count={0}
                        reviews={[]}
                        sorting={ESortOption.Helpfulness}
                        hasMore={false}
                        onChangeSort={action('onChangeSort')}
                        onLoadMore={action('onLoadMore')}
                    />
                </div>
            );
        })
        .add('success', ReviewsList => {
            return (
                <div style={{ width: 300 }}>
                    <ReviewsList
                        state="success"
                        count={45}
                        reviews={[{
                            rating: { val: 4, max: 5 },
                            text: 'Хороший эспрессо с пенкой, можно добавить воды, упакована была хорошо',
                            name: 'Иван Крылов',
                            avatarId: '15298/enc-e23b6f52396b35bc1cd1183c26b26bc4bacbc1715f7a3341bf8f78d1bf20bbb4',
                            time: 1654086965000,
                            source: 'Яндекс.Маркет',
                        }, {
                            rating: { val: 3, max: 5 },
                            text: 'Достоинства: • Товары надёжно упакованы\n• Пункт выдачи легко найти\n• Было просто забрать заказ\n• Заказ был в пункте выдачи в назначенный день\n\nКомментарий: Отлично упакован. Всё соответствует. Большое спасибо',
                            name: 'Сергей П.',
                            avatarId: '15298/enc-cd4865a1fd4c67f99eb23d5b07c33f9498eee9008467e0d4eb455d6405be2647',
                            time: 1653814279000,
                            source: 'Яндекс.Маркет',
                        }, {
                            rating: { val: 2, max: 5 },
                            text: 'Недостатки: Привезли не то или не все товары\n\nКомментарий: Привезли не тот заказ, а возвращать надо через пункт выдачи!!! Заказ был чёрный+бронза, а привезли чёрный+серебро!',
                            name: 'Анна П.',
                            avatarId: '54535/nhUWZWZBjEBHwsGYGj0tjEvhcA-1',
                            time: 1653664523000,
                            source: 'Яндекс.Маркет',
                        }]}
                        sorting={ESortOption.Helpfulness}
                        hasMore
                        onChangeSort={action('onChangeSort')}
                        onLoadMore={action('onLoadMore')}
                    />
                </div>
            );
        })
        .add('loading', ReviewsList => {
            return (
                <div style={{ width: 300 }}>
                    <ReviewsList
                        state="loading"
                        count={45}
                        reviews={[{
                            rating: { val: 4, max: 5 },
                            text: 'Хороший эспрессо с пенкой, можно добавить воды, упакована была хорошо',
                            name: 'Иван Крылов',
                            avatarId: '15298/enc-e23b6f52396b35bc1cd1183c26b26bc4bacbc1715f7a3341bf8f78d1bf20bbb4',
                            time: 1654086965000,
                            source: 'Яндекс.Маркет',
                        }, {
                            rating: { val: 3, max: 5 },
                            text: 'Достоинства: • Товары надёжно упакованы\n• Пункт выдачи легко найти',
                            name: 'Сергей П.',
                            avatarId: '15298/enc-cd4865a1fd4c67f99eb23d5b07c33f9498eee9008467e0d4eb455d6405be2647',
                            time: 1653814279000,
                            source: 'Яндекс.Маркет',
                        }]}
                        sorting={ESortOption.Helpfulness}
                        hasMore
                        onChangeSort={action('onChangeSort')}
                        onLoadMore={action('onLoadMore')}
                    />
                </div>
            );
        });
});
