#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

BEGIN { use_ok('JavaIntapiClientBase'); }

use open ':std' => ':utf8';

use Direct::ValidationResult qw//;
use Direct::Validation::Errors;


my @items = (
    [[], sub { Direct::ValidationResult->new() }],
    [[{}], sub { Direct::ValidationResult->new() }],
    [[{}, {text => ''}, {text => 'error 1'}, {path => ''}], sub { Direct::ValidationResult->new() }],
    [[{text => 'error 2', path => ''}], sub {
        my $vr = Direct::ValidationResult->new(); 
        $vr->add_generic(error_InvalidField('error 2'));
        return $vr;
    }],
    [[{text => 'error 3', path => 'phone'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        $vr->add(phone => error_InvalidField('error 3'));
        return $vr;
    }],
    [[{text => 'error 4', path => '[0]'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        my $elem_vr = $vr->next;
        $elem_vr->add_generic(error_InvalidField('error 4'));
        return $vr;
    }],
    [[{text => 'error 5', path => '[2]'}, {text => 'error 6', path => '[0]'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        $vr->next->add_generic(error_InvalidField('error 6'));
        $vr->next;
        $vr->next->add_generic(error_InvalidField('error 5'));
        return $vr;
    }],
    [[{text => 'error 7', path => '[2].phone'}, {text => 'error 8', path => '[0].city'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        $vr->next->add(city => error_InvalidField('error 8'));
        $vr->next;
        $vr->next->add(phone => error_InvalidField('error 7'));
        return $vr;
    }],
    [[{text => 'error 8', path => '[0].phone.city_code'}, {text => 'error 9', path => '[0].phone.country_code'}, 
     {text => 'error 10', path => '[0].phone.city_code'}, {text => 'error 11', path => '[0].city'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        my $elem_vr = $vr->next;
        $elem_vr->add(city => error_InvalidField('error 11'));

        my $phone_vr = Direct::ValidationResult->new();
        $phone_vr->add(country_code => error_InvalidField('error 9'));
        $phone_vr->add(city_code => [error_InvalidField('error 8'), error_InvalidField('error 10')]);
        $elem_vr->add(phone => $phone_vr);
        return $vr;
    }],
    [[{text => 'error 12', path => 'phone'}, {text => 'error 13', path => '[0]'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        $vr->add(phone => error_InvalidField('error 12'));
        $vr->next->add_generic(error_InvalidField('error 13'));
        return $vr;
    }],
    [[{text => 'error 14', path => 'phone.city_code'}, {text => 'error 15', path => 'country.city'}], sub {
        my $vr = Direct::ValidationResult->new(); 
        my $phone_vr = Direct::ValidationResult->new();
        $phone_vr->add(city_code => error_InvalidField('error 14'));
        $vr->add(phone => $phone_vr);

        my $country_vr = Direct::ValidationResult->new();
        $country_vr->add(city => error_InvalidField('error 15'));
        $vr->add(country => $country_vr);
        return $vr;
    }],
    [[{text => 'error 16', path => '[2].phone[0][1].city_code[1]'}, {text => 'error 17', path => ''}], sub {
        my $vr = Direct::ValidationResult->new(); 
        $vr->add_generic(error_InvalidField('error 17'));

        my $city_code_vr = Direct::ValidationResult->new();
        $city_code_vr->next;
        $city_code_vr->next->add_generic(error_InvalidField('error 16'));

        my $phone_vr = Direct::ValidationResult->new();
        my $phone_elem_vr = $phone_vr->next;
        $phone_elem_vr->next;
        $phone_elem_vr->next->add(city_code => $city_code_vr);

        $vr->next;
        $vr->next;
        $vr->next->add(phone => $phone_vr);
        return $vr;
    }],
);

foreach my $item (@items) {
    my ($errors, $get_expected_vr) = @$item;
    my $vr = JavaIntapiClientBase->convert_error_list_to_validation_result($errors);

    my $expected_vr = $get_expected_vr->();
    is_deeply($vr, $expected_vr, 'validation result structures should be the same');
}


done_testing();

1;
