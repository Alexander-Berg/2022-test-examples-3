#!/usr/bin/perl

use Direct::Modern;

use Yandex::ListUtils qw/xminus/;

use API::DevelopersSubscription qw/filter_cyrillic_domains send_cyrillic_email_domain_notification/;

use Test::More tests => 19;

BEGIN {
    use_ok('API::DevelopersSubscription');
}

my @test_arguments = (
    [ ['qwe@yandex.ru'], ['qwe@yandex.ru'], "latin domain" ],
    [ ['qwe@yandex-team.ru'], ['qwe@yandex-team.ru'], "latin domain with hyphen" ],
    [ ['qwe@yandex2.ru'], ['qwe@yandex2.ru'], "latin domain with number" ],
    [ ['qwe@yandex.ru  '], ['qwe@yandex.ru'], "latin domain with space" ],
    [ ['qwe@direct.yandex.ru'], ['qwe@direct.yandex.ru'], "latin third-level domain" ],
    [ ['qwe@енот.рф'], [], "cyrillic domain" ],
    [ ['qwe@енот.рад.рф'], [], "cyrillic third-level domain" ],
    [ ['qwe@enot.rad.рф'], [], "cyrillic part of domain" ],
    [ ['qwe@енот.рф', 'qwe@yandex.ru'], ['qwe@yandex.ru'], "cyrillic and latin domains" ],
);

for my $test_argument (@test_arguments) {
    my $params;
    for my $email (@{$test_argument->[0]}) {
        $email =~ s/^\s*(.*?)\s*$/$1/;
        push @$params, $email;
    }
    my $wrong_expected = xminus($params, $test_argument->[1]);
    {
        no strict qw/ refs /;
        no warnings qw/ once redefine /;

        *API::DevelopersSubscription::send_cyrillic_email_domain_notification = sub {
            is_deeply(shift, $wrong_expected, 'send emails with errors')
        };
    }
    is_deeply(
        filter_cyrillic_domains($test_argument->[0]), $test_argument->[1], $test_argument->[3]
    );
}





