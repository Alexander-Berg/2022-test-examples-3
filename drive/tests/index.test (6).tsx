import { render } from 'enzyme';
import * as React from 'react';

import { FinesRoutes } from '../../../../utils/navigation';
import { ShortFinesInfo } from '../ShortFinesInfo';
import { FINE_INFO_DATA } from './MockedData';

describe('Short fine item', () => {
    it('Correct render in car card', () => {
        const component = render(
            <ShortFinesInfo data={FINE_INFO_DATA} route={FinesRoutes.cars} onShortItemClick={() => {}}/>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Correct render in clients card', () => {
        const component = render(
            <ShortFinesInfo data={FINE_INFO_DATA} route={FinesRoutes.clients} onShortItemClick={() => {}}/>,
        );
        expect(component).toMatchSnapshot();
    });
});
