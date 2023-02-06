import * as api from 'entry/api/common';
import { ETypeString } from 'types/entities';
import { PinFormModal } from '../PinFormModal';
import { createPinFormSubmit, showPinModal } from '../PinForm.service';

jest.mock('entry/api/common', () => ({
  jsonApiCall: jest.fn(() => {
    return Promise.resolve();
  }),
}));
jest.mock('../PinFormModal', () => ({
  PinFormModal: {
    open: jest.fn(),
  },
}));

describe('PinForm.service', () => {
  beforeEach(() => {
    window.yaCounterHit = jest.fn();
  });

  describe('createPinFormSubmit', () => {
    it('returns onSubmit handler', () => {
      const formSubmit = createPinFormSubmit({
        pinTarget: {
          eid: 123,
          etype: ETypeString.Issue,
        },
      });
      expect(formSubmit).toBeInstanceOf(Function);
    });

    describe('when callId is not presented', () => {
      const formSubmit = jest.fn(
        createPinFormSubmit({
          pinTarget: {
            eid: 123,
            etype: ETypeString.Issue,
          },
        }),
      );

      it('fetches server with proper scheme', () => {
        const promise = formSubmit({
          pin: 'pin',
        });

        expect(promise).toBeInstanceOf(Promise);

        expect(api.jsonApiCall).toBeCalledWith(
          expect.objectContaining({
            data: {
              pin: 'pin',
              pinTarget: {
                etype: ETypeString.Issue,
                eid: 123,
              },
            },
          }),
        );
      });
    });

    describe('when callId is presented', () => {
      const formSubmit = jest.fn(
        createPinFormSubmit({
          pinSource: {
            eid: 321,
            etype: ETypeString.YcCall,
          },
          pinTarget: {
            eid: 123,
            etype: ETypeString.Issue,
          },
        }),
      );

      it('fetches server with proper scheme', () => {
        formSubmit({
          pin: 'pin',
        });

        expect(api.jsonApiCall).toBeCalledWith(
          expect.objectContaining({
            data: {
              pin: 'pin',
              pinSource: {
                etype: ETypeString.YcCall,
                eid: 321,
              },
              pinTarget: {
                etype: ETypeString.Issue,
                eid: 123,
              },
            },
          }),
        );
      });
    });
  });

  describe('showPinModal', () => {
    it('calls PinForm.open', () => {
      showPinModal({
        pinTarget: {
          eid: 123,
          etype: ETypeString.Issue,
        },
      });

      expect(PinFormModal.open).toBeCalledTimes(1);
    });
  });
});
