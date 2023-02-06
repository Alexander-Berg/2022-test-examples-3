import React from 'react';
import { shallow } from 'enzyme';

import Preview from '../../../../src/components/notification/preview';

describe('Preview', () => {
    describe('plain', () => {
        test('should render preview if src is passed', () => {
            const component = shallow(
                <Preview
                    notification={{
                        meta: { test: 'resource' },
                        preview: 'test',
                        preview_src: '//yandex.ru',
                    }} />
            );

            expect(component.html()).toMatchSnapshot();
        });
    });
});
