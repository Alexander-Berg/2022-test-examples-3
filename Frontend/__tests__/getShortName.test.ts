import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { EPlatform } from 'neo/types/EPlatform';
import { getShortName } from '../getShortName';

describe('getShortName', () => {
  it('Возвращает пустое поле, если не передаем имя', () => {
    const serverCtx = getServerCtxStub();
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: true,
        name: ' ',
      },
    );
    expect(name).toBe('');
  });

  it('Ставит фамилию на первое место, если передаем его вторым элементом строки, фамилия одна буква', () => {
    const serverCtx = getServerCtxStub(
      {
        specialArgs: {
          neo: {
            platform: EPlatform.DESKTOP,
          },
        },
      });
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: true,
        name: 'Ю Ин',
      },
    );
    expect(name).toBe('Ин Ю');
  });

  it('Свайпает и возвращает фамилию с точкой, если передаем фамилию первым элементом', () => {
    const serverCtx = getServerCtxStub();
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: true,
        name: 'Иван Иванов Иванович',
      },
    );
    expect(name).toBe('Иванов Иванович И.');
  });
  it('Возвращает имя целиком, если передаем только имя', () => {
    const serverCtx = getServerCtxStub();
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: true,
        name: 'Вася',
      },
    );
    expect(name).toBe('Вася');
  });
  it('Свайпает и возвращает фамилию с точкой, если передаем фамилию и имя', () => {
    const serverCtx = getServerCtxStub();
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: true,
        name: 'Иван Иванов',
      },
    );
    expect(name).toBe('Иванов И.');
  });
  it('Не свайпает имя, но сокращает фамилию при передаче ФИ', () => {
    const serverCtx = getServerCtxStub();
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: false,
        name: 'Иван Иванов',
      },
    );
    expect(name).toBe('И. Иванов');
  });
  it('Не свайпает, но возвращает фамилию с точкой, если передаем фио', () => {
    const serverCtx = getServerCtxStub();
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: false,
        name: 'Иван Иванович Иванов',
      },
    );
    expect(name).toBe('И. Иванович Иванов');
  });
  it('Не свайпает фио и не ставит точку после фамилии, если передаем фамилию одной буквой', () => {
    const serverCtx = getServerCtxStub(
      {
        specialArgs: {
          neo: {
            platform: EPlatform.DESKTOP,
          },
        },
      });
    const name = getShortName(
      serverCtx,
      {
        hasSwipe: false,
        name: 'Ю Ин',
      },
    );
    expect(name).toBe('Ю Ин');
  });
});
