import React from 'react';
import { shallow } from 'enzyme';

import { Link } from '../../../../src/components/notification/link';

describe('Link', () => {
    describe('plain', () => {
        test('should render link with href and target', () => {
            const component = shallow(
                <Link href="//yandex.ru" target="_blank">Link</Link>
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render empty string if no href is passed', () => {
            expect(shallow(<Link>Link</Link>).html()).toMatchSnapshot();
        });
    });
});
