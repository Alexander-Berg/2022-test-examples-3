import {leftSpec} from './light-specs/left';
import {rightSpec} from './light-specs/right';
import {getSwaggerDiffBreakingChanges} from '../swagger-diff';
import { Link } from '../utils/inbounder/inbounder';


describe('getSwaggerDiffBreakingChanges with light schemes', () => {
    it('without excluded checks', async () => {
        const diff = await getSwaggerDiffBreakingChanges(leftSpec, rightSpec, []);

        expect(diff).toEqual({
            'parameter-gets-moved': [{
                leftValue: 'query',
                rightValue: 'body',
                method: 'get',
                param: 'whateverId',
                path: '/parameter-gets-moved',
                rule: 'parameter-gets-moved',
            }],
            'parameter-type-changes': [{
                in: 'query',
                leftValue: 'string',
                method: 'get',
                param: 'hyperId',
                path: '/parameter-type-is-going-to-change',
                rightValue: 'number',
                rule: 'parameter-type-changes',
            }],
            'endpoint-was-removed': [{
                method: 'get',
                path: '/an-old-one',
                rule: 'endpoint-was-removed',
            }, {
                method: 'get',
                path: '/path-gonna-be-removed',
                rule: 'endpoint-was-removed',
            }, {
                method: 'post',
                path: '/path-gonna-be-removed',
                rule: 'endpoint-was-removed',
            }],
            'add-required-param': [
                {
                    method: 'get',
                    param: 'requiredParam',
                    path: '/new-required-parameter',
                    rule: 'add-required-param'
                },
                {
                    method: 'get',
                    param: 'new-name-2',
                    path: '/parameter-became-renamed',
                    rule: 'add-required-param'
                },
                {
                    method: 'get',
                    param: 'new-name-4',
                    path: '/parameter-became-renamed',
                    rule: 'add-required-param'
                },
            ],
            'parameter-became-renamed': [
                {
                    in: 'query',
                    leftValue: 'old-name',
                    method: 'get',
                    rule: 'parameter-became-renamed',
                    param: 'old-name',
                    path: '/parameter-became-renamed',
                    rightValue: 'new-name-1'
                }
            ],
            'parameter-became-required': [
                {
                    in: 'query',
                    leftValue: false,
                    method: 'get',
                    rule: 'parameter-became-required',
                    param: 'requiredParam',
                    path: '/parameter-became-required',
                    rightValue: true
                }
            ],
            'parameter-was-removed': [
                {
                    in: 'query',
                    leftValue: true,
                    method: 'get',
                    rule: 'parameter-was-removed',
                    param: 'to-be-removed',
                    path: '/parameter-was-removed',
                    rightValue: false
                }
            ],
            "response-modified": [{
                method: 'get',
                modifiedDefinition: "responseC",
                modifiedProperty: 'responseC_field1',
                path: '/break-object-dto',
                responseName: '200',
                rule: 'response-modified',
                schemaPath: [ new Link('responseA', 'responseA_propertyB'), new Link('responseB', 'responseB_propertyC')]
            }, {
                method: 'get',
                modifiedDefinition: "responseC",
                modifiedProperty: 'responseC_field1',
                path: '/break-array-dto',
                responseName: '200',
                rule: 'response-modified',
                schemaPath: [new Link('Array'), new Link('ArrayItem', 'data')]
            }]
        });
    });

    it('with excluded check - parameter-was-removed', async () => {
        const received = await getSwaggerDiffBreakingChanges(leftSpec, rightSpec, ['parameter-was-removed'])
        expect(received).toEqual({
            'parameter-gets-moved': [{
                leftValue: 'query',
                rightValue: 'body',
                method: 'get',
                param: 'whateverId',
                path: '/parameter-gets-moved',
                rule: 'parameter-gets-moved',
            }],
            'parameter-type-changes': [{
                in: 'query',
                leftValue: 'string',
                method: 'get',
                param: 'hyperId',
                path: '/parameter-type-is-going-to-change',
                rightValue: 'number',
                rule: 'parameter-type-changes',
            }],
            'endpoint-was-removed': [{
                method: 'get',
                path: '/an-old-one',
                rule: 'endpoint-was-removed',
            }, {
                method: 'get',
                path: '/path-gonna-be-removed',
                rule: 'endpoint-was-removed',
            }, {
                method: 'post',
                path: '/path-gonna-be-removed',
                rule: 'endpoint-was-removed',
            }],
            'add-required-param': [
                {
                    method: 'get',
                    param: 'requiredParam',
                    path: '/new-required-parameter',
                    rule: 'add-required-param'
                },
                {
                    method: 'get',
                    param: 'new-name-2',
                    path: '/parameter-became-renamed',
                    rule: 'add-required-param'
                },
                {
                    method: 'get',
                    param: 'new-name-4',
                    path: '/parameter-became-renamed',
                    rule: 'add-required-param'
                },
            ],
            'parameter-became-renamed': [{
                in: 'query',
                leftValue: 'old-name',
                method: 'get',
                rule: 'parameter-became-renamed',
                param: 'old-name',
                path: '/parameter-became-renamed',
                rightValue: 'new-name-1'
            }],
            'parameter-became-required': [{
                in: 'query',
                leftValue: false,
                method: 'get',
                rule: 'parameter-became-required',
                param: 'requiredParam',
                path: '/parameter-became-required',
                rightValue: true
            }],
            "response-modified": [{
                method: 'get',
                modifiedDefinition: "responseC",
                modifiedProperty: 'responseC_field1',
                path: '/break-object-dto',
                responseName: '200',
                rule: 'response-modified',
                schemaPath: [ new Link('responseA', 'responseA_propertyB'), new Link('responseB', 'responseB_propertyC')]
            }, {
                method: 'get',
                modifiedDefinition: "responseC",
                modifiedProperty: 'responseC_field1',
                path: '/break-array-dto',
                responseName: '200',
                rule: 'response-modified',
                schemaPath: [new Link('Array'), new Link('ArrayItem', 'data')]
            }]
        });
    });
});
