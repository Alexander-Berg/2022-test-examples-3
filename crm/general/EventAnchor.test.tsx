import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mocked } from 'ts-jest/utils';
import { logger } from 'services/Logger';
import { config } from 'services/Config';
import { withEventAnchor } from './withEventAnchor';
import { uxLogStub } from '../UXLogger.stubs';
import { UXLogger } from '../UXLogger';

jest.useFakeTimers('modern');

jest.mock('services/Logger');
const loggerMocked = mocked(logger);

window.requestAnimationFrame = (callback) => {
  callback(0);
  return 0;
};

const ModalWithHoc = withEventAnchor(({ innerRef, children, ...props }) => {
  return ReactDOM.createPortal(
    <div {...props} ref={innerRef as React.Ref<HTMLDivElement>} data-testid='modal'>
      {children}
    </div>,
    document.body,
  );
});

const TestComponent: React.FC<{id?: string, scope?: "inplace"}> = ({
  children, scope, id = ""
}) => {
  const [visible, setVisible] = useState(false)

  const handleToggle = () => {
    setVisible(!visible)
  }

  return (
    <>
      <div onClick={handleToggle} data-testid={`toggle${id}`}></div>
      <ModalWithHoc scope={scope} visible={visible}>
        {children}
      </ModalWithHoc>
    </>
  )
}

describe('EventAnchor', () => {
  beforeEach(() => {
    config.value.features.newFrontendLogs = true;
  });

  describe('component with hoc', () => {
    describe("props.visible", () => {
      describe("when equals true", () => {
        it("recieves logs with anchor path", () => {
          render(
            <UXLogger>
              <div data-testid='container'>
                <TestComponent>
                <div data-testid='target' />
                </TestComponent>
              </div>
            </UXLogger>
          )
    
          userEvent.click(screen.getByTestId("toggle"))
          loggerMocked.reportInfo.mockClear();
          userEvent.click(screen.getByTestId("target"))
    
          expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
            uxLogStub('click', undefined, ['target', 'modal', 'toggle', 'container']),
          );
        })

        it("recieves logs with complex anchor path", () => {
          render(
            <UXLogger>
              <div data-testid='container'>
                <TestComponent id="0">
                  <TestComponent id="1">
                    <div data-testid='target' />
                  </TestComponent>
                </TestComponent>
              </div>
            </UXLogger>
          )
    
          userEvent.click(screen.getByTestId("toggle0"))
          userEvent.click(screen.getByTestId("toggle1"))
          loggerMocked.reportInfo.mockClear();
          userEvent.click(screen.getByTestId("target"))
    
          expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
            uxLogStub('click', undefined, ['target', 'modal', 'toggle1', 'modal', 'toggle0', 'container']),
          );
        })
      })

      describe("when equals false or unmount", () => {
          it("removes modal anchor", () => {
            render(
              <UXLogger>
                <div data-testid='container'>
                  <TestComponent>
                    <div data-testid='target' />
                  </TestComponent>
                </div>
              </UXLogger>
            )
    
            userEvent.click(screen.getByTestId("toggle"))
            loggerMocked.reportInfo.mockClear();
            userEvent.click(screen.getByTestId("target"))
      
            expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
              uxLogStub('click', undefined, ['target', 'modal', 'toggle', 'container']),
            );
    
            userEvent.click(screen.getByTestId("toggle"))
            loggerMocked.reportInfo.mockClear();
            userEvent.click(screen.getByTestId("target"))
      
            expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
              uxLogStub('click', undefined, ['target', 'modal']),
            );
        })
      })
    })

    describe("props.scope", () => {
      describe("when equals 'inplace'", () => {
        it("doesn't recieve logs with anchor path", () => {
          render(
            <UXLogger>
              <div data-testid='container'>
                <TestComponent scope='inplace'>
                <div data-testid='target' />
                </TestComponent>
              </div>
            </UXLogger>
          )
    
          userEvent.click(screen.getByTestId("toggle"))
          loggerMocked.reportInfo.mockClear();
          userEvent.click(screen.getByTestId("target"))
    
          expect(loggerMocked.reportInfo.mock.calls[0][0]).toStrictEqual(
            uxLogStub('click', undefined, ['target', 'modal']),
          );
        })
      })
    })
  });
});
