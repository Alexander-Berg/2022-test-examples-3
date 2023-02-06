import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { MultiOverlayDropZone } from './MultiOverlayDropZone';
import { zoneStub } from './MultiOverlayDropZone.stubs';

describe('MultiOverlayDropZone', () => {
  describe('props.zones', () => {
    describe('when defined', () => {
      describe('when fire drag enter event', () => {
        it('renders zones', () => {
          const { container } = render(
            <TestBed>
              <MultiOverlayDropZone zones={[zoneStub]} />
            </TestBed>,
          );

          expect(screen.queryByText(zoneStub.text)).not.toBeVisible();

          fireEvent.dragEnter(container, {
            type: 'dragenter',
            dataTransfer: {
              types: ['Files'],
              files: [],
            },
          });

          expect(screen.getByText(zoneStub.text)).toBeVisible();
        });
      });
    });
  });
});
