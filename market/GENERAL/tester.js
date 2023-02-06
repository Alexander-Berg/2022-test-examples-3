$(function() {
    $('form').on('submit', function() {

        $.get('/products', {
            text: $('#text').val(),
            price: $('#price').val(),
            url: $('#url').val(),
            ip: $('#ip').val(),
            v: $('#v').val(),
            partner: $('#partner').val(),
            aff_id: $('#aff_id').val(),
            aff_sub: $('#aff_sub').val(),
            source: $('#source').val(),
            lowest_only: $('#lowest_only').prop('checked'),
            other_reg: $('#other_reg').prop('checked'),
            city: $('#city').val(),
            region: $('#region').val(),
            country: $('#country').val(),
            currency: $('#currency').val(),
            clientid: $('#clientid').val(),
            first_run: $('#first_run').val(),
            activate_time: $('#activate_time').val(),
            opt_in: $('#opt_in').prop('checked'),
            debug: $('#debug').prop('checked'),
            model_id: $('#model_id').val(),
            is_shop: $('#is_shop').prop('checked')
        }).done(function(data) {
            $('.response pre#nodejs').text(JSON.stringify(data, null, 2));
        });

        $.ajax({
            url: 'https://karma.metabar.ru/suggestold/price/lowest',
            data: {
                text: $('#text').val(),
                price: $('#price').val()
            },
            dataType: 'JSONP',
            success: function(data) {
                $('.response pre#python').text(JSON.stringify(data, null, 2));
            }
        });

        return false;
    });
});