import { getYqlLink } from './links';
import { URLS } from '../constants';

describe('links', () => {
  it.each([
    ['VLeskin is a god of laziness', ['VLeskin'], ['everyone'], 'everyone is a god of laziness'],
    [
      'several {%PLC%} placeholders works correctly {%PLC%}',
      ['{%PLC%}'],
      ['fn'],
      'several fn placeholders works correctly fn',
    ],
    [
      'regexp chars (.*) also should be replaced correctly',
      ['(.*)'],
      ['ok'],
      'regexp chars ok also should be replaced correctly',
    ],
    [
      'different placeholder: {%CHASE%} {%FOR%} {%PLUSES%}',
      ['{%CHASE%}', '{%FOR%}', '{%PLUSES%}'],
      ['is', 'very', 'bad'],
      'different placeholder: is very bad',
    ],
  ])('getYqlLink(%s, %s -> %s) === %s', (template, placeholder, replaceValue, expected) => {
    expect(
      getYqlLink(
        template,
        placeholder.reduce((acc, cur, i) => {
          acc[cur] = replaceValue[i];
          return acc;
        }, {})
      )
    ).toBe(`${URLS.YQL_URL}?query=${encodeURI(expected)}`);
  });
});
