import * as React from 'react';
import { shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { LcMarketAffiliate } from '../LcMarketAffiliate';
import { LcSizes, LcSizePx, LcFont, LcTypeface, LcAlign } from '../../lcTypes/lcTypes';
import { Columns } from '../LcMarketAffiliate.types';
import { LcMarketAffiliateItemType } from '../Item/LcMarketAffiliate-Item.types';

expect.addSnapshotSerializer(serializer);

const commonItemParams = {
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

describe('<LcMarketAffiliate/> component', () => {
    test('should render LcMarketAffiliate', () => {
        const component = shallow(
            <LcMarketAffiliate
                events={[]}
                offsets={{
                    padding: {
                        top: LcSizes.S,
                        bottom: LcSizes.S,
                    },
                }}
                columns={Columns.Two}
                itemPaddingHorizontal={LcSizes.NONE}
                itemPaddingVertical={LcSizes.NONE}
                items={[
                    {
                        ...commonItemParams,
                        type: LcMarketAffiliateItemType.Widget,
                    }, {
                        ...commonItemParams,
                        type: LcMarketAffiliateItemType.Text,
                    }, {
                        ...commonItemParams,
                        type: LcMarketAffiliateItemType.Image,
                    },
                ]}
            />
        );

        expect(component.html()).toMatchSnapshot();
    });
});
