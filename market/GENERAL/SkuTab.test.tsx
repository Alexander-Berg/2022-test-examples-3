import React, { useEffect } from 'react';
import { useAction } from '@reatom/react';
import { Model } from '@yandex-market/mbo-parameter-editor';
import { screen } from '@testing-library/react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { render } from 'src/test/customRender';
import { SkuTab } from 'src/pages/ModelEditorCluster/tabs';
import { SetModelIdAction, SetNormalisedModelAction } from 'src/pages/ModelEditorCluster/atoms';

describe('<SkuTab />', () => {
  it('render without errors', async () => {
    const Provider = setupTestProvider();

    expect(() => {
      render(
        <Provider>
          <MyComp />
        </Provider>
      );
    }).not.toThrow();

    await screen.findByText('Добавить SKU');
  });
});

function MyComp() {
  const setNormalisedModel = useAction(SetNormalisedModelAction);
  const setModelId = useAction(SetModelIdAction);
  useEffect(() => {
    setNormalisedModel({ id: 1, title: 'title', currentType: 'SKU' } as Model);
    setModelId(1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <SkuTab />;
}
