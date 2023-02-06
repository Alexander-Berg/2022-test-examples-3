#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use utf8;

use Test::More;

use BS::ExportWorker ();

cmp_ok(scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS),
       '==',
       scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS_RET),
       '@SELECT_FIELDS_BIDS and @SELECT_FIELDS_BIDS_RET has equal size',
       );
cmp_ok(scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS),
       '==',
       scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS_DYN),
       '@SELECT_FIELDS_BIDS and @SELECT_FIELDS_BIDS_DYN has equal size',
       );
cmp_ok(scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS),
       '==',
       scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS_PERF),
       '@SELECT_FIELDS_BIDS and @SELECT_FIELDS_BIDS_PERF has equal size',
       );
cmp_ok(scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS),
    '==',
    scalar(@BS::ExportWorker::SELECT_FIELDS_BIDS_BASE),
    '@SELECT_FIELDS_BIDS and @SELECT_FIELDS_BIDS_BASE has equal size',
);

my $as_pattern = qr/\s+AS\s+(.*)$/i;

my %arrays_to_test = (
    SELECT_FIELDS_BIDS => \@BS::ExportWorker::SELECT_FIELDS_BIDS,
    SELECT_FIELDS_BIDS_RET => \@BS::ExportWorker::SELECT_FIELDS_BIDS_RET,
    SELECT_FIELDS_BIDS_DYN => \@BS::ExportWorker::SELECT_FIELDS_BIDS_DYN,
    SELECT_FIELDS_BIDS_PERF => \@BS::ExportWorker::SELECT_FIELDS_BIDS_PERF,
    SELECT_FIELDS_BIDS_BASE => \@BS::ExportWorker::SELECT_FIELDS_BIDS_BASE,
);

for my $field_num (0 .. $#BS::ExportWorker::SELECT_FIELDS_BIDS) {
    my %field_names;
    for my $ar_name (keys %arrays_to_test) {
        my $f = $arrays_to_test{$ar_name}->[$field_num];
        if ($f =~ $as_pattern) {
            $field_names{$ar_name} = $1;
        }
        else {
            $field_names{$ar_name} = $f;
        }
        $field_names{$ar_name} =~ s!^\w+\.!!;
    }
    my $valid_name = $field_names{SELECT_FIELDS_BIDS};
    my %invalid = map { $_ => $field_names{$_} } grep { $field_names{$_} ne $valid_name } keys %field_names;
    ok(scalar keys %invalid == 0,
        "all column names are the same ('$valid_name') for field N $field_num "
        .( join ", ", map { "($_ : $invalid{$_})" } keys %invalid )
    );
}

done_testing();

