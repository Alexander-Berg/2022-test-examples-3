import {
  OperationStatus,
  UploadImagesRequest,
  UploadImagesResponse,
} from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { ModelType, OperationStatusType, RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ImageType, NormalisedModel, NormalizedImage } from '@yandex-market/mbo-parameter-editor';
import { AnyAction } from 'redux';
import { Subject } from 'rxjs';

import { Mappings } from 'src/shared/common-logs/services/StorageManager';
import { EpicDependencies } from 'src/tasks/common-logs/store/configureStore';
import { imagesActions } from 'src/tasks/common-logs/store/images/actions';
import { RootState } from 'src/tasks/common-logs/store/root/reducer';
import { uploadImagesEpic } from './uploadImagesEpic';

describe('uploadImagesEpic', () => {
  it('works with empty', async () => {
    const action$ = new Subject<AnyAction>();
    const state$ = {} as any;
    const dependencies = {} as EpicDependencies;

    const returned: any[] = [];

    const epic = uploadImagesEpic(action$ as any, state$, dependencies);

    epic.subscribe(data => {
      returned.push(data);
    });

    await action$.next(imagesActions.upload([]));

    expect(returned).toEqual([
      {
        payload: 'IMAGE_UPLOADING',
        type: '@@loader//HIDE',
      },
    ]);
  });
  it('works with image and empty store', async () => {
    const action$ = new Subject<AnyAction>();
    const state = {
      offers: {
        mappings: {
          activeOfferId: {},
        } as Record<string, Mappings>,
        matchings: {
          activeOfferId: {},
        } as Record<string, Mappings>,
      },
      data: { activeOfferId: 'activeOfferId', inputData: {}, taskOfferIds: ['activeOfferId'] },
      images: {
        movedImages: {},
        offerImages: {},
        offerDataImages: {},
        modelImages: {},
      },
      models: { normalisedModels: {} },
    } as RootState;
    const state$ = { value: state } as any;

    const uploadImagesMock = (request: UploadImagesRequest) => {
      const response: UploadImagesResponse = {
        result: { status: OperationStatus.SUCCESS },
        status: [
          ...(request.remote_url || []).map(url => ({ picture: { url } })),
          ...(request.local_image || []).map(({ content_base64 }) => ({ picture: { url: content_base64 } })),
        ],
      };

      return Promise.resolve(response);
    };

    const dependencies = {
      aliasMaker: {
        uploadImages: uploadImagesMock,
      } as any,
    } as EpicDependencies;

    const returned: any[] = [];

    const epic = uploadImagesEpic(action$ as any, state$, dependencies);

    epic.subscribe(data => {
      returned.push(data);
    });

    await action$.next(imagesActions.upload([{ url: 'test', type: ImageType.REMOTE }]));

    expect(returned).toEqual([
      { payload: ['test'], type: '@@images//UPLOADED' },
      { payload: [], type: '@@models//SET_SOME_NORMALISED_MODELS' },
      { payload: {}, type: '@@searchImages//SET_OFFERS' },
      { payload: {}, type: '@@searchImages//SET_OFFERS_DATA' },
      { payload: {}, type: '@@searchImages//SET_MODELS' },
    ]);
  });
  it('works with image', async () => {
    const action$ = new Subject<AnyAction>();

    const model = {
      id: 123,
      currentType: ModelType.GURU,
      title: 'testModelTitle',
      normalizedPictures: [{ url: 'test', type: ImageType.REMOTE }],
      normalizedPickers: [{ url: 'test', type: ImageType.REMOTE }],
    } as NormalisedModel;
    const sku = {
      id: 234,
      currentType: ModelType.SKU,
      title: 'testSkuTitle',
      relations: [{ type: RelationType.SKU_PARENT_MODEL, id: 123 }],
    } as NormalisedModel;

    const images = { activeOfferId: [{ url: 'test', type: ImageType.REMOTE }] } as Record<string, NormalizedImage[]>;
    const state = {
      offers: {
        mappings: {
          activeOfferId: {
            mapping_meta: {
              modelId: model.id,
              skuId: sku.id,
            },
          },
        } as Record<string, Mappings>,
        matchings: {
          activeOfferId: {},
        } as Record<string, Mappings>,
      },
      data: { activeOfferId: 'activeOfferId', inputData: {}, taskOfferIds: ['activeOfferId'] },
      images: {
        movedImages: {},
        offerImages: images,
        offerDataImages: images,
        modelImages: images,
      },
      models: {
        normalisedModels: {
          [model.id]: model,
          [sku.id]: sku,
        },
      },
    } as RootState;
    const state$ = { value: state } as any;

    const uploadImagesMock = (request: UploadImagesRequest) => {
      const response: UploadImagesResponse = {
        result: { status: OperationStatus.SUCCESS },
        status: [
          ...(request.remote_url || []).map(url => ({ picture: { url: `${url}Uploaded` } })),
          ...(request.local_image || []).map(({ content_base64 }) => ({
            picture: { url: `${content_base64}Uploaded` },
          })),
        ],
      };

      return Promise.resolve(response);
    };

    const dependencies = {
      aliasMaker: {
        uploadImages: uploadImagesMock,
      } as any,
    } as EpicDependencies;

    const returned: any[] = [];

    const epic = uploadImagesEpic(action$ as any, state$, dependencies);

    epic.subscribe(data => {
      returned.push(data);
    });

    await action$.next(imagesActions.upload([{ url: 'test', type: ImageType.REMOTE }]));

    expect(returned).toEqual([
      { payload: ['test'], type: '@@images//UPLOADED' },
      {
        payload: [
          {
            currentType: 'GURU',
            id: 123,
            normalizedPickers: [
              { picture: { url: 'testUploaded' }, type: 'REMOTE', url: 'testUploaded', validationMessage: undefined },
            ],
            normalizedPictures: [
              { picture: { url: 'testUploaded' }, type: 'REMOTE', url: 'testUploaded', validationMessage: undefined },
            ],
            title: 'testModelTitle',
          },
        ],
        type: '@@models//SET_SOME_NORMALISED_MODELS',
      },
      {
        payload: {
          activeOfferId: [
            { picture: { url: 'testUploaded' }, type: 'REMOTE', url: 'testUploaded', validationMessage: undefined },
          ],
        },
        type: '@@searchImages//SET_OFFERS',
      },
      {
        payload: {
          activeOfferId: [
            { picture: { url: 'testUploaded' }, type: 'REMOTE', url: 'testUploaded', validationMessage: undefined },
          ],
        },
        type: '@@searchImages//SET_OFFERS_DATA',
      },
      {
        payload: {
          activeOfferId: [
            { picture: { url: 'testUploaded' }, type: 'REMOTE', url: 'testUploaded', validationMessage: undefined },
          ],
        },
        type: '@@searchImages//SET_MODELS',
      },
    ]);
  });
  it('works with failed uploads', async () => {
    const action$ = new Subject<AnyAction>();

    const state = {
      offers: {
        mappings: {
          activeOfferId: {},
        } as Record<string, Mappings>,
        matchings: {
          activeOfferId: {},
        } as Record<string, Mappings>,
      },
      data: { activeOfferId: 'activeOfferId', inputData: {}, taskOfferIds: ['activeOfferId'] },
      images: {
        movedImages: {},
        offerImages: {},
        offerDataImages: {},
        modelImages: {},
      },
      models: { normalisedModels: {} },
    } as RootState;
    const state$ = { value: state } as any;

    const uploadImagesMock = () => {
      const response: UploadImagesResponse = {
        result: { status: OperationStatus.SUCCESS },
        status: [{ status: { status: OperationStatusType.INTERNAL_ERROR } as any }],
      };

      return Promise.resolve(response);
    };

    const dependencies = {
      aliasMaker: {
        uploadImages: uploadImagesMock,
      } as any,
    } as EpicDependencies;

    const returned: any[] = [];

    const epic = uploadImagesEpic(action$ as any, state$, dependencies);

    epic.subscribe(data => {
      returned.push(data);
    });

    await action$.next(imagesActions.upload([{ url: 'test', type: ImageType.REMOTE }]));

    expect(returned).toEqual([
      { payload: ['test'], type: '@@images//UPLOADED' },
      { payload: [], type: '@@models//SET_SOME_NORMALISED_MODELS' },
      { payload: {}, type: '@@searchImages//SET_OFFERS' },
      { payload: {}, type: '@@searchImages//SET_OFFERS_DATA' },
      { payload: {}, type: '@@searchImages//SET_MODELS' },
      { payload: ['test'], type: '@@images//REVERT' },
    ]);
  });
  it('works with upload failure', async () => {
    const action$ = new Subject<AnyAction>();

    const state = {
      offers: {
        mappings: {
          activeOfferId: {},
        } as Record<string, Mappings>,
      },
      data: { activeOfferId: 'activeOfferId' },
      images: {
        movedImages: {},
        offerImages: {},
        offerDataImages: {},
        modelImages: {},
      },
    } as RootState;
    const state$ = { value: state } as any;

    const uploadImagesMock = () => {
      const response: UploadImagesResponse = {
        result: { status: OperationStatus.INTERNAL_ERROR },
      };

      return Promise.resolve(response);
    };

    const dependencies = {
      aliasMaker: {
        uploadImages: uploadImagesMock,
      } as any,
    } as EpicDependencies;

    const returned: any[] = [];

    const epic = uploadImagesEpic(action$ as any, state$, dependencies);

    epic.subscribe(data => {
      returned.push(data);
    });

    await action$.next(imagesActions.upload([{ url: 'test', type: ImageType.REMOTE }]));

    expect(returned).toEqual([
      { payload: 'IMAGE_UPLOADING', type: '@@loader//HIDE' },
      { payload: ['test'], type: '@@images//UPLOADED' },
      { payload: ['test'], type: '@@images//REVERT' },
    ]);
  });
});
