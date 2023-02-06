import { getLockErrorMessage, getLockErrorMessageParts } from '../../../components/helpers/locks';

const messageLengthTitles = new Map([[true, '(длинная формулировка)'], [false, '(короткая формулировка)']]);

const resourcePaths = {
    file1: '/disk/folder/file',
    file2: '/disk/other_folder/file',
    folder1: '/disk/folder',
    folder2: '/disk/other_folder',
    trash: '/trash',
    fileInTrash: '/trash/file'
};

const uids = {
    my: '007',
    other: '001'
};

const operationTitlesForCases = {
    copy: 'копировании',
    move: 'перемещении',
    delete: 'удалении',
    rename: 'переименовании',
    upload: 'загрузке',
    publish: 'публикации',
    unshare: 'удалении общего доступа',
    cleanTrash: 'очистке корзины',
    leaveFolder: 'отказе от общего доступа',
    acceptInvite: 'принятии приглашения',
    accessFolder: 'настройке доступа',
    createFolder: 'создании папки',
    inviteFolder: 'приглашении',
    uploadFolder: 'загрузке папки'
};

describe('Метод `getLockErrorMessage`', () => {
    // лок перемещения
    describe('должен выводить правильное сообщение при', () => {
        const commonOperations = ['copy', 'move', 'delete', 'rename', 'publish'];
        const folderOperations = ['upload', 'createFolder', 'uploadFolder'];
        const cases = [
            {
                title: 'ресурса, перемещаемого мной',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса, перемещаемого другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'папки с ресурсом, перемещаемым мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'папки с ресурсом, перемещаемым другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'ресурса, находящегося в перемещаемой мной папке',
                target: resourcePaths.file1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса, находящегося в папке, перемещаемой другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'ресурса в папку, перемещаемую мной',
                target: resourcePaths.file1,
                locked: resourcePaths.folder2,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса в папку, перемещаемую другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.folder2,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'в папку, перемещаемую мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: folderOperations
            },
            {
                title: 'в папку, перемещаемую другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: folderOperations
            },
            {
                title: 'в папку, чья родительская папка перемещается мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: folderOperations
            },
            {
                title: 'в папку, чья родительская папка перемещается другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: folderOperations
            },
            {
                title: 'в перемещаемую общую папку',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['unshare']
            },
            {
                title: 'в общую папку, перемещаемую владельцем',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: ['leaveFolder']
            },
            {
                title: 'в общую папку, перемещаемую владельцем',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: ['acceptInvite']
            },
            {
                title: 'перемещаемой общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['accessFolder']
            },
            {
                title: 'в перемещаемую общую папку',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['inviteFolder']
            }
        ];

        cases.forEach((moveCase) => {
            moveCase.operations.forEach((failedOperationType) => {
                for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                    const caseTitle = [
                        operationTitlesForCases[failedOperationType],
                        moveCase.title,
                        messageLengthTitle
                    ].join(' ');

                    it(caseTitle, () => {
                        const message = getLockErrorMessage({
                            myUid: uids.my,
                            isLong: isLong,
                            lockType: 'move_resource',
                            lockInitiatorUid: moveCase.uid,
                            lockedResourcePath: moveCase.locked,
                            targetResourcePath: moveCase.target,
                            failedOperationType: failedOperationType
                        });

                        expect(message).toMatchSnapshot();
                    });
                }
            });
        });
    });

    // лок копирования
    describe('должен выводить правильное сообщение при', () => {
        const commonOperations = ['copy', 'move', 'delete', 'rename', 'publish'];
        const folderOperations = ['upload', 'createFolder', 'uploadFolder'];
        const cases = [
            {
                title: 'ресурса, копируемого мной',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса, копируемого другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'папки с ресурсом, копируемым мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'папки с ресурсом, копируемым другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'ресурса, находящегося в копируемой мной папке',
                target: resourcePaths.file1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса, находящегося в папке, копируемой другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'ресурса в папку, копируемую мной',
                target: resourcePaths.file1,
                locked: resourcePaths.folder2,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса в папку, копируемую другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.folder2,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'в папку, копируемую мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: folderOperations
            },
            {
                title: 'в папку, копируемую другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: folderOperations
            },
            {
                title: 'в папку, чья родительская папка копируется мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: folderOperations
            },
            {
                title: 'в папку, чья родительская папка копируется другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: folderOperations
            },
            {
                title: 'в копируемую общую папку',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['unshare']
            },
            {
                title: 'в общую папку, копируемую владельцем',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: ['leaveFolder']
            },
            {
                title: 'в общую папку, копируемую владельцем',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: ['acceptInvite']
            },
            {
                title: 'копируемой общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['accessFolder']
            },
            {
                title: 'в копируемую общую папку',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['inviteFolder']
            }
        ];

        cases.forEach((copyCase) => {
            copyCase.operations.forEach((failedOperationType) => {
                for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                    const caseTitle = [
                        operationTitlesForCases[failedOperationType],
                        copyCase.title,
                        messageLengthTitle
                    ].join(' ');

                    it(caseTitle, () => {
                        const message = getLockErrorMessage({
                            myUid: uids.my,
                            isLong: isLong,
                            lockType: 'copy_resource',
                            lockInitiatorUid: copyCase.uid,
                            lockedResourcePath: copyCase.locked,
                            targetResourcePath: copyCase.target,
                            failedOperationType: failedOperationType
                        });

                        expect(message).toMatchSnapshot();
                    });
                }
            });
        });
    });

    // лок добавления в корзину
    describe('должен выводить правильное сообщение при', () => {
        const commonOperations = ['copy', 'move', 'delete', 'rename', 'publish'];
        const folderOperations = ['upload', 'createFolder', 'uploadFolder'];
        const cases = [
            {
                title: 'ресурса, удаляемого мной',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса, удаляемого другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'папки с ресурсом, удаляемым мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'папки с ресурсом, удаляемым другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'ресурса, находящегося в удаляемой мной папке',
                target: resourcePaths.file1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса, находящегося в папке, удаляемой другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'ресурса в папку, удаляемую мной',
                target: resourcePaths.file1,
                locked: resourcePaths.folder2,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'ресурса в папку, удаляемую другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.folder2,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'в папку, удаляемую мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: folderOperations
            },
            {
                title: 'в папку, удаляемую другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: folderOperations
            },
            {
                title: 'в папку, чья родительская папка удаляется мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: folderOperations
            },
            {
                title: 'в папку, чья родительская папка удаляется другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: folderOperations
            },
            {
                title: 'в удаляемую общую папку',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['unshare']
            },
            {
                title: 'в общую папку, удаляемую владельцем',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: ['leaveFolder']
            },
            {
                title: 'в общую папку, удаляемую владельцем',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.other,
                operations: ['acceptInvite']
            },
            {
                title: 'удаляемой общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['accessFolder']
            },
            {
                title: 'в удаляемую общую папку',
                target: resourcePaths.folder1,
                locked: resourcePaths.folder1,
                uid: uids.my,
                operations: ['inviteFolder']
            },
            {
                title: 'в момент перемещения туда файла',
                target: resourcePaths.trash,
                locked: resourcePaths.fileInTrash,
                uid: uids.my,
                operations: ['cleanTrash']
            }
        ];

        cases.forEach((deleteCase) => {
            deleteCase.operations.forEach((failedOperationType) => {
                for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                    const caseTitle = [
                        operationTitlesForCases[failedOperationType],
                        deleteCase.title,
                        messageLengthTitle
                    ].join(' ');

                    it(caseTitle, () => {
                        const message = getLockErrorMessage({
                            myUid: uids.my,
                            isLong: isLong,
                            lockType: 'trash_append',
                            lockInitiatorUid: deleteCase.uid,
                            lockedResourcePath: deleteCase.locked,
                            targetResourcePath: deleteCase.target,
                            failedOperationType: failedOperationType
                        });

                        expect(message).toMatchSnapshot();
                    });
                }
            });
        });
    });

    // лок редактирования документа
    describe('должен выводить правильное сообщение при', () => {
        const commonOperations = ['copy', 'move', 'delete', 'rename', 'publish'];
        const cases = [
            {
                title: 'файла, редактируемого мной',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'файла, редактируемого другим участником общей папки',
                target: resourcePaths.file1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            },
            {
                title: 'папки с файлом, редактируемым мной',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.my,
                operations: commonOperations
            },
            {
                title: 'папки с файлом, редактируемым другим участником общей папки',
                target: resourcePaths.folder1,
                locked: resourcePaths.file1,
                uid: uids.other,
                operations: commonOperations
            }
        ];

        cases.forEach((officeCase) => {
            officeCase.operations.forEach((failedOperationType) => {
                for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                    const caseTitle = [
                        operationTitlesForCases[failedOperationType],
                        officeCase.title,
                        messageLengthTitle
                    ].join(' ');

                    it(caseTitle, () => {
                        const message = getLockErrorMessage({
                            myUid: uids.my,
                            isLong: isLong,
                            lockType: 'office',
                            lockInitiatorUid: officeCase.uid,
                            lockedResourcePath: officeCase.locked,
                            targetResourcePath: officeCase.target,
                            failedOperationType: failedOperationType
                        });

                        expect(message).toMatchSnapshot();
                    });
                }
            });
        });
    });

    // лок очистки корзины
    describe('должен выводить правильное сообщение при', () => {
        const dropLocks = ['trash_drop_all', 'clean_trash'];

        const cases = [
            {
                title: 'перемещении в корзину в момент ее очистки',
                target: resourcePaths.file1,
                locked: resourcePaths.trash,
                operation: 'delete'
            },
            {
                title: 'удалении из корзины в момент ее очистки',
                target: resourcePaths.fileInTrash,
                locked: resourcePaths.trash,
                operation: 'delete'
            },
            {
                title: 'восстановлении из корзины в момент ее очистки',
                target: resourcePaths.fileInTrash,
                locked: resourcePaths.trash,
                operation: 'restore'
            },
            {
                title: 'очистке корзины в момент ее очистки',
                target: resourcePaths.trash,
                locked: resourcePaths.trash,
                operation: 'cleanTrash'
            }
        ];

        cases.forEach((dropCase) => {
            dropLocks.forEach((dropLock) => {
                for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                    const caseTitle = [dropCase.title, messageLengthTitle].join(' ');

                    it(caseTitle, () => {
                        const message = getLockErrorMessage({
                            myUid: uids.my,
                            isLong: isLong,
                            lockType: dropLock,
                            lockInitiatorUid: uids.my,
                            lockedResourcePath: dropCase.locked,
                            targetResourcePath: dropCase.target,
                            failedOperationType: dropCase.operation
                        });

                        expect(message).toMatchSnapshot();
                    });
                }
            });
        });
    });

    // лок восстановления из корзины
    describe('должен выводить правильное сообщение при', () => {
        const cases = [
            {
                title: 'очистке корзины в момент восстановления файла',
                target: resourcePaths.trash,
                locked: resourcePaths.fileInTrash,
                operation: 'cleanTrash'
            },
            {
                title: 'удалении из корзины восстанавливаемого файла',
                target: resourcePaths.fileInTrash,
                locked: resourcePaths.fileInTrash,
                operation: 'delete'
            },
            {
                title: 'попытке восстановить восстанавливаемый файл',
                target: resourcePaths.fileInTrash,
                locked: resourcePaths.fileInTrash,
                operation: 'restore'
            },
            {
                title: 'попытке очистки корзины в момент восстановления файла',
                target: resourcePaths.trash,
                locked: resourcePaths.fileInTrash,
                operation: 'cleanTrash'
            }
        ];

        cases.forEach((restoreCase) => {
            for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                const caseTitle = [restoreCase.title, messageLengthTitle].join(' ');

                it(caseTitle, () => {
                    const message = getLockErrorMessage({
                        myUid: uids.my,
                        isLong: isLong,
                        lockType: 'trash_restore',
                        lockInitiatorUid: uids.my,
                        lockedResourcePath: restoreCase.locked,
                        targetResourcePath: restoreCase.target,
                        failedOperationType: restoreCase.operation
                    });

                    expect(message).toMatchSnapshot();
                });
            }
        });
    });

    // лок удаления из корзины
    describe('должен выводить правильное сообщение при', () => {
        const removeLocks = ['trash_drop_element', 'rm'];

        const cases = [
            {
                title: 'очистке корзины в момент удаления файла',
                target: resourcePaths.trash,
                locked: resourcePaths.fileInTrash,
                operation: 'cleanTrash'
            },
            {
                title: 'удалении из корзины удаляемого файла',
                target: resourcePaths.fileInTrash,
                locked: resourcePaths.fileInTrash,
                operation: 'delete'
            },
            {
                title: 'попытке восстановить удаляемый файл',
                target: resourcePaths.fileInTrash,
                locked: resourcePaths.fileInTrash,
                operation: 'restore'
            }
        ];

        cases.forEach((removeCase) => {
            removeLocks.forEach((removeLock) => {
                for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
                    const caseTitle = [removeCase.title, messageLengthTitle].join(' ');

                    it(caseTitle, () => {
                        const message = getLockErrorMessage({
                            myUid: uids.my,
                            isLong: isLong,
                            lockType: removeLock,
                            lockInitiatorUid: uids.my,
                            lockedResourcePath: removeCase.locked,
                            targetResourcePath: removeCase.target,
                            failedOperationType: removeCase.operation
                        });

                        expect(message).toMatchSnapshot();
                    });
                }
            });
        });
    });

    describe('с опцией `onlyReason`', () => {
        for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
            it(['должен возвращать только причину лока', messageLengthTitle].join(' '), () => {
                const message = getLockErrorMessage({
                    myUid: uids.my,
                    isLong: isLong,
                    lockType: 'move_resource',
                    onlyReason: true,
                    lockInitiatorUid: uids.my,
                    lockedResourcePath: resourcePaths.file1,
                    targetResourcePath: resourcePaths.file1,
                    failedOperationType: 'copy'
                });

                expect(message).toMatchSnapshot();
            });
        }
    });

    describe('с опцией `joinWith`', () => {
        for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
            it(
                ['должен использовать эту опцию для склейки в случае, если это строка', messageLengthTitle].join(' '),
                () => {
                    const message = getLockErrorMessage({
                        myUid: uids.my,
                        isLong: isLong,
                        joinWith: '--',
                        lockType: 'move_resource',
                        lockInitiatorUid: uids.my,
                        lockedResourcePath: resourcePaths.file1,
                        targetResourcePath: resourcePaths.file1,
                        failedOperationType: 'copy'
                    });

                    expect(message).toMatchSnapshot();
                }
            );
        }
    });
});

describe('Метод `getLockErrorMessageParts`', () => {
    for (const [isLong, messageLengthTitle] of messageLengthTitles.entries()) {
        it(
            ['должен вернуть массив предложений нотификации', messageLengthTitle].join(' '),
            () => {
                const message = getLockErrorMessageParts({
                    myUid: uids.my,
                    isLong: isLong,
                    lockType: 'move_resource',
                    lockInitiatorUid: uids.my,
                    lockedResourcePath: resourcePaths.file1,
                    targetResourcePath: resourcePaths.file1,
                    failedOperationType: 'copy'
                });

                expect(message).toMatchSnapshot();
            }
        );
    }
});
