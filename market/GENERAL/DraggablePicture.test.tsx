import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { DragDropContext, Droppable } from 'react-beautiful-dnd';

import { DraggablePicture } from './DraggablePicture';

const url = 'https://www.example.com';

test('render DraggablePicture', () => {
  const onSelect = jest.fn();
  const onRemove = jest.fn();
  const app = render(
    <DragDropContext onDragEnd={jest.fn()}>
      <Droppable droppableId="1">
        {providedDrop => (
          <div ref={providedDrop.innerRef}>
            <DraggablePicture url={url} index={1} isSelected={false} onSelect={onSelect} onRemove={onRemove} />
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );

  const picture = app.getByTitle(url);
  fireEvent.click(picture);

  const removeBtn = app.getAllByTestId('remove-pictures')[0];
  fireEvent.click(removeBtn);

  expect(onSelect.mock.calls.length).toBe(1);
  expect(onRemove.mock.calls.length).toBe(1);
});
