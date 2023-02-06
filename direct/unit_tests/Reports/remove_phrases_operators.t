#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;
use Reports::ClientPotential;
use Settings;
use Storable qw/dclone/;


*rmop = sub { 
            my $src = dclone(shift);
            Reports::ClientPotential::remove_phrases_operators($src);
            return $src;
        };

my @tests = (
    {   
        src => ['[телевизор цветной] купить -собака'],
        res => ['телевизор цветной купить -собака'],
    },
    {   
        src => ['!телевизор цветной купить -собака'],
        res => ['телевизор цветной купить -собака'],
    },
    {   
        src => ['телевизор "цветной купить" -собака'],
        res => ['телевизор цветной купить -собака'],
    },
    {   
        src => ['телевизор цветной +с ручкой -собака'],
        res => ['телевизор цветной с ручкой -собака'],
    },
    {   
        src => ['телевизор цветной купить -!собака'],
        res => ['телевизор цветной купить -!собака'],
    },
    {   
        src => ['"!телевизор +с ручкой [цветной купить]"'],
        res => ['телевизор с ручкой цветной купить'],
    },
    {   
        src => ['"!телевизор +с ручкой [цветной купить]"',
                '!телевизор цветной купить -!собака'],
        res => ['телевизор с ручкой цветной купить',
                'телевизор цветной купить -!собака'],
    },
);

foreach my $test (@tests) {
    is_deeply(rmop($test->{src}), $test->{res});
}

done_testing();
