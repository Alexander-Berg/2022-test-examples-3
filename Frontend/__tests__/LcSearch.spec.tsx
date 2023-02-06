import * as React from 'react';
import { shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { LcSearch } from '../LcSearch';
import { LcSizes, LcAlign } from '../../lcTypes/lcTypes';

expect.addSnapshotSerializer(serializer);

describe('<LcSearch/> component', () => {
    test('should render LcSearch', () => {
        const component = shallow(
            <LcSearch
                align={LcAlign.CENTER}
                size={LcSizes.M}
                width={LcSizes.S}
                placeholder="Поиск в Маркете"
                websiteUrl="market.yandex.ru"
                searchUrl="https://yandex.com.tr/search/"
                hasYellowBorder
                sectionId="1"
                events={[]}
                offsets={{
                    padding: {
                        top: LcSizes.S,
                        bottom: LcSizes.S,
                    },
                }}
            />
        );

        expect(component.html()).toMatchSnapshot();
    });
});
