import {CommentsState, JmfComment, ReduxState, RegularEntity} from 'types/models';
import {FilterConstructor} from 'utils/filterConstructor';

import {
    createFilter,
    deleteComment,
    getCommentsToAdd,
    getEntityComments,
    getFiltersFromState,
    getPaginationFromState,
    canAddComment,
    replaceCommentByIndex,
    getEmailString,
    concatNameAndEmails,
} from '../utils';
import {
    COMMENT_FILTER_TYPE,
    CONTACT_COMMUNICATION_FILTERS,
    DEFAULT_COMMENTS_PAGINATION,
    DELETE_COMMENT_MESSAGE,
    USER_COMMUNICATION_FILTERS,
} from '../constants';

/** ======================== Тестовые данные ========================= */

const testAuthorTitle1 = 'Тестовый автор';

const testAuthor1: RegularEntity = {
    _permissions: {'@view': true, '@edit': true},
    gid: 'author@1',
    metaclass: 'author',
};

const testAuthor2: RegularEntity = {
    _permissions: {'@view': true, '@edit': true},
    gid: 'author@2',
    metaclass: 'author',
    title: testAuthorTitle1,
};

const testComment1: JmfComment = {
    _permissions: {'@view': true, '@edit': true},
    gid: 'comment@1',
    metaclass: 'comment',
    author: testAuthor1,
    body: 'comment1',
    textBody: 'comment1',
    entity: {
        _permissions: {'@view': true, '@edit': true},
        gid: 'entity@1',
        metaclass: 'entity',
    },
    '@attachments': [],
};

const testComment2: JmfComment = {
    _permissions: {'@view': true, '@edit': true},
    gid: 'comment@2',
    metaclass: 'comment',
    author: testAuthor2,
    body: 'comment2',
    textBody: 'comment2',
    entity: {
        _permissions: {'@view': true, '@edit': true},
        gid: 'entity@1',
        metaclass: 'entity',
    },
    '@attachments': [],
};

const testComments1 = [testComment1, testComment2];

const testComment3: JmfComment = {
    _permissions: {'@view': true, '@edit': true},
    gid: 'comment@3',
    metaclass: 'comment',
    author: {
        _permissions: {'@view': true, '@edit': true},
        gid: 'author@1',
        metaclass: 'author',
    },
    body: 'comment3',
    textBody: 'comment3',
    entity: {
        _permissions: {'@view': true, '@edit': true},
        gid: 'entity@1',
        metaclass: 'entity',
    },
    '@attachments': [],
};

const testFilter1 = FilterConstructor.eq('test1', 'test1');
const testFilter2 = FilterConstructor.eq('test2', 'test2');

const testCommentStateFilter1 = {
    type: COMMENT_FILTER_TYPE.USER_COMMUNICATION,
    value: [testFilter1, testFilter2],
};

const testPagination1 = {
    limit: 11,
    offset: 111,
    hasMore: true,
};

const testCommentsState1: CommentsState = {
    pagination: testPagination1,
    filter: testCommentStateFilter1,
    comments: testComments1,
};

const testCommentsState2: CommentsState = {};

const testReduxCommentsState1: ReduxState['jmf']['comments'] = {
    'entity@1': testCommentsState1,
    'entity@2': testCommentsState2,
};

const testEmail1 = 'test1@test1.ru';
const testEmail2 = 'test2@test2.ru';

const testName1 = 'Тестовое имя';

/** ================================================================== */

