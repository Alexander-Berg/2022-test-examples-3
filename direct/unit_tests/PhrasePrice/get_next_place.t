#!/usr/bin/perl

use Direct::Modern;

use Test::More;

BEGIN { use_ok( 'PhrasePrice', 'get_next_place' ); }

use Yandex::Test::UTF8Builder;
use PlacePrice;

my $CASES = [
    {
        name => 'для API',
        places => {
            'GUARANTEE4' => 'GUARANTEE1',
            'GUARANTEE1' => 'PREMIUM4',
            # тестируем багу :( после исправления удалить эту и раскомментить следующую
            'PREMIUM4' => 'PREMIUM2',
            #'PREMIUM4' => 'PREMIUM3',
            'PREMIUM3' => 'PREMIUM2',
            'PREMIUM2' => 'PREMIUM1',
            'PREMIUM1' => undef,
        },
        params => {
            for => 'API',
        },
    },
    {
        name => 'для фронтенда',
        places => {
            'GUARANTEE4' => 'GUARANTEE1',
            'GUARANTEE1' => 'PREMIUM4',
            'PREMIUM4' => 'PREMIUM2',
            'PREMIUM2' => 'PREMIUM1',
            'PREMIUM1' => undef,
        },
        params => {
            for => 'front',
        }
    },
    {
        name => 'для фронтенда в РМП',
        places => {
            'GUARANTEE4' => 'GUARANTEE1',
            'GUARANTEE1' => 'PREMIUM4',
            'PREMIUM4' => 'PREMIUM1',
            'PREMIUM1' => undef,
        },
        params => {
            for => 'front',
            mobile_content => 1,
        }
    },
    {
        name => 'для заданий мастера цен до рефакторинга позиций',
        places => {
            'GUARANTEE4' => 'GUARANTEE1',
            'GUARANTEE1' => 'PREMIUM4',
            'PREMIUM4' => 'PREMIUM2',
            'PREMIUM2' => 'PREMIUM1',
            'PREMIUM1' => undef,
        },
        params => {
        }
    },
];

for my $case (@$CASES) {
    for my $place (sort keys %{$case->{places}}) {
        my $name = 'Получение следующей после '.$place.' позиции '.$case->{name};
        my $expected_next_place = $case->{places}{$place} ? $PlacePrice::PLACES{$case->{places}{$place}} : undef;
        is(get_next_place($case->{params}, $PlacePrice::PLACES{$place}), $expected_next_place, $name);
    }
}

done_testing;
