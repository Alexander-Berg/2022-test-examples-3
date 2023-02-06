import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { configure } from 'mobx';
import { cleanup } from '@testing-library/react/pure';
import { formStub } from './stub/formStub';
import { DynamicallyModalFormService } from './DynamicallyModalForm.service';

configure({ safeDescriptors: false });

const baseUrl = '/dynamically-form';
const wrongUrl = '/wrongUrl';
const server = setupServer(
  rest.get(`${window.CRM_SPACE_API_HOST}${baseUrl}`, (req, res, ctx) => {
    return res(ctx.json(formStub));
  }),
  rest.get(`${window.CRM_SPACE_API_HOST}${wrongUrl}`, (req, res, ctx) => {
    return res(
      ctx.status(404),
      ctx.json({
        message: `form not found`,
      }),
    );
  }),
  rest.post(`${window.CRM_SPACE_API_HOST}${baseUrl}`, (req, res, ctx) => {
    const { body } = req;
    //@ts-ignore
    const { value } = body.args.fields[0].data;
    return res(
      ctx.json({
        results: value,
      }),
    );
  }),
  rest.post(`${window.CRM_SPACE_API_HOST}${baseUrl}/update-fields`, (req, res, ctx) => {
    //@ts-ignore
    const { value } = req.body.fields[1].data;

    return res(ctx.json(value));
  }),
);

describe('DynamicallyModalFormService', () => {
  let dynamicallyFormService;
  beforeAll(() => {
    server.listen();
  });
  beforeEach(() => {
    dynamicallyFormService = new DynamicallyModalFormService();
  });
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });
  afterAll(() => {
    server.close();
  });

  describe('.handleCloseForm', () => {
    it('calls .ResetAll', () => {
      const resetAll = jest.spyOn(dynamicallyFormService, 'resetAll');
      dynamicallyFormService.handleCloseForm();
      expect(resetAll).toBeCalledTimes(1);
    });
    it('calls formClosingListener', async () => {
      const formClosingHandler = jest.fn();
      dynamicallyFormService.setFormClosingListener(formClosingHandler);
      dynamicallyFormService.handleCloseForm();
      expect(formClosingHandler).toBeCalledTimes(1);
    });
  });

  describe('.handleSuccessSubmit', () => {
    it('calls .ResetAll', () => {
      const resetAll = jest.spyOn(dynamicallyFormService, 'resetAll');
      dynamicallyFormService.handleSuccessSubmit('test');
      expect(resetAll).toBeCalledTimes(1);
    });
    it('calls successSubmitListener', async () => {
      const successSubmitHandler = jest.fn();
      dynamicallyFormService.setSuccessSubmitListener(successSubmitHandler);
      dynamicallyFormService.handleSuccessSubmit('test');
      expect(successSubmitHandler).toBeCalledTimes(1);
      expect(successSubmitHandler).toBeCalledWith('test');
    });
  });

  describe('.loadForm', () => {
    describe('if loaded success', () => {
      it('loads form', async () => {
        await dynamicallyFormService.setUrl(baseUrl).loadForm();
        expect(dynamicallyFormService.formData).toEqual(formStub);
      });
    });
    describe('if loading failed', () => {
      it('sets loading error', async () => {
        const mockError = new Error('form not found');
        try {
          await dynamicallyFormService.setUrl(wrongUrl).loadForm();
        } catch (error) {
          expect(error).toEqual(mockError);
        }
        expect(dynamicallyFormService.loadError).toEqual(mockError);
      });
    });
  });
  describe('.submitForm', () => {
    it('submits form with correct data', async () => {
      const handleSuccessSubmit = jest.fn();
      dynamicallyFormService.setSuccessSubmitListener(handleSuccessSubmit);
      await dynamicallyFormService.setUrl(baseUrl).loadForm();
      expect(dynamicallyFormService.formData).toEqual(formStub);
      await dynamicallyFormService.submitForm({ TextFieldId: 'new text value' });
      expect(handleSuccessSubmit).toBeCalledWith('new text value');
    });

    it('calls .resetAll after success submit', async () => {
      await dynamicallyFormService.setUrl(baseUrl).loadForm();
      expect(dynamicallyFormService.formData).toEqual(formStub);
      const resetAll = jest.spyOn(dynamicallyFormService, 'resetAll');
      await dynamicallyFormService.submitForm({ TextFieldId: 'new text value' });
      expect(resetAll).toBeCalledTimes(1);
    });
  });

  describe('.onChange', () => {
    it('updates fields if isFieldsUpdateNeeded is true', async () => {
      await dynamicallyFormService.setUrl(baseUrl).loadForm();
      expect(dynamicallyFormService.formData).toEqual(formStub);
      dynamicallyFormService.setFormData = jest.fn();

      const value = 'New text';
      await dynamicallyFormService.onChange(
        { name: 'TextFieldId2', value },
        dynamicallyFormService.initialValues,
      );

      expect(dynamicallyFormService.setFormData).toBeCalledTimes(1);
      expect(dynamicallyFormService.setFormData).toBeCalledWith(value);
    });

    it("doesn't update fields if isFieldsUpdateNeeded is false", async () => {
      await dynamicallyFormService.setUrl(baseUrl).loadForm();
      expect(dynamicallyFormService.formData).toEqual(formStub);
      dynamicallyFormService.setFormData = jest.fn();

      const value = 'New Text';
      await dynamicallyFormService.onChange(
        { name: 'TextFieldId', value },
        dynamicallyFormService.initialValues,
      );

      expect(dynamicallyFormService.setFormData).toBeCalledTimes(0);
    });
  });
});
