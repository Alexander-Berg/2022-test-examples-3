#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Yandex::Test::UTF8Builder;
use Test::More tests => 5;
use Test::Deep;
use Settings;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

BEGIN { use_ok( 'Phrase', 'fixate_stopwords' ); }

use utf8;
use open ':std' => ':utf8';

*fs = \&Phrase::fixate_stopwords;

copy_table(PPCDICT, 'stopword_fixation');
do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => 'love is' });
do_insert_into_table(PPCDICT, 'stopword_fixation', { phrase => 'на все' });

my $fix = {};
my $p = [ { phrase => "купить на все деньги love is дешево" } ];
ok(fs($p, $fix));
cmp_deeply($fix, 
    { "купить +на +все деньги love +is дешево" => 
        { 
            original => "купить на все деньги love is дешево", 
            fixation => [ ['+на +все','на все'], ['love +is','love is'] ],
        }
    }
);

$fix = {};
$p = [ { phrase => "купить love is на все" } ];
ok(fs($p, $fix));
cmp_deeply($fix, 
    { "купить love +is +на +все" => 
        { 
            original => "купить love is на все", 
            fixation => [ ['love +is','love is'], ['+на +все','на все'] ],
        }
    }
);


