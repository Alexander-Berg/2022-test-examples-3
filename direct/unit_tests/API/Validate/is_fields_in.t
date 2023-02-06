#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 11;
use Test::Deep;

BEGIN { use_ok( 'API::Validate::Structure' ); }

use Yandex::Test::UTF8Builder;
use utf8;

sub valid {
    my ($params, $rule, $test_name) = @_;

    is( API::Validate::Structure::is_fields_in($params, $rule), 1, $test_name );
}

sub invalid {
    my ($params, $rule, $test_name) = @_;

    is( API::Validate::Structure::is_fields_in($params, $rule), 0, $test_name );
}

# одно условие
valid( { Mode => 'SinglePrice', CampaignID => 9946257 }, ['Mode', ['SinglePrice']], 'Правило задано массивом. OK!' );
invalid( { Mode => 'Wizard', CampaignID => 9946257 }, ['Mode', ['SinglePrice']], 'Правило задано массивом. FAIL!' );
valid( { Action => 'Get', SelectionCriteria => {} }, {Action => ['Get']}, 'Правило задано хешом. OK!' );
invalid( { Action => 'Upload', SelectionCriteria => {} }, {Action => ['Get']}, 'Правило задано хешом. FAIL!' );
# пустое условие
valid( { Mode => 'SinglePrice', CampaignID => 9946257 }, [], 'пустой массив игнорируем условие' );
valid( { Mode => 'SinglePrice', CampaignID => 9946257 }, {}, 'пустой хеш игнорируем условие' );
# несколько условий
valid( {Mode => "Wizard", PhrasesType => "Both", MaxPrice => 60.01},
      ['Mode', ['Wizard'], 'PhrasesType', ['Network', 'Both']], 'Правила заданы массивом. OK!' );

invalid( {Mode => "Wizard", PhrasesType => "Search", MaxPrice => 60.01},
        ['Mode', ['Wizard'], 'PhrasesType', ['Network', 'Both']], 'Правила заданы массивом. FAIL!' );

valid( {Mode => "Wizard", PhrasesType => "Both", MaxPrice => 60.01},
      {'Mode' => ['Wizard'], 'PhrasesType' => ['Network', 'Both']}, 'Правила заданы хешом. OK!' );

invalid( {Mode => "Wizard", PhrasesType => "Search", MaxPrice => 60.01},
        {'Mode' => ['Wizard'], 'PhrasesType' => ['Network', 'Both']}, 'Правила заданы хешом. FAIL!' );