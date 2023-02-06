import Immutable from 'seamless-immutable';

import type {State} from 'reducers';
import type {ModelForecastId} from 'entities/modelForecasts/types';
import * as cat from 'spec/cat/componentsHelpers';
import {mountPidget} from 'pidgets/testHelpers/mountPidget';

import type {PidgetState} from '../../state';
import {IncutsPromotionForm} from '../../';

const getState = (initialState = {}) =>
    Immutable.from({
        collections: {
            incutColors: {},
            incuts: {},
            brands: {},
            modelForecasts: {},
            categories: {},
            vendors: {},
        },
    }).merge(initialState, {deep: true}) as Partial<State>;

const setting = [
    {
        control: 'BUTTON',
        supportedTransitions: [
            'NEW_TO_DRAFT',
            'DRAFT_SELF',
            'MODERATION_FAILED_TO_AWAITING_MODERATION_OR_SELF',
            'ACTIVATION_READY_TO_AWAITING_MODERATION_OR_SELF',
            'ACTIVE_TO_ACTIVATING',
            'INACTIVE_SELF',
            'NEW_TO_ACTIVATION_READY',
            'NEW_TO_AWAITING_MODERATION',
            'DRAFT_TO_ACTIVATION_READY',
            'DRAFT_TO_AWAITING_MODERATION',
        ],
    },
];

const defaultMetaState = {
    AUTOBANNER_WITH_MODELS: {
        readOnly: [],
        hidden: [],
        transitions: ['NEW_TO_DRAFT', 'NEW_TO_AWAITING_MODERATION'],
    },
    MODELS: {
        readOnly: [],
        hidden: [],
        transitions: ['NEW_TO_DRAFT', 'NEW_TO_ACTIVATION_READY'],
    },
};

const modelsDrawerState = {
    mainCheckboxState: false,
    modelIdsToShow: [1] as ModelForecastId[],
    isHideTable: true,
    draft: {
        selectedModelIds: [],
        mainCheckboxState: false,
        modelIdsToShow: [],
    },
    selectedModelIds: [1] as ModelForecastId[],
};

cat.describe('Pidget. IncutsPromotionForm', () => {
    cat.test(
        {
            id: 'vendor_auto-1',
            name: 'Test',
        },
        async () => {
            await cat.step('Монтируем пиджет IncutsPromotionForm', () => {
                mountPidget<PidgetState>({
                    globalState: getState(),
                    Component: IncutsPromotionForm,
                    props: {
                        setting,
                    },
                    pidgetState: {
                        type: 'ready',
                        value: {
                            bidRecommendations: {
                                loading: false,
                                hasError: false,
                                value: {},
                            },
                            submitting: false,
                            parentCategories: {
                                items: [],
                                loading: false,
                            },
                            modelsDrawer: modelsDrawerState,
                            autoBannerAdditionalInfo: {
                                loading: false,
                                colors: [],
                            },
                            meta: defaultMetaState,
                        },
                    },
                });
            });
        },
    );
});
