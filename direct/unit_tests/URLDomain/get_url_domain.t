#!/usr/bin/perl

#    $Id$

use strict;
use utf8;
use warnings;
use Test::More;
use Test::Deep;
use Test::MockObject::Extends;
use HTTP::Headers;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

use URLDomain;

use constant NOT_FOUND_MSG => "Ваш сервер вернул ошибку: 404 Not Found";
use constant REDIRECT_ERROR_MSG => "В цепочке редиректов более 5 URL";
use constant REDIRECT_ERROR_MSG_COUNTER => "В цепочке редиректов более 9 URL";
use constant SERVER_ERROR_MSG => "Ваш сервер вернул ошибку: 500 Server Error";
use constant TIMEOUT_ERROR_MSG => "Ваш сайт не ответил в течение семи секунд";

my %db = (
    trusted_redirects => {
        original_db => PPCDICT,
        like => 'trusted_redirects',
        rows => [
            {
                domain => 'notcounter1.com',
                redirect_type => 'short',
            },
            {
                domain => 'counter1.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter2.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter3.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter4.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter5.com',
                redirect_type => 'counter',
            },
            {
                domain => 'counter6.com',
                redirect_type => 'counter',
            },
            {
                domain => 'pixel.everesttech.net',
                redirect_type => 'counter',
            },
        ],
    },
    ppc_properties => {
        original_db => PPCDICT,
        like => 'ppc_properties',
        rows => [
            {
                name => 'use_gozora_in_process_images_queue',
                value => 1,
            },
        ],
    },
);

init_test_dataset(\%db);

