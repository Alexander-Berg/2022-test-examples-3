const executeSequentially = function(tasks) {
    if (tasks && tasks.length > 0) {
        let task = tasks.shift();

        return task().then(function() {
            return executeSequentially(tasks);
        });
    }

    return Promise.resolve();
};
/**
 * Выбор значения в sField
 * @param {String} block - селектор s-field
 * @param {String} value - значение. Дату передавать в виде 'MM-DD-YYYY'.
 * @param {String} type
 * @param {Object} data
 * @returns {Object}
 */
module.exports = function setReactSFieldValue(block, value, type, data) {
    if (type === 'input') {
        return this
            .clearFieldValue(`${block} input`)
            .setValue(`${block} input`, value);
    }
    if (type === 'textarea') {
        return this
            .clearFieldValue(`${block} textarea`)
            .setValue(`${block} textarea`, value);
    }
    if (type === 'macros-textarea') {
        return this
            .setValue(`${block} .MacrosInput-Control`, value);
    }
    if (type === 'radio') {
        return this
            .click(`${block} input[value=${value}]`)
            .pause(500);
    }
    if (type === 'checkbox') {
        // Не имплеменчу unclick пока
        return this
            .click(`${block} input`);
    }
    if (type === 'checkbox-confirmed') {
        return this
            .disableAnimations('*')
            .click(`${block} input`)
            .pause(300)
            .patchStyle('body', { overflow: 'visible' })
            .staticElement('.Modal:last-child')
            .staticElement('.Modal:last-child .Modal-Wrapper')
            .waitForVisible('.Modal:last-child .FModal-Content')
            .pause(500)
            .patchStyle('.Modal:last-child .FModal-Content', { maxHeight: '100%' })
            .then(() => {
                if (data && data.screenshot) {
                    return this.assertView(data.screenshot, '.Modal:last-child .FModal-Content');
                }
            })
            .click('.Modal:last-child.ExternalOfferForm-ModalContent .SField-ModalButton')
            .waitForHidden('.Modal:last-child .SField-ModalContent')
            .unStaticElement('.Modal:last-child')
            .unStaticElement('.Modal:last-child .Modal-Wrapper');
    }
    if (type === 'checkbox-group') {
        return this
            .click(`${block} input[name='${value}']`);
    }
    if (type === 'select') {
        return this
            .click(`${block} .Select2-Button`)
            .waitForVisible('.Select2-Popup.Popup2_visible')
            .click(`.Select2-Popup.Popup2_visible .Menu-Item:nth-of-type(${value})`);
    }
    if (type === 'date') {
        const [day, month, year] = value.split('.');
        const _data = data || { lang: 'ru' };
        const lang = _data.lang;

        return this
            .sendKeys(`${block} .DateTimeField-EditableSegment:nth-child(1)`, lang === 'ru' ? day : year)
            .sendKeys(`${block} .DateTimeField-EditableSegment:nth-child(3)`, lang === 'ru' ? month : day)
            .sendKeys(`${block} .DateTimeField-EditableSegment:nth-child(5)`, lang === 'ru' ? year : month);
    }
    if (type === 'time') {
        const [hour, minute] = value.split(':');
        return this
            .setValue(`${block} .DateTimeField-EditableSegment:nth-child(1)`, hour)
            .setValue(`${block} .DateTimeField-EditableSegment:nth-child(3)`, minute);
    }
    if (type === 'attachment') {
        return this
            .patchStyle(`${block} input[type=file]`, {
                display: 'block',
            })
            .uploadFile(value)
            .then(remotePath => {
                return this.setValue(`${block} input[type=file]`, remotePath.value);
            })
            .patchStyle(`${block} input[type=file]`, {
                display: 'none',
            });
    }

    if (type === 'attachments') {
        return this
            .patchStyle(`${block} input[type=file]`, {
                display: 'block',
            })
            .then(() => {
                return Promise.all(value.map(file => {
                    return this.uploadFile(file);
                }));
            })
            .then(remotePaths => {
                // Для гарантии порядка файлов на скриншоте и в запросе
                return executeSequentially(
                    remotePaths.map(path => {
                        return () => this.setValue(`${block} input[type=file]`, path.value);
                    }),
                );
            })
            .patchStyle(`${block} input[type=file]`, {
                display: 'none',
            });
    }

    if (type === 'address') {
        return this
            .clearFieldValue(`${block} textarea`)
            .setValue(`${block} textarea`, value)
            .waitForVisible(`${block} ymaps[class$="-suggest-item-${data.position - 1}"]`)
            .click(`${block} ymaps[class$="-suggest-item-${data.position - 1}"]`)
            .waitForHidden(`${block} ymaps[class*="search__suggest"]`)
            .pause(300);
    }

    return this;
};
