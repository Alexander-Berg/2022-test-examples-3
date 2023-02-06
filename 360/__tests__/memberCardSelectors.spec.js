import {makeGetMemberCard} from '../memberCardSelectors';

describe('memberCardSelectors', () => {
  describe('getMemberCard', () => {
    const getMemberCard = makeGetMemberCard();
    test('должен возвращать данные о состоянии карточки для переданного логина', () => {
      const login = 'pistch';
      const memberCardData = {
        isLoading: false,
        member: {login}
      };
      const state = {
        memberCard: {
          pistch: memberCardData
        }
      };

      expect(getMemberCard(state, {login})).toEqual(memberCardData);
    });
    test('должен возвращать пустой объект, если для переданного логина нет данных', () => {
      const login = 'loginNotFound';
      const memberCardData = {
        isLoading: false,
        member: {login}
      };
      const state = {
        memberCard: {
          pistch: memberCardData
        }
      };

      expect(getMemberCard(state, {login})).toEqual({});
    });
  });
});
