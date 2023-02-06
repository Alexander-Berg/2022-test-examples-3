import { getInstoryInfo } from 'news/lib/story/getInstoryInfo';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { story, storyWithoutSnippets } from 'news/tests/stubs/story/story';
import { region } from 'news/tests/stubs/region';

describe('getInstoryInfo', () => {
  it('возвращает url для первого документа', () => {
    const { moreUrlSource } = getInstoryInfo(
      getServerCtxStub(),
      story,
      region,
    );

    expect(moreUrlSource).toBe('https://yandex.ru/news/instory/translit_title_snippet--cl4url?lr=213&content=alldocs&stid=stid&persistent_id=1234&from=story');
  });

  it('возвращает пустой url, если нет сниппетов', () => {
    const { moreUrlSource } = getInstoryInfo(
      getServerCtxStub(),
      storyWithoutSnippets,
      region,
    );

    expect(moreUrlSource).toBeUndefined();
  });
});
