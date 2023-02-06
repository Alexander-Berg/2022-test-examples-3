import React from 'react';
import { render } from '@testing-library/react';
import withArray from '../withArray';

const TestComponent = ({ value }) => {
  return <span id={value.id}>{value.text}</span>;
};

describe('withArray', () => {
  describe('single value render', () => {
    it('should render single value with id', () => {
      const WithArray = withArray({ isSupportSingleValue: true })(TestComponent);

      const { container } = render(<WithArray items={{ id: 1, text: 'text' }} />);

      expect(container).toContainHTML('<span id="1">text</span>');
    });

    it('should render single value with zero id', () => {
      const WithArray = withArray({ isSupportSingleValue: true })(TestComponent);

      const { container } = render(<WithArray items={{ id: 0, text: 'text' }} />);

      expect(container).toContainHTML('<span id="0">text</span>');
    });

    it('should not render single item without id prop', () => {
      const WithArray = withArray({ isSupportSingleValue: true })(TestComponent);

      const { container } = render(<WithArray items={{ text: 'text' }} />);

      expect(container).toContainHTML('\u2014');
    });

    it('should not render single item without isSupportSingleValue flag', () => {
      const WithArray = withArray()(TestComponent);

      const { container } = render(<WithArray items={{ id: 1, text: 'text' }} />);

      expect(container).toContainHTML('\u2014');
    });
  });

  describe('array value render', () => {
    it('should render empty without props', () => {
      const WithArray = withArray()(TestComponent);

      const { container } = render(<WithArray />);

      expect(container).toContainHTML('\u2014');
    });

    it('should render empty array', () => {
      const WithArray = withArray()(TestComponent);

      const { container } = render(<WithArray items={[]} />);

      expect(container).toContainHTML('\u2014');
    });

    it('should render array', () => {
      const WithArray = withArray()(TestComponent);

      const { container } = render(
        <WithArray
          items={[
            { id: 1, text: 'text1' },
            { id: 2, text: 'text2' },
          ]}
        />,
      );

      expect(container).toContainHTML(
        '<span id="1">text1</span><span>, </span><span id="2">text2</span>',
      );
    });

    it('should render empty, by default single value not support', () => {
      const WithArray = withArray()(TestComponent);

      const { container } = render(<WithArray items={{ id: 1, text: 'text1' }} />);

      expect(container).toContainHTML('\u2014');
    });
  });

  describe('props through config support check', () => {
    it('should support override value key', () => {
      const TestComponent2 = ({ value2 }) => {
        return <span id={value2.id}>{value2.text}</span>;
      };

      const WithArray = withArray({ slugValue: 'value2' })(TestComponent2);

      const { container } = render(<WithArray items={[{ id: 1, text: 'text1' }]} />);

      expect(container).toContainHTML('<span id="1">text1</span>');
    });

    it('should support override items key', () => {
      const WithArray = withArray({ slugItems: 'items2' })(TestComponent);

      const { container } = render(<WithArray items2={[{ id: 1, text: 'text1' }]} />);

      expect(container).toContainHTML('<span id="1">text1</span>');
    });

    it('should support override getId props', () => {
      const WithArray = withArray({ getId: (item) => item.id2 })(TestComponent);

      const { container } = render(<WithArray items={[{ id: 1, id2: 1, text: 'text' }]} />);

      expect(container).toContainHTML('<span id="1">text</span>');
    });
  });
});
