'use strict';

const dataWithGallery = require('./with-gallery-data')();

dataWithGallery.data_stub.construct.text = '123456'; // Текст длинною до 6 символов (включительно)

module.exports = dataWithGallery;
