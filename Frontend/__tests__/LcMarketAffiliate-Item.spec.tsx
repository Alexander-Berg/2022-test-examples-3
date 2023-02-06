import * as React from 'react';
import { shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { LcSizePx, LcFont, LcTypeface, LcAlign } from '../../lcTypes/lcTypes';
import { LcMarketAffiliateItemType } from '../Item/LcMarketAffiliate-Item.types';
import LcMarketAffiliateItem from '../Item/LcMarketAffiliate-Item';

expect.addSnapshotSerializer(serializer);

const commonLcMarketAffiliateItemProps = {
    widgetParams: JSON.stringify({
        type: 'offers',
        params: {
            clid: 2310490,
            themeId: 2,
            searchText: 'Смартфон',
        },
    }),
    textParams: {
        content:
            '<a href="https://yandex.ru/support/market-distr/widgets/widgets.html">Виджеты Маркета на сайте партнера</a>',
        size: LcSizePx.s16,
        font: LcFont.TEXT,
        typeface: LcTypeface.REGULAR,
        align: LcAlign.LEFT,
    },
    imageUrl: '//yastatic.net/s3/lpc/assets/image-preview.svg',
    mix: 'mix',
};

describe('<LcMarketAffiliateItem/> component', () => {
    test('should render LcMarketAffiliateItem, type "Widget"', () => {
        // @ts-ignore
        window.YaMarketAffiliate = {
            createWidget: jest.fn(),
        };

        const component = shallow(
            <LcMarketAffiliateItem
                type={LcMarketAffiliateItemType.Widget}
                {...commonLcMarketAffiliateItemProps}
            />
        );

        expect(component.html()).toMatchSnapshot();
    });

    test('should render LcMarketAffiliateItem, type "Text"', () => {
        const component = shallow(
            <LcMarketAffiliateItem
                type={LcMarketAffiliateItemType.Text}
                {...commonLcMarketAffiliateItemProps}
            />
        );

        expect(component.html()).toMatchSnapshot();
    });

    test('should render LcMarketAffiliateItem, type "Image"', () => {
        const component = shallow(
            <LcMarketAffiliateItem
                type={LcMarketAffiliateItemType.Image}
                {...commonLcMarketAffiliateItemProps}
            />
        );

        expect(component.html()).toMatchSnapshot();
    });
});
