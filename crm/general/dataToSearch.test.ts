import dataToSearch from './dataToSearch';

describe('dataToSearch', () => {
  it('should no error on wrong url', () => {
    expect(dataToSearch({ data: {} })).toEqual('');
  });

  it("should don't add extra keys", () => {
    expect(dataToSearch({ data: { accountId: 1 } })).toEqual('accountId=1');
  });

  it('should parse all props', () => {
    expect(
      dataToSearch({
        data: {
          accountId: 1,
          categoryId: 2,
          campaignId: '3',
          mailId: 4,
          issueId: 5,
          byEObject: {
            eid: 10,
            etype: 'Mail',
          },
          byChat: {
            chatId: 'chatId_1',
            orgId: 'chatOrgId_1',
          },
        },
        context: { module: 'Ticket' },
      }),
    ).toEqual(
      'accountId=1&categoryId=2&campaignId=3&mailId=4&issueId=5&eid=10&etype=Mail&chatId=chatId_1&orgId=chatOrgId_1&module=Ticket',
    );
  });

  it('should convert email', () => {
    expect(
      dataToSearch({
        data: {
          email: 'email@email.ru',
        },
      }),
    ).toEqual('email=email%40email.ru');
  });
});
