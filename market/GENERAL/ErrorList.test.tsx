import React from 'react';
import { setupWithReatom } from 'src/test/withReatom';
import { ErrorList, ERROR_IN_PROCESSING, ERROR_BEFORE_PROCESSING } from './ErrorList';
import { shopModel } from 'src/test/data/shopModel';
import { ShopModelProcessingStatus } from 'src/java/definitions';

const errorMessages = [
  'С изображением https://img.best-kitchen.ru/images/products/1/6164/62494740/55096.970.jpg, представленным в 47266935, обнаружены проблемы: изображение НЕ на белом фоне',
];
describe('<ErrorList />', () => {
  test('when shop model in processing', () => {
    const { app } = setupWithReatom(
      <ErrorList
        messages={errorMessages}
        model={{ ...shopModel, shopModelProcessingStatus: ShopModelProcessingStatus.PROCESSING }}
        isAg
      />
    );
    app.getByText(errorMessages[0]);
    app.getByText(ERROR_IN_PROCESSING);
  });

  test('when shop model no sent', () => {
    const { app } = setupWithReatom(
      <ErrorList
        messages={errorMessages}
        model={{ ...shopModel, shopModelProcessingStatus: ShopModelProcessingStatus.NOT_SENT }}
        isAg
      />
    );
    app.getByText(errorMessages[0]);
    app.getByText(ERROR_BEFORE_PROCESSING);
  });
});
