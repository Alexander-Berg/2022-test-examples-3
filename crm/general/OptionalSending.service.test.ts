import { FormBySchemeProps } from 'components/FormByScheme/FormByScheme/FormByScheme.types';
import { OptionalSending } from './OptionalSending.service';

const mockScheme: FormBySchemeProps['scheme'] = {
  meta: {
    fieldsVisibility: ['product'],
    fields: [
      {
        options: {
          suggestType: 'Tree',
        },
        id: 'product',
        title: 'Продукт',
        sortable: false,
        isPinned: false,
        type: 'Suggest',
        access: 1,
        provider: '/dicts/product',
        isFieldsUpdateNeeded: false,
      },
    ],
  },
  data: [
    {
      id: '335',
      fields: [
        {
          type: 'Suggest',
          id: 'product',
          data: {
            value: {
              id: '82',
              name: 'Авто.ру',
            },
          },
        },
      ],
    },
  ],
};

const fieldName = 'Suggestproduct';

describe('OptionalSending', () => {
  describe('.toggleFieldSelecting', () => {
    describe('if the field non selected', () => {
      it('selects field', async () => {
        const service = new OptionalSending(mockScheme);
        service.toggleFieldSelecting(fieldName);
        expect(service.isFieldSelected(fieldName)).toBeTruthy();
      });
    });
    describe('if the field selected', () => {
      it('unselects the field', async () => {
        const service = new OptionalSending(mockScheme);
        service.toggleFieldSelecting(fieldName);
        service.toggleFieldSelecting(fieldName);
        expect(service.isFieldSelected(fieldName)).toBeFalsy();
      });
    });
  });
  describe('.onChange', () => {
    describe('if the field unselected', () => {
      it('selects the field', async () => {
        const onChangeMock = jest.fn();
        const service = new OptionalSending(mockScheme, undefined, onChangeMock);
        service.onChange({ name: fieldName }, {});
        expect(service.isFieldSelected(fieldName)).toBeTruthy();
      });
    });
    it('calls original onChange', async () => {
      const onChangeMock = jest.fn();
      const service = new OptionalSending(mockScheme, undefined, onChangeMock);
      service.onChange({ name: fieldName }, {});
      expect(onChangeMock).toBeCalledTimes(1);
    });
  });
  describe('.onSubmit', () => {
    it('calls original onSubmit with correct values', async () => {
      const onSubmitMock = jest.fn();
      const service = new OptionalSending(mockScheme, onSubmitMock);
      const mockValues = { [fieldName]: { name: 'test' } };
      const mockOptionalValues = {
        [fieldName]: {
          optionalSendingField: true,
          needSend: false,
          value: mockValues[fieldName],
        },
      };
      service.onSubmit(mockValues);
      expect(onSubmitMock).toBeCalledWith(mockOptionalValues, []);
    });
  });
});
