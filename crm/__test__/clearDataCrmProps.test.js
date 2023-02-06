import clearDataCrmProps from '../clearDataCrmProps';

describe('clear data crm props', () => {
  test('one', () => {
    expect(clearDataCrmProps('<div class="1" data-crm-foo="bar"></div>')).toEqual('<div class="1"></div>');
  });

  test('multy', () => {
    expect(clearDataCrmProps('<div data-crm-foo="bar" data-crm-bar></div>')).toEqual('<div></div>');
  });

  test('nested', () => {
    expect(clearDataCrmProps('<div data-crm-foo="bar" data-crm-bar><div data-crm></div></div>'))
      .toEqual('<div><div></div></div>');
  });

  test('clear class crm-editor-factor', () => {
    expect(clearDataCrmProps('<div class="crm-editor-factor"></div>'))
      .toEqual('<div class=""></div>');
  });

  test('clear class spSignature', () => {
    expect(clearDataCrmProps('<div class="spSignature"></div>'))
      .toEqual('<div class=""></div>');
  });
});
