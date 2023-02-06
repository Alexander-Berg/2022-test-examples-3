import { GetModelRequestParams, Logger, Model, ModelRejected, ApiResponse } from './types';
import { makeLogErrorInterceptor } from './interceptors/logError';
import { ModelsApi } from './ModelsApi';
import { composeResponseInterceptors } from './interceptors/utils';
import { makeCookieRenewInterceptor } from './interceptors/cookieRenew';
import { makeRetryInterceptor } from './interceptors/retry';
import { Api } from './Api';

interface DataItem {
    id: string;
    name: string;
    value: string;
}

enum ModelNames {
    getList = 'get-list',
    getItem = 'get-item',
    doSetItem = 'do-set-item'
}

type Models = {
    [ModelNames.getList]: Model<Record<string, never>, Array<DataItem>>;
    [ModelNames.getItem]: Model<{ id: string }, DataItem>;
    [ModelNames.doSetItem]: Model<DataItem, { ok: boolean }>;
};

class ErrorReporter implements Logger {
    logError(_error: Error) {
        // some actions to log error
    }
}

// service-specific headers needed for particular api endpoint
function getAdditionalHeaders() {
    return {
        'x-yandex-ckey': 'ckey_value',
        'x-yandex-uid': 'uid_value',
    };
}

// service-specific body fields needed for particular api endpoint
function getAdditionalBodyFields() {
    return {
        _exp: 'experiments_string_value',
        _eexp: 'applied_experiments_value',
    };
}

const getNewCkey = () => Promise.resolve();

const ckeyRetryInterceptor = makeRetryInterceptor<Models>((model, modelsApi) => {
    // Выбираем какие модели ретраить
    if (modelsApi.isModelRejected(model.result)) {
        if (model.params.name === ModelNames.doSetItem) {
            return false;
        }

        if (typeof model.result.error === 'string') {
            return model.result.error === 'NO_CKEY';
        }

        return model.result.error.code === 'NO_CKEY';
    }

    return false;
}, {
    // [ModelNames.getList]: {
    //     maxCount: 2,
    // },
},
async() => {
    await getNewCkey();

    return;
});
//
const errorLogInterceptor = makeLogErrorInterceptor<Models>(
    (params: ModelRejected<keyof Models>, apiResponse: ApiResponse<any>,
    ) => {
    // console.log(params);
    });

const cookieRenewInterceptor = makeCookieRenewInterceptor<Models>('');

const api = new Api('https://mail.yandex.ru/web-api/_m', {
    getAdditionalHeaders,
    getAdditionalBodyFields,
}, new ErrorReporter());
const modelsApi = new ModelsApi<Models>(
    api,
    {
        modelsQueryParameter: '_models',
    },
    {
        response: composeResponseInterceptors(
            ckeyRetryInterceptor,
            errorLogInterceptor,
            cookieRenewInterceptor,
        ),
    },
);

modelsApi.request([
    { name: ModelNames.getList, params: {} },
    { name: ModelNames.getItem, params: {} },
] as const).then(result => {
    result[0].name;
    result[1].name;

    if (modelsApi.isModelRejected(result[0])) {
        result[0].name;
        result[0].error;
    }

    if (modelsApi.isModelResolved(result[1])) {
        const items = result[1].data;

        result[1].name;
    }

    if (modelsApi.isModelResolved(result[1])) {
        result[1].data;
    }
});

const itemIds = ['123', '456', '789', '234']; // Some runtime dynamic value
const modelsToFetch: GetModelRequestParams<Models, ModelNames.getItem>[] = itemIds
    .map(id => ({ name: ModelNames.getItem, params: { id } }));

modelsApi.request(modelsToFetch).then(result => {
    result.forEach(r => {
        // eslint-disable-next-line no-console
        console.log(r.name);

        if (modelsApi.isModelResolved(r)) {
            r.data.id;
        }
    });
});

const item: DataItem = {
    id: '123',
    name: 'some-cool-item',
    value: '42',
};
const setItemParams = [
    { name: ModelNames.doSetItem, params: item },
    { name: ModelNames.getItem, params: { id: item.id } },
] as const;

modelsApi.request(setItemParams).then(result => {
    const [_setItemResult, itemModel] = result;

    if (modelsApi.isModelResolved(itemModel)) {
        const updatedItem = itemModel.data;

        itemModel.name;

        // do something with updated item
    }
});

const itemsRequestParams = [
    { name: ModelNames.getItem, params: { id: '123' } },
    { name: ModelNames.getItem, params: { id: '456' } },
] as const;

modelsApi.request(itemsRequestParams).then(result => {
    result.forEach(modelResult => {
        // and vice versa, 'modelsApi.isModelResolved(modelResult)' is available
        if (modelsApi.isModelRejected(modelResult)) {
            // handle error

            return;
        }

        const item = modelResult.data;
        // handle successful model response
    });
});
