#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use my_inc '../..';

use List::MoreUtils qw/uniq/;

use Yandex::DBTools;

use lib::abs '../../protected';

use Settings;
use Encode;
use PhraseText;

my @WORDS = (
    'кредит',
    'кредита',
    'банк',
    'dsafsafafsafsafdsafdsafsa',
    'ремонт ',
    'кондиционер',
    'сплит-система',
    'пенобетон',
    'ghjkhjhgjkhgkjhgkjhg',
    'автокредит',
    'грузовые перевозки',
    'абракадабра',
    'фикус',
    'грузоперевозки',
    'доставка пиццы',
    'доставка суши',
    'пицца на дом',
    'юридическая помощь',
);
my @HASHES = qw/
    15015910599689900176
    155964915372422956
    8170236785243893294
/;

my @all_hashes = (@HASHES, map {get_phrase_hash($_)} @WORDS,);
my $links = get_all_sql(PPCDICT, ["SELECT * FROM suggest_phrases_links", WHERE => {src_hash => \@all_hashes}, "ORDER BY dst_hash"]);

my $stat = {};
my %all_hashes = map {$_ => 1} @all_hashes;
# выбираем по 10 подсказок + кросс-линки
$links = [grep {$stat->{$_->{src_hash}}++ < 10 || $all_hashes{$_->{dst_hash}}} @$links];

my @phrases_hashes = uniq @all_hashes, map {($_->{src_hash}, $_->{dst_hash})} @$links;
my $phrases = get_all_sql(PPCDICT, ["SELECT * FROM suggest_phrases", WHERE => {phrase_hash => \@phrases_hashes}]);

my $data = {
    phrases => _format_data($phrases),
    links => _format_data($links),
};

my $file = lib::abs::path("./_data.json.gz");
open(my $fh, '|-', "gzip -9 >$file.tmp") || die $!;
print $fh Encode::encode_utf8(JSON::to_json($data)) or die $!;
close $fh or die $!;
rename("$file.tmp", $file) or die $!;


sub _format_data {
    my $d = shift;
    my @fields = keys %{$d->[0]};
    return +{
        fields => \@fields,
        data => [map {[@$_{@fields}]} @$d],
    };
}
