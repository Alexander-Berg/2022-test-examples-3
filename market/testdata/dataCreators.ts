import type {PlacementModelsState} from '../../types';

export const createEmptyPlacementModels = (): PlacementModelsState => {
    return {
        serviceParts: [],
        models: [],
        detailsByModel: {},
    };
};
