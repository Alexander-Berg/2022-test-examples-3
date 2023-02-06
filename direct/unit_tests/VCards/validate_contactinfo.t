#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More;

use Yandex::HashUtils;
use VCards;

use utf8;
use open ':std' => ':utf8';
use Yandex::Test::UTF8Builder;

# ошибки через запятую
*vct = sub {join ',', validate_contactinfo(@_) };
# количество ошибок
*vcn = sub {scalar validate_contactinfo(@_) };

my %ci_ok = (
    country_code  => "+7", 
    city_code     => "812",
    phone         => "2128506",
    ext           => "340", 
    name          => "company", 
    contactperson => "mr. Smith",
    worktime      => "1#3#10#15#18#30;4#6#10#30#20#30",
    country       => "Russia", 
    city          => "Moscow",
    extra_message => "I know it's hard to believe, but I haven't been warm for a week..."
);

cmp_ok(vcn(\%ci_ok), '==',   0, 'correct contactinfo, number of errors');

cmp_ok(vct(\%ci_ok), 'eq', '', 'correct contactinfo, text of errors');

cmp_ok(vcn(hash_merge {}, \%ci_ok, {city_code => 0}), '==', 1, 'incorrect contactinfo, city_code=0');

for my $f (qw/country_code city_code phone name worktime country city/){
    cmp_ok( vcn(hash_merge {}, \%ci_ok, { $f => '' }), '>',   0, "contactinfo without $f" );
}

cmp_ok( vcn(hash_merge {}, \%ci_ok, {country_code  => "8", city_code => "800", phone => "2128506", ext => "340"}), '==', 0, "country_code: 8 city_code: 800" );
cmp_ok( vcn(hash_merge {}, \%ci_ok, {country_code  => "+8", city_code => "800", phone => "2128506", ext => "340"}), '==', 1, "country_code: 8 city_code: 800" );

for my $f (qw/name contactperson street house build apart extra_message/) {
    # validate_contactinfo делает smartstrip на все поля, убирая переводы строк
    is(vct(hash_merge {}, \%ci_ok, { $f => "test\n" }), '', "перевод строки в конце $f");
}

# ограничения на максимально допустимую длину данных в полях
my %size_restrictions = (
    name => 255,
    street => 55,
    house => 30,
    build => 10,
    apart => 100,
    contactperson => 155,
    city => 55,
    country => 50,
    extra_message => 200,
);
while ( my($field_name, $max_allowed_length) = each %size_restrictions ) {
    cmp_ok( vcn( hash_merge {}, \%ci_ok, {$field_name => '9' x $max_allowed_length} ), '==', 0, "максимально допустимое по длине значение в поле $field_name" );
    cmp_ok( vcn( hash_merge {}, \%ci_ok, {$field_name => '9' x ($max_allowed_length+1)} ), '>', 0, "превышено максимально допустимое по длине значение в поле $field_name" );
}

cmp_ok( vcn(hash_merge {}, \%ci_ok, { worktime => "1#3#10#15#18#30;4#6#10#30#20#25" }), '==',   1, "Incorrect worktime, minutes not divisable by 15" );
cmp_ok( vcn(hash_merge {}, \%ci_ok, {phone => "0"}), '==', 1, "Incorrect phone number" );

done_testing;