# каждый эл-т цепочки описывает последовательность запросов к домену и рез-т запроса (ключ domain),
# а также рез-ты с параметром check_for_http_status (ключ result_with_check_for_http_status) и
# без него (ключ result_without_check_for_http_status).
# логика работы тестируемой ф-ии: домен берём из первого несчётчика, а простукиваем либо до нужной глубины (с флагом),
# либо до первого несчётчика.
my @chains = (
    {
        domains => [
            {name => 'http://counter1.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'counter1.com',
            redirect_result_href => 'http://counter1.com',
            msg => 'counter1.com',
            redirect_chain => [
                {
                  url => 'http://counter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'counter1.com',
            redirect_result_href => 'http://counter1.com',
            msg => 'counter1.com',
            redirect_chain => [
                {
                  url => 'http://counter1.com',
                },
            ],
            res => 1,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 404},
        ],
        result_without_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                    url => 'http://counter1.com',
                    label => 'Not found',
                },
            ],
            res => 0,
        },
        result_with_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                    url => 'http://counter1.com',
                    label => 'Not found',
                },
            ],
            res => 0,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 404},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                    url => 'http://notcounter1.com',
                    label => 'Not found',
                },
            ],
            res => 0,
        },
    },
    {
        domains => [
            {name => 'http://notcounter2.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter2.com',
            redirect_result_href => 'http://notcounter2.com',
            msg => 'notcounter2.com',
            redirect_chain => [
                {
                    url => 'http://notcounter2.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter2.com',
            redirect_result_href => 'http://notcounter2.com',
            msg => 'notcounter2.com',
            redirect_chain => [
                {
                    url => 'http://notcounter2.com',
                },
            ],
            res => 1,
        },
    },
    {
        domains => [
            {name => 'http://notcounter2.com', code => 500},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter2.com',
            redirect_result_href => 'http://notcounter2.com',
            msg => 'notcounter2.com',
            redirect_chain => [
                {
                    url => 'http://notcounter2.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => SERVER_ERROR_MSG,
            redirect_chain => [
                {
                    url => 'http://notcounter2.com',
                    label => 'Error',
                },
            ],
            res => 0,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 1031},
        ],
        result_without_check_for_http_status => {
            msg => TIMEOUT_ERROR_MSG,
            redirect_chain => [
                {
                    url => 'http://counter1.com',
                    label => 'Error',
                },
            ],
            res => 0,
        },
        result_with_check_for_http_status => {
            msg => TIMEOUT_ERROR_MSG,
            redirect_chain => [
                {
                    url => 'http://counter1.com',
                    label => 'Error',
                },
            ],
            res => 0,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://www.notcounter1.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  url => 'http://notcounter1.com',
                },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://www.notcounter1.com',
                    url => 'http://notcounter1.com',
                    label => 'http',
                },
                {
                    url => 'http://www.notcounter1.com',
                },
            ],
            'res' => 1,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter1.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  url => 'http://notcounter1.com',
                },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://notcounter1.com',
                    label => 'http',
                },
                {
                    url => 'http://counter1.com',
                },
            ],
            'res' => 1,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    label => 'http',
                    url => 'http://counter1.com',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  redirect => 'http://notcounter1.com',
                  url => 'http://counter1.com',
                  label => 'http',
                },
                {
                  url => 'http://notcounter1.com',
                }
            ],
            'res' => 1,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 404},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://counter2.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter2.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                  redirect => 'http://counter2.com',
                  url => 'http://counter1.com',
                  label => 'http',
                },
                {
                  redirect => 'http://notcounter1.com',
                  url => 'http://counter2.com',
                  label => 'http',
                },
                {
                  redirect => 'http://notcounter2.com',
                  url => 'http://notcounter1.com',
                  label => 'http',
                },
                {
                  url => 'http://notcounter2.com',
                  label => 'Not found',
                },
            ],
            'res' => 0,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 500},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => SERVER_ERROR_MSG,
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                    label => 'Error',
                }
            ],
            'res' => 0
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 1031},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => TIMEOUT_ERROR_MSG,
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                    label => 'Error',
                }
            ],
            'res' => 0
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 500},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  url => 'http://notcounter1.com',
                },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            msg => SERVER_ERROR_MSG,
            redirect_chain => [
                {
                    redirect => 'http://notcounter2.com',
                    url => 'http://notcounter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://counter1.com',
                    url => 'http://notcounter2.com',
                    label => 'http',
                },
                {
                    redirect => 'http://counter2.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://counter2.com',
                    label => 'Error',
                },
            ],
            'res' => 0,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 1031},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  url => 'http://notcounter1.com',
                },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            msg => TIMEOUT_ERROR_MSG,
            redirect_chain => [
                {
                    redirect => 'http://notcounter2.com',
                    url => 'http://notcounter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://counter1.com',
                    url => 'http://notcounter2.com',
                    label => 'http',
                },
                {
                    redirect => 'http://counter2.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://counter2.com',
                    label => 'Error',
                },
            ],
            'res' => 0,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://notcounter2.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://counter2.com',
                    url => 'http://notcounter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://notcounter2.com',
                    url => 'http://counter2.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter2.com',
                },
            ],
            'res' => 1,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 302},
            {name => 'http://counter2.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://notcounter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://notcounter2.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    redirect => 'http://counter2.com',
                    url => 'http://notcounter2.com',
                    label => 'http',
                },
                {
                    url => 'http://counter2.com',
                },
            ],
            'res' => 1,
        },
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com', code => 399},
            {name => 'http://counter1.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'counter1.com',
            redirect_result_href => 'http://counter1.com',
            msg => 'counter1.com',
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                    label => 'predefined',
                },
                {
                    url => 'http://counter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            domain_redir => 'counter1.com',
            redirect_result_href => 'http://counter1.com',
            msg => 'counter1.com',
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                    label => 'predefined',
                },
                {
                    url => 'http://counter1.com',
                },
            ],
            res => 1,
        },
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com', code => 399},
            {name => 'http://counter1.com', code => 404},
        ],
        result_without_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                    label => 'predefined',
                },
                {
                    url => 'http://counter1.com',
                    label => 'Not found',
                },
            ],
            res => 0,
        },
        result_with_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                    label => 'predefined',
                },
                {
                    url => 'http://counter1.com',
                    label => 'Not found',
                },
            ],
            res => 0,
        },
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fnotcounter1.com', code => 399},
            {name => 'http://notcounter1.com', code => 404},
        ],
        result_without_check_for_http_status => {
            msg => 'notcounter1.com',
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fnotcounter1.com',
                    label => 'predefined',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => NOT_FOUND_MSG,
            redirect_chain => [
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fnotcounter1.com',
                    label => 'predefined',
                },
                {
                    url => 'http://notcounter1.com',
                    label => 'Not found',
                },
            ],
            'res' => 0,
        },
    },
    {
        domains => [
            {name => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com', code => 399},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://notcounter1.com', code => 500},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                    label => 'predefined',
                },
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                },
            ],
            res => 1,
        },
        result_with_check_for_http_status => {
            msg => SERVER_ERROR_MSG,
            redirect_chain => [
                {
                    redirect => 'http://counter1.com',
                    url => 'http://pixel.everesttech.net/?url=http%3A%2F%2Fcounter1.com',
                    label => 'predefined',
                },
                {
                    redirect => 'http://notcounter1.com',
                    url => 'http://counter1.com',
                    label => 'http',
                },
                {
                    url => 'http://notcounter1.com',
                    label => 'Error',
                },
            ],
            res => 0,
        },
    },
    {
        domains => [
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://counter3.com', code => 302},
            {name => 'http://counter4.com', code => 302},
            {name => 'http://counter5.com', code => 302},
            {name => 'http://counter6.com', code => 302},
            {name => 'http://counter7.com', code => 302},
            {name => 'http://counter8.com', code => 302},
            {name => 'http://counter9.com', code => 302},
            {name => 'http://counter9.com', code => 200},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'counter7.com',
            redirect_result_href => 'http://counter7.com',
            msg => 'counter7.com',
            redirect_chain => [
                {
                  redirect => 'http://counter2.com',
                  url => 'http://counter1.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter3.com',
                  url => 'http://counter2.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter4.com',
                  url => 'http://counter3.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter5.com',
                  url => 'http://counter4.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter6.com',
                  url => 'http://counter5.com',
                  label => 'http',
                },
                {
                  label => "http",
                  redirect => "http://counter7.com",
                  url => "http://counter6.com",
                },
                { url => "http://counter7.com" },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            warn => REDIRECT_ERROR_MSG_COUNTER,
            domain_redir => 'counter7.com',
            redirect_result_href => 'http://counter7.com',
            msg => 'counter7.com',
            redirect_chain => [
                {
                  redirect => 'http://counter2.com',
                  url => 'http://counter1.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter3.com',
                  url => 'http://counter2.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter4.com',
                  url => 'http://counter3.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter5.com',
                  url => 'http://counter4.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter6.com',
                  url => 'http://counter5.com',
                  label => 'http',
                },
                {
                  label => "http",
                  redirect => "http://counter7.com",
                  url => "http://counter6.com",
                },
                {
                    label => "http",
                    redirect => "http://counter8.com",
                    url => "http://counter7.com",
                },
                {
                    label => "http",
                    redirect => "http://counter9.com",
                    url => "http://counter8.com",
                },
                {
                    label => "http",
                    redirect => "http://counter9.com",
                    url => "http://counter9.com",
                },
                { label => "Error", url => "http://counter9.com" },
            ],
            'res' => 1,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://notcounter2.com', code => 302},
            {name => 'http://notcounter3.com', code => 302},
            {name => 'http://notcounter4.com', code => 302},
            {name => 'http://notcounter5.com', code => 302},
            {name => 'http://notcounter6.com', code => 302},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  url => 'http://notcounter1.com',
                },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            msg => REDIRECT_ERROR_MSG,
            redirect_chain => [
                {
                  redirect => 'http://notcounter2.com',
                  url => 'http://notcounter1.com',
                  label => 'http'
                },
                {
                  redirect => 'http://notcounter3.com',
                  url => 'http://notcounter2.com',
                  label => 'http'
                },
                {
                  redirect => 'http://notcounter4.com',
                  url => 'http://notcounter3.com',
                  label => 'http'
                },
                {
                  redirect => 'http://notcounter5.com',
                  url => 'http://notcounter4.com',
                  label => 'http'
                },
                {
                  redirect => 'http://notcounter6.com',
                  url => 'http://notcounter5.com',
                  label => 'http',
                },
                {
                  url => 'http://notcounter6.com',
                  label => 'Error',
                },
            ],
            'res' => 0,
        },
    },
    {
        domains => [
            {name => 'http://notcounter1.com', code => 302},
            {name => 'http://counter1.com', code => 302},
            {name => 'http://counter2.com', code => 302},
            {name => 'http://counter3.com', code => 302},
            {name => 'http://counter4.com', code => 302},
            {name => 'http://counter5.com', code => 302},
        ],
        result_without_check_for_http_status => {
            domain_redir => 'notcounter1.com',
            redirect_result_href => 'http://notcounter1.com',
            msg => 'notcounter1.com',
            redirect_chain => [
                {
                  url => 'http://notcounter1.com',
                },
            ],
            'res' => 1,
        },
        result_with_check_for_http_status => {
            msg => REDIRECT_ERROR_MSG,
            redirect_chain => [
                {
                  redirect => 'http://counter1.com',
                  url => 'http://notcounter1.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter2.com',
                  url => 'http://counter1.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter3.com',
                  url => 'http://counter2.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter4.com',
                  url => 'http://counter3.com',
                  label => 'http'
                },
                {
                  redirect => 'http://counter5.com',
                  url => 'http://counter4.com',
                  label => 'http',
                },
                {
                  url => 'http://counter5.com',
                  label => 'Error',
                },
            ],
            'res' => 0,
        },
    },
);

