#!/usr/bin/perl

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

my $ROOT = "$Settings::ROOT/protected";

my %SKIP = map {$_ => 1} qw/
    BS::ExportWorker::LogBrokerBuffer
    BS::ExportWorker::LogBrokerBufferPipeEach
    BS::ExportWorker::LogBrokerBufferPipeWhole
    Campaign::RelatedKeywordsBudgets
    DevTools
    Direct::Test::DBObjects
    JavaIntapi::MigrateMobileApps
    JavaIntapi::SendMetricsToSolomon
    PPCLoginBox
    ServiceSOAP
    Stat::CustomizedArray::OrderTarget
    Template::Plugin::Sitelinks
/;

my @perl_files = 
    map {+{name => $_, rel_name => s/^\Q$Settings::ROOT\E\/*//r, mod => mod_name($_)}}
    grep {-f && /\.p[ml]$/ && !/unit_tests|one-shot/} 
    Test::ListFiles->list_repository($Settings::ROOT);

sub mod_name {
    my $file = shift;
    return undef unless $file =~ s/\.pm$//;
    $file =~ s/^\Q$Settings::ROOT\E\/*(protected|perl\/\w+|api\/lib|api\/services\/v5)\/+// || return undef;
    $file =~ s/\//::/g;
    return $file;
}

my %USED;
for my $file (@perl_files) {
    my $cont = scalar read_file $file->{name};
    $cont =~ s/\n=\w+.*?\n=cut//gs;
    $cont =~ s/^\s*#.*//gm;
    $cont =~ s/\n__(END|DATA)__\s*\n.*//s;
    for my $dep ($cont =~ /(?:;|^|\n)\s*(?:use|require|extends)\s+['"]?([a-z0-9_:]+)/gi) {
        $USED{$dep}++;
    }
    for my $dep ($cont =~ /(?:;|^|\n)\s*use\s+base\s+qw\/([a-z0-9_:]+)/gi) {
        $USED{$dep}++;
    }
}

for my $mod (map {$_->{mod}} grep {$_->{mod}} @perl_files) {
    unless (
        $mod =~ /^Intapi::/
        || $mod =~ /^Export::/
        || $mod =~ /^Sandbox::/
        || $mod =~ /^Direct::YT::/
        || $mod =~ /^Settings/
        || $mod =~ /^PSGIApp::/
        || $mod =~ /^Plack::/
        || $mod =~ /^Model::/
        || $mod =~ /^Direct::Model::/
        || $mod =~ /^Direct::Role::/
        || $mod =~ /^Test::/
        || $mod =~ /^API::/
        || $mod =~ /^Units::/        
        || $mod =~ /^Apache::/        
        || $mod =~ /^Yandex::/        
        || $SKIP{$mod}
    ) {
        ok $USED{$mod}, "Module $mod should be used";
    }
}

done_testing;
