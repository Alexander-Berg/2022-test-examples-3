import { getPageDataUrl } from './page-data.utils';

describe('page-data.utils', () => {
  it('getPageDataUrl', () => {
    expect(getPageDataUrl('123', {})).toBe('/page-data?pageId=123');
    // ignore branch params in case isDraft === false
    expect(
      getPageDataUrl('123', {
        branchId: 0,
        branchName: 'madv-101/testik',
      })
    ).toBe('/page-data?pageId=123');

    expect(
      getPageDataUrl(
        '123',
        {
          branchId: 0,
          branchName: 'madv-101/testik',
        },
        true
      )
    ).toBe('/page-data?pageId=123&branch_name=madv-101%2Ftestik&branch_id=0&draft=true');

    expect(
      getPageDataUrl(
        '123',
        {
          branchId: 3232323323323232,
          branchRevisionId: 483463836348,
          revisionId: 1234,
        },
        true
      )
    ).toBe('/page-data?pageId=123&branch_id=3232323323323232&branch_revision_id=483463836348&draft=true');

    expect(getPageDataUrl('123', { branchName: 'qwe' }, true)).toBe('/page-data?pageId=123&branch_name=qwe&draft=true');
  });
});
