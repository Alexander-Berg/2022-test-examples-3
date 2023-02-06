'use strict';

const dataWithGallery = require('./with-gallery-data')();

dataWithGallery.data_stub.construct.text = 'Текст длинною от 33 до 54 символов (включительно)';

module.exports = dataWithGallery;
