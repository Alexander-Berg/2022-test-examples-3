import calcNewOfficesForMembers from '../calcNewOfficesForMembers';

describe('eventForm/utils/calcNewOfficesForMembers', () => {
  test('не должен предлагать переговорки участникам без привязки к офису', () => {
    const login = 'tavria';
    expect(calcNewOfficesForMembers([{login}], {[login]: {isRoomNeeded: true}}, [])).toEqual([]);
  });

  test('не должен предлагать переговорки участникам, которые уже были во встрече', () => {
    const member = {
      login: 'tavria',
      email: 'tavria@yandex-team.ru',
      officeId: 1
    };
    expect(
      calcNewOfficesForMembers([member], {[member.login]: {isRoomNeeded: true}}, [], {
        [member.email]: true
      })
    ).toEqual([]);
  });

  test('не должен предлагать переговорки участникам, для которых isRoomNeeded = false', () => {
    const member = {
      login: 'tavria',
      email: 'tavria@yandex-team.ru',
      officeId: 1
    };
    expect(calcNewOfficesForMembers([member], {[member.login]: {isRoomNeeded: false}}, [])).toEqual(
      []
    );
  });

  test('не должен предлагать переговорки участникам, для которых уже есть переговорка в их офисе', () => {
    const member = {
      login: 'tavria',
      email: 'tavria@yandex-team.ru',
      officeId: 1
    };
    expect(
      calcNewOfficesForMembers([member], {[member.login]: {isRoomNeeded: true}}, [
        {officeId: member.officeId}
      ])
    ).toEqual([]);
  });

  test('должен работать (main case)', () => {
    const member = {
      login: 'tavria',
      email: 'tavria@yandex-team.ru',
      officeId: 1
    };
    expect(calcNewOfficesForMembers([member], {[member.login]: {isRoomNeeded: true}}, [])).toEqual([
      {officeId: member.officeId, resource: null}
    ]);
  });
});
