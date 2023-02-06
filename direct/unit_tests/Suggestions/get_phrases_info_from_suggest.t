#!/usr/bin/perl

# $Id$

# TODO EXP 11.02.2010
# -Избавиться от работы с настоящей базой, использовать DBUnitTest
# -Сделать мониторинг о том, что в phrases_suggest значения вменяемы: ctr'ы и цены верно отнормированы

use warnings;
use strict;
use Test::More tests => 15;

use lib::abs;

use Settings;
use Suggestions;

use utf8;
use open ':std' => ':utf8';

BEGIN {
    require "".lib::abs::path("_init.pl");
}


*gpifs = sub { get_phrases_info_from_suggest(@_) };


my $phrases;

# Проверяем, что на пустом массиве все корректно
$phrases = [];
gpifs($phrases);
cmp_ok( scalar(@$phrases), '==', 0, "empty phrases" ); 

# Проверяем, что для популярных фраз возвращаются ненулевые значения показов, а для неизвестных -- нулевые.
$phrases = [{phrase => "кредит"}, {phrase => "кредита"}, {phrase => "банк"}, {phrase => "dsafsafafsafsafdsafdsafsa"}];
gpifs($phrases);
cmp_ok( scalar(@$phrases), '!=', 0, "non-empty phrases" ); 
cmp_ok( $phrases->[0]->{shows}, '!=', 0, "shows for popular phrases should not be 0" ); 
cmp_ok( $phrases->[0]->{phrase_hash}, '==', $phrases->[1]->{phrase_hash}, "normal form of phrase should be used" ); 
cmp_ok( $phrases->[0]->{phrase_hash}, '!=', $phrases->[2]->{phrase_hash}, "normal form of phrase should be used" ); 
cmp_ok( $phrases->[3]->{shows}, '==', 0, "shows for unknown phrases should be 0" ); 
ok( exists $phrases->[3]->{notintable}, "flag for unknown phrases" ); 

# Проверяем, что для популярных фраз присутствуют все необходимые поля
$phrases = [{phrase => "ремонт"}];
gpifs($phrases);
ok(exists $phrases->[0]->{price});
ok(exists $phrases->[0]->{banners_cnt});
ok(exists $phrases->[0]->{users_cnt});
ok(exists $phrases->[0]->{orders_cnt});


# Проверяем, что функция корректно отрабатывает, если вместо фраз заданы их хеши
# Берем хеши фраз  "кредит", "банк" и "dsafsafafsafsafdsafdsafsa" 
$phrases = [{phrase_hash => "15015910599689900176"}, {phrase_hash => "155964915372422956"}, {phrase_hash => "8170236785243893294"} ];
gpifs($phrases);
cmp_ok( $phrases->[0]->{shows}, '!=', 0, "shows for hash of popular phrase should not be 0" ); 
cmp_ok( $phrases->[0]->{phrase_orig}, 'ne', '', "hash of popular phrase" ); 
cmp_ok( $phrases->[2]->{shows}, '==', 0, "shows for hash of unknown phrase should be 0" ); 
ok( exists $phrases->[2]->{notintable}, "flag for unknown phrases" ); 

=for none
# Проверяем, что ctr правильно отнормирован: не слишком маленький, не слишком большой
$phrases = [{phrase => "бест"}, {phrase => "компьютерный"}, {phrase => "сафари"}];
gpifs($phrases);
my $small_ctr_count = scalar grep {exists $_->{ctr} && $_->{ctr} < 0.2} @$phrases;
cmp_ok($small_ctr_count, '==', 0, "too small ctr looks suspicious");
# 
$phrases=[{phrase=>'люстры светильники'}, {phrase => 'тайланд горящие'}, {phrase => 'загранпаспорт срочно'}, 
          {phrase => 'печной кирпич'}, {phrase => 'усыпление животных'}];
gpifs($phrases);
$small_ctr_count = scalar grep {exists $_->{ctr} && $_->{ctr} < 2} @$phrases;
cmp_ok($small_ctr_count, '==', 0, "small ctr for good phrase looks suspicious");
my $big_ctr_count = scalar grep {exists $_->{ctr} && $_->{ctr} >50} @$phrases;
cmp_ok($big_ctr_count, '==', 0, "too big ctr looks suspicious");

# Проверяем, что цена правильно отнормирована: не слишком маленькая, не слишком большая
$phrases=[{phrase=>'дизельгенератор'}, {phrase => 'дизель генератор'}, {phrase => 'дизельные электростанции'}, 
          {phrase => 'дизельэлектростанции'}, {phrase => 'аренда башенных кранов'}];
gpifs($phrases);
my $big_price_count = scalar grep {exists $_->{price} && $_->{price} > 50} @$phrases;
cmp_ok($big_price_count, '==', 0, "too big prices look suspicious");
#
$phrases=[{phrase=>'ремонт квартир'}, {phrase => 'грузоперевозки'}, {phrase => 'пластиковые окна'}, 
          {phrase => 'юридические услуги'}, {phrase => 'купить квартиру'}];
gpifs($phrases);
my $small_price_count = scalar grep {exists $_->{price} && $_->{price} < 0.1} @$phrases;
cmp_ok($small_price_count, '==', 0, "small prices for popular phrases look suspicious");

=cut

