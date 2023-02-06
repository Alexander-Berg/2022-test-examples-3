import createGetStartrekLink from '../createGetStartrekLink';

const getStartrekLink = createGetStartrekLink({ local: 'local/', external: 'external/' });

describe('createGetStartrekLink', () => {
  test('local', () => {
    expect(getStartrekLink('CRM-0000')).toBe('local/CRM-0000');
  });

  test('external', () => {
    expect(getStartrekLink('EX:CRM-0000')).toBe('external/CRM-0000');
  });
});
