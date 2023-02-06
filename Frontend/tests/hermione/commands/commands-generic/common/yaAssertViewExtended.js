/**
 * Делает assertView с захватом пространства вокруг элемента
 * params и name пробрасываются в вызываемый хелпером assertView()
 *
 * @param {String} name - имя скриншота
 * @param {String} selector - захватываемый элемент
 * @param {Object} params - параметры стандартного assertView()
 * @property {Number} params.verticalOffset - вертикальный отступ от границ захватываемого элемента
 * @property {Number} params.horisontalOffset - горизонтальный отступ от границ захватываемого элемента
 * @property {Number} params.container - куда вставлять "накрывающий" элемент
 *
 * @returns {Promise}
 */
module.exports = function yaAssertViewExtended(name, selector, params = {}) {
    const AREA_COVER_ID = 'assert-view-area-cover';

    params.verticalOffset = typeof params.verticalOffset !== 'undefined' ? params.verticalOffset : 50;
    params.horisontalOffset = typeof params.horisontalOffset !== 'undefined' ? params.horisontalOffset : 0;
    params.container = params.container || 'body';

    return this
        .execute(setupCoveringElement, selector, params, AREA_COVER_ID)
        .assertView(name, '#' + AREA_COVER_ID, params)
        .execute(removeCoveringElement, '#' + AREA_COVER_ID);
};

function setupCoveringElement(selector, params, areaCoverId) {
    positionAreaCover(selector, buildCoveringElem(areaCoverId), params);

    function positionAreaCover(selector, coveringElem, params) {
        const targetBox = document.querySelector(selector).getBoundingClientRect();
        const containerBox = document.querySelector(params.container).getBoundingClientRect();

        const winHeight = document.body.offsetHeight;
        const winWidth = document.body.offsetWidth;

        const coverTop = Math.max(0, targetBox.top - params.verticalOffset - containerBox.top);
        const coverHeight = targetBox.height + params.verticalOffset * 2;
        const coverLeft = Math.max(0, targetBox.left - params.horisontalOffset - containerBox.left);
        const coverWidth = targetBox.width + params.horisontalOffset * 2;

        coveringElem.style.top = coverTop + 'px';
        coveringElem.style.left = coverLeft + 'px';
        coveringElem.style.width = coverWidth + 'px';
        coveringElem.style.height = coverHeight + 'px';

        // не выходим за границы страницы
        if (coveringElem.offsetTop < 0) {
            coveringElem.style.top = coverTop + coveringElem.offsetTop + 'px';
        }
        if (coveringElem.offsetTop + coverHeight > winHeight) {
            coveringElem.style.height = winHeight - coverTop + 'px';
        }
        if (coveringElem.offsetLeft < 0) {
            coveringElem.style.left = coverLeft + coveringElem.offsetLeft + 'px';
        }
        if (coveringElem.offsetLeft + coverWidth > winWidth) {
            coveringElem.style.width = winWidth - coveringElem.offsetLeft + 'px';
        }
    }

    function buildCoveringElem(id) {
        const coveringElem = document.createElement('div');
        coveringElem.id = id;
        coveringElem.style.position = 'absolute';
        coveringElem.style.zIndex = '999';
        document.body.appendChild(coveringElem);

        return coveringElem;
    }
}

function removeCoveringElement(coveringElemSelector) {
    const coveringElem = document.querySelector(coveringElemSelector);
    coveringElem && coveringElem.parentNode.removeChild(coveringElem);
}
