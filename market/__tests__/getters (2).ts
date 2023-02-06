import { getOptionId, getOptionName } from 'src/entities/option/getters';
import { Option } from 'src/entities/option/types';

const option: Option = {
  id: 1,
  name: 'Название опции',
};

describe('src/entities/option', () => {
  describe('getters', () => {
    test('getOptionId должен вернуть id опции', () => {
      expect(getOptionId(option)).toEqual(option.id);
    });

    test('getCategoryName должен вернуть название опции', () => {
      expect(getOptionName(option)).toEqual(option.name);
    });
  });
});