sub resp_200 {
    return HTTP::Response->new(200, 'OK', HTTP::Headers->new('X-Yandex-Orig-Http-Code' => 200));
}

sub resp_302 {
    my ($location) = @_;

    return HTTP::Response->new(302, 'Found', HTTP::Headers->new(Location => $location));
}

sub resp_404 {
    return HTTP::Response->new(404, 'Not Found', HTTP::Headers->new('X-Yandex-Orig-Http-Code' => 404));
}

sub resp_500 {
    return HTTP::Response->new(500, 'Server Error', HTTP::Headers->new('X-Yandex-Orig-Http-Code' => 500))
}

sub resp_1031 {
    return HTTP::Response->new(500, 'Server Error', HTTP::Headers->new('X-Yandex-Http-Code' => 1031))
}

# индекс текущей цепочки, данные и индекс домена в текущей цепочке
my $curr_chain = {
  idx => 0,
  domain_idx => 0,
  data => undef,
};

sub item {
    my ($domain_idx) = @_;

    return $curr_chain->{data}->{domains}->[$domain_idx];
}

sub incr_domain_idx {
    ++$curr_chain->{domain_idx};
}

sub first_domain {
    return item(0);
}

sub curr_domain {
    return item($curr_chain->{domain_idx});
}

sub next_domain {
    return item($curr_chain->{domain_idx} + 1);
}

