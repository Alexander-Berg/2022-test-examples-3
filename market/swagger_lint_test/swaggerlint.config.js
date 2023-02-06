// see https://github.com/antonk52/swaggerlint
module.exports = {
    rules: {
        'expressive-path-summary': false,
        'latin-definitions-only':['', {ignore: ['«', '»']}],
        'no-empty-object-type': true,
        'no-external-refs': false,
        'no-inline-enums': false,
        'no-ref-properties': false,
        'no-single-allof': true,
        'no-trailing-slash': true,
        'object-prop-casing': [
            'camel',
            {
                ignore: [],
            },
        ],
        'only-valid-mime-types': false,
        'parameter-casing': [
            'camel',
            {
                path: 'camel',
                query: 'camel',
                body: 'camel',
                ignore: [],
            },
        ],
        'path-param-required-field': true,
        'required-operation-tags': true,
        'required-parameter-description': true,
        'required-tag-description': true,
    },
    ignore: {
        definitions: []
    }
};
