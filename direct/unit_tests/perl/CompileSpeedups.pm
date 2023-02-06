package unit_tests::perl::CompileSpeedups;

=head2 NAME

    unit_tests::perl::CompileSpeedups - ускорение тестов на компиляцию за счёт грязных хаков

=head2 DESCRIPTION

=cut

use strict;
use warnings;
use utf8;

BEGIN {
    my %FAKE_REQUIRE = (
        "Text::CLemmer2" => {
            LEM_LANG_RUS => sub {},
            analyze => sub {},
        },
        "YaCatalogLite" => {
            Trees => {},
        },
        "DateTime" => {
            VERSION => \("1.06"),
        },
        );

    no strict;

    while(my ($mod, $data) = each %FAKE_REQUIRE) {
        my $file = ($mod =~ s/::/\//gr).'.pm';
        $INC{$file} = 1;

        while(my ($var, $value) = each %$data) {
            no warnings 'prototype';
            no warnings 'redefine';
            *{"${mod}::$var"} = $value;
        }
    }

    @INC = grep {-e && $_ ne '.'} @INC;

}


1;
