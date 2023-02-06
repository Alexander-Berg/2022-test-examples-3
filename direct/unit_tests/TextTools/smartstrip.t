#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;

use TextTools qw/smartstrip/;

# [$before_strip, $after_strip, %O]
my @tests = (
    ['', ''],
    [undef, undef],
    ["  test  \x{ab}zzz\x{bb} phrase  ", 'test "zzz" phrase'],
    ["  test  \x{ab}zzz\x{bb} phrase  ", "test \x{ab}zzz\x{bb} phrase", dont_replace_angle_quotes => 1],
    ["\ntest\nphrase\n", 'test phrase'],
);

Test::More::plan(tests => 2*scalar(@tests));

for my $test (@tests) {
    my ($before_strip, $after_strip, %O) = @$test;
    my $before_strip_copy = $before_strip;
    my $return_after_strip = smartstrip($before_strip_copy, %O);
    is($return_after_strip, $after_strip, 'smartstrip(' . _str_val($before_strip). ') return is ' . _str_val($after_strip));
    is($before_strip_copy, $after_strip, 'smartstrip(' . _str_val($before_strip) . ') argument modified to ' . _str_val($after_strip));
}

sub _str_val {
    my ($val) = @_;

    return (defined $val) ? "'$val'" : 'undef';
}
