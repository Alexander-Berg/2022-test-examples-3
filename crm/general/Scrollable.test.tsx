import React from 'react';
import { render } from '@testing-library/react';
import throttle from 'lodash/throttle';
import { mocked } from 'ts-jest/utils';
import { Scrollable } from './Scrollable';
import { getScrollDirection } from './Scrollable.utils';

jest.mock('./Scrollable.utils', () => {
  const { getScrollDirection } = jest.requireActual('./Scrollable.utils');
  return {
    getScrollDirection: jest.fn(getScrollDirection),
  };
});
const getScrollDirectionMock = mocked(getScrollDirection);

jest.mock('lodash/throttle');
// eslint-disable-next-line @typescript-eslint/no-explicit-any
mocked(throttle).mockImplementation((fn: any) => {
  fn.cancel = jest.fn();
  return fn;
});

describe('Scrollable', () => {
  it('calls getScrollDirection on scroll event', async () => {
    const { container } = render(<Scrollable />);

    const scroll = container.querySelector('.Scrollable__scroll')!;
    expect(getScrollDirectionMock).toBeCalledTimes(1);

    scroll.dispatchEvent(new Event('scroll'));
    expect(getScrollDirectionMock).toBeCalledTimes(2);
  });

  it('has both shadows when scrollDirection is both', () => {
    getScrollDirectionMock.mockReturnValueOnce('both');
    const { container } = render(<Scrollable />);

    const scrollable = container.querySelector('.Scrollable')!;
    expect(scrollable).toHaveClass('Scrollable_top', 'Scrollable_bottom');
  });

  it('has no shadow when scrollDirection is none', () => {
    getScrollDirectionMock.mockReturnValueOnce('none');
    const { container } = render(<Scrollable />);

    const scrollable = container.querySelector('.Scrollable')!;
    expect(scrollable).not.toHaveClass('Scrollable_top');
    expect(scrollable).not.toHaveClass('Scrollable_bottom');
  });

  it('has only bottom shadow when scrollDirection is bottom', () => {
    getScrollDirectionMock.mockReturnValueOnce('bottom');
    const { container } = render(<Scrollable />);

    const scrollable = container.querySelector('.Scrollable')!;
    expect(scrollable).toHaveClass('Scrollable_bottom');
    expect(scrollable).not.toHaveClass('Scrollable_top');
  });

  it('has only top shadow when scrollDirection is top', () => {
    getScrollDirectionMock.mockReturnValueOnce('top');
    const { container } = render(<Scrollable />);

    const scrollable = container.querySelector('.Scrollable')!;
    expect(scrollable).toHaveClass('Scrollable_top');
    expect(scrollable).not.toHaveClass('Scrollable_bottom');
  });
});

describe('getScrollDirection', () => {
  describe('when content is same height as container', () => {
    it('returns "none"', () => {
      const res = getScrollDirection({ scrollTop: 0, clientHeight: 300, scrollHeight: 300 });
      expect(res).toBe('none');
    });
  });

  describe('when content height is bigger than container', () => {
    it('returns "bottom" when scrollTop = 0', () => {
      const res = getScrollDirection({ scrollTop: 0, clientHeight: 100, scrollHeight: 200 });
      expect(res).toBe('bottom');
    });

    it('returns "both" when scrolled to the middle', () => {
      const res = getScrollDirection({ scrollTop: 50, clientHeight: 100, scrollHeight: 200 });
      expect(res).toBe('both');
    });

    it('returns "top" when scrolled to the end', () => {
      const res = getScrollDirection({ scrollTop: 100, clientHeight: 100, scrollHeight: 200 });
      expect(res).toBe('top');
    });
  });
});