describe('Тестирование вспомогательных утилит jmfComments', () => {
    describe('getPaginationFromState', () => {
        it('При наличии параметров пагинации в состоянии и без обновления с затиранием возвращает их', () => {
            const pagination = getPaginationFromState(false, testCommentsState1);

            expect(pagination).toEqual(testPagination1);
        });

        it('При отсутствии параметров пагинации в состоянии и без обнолвения с затиранием возвращает дефолтные параметры', () => {
            const pagination = getPaginationFromState(false, testCommentsState2);

            expect(pagination).toEqual(DEFAULT_COMMENTS_PAGINATION);
        });

        it('При наличии параметров пагинации в состоянии и с обновление с затиранием возращает дефолтные параметры с лимитом из параметров в состоянии', () => {
            const pagination = getPaginationFromState(true, testCommentsState1);

            const expected = {...DEFAULT_COMMENTS_PAGINATION, limit: testCommentsState1.pagination?.limit};

            expect(pagination).toEqual(expected);
        });
    });

    describe('getFiltersFromState', () => {
        const gid = 'entity@1';

        it('При наличии фильтров в состоянии возвращает их', () => {
            const filters = getFiltersFromState(gid, testCommentsState1);

            expect(filters).toEqual(testCommentStateFilter1);
        });

        it('При отсутствии фильтров в состоянии возвращаем дефолтные', () => {
            const filters = getFiltersFromState(gid, testCommentsState2);

            expect(filters.type).toEqual(COMMENT_FILTER_TYPE.ALL);
            expect(filters.value).toBeInstanceOf(Array);
            expect(filters.value).toHaveLength(1);
            expect(filters.value).toContainEqual(FilterConstructor.eq('entity', gid));
        });
    });

    describe('getEntityComments', () => {
        it('При наличии состояния комментариев в Redux для данной сущности и наличия списка комментариев в этом состоянии возвращает их', () => {
            const comments = getEntityComments(testReduxCommentsState1, 'entity@1');

            expect(comments).toBeInstanceOf(Array);
            expect(comments).toHaveLength(2);
            expect(comments).toContain(testComment1);
            expect(comments).toContain(testComment2);
        });

        it('При наличии состояния комментариев в Redux для данной сущности и отсутствия списка комментариев в этом состоянии возвращает пустой массив', () => {
            const comments = getEntityComments(testReduxCommentsState1, 'entity@2');

            expect(comments).toBeInstanceOf(Array);
            expect(comments).toHaveLength(0);
        });

        it('При отсутствии состояния комментариев в Redux для данной сущности возвращает пустой массив', () => {
            const comments = getEntityComments(testReduxCommentsState1, 'entity@3');

            expect(comments).toBeInstanceOf(Array);
            expect(comments).toHaveLength(0);
        });
    });

    describe('canAddComment', () => {
        it('При отсутсвии комментария с таким же Gid как у переденно возвращает true', () => {
            const needAddComment = canAddComment(testComments1, testComment3);

            expect(needAddComment).toBeTruthy();
        });

        it('При наличии комментария с такие же Gid как у переданного возвращает false', () => {
            const needAddComment = canAddComment(testComments1, testComment2);

            expect(needAddComment).toBeFalsy();
        });
    });

    describe('getCommentsToAdd', () => {
        it('При отсутствии всех комментариев для добавления в списке текущих возвращает все переданные комментарии', () => {
            const commentsToAdd = getCommentsToAdd(testComments1, [testComment3]);

            expect(commentsToAdd).toBeInstanceOf(Array);
            expect(commentsToAdd).toHaveLength(1);
            expect(commentsToAdd).toContain(testComment3);
        });

        it('При присутствии каких-то комментариев для добавления в списке текущих возвращает массив без присутствующих', () => {
            const commentsToAdd = getCommentsToAdd(testComments1, [testComment2, testComment3]);

            expect(commentsToAdd).toBeInstanceOf(Array);
            expect(commentsToAdd).toHaveLength(1);
            expect(commentsToAdd).toContain(testComment3);
        });

        it('При присутствии всех комментариев для добавления в списке текущих возвращает пустой массив', () => {
            const commentsToAdd = getCommentsToAdd(testComments1, [testComment2, testComment1]);

            expect(commentsToAdd).toBeInstanceOf(Array);
            expect(commentsToAdd).toHaveLength(0);
        });
    });

    describe('replaceCommentByIndex', () => {
        it('При отрицательном переданном индексе возвращает неизмененные комментарии', () => {
            const comments = replaceCommentByIndex(testComments1, testComment3, -1);

            expect(comments).toEqual(testComments1);
        });

        it('При переданном индексе выходящем за граница индексов комментариев возвращает неизмененные комментарии', () => {
            const index = testComments1.length;
            const comments = replaceCommentByIndex(testComments1, testComment3, index);

            expect(comments).toEqual(testComments1);
        });

        it('При корректном индексе заменяет комментарий', () => {
            const comments = replaceCommentByIndex(testComments1, testComment3, 1);

            expect(comments).toBeInstanceOf(Array);
            expect(comments).toHaveLength(2);
            expect(comments).toContain(testComment3);
            expect(comments).toContain(testComment1);
            expect(comments[1]).toEqual(testComment3);
        });
    });

    describe('deleteComment', () => {
        it('При отсутствии в списке комментария который хотим удалить возвращает не измененный список', () => {
            const comments = deleteComment(testComments1, testComment3);

            expect(comments).toBeInstanceOf(Array);
            expect(comments).toHaveLength(2);
            expect(comments).toContain(testComment1);
            expect(comments).toContain(testComment2);
            expect(comments).toStrictEqual(testComments1);
        });

        it('При наличии в списке комментария который хотим удалить заменяет его текст и удаляет вложения', () => {
            const comments = deleteComment(testComments1, testComment1);

            expect(comments).toBeInstanceOf(Array);
            expect(comments).toHaveLength(2);

            const deletedComment = comments.find(comment => comment.gid === 'comment@1');

            expect(deletedComment).toBeDefined();
            expect(deletedComment).toHaveProperty('body');
            expect(deletedComment).toHaveProperty('textBody');
            expect(deletedComment).toHaveProperty('@attachments');
            expect(deletedComment?.body).toEqual(DELETE_COMMENT_MESSAGE);
            expect(deletedComment?.textBody).toEqual(DELETE_COMMENT_MESSAGE);
            expect(deletedComment?.['@attachments']).toHaveLength(0);
        });
    });

    describe('createFilter', () => {
        const gid = 'entity@1';
        const entityFilter = FilterConstructor.eq('entity', gid);

        it('Правильное создание фильтра для выбранного фильтра "Общение с клиентом"', () => {
            const filter = createFilter(gid, COMMENT_FILTER_TYPE.USER_COMMUNICATION);

            expect(filter.type).toEqual(COMMENT_FILTER_TYPE.USER_COMMUNICATION);
            expect(filter.value).toBeInstanceOf(Array);
            expect(filter.value).toHaveLength(2);
            expect(filter.value).toContain(USER_COMMUNICATION_FILTERS);
            expect(filter.value).toContainEqual(entityFilter);
        });

        it('Правильное создание фильтра для выбранного фильтра "Общение с партнерами"', () => {
            const filter = createFilter(gid, COMMENT_FILTER_TYPE.CONTACT_COMMUNICATION);

            expect(filter.type).toEqual(COMMENT_FILTER_TYPE.CONTACT_COMMUNICATION);
            expect(filter.value).toBeInstanceOf(Array);
            expect(filter.value).toHaveLength(2);
            expect(filter.value).toContain(CONTACT_COMMUNICATION_FILTERS);
            expect(filter.value).toContainEqual(entityFilter);
        });

        it('Правильное создание фильтра для выбранного фильтра "Все"', () => {
            const filter = createFilter(gid, COMMENT_FILTER_TYPE.ALL);

            expect(filter.type).toEqual(COMMENT_FILTER_TYPE.ALL);
            expect(filter.value).toBeInstanceOf(Array);
            expect(filter.value).toHaveLength(1);
            expect(filter.value).toContainEqual(entityFilter);
        });
    });

    describe('getEmailString', () => {
        it('При передаче undefined возвращает null', () => {
            const emailString = getEmailString(undefined);

            expect(emailString).toEqual(null);
        });

        it('При передача null возвращает null', () => {
            const emailString = getEmailString(null);

            expect(emailString).toEqual(null);
        });

        it('При передачи строки с email возвращает неизменную строку', () => {
            const emailString = getEmailString(testEmail1);

            expect(emailString).toEqual(testEmail1);
        });

        it('При передачи массива с email конкатинирует из в строку через запятую', () => {
            const emailString = getEmailString([testEmail1, testEmail2]);
            const expectResult = `${testEmail1}, ${testEmail2}`;

            expect(emailString).toEqual(expectResult);
        });
    });

    describe('concatNameAndEmails', () => {
        it('При запуске без аргументов возвращает null', () => {
            const nameAndEmail = concatNameAndEmails();

            expect(nameAndEmail).toEqual(null);
        });

        it('При передачи только имени возвращает только имя', () => {
            const nameAndEmail = concatNameAndEmails(testName1);

            expect(nameAndEmail).toEqual(testName1);
        });

        it('При передачи только строки email возвращает эту строку', () => {
            const nameAndEmail = concatNameAndEmails(null, testEmail1);

            expect(nameAndEmail).toEqual(testEmail1);
        });

        it('При передачи только массива email возвращает строку emails через запятую', () => {
            const nameAndEmail = concatNameAndEmails(null, [testEmail1, testEmail2]);
            const expectResult = `${testEmail1}, ${testEmail2}`;

            expect(nameAndEmail).toEqual(expectResult);
        });

        it('При передачи имени и строки с email возвращает отформатированную сроку', () => {
            const nameAndEmail = concatNameAndEmails(testName1, testEmail1);
            const expectResult = `${testName1} (${testEmail1})`;

            expect(nameAndEmail).toEqual(expectResult);
        });

        it('При передачи имени и массива с email возвращает отформатированную сроку', () => {
            const nameAndEmail = concatNameAndEmails(testName1, [testEmail1, testEmail2]);
            const expectResult = `${testName1} (${testEmail1}, ${testEmail2})`;

            expect(nameAndEmail).toEqual(expectResult);
        });
    });
});
