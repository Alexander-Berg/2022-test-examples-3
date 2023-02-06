import { LcJobsEntityListType } from '../..';

const defaultLocationsData = require('./locations.json');
const defaultServicesData = require('./services.json');
const defaultProfessionsData = require('./professions.json');

export const getDefaultContentData = (type: LcJobsEntityListType) => {
    let defaultContent;

    switch (type) {
        case LcJobsEntityListType.Locations:
            defaultContent = defaultLocationsData?.content?.content?.content || [];
            break;
        case LcJobsEntityListType.Services:
            defaultContent = defaultServicesData?.content?.content?.content || [];
            break;
        case LcJobsEntityListType.Professions:
            defaultContent = defaultProfessionsData?.content?.content?.content || [];
            break;
        default:
            defaultContent = [];
            break;
    }

    return defaultContent.sort(() => Math.random() > 0.5 ? -1 : 1);
};

export const getDefaultEmptyListProps = (type: LcJobsEntityListType) => {
    let defaultContent;

    switch (type) {
        case LcJobsEntityListType.Locations:
            defaultContent = defaultLocationsData?.content?.content?.emptyListProps || {};
            break;
        case LcJobsEntityListType.Services:
            defaultContent = defaultServicesData?.content?.content?.emptyListProps || {};
            break;
        case LcJobsEntityListType.Professions:
            defaultContent = defaultProfessionsData?.content?.content?.emptyListProps || {};
            break;
        default:
            defaultContent = {};
            break;
    }

    return defaultContent;
};
