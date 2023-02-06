#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More tests => 29;
use Test::Deep;


use CheckAdv;
use BannerFlags;

sub f {
    my $flags_str = shift;
    my %opt = @_;
    $opt{ClientID} = 0 unless defined $opt{ClientID};

    return CheckAdv::check_banner_for_regions($flags_str, %opt);
}

my $kz = 159;
my $by = 149;
my $russia = 225;
my $moscow_and_reg = 1;
my $SNG = 166;
my $abkhasia = 29386;
my $brest = 29632;
my $astana = 163;
my $uk = 102;

cmp_deeply(f("", geo => $by), {}, "Show in kz - 0");
cmp_deeply(f("", geo => 1), {}, "Show in moscow");

# KZ
cmp_deeply(f("alcohol", geo => $kz), {}, "Accepted in KZ 1");
cmp_deeply(f("alcohol", geo => $astana), {}, "Accepted in Astana");

cmp_deeply(f("alcohol", geo => "$astana,$brest"), {all => {kz => {alcohol => 1}, by => {alcohol => 1}}}, "Declined in Astana and Brest");

cmp_deeply(f("fake", geo => $kz), {}, "Show in KZ");
cmp_deeply(f("fake", geo => "$kz,$moscow_and_reg"), {}, "Show in KZ 2");

# except KZ vs BY
cmp_deeply(f("alcohol", geo => 166), {kz => {alcohol => 1}, by => {alcohol => 1}}, "SNG excepted KZ, BY");
cmp_deeply(f("alcohol", geo => "$by,$kz"), {all=>{kz => {alcohol => 1}, by => {alcohol => 1}}}, "KZ, BY excepted KZ, BY");

# BY
cmp_deeply(f("alcohol", geo => $by), {all=>{by => {alcohol => 1}}}, "Excepted BY");

cmp_deeply(f("fake", geo => $by), {}, "Show in BY");
cmp_deeply(f("fake", geo => "$by,$moscow_and_reg"), {}, "Show in BY 2");

# RU
cmp_deeply(f("alcohol", geo => $russia), {all => {ru => {alcohol => 1}}}, "Excepted RU");
cmp_deeply(f("alcohol", geo => "$abkhasia,$moscow_and_reg"), {ru => {alcohol => 1}}, "Excepted RU 2");


# combined flags
cmp_deeply(f("test,alcohol", geo => $kz), {}, "Accepted in KZ 2");
cmp_deeply(f("test,alcohol", geo => $by), {all=>{by => {alcohol => 1}}}, "Excepted BY");

cmp_deeply(f("test,alcohol", geo => 0), {kz => {alcohol => 1}, by => {alcohol => 1}, ru => {alcohol => 1}}, "World excepted KZ, BY, RU");

# 
cmp_deeply(f("alcohol", geo => $by), {all=>{by => {alcohol => 1}}}, "Excepted BY");

#
cmp_deeply(f("alcohol", geo => "$brest"), {all => {by => {alcohol => 1}}}, "part of BY excepted BY");
cmp_deeply(f("alcohol", geo => "$abkhasia,$brest"), {by => {alcohol => 1}}, "part of BY + another region excepted BY");
cmp_deeply(f("alcohol", geo => "$abkhasia,$by"), {by => {alcohol => 1}}, "BY + another region excepted BY");
cmp_deeply(f("alcohol", geo => "$SNG,-$by,-$kz"), {}, "Targeting: SNG -by -kz");
cmp_deeply(f("alcohol", geo => "$SNG,-$by,-$kz,$brest"), {by => {alcohol => 1}}, "Targeting: SNG -by -kz +brest");

#
cmp_deeply(f("medicine,med_services", geo => $by), {}, "Show medicine in BY");
cmp_deeply(f("medicine,med_equipment", geo => $by), {all=>{by => {med_equipment => 1}}}, "Excepted med_equipment BY");
cmp_deeply(f("med_equipment", geo => $by), {all=>{by => {med_equipment => 1}}}, "Excepted med_equipment BY 2");

{
    local %CheckAdv::BANLIST = (
        kz => [
            'kz_and_by',
            'kz_only'
        ],
        by => [
            'kz_and_by',
            'by_only'
        ],
    );

    local %BannerFlags::AD_WARNINGS = (
        'kz_and_by' => {long_text => "",
                       short_text => ""},
        'kz_only' => {long_text => "",
                       short_text => ""},
        'by_only' =>  {long_text => "",
                       short_text => ""},
        'tobacco' =>  {long_text => "",
                       short_text => ""},
        'age'    =>   {variants => [qw/18 16 12 6 0/], is_common_warn => 1},
    );

    cmp_deeply(f("kz_and_by,by_only", geo => "$SNG"), { kz => {kz_and_by => 1}, by => {kz_and_by => 1, by_only => 1}}, "SNG excepted by, kz");
    cmp_deeply(f("kz_and_by,by_only", geo => "$kz,$by"), { all => {kz => {kz_and_by => 1}, by => {kz_and_by => 1, by_only => 1}}}, "kz, by excepted kz, by");
    cmp_deeply(f("by_only", geo => "$kz,$by"), { by => {by_only => 1}}, "flag 'by_only'");
}
