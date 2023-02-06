import { TaskStatus } from '@/apollo/generated/graphql';
import { getAccentColorFromStatus } from '@/components/DeliveryTaskView/utils';
import { ASH_GRAY, GRASS_GREEN } from '@/constants/colors';

describe('DeliveryTaskView/utils', () => {
  describe('getAccentColorFromStatus', () => {
    it('Должна вернуть цвет GRASS_GREEN, если передан TaskStatus.Delivered', () => {
      expect(getAccentColorFromStatus(TaskStatus.Delivered)).toEqual(
        GRASS_GREEN,
      );
    });

    it('Должна вернуть цвет ASH_GRAY, если передан TaskStatus.DeliveryFailed', () => {
      expect(getAccentColorFromStatus(TaskStatus.DeliveryFailed)).toEqual(
        ASH_GRAY,
      );
    });
  });
});
