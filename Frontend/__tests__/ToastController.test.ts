import { toastController, ToastProps } from '..';

describe('ToastController', () => {
    let listenerMock: jest.Mock<undefined, [Maybe<ToastProps>]>;

    beforeEach(() => {
        listenerMock = jest.fn();
        toastController.onNewToast.addListener(listenerMock);
    });

    afterEach(() => {
        toastController.onNewToast.removeListener(listenerMock);
    });

    describe('#show', () => {
        it('should fire newtoast events', () => {
            const text = 'placeholder';
            toastController.show(text);
            expect(listenerMock.mock.calls[0][0]?.text).toBe(text);
        });

        // Otherwise React will not update the UI
        it('should create new toast objects', () => {
            toastController.show('placeholder1');
            const oldToast = toastController.lastToast;
            toastController.show('placeholder2');
            expect(listenerMock.mock.calls[1][0]).not.toBe(oldToast);
        });
    });

    describe('#hide', () => {
        it('should fire empty events', () => {
            toastController.hide();
            expect(listenerMock).toBeCalledWith(undefined);
        });
    });
});
