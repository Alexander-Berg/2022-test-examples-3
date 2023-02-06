import * as React from 'react';
import { shallow } from 'enzyme';
import { LcHiddenSectionPresenter } from '../LcHiddenSection';
import { ILcHiddenSectionProps } from '../LcHiddenSection.types';

describe('LcHiddenSectionPresenter tests', () => {
    function getDefaultProps(): ILcHiddenSectionProps {
        return {
            anchor: 'test-id',
            selectedId: 'selected-id',
            messages: {
                selectedSectionText: '',
                sectionText: '',
            },
        };
    }

    test('should render correctly when selected id equals to sections\'s id', () => {
        const props = getDefaultProps();
        props.selectedId = props.anchor;
        const wrapper = shallow(<LcHiddenSectionPresenter {...props} />);

        expect(wrapper.hasClass('lc-hidden-section_focused')).toBe(true);
    });

    test('should render correctly when selected id does not equal to sections\'s id', () => {
        const props = getDefaultProps();
        const wrapper = shallow(<LcHiddenSectionPresenter {...props} />);

        expect(wrapper.hasClass('lc-hidden-section_focused')).toBe(false);
    });
});
