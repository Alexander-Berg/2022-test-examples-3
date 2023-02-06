import React from 'react';
import { render, fireEvent, screen } from '@testing-library/react';
import { zoneStub } from 'components/MultiOverlayDropZone';
import { TestBed } from 'components/TestBed';
import { AttachFilesDNDContainer } from './AttachFilesDNDContainer';
import { AttachFilesDNDContext } from '../AttachFilesDNDService';
import { AttachFilesDNDScope } from '../AttachFilesDNDScope';

afterEach(() => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (global as any).window.__isReactDndBackendSetUp = false;
});

describe('AttachFilesDNDContainer', () => {
  describe('props.children', () => {
    describe('when defined', () => {
      it('renders children', () => {
        render(
          <AttachFilesDNDContainer>
            <div data-testid="child" />
          </AttachFilesDNDContainer>,
        );

        expect(screen.getByTestId('child')).toBeInTheDocument();
      });
    });
  });

  it('provides AttachFilesDNDContext', () => {
    const attachFilesDNDService = jest.fn();

    render(
      <AttachFilesDNDContainer>
        <AttachFilesDNDContext.Consumer>
          {(service) => {
            attachFilesDNDService(service);
            return null;
          }}
        </AttachFilesDNDContext.Consumer>
      </AttachFilesDNDContainer>,
    );

    expect(attachFilesDNDService.mock.calls[0][0]).toBeDefined();
  });

  describe('when drag', () => {
    it('renders drop zones', () => {
      const { container } = render(
        <TestBed>
          <AttachFilesDNDContainer>
            <AttachFilesDNDContext.Consumer>
              {(service) => {
                service?.addZones([zoneStub]);
                return null;
              }}
            </AttachFilesDNDContext.Consumer>
          </AttachFilesDNDContainer>
        </TestBed>,
      );

      fireEvent.dragEnter(container, {
        type: 'dragenter',
        dataTransfer: {
          types: ['Files'],
          files: [],
        },
      });

      expect(screen.getByText(zoneStub.text)).toBeInTheDocument();
    });

    it('renders drop zone at top AttachFilesDNDContainer', () => {
      const { container } = render(
        <TestBed>
          <div data-testid="top container">
            <AttachFilesDNDContainer>
              <div data-testid="bottom container">
                <AttachFilesDNDContainer>
                  <AttachFilesDNDContext.Consumer>
                    {(service) => {
                      service?.addZones([zoneStub]);
                      return null;
                    }}
                  </AttachFilesDNDContext.Consumer>
                </AttachFilesDNDContainer>
              </div>
            </AttachFilesDNDContainer>
          </div>
        </TestBed>,
      );

      fireEvent.dragEnter(container, {
        type: 'dragenter',
        dataTransfer: {
          types: ['Files'],
          files: [],
        },
      });

      expect(screen.getByTestId('bottom container')).not.toContainElement(
        screen.getByText(zoneStub.text),
      );
      expect(screen.getByTestId('top container')).toContainElement(screen.getByText(zoneStub.text));
    });

    it('renders drop zone at top AttachFilesDNDContainer in scope', () => {
      const { container } = render(
        <TestBed>
          <div data-testid="top container">
            <AttachFilesDNDContainer>
              <AttachFilesDNDScope>
                <div data-testid="bottom container">
                  <AttachFilesDNDContainer>
                    <AttachFilesDNDContext.Consumer>
                      {(service) => {
                        service?.addZones([zoneStub]);
                        return null;
                      }}
                    </AttachFilesDNDContext.Consumer>
                  </AttachFilesDNDContainer>
                </div>
              </AttachFilesDNDScope>
            </AttachFilesDNDContainer>
          </div>
        </TestBed>,
      );

      fireEvent.dragEnter(container, {
        type: 'dragenter',
        dataTransfer: {
          types: ['Files'],
          files: [],
        },
      });

      expect(screen.getByTestId('bottom container')).toContainElement(
        screen.getByText(zoneStub.text),
      );
    });
  });
});
