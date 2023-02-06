module.exports = function() {
    return this
        .execute(function() {
            const selectors = ['.MainLayout-Header', '.MainLayout-HeaderTabs'];

            const style = document.createElement('style');
            style.innerText = selectors.join(',') + '{position:static !important}';
            document.body.appendChild(style);
        });
};
