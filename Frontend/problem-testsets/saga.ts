import { AxiosRequestConfig } from 'axios';
import { all, call, put, takeEvery, takeLatest, select } from 'redux-saga/effects';
import { getType, ActionType } from 'typesafe-actions';

import { problemTestsetsUrls } from 'common/urls/api';
import { hasMarkdownStatements } from 'common/utils/helpers/problem-settings';
import { IProblemTestsetBriefInfo } from 'common/types/problem-test';
import { ProblemSettingsErrorMessages } from 'common/constants/problem-settings-errors';

import fetch from 'client/common/fetch';
import { notify } from 'client/store/notifications/actions';
import {
    addSample,
    addTestset,
    fetchTestset,
    fetchTestsets,
    refreshTestsets,
    removeTestset,
    updateTestset,
} from 'client/store/problem-testsets/actions';
import i18n from 'client/utils/i18n';
import { updateMarkdownStatementsRequests } from 'client/store/problem-settings/helpers';
import { selectSettings } from 'client/selectors/problem-settings';
import { selectTestsets } from 'client/selectors/problem-testsets';

function* fetchTestsetsSaga(action: ActionType<typeof fetchTestsets.request>) {
    const { problemId } = action.payload;
    const encodedId = decodeURIComponent(problemId);
    const url = problemTestsetsUrls.list.build({ problemId: encodedId });
    const props = { url };

    try {
        const { data } = yield call(fetch, props);

        yield put(fetchTestsets.success({ problemId, data }));
    } catch (error) {
        yield put(fetchTestsets.failure({ problemId, error }));

        yield put(
            notify({
                status: 'FAILURE',
                message: i18n.text({ keyset: 'problem-tests', key: 'testsets__fetch-error' }),
            }),
        );
    }
}

function* fetchTestsetSaga(action: ActionType<typeof fetchTestset.request>) {
    const { problemId, testsetId } = action.payload;
    const encodedId = decodeURIComponent(problemId);
    const url = problemTestsetsUrls.testset.build({ problemId: encodedId, testsetId });
    const props = { url };

    try {
        const { data } = yield call(fetch, props);
        yield put(fetchTestset.success({ problemId, testsetId, data }));
    } catch (error) {
        yield put(fetchTestset.failure({ problemId, testsetId, error }));

        yield put(
            notify({
                status: 'FAILURE',
                message: i18n.text({ keyset: 'problem-tests', key: 'testsets__fetch-tests-error' }),
            }),
        );
    }
}

function* removeTestsetSaga(action: ActionType<typeof removeTestset.request>) {
    const { problemId, testsetId } = action.payload;
    const encodedId = decodeURIComponent(problemId);
    const url = problemTestsetsUrls.testset.build({ problemId: encodedId, testsetId });
    const props: AxiosRequestConfig = {
        method: 'DELETE',
        url,
    };
    // Временное решение, пока не сделаем асинхронную перегенерацию через бэк в CONTEST-7023
    const settings = yield select(selectSettings, problemId);
    const testsets = yield select(selectTestsets, problemId);
    const updatedTestset = testsets.find(
        (testset: IProblemTestsetBriefInfo) => testset.id === testsetId,
    );
    const shouldUpdateStatements =
        updatedTestset.name === 'samples' && hasMarkdownStatements(settings);

    try {
        yield call(fetch, props);
        const requests = updateMarkdownStatementsRequests({
            problemId,
            shouldUpdateStatements,
            settings,
        });
        yield all(requests.map((request: AxiosRequestConfig) => call(fetch, request)));

        yield put(
            notify({
                status: 'SUCCESS',
                message: i18n.text({
                    keyset: 'problem-tests',
                    key: 'remove-testset-success',
                }),
            }),
        );

        yield put(removeTestset.success({ problemId }));
        yield put(refreshTestsets({ problemId }));
    } catch (error) {
        const { code } = error.response.data;

        yield put(
            notify({
                status: 'FAILURE',
                message:
                    ProblemSettingsErrorMessages[code] ??
                    i18n.text({
                        keyset: 'problem-tests',
                        key: 'remove-testset-error',
                    }),
            }),
        );

        yield put(removeTestset.failure({ problemId, error }));
    }
}

