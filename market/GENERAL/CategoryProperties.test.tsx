import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import React from 'react';

import { Language, Source, TovarCategoryDto } from 'src/java/definitions';
import { RestService } from 'src/services';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { api } from 'src/test/singletons/apiSingleton';
import { CategoryProperties } from 'src/widgets';

describe('<CategoryProperties />', () => {
  const mockedAlert = jest.fn(() => true);
  const mockedPositiveConfirmFunc = jest.fn(() => true);
  const mockedNegativeConfirmFunc = jest.fn(() => false);

  const onCategoryDeleted = jest.fn((hid: number) => hid);
  const onCategoryMoved = jest.fn();
  const onCategorySaved = jest.fn();
  const onCreateCategory = jest.fn((parentHid: number) => parentHid);

  const policies = [{ id: 1, guiText: 'policy1', reportText: '' }];
  const categories = [
    {
      guruCategoryId: 123,
      guruCategoryName: 'эликсиры',
      name: 'category name',
      hid: 123,
      parentHid: 1,
      published: true,
      tovarId: 1,
      visual: true,
    },
  ];
  const restrictions = [{ id: 1, name: 'restriction' }];
  const category: TovarCategoryDto = {} as TovarCategoryDto;

  async function renderApp() {
    window.alert = mockedAlert;
    window.confirm = mockedPositiveConfirmFunc;
    const Provider = setupTestProvider();
    const view = render(
      <Provider>
        <CategoryProperties
          hid={123}
          onCategoryDeleted={onCategoryDeleted}
          onCategoryMoved={onCategoryMoved}
          onCategorySaved={onCategorySaved}
          onCreateCategory={onCreateCategory}
        />
      </Provider>
    );
    await act(async () => {
      resolveApiRequests(api);
    });
    return view;
  }

  async function changeCheckboxDependentOnClusterizeValue(searchString: string) {
    const testField = screen
      ?.getByText(searchString)
      // eslint-disable-next-line testing-library/no-node-access
      ?.parentElement?.getElementsByTagName('input')
      .item(0);
    userEvent.click(testField!);

    expect(testField!.checked).toBeFalsy();
    // eslint-disable-next-line testing-library/no-node-access
    const clusterize = screen?.getByText('Кластеризовать')?.parentElement?.getElementsByTagName('input')[0];
    expect(clusterize).toBeTruthy();
    userEvent.click(clusterize!);

    userEvent.click(testField!);
    expect(testField!.checked).toBeTruthy();
  }

  function resolveApiRequests(api: MockedApiObject<RestService>) {
    api.productTreeController.getReturnPolicies.next().reject(new Error());
    api.categoryTreeController.getCategories.next().reject(new Error());
    api.productTreeController.getRestrictions.next().reject(new Error());
    api.userController.getUsers.next().reject(new Error());

    api.productTreeController.getTovarCategory.next().resolve({ data: category });
    api.productTreeController.getRecipesCount.next().resolve({ data: 1 });
  }

  it('shows confirmation', async () => {
    await renderApp();

    // eslint-disable-next-line testing-library/no-node-access
    const checkbox = screen.getByText('Кластеризовать').parentElement?.getElementsByTagName('input')[0];
    expect(checkbox).toBeTruthy();
    window.confirm = mockedPositiveConfirmFunc;
    userEvent.click(checkbox!);
    expect(mockedPositiveConfirmFunc).toBeCalled();

    userEvent.click(checkbox!);
    // need to cover else statement of a branch
    window.confirm = mockedNegativeConfirmFunc;
    userEvent.click(checkbox!);
    userEvent.click(checkbox!);
    expect(mockedNegativeConfirmFunc).toBeCalled();
  });

  it('dependent fields publishClusters', async () => {
    await renderApp();
    changeCheckboxDependentOnClusterizeValue('Публиковать кластеры');
  });

  it('dependent fields fixateClusters', async () => {
    await renderApp();
    changeCheckboxDependentOnClusterizeValue('Зафиксировать кластеры');
  });

  it('dependent fields showModelTypes switches on showOffers', async () => {
    await renderApp();
    const selectInput = screen.getByText('Не показывать карточки');
    const DOWN_ARROW = { keyCode: 40 }; // down arrow key code
    fireEvent.keyDown(selectInput, DOWN_ARROW);

    userEvent.click(await screen.findByText('Кластер'));

    // eslint-disable-next-line testing-library/no-node-access
    const showOffersCheckbox = screen.getByText('Показывать офферы').parentElement?.getElementsByTagName('input')[0];
    expect(showOffersCheckbox?.checked).toBeTruthy();
  });

  it('dependent fields showModelTypes switches off showOffers', async () => {
    await renderApp();
    const selectInput = screen.getByText('Не показывать карточки');
    const DOWN_ARROW = { keyCode: 40 }; // down arrow key code
    fireEvent.keyDown(selectInput, DOWN_ARROW);

    userEvent.click(await screen.findByText('Гуру-карточка'));

    // eslint-disable-next-line testing-library/no-node-access
    const showOffersCheckbox = screen.getByText('Показывать офферы').parentElement?.getElementsByTagName('input')[0];
    expect(showOffersCheckbox?.checked).toBeFalsy();
  });

  it('submit form', async () => {
    await renderApp();
    const form = screen.getByLabelText('form');
    expect(form).toBeTruthy();

    const catSearchCheckBox = screen
      .getByText('Не искать по категории')
      // eslint-disable-next-line testing-library/no-node-access
      .parentElement?.getElementsByTagName('input')[0];
    expect(catSearchCheckBox).toBeTruthy();
    userEvent.click(catSearchCheckBox!);

    // eslint-disable-next-line testing-library/no-node-access
    const description = (await screen.findByText('Описание')).parentElement?.getElementsByTagName('textarea')[0];
    expect(description).toBeTruthy();
    const newDescriptionValue = 'Testik Testovich Тестинберг';
    fireEvent.change(description!, { target: { value: newDescriptionValue } });
    userEvent.click(screen.getByText('Сохранить'));
    expect(api.productTreeController.saveTovarCategory.activeRequests()).toEqual([]);

    expect(await screen.findByText('Результат сохранения')).toBeTruthy();

    userEvent.click(screen.getByText('Заблокировать'));
    expect(await screen.findByText('Результат сохранения')).toBeTruthy();

    userEvent.click(screen.getByText('Создать подкатегорию'));
    expect(onCreateCategory).toReturnWith(123);

    window.confirm = mockedPositiveConfirmFunc;
    userEvent.click(screen.getByText('Удалить'));
    await act(async () => {
      api.productTreeController.deleteCategory.next().resolve();
    });
    expect(onCategoryDeleted).toReturnWith(123);
  });

  it('save category', async () => {
    const Provider = setupTestProvider();
    render(
      <Provider>
        <CategoryProperties
          hid={123}
          onCategoryDeleted={onCategoryDeleted}
          onCategoryMoved={onCategoryMoved}
          onCategorySaved={onCategorySaved}
          onCreateCategory={onCreateCategory}
        />
      </Provider>
    );

    await act(async () => {
      const category: TovarCategoryDto = {
        hid: 111,
        parentHid: 333,
        blockedChanges: true,
        published: true,
        guruCategoryId: categories[0].guruCategoryId,
        visualCategoryId: 11,
        returnPolicies: [
          {
            regionId: Language.RUSSIAN,
            policyId: policies[0].id,
          },
        ],
        names: [
          {
            id: 11,
            word: 'pomogite',
            morphological: true,
            language: Language.RUSSIAN,
          },
          {
            id: 22,
            word: 'ya zapert',
            morphological: true,
            language: Language.UKRANIAN,
          },
        ],
        uniqueNames: [
          {
            id: 1,
            word: 'testyara',
            morphological: true,
            language: Language.RUSSIAN,
          },
          {
            id: 2,
            word: 'vsem privet',
            morphological: true,
            language: Language.UKRANIAN,
          },
        ],
        showModelTypes: [Source.GURU, Source.CLUSTER],
        clusterize: true,
        linkedCategories: categories[0].hid.toString(),
      } as TovarCategoryDto;

      api.productTreeController.getReturnPolicies
        .next()
        .resolve({ data: { items: policies, offset: 0, limit: 1, total: 1 } });
      api.categoryTreeController.getCategories.next().resolve(categories);

      api.productTreeController.getRestrictions
        .next()
        .resolve({ data: { items: restrictions, limit: 1, offset: 0, total: 1 } });

      api.productTreeController.getTovarCategory.next().resolve({ data: category });
      api.productTreeController.getRecipesCount.next().resolve({ data: 1 });
    });

    userEvent.click(screen.getByText('Разблокировать'));
    expect(api.productTreeController.saveTovarCategory.activeRequests()).toHaveLength(1);
  });
});
