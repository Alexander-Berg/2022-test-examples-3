import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IssuePropsContext from 'modules/issues/components/Issue/IssuePropsContext';
import { Redux } from 'modules/issues/redux/createRedux';
import { IssueHeader } from './IssueHeader';
import { issueStub } from './IssueHeader.stubs';

jest.mock('modules/issues/components/Issue/IssueField', () => (props) => <div {...props} />);

// eslint-disable-next-line
(window as any).ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
}));

const IssuePropsContextProviderStub = {
  maxAccess: 3,
  issue: issueStub,
  dispatch: jest.fn(),
  redux: {} as Redux,
};

describe('IssueHeader', () => {
  it('renders id', () => {
    render(
      <IssueHeader
        issue={{
          ...issueStub,
          data: {
            ...issueStub.data,
            id: 123,
          },
        }}
      />,
    );

    expect(screen.getByText('123')).toBeInTheDocument();
  });

  it('copies on click and shows tooltip', () => {
    const handleCopy = jest.fn();
    document.execCommand = handleCopy;

    render(
      <IssueHeader
        issue={{
          ...issueStub,
          data: {
            ...issueStub.data,
            id: 123,
          },
        }}
      />,
    );

    expect(screen.queryByText('Ссылка скопирована')).not.toBeInTheDocument();

    userEvent.click(screen.getByTitle('Скопировать ссылку'));

    expect(handleCopy).toBeCalledWith('copy');
    expect(screen.getByText('Ссылка скопирована')).toBeInTheDocument();
  });

  it('renders name', () => {
    render(
      <IssueHeader
        issue={{
          ...issueStub,
          data: {
            ...issueStub.data,
            name: 'test name',
          },
        }}
      />,
    );

    expect(screen.getByText('test name')).toBeInTheDocument();
  });

  it('renders account name', () => {
    render(
      <IssueHeader
        issue={{
          ...issueStub,
          data: {
            ...issueStub.data,
            account: {
              ...issueStub.data.account,
              info: {
                ...issueStub.data.account.info,
                name: 'test info name',
              },
            },
          },
        }}
      />,
    );

    expect(screen.getByTestId('AccountName')).toHaveTextContent('test info name');
  });

  describe('props.issue.props', () => {
    describe('props.quickPanel', () => {
      describe('when defined', () => {
        it('renders quickPanel buttons on more button click', () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader issue={issueStub} />
            </IssuePropsContext.Provider>,
          );

          expect(screen.queryByText('quickPanel text')).not.toBeInTheDocument();

          userEvent.click(screen.getByTestId('MoreButton'));

          expect(screen.getByText('quickPanel text')).toBeInTheDocument();
        });
      });

      describe('when undefined', () => {
        it("doesn't render quickPanel buttons on more button click", () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader
                issue={{
                  ...issueStub,
                  props: {
                    ...issueStub.props,
                    quickPanel: {
                      ...issueStub.props.quickPanel,
                      items: [],
                    },
                  },
                }}
              />
            </IssuePropsContext.Provider>,
          );

          userEvent.click(screen.getByTestId('MoreButton'));

          expect(screen.queryByText('quickPanel text')).not.toBeInTheDocument();
        });
      });

      describe('when no access', () => {
        it("doesn't render quickPanel buttons on more button click", () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader
                issue={{
                  ...issueStub,
                  props: {
                    ...issueStub.props,
                    quickPanel: {
                      ...issueStub.props.quickPanel,
                      access: 0,
                    },
                  },
                }}
              />
            </IssuePropsContext.Provider>,
          );

          userEvent.click(screen.getByTestId('MoreButton'));

          expect(screen.queryByText('quickPanel text')).not.toBeInTheDocument();
        });
      });
    });

    describe('props.actions', () => {
      describe('when defined', () => {
        it('renders actions buttons on more button click', () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader issue={issueStub} />
            </IssuePropsContext.Provider>,
          );

          expect(screen.queryByText('action text')).not.toBeInTheDocument();

          userEvent.click(screen.getByTestId('MoreButton'));

          expect(screen.getByText('action text')).toBeInTheDocument();
        });
      });

      describe('when undefined', () => {
        it("doesn't render actions buttons on more button click", () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader
                issue={{
                  ...issueStub,
                  props: {
                    ...issueStub.props,
                    actions: {
                      ...issueStub.props.actions,
                      items: [],
                    },
                  },
                }}
              />
            </IssuePropsContext.Provider>,
          );

          userEvent.click(screen.getByTestId('MoreButton'));

          expect(screen.queryByText('action text')).not.toBeInTheDocument();
        });
      });

      describe('when no access', () => {
        it("doesn't render actions buttons on more button click", () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader
                issue={{
                  ...issueStub,
                  props: {
                    ...issueStub.props,
                    actions: {
                      ...issueStub.props.actions,
                      access: 0,
                    },
                  },
                }}
              />
            </IssuePropsContext.Provider>,
          );

          userEvent.click(screen.getByTestId('MoreButton'));

          expect(screen.queryByText('action text')).not.toBeInTheDocument();
        });
      });
    });

    describe('props.actionStateChange', () => {
      describe('when defined', () => {
        it('renders actionStateChange buttons', () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader issue={issueStub} />
            </IssuePropsContext.Provider>,
          );

          expect(screen.getByText('actionStateChange text')).toBeInTheDocument();
        });
      });

      describe('when undefined', () => {
        it("doesn't render actionStateChange buttons", () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader
                issue={{
                  ...issueStub,
                  props: {
                    ...issueStub.props,
                    actionStateChange: {
                      ...issueStub.props.actionStateChange,
                      items: [],
                    },
                  },
                }}
              />
            </IssuePropsContext.Provider>,
          );

          expect(screen.queryByText('actionStateChange text')).not.toBeInTheDocument();
        });
      });

      describe('when no access', () => {
        it("doesn't render actionStateChange buttons", () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueHeader
                issue={{
                  ...issueStub,
                  props: {
                    ...issueStub.props,
                    actionStateChange: {
                      ...issueStub.props.actionStateChange,
                      access: 0,
                    },
                  },
                }}
              />
            </IssuePropsContext.Provider>,
          );

          expect(screen.queryByText('actionStateChange text')).not.toBeInTheDocument();
        });
      });
    });
  });
});
