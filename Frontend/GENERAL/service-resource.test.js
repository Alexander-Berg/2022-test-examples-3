import { createAction } from 'redux-actions';
import { dispatchParallel } from 'tools-access-react-redux/src/parallel';

import {
    resourceObsolete,
    resourceActionMeta,
    ACTION_META_REMOVE,
    ACTION_META_UPDATE,
    resourceServiceTags,
    SERVICE_RESOURCE_TAGS_UPDATE,
    SERVICE_RESOURCE_UPDATE,
    resourceAcceptReject,
    RESOURCE_APPROVE,
    RESOURCE_PROVIDE,
    RESOURCE_REJECT,
    RESOURCE_REMOVE,
    RESOURCE_UPDATE,
    SUPPLIER_RESOURCES_REMOVE,
    SUPPLIER_RESOURCES_UPDATE,
    CONSUMER_RESOURCES_REMOVE,
    CONSUMER_RESOURCES_UPDATE,
    removeServiceResourceData,
    updateAction,
    updateGlobalResourceTags,
    updateLocalResourceTags,
    updateServiceResourceData,
    updateServiceResource,
    ACTION_UPDATE,
    GLOBAL_RESOURCE_TAGS_UPDATE,
} from './service-resource';

import { requestJson } from './request-json';

describe('updateServiceResourceData', () => {
    it('Should create JSON_REQUEST actions', () => {
        const params = {
            id: 1,
            actions: ['some_info', 'meta_info', 'other_info'],
            resource: {
                type: {
                    usage_tag: { id: 2 },
                    supplier: { id: 3 },
                    dependencies: [{ id: 4 }, { id: 5 }],
                },
                obsolete_id: 6,
            },
            supplier_tags: [{ id: 7 }, { id: 8 }],
            service: { id: 9 },
            tags: [{ id: 10 }, { id: 11 }],
        };

        const actual = updateServiceResourceData(params);

        const expected = dispatchParallel([
            requestJson({
                pathname: `/back-proxy/api/resources/resources/${params.resource.obsolete_id}/`,
                query: {
                    fields: 'external_id,name',
                },
            }, RESOURCE_UPDATE),
            requestJson({
                credentials: 'include',
                pathname: '/back-proxy/api/resources/serviceresources/',
                query: {
                    usage_tag: params.resource.type.usage_tag.id,
                    type__in: '4,5',
                    fields: 'name,external_id',

                    service: params.resource.type.supplier.id,
                    tag__in: '7,8',
                    ordering: 'resource__type_id',
                },
            }, SUPPLIER_RESOURCES_UPDATE),
            requestJson({
                pathname: '/back-proxy/api/resources/serviceresources/',
                query: {
                    usage_tag: params.resource.type.usage_tag.id,
                    type__in: '4,5',
                    fields: 'name,external_id',

                    service: params.service.id,
                    tag__in: '10,11',
                    ordering: 'resource__type_id',
                },
            }, CONSUMER_RESOURCES_UPDATE),
            requestJson({
                method: 'POST',
                pathname: `/back-proxy/api/resources/serviceresources/${params.id}/actions/`,
                data: { action: 'meta_info' },
            }, ACTION_META_UPDATE),
        ]);

        expect(actual).toEqual(expected);
    });

    it('Should not create JSON_REQUEST actions', () => {
        const params = {
            id: 1,
            actions: ['some_info', 'other_info'],
            resource: {
                type: {
                    usage_tag: null,
                },
                obsolete_id: null,
            },
        };

        const actual = updateServiceResourceData(params);

        const expected = dispatchParallel([]);

        expect(actual).toEqual(expected);
    });
});

describe('removeServiceResourceData', () => {
    it('Should create JSON_REQUEST actions', () => {
        const actual = removeServiceResourceData();

        const expected = dispatchParallel([
            createAction(RESOURCE_REMOVE)(),
            createAction(SUPPLIER_RESOURCES_REMOVE)(),
            createAction(CONSUMER_RESOURCES_REMOVE)(),
            createAction(ACTION_META_REMOVE)(),
        ]);

        expect(actual).toEqual(expected);
    });
});

