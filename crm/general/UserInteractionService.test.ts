import { ModalView, UserInteractionService } from './UserInteractionService';

const confirm = jest.fn();

const openWithConfirm = ({ onSubmitSuccess }) => {
  onSubmitSuccess(true);
};

const openWithCancel = ({ onClose }) => {
  onClose();
};

const ViewStub: ModalView = {
  open: jest.fn(),
};

describe('UserInteractionService', () => {
  beforeAll(() => {
    window.confirm = confirm;
  });

  describe('when confirmView is not set', () => {
    let userInteractionService;
    beforeEach(() => {
      userInteractionService = new UserInteractionService();
    });

    describe('when is confirmed', () => {
      beforeEach(() => {
        confirm.mockReturnValueOnce(true);
      });

      it('returns true', async () => {
        const isConfirmed = await userInteractionService.confirm({ title: 'Test' });

        expect(confirm).toBeCalled();
        expect(isConfirmed).toEqual(true);
      });
    });

    describe('when is not confirmed', () => {
      beforeEach(() => {
        confirm.mockReturnValueOnce(false);
      });
      afterEach(() => {
        jest.clearAllMocks();
      });

      it('returns false', async () => {
        const userInteractionService = new UserInteractionService();

        const isConfirmed = await userInteractionService.confirm({ title: 'Test' });

        expect(confirm).toBeCalled();
        expect(isConfirmed).toEqual(false);
      });
    });
  });

  describe('when confirmView is set', () => {
    let userInteractionService;
    beforeEach(() => {
      userInteractionService = new UserInteractionService();
      userInteractionService.setConfirmView(ViewStub);
    });

    describe('when confirm is called', () => {
      it('calls confirmView open function', () => {
        userInteractionService.confirm({ title: 'Test' });

        expect(ViewStub.open).toBeCalledTimes(1);
      });
    });

    describe('when is confirmed', () => {
      beforeEach(() => {
        ViewStub.open = openWithConfirm;
      });

      it('returns true', async () => {
        const isConfirmed = await userInteractionService.confirm({ title: 'Test' });

        expect(isConfirmed).toEqual(true);
      });
    });

    describe('when is not confirmed', () => {
      beforeEach(() => {
        ViewStub.open = openWithCancel;
      });

      it('returns false', async () => {
        const isConfirmed = await userInteractionService.confirm({ title: 'Test' });

        expect(isConfirmed).toEqual(false);
      });
    });
  });
});
