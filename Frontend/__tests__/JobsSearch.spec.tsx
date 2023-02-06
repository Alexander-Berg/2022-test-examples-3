import * as React from 'react';
import { mount } from 'enzyme';
import 'jest-enzyme';
import { Textinput as SubjectInput } from '@yandex-lego/components/Textinput/desktop/bundle';
import { JobsSearch as Subject } from '../JobsSearch';

import { IJobsSearchProps as IProps } from '../JobsSearch.types';

describe('JobsSearch', () => {
    let defaultProps: IProps;

    beforeEach(() => {
        defaultProps = {
            searchString: '',
            placeholder: 'Some placeholder',
            onSearchChange: () => {},
        };
    });

    it('should render without crashing', () => {
        const wrapper = mount(<Subject {...defaultProps} />);

        expect(wrapper.html()).toMatchSnapshot();
    });

    it('should add specified placeholder for input', () => {
        const input = mount(<Subject {...defaultProps} />).find(SubjectInput);

        expect(input).toHaveProp('placeholder', defaultProps.placeholder);
    });

    it('should use searchString argument as value of input', () => {
        const props: IProps = { ...defaultProps, searchString: 'some string' };
        const input = mount(<Subject {...props} />).find(SubjectInput);

        expect(input).toHaveProp('value', props.searchString);
    });
});
