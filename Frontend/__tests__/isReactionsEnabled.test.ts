import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { EPage } from 'sport/types/EPage';
import { isReactionsEnabled } from '../isReactionsEnabled';

describe('isReactionsEnabled', () => {
  it('Возвращает true для STORY, если флаг yxneo_sport_story_enable-reactions включен', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_story_enable-reactions': '1',
          },
          page: EPage.STORY,
        },
      },
    });

    const result = isReactionsEnabled(serverCtx);
    expect(result).toBe(true);
  });

  it('Возвращает false для остальных страниц, если флаг yxneo_sport_story_enable-reactions включен', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_story_enable-reactions': '1',
          },
          page: EPage.EVENT,
        },
      },
    });

    const result = isReactionsEnabled(serverCtx);
    expect(result).toBe(false);
  });

  it('Возвращает false для STORY, если флаг yxneo_sport_story_enable-reactions выключен', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        neo: {
          flags: {
            'yxneo_sport_story_enable-reactions': '0',
          },
          page: EPage.STORY,
        },
      },
    });

    const result = isReactionsEnabled(serverCtx);
    expect(result).toBe(false);
  });
});