describe('resourceActionMeta', () => {
    it('Should return payload.results', () => {
        const result = [];

        expect(resourceActionMeta({}, {
            type: ACTION_META_UPDATE,
            payload: { result },
        }))
            .toBe(result);
    });

    it('Should throw payload', () => {
        const error = new Error();

        expect(() => {
            resourceActionMeta({}, {
                type: ACTION_META_UPDATE,
                error: true,
                payload: error,
            });
        })
            .toThrow(error);
    });
});

describe('resourceObsolete', () => {
    it('Should return payload', () => {
        const payload = { some: 'data' };
        expect(resourceObsolete({}, {
            type: RESOURCE_UPDATE,
            payload,
        }))
            .toBe(payload);
    });

    it('Should throw payload', () => {
        const error = new Error();

        expect(() => {
            resourceObsolete({}, {
                type: RESOURCE_UPDATE,
                error: true,
                payload: error,
            });
        })
            .toThrow(error);
    });
});

describe('resourceServiceTags', () => {
    it('Should return payload.results', () => {
        const results = [];

        expect(resourceServiceTags({}, {
            type: SERVICE_RESOURCE_TAGS_UPDATE,
            payload: { results },
        }))
            .toBe(results);
    });

    it('Should throw payload', () => {
        const error = new Error();

        expect(() => {
            resourceServiceTags({}, {
                type: SERVICE_RESOURCE_TAGS_UPDATE,
                error: true,
                payload: error,
            });
        })
            .toThrow(error);
    });
});

describe('resourceAcceptReject', () => {
    it('Should return null on RESOURCE_APPROVE', () => {
        expect(resourceAcceptReject({}, {
            type: RESOURCE_APPROVE,
        }))
            .toBe(null);
    });

    it('Should throw error on RESOURCE_APPROVE error', () => {
        const error = new Error('ASD');

        expect(() => {
            return resourceAcceptReject({}, {
                type: RESOURCE_APPROVE,
                error: true,
                payload: error,
            });
        })
            .toThrow(error);
    });

    it('Should return null on RESOURCE_PROVIDE', () => {
        expect(resourceAcceptReject({}, {
            type: RESOURCE_PROVIDE,
        }))
            .toBe(null);
    });

    it('Should throw error on RESOURCE_PROVIDE error', () => {
        const error = new Error('ASD');

        expect(() => {
            return resourceAcceptReject({}, {
                type: RESOURCE_PROVIDE,
                error: true,
                payload: error,
            });
        })
            .toThrow(error);
    });

    it('Should return null on RESOURCE_REJECT', () => {
        expect(resourceAcceptReject({}, {
            type: RESOURCE_REJECT,
        }))
            .toBe(null);
    });

    it('Should throw error on RESOURCE_REJECT error', () => {
        const error = new Error('ASD');

        expect(() => {
            return resourceAcceptReject({}, {
                type: RESOURCE_REJECT,
                error: true,
                payload: error,
            });
        })
            .toThrow(error);
    });
});

describe('updateAction', () => {
    it('Should create request json action', () => {
        const id = 42;
        const action = 'foo';

        expect(updateAction(id, action))
            .toEqual(requestJson({
                method: 'POST',
                pathname: `/back-proxy/api/resources/serviceresources/${id}/actions/`,
                data: { action },
            }, ACTION_UPDATE));
    });
});

describe('updateServiceResource', () => {
    it('Should create request json action', () => {
        const id = 42;

        expect(updateServiceResource(id))
            .toEqual(requestJson({
                pathname: `/back-proxy/api/resources/serviceresources/${id}/`,
            }, SERVICE_RESOURCE_UPDATE));
    });
});

describe('updateGlobalResourceTags', () => {
    it('Should create request json action', () => {
        expect(updateGlobalResourceTags())
            .toEqual(requestJson({
                pathname: '/back-proxy/api/v3/resources/tags/',
                query: {
                    page_size: 100500,
                    service__isnull: true,
                },
            }, GLOBAL_RESOURCE_TAGS_UPDATE));
    });
});

describe('updateLocalResourceTags', () => {
    it('Should create request json action', () => {
        const service = 42;

        expect(updateLocalResourceTags({ service }))
            .toEqual(requestJson({
                pathname: '/back-proxy/api/v3/resources/tags/',
                query: {
                    page_size: 100500,
                    service,
                },
            }, SERVICE_RESOURCE_TAGS_UPDATE));
    });
});
