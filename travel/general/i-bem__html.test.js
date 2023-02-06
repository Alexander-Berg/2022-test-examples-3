BEM.TEST.decl({ block : 'i-bem', elem : 'html' }, function() {

    $.each({
        'elem params should be stringified correctly' : {
                input : { block : 'b-html-test', elem : 'e', js : { name : 'val' } } ,
                output : '<div class="b-html-test__e" onclick="return {\'b-html-test__e\':{\'name\':\'val\'}}"></div>'
            },
        'elem params in block should be stringified correctly' : {
                input : {
                    block : 'b-html-test',
                    content : { elem : 'e', js : { name : 'val' }}
                },
                output : '<div class="b-html-test"><div class="b-html-test__e" onclick="return {\'b-html-test__e\':{\'name\':\'val\'}}"></div></div>'
            },
        'elem params in mix should be stringified correctly' : {
                input : {
                    block : 'b-html-test',
                    js : {},
                    mix : [{ block : 'b-html-test-mix', elem : 'e', js : true}]
                },
                output : '<div class="b-html-test b-html-test-mix__e i-bem" onclick="return {\'b-html-test\':{},\'b-html-test-mix__e\':{}}"></div>'
            }
        }, function(name, params) {
            it(name, function() {
                expect(BEM.HTML.build(params.input)).toEqual(params.output);
            });
        });

    it('"stop" method should prevent processing base templates', function() {
        BEM.HTML.decl('b-html-stop-test', {
            onBlock : function(ctx) {
                ctx.tag('a');
            }
        });
        BEM.HTML.decl('b-html-stop-test', {
            onBlock : function(ctx) {
                ctx.stop();
            }
        });

        expect(BEM.HTML.build({ block : 'b-html-stop-test' })).toEqual('<div class="b-html-stop-test"></div>');
    });

});