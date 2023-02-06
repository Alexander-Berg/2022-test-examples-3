import LayerRecord from '../LayerRecord';

describe('LayerRecord', () => {
  describe('hasFullRights', () => {
    test('должен возвращать false, если у пользователя нет прав на редактирование', () => {
      const layer = new LayerRecord({
        isOwner: false
      });

      expect(layer.hasFullRights()).toBe(false);
    });

    test('должен возвращать true, если у пользователя есть права на редактирование', () => {
      const layer = new LayerRecord({
        perm: 'edit',
        isOwner: false
      });

      expect(layer.hasFullRights()).toBe(true);
    });

    test('должен возвращать true, если пользователь явялется владельцем слоя', () => {
      const layer = new LayerRecord({
        isOwner: true
      });

      expect(layer.hasFullRights()).toBe(true);
    });
  });

  describe('isUnlocked', () => {
    test('должен возвращать false, если пользователь не является владельцем', () => {
      const layer = new LayerRecord({isOwner: false});

      expect(layer.isUnlocked()).toBe(false);
    });

    test('должен возвращать false, если нет подписчиков', () => {
      const layer = new LayerRecord({
        isOwner: true,
        participantsCount: 0
      });

      expect(layer.isUnlocked()).toBe(false);
    });

    test('должен возвращать true, если есть подписчики', () => {
      const layer = new LayerRecord({
        isOwner: true,
        participantsCount: 1
      });

      expect(layer.isUnlocked()).toBe(true);
    });
  });

  describe('canDelete', () => {
    test('должен возвращать false, если пользователь не является владельцем', () => {
      const layer = new LayerRecord({
        isOwner: false,
        type: 'user'
      });

      expect(layer.canDelete()).toBe(false);
    });

    test('должен возвращать false, если это не пользовательский тип слоя', () => {
      const layer = new LayerRecord({
        isOwner: true,
        type: 'feed'
      });

      expect(layer.canDelete()).toBe(false);
    });

    test('должен возвращать true, если пользователь является владельцем и слой имеет пользовательский тип', () => {
      const layer = new LayerRecord({
        isOwner: true,
        type: 'user'
      });

      expect(layer.canDelete()).toBe(true);
    });
  });
});
