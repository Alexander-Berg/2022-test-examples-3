#!/usr/bin/env perl

use strict;
use warnings;
use utf8;

use Test::More;

BEGIN {
    use_ok('Campaign');
    use_ok('Settings');

    use_ok('Yandex::TimeCommon');
}

my $f = \&Campaign::is_all_money_transfer_available;

my %campaign = (
    type => 'text',
    statusShow => 'Yes',
    statusModerate => 'Yes',
    statusActive => 'Yes',
    finish_time => '0000-00-00 00:00:00',
);

ok((sub { eval { $f->(); } or do { return $@; } })->() =~ /invalid/);

ok(!$f->(\%campaign));

ok(!$f->({%campaign, statusShow => 'No', stopTime => unix2mysql(time - $Settings::TRANSFER_DELAY_AFTER_STOP + 1)}));
ok($f->({%campaign, statusShow => 'No', stopTime => unix2mysql(time - $Settings::TRANSFER_DELAY_AFTER_STOP - 1)}));

ok(!$f->({%campaign, finish_time => unix2mysql(time + 1)}));
ok($f->({%campaign, finish_time => unix2mysql(time - 24*60*60)}));

ok($f->({%campaign, statusModerate => 'New'}));
ok($f->({%campaign, statusModerate => 'No', statusActive => 'No'}));

done_testing();

1;
