import { enhanceContainer } from '../enhance-container';

describe('enhanceContainer', () => {
  test('should add property `removeChildren` to HTMLElement', () => {
    const element = document.createElement('div');
    const enhancedElement = enhanceContainer(element);

    expect(typeof enhancedElement?.removeChildren).toBe('function');
  });
});
