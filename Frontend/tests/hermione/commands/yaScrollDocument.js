module.exports = function yaScrollDocument(offsetX = 0, offsetY = 0) {
    return this.execute(function(targetScrollLeft, targetScrollTop) {
        document.documentElement.scrollLeft = targetScrollLeft; // скроллим по оси X
        document.documentElement.scrollTop = targetScrollTop; // скроллим по оси Y
    }, offsetX, offsetY);
};
