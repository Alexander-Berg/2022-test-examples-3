block('user').mod('example-skip', 'yes').mode('user-menu')(function() {
    return applyNext().filter(function(item) {
        return item.type !== 'mail';
    });
});
