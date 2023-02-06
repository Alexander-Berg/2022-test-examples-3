import { fireEvent, render, screen } from '@testing-library/react';
import React from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { ImageType, NormalisedModel, Parameter } from '@yandex-market/mbo-parameter-editor';
import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { SkuViewer } from './SkuViewer';
import { ActionsContext, PictureEditorActions } from 'src/contexts';

describe('SkuViewer', () => {
  it('renders empty', () => {
    render(
      <DndProvider backend={HTML5Backend}>
        <SkuViewer
          sku={partialWrapper<NormalisedModel>({
            normalizedPictures: [],
            parameterValues: {},
          })}
          model={partialWrapper<NormalisedModel>({
            normalizedPickers: [],
          })}
          pickerParameter={partialWrapper<Parameter>({ id: 123, xslName: 'test', valueType: ValueType.ENUM })}
        />
      </DndProvider>
    );
  });
  it('handle remove all', () => {
    const context = partialWrapper<PictureEditorActions>({ showError: jest.fn(), setModelPictures: jest.fn() });
    render(
      <ActionsContext.Provider value={context}>
        <DndProvider backend={HTML5Backend}>
          <SkuViewer
            sku={partialWrapper<NormalisedModel>({
              id: 123,
              normalizedPictures: [
                { url: 'test_src1', type: ImageType.REMOTE },
                { url: 'test_src2', type: ImageType.REMOTE },
                { url: 'test_src3', type: ImageType.REMOTE },
                { url: 'test_src4', type: ImageType.REMOTE },
                { url: 'test_src5', type: ImageType.REMOTE },
                { url: 'test_src6', type: ImageType.REMOTE },
              ],
              parameterValues: {},
            })}
            model={partialWrapper<NormalisedModel>({
              normalizedPickers: [],
            })}
            pickerParameter={partialWrapper<Parameter>({ id: 123, xslName: 'test', valueType: ValueType.ENUM })}
          />
        </DndProvider>
      </ActionsContext.Provider>
    );

    expect(screen.queryAllByRole('img')).toHaveLength(6);

    expect(screen.queryByText('Удалить все')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Удалить все'));

    expect(context.setModelPictures).toBeCalledWith(123, []);
  });
});
