(function(window, document, $, BEM) {
    'use strict';

    // Для проверки наличия ошибок в серверном javascript нужно сохранять ajax-ответ
    BEM.channel('serp-request').on('success', function(e, data) {
        window.hermione['ajax-success'] = data;
    });

    BEM.channel('serp-request').on('error', function(e, data) {
        window.hermione['ajax-fail'] = data;
    });

    // addSendListener
    window.hermione['ajax-last-request-settings'] = [];
    window.hermione['ajax-response-urls'] = [];

    $(document).on('ajaxSend', function(event, jqxhr, settings) {
        window.hermione['ajax-last-request-settings'].push(settings);
    });

    $(document).on('ajaxSuccess', function(event, xhr, settings) {
        window.hermione['ajax-response-urls'].push(settings.url);
    });

    // FIXME больше не нужен, поскольку mini-suggest не использует jquery?
    BEM.channel('main-suggest')
        .on('ajaxSend', function(event, url) {
            window.hermione['ajax-last-request-settings'].push({ url: url });
        })
        .on('ajaxSuccess', function(event, url) {
            window.hermione['ajax-response-urls'].push(url);
        });

    // TODO: Вынести общие с hermione ассеты в одно место https://st.yandex-team.ru/SERPJTOJS-526
    BEM.decl('i-request_type_ajax', {
        _get: function(request, onSuccess, onError, params) {
            var parsedRequestUrl = BEM.blocks.uri.parse((params && params.url) || this.params.url),
                isInternalUrl = !parsedRequestUrl.host(),
                requestAuthority,
                currentPageAuthority;

            if (!isInternalUrl) {
                requestAuthority = parsedRequestUrl.uriParts.authority;
                currentPageAuthority = BEM.blocks.uri.parse(window.location.href).uriParts.authority;
                isInternalUrl = requestAuthority === currentPageAuthority;
            }

            if (isInternalUrl) {
                var meta = window.hermione.meta,
                    tpid = meta.tpid,
                    templateExpFlags = meta.templateExpFlags,
                    url = BEM.blocks.uri.parse((params && params.url) || this.params.url);

                templateExpFlags && url.replaceParam('template_exp_flags', templateExpFlags);

                params = params || {};
                params.url = url.replaceParam('tpid', tpid).build();
            }

            this.__base.call(this, request, onSuccess, onError, params);
        }
    });

    (function(hermione) {
        var url = BEM.blocks.uri.parse(window.location.href),
            // находит последнее значение параметра tpid и template_exp_flags в URL
            tpid = (url.getParam('tpid') || []).pop(),
            templateExpFlags = (url.getParam('template_exp_flags') || []).pop();

        hermione.meta = hermione.meta || {};
        hermione.meta.tpid = tpid;

        if (templateExpFlags) {
            hermione.meta.templateExpFlags = templateExpFlags;
        }

        // если в window.hermione уже что-то лежало, доопределим, но не перетрём
        window.hermione = hermione;
    })(window.hermione || {});
})(window, document, $, BEM);
