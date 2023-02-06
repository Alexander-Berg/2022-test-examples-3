module.exports = async function ywRemoveShadowFromContainer(selector) {
    await this.execute(function(cardSelector) {
        document.querySelectorAll(cardSelector).forEach(item => {
            // тень вылезает за вьюпорт, поэтому ее надо удалять
            item.style.boxShadow = 'none';
        });
    }, selector);
};
