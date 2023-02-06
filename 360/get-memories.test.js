'use strict';

const s = require('serializr');
const schema = require('./get-memories.js');
const deserialize = s.deserialize.bind(s, schema);

test('do nothing for empty', () => {
    const result = deserialize({
        empty: true
    });
    expect(result).toEqual({
        empty: true
    });
});

test('convert kebab case to camel case (no items)', () => {
    const result = deserialize({
        empty: false,
        all_photos_link: 'all_photos_link',
        gallery_tail_link: 'gallery_tail_link'
    });
    expect(result).toEqual({
        empty: false,
        allPhotosLink: 'all_photos_link',
        galleryTailLink: 'gallery_tail_link',
        items: []
    });
});

test('convert kebab case to camel case', () => {
    const result = deserialize({
        empty: false,
        all_photos_link: 'all_photos_link',
        gallery_tail_link: 'gallery_tail_link',
        items: [ {
            id: '1',
            link: 'link',
            image_url: 'image_url',
            title_1: 'title_1',
            title_2: 'title_2'
        } ]
    });
    expect(result).toEqual({
        empty: false,
        allPhotosLink: 'all_photos_link',
        galleryTailLink: 'gallery_tail_link',
        items: [ {
            id: '1',
            link: 'link',
            imageUrl: 'image_url',
            title: 'title_1',
            subtitle: 'title_2'
        } ]
    });
});
