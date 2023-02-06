#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Test::Deep;

use URLDomain qw/clear_banner_href/;

use utf8;
use open ':std' => ':utf8';


my @tests = (
    [	
        {href => "http://yandex.ru"},
        "http://yandex.ru",
        "Usual href with protocol"
    ],
    [	
        {href => "yandex.ru"},
        "http://yandex.ru",
        "Usual href without protocol"
    ],
    [	
        {href => "https://yandex.ru"},
        "https://yandex.ru",
        "Usual href with https protocol"
    ],
    [	
        {href => "ftp://yandex.ru"},
        "ftp://yandex.ru",
        "Usual href with invalid protocol, but still protocol"
    ],
    [	
        {href => "yandex.ru", protocol=>"https"},
        "https://yandex.ru",
        "Usual href with separated protocol"
    ],
    [	
        {href => "yandex.ru", protocol=>"ftp"},
        "ftp://yandex.ru",
        "Usual href with separated protocol"
    ],
    [	
        {href => "", protocol=>"https"},
        "",
        "Empty href"
    ],
    [	
        {href => "yandex.ru:1234/lala.html", protocol=>"https"},
        "https://yandex.ru:1234/lala.html",
        "Usual href with port, filename and separated protocol"
    ],
);


Test::More::plan(tests => scalar (@tests));


# Чистка ссылки
foreach my $test (@tests) {
	my $res = clear_banner_href($test->[0]->{href}, $test->[0]->{protocol});
	is_deeply ($res, $test->[1], $test->[2]);
}


1;
