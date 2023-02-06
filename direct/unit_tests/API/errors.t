#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 9;

BEGIN { use_ok( 'API::Errors' ); }

use Yandex::Test::UTF8Builder;
use utf8;

check_errors_hash(%API::Errors::ERRORS);
check_errors_hash(%API::Errors::WARNINGS);

sub check_errors_hash {
    my %errors = @_;
    ok(scalar %errors, ' hash not empty');
    SKIP: {
        ok(check_errors_hash_structure(%errors)
            , ' checking errors hash structure')
            or skip "unconsistent errors hash structure, bailing out", 2;
        ok(( my %errors_by_code =
            map { $errors{$_}->{code} => $_ }
            keys %errors
            ) , " making code => error_data hash"
        );
        ok(scalar(keys %errors_by_code) == scalar(keys %errors)
            , ' checking if all error codes are unique');
    }
}

sub check_errors_hash_structure {
    my %errors = @_;
    foreach my $error_id ( keys %errors ) {
        unless($error_id =~ /^[0-9\w]+$/) {
            return nd("wrong error id $error_id");
        }
        unless($errors{$error_id}) {
            return nd("no error data for $error_id");
        }
        my $e = $errors{$error_id};
        unless($e->{code} =~ /^\d+$/) {
            return nd(qq~wrong code `$e->{code}' for $error_id~);
        }
        unless($e->{string}) {
            return nd("no error string for $error_id");
        }
    }
    return 1;
}

sub nd {
    diag(@_);
    return 0;
}


