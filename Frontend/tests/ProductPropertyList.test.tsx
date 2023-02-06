import * as React from 'react';
import { render } from 'enzyme';
import { PlatformContext } from '@src/hocs/withPlatform/context';
import { Platform } from '@src/typings/platform';
import { ProductPropertyList } from '../ProductPropertyList';

const items: [string, string][] = [
    ['1', '1'],
    ['2', '2'],
    ['3', '3'],
    ['4', '4'],
    ['5', '5'],
    ['6', '6'],
    ['7', '7'],
    ['8', '8'],
    ['9', '9'],
];

describe('ProductPropertyList', () => {
    it('render 8 items for desktop', () => {
        const wrapper = render((
            <PlatformContext.Provider value={Platform.Desktop}>
                <ProductPropertyList items={items} />
            </PlatformContext.Provider>
        ));

        expect(wrapper).toMatchSnapshot();
    });

    it('render 5 items for touch', () => {
        const wrapper = render((
            <PlatformContext.Provider value={Platform.Touch}>
                <ProductPropertyList items={items} />
            </PlatformContext.Provider>
        ));

        expect(wrapper).toMatchSnapshot();
    });

    it('render 3 items for desktop with initialCount props', () => {
        const wrapper = render((
            <PlatformContext.Provider value={Platform.Desktop}>
                <ProductPropertyList items={items} initialCount={3} />
            </PlatformContext.Provider>
        ));

        expect(wrapper).toMatchSnapshot();
    });

    it('render 3 items for touch with initialCount props', () => {
        const wrapper = render((
            <PlatformContext.Provider value={Platform.Touch}>
                <ProductPropertyList items={items} initialCount={3} />
            </PlatformContext.Provider>
        ));

        expect(wrapper).toMatchSnapshot();
    });
});
