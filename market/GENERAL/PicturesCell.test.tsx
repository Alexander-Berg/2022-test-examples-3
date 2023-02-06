import React from 'react';
import { fireEvent } from '@testing-library/react';

import { shopModel, agValidations } from 'src/test/data/shopModel';
import { PicturesCell } from './PicturesCell';
import { setupWithReatom } from 'src/test/withReatom';

describe('<PicturesCell />', () => {
  test('render PicturesCell', () => {
    const { app } = setupWithReatom(<PicturesCell row={shopModel} />);
    app.getByTestId('picture-mini-view');
  });

  test('render PicturesCell not valid', () => {
    const invalidModel = {
      ...shopModel,
      validationResult: agValidations,
    };
    // закидываем модельку с невалидной картинкой
    const { app } = setupWithReatom(<PicturesCell row={invalidModel} />);
    const text = app.getByTestId('picture-mini-view');

    // смотрим пявился ли индикатор ошибки
    const hasErrorCls = app.container.querySelector('.ErrorCell');
    expect(hasErrorCls).toBeInTheDocument();

    // при наведении должен появиться попап с инфой об ошибке
    fireEvent.mouseOver(text);
    app.getByText(new RegExp(agValidations.errors[1].message, 'i'));
  });

  test('open picture form', () => {
    const invalidModel = {
      ...shopModel,
      validationResult: agValidations,
    };
    // рендерим с реатомом что бы можно было открыть форм с картинками
    const { app } = setupWithReatom(<PicturesCell row={invalidModel} />);
    const text = app.getByTestId('picture-mini-view');
    // смотрим что форма открывается при клике
    fireEvent.click(text);
    app.getByTestId('pictures-form');
  });
});
