import { retryValidateRequest } from './retryValidateRequest';

describe('retryValidateRequest', () => {
  const dataWithMessage = {
    upsaleConfirmData: {
      message: 'hello',
    },
  };

  const dataWithoutMessage = {
    id: 1,
    upsaleConfirmData: {},
  };

  describe('when data.confirmFormData.message', () => {
    describe('when user confirms message', () => {
      beforeAll(() => {
        window.confirm = () => true;
      });

      it('calls recursion with arguments', () => {
        const request = jest.fn(
          (needValidate: boolean, strArgument: string, numArgument: number) => {
            retryValidateRequest(dataWithMessage, request, strArgument, numArgument);
          },
        );

        request(true, 'someArgument', 23);

        expect(request).toBeCalledWith(expect.any(Boolean), 'someArgument', 23);
      });

      describe('when data changes depending on needValidate', () => {
        let request;
        beforeEach(() => {
          request = jest.fn((needValidate: boolean = true) => {
            const data = needValidate ? dataWithMessage : dataWithoutMessage;
            return retryValidateRequest(data, request);
          });
        });

        it('runs recursion in 2 calls depth', () => {
          request();

          expect(request).toBeCalledTimes(2);
        });

        it('returns data argument', () => {
          const data = request();

          expect(data.id).toBe(1);
          expect(request).toBeCalledTimes(2);
        });
      });

      describe('when data does not changes at all', () => {
        let request;
        beforeEach(() => {
          request = jest.fn(() => {
            retryValidateRequest(dataWithMessage, request);
          });
        });

        it('runs recursion in 2 calls depth maximum', () => {
          request();

          expect(request).toBeCalledTimes(2);
        });
      });
    });

    describe('when user does not confirm message', () => {
      beforeAll(() => {
        window.confirm = () => false;
      });

      let request;
      beforeEach(() => {
        request = jest.fn(() => {
          return retryValidateRequest(dataWithMessage, request);
        });
      });

      it('does not retry request', () => {
        request();

        expect(request).toBeCalledTimes(1);
      });

      it('does not return data argument', () => {
        const data = request();

        expect(data).toBeUndefined();
        expect(request).toBeCalledTimes(1);
      });
    });
  });

  describe('when !data.confirmData.message', () => {
    let request;
    beforeEach(() => {
      request = jest.fn(() => {
        return retryValidateRequest(dataWithoutMessage, request);
      });
    });

    it('returns data argument', () => {
      const data = request();

      expect(data.id).toBe(1);
      expect(request).toBeCalledTimes(1);
    });
  });
});
