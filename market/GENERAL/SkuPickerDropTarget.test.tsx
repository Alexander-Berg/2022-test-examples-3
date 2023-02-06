import { fireEvent, render } from '@testing-library/react';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { DndProvider } from 'react-dnd';
import React from 'react';
import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { ImageType, NormalisedModel, Parameter, ParameterValue } from '@yandex-market/mbo-parameter-editor';
import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { SkuPickerDropTarget } from './SkuPickerDropTarget';
import { SLOT_CLASS_NAME } from '../Slot';

const CLOSE_CLASS = 'SkuPickerSelectionModalMock-CloseButton';

jest.mock('../SkuPickerSelectionModal', () => ({
  SkuPickerSelectionModal: ({ onClose }: { onClose: () => void }) => {
    // eslint-disable-next-line jsx-a11y/control-has-associated-label
    return <button type="button" className={CLOSE_CLASS} onClick={onClose} />;
  },
}));

describe('SkuPickerDropTarget', () => {
  it('works', () => {
    const { container } = render(
      <DndProvider backend={HTML5Backend}>
        <SkuPickerDropTarget
          model={partialWrapper<NormalisedModel>({})}
          sku={partialWrapper<NormalisedModel>({ parameterValues: [] })}
          pickerParameter={partialWrapper<Parameter>({ id: 123 })}
          onDrop={jest.fn()}
        />
      </DndProvider>
    );

    expect(container.innerHTML).toEqual('');
  });
  it('renders with data', () => {
    const { container } = render(
      <DndProvider backend={HTML5Backend}>
        <SkuPickerDropTarget
          model={partialWrapper<NormalisedModel>({
            normalizedPickers: [
              {
                type: ImageType.PICKER,
                url: 'test_src',
                link: { parameterId: 123, optionId: 321, type: ValueType.ENUM },
              },
            ],
          })}
          sku={partialWrapper<NormalisedModel>({
            parameterValues: { 123: [partialWrapper<ParameterValue>({ optionId: 321 })] },
          })}
          pickerParameter={partialWrapper<Parameter>({ id: 123, xslName: 'test', valueType: ValueType.ENUM })}
          onDrop={jest.fn()}
        />
      </DndProvider>
    );

    const image = container.getElementsByTagName('img').item(0);

    expect(image).toBeDefined();
    expect(image!.src).toContain('test_src');
  });
  it('show modal', () => {
    const { container } = render(
      <DndProvider backend={HTML5Backend}>
        <SkuPickerDropTarget
          model={partialWrapper<NormalisedModel>({
            normalizedPickers: [],
          })}
          sku={partialWrapper<NormalisedModel>({
            parameterValues: { 123: [partialWrapper<ParameterValue>({ optionId: 321 })] },
          })}
          pickerParameter={partialWrapper<Parameter>({ id: 123, xslName: 'test', valueType: ValueType.ENUM })}
          onDrop={jest.fn()}
        />
      </DndProvider>
    );

    const slot = container.getElementsByClassName(SLOT_CLASS_NAME).item(0);

    expect(slot).toBeDefined();

    fireEvent.click(slot!);

    const closeButton = container.getElementsByClassName(CLOSE_CLASS).item(0);

    expect(closeButton).toBeDefined();

    fireEvent.click(closeButton!);

    expect(container.getElementsByClassName(CLOSE_CLASS)).toHaveLength(0);
  });
});
