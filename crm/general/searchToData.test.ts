import searchToData from './searchToData';

describe('searchToData', () => {
  it('should no error on wrong url', () => {
    expect(searchToData('wrong url')).toEqual({ data: {} });
  });

  it("should don't add extra keys", () => {
    expect(searchToData('?accountId=1')).toEqual({ data: { accountId: 1 } });
  });

  it('should parse all props', () => {
    expect(
      searchToData(
        '?accountId=1&categoryId=2&campaignId=3&mailId=4&issueId=5&eid=10&etype=Mail&chatId=chatId_1&orgId=orgId_1&module=Ticket&action=Issue',
      ),
    ).toEqual({
      data: {
        accountId: 1,
        categoryId: 2,
        campaignId: 3,
        mailId: 4,
        issueId: 5,
        byEObject: {
          eid: 10,
          etype: 'Mail',
        },
        byChat: {
          chatId: 'chatId_1',
          orgId: 'orgId_1',
        },
      },
      context: {
        action: 'Issue',
        module: 'Ticket',
      },
    });
  });

  it('should parse email', () => {
    expect(searchToData('?email=email%40email.ru')).toEqual({
      data: {
        email: 'email@email.ru',
      },
    });
  });
});
