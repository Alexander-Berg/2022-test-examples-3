import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TemplatesMenu } from '../TemplatesMenu';
import { TemplateProviderStub } from './TemplateProvider.stub';
import { TemplatesMenuService } from '../TemplatesMenu.service';

const createTestEnv = () => {
  const templateProviderStub = new TemplateProviderStub();
  const templateTypes = templateProviderStub.getTypes();
  const templateStub = templateProviderStub.getTemplate('1');

  const handleTemplateClickMock = jest.fn();
  const getTemplateListByFilterSpy = jest.spyOn(templateProviderStub, 'getTemplateListByFilter');

  const { container } = render(
    <TemplatesMenu
      templatesMenuService={new TemplatesMenuService(new TemplateProviderStub())}
      onTemplateClick={handleTemplateClickMock}
    />,
  );

  getTemplateListByFilterSpy.mockClear();

  return {
    container,
    templateStub,
    templateTypes,
    handleTemplateClickMock,
    getTemplateListByFilterSpy,
  };
};

describe('TemplatesMenu', () => {
  describe('base render with stub', () => {
    it('renders ui', () => {
      const { container } = createTestEnv();

      expect(container).toMatchSnapshot();
    });
  });

  describe('when change template type', () => {
    it('calls getTemplateListByFilter', () => {
      const { templateTypes, getTemplateListByFilterSpy } = createTestEnv();

      act(() => {
        userEvent.click(screen.getByText(templateTypes[1].caption));
      });

      waitFor(() => {
        expect(getTemplateListByFilterSpy).toBeCalledWith({ typeId: templateTypes[1].id });
      });
    });
  });

  describe('when change search text', () => {
    it('calls getTemplateListByFilter', () => {
      const { templateTypes, getTemplateListByFilterSpy } = createTestEnv();

      act(() => {
        userEvent.type(screen.getByPlaceholderText('Поиск'), 'search');
      });

      waitFor(() => {
        expect(getTemplateListByFilterSpy).toBeCalledWith({
          typeId: templateTypes[0].id,
          text: 'search',
        });
      });
    });
  });

  describe('.onTemplateClick', () => {
    it('calls with template', () => {
      const { templateStub, handleTemplateClickMock } = createTestEnv();

      act(() => {
        userEvent.click(screen.getByText(templateStub.name!));
      });

      expect(handleTemplateClickMock).toBeCalledTimes(1);
      expect(handleTemplateClickMock).toBeCalledWith(templateStub);
    });
  });
});
