import { getServerCtxStub } from 'mg/tests/stubs/contexts/ServerCtx';
import { getLanguage } from '../getLanguage';

interface ILanguageMock {
    request?: {
        cookies: {
            my?: string;
        };
        tld?: string;
    };
    report?: {
        language?: string;
    }
}

function getServerCtx(mock?: ILanguageMock) {
  return getServerCtxStub({
    findLastItemArgs: {
      request: 'request',
      report: 'report',
    },
    additionalItemMap: {
      request: {
        cookies: {
          my: mock?.request?.cookies.my,
        },
        tld: mock?.request?.tld,
      },
      report: {
        language: mock?.report?.language,
      },
    },
  });
}

describe('getLanguage', function() {
  it('Возвращает корректный язык из cookie', function() {
    const mockObject = {
      request: {
        cookies: {
          my: 'YycCAAQA', // значение для казахского языка
        },
        tld: 'kk',
      },
    };
    const serverCtx = getServerCtx(mockObject);
    const lang = getLanguage(serverCtx.neo.ctxRR);

    expect(lang).toBe('kk');
  });

  it('Возвращает коррекетный язык из tld', function() {
    const mockObject = {
      request: {
        tld: 'com',
        cookies: {
          my: '',
        },
      },
    };

    const serverCtx = getServerCtx(mockObject);
    const lang = getLanguage(serverCtx.neo.ctxRR);

    expect(lang).toBe('en');
  });

  it('Возвращает корректный язык из документа', function() {
    const mockObject = {
      request: {
        tld: '',
        cookies: {
          my: '',
        },
      },
      report: {
        language: 'ruuk',
      },
    };

    const serverCtx = getServerCtx(mockObject);
    const lang = getLanguage(serverCtx.neo.ctxRR);

    expect(lang).toBe('uk');
  });

  it('По умолчанию возвращает русский язык', function() {
    const serverCtx = getServerCtx();
    const lang = getLanguage(serverCtx.neo.ctxRR);

    expect(lang).toBe('ru');
  });
});
