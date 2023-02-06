module.exports = function(selector) {
    return this.selectorExecute(selector, function(forms) {
        forms.forEach(function(form) {
            form.addEventListener('submit', function(e) {
                e.preventDefault();
            });
        });
    });
};
