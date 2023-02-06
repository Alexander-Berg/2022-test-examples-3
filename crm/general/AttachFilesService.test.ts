import { waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { AttachFilesService } from './AttachFilesService';
import {
  attachFilesServiceInitialStub,
  attachFileStub,
  responseAttachFilesStub,
} from './AttachFilesService.stubs';

const server = setupServer(
  rest.post(`/actions/${attachFilesServiceInitialStub.objectName}/file/add`, (req, res, ctx) => {
    return res(ctx.json(responseAttachFilesStub));
  }),
  rest.post(
    `/actions/${attachFilesServiceInitialStub.objectName}/file/updateLinks`,
    (req, res, ctx) => {
      return res(ctx.json(responseAttachFilesStub));
    },
  ),
  rest.post(`/actions/${attachFilesServiceInitialStub.objectName}/file/remove`, (req, res, ctx) => {
    return res(ctx.json(responseAttachFilesStub));
  }),
);

describe('AttachFilesService', () => {
  const { name, objectName, formApi, containerId, objId } = attachFilesServiceInitialStub;
  const attachFilesService = new AttachFilesService();
  attachFilesService.init(name, objectName, formApi, containerId, objId);

  const formChange = jest.spyOn(attachFilesService.formApi!, 'change');

  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  afterEach(() => {
    formChange.mockClear();
  });

  it('adds files', async () => {
    expect(attachFilesService.isFetch).toBeFalsy();
    attachFilesService.addFiles([attachFileStub]);
    expect(attachFilesService.isFetch).toBeTruthy();

    formChange.mockClear();

    await waitFor(() => {
      expect(formChange).toBeCalledWith(
        attachFilesServiceInitialStub.name,
        responseAttachFilesStub,
      );
      expect(formChange).toBeCalledWith('containerId', responseAttachFilesStub.containerId);
      expect(attachFilesService.isFetch).toBeFalsy();
    });
  });

  it('updates files', async () => {
    expect(attachFilesService.isFetch).toBeFalsy();
    attachFilesService.updateFiles([objId]);
    expect(attachFilesService.isFetch).toBeTruthy();

    formChange.mockClear();

    await waitFor(() => {
      expect(formChange).toBeCalledWith(
        attachFilesServiceInitialStub.name,
        responseAttachFilesStub,
      );
      expect(formChange).toBeCalledWith('containerId', responseAttachFilesStub.containerId);
      expect(attachFilesService.isFetch).toBeFalsy();
    });
  });

  it('removes files', async () => {
    expect(attachFilesService.isFetch).toBeFalsy();
    attachFilesService.removeFile(objId);
    expect(attachFilesService.isFetch).toBeTruthy();

    formChange.mockClear();

    await waitFor(() => {
      expect(formChange).toBeCalledWith(
        attachFilesServiceInitialStub.name,
        responseAttachFilesStub,
      );
      expect(formChange).toBeCalledWith('containerId', responseAttachFilesStub.containerId);
      expect(attachFilesService.isFetch).toBeFalsy();
    });
  });
});
