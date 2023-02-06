import React from 'react';
import userEvent from '@testing-library/user-event';
import { act } from '@testing-library/react';

import { ParameterFilterForm } from './ParameterFilterForm';
import { categoryData, parameter, shopModel } from 'src/test/data';
import { ParameterFilter } from 'src/filters';
import { ValueSource } from 'src/java/definitions';
import { setupWithReatom } from 'src/test/withReatom';
import { setAllShopModelsAction, shopModelsAtom, shopModelsListAtom } from 'src/store/shopModels';
import { filterAtom, setFilterAction } from '../../store/filter.atom';
import { categoriesByParentHidAtom, categoryDataAtom, setCategoryData } from 'src/store/categories';
import { currentCategoryDataAtom, currentCategoryIdAtom } from '../../store';

const sources = [ValueSource.FORMALIZATION, ValueSource.MANUAL, ValueSource.RULE];
const parameterFilter: ParameterFilter = {
  parameterId: parameter.id,
  valueSource: sources,
  searchStr: 'пластик',
};

const atoms = {
  shopModelsAtom,
  shopModelsListAtom,
  filterAtom,
  categoriesByParentHidAtom,
  currentCategoryIdAtom,
  categoryDataAtom,
  currentCategoryDataAtom,
};

const actions = [
  setAllShopModelsAction([shopModel]),
  setCategoryData(categoryData),
  setFilterAction({ marketCategoryId: categoryData.hid }),
];

describe('<ParameterFilterForm />', () => {
  test('display selected filters', () => {
    const { app } = setupWithReatom(
      <ParameterFilterForm
        parameter={parameter}
        filter={{ parameterFilters: [parameterFilter] }}
        onChange={jest.fn()}
      />
    );

    // проверяем что в форме выбраны все чекбоксы с источниками
    const checkboxes = app.getAllByRole('checkbox') as HTMLInputElement[];
    const checkeds = checkboxes.filter(el => el.checked).map(el => el.name);
    expect(checkeds).toHaveLength(3);
    sources.forEach(source => {
      expect(checkeds).toContain(source);
    });

    // отображается ли текстовый поиск
    const textSearch = app.getByRole('textbox') as HTMLInputElement;
    expect(textSearch.value).toBe('пластик');
  });

  test('select filters', () => {
    const afterSubmit = jest.fn();
    const { app } = setupWithReatom(<ParameterFilterForm filter={{}} parameter={parameter} onChange={afterSubmit} />);

    userEvent.click(app.getByText('Правила'));
    userEvent.type(app.getByRole('textbox'), 'пластик');

    // проверяем что все выбранное отображается
    const checkboxes = app.getAllByRole('checkbox') as HTMLInputElement[];
    const checked = checkboxes.find(el => el.name === ValueSource.RULE);
    expect(checked).toBeTruthy();

    const textSearch = app.getByRole('textbox') as HTMLInputElement;
    expect(textSearch.value).toBe('пластик');

    // сохраняем
    userEvent.click(app.getByText('Применить'));

    expect(afterSubmit).toHaveBeenCalledTimes(1);
    expect(afterSubmit.mock.calls[0][0]).toEqual({
      parameterFilters: [
        {
          parameterId: parameter.id,
          searchStr: 'пластик',
          valueSource: ['RULE'],
        },
      ],
    });
  });

  test('show models count', async () => {
    jest.useFakeTimers();
    const onUpdate = jest.fn();

    const { app } = setupWithReatom(
      <ParameterFilterForm filter={{ marketCategoryId: categoryData.hid }} parameter={parameter} onChange={onUpdate} />,
      atoms,
      actions
    );

    app.getByText(/1 товар/);

    const input = app.getByRole('textbox');

    userEvent.type(input, 'fdnfkdnf');
    // у текстового поля есть debounce, поэтому запускаем таймер сами и форсируем отрисовку
    act(() => {
      jest.runAllTimers();
    });
    app.getByText(/0 товаров/);

    // вводим существующее значение у товара
    userEvent.clear(input);
    userEvent.type(input, 'полировка');
    act(() => {
      jest.runAllTimers();
    });
    app.getByText(/1 товар/);

    userEvent.click(app.getByText('Применить'));

    expect(onUpdate).toHaveBeenCalledTimes(1);
  });
});
