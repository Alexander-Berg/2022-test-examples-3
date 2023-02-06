import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { TreeItem, TreeItemAction, TreeItemActionList, TreeItemContent, TreeItemIndent, TreeItemIndents } from '.';

let wrapper: ReactWrapper | null;

describe('<TreeItem />', () => {
  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('should render without error', () => {
    expect(() => {
      wrapper = mount(
        <TreeItem>
          <TreeItemIndents count={1} />
          <TreeItemContent />
          <TreeItemActionList>
            <TreeItemAction />
          </TreeItemActionList>
        </TreeItem>
      );
    }).not.toThrow();
  });

  it('should call onClick when clicked', () => {
    const handleClickMock = jest.fn();

    wrapper = mount(<TreeItem onClick={handleClickMock} />);
    wrapper.simulate('click');

    expect(handleClickMock).toHaveBeenCalledTimes(1);
  });

  it('should have the role `treeitem`', () => {
    wrapper = mount(<TreeItem />);

    expect(wrapper.find('[role="treeitem"]')).toHaveLength(1);
  });

  describe('<TreeItemIndents />', () => {
    it('should render correct number of indents', () => {
      const count = 4;
      wrapper = mount(<TreeItemIndents count={count} />);

      expect(wrapper.find(TreeItemIndent)).toHaveLength(count);
    });

    it('it should return null if count is 0', () => {
      wrapper = mount(<TreeItemIndents count={0} />);

      expect(wrapper.getDOMNode()).toBeNull();
    });
  });
});
