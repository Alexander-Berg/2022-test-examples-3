#!/usr/bin/perl

use strict;
use warnings;

# $Id$

=pod

    Тест на "интеграцию" авторасклейки в процесс сохранения объявления. 
    Проверяет, что фразы, обработанные авторасклейкой, проходят наши же проверки. 

=cut

use Test::More;
use Lang::Unglue; 
use Direct::Validation::Keywords qw/base_validate_keywords/;

use utf8;

sub validate {
    
    my $phrases = shift;
    
    my $validation_result = base_validate_keywords([split /\s*,[\s,]*/, $phrases]);
    return join ',', @{$validation_result->one_error_text_by_objects};
}

sub ug {
    my ($in, $len) = @_;
    my $i = 1;
    my $block = [
        {
            bid => 1,
            Phrases => [
                map { s/^\s+|\s+$//g; {phrase=>$_, md5 => $i++} } split(/,/, $in)
                ]
        }
    ];
    unglue_phrases($block, $len);
    return join ", ", map {$_->{phrase}.$_->{phrase_unglued_suffix}} @{ $block->[0]->{Phrases} };
}

# набор тестов -- массив наборов фраз
my @testcases = (
    'песни тексты песен, текст песни',
    'радиоуправляемые бои танков, радиоуправляемые бои танков танковые ',
);

Test::More::plan(tests => scalar(@testcases) );

my $i;
for my $phrases ( @testcases ){
    my $unglued_phrases = ug($phrases);
    is(validate($unglued_phrases), '', "validate " . $i++);
}

