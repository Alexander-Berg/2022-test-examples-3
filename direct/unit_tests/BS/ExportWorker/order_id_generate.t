#!/usr/bin/perl

use Test::More;

use BS::ExportWorker ();

my @cids = (123, 45678, 1234567890);
my %expected_order_ids = (
    123  => 100000123,
    45678 => 100045678,
    1234567890  => 1334567890,
);
my $new_order_ids = BS::ExportWorker::fetch_bs_order_ids(\@cids);
is_deeply($new_order_ids, \%expected_order_ids);

done_testing();

