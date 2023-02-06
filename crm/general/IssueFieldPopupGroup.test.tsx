import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IssuePropsContext from 'modules/issues/components/Issue/IssuePropsContext';
import { Redux } from 'modules/issues/redux/createRedux';
import { IssueFieldPopupGroup } from './IssueFieldPopupGroup';
import { issueStub } from '../IssueHeader.stubs';

const IssuePropsContextProviderStub = {
  maxAccess: 3,
  issue: issueStub,
  dispatch: jest.fn(),
  redux: {} as Redux,
};

const fieldItemStub = {
  val: 'val',
  text: 'text',
  alert: false,
  redirect: false,
};

const fieldItemWithResolutionsStub = {
  ...fieldItemStub,
  items: [
    {
      val: 'resolution val 1',
      text: 'resolution text 1',
      alert: false,
      redirect: false,
    },
    {
      val: 'resolution val 2',
      text: 'resolution text 2',
      alert: false,
      redirect: false,
    },
  ],
};

describe('IssueFieldPopupGroup', () => {
  describe('props.fieldItem', () => {
    describe('when defined', () => {
      it('renders IssueFiled as button', () => {
        render(
          <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
            <IssueFieldPopupGroup name="actionStateChange" fieldItem={fieldItemStub} />
          </IssuePropsContext.Provider>,
        );

        expect(screen.getAllByRole('button').length).toBe(1);

        expect(screen.getByText(fieldItemStub.text)).toBeInTheDocument();
      });
    });
  });

  describe('props.groupMode', () => {
    describe('when fieldItem.items defined and more than 0', () => {
      describe("when equal 'popup'", () => {
        it('renders IssueFiled as button with popup', () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueFieldPopupGroup
                name="actionStateChange"
                fieldItem={fieldItemWithResolutionsStub}
                groupMode="popup"
              />
            </IssuePropsContext.Provider>,
          );

          expect(screen.getAllByRole('button').length).toBe(1);

          expect(
            screen.queryByText(fieldItemWithResolutionsStub.items[0].text),
          ).not.toBeInTheDocument();
          expect(
            screen.queryByText(fieldItemWithResolutionsStub.items[1].text),
          ).not.toBeInTheDocument();

          userEvent.click(screen.getByRole('button'));

          expect(screen.getByRole('presentation')).toBeInTheDocument();

          expect(screen.getByText(fieldItemWithResolutionsStub.items[0].text)).toBeInTheDocument();
          expect(screen.getByText(fieldItemWithResolutionsStub.items[1].text)).toBeInTheDocument();
        });
      });

      describe("when equal 'list'", () => {
        it('renders IssueFiled as list', () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueFieldPopupGroup
                name="actionStateChange"
                fieldItem={fieldItemWithResolutionsStub}
                groupMode="list"
              />
            </IssuePropsContext.Provider>,
          );

          expect(screen.getAllByRole('button').length).toBe(2);

          expect(screen.getByText(fieldItemWithResolutionsStub.items[0].text)).toBeInTheDocument();
          expect(screen.getByText(fieldItemWithResolutionsStub.items[1].text)).toBeInTheDocument();
        });
      });

      describe('when undefined', () => {
        it('renders IssueFiled as button', () => {
          render(
            <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
              <IssueFieldPopupGroup name="actionStateChange" fieldItem={fieldItemStub} />
            </IssuePropsContext.Provider>,
          );

          expect(screen.getAllByRole('button').length).toBe(1);
          expect(screen.getByText(fieldItemStub.text)).toBeInTheDocument();
        });
      });
    });

    describe('when fieldItem.items undefined', () => {
      it('renders IssueFiled as button', () => {
        render(
          <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
            <IssueFieldPopupGroup name="actionStateChange" fieldItem={fieldItemStub} />
          </IssuePropsContext.Provider>,
        );

        expect(screen.getAllByRole('button').length).toBe(1);
        expect(screen.getByText(fieldItemStub.text)).toBeInTheDocument();
      });
    });
  });

  describe('props.pin', () => {
    describe('when defined', () => {
      it('renders button with pin borders', () => {
        const { container } = render(
          <IssuePropsContext.Provider value={IssuePropsContextProviderStub}>
            <IssueFieldPopupGroup
              name="actionStateChange"
              fieldItem={fieldItemStub}
              pin="clear-brick"
            />
          </IssuePropsContext.Provider>,
        );

        expect(container.getElementsByClassName('Button2_pin_clear-brick').length).toBe(1);
      });
    });
  });
});