function* updateTestsetSaga(action: ActionType<typeof updateTestset.request>) {
    const { problemId, testsetId, data } = action.payload;
    const encodedId = decodeURIComponent(problemId);
    const url = problemTestsetsUrls.testset.build({ problemId: encodedId, testsetId });
    const props: AxiosRequestConfig = {
        method: 'PATCH',
        url,
        data,
    };

    // Временное решение, пока не сделаем асинхронную перегенерацию через бэк в CONTEST-7023
    const settings = yield select(selectSettings, problemId);
    const testsets = yield select(selectTestsets, problemId);
    const updatedTestset = testsets.find(
        (testset: IProblemTestsetBriefInfo) => testset.id === testsetId,
    );
    const shouldUpdateStatements =
        updatedTestset.name === 'samples' && hasMarkdownStatements(settings);

    try {
        const { data } = yield call(fetch, props);
        const requests = updateMarkdownStatementsRequests({
            problemId,
            shouldUpdateStatements,
            settings,
        });
        yield all(requests.map((request: AxiosRequestConfig) => call(fetch, request)));

        yield put(
            notify({
                status: 'SUCCESS',
                message: i18n.text({
                    keyset: 'problem-tests',
                    key: 'update-testset-success',
                }),
            }),
        );

        yield put(updateTestset.success({ problemId, testsetId, data: data[testsetId] }));
        yield put(fetchTestset.request({ problemId, testsetId }));
    } catch (error) {
        const { code } = error.response.data;

        yield put(
            notify({
                status: 'FAILURE',
                message:
                    ProblemSettingsErrorMessages[code] ??
                    i18n.text({
                        keyset: 'problem-tests',
                        key: 'update-testset-error',
                    }),
            }),
        );

        yield put(updateTestset.failure({ error, problemId, testsetId }));
    }
}

function* addTestsetSaga(action: ActionType<typeof addTestset.request>) {
    const { problemId } = action.payload;
    const encodedId = decodeURIComponent(problemId);
    const url = problemTestsetsUrls.list.build({ problemId: encodedId });
    const props: AxiosRequestConfig = {
        method: 'POST',
        url,
    };

    try {
        yield call(fetch, props);

        yield put(
            notify({
                status: 'SUCCESS',
                message: i18n.text({
                    keyset: 'problem-tests',
                    key: 'add-testset-success',
                }),
            }),
        );

        yield put(addTestset.success({ problemId }));
        yield put(refreshTestsets({ problemId }));
    } catch (error) {
        const { code } = error.response.data;

        yield put(
            notify({
                status: 'FAILURE',
                message:
                    ProblemSettingsErrorMessages[code] ??
                    i18n.text({
                        keyset: 'problem-tests',
                        key: 'add-testset-error',
                    }),
            }),
        );

        yield put(addTestset.failure({ problemId, error }));
    }
}

function* addSampleSaga(action: ActionType<typeof addSample.request>) {
    const { problemId } = action.payload;
    const encodedId = decodeURIComponent(problemId);
    const url = problemTestsetsUrls.sample.build({ problemId: encodedId });
    const props: AxiosRequestConfig = {
        method: 'POST',
        url,
    };

    // Временное решение, пока не сделаем асинхронную перегенерацию через бэк в CONTEST-7023
    const settings = yield select(selectSettings, problemId);
    const shouldUpdateStatements = hasMarkdownStatements(settings);

    try {
        yield call(fetch, props);
        const requests = updateMarkdownStatementsRequests({
            problemId,
            shouldUpdateStatements,
            settings,
        });
        yield all(requests.map((request: AxiosRequestConfig) => call(fetch, request)));

        yield put(
            notify({
                status: 'SUCCESS',
                message: i18n.text({
                    keyset: 'problem-tests',
                    key: 'add-example-success',
                }),
            }),
        );

        yield put(addSample.success({ problemId }));
        yield put(refreshTestsets({ problemId }));
    } catch (error) {
        const { code } = error.response.data;

        yield put(
            notify({
                status: 'FAILURE',
                message:
                    ProblemSettingsErrorMessages[code] ??
                    i18n.text({
                        keyset: 'problem-tests',
                        key: 'add-example-error',
                    }),
            }),
        );

        yield put(addSample.failure({ problemId, error }));
    }
}

export default function* () {
    yield all([
        takeLatest(getType(fetchTestsets.request), fetchTestsetsSaga),
        takeLatest(getType(refreshTestsets), fetchTestsetsSaga),
        takeLatest(getType(fetchTestset.request), fetchTestsetSaga),
        takeEvery(getType(removeTestset.request), removeTestsetSaga),
        takeLatest(getType(updateTestset.request), updateTestsetSaga),
        takeEvery(getType(addTestset.request), addTestsetSaga),
        takeLatest(getType(addSample.request), addSampleSaga),
    ]);
}
