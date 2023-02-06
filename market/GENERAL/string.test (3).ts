import { stringToMultiNumbers } from 'src/utils/string';

describe('string utils', () => {
  it('stringToMultiNumbers', () => {
    expect(stringToMultiNumbers('aqwe qwe 12 sfd45')).toEqual(' 12 45');
    expect(stringToMultiNumbers(' aqwe555qwe 12 sfd45    ')).toEqual(' 555 12 45 ');
    expect(stringToMultiNumbers(' aqwe5qwe12s fd45    ')).toEqual(' 5 12 45 ');
  });
});
