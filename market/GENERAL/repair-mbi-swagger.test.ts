import {Spec} from 'swagger-schema-official';

import {repairMbiSwagger} from './repair-mbi-swagger';

const originalSpec: Spec = {
    swagger: 'ololo',
    info: {
        title: 'title',
        version: 'version',
    },
    paths: {
        '/no/path/vars': {
            'get': {
                responses: {},
                parameters: [
                    {in: 'query', name: 'query_q1'},
                    {in: 'query', name: 'queryQ2'},
                    {in: 'query', name: 'query'},
                ]
            },
        },
        '/single/{path}/parameter': {
            'get': {
                responses: {},
                parameters: [
                    {in: 'query', name: 'PATH'},
                    {in: 'query', name: 'path'},
                    {in: 'query', name: 'query'},
                ],
            },
        },
        '/several/{path_one}/path/{pathTwo}/parameters/switch/cases': {
            'get': {
                responses: {},
                parameters: [
                    {in: 'query', name: 'pathOne'},
                    {in: 'query', name: 'path_two'},
                    {in: 'query', name: 'pathThree'},
                    {in: 'query', name: 'path_four'},
                ],
            },
            'post': {
                responses: {},
                parameters: [
                    {in: 'query', name: 'path_one'},
                    {in: 'query', name: 'pathTwo'},
                    {in: 'query', name: 'pathThree'},
                    {in: 'query', name: 'path_four'},
                ],
            },
            'put': {
                responses: {},
                parameters: [
                    {in: 'path', name: 'path_one', required: true},
                    {in: 'path', name: 'path_two', required: true},
                    {in: 'query', name: 'pathThree'},
                    {in: 'query', name: 'path_four'},
                ],
            }
        },
    },
}

describe('repairMbiSwagger', () => {
    it('should repair MBI Swagger spec', () => {
        expect(repairMbiSwagger(originalSpec)).toEqual({
            swagger: 'ololo',
            info: {
                title: 'title',
                version: 'version',
            },
            paths: {
                '/no/path/vars': {
                    'get': {
                        responses: {},
                        parameters: [
                            {in: 'query', name: 'query_q1'},
                            {in: 'query', name: 'queryQ2'},
                            {in: 'query', name: 'query'},
                        ]
                    },
                },
                '/single/{path}/parameter': {
                    'get': {
                        responses: {},
                        parameters: [
                            {in: 'query', name: 'PATH'},
                            {in: 'path', name: 'path', required: true},
                            {in: 'query', name: 'query'},
                        ],
                    },
                },
                '/several/{path_one}/path/{pathTwo}/parameters/switch/cases': {
                    'get': {
                        responses: {},

                        parameters: [
                            {in: 'path', name: 'path_one', required: true},
                            {in: 'path', name: 'pathTwo', required: true},
                            {in: 'query', name: 'pathThree'},
                            {in: 'query', name: 'path_four'},
                        ],
                    },
                    'post': {
                        responses: {},
                        parameters: [
                            {in: 'path', name: 'path_one', required: true},
                            {in: 'path', name: 'pathTwo', required: true},
                            {in: 'query', name: 'pathThree'},
                            {in: 'query', name: 'path_four'},
                        ],
                    },
                    'put': {
                        responses: {},
                        parameters: [
                            {in: 'path', name: 'path_one', required: true},
                            {in: 'path', name: 'pathTwo', required: true},
                            {in: 'query', name: 'pathThree'},
                            {in: 'query', name: 'path_four'},
                        ],
                    }
                },
            },
        });
    });
});
