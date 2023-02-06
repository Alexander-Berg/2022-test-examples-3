import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { ThemeContext } from '@src/storybook/decorators/withPlatformAndTheme';
import { Platform } from '@src/typings/platform';
import { ETheme } from '@src/store/services/internal/types';
import { ProductPropertyList } from '../ProductPropertyList';
import type { IProductPropertyListProps } from '../ProductPropertyList.typings';

const defaultProps: IProductPropertyListProps = {
    items: [
        ['Производитель', 'Xiaomi'],
        ['Тип датчиков', 'оптические'],
        [new Array(35).join('|'), new Array(35).join('|')],
        [
            'Дополнительные функции',
            'построение карты помещения, программирование по дням недели, расчет времени уборки, таймер',
        ],
        ['Экосистема Умного дома', ''],
        ['Версия интерфейса HDMI', '2.0'],
        ['Объем контейнера для пыли', '0.6л'],
        ['Фильтр тонкой очистки', 'Да'],
    ],
};

createPlatformStories('Tests/ProductPropertyList', ProductPropertyList, (stories, platform) => {
    stories
        .add('default', Component => {
            return (
                <ThemeContext.Consumer>
                    {theme => (
                        <div
                            style={{
                                backgroundColor: platform === Platform.Desktop && theme === ETheme.DARK ?
                                    'var(--color-g-bg-secondary)' : '',
                            }}
                        >
                            <Component
                                {...defaultProps}
                            />
                        </div>
                    )}
                </ThemeContext.Consumer>
            );
        });
});
