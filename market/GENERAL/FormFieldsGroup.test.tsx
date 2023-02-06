import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { FormFieldsGroup, Props } from './FormFieldsGroup';

const DEFAULT_TITLE = 'Test title';
const CHILDREN_TEXT_CONTENT = 'CHILDREN_TEXT_CONTENT';

const defaultProps: Props = {
  title: DEFAULT_TITLE,
  isLoading: true,
  children: <div>{CHILDREN_TEXT_CONTENT}</div>,
};

describe('FormFieldsGroup', () => {
  let wrapper: ReactWrapper | null;

  const renderWithProps = (props: Props) => {
    wrapper = mount(<FormFieldsGroup {...props} />);
  };

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('with default props', () => {
    beforeEach(() => {
      renderWithProps(defaultProps);
    });

    it('should be render', () => {
      expect(wrapper!.find(FormFieldsGroup)).toHaveLength(1);
    });

    it('should contain children', () => {
      expect(wrapper!.text()).toContain(CHILDREN_TEXT_CONTENT);
    });

    it('should contain title', () => {
      expect(wrapper!.find('.Label').first().html()).toContain(DEFAULT_TITLE);
    });
  });
});
