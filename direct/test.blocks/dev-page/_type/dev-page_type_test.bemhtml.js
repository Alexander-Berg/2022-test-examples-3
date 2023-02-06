block('dev-page').mod('type', 'test').elem('body')(

    def()(function() {
        return applyNext({
            'ctx.mix': [].concat(this.ctx.mix || []).concat({ block: 'i-ua', js: true })
        });
    }),

    content()(function() {
        return [{ elem: 'uncaught-switcher' }, applyNext()];
    })
);

block('dev-page').mod('type', 'test').elem('uncaught-switcher').content()({
    elem: 'script',
    script: 'var isUncaughtExceptionsAllowed = +localStorage.getItem(\'allowUncaught\') === 1;' +
        'function switchExceptions() {' +
        'localStorage.setItem(\'allowUncaught\', isUncaughtExceptionsAllowed ? 0 : 1);' +
        'window.location.reload()}' +
        'document.write(\'Uncaught exceptions are <strong>\' + (isUncaughtExceptionsAllowed ? \'enabled\' : \'disabled\') + \'</strong>, \');' +
        'document.write(\'<a href="javascript:switchExceptions()">\');' +
        'document.write((isUncaughtExceptionsAllowed ? \'disable\' : \'enable\') + \' them\');' +
        'document.write("</a>");'
});
