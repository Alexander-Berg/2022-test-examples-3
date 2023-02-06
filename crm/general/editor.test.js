import $ from 'jquery';
import Editor from './editor';

const CONTAINER_CLASS = 'TestClassname';
const testContainer = $(`<div class="${CONTAINER_CLASS}"></div>`);
const mockEditor = {
  containerClass: CONTAINER_CLASS,
  getContainer: jest.fn(),
  getHtmlContainer: Editor.prototype.getHtmlContainer,
};
describe('Upsale Editor', () => {
  test(`Should return null from 'getHtmlContainer' if Editable and FakeEditableJq not exists`, () => {
    expect(mockEditor.getHtmlContainer()).toEqual(null);
  });

  test(`Should not throw error if editable method not exists`, () => {
    expect(mockEditor.getHtmlContainer.bind(mockEditor)).not.toThrow();
  });

  test(`Should return FakeEditable container from 'getHtmlContainer' if FakeEditableJq exists`, () => {
    const fakeContainer = $(`<div></div>`).append(testContainer);
    expect(mockEditor.getHtmlContainer(fakeContainer)).toEqual(testContainer);
  });

  test(`Should return Editable container from 'getHtmlContainer' if Editable exists`, () => {
    const editableContainer = $(`<div></div>`).append(testContainer)[0];
    mockEditor.instance = { editable: () => ({ $: editableContainer }) };
    mockEditor.getContainer.mockReturnValueOnce(testContainer);
    expect(mockEditor.getHtmlContainer()).toEqual(testContainer);
  });
});
