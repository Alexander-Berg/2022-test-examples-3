import {
    searchScope,
    isSuggestDisabled,
    isSuggestVisible,
    defaultSearchScope,
    isSearchSessionStarted
} from '../../../../components/redux/store/selectors/search';

import { SEARCH_SCOPES } from '../../../../components/consts';

const getStore = (idContext, scope = '', active = true, sessionId = null) => ({
    page: { search: { scope }, idContext },
    suggest: { active, sessionId }
});

describe('searchScope', () => {
    it('должен вернуть указанный scope, если таковой имеется в store', () => {
        const scope = '/testScope';
        expect(searchScope(getStore(undefined, scope))).toBe(scope);
    });
    it('в Корзине должен вернуть /trash', () => {
        expect(searchScope(getStore('/trash'))).toBe(SEARCH_SCOPES.TRASH);
    });
    it('в Истории должен вернуть /journal', () => {
        expect(searchScope(getStore('/journal'))).toBe(SEARCH_SCOPES.JOURNAL);
    });
    it(`в папке Диска должен вернуть ${defaultSearchScope}`, () => {
        expect(searchScope(getStore('/disk/foo'))).toBe(defaultSearchScope);
    });
    it(`в корне Диска должен вернуть ${defaultSearchScope}`, () => {
        expect(searchScope(getStore('/disk'))).toBe(defaultSearchScope);
    });
    it(`в альбомах должен вернуть ${defaultSearchScope}`, () => {
        expect(searchScope(getStore('/albums'))).toBe(defaultSearchScope);
    });
    it(`во всех фото должен вернуть ${defaultSearchScope}`, () => {
        expect(searchScope(getStore('/photos'))).toBe(defaultSearchScope);
    });
});

describe('isSuggestDisabled', () => {
    it('должен вернуть `true` для Корзины', () => {
        expect(isSuggestDisabled(getStore('/trash'))).toBe(true);
    });
    it('должен вернуть `true` для Истории', () => {
        expect(isSuggestDisabled(getStore('/journal'))).toBe(true);
    });
    it('должен вернуть `false` для всех остальных idContext', () => {
        expect(isSuggestDisabled(getStore('/disk/folder'))).toBe(false);
    });
});

describe('isSuggestVisible', () => {
    it('должен вернуть `true`, если саджест активен и не заблокирован', () => {
        expect(isSuggestVisible(getStore('/disk', '', true))).toBe(true);
    });
    it('должен вернуть `false`, если саджест заблокирован', () => {
        expect(isSuggestVisible(getStore('/trash', '', true))).toBe(false);
        expect(isSuggestVisible(getStore('/journal', '', true))).toBe(false);
    });
    it('должен вернуть `false`, если саджест неактивен', () => {
        expect(isSuggestVisible(getStore('/disk/yoyo', '', false))).toBe(false);
    });
});

describe('isSearchSessionStarted', () => {
    it('должен вернуть `false`, если нет идентификатора сессии sessionId', () => {
        expect(isSearchSessionStarted(getStore('', '', true, null))).toBe(false);
    });
    it('должен вернуть `true`, если есть идентификатор сессии sessionId', () => {
        expect(isSearchSessionStarted(getStore('/journal', '', true, 'bb958af428b3b973336b805e4603681e'))).toBe(true);
    });
});
