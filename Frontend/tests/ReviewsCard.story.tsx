import * as React from 'react';
import { ComponentStories } from '@yandex-int/storybook-with-platforms';

import { withPlatform } from '../../../storybook/decorators';
import { Platform } from '../../../typings/platform';
import { DesktopReviewCard } from '../ReviewCard@desktop';
import { TouchReviewCard } from '../ReviewCard@touch';
import { TReviewCardPropsData } from '../ReviewCard.types';

const datas: TReviewCardPropsData[] = [
    {
        reviewerData: {
            title: 'Евгений Васильев',
            jobTitle: 'Менеджер ВТБ',
            type: 'Person',
        },
        buttonData: {
            label: 'Читать полностью',
            theme: 'light',
            type: 'link',
        },
        review: 'Очень понравилось! За тебя все оформляется с начала создания, а результаты приятно удивили: думала, что нереально их получить в этот же день. Поняли в каком направлении движемся и как развивать дальше наш продукт...',
    },
    {
        reviewerData: {
            title: 'Adapty',
            type: 'Company',
        },
        buttonData: {
            label: 'Читать полностью',
            theme: 'light',
            type: 'link',
        },
        review: 'Очень понравилось! За тебя все оформляется с начала создания, а результаты приятно удивили: думала, что нереально их получить в этот же день. Поняли в каком направлении движемся и как развивать дальше наш продукт...',
    },
];

const customStyle = {
    display: 'grid',
    gridTemplateColumns: ' 200rem 450rem',
    alignItems: 'center',
    gridGap: '20rem',
    background: '#1F2533',
    padding: '20rem',
    color: '#fff',
};

new ComponentStories(module, 'Tests|Card', { desktop: DesktopReviewCard })
    .addDecorator(withPlatform(Platform.Desktop))
    .add('ReviewCardDark', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={`${data.theme}`}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                        </div>
                        <Component data={data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));

new ComponentStories(module, 'Tests|Card', { 'touch-phone': TouchReviewCard })
    .addDecorator(withPlatform(Platform.Touch))
    .add('ReviewCardDark', Component => (
        <div style={customStyle} className="test">
            {
                datas.map(data => (
                    <React.Fragment key={`${data.theme}`}>
                        <div>
                            <p> <b>Тема:</b> {data.theme}</p>
                        </div>
                        <Component data={data} />
                    </React.Fragment>
                ))
            }
        </div>
    ));
