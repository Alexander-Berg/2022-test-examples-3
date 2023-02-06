#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use ModerateChecks;
use Yandex::MyGoodWords;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use utf8;
use open ':std' => ':utf8';

use Data::Dumper;
$Data::Dumper::Sortkeys = 1;

my %db = (
    ppc_properties => {
        original_db => PPCDICT
    },
);
init_test_dataset(\%db);

*cma = sub { return (check_moderate_phrases(@_))[0] };

my $bo = get_banner();


#nothing changed
is(cma($bo,$bo), 0, 'nothing changed');

#phrase change semantic
is(cma(get_banner(change_phrase_semantic=>1), $bo), 1, "change phrase semantic");

#phrase change plus words
is(cma(get_banner(change_phrase_plus_words=>1), $bo), 1, "change phrase plus words");

#phrase change delete word
is(cma(get_banner(change_phrase_delete_word=>1), $bo), 1, "change phrase delete word");

#change_phrase_add_minus_word
is(cma(get_banner(change_phrase_add_minus_word=>1), $bo), 1, "change_phrase_add_minus_word");

#change_phrase_del_minus_word
is(cma(get_banner(change_phrase_del_minus_word=>1), $bo), 1, "change_phrase_del_minus_word");

#add_spec_symbols
is(cma(get_banner(add_spec_symbols=>1), $bo), 1, "add_spec_symbols");

#remove_spec_symbols
is(cma(get_banner(remove_spec_symbols=>1), $bo), 1, "remove_spec_symbols");

# добавили "!" перед фразой - не отправляем на модерацию
is(cma(get_banner(add_stop_phrase => 1), $bo), 1, "add_stop_phrase");

# убрали "!" перед фразой - отправляем на модерацию
is(cma(get_banner(remove_stop_phrase => 1), $bo), 1, "remove_stop_phrase");

sub get_banner
{
    my %OPT = @_;

    my %BASE_BANNER = (
        'Phrases' => [
                    {
                    'id' => '50338818',
                    'phrase' => "Кондиционер купить",
                    'norm_phrase' => Yandex::MyGoodWords::norm_words("Кондиционер купить"),
                    },
                    {
                    'id' => '50338819',
                    'phrase' => "Холодильник продать -бесплатно",
                    'norm_phrase' => Yandex::MyGoodWords::norm_words("Холодильник продать -бесплатно"),
                    },
                    {
                    'id' => '50338820',
                    'phrase' => "Холодные кондиционеры",
                    'norm_phrase' => Yandex::MyGoodWords::norm_words("Холодные кондиционеры"),
                    },
                    {
                    'id' => '50338822',
                    'phrase' => "!Кондиционер",
                    'norm_phrase' => Yandex::MyGoodWords::norm_words("!Кондиционер"),
                    },
                    ],
        'geo' => '159',
        'partly_declined_phrases' => '0',
        'pstatusModerate' => 'Yes',
        'statusModerate' => 'Yes',
    );

    my %b = %BASE_BANNER;
    

    if ($OPT{change_phrase_semantic}) {
        $b{Phrases}[0]{phr} = 'Кондиционер КУПИТЬ';
        $b{Phrases}[0]{phrase} = 'Кондиционер КУПИТЬ';
    }

    if ($OPT{change_phrase_plus_words}) {
        $b{Phrases}[0]{phr} = 'Кондиционер КУПИТЬ продать';
        $b{Phrases}[0]{phrase} = 'Кондиционер КУПИТЬ продать';
    }

    if ($OPT{change_phrase_delete_word}) {
        $b{Phrases}[0]{phr} = 'Кондиционер продать';
        $b{Phrases}[0]{phrase} = 'Кондиционер продать';
    }

    if ($OPT{change_phrase_del_minus_word}) {
        $b{Phrases}[1]{phr} = 'Холодильник продать';
        $b{Phrases}[1]{phrase} = 'Холодильник продать';
    }

    if ($OPT{change_phrase_add_minus_word}) {
        $b{Phrases}[1]{phr} = 'Холодильник продать -бесплатно -задаром';
        $b{Phrases}[1]{phrase} = 'Холодильник продать -бесплатно -задаром';
    }

    if ($OPT{add_spec_symbols}) {
        $b{Phrases}[0]{phr} = 'Кондиционер +купить';
        $b{Phrases}[0]{phrase} = 'Кондиционер +купить';
    }

    if ($OPT{remove_spec_symbols}) {
        $b{Phrases}[3]{phr} = 'Холодные кондиционеры';
        $b{Phrases}[3]{phrase} = 'Холодные кондиционеры';
    }

    if ($OPT{add_stop_phrase}) {
        $b{Phrases}[0]{phr} = '!Кондиционер купить';
        $b{Phrases}[0]{phrase} = '!Кондиционер купить';
    }

    if ($OPT{remove_stop_phrase}) {
        $b{Phrases}[4]{phr} = 'Кондиционер';
        $b{Phrases}[4]{phrase} = 'Кондиционер';
    }

    return \%b;
}

done_testing();
