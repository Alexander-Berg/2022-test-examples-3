const joinPresets = require('../utils/joinPresets');

it('join presets', () => {
    const presets = [
        {
            'default-src': [
                'preset1-default-src',
            ],
        },
        {
            'connect-src': [
                'preset2-connect-src1',
                'preset2-connect-src2',
            ],
            'child-src': [
                'preset2-child-src',
            ],
            'img-src': [
                'preset2-img-src1',
                'preset2-img-src2',
            ],
        },
        {
            'connect-src': [
                'preset3-connect-src1',
                'preset3-connect-src2',
            ],
            'media-src': [
                'preset3-media-src',
            ],
        },
    ];

    expect(joinPresets(presets)).toStrictEqual({
        'default-src': [
            'preset1-default-src',
        ],
        'connect-src': [
            'preset2-connect-src1',
            'preset2-connect-src2',
            'preset3-connect-src1',
            'preset3-connect-src2',
        ],
        'child-src': [
            'preset2-child-src',
        ],
        'img-src': [
            'preset2-img-src1',
            'preset2-img-src2',
        ],
        'media-src': [
            'preset3-media-src',
        ],
    });
});
