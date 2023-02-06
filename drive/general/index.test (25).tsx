import { shallow } from 'enzyme';
import React from 'react';

import { Collapse2 } from './index';

const className = 'cn-test';
const headerClassname = 'header-cn-test';

const Component = (initialExpanded: boolean) => <Collapse2 title={'Заголовок'}
                                                           expandText={'Развернуть тестовый заголовок'}
                                                           closeText={'Свернуть тестовый заголовок'}
                                                           initialExpanded={initialExpanded}
                                                           className={className}
                                                           headerClassname={headerClassname}>
    <div>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ad adipisci alias aperiam, consectetur
        dolorum facilis inventore ipsum iste laboriosam maiores mollitia nisi nobis perferendis, repellendus
        voluptas. Eveniet id perferendis sequi.
    </div>
</Collapse2>;

describe('Collapse2', () => {
    it('should be open', () => {
        const wrapper = shallow(Component(true));
        expect(wrapper).toMatchSnapshot();
    });

    it('should be closed', () => {
        const wrapper = shallow(Component(false));
        expect(wrapper).toMatchSnapshot();
    });

    it('should correctly work with class names', () => {
        const wrapper = shallow(Component(true));
        expect(wrapper.find(`.${className}`)).toMatchSnapshot();
        expect(wrapper.find(`.${headerClassname}`)).toMatchSnapshot();
    });
});
