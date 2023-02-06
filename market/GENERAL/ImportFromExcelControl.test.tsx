import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { ImportFromExcelControl } from '.';

describe('components', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<ImportFromExcelControl />', () => {
    it('should be call onUpload', () => {
      const file = {};
      const files = {
        0: file,
        item: jest.fn(() => file),
      };
      const handleUpload = jest.fn();

      wrapper = mount(<ImportFromExcelControl onUpload={handleUpload} />);

      wrapper.find('input').simulate('change', {
        target: {
          files,
        },
      });

      expect(handleUpload).toBeCalledTimes(1);
      expect(handleUpload).toBeCalledWith(file);
    });

    it('should be shown default text', () => {
      wrapper = mount(<ImportFromExcelControl onUpload={jest.fn()}>Загрузить</ImportFromExcelControl>);

      expect(wrapper.text()).toContain('Загрузить');
    });

    it('should be shown text of loading', () => {
      wrapper = mount(<ImportFromExcelControl isLoading onUpload={jest.fn()} />);

      expect(wrapper.text()).toContain('Загрузка файла');
    });

    it('should be shown text of processing', () => {
      wrapper = mount(<ImportFromExcelControl isProcessing onUpload={jest.fn()} />);

      expect(wrapper.text()).toContain('Обработка файла');
    });
  });
});
