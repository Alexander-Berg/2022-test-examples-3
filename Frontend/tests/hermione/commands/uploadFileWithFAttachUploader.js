/**
 * Загружает файл с помощью блока загрузки файла
 * @param {String} selector
 * @param {Array} filesData
 * @returns {Promise}
 */
module.exports = function uploadFileWithFAttachUploader(selector, filesData) {
    return this
        .execute((selector, filesData) => {
            const files = filesData.map(file => new File(file.content, file.name));

            $(selector).bem('f-attach-uploader').trigger(
                'files',
                files
            );
        }, selector, filesData)
        .waitForVisible(`${selector} .f-attach-uploader__file_state_success`);
};
