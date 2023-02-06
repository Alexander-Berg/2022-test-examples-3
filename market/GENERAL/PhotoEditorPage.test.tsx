import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { MemoryRouter } from 'react-router';
import { PhotoEditorPage, PhotoEditorPageProps } from './PhotoEditorPage';

function setupProps(newProps: Partial<PhotoEditorPageProps> = {}) {
  const props: PhotoEditorPageProps = {
    finishEditGood: jest.fn(),
    getGoods: jest.fn(),
    startEditGood: jest.fn(),
    goodChangingIds: {},
    goods: [],
    loading: false,
    paging: {},
    totalCount: 0,
    ...newProps,
  };

  return props;
}

describe('<PhotoEditorPage />', () => {
  describe('renders without crashing', () => {
    it('loading is false', () => {
      const div = document.createElement('div');

      ReactDOM.render(
        <MemoryRouter>
          <PhotoEditorPage {...setupProps()} />
        </MemoryRouter>,
        div
      );
      ReactDOM.unmountComponentAtNode(div);
    });

    it('loading is true', () => {
      const div = document.createElement('div');

      ReactDOM.render(
        <MemoryRouter>
          <PhotoEditorPage {...setupProps()} loading />
        </MemoryRouter>,
        div
      );
      ReactDOM.unmountComponentAtNode(div);
    });
  });
});
