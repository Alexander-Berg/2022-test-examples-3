import { VirtualListMeta } from '../VirtualListMeta';

describe('VirtualListMeta', () => {
  describe('.constructor', function() {
    it('has default value', () => {
      const meta = new VirtualListMeta();

      expect(meta.firstId).toBe(0);
      expect(meta.firstSeqId).toBe(-1);
      expect(meta.lastId).toBe(0);
      expect(meta.lastSeqId).toBe(-1);
    });

    it('supports init by value', () => {
      const meta = new VirtualListMeta({ firstId: 1, firstSeqId: 2, lastId: 3, lastSeqId: 4 });

      expect(meta.firstId).toBe(1);
      expect(meta.firstSeqId).toBe(2);
      expect(meta.lastId).toBe(3);
      expect(meta.lastSeqId).toBe(4);
    });
  });

  describe('.updateFromJson', () => {
    it('updates with new version meta', () => {
      const meta = new VirtualListMeta({ firstId: 1, firstSeqId: 2, lastId: 3, lastSeqId: 4 });

      meta.updateFromJson({ firstId: 5, firstSeqId: 6, lastId: 7, lastSeqId: 8 });

      expect(meta.firstId).toBe(5);
      expect(meta.firstSeqId).toBe(6);
      expect(meta.lastId).toBe(7);
      expect(meta.lastSeqId).toBe(8);
    });

    it('does not update with old version meta', () => {
      const meta = new VirtualListMeta({ firstId: 5, firstSeqId: 6, lastId: 7, lastSeqId: 8 });

      meta.updateFromJson({ firstId: 1, firstSeqId: 2, lastId: 3, lastSeqId: 4 });

      expect(meta.firstId).toBe(5);
      expect(meta.firstSeqId).toBe(6);
      expect(meta.lastId).toBe(7);
      expect(meta.lastSeqId).toBe(8);
    });
  });
});
