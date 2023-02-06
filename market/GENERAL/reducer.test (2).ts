import { getInstruction, getInstructionFailure, getInstructionSuccess, updateWhBarcode } from './actions';
import reducer, { initialState } from './reducer';

describe('Photographer instruction reducer', () => {
  describe('updateWhBarcode', () => {
    it('should update whBarcode', () => {
      const whBarcode = 'barcode';

      expect(reducer(initialState, updateWhBarcode(whBarcode))).toEqual({
        ...initialState,
        whBarcode,
      });
    });
  });

  describe('getInstruction', () => {
    it('should set loading to true', () => {
      const whBarcode = 'barcode';

      expect(reducer(initialState, getInstruction(whBarcode))).toEqual({
        ...initialState,
        loading: true,
      });
    });
  });

  describe('getInstructionSuccess', () => {
    it('should set loading to false and set instruction and reset whBarcode', () => {
      const instruction = 'url';

      expect(
        reducer(
          {
            ...initialState,
            loading: true,
            whBarcode: 'barcode',
          },
          getInstructionSuccess(instruction)
        )
      ).toEqual({
        ...initialState,
        instruction,
        loading: false,
        whBarcode: '',
      });
    });
  });

  describe('getInstructionFailure', () => {
    it('should set loading to false', () => {
      expect(
        reducer(
          {
            ...initialState,
            loading: true,
          },
          getInstructionFailure()
        )
      ).toEqual({
        ...initialState,
        loading: false,
      });
    });
  });
});
