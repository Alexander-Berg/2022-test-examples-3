import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { ChangesDialog } from '.';
import { CommentForm } from './forms';

describe('components', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<ChangesDialog />', () => {
    it('should be render null', () => {
      wrapper = mount(<ChangesDialog changesCount={0} onSave={jest.fn()} onDiscard={jest.fn()} />);

      expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('should be render dialog', () => {
      wrapper = mount(<ChangesDialog changesCount={1} onSave={jest.fn()} onDiscard={jest.fn()} />);

      expect(wrapper.isEmptyRender()).toBe(false);
    });

    it('should call onSave with comment', () => {
      const handleSave = jest.fn();
      const handleDiscard = jest.fn();
      const commentText = 'Hello';
      wrapper = mount(
        <ChangesDialog form={CommentForm} changesCount={1} onSave={handleSave} onDiscard={handleDiscard} />
      );
      wrapper.find('textarea[name="comment"]').simulate('change', { target: { value: commentText } });
      wrapper.findWhere(node => node.key() === 'Сохранить').simulate('click');

      expect(handleSave).toHaveBeenCalledWith(commentText);
    });

    it('should be call onDiscard', () => {
      const handleSave = jest.fn();
      const handleDiscard = jest.fn();

      wrapper = mount(<ChangesDialog changesCount={1} onSave={handleSave} onDiscard={handleDiscard} />);
      wrapper.findWhere(node => node.key() === 'Отменить').simulate('click');

      expect(handleDiscard).toHaveBeenCalledTimes(1);
      expect(handleSave).toHaveBeenCalledTimes(0);
    });

    it('it should be pluralized text', () => {
      wrapper = mount(<ChangesDialog changesCount={1} onSave={jest.fn()} onDiscard={jest.fn()} />);

      expect(wrapper.text()).toContain('1 несохраненное изменение');

      wrapper.setProps({ changesCount: 2 });

      expect(wrapper.text()).toContain('2 несохраненных изменения');

      wrapper.setProps({ changesCount: 5 });

      expect(wrapper.text()).toContain('5 несохраненных изменений');
    });
  });
});
