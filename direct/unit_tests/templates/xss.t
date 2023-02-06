#!/usr/bin/perl

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;

use Settings;

use utf8;
use open ':std' => ':utf8';

my $TMPL_ROOTS = $Settings::TT_INCLUDE_PATH;

my @files = grep {-f && /\.(html|tt2)$/} Test::ListFiles->list_repository($TMPL_ROOTS);

Test::More::plan(tests => scalar(@files));

for my $file (@files) {
    my @lines = read_file($file);
    my @errors;
    for my $i (0..$#lines) {
        for my $code ($lines[$i] =~ /\[%\s*(.*?)\s*%\]/g) {
            if (!check_code($code)) {
                $code =~ s/[^\x00-\x7F]/?/g;
                push @errors, "line ".($i+1).": $code";
            }
        }
        
        # ищем строки вида href='[% xxx | html 
        if ($lines[$i] =~ /( ' \[% [^]]* \| \s* html)/x) {
            my $code = $1;
            # не смотрим на | js | html
            if ($code !~ /\|\s*js\s*\|\s*html/) {
                push @errors, "line ".($i+1).": $code";
            }
        }
    }
    ok(!@errors, "checking $file".(@errors ? ": ".join(', ', @errors) : ''));
}

sub check_code {
    my $code = shift;
    if ($code =~ /^#/) {
        return 1;
    }
    if ($code =~ /\|\s*(html|js|url)$/) {
        return 1;
    }
    if ($code =~ /^(UNLESS|IF)\s/) {
        return 1;
    }
    if ($code =~ /=/) {
        return 1;
    }
    my $item_re = qr/(FORM\.\w+|FORM\.item\(\['"]\w+['"]\))/;
    if (grep {/^$item_re$/} split /\s*\|\|\s*/, $code) {
        return 0;
    }
    return 1;
}
