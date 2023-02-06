#!/usr/bin/perl

use Direct::Modern;

use Test::Deep;
use Test::More;

use Yandex::Test::UTF8Builder;

use MobileContent;


use constant DEFAULT => undef;
local $MobileContent::USED_VERSION_PARTS = 2;
*g = \&MobileContent::get_major_os_version;

my @tests = (
    # [$arg, $res]
    [undef, DEFAULT],
    [[], DEFAULT],
    [{}, DEFAULT],
    [sub {}, DEFAULT],
    ['', DEFAULT],
    ['0.0.', DEFAULT],
    ['text', DEFAULT],
    ['text.with.dots', DEFAULT],
    ['.', DEFAULT],
    ['..', DEFAULT],
    ['..1', DEFAULT],
    ['a1..', DEFAULT],
    ['-5.-4', DEFAULT],
    ['-5.0', DEFAULT],
    ['-5', DEFAULT],
    ['.-4', DEFAULT],
    ['0.-5', DEFAULT],
    ['Android M', DEFAULT],
    ['зависит от устройства', DEFAULT],
    ['Cihaza göre değişir', DEFAULT],
    # valid
    [0, DEFAULT],
    ['0', DEFAULT],
    ['0.0', DEFAULT],
    ['0.0.0', DEFAULT],
    [0.1, '0.1'],
    [0.42, '0.42'],
    ['12.34.56', '12.34'],
    ['8.4.1 beta 3', '8.4'],
    ['4.2 или более поздняя', '4.2'],
    # mixed
    ['1..', '1.0'],
    ['.1.', '0.1'],
    ['a.1.', '0.1'],
    ['1.a.', '1.0'],
    ['-2.2.', '0.2'],
    ['2.-2.', '2.0'],
    ['8.4a', '8.0'],
);

Test::More::plan(tests => scalar(@tests) + 1);

for my $t (@tests) {
    is_deeply(g($t->[0]), $t->[1], sprintf('%-20s -> %s', ($t->[0] // 'undef'), ($t->[1] // 'undef')));
}

{
    local $MobileContent::USED_VERSION_PARTS = 3;
    is_deeply(g('98.76.54.32'), '98.76.54', 'Using 3 parts: 98.76.54.32 -> 98.76.54]');
}

done_testing();
