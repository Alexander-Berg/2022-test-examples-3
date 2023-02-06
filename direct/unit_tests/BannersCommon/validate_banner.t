#!/usr/bin/perl

=head2 TODO

    Сейчас тест целенаправленно проверяет только валидацию заголовка и текста баннера.
    Надо добавить кейсов для проверки остальных полей и их сочетаний (ссылка+доп.ссылки, визитка+ссылки и т.п.)
    Для части проверок придётся класть фейковые данные в БД.

=cut

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Storable qw/dclone/;
use Yandex::HashUtils;

use BannersCommon;
use MinusWordsTools;

use utf8;

my $standard_banner = {
    title => 'заголовок баннера',
    body => 'тело баннера',
    banner_type => 'desktop',
    phrases => [
        'фраза1',
        'вторая фраза',
        'test',
    ],
    geo => '225,-213,-10174',
    href => 'http://ya.ru/',
    sitelinks => [
        {
            title => '1-я доп.ссылка',
            href => 'http://ya.ru/?sitelink1',
        },
        {
            title => '2-я доп.ссылка',
            href => 'http://ya.ru/?sitelink2',
        },
        {
            title => '3-я доп.ссылка',
            href => 'ya.ru/?sitelink3',
        },
    ],
    banner_minus_words => MinusWordsTools::minus_words_interface_show_format2array('-минус -слова на баннер'),
    banner_with_phone => 0,
    has_href => 1,
};

my $standard_options = {
    use_banner_with_flags => 1,
    ClientID => 0,
};

# Таблицы используются внутри validate_banner
copy_table(PPCDICT,'trusted_redirects');
copy_table(PPCDICT,'bad_domains');
copy_table(PPCDICT,'mirrors_correction');
copy_table(PPCDICT,'mirrors');
copy_table(PPCDICT,'ppc_properties');

=head2 @test_data

    Тест-кейсы представляют собой отличия от "стандартного" баннера $standard_banner и
    "стандартных" параметров $standard_options.

=cut

my @test_data = (
    {
        test_name => 'стандартный баннер',
    },
    {
        test_name => 'баннер с неразрывным пробелом и кавычками-ёлочками в заголовке и теле',
        banner_diff => {
            title => "фраза\x{00a0}«не\x{00a0}перенесётся»",
            body => "и\x{00a0}тело\x{00a0}«не\x{00a0}перенесётся»",
        },
    },
    {
        test_name => 'баннер с неразрывной строкой предельной длины в заголовке и теле',
        banner_diff => {
            title => 'ы' x $MAX_TITLE_UNINTERRUPTED_LENGTH,
            body => 'ы' x $MAX_BODY_UNINTERRUPTED_LENGTH,
        },
    },
    {
        test_name => 'баннер с неразрывной строкой больше предельной длины в заголовке',
        banner_diff => {
            title => 'ы' x ($MAX_TITLE_UNINTERRUPTED_LENGTH+1),
        },
        is_error_expected => 1,
    },
    {
        test_name => 'баннер с неразрывной строкой больше предельной длины в теле',
        banner_diff => {
            body => 'ы' x ($MAX_BODY_UNINTERRUPTED_LENGTH+1),
        },
        is_error_expected => 1,
    },
    {
        test_name => 'баннер с переводом строки в середине заголовка',
        banner_diff => {
            title => "заголовок\nбаннера",
        },
        is_error_expected => 0, # validate_banner делает smartstrip на загловок и убирает перевод строки
    },
    {
        test_name => 'баннер с переводом строки в конце заголовка',
        banner_diff => {
            title => "заголовок баннера\n",
        },
        is_error_expected => 0, # validate_banner делает smartstrip на загловок и убирает перевод строки
    },
    {
        test_name => 'баннер с переводом строки в середине текста',
        banner_diff => {
            body => "тело\nбаннера",
        },
        is_error_expected => 0, # validate_banner делает smartstrip на текст и убирает перевод строки
    },
    {
        test_name => 'баннер с переводом строки в конце текста',
        banner_diff => {
            body => "тело баннера\n",
        },
        is_error_expected => 0, # validate_banner делает smartstrip на текст и убирает перевод строки
    },
    {
        test_name => 'отсутвтующий протокол в ссылке баннера',
        banner_diff => {
            href => "ya.ru",
        },
        is_error_expected => 0,
    },
    {
    	test_name => "allowed mobile banner",
    	banner_diff => {
            banner_type => 'mobile'
        },
        is_error_expected => 0,
    },
    {
        test_name => "incorrect banner type",
        banner_diff => {
            banner_type => 'chobile'
        },
        is_error_expected => 1,
    }
);

for my $test_case(@test_data) {
    my $banner = dclone($standard_banner);
    hash_merge $banner, $test_case->{banner_diff} if $test_case->{banner_diff};

    my $options = dclone($standard_options);
    hash_merge $options, $test_case->{options_diff} if $test_case->{options_diff};

    my @validate_errors = BannersCommon::validate_banner($banner, $options);
    my $errors_text = join ', ', @validate_errors;
    if ($test_case->{is_error_expected}) {
        isnt($errors_text, '', $test_case->{test_name});
    } else {
        is($errors_text, '', $test_case->{test_name});
    }
}

$standard_options->{use_multierrors_format} = 1;

hash_merge \my %banner_1, $standard_banner, {banner_type => 'desktop', bid => 800};
hash_merge \my %options_1, $standard_options, {exists_banners_type => {800 => 'desktop'}};  
my ($errors_1) = BannersCommon::validate_banner(\%banner_1, \%options_1);
is(scalar keys %$errors_1, 0); 

hash_merge \my %banner_2, $standard_banner, {banner_type => 'desktop', bid => 800};
hash_merge \my %options_2, $standard_options, {};  
my ($errors_2) = BannersCommon::validate_banner(\%banner_2, \%options_2);
is(scalar @{$errors_2->{800}->{banner_type}}, 1);

hash_merge \my %options_3, $standard_options, {skip_changing_banner_type => 1};  
my ($errors_3) = BannersCommon::validate_banner(\%banner_2, \%options_3);
is(scalar keys %$errors_3, 0);

hash_merge \my %banner_4, $standard_banner, {banner_type => 'mobile', bid => 659, title => ";;{};@#*&"};
hash_merge \my %options_4, $standard_options, {exists_banners_type => {659 => 'desktop', 660 => 'mobile'}};  
my ($errors_4) = BannersCommon::validate_banner(\%banner_4, \%options_4);
is(scalar @{$errors_4->{659}->{banner_type}}, 1);
is(scalar @{$errors_4->{659}->{title}}, 1);


hash_merge \my %banner_5, $standard_banner, {banner_type => 'mobile', mbid => 431};
hash_merge \my %options_5, $standard_options, {is_mediaplan_banner => 1, exists_banners_type => {659 => 'desktop', 431 => 'desktop'}};  
my ($errors_5) = BannersCommon::validate_banner(\%banner_5, \%options_5);
is(scalar @{$errors_5->{431}->{banner_type}}, 1);

hash_merge \my %banner_6, $standard_banner, {banner_type => 'mobile', mbid => 431};
hash_merge \my %options_6, $standard_options, {is_mediaplan_banner => 1, exists_banners_type => {659 => 'mobile'}};  
my ($errors_6) = BannersCommon::validate_banner(\%banner_6, \%options_6);
is(scalar @{$errors_6->{431}->{banner_type}}, 1);

done_testing;

