import { getTail } from 'news/lib/story/getTail';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { IStory } from 'news/types/IStory';
import { doc } from 'news/tests/stubs/story/doc';
import { turboUrls } from 'news/tests/stubs/contexts/item/turboUrls';

describe('getTail', () => {
  it('обрабатывает старый формат хвоста', () => {
    const { tail } = getTail(
      getServerCtxStub(),
      getStory({
        tail: {
          favorites: [],
          docs: [doc],
          foreign_agency: [],
          foreign_lang_docs: [],
        },
      }),
      turboUrls,
    );

    expect(tail).toHaveLength(1);
  });

  it('обрабатывает новый формат хвоста', () => {
    const { tail } = getTail(
      getServerCtxStub(),
      getStory(),
      turboUrls,
    );

    expect(tail).toHaveLength(1);
  });
});

function getStory(story?: Partial<IStory>) {
  return {
    tail: [doc],
    counts: {
      by_genre: {
        total: {
          uniqs: 3,
        },
      },
    },
    ...story,
  } as unknown as IStory;
}
