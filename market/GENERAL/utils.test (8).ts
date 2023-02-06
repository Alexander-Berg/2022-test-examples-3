import { formatMessageWithParams } from './utils';

const multiTemplate = 'Параметры {{#skuParameters}}{{.}}, {{/skuParameters}} скю {{#sku}}{{.}},{{/sku}}';
const multiTemplateParams = [
  { name: 'skuParameters', value: '94022' },
  { name: 'skuParameters', value: '1212' },
  { name: 'sku', value: '112' },
  { name: 'sku', value: '113' },
];

const template = 'Категория {{category}} параметр {{param}}';
const templateParams = [
  { name: 'category', value: '94022' },
  { name: 'param', value: '1212' },
];

describe('verdict utils', () => {
  test('formatMessageWithParams', () => {
    const message = formatMessageWithParams(template, templateParams);
    expect(message).toBe('Категория 94022 параметр 1212');
  });

  test('formatMessageWithParams multiparams', () => {
    const message = formatMessageWithParams(multiTemplate, multiTemplateParams);
    expect(message).toBe('Параметры 94022, 1212 скю 112, 113');
  });
});
