import { prepareWidgetStory } from 'sport/lib/dataSource/prepareWidget';
import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { story, storyNoSnippetsDoc } from 'sport/tests/stubs/story/story';

test('prepareWidgetStory snippets source_name', () => {
  const actualWidgetStory = [prepareWidgetStory(getServerCtxStub(), story)];
  const expectedWidgetStory = [{
    title: 'Сборная России сыграет с Канадой в четвертьфинале ЧМ-2021',
    sourceName: 'Чемпионат (snippets)',
    time: '01.01.1970 в 03:00',
    url: 'https://yandex.ru/sport/story/Sbornaya_Rossii_sygraet_sKanadoj_vchetvertfinale_CHM-2021--1adea0af573eacadd08e48fae917a43a?lang=ru&wan=1&persistent_id=145408097',
    target: '_self',
    cmntCount: 0,
  }];

  expect(actualWidgetStory).toMatchObject(expectedWidgetStory);
});

test('prepareWidgetStory title source_name', () => {
  const actualWidgetStory = [prepareWidgetStory(getServerCtxStub(), storyNoSnippetsDoc)];
  const expectedWidgetStory = [{
    title: 'Сборная России сыграет с Канадой в четвертьфинале ЧМ-2021',
    sourceName: 'Чемпионат (title)',
    time: '01.01.1970 в 03:00',
    url: 'https://yandex.ru/sport/story/Sbornaya_Rossii_sygraet_sKanadoj_vchetvertfinale_CHM-2021--1adea0af573eacadd08e48fae917a43a?lang=ru&wan=1&persistent_id=145408097',
    target: '_self',
    cmntCount: 0,
  }];

  expect(actualWidgetStory).toMatchObject(expectedWidgetStory);
});
