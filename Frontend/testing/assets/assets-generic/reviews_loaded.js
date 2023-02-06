/* globals BEM:false */
'use strict';

BEM.DOM.decl('reviews', {
    _setActiveReview: function(review) {
        review.setMod('active');
    }
});