sub reset_curr_chain {
    $curr_chain->{idx} = 0;
    $curr_chain->{domain_idx} = 0;
    $curr_chain->{data} = undef;
}

our $PREDEFINED_REDIRECT_CODE = 399;

my $ua = Test::MockObject::Extends->new('LWP::UserAgent');

sub sr {
    my ($self, $req) = @_;

    my $domain = curr_domain();

    if ( $domain->{code} == $PREDEFINED_REDIRECT_CODE ) {
        # predefined_redirect обрабатывается в get_redirect_chain,
        # поэтому нам нужно сдвинуть указатель вперед на одну позицию
        incr_domain_idx();
        $domain = curr_domain();
    }

    my $resp;

    if ($domain->{code} == 200) {
        $resp = resp_200();
    } elsif($domain->{code} == 302) {
            my $rdomain = next_domain();
            $resp = resp_302($rdomain ? $rdomain->{name} : 'http://dummy.com');
    } elsif($domain->{code} == 404) {
        $resp = resp_404();
    } elsif($domain->{code} == 500) {
        $resp = resp_500();
    } elsif ($domain->{code} == 1031) {
        $resp = resp_1031();
    } else {
        die('unknown code');
    }

    $resp->request($req);

    incr_domain_idx();

    return $resp;
}

$ua->mock('simple_request', \&sr);

*gud = \&URLDomain::get_url_domain;

sub delete_rr {
    my ($result) = @_;

    foreach (@{$result->{redirect_chain}}) {
        delete($_->{request});
        delete($_->{response});
    }
}

my $chain_idx = 0;
foreach my $chain (@chains) {
    ++$chain_idx;
    my $chain_name = "chain_$chain_idx";
    $curr_chain->{idx} = $chain_idx;
    $curr_chain->{data} = $chain;

    # не проверяем http статус
    {
        my $result = gud(first_domain()->{name}, {ua => $ua, no_stderr => 1});
        delete_rr($result);

        cmp_deeply($result, $curr_chain->{data}->{result_without_check_for_http_status}, $chain_name);
    
        # обнуляем доменный индекс для следующего теста
        $curr_chain->{domain_idx} = 0;
    }

    # проверяем http статус
    {
        my $result = gud(first_domain()->{name}, {ua => $ua, check_for_http_status => 1, no_stderr => 1});
        delete_rr($result);

        cmp_deeply($result, $curr_chain->{data}->{result_with_check_for_http_status}, $chain_name);
    }

    reset_curr_chain();
}

done_testing();
