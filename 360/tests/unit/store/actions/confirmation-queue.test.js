import { add, decideSelectAlbum, getActiveConfirmationOperation } from '../../../../components/redux/store/actions/confirmation-queue';
import { popFnCalls } from '../../helpers/pop-fn-calls';

jest.mock('../../../../components/redux/store/actions/dialogs', () => ({
    openDialog: jest.fn(),
    closeDialog: jest.fn(),
}));
import { openDialog, closeDialog } from '../../../../components/redux/store/actions/dialogs';

const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch) : arg);
const deleteFunctionProps = (opts) => {
    Object.keys(opts).forEach((key) => {
        if (typeof opts[key] === 'function') {
            delete opts[key];
        }
    });
};

describe('confirmation-queue', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('confirmation', () => {
        it('should open dialog after add confirmation', () => {
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? undefined : key.replace(/^\./, '')
                },
                dialogParams: {
                    title: 'test title',
                    text: 'test text'
                }
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            const onReject = openDialogCalls[0][1].onReject;
            deleteFunctionProps(openDialogCalls[0][1]);
            expect(openDialogCalls).toEqual([['confirmation', {
                isGroup: false,
                title: 'test title',
                text: 'test text'
            }]]);
            // сбросим подтверждение чтобы можно было добавить новое в очередь
            onReject();
        });

        it('should call onSubmit callback', () => {
            const onSubmit = jest.fn();
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? undefined : key.replace(/^\./, '')
                },
                onSubmit
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onSubmit();
            expect(closeDialog).toBeCalledWith('confirmation');
            expect(onSubmit).toBeCalled();
        });

        it('should call onReject callback', () => {
            const onReject = jest.fn();
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? undefined : key.replace(/^\./, '')
                },
                onReject
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onReject();
            expect(closeDialog).toBeCalledWith('confirmation');
            expect(onReject).toBeCalled();
        });

        it('group operation / onSubmitAll - should call onSubmit callback for every operation of group', () => {
            const onSubmit1 = jest.fn();
            const onSubmit2 = jest.fn();
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 1 : key.replace(/^\./, '')
                },
                onSubmit: onSubmit1
            })(dispatch);
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 1 : key.replace(/^\./, '')
                },
                onSubmit: onSubmit2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onSubmitAll();
            expect(closeDialog).toBeCalledWith('confirmation');
            expect(onSubmit1).toBeCalled();
            expect(onSubmit2).toBeCalled();
        });

        it('group operation / onRejectAll - should call onReject callback for every operation of group', () => {
            const onReject1 = jest.fn();
            const onReject2 = jest.fn();
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 2 : key.replace(/^\./, '')
                },
                onReject: onReject1
            })(dispatch);
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 2 : key.replace(/^\./, '')
                },
                onReject: onReject2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onRejectAll();
            expect(closeDialog).toBeCalledWith('confirmation');
            expect(onReject1).toBeCalled();
            expect(onReject2).toBeCalled();
        });

        it('group operation / onSubmit - should call onSubmit callback only for first operation and open dialog for second', () => {
            const onSubmit1 = jest.fn();
            const onSubmit2 = jest.fn();
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 3 : key.replace(/^\./, '')
                },
                onSubmit: onSubmit1
            })(dispatch);
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 3 : key.replace(/^\./, '')
                },
                onSubmit: onSubmit2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            openDialog.mock.calls[0][1].onSubmit();
            expect(closeDialog).toBeCalledWith('confirmation');
            expect(onSubmit1).toBeCalled();
            expect(onSubmit2).not.toBeCalled();
            expect(openDialog).toBeCalledTimes(2);

            // сбросим вторую операцию чтобы очистить очередь для следующих тестов
            openDialog.mock.calls[1][1].onReject();
        });

        it('group operation / onReject - should call onReject callback only for first operation and open dialog for second', () => {
            const onReject1 = jest.fn();
            const onReject2 = jest.fn();
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 4 : key.replace(/^\./, '')
                },
                onReject: onReject1
            })(dispatch);
            add('confirmation', {
                operation: {
                    get: (key) => key === '.gid' ? 4 : key.replace(/^\./, '')
                },
                onReject: onReject2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            openDialog.mock.calls[0][1].onReject();
            expect(closeDialog).toBeCalledWith('confirmation');
            expect(onReject1).toBeCalled();
            expect(onReject2).not.toBeCalled();
            expect(openDialog).toBeCalledTimes(2);

            // сбросим вторую операцию чтобы очистить очередь для следующих тестов
            openDialog.mock.calls[1][1].onReject();
        });
    });

    describe('prompt', () => {
        const originalDateNow = Date.now;
        beforeEach(() => {
            Date.now = () => 1571671403618;
        });
        afterEach(() => {
            Date.now = originalDateNow;
        });

        it('should open dialog after add propmt', () => {
            add('prompt', {
                operation: {
                    get: () => {}
                },
                dialogParams: {
                    title: 'test title',
                    submitButtonText: 'test submit button text'
                }
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            const onClose = openDialogCalls[0][1].onClose;
            deleteFunctionProps(openDialogCalls[0][1]);
            expect(openDialogCalls).toEqual([['rename', {
                title: 'test title',
                submitButtonText: 'test submit button text'
            }]]);
            // сбросим подтверждение чтобы можно было добавить новое в очередь
            onClose();
        });

        it('should call onSubmit callback', () => {
            const onSubmit = jest.fn();
            add('prompt', {
                operation: {
                    get: () => {}
                },
                onSubmit
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onSubmit();
            expect(closeDialog).toBeCalledWith('rename');
            expect(onSubmit).toBeCalled();
        });

        it('should call onCancel callback', () => {
            const onReject = jest.fn();
            add('prompt', {
                operation: {
                    get: () => {}
                },
                onReject
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onClose();
            expect(closeDialog).toBeCalledWith('rename');
            expect(onReject).toBeCalled();
        });
    });

    describe('select-folder', () => {
        const getOperation = (gid) => ({
            get: (key) => key === '.gid' ? gid : key.replace(/^\./, ''),
            getType: () => {}
        });

        it('should open dialog after add select-folder', () => {
            add('select-folder', {
                operation: getOperation()
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            const onCancel = openDialogCalls[0][1].onCancel;
            deleteFunctionProps(openDialogCalls[0][1]);
            expect(openDialogCalls).toEqual([['selectFolder', {
                folderId: '/disk'
            }]]);
            // сбросим диалог чтобы можно было добавить новый в очередь
            onCancel();
        });

        it('should call onSubmit callback for every operation of group', () => {
            const onSubmit1 = jest.fn();
            const onSubmit2 = jest.fn();
            add('select-folder', {
                operation: getOperation(5),
                onSubmit: onSubmit1
            })(dispatch);
            add('select-folder', {
                operation: getOperation(5),
                onSubmit: onSubmit2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onSubmit({});
            expect(closeDialog).toBeCalledWith('selectFolder');
            expect(onSubmit1).toBeCalled();
            expect(onSubmit2).toBeCalled();
        });

        it('should call onReject callback for every operation of group', () => {
            const onReject1 = jest.fn();
            const onReject2 = jest.fn();
            add('select-folder', {
                operation: getOperation(6),
                onReject: onReject1
            })(dispatch);
            add('select-folder', {
                operation: getOperation(6),
                onReject: onReject2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onCancel();
            expect(closeDialog).toBeCalledWith('selectFolder');
            expect(onReject1).toBeCalled();
            expect(onReject2).toBeCalled();
        });
    });

    describe('select-album', () => {
        const getOperation = (gid) => ({
            get: (key) => key === '.gid' ? gid : key.replace(/^\./, ''),
            getType: () => 'addToAlbum'
        });

        it('should open dialog after add select-album', () => {
            add('select-album', {
                operation: getOperation()
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            const onCancel = openDialogCalls[0][1].onCancel;
            deleteFunctionProps(openDialogCalls[0][1]);
            expect(openDialogCalls).toEqual([['selectAlbum', {}]]);
            // сбросим диалог чтобы можно было добавить новый в очередь
            onCancel();
        });

        it('should call onReject callback for every operation of group', (done) => {
            const onReject1 = jest.fn();
            const onReject2 = jest.fn();
            add('select-album', {
                operation: getOperation(7),
                onReject: onReject1
            })(dispatch);
            add('select-album', {
                operation: getOperation(7),
                onReject: onReject2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            const openDialogCalls = popFnCalls(openDialog);
            openDialogCalls[0][1].onCancel();
            expect(onReject1).toBeCalled();
            expect(onReject2).toBeCalled();
            setTimeout(() => {
                expect(closeDialog).toBeCalledWith('selectAlbum');
                done();
            }, 42);
        });

        it('should call onSubmit callback for every operation of group after positive decideSelectAlbum dispatch', (done) => {
            const onSubmit1 = jest.fn();
            const onSubmit2 = jest.fn();
            const onSubmit3 = jest.fn();
            add('select-album', {
                operation: getOperation(8),
                onSubmit: onSubmit1
            })(dispatch);
            add('select-album', {
                operation: getOperation(8),
                onSubmit: onSubmit2
            })(dispatch);
            add('select-album', {
                operation: getOperation(8),
                onSubmit: onSubmit3
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            dispatch(decideSelectAlbum(true, 'test-album-id'));
            expect(onSubmit1).toBeCalled();
            expect(onSubmit2).toBeCalled();
            expect(onSubmit3).toBeCalled();
            setTimeout(() => {
                expect(closeDialog).toBeCalledWith('selectAlbum');
                done();
            }, 42);
        });

        it('should call onReject callback for every operation of group after negative decideSelectAlbum dispatch', (done) => {
            const onReject1 = jest.fn();
            const onReject2 = jest.fn();
            add('select-album', {
                operation: getOperation(9),
                onReject: onReject1
            })(dispatch);
            add('select-album', {
                operation: getOperation(9),
                onReject: onReject2
            })(dispatch);
            expect(openDialog).toBeCalledTimes(1);
            dispatch(decideSelectAlbum(false));
            expect(onReject1).toBeCalled();
            expect(onReject2).toBeCalled();
            setTimeout(() => {
                expect(closeDialog).toBeCalledWith('selectAlbum');
                done();
            }, 42);
        });
    });

    it('getActiveConfirmationOperation - should return confirmationParams', () => {
        const operation = {
            get: () => {},
            getType: () => {}
        };
        add('select-folder', {
            operation
        })(dispatch);
        expect(getActiveConfirmationOperation()).toBe(operation);

        expect(openDialog).toBeCalledTimes(1);
        const openDialogCalls = popFnCalls(openDialog);
        // сбросим диалог чтобы очистить очередь
        openDialogCalls[0][1].onCancel();
    });
});
