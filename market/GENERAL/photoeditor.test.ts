import testStore from 'src/helpers/testStore';
import { selectGoodsState, selectPhotoeditorState } from './photoeditor';

const state = testStore().store.getState();

describe('photoeditor selectors', () => {
  describe('selectPhotoeditorState', () => {
    it('should return PhotoeditorState', () => {
      expect(selectPhotoeditorState(state)).toEqual(state.photoeditor);
    });
  });

  describe('selectGoodsState', () => {
    it('should return GoodsState', () => {
      expect(selectGoodsState(state)).toEqual(state.photoeditor.goods);
    });
  });
});
