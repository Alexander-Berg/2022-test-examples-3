import { getContainer } from '../get-container';

describe('getContainer', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="div">
        <input id="input" />
      </div>
    `;
  });

  test('should return container by id', () => {
    const divElement = document.getElementById('div');
    const inputElement = document.getElementById('input');

    expect(getContainer('div')).toBe(divElement);
    expect(getContainer('input')).toBe(inputElement);
  });

  test('should return container by container element', () => {
    const divElement = document.getElementById('div') as HTMLElement;
    const inputElement = document.getElementById('input') as HTMLElement;

    expect(getContainer(divElement)).toBe(divElement);
    expect(getContainer(inputElement)).toBe(inputElement);
  });

  test('should return null when getting container with id does not exist', () => {
    const nonExistentContainerId = 'not-exist';

    expect(getContainer(nonExistentContainerId)).toBe(null);
  });
});
