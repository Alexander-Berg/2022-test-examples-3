import { ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { ModelTitle } from '..';
import { concatModelTitles } from '../concatModelTitles';

const createTitle = ({ title, error }: { title?: string; error?: string }): ModelTitle => ({
  id: 1,
  currentType: ModelType.GURU,
  title,
  error: error ? new Error(error) : undefined,
});

describe('concatModelTitles', () => {
  test('concat without errors', () => {
    expect(concatModelTitles(createTitle({ title: 'Foo' }), createTitle({ title: 'Bar' }))).toEqual(
      createTitle({ title: 'FooBar' })
    );
  });

  test('concat first with error', () => {
    const title = concatModelTitles(createTitle({ error: 'Foo' }), createTitle({ title: 'Bar' }));

    expect(title).toEqual(createTitle({ title: 'Bar', error: 'Foo' }));
  });

  test('concat second with error', () => {
    const title = concatModelTitles(createTitle({ title: 'Foo' }), createTitle({ error: 'Bar' }));

    expect(title).toEqual(createTitle({ title: 'Foo', error: 'Bar' }));
  });

  test('concat first and second with error', () => {
    const title = concatModelTitles(createTitle({ error: 'Foo' }), createTitle({ error: 'Bar' }));

    expect(title).toEqual(createTitle({ error: 'Foo Bar' }));
  });
});
