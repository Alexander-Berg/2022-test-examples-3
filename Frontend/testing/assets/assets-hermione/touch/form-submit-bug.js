(function(document, BEM) {
    // Эмулируем баг неотправки формы в новом табе в iOS и ПП на Android
    // @see SERP-68082, SERP-55186, SERP-69120, SERP-61407
    if (!BEM.blocks['i-ua'].ios && !BEM.blocks['i-global'].param('isSearchAndroidApp')) return;

    document.querySelectorAll('form[target=_blank]').forEach(function(form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
        });
    });
})(document, BEM);
