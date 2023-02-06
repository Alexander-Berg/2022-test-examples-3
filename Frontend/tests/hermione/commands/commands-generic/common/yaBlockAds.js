module.exports = function() {
    return this
        .execute(function() {
            function hideIfExists(selector) {
                const element = document.querySelector(selector);
                if (element) {
                    element.style.display = 'none';
                }
            }

            hideIfExists('.TopBanner');
            hideIfExists('.BottomBanner');
            hideIfExists('.AdsColumn');
        });
};
