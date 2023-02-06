#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use utf8;


BEGIN {
    use_ok 'FilterSchema';
}

sub vr2hash{
    my $vr = shift;
    my $vr_hash = $vr->convert_to_hash;

    return {
        generic_errors => [map {$_->{description}} @$vr_hash] ,
    } if ref $vr_hash eq 'ARRAY';
    foreach my $err_item (@{$vr_hash->{objects_results} // {}}){
        next unless $err_item;
        if (ref $err_item eq 'ARRAY'){
             $err_item = { GENERIC =>  [map { $_->{description} } @$err_item ] };
             next;
        }
        foreach my $field (keys %$err_item){
            $err_item->{$field} = [map { $_->{description} } @{$err_item->{$field}}]
        }
    }

    return {
        object_errors =>  $vr_hash->{objects_results},
        generic_errors => [map {$_->{description}} @{$vr_hash->{generic_errors}}],
    }
}

my $validator = FilterSchema->new(filter_type=>'hotels_GoogleHotels');

ok $validator->compiled_schema, 'compilation';

my $rules = [
    {field => 'class', relation => '==', value => [5]},
];
ok $validator->reset->check($rules)->is_valid, 'correct data' ;

    
done_testing;
