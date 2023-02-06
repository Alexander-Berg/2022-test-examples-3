import {
  scrollIntoViewIfNeeded,
  getScrollableParent,
  getVenderedProp,
  setVenderedProp
} from '../dom';

describe('dom', () => {
  describe('getScrollableParent', () => {
    beforeEach(() => {
      jest.spyOn(window, 'getComputedStyle');
    });

    test('должен возвращать document, если он является родителем', () => {
      window.getComputedStyle.mockReturnValue({});

      const node = {
        parentNode: document
      };

      expect(getScrollableParent(node)).toEqual(document);
    });

    test('должен возвращать родителя, если у него overflowY = auto', () => {
      window.getComputedStyle.mockReturnValue({
        overflowY: 'auto',
        overflowX: 'hidden'
      });

      const node = {
        parentNode: {}
      };

      expect(getScrollableParent(node)).toEqual(node.parentNode);
    });

    test('должен возвращать родителя, если у него overflowY = scroll', () => {
      window.getComputedStyle.mockReturnValue({
        overflowY: 'scroll',
        overflowX: 'hidden'
      });

      const node = {
        parentNode: {}
      };

      expect(getScrollableParent(node)).toEqual(node.parentNode);
    });

    test('должен возвращать родителя, если у него overflowX = auto', () => {
      window.getComputedStyle.mockReturnValue({
        overflowY: 'hidden',
        overflowX: 'auto'
      });

      const node = {
        parentNode: {}
      };

      expect(getScrollableParent(node)).toEqual(node.parentNode);
    });

    test('должен возвращать родителя, если у него overflowX = scroll', () => {
      window.getComputedStyle.mockReturnValue({
        overflowY: 'hidden',
        overflowX: 'scroll'
      });

      const node = {
        parentNode: {}
      };

      expect(getScrollableParent(node)).toEqual(node.parentNode);
    });

    test('должен искать глубже, пока не дойдет до подходящего родителя или document', () => {
      const node = {
        parentNode: {
          parentNode: {}
        }
      };

      window.getComputedStyle
        .mockReturnValueOnce({
          overflowY: 'hidden',
          overflowX: 'hidden'
        })
        .mockReturnValueOnce({
          overflowY: 'scroll',
          overflowX: 'hidden'
        });

      expect(getScrollableParent(node)).toEqual(node.parentNode.parentNode);
    });
  });

  describe('scrollIntoViewIfNeeded', () => {
    beforeEach(() => {
      jest.spyOn(window, 'getComputedStyle').mockReturnValue({
        overflowY: 'scroll',
        overflowX: 'hidden'
      });
    });

    test('должен вызвать скролл, если верхний край элемента находится за пределами родителя', () => {
      const node = {
        getBoundingClientRect() {
          return {
            top: 100,
            bottom: 0,
            left: 0,
            right: 0
          };
        },
        parentNode: {
          getBoundingClientRect() {
            return {
              top: 200,
              bottom: 0,
              left: 0,
              right: 0
            };
          }
        },
        scrollIntoView: jest.fn()
      };

      scrollIntoViewIfNeeded(node);

      expect(node.scrollIntoView).toHaveBeenCalledTimes(1);
    });

    test('должен вызвать скролл, если нижний край элемента находится за пределами родителя', () => {
      const node = {
        getBoundingClientRect() {
          return {
            top: 0,
            bottom: 200,
            left: 0,
            right: 0
          };
        },
        parentNode: {
          getBoundingClientRect() {
            return {
              top: 0,
              bottom: 100,
              left: 0,
              right: 0
            };
          }
        },
        scrollIntoView: jest.fn()
      };

      scrollIntoViewIfNeeded(node);

      expect(node.scrollIntoView).toHaveBeenCalledTimes(1);
    });

    test('должен вызвать скролл, если левый край элемента находится за пределами родителя', () => {
      const node = {
        getBoundingClientRect() {
          return {
            top: 0,
            bottom: 0,
            left: 100,
            right: 0
          };
        },
        parentNode: {
          getBoundingClientRect() {
            return {
              top: 0,
              bottom: 0,
              left: 200,
              right: 0
            };
          }
        },
        scrollIntoView: jest.fn()
      };

      scrollIntoViewIfNeeded(node);

      expect(node.scrollIntoView).toHaveBeenCalledTimes(1);
    });

    test('должен вызвать скролл, если правый край элемента находится за пределами родителя', () => {
      const node = {
        getBoundingClientRect() {
          return {
            top: 0,
            bottom: 0,
            left: 0,
            right: 200
          };
        },
        parentNode: {
          getBoundingClientRect() {
            return {
              top: 0,
              bottom: 0,
              left: 0,
              right: 100
            };
          }
        },
        scrollIntoView: jest.fn()
      };

      scrollIntoViewIfNeeded(node);

      expect(node.scrollIntoView).toHaveBeenCalledTimes(1);
    });

    test('не должен вызывать скролл, если элемент не выходит за пределы родителя', () => {
      const node = {
        getBoundingClientRect() {
          return {
            top: 0,
            bottom: 0,
            left: 0,
            right: 0
          };
        },
        parentNode: {
          getBoundingClientRect() {
            return {
              top: 0,
              bottom: 0,
              left: 0,
              right: 0
            };
          }
        },
        scrollIntoView: jest.fn()
      };

      scrollIntoViewIfNeeded(node);

      expect(node.scrollIntoView).toHaveBeenCalledTimes(0);
    });
  });

  describe('getVenderedProp', () => {
    test('должен возвращать свойство с браузерными префиксами', () => {
      expect(getVenderedProp('transform', 'translateY(0)')).toEqual({
        transform: 'translateY(0)',
        WebkitTransform: 'translateY(0)',
        MozTransform: 'translateY(0)',
        MsTransform: 'translateY(0)',
        OTransform: 'translateY(0)'
      });
    });
  });

  describe('setVenderedProp', () => {
    test('должен добавлять элементу свойство с браузерными префиксами', () => {
      const element = {
        style: {}
      };

      setVenderedProp(element, 'transform', 'translateY(0)');

      expect(element.style).toEqual({
        transform: 'translateY(0)',
        WebkitTransform: 'translateY(0)',
        MozTransform: 'translateY(0)',
        MsTransform: 'translateY(0)',
        OTransform: 'translateY(0)'
      });
    });
  });
});
