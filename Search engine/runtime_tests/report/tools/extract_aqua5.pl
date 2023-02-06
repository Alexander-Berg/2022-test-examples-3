#!/usr/bin/env perl

use strict;
use warnings;
use utf8;

use Encode;
use List::MoreUtils qw/ uniq /;
use File::Basename qw/ basename /;
use Data::Dumper;
use JSON;

open my $fh, 'twr6265_5.txt' or die $!;
open my $weird, '>weird.txt' or die $!;

system "which wget" and die "wget not found";
system "which json_xs" and die "json_xs not found";
system "install -d html" and die $!;
system "install -d json" and die $!;
system "install -d json_other" and die $!;
system "install -d json_weird" and die $!;

my %file;
my %names;
my %stat;
my @duplicate;

use open ':std' => 'utf8';

while (<$fh>) {
    $stat{all}++;

    $_ = Encode::decode_utf8($_);
    my ($name, $url) = map { s/^\s+|\s+$//g; s/\s+/ /g; $_ } split /\s*\|\s*/;

    $stat{aqua_passed}++;

    my $json = join(' - ', map { s/^\s+|\s+$//g; s/\s+/ /g; $_ } split '[:/]', $name);
    $json =~ s/[\(\)\'\"\+\?\r\n]+//g;

    my $dst = 'json';
    $json = "$dst/$json";

    my $prev_json;

    if (exists $file{$json}) {
        #die "Duplicated: $name";

        push @duplicate, $name;
        $stat{duplicate}++;

        #warn("$name\nDuplicated file $json for cases: \n" . join("\n", $name, $file{$json}));
        $json = "$json." . ($file{$json}+1);
        if (-e "$json.json") {
            $prev_json = "$json.1.json";
            system "mv", "$json.json", $prev_json;
        }
    }

    $file{$json}++;
    push @{ $names{$json} }, $name;

    $json = "$json.json";

    my $file = 'html/' . basename($url);
    unless (-s $file) {
        sleep 1;
        if (system "wget $url -O $file") {
            print $weird join(' | ', $name, 'ERR_FETCH_HTML', $url) . "\n";
            $stat{err_fetch}++;
            next;
        }
    }

    if (system "grep -q 'BEGIN REPORT JSON' $file") {
        print $weird join(' | ', $name, 'NO_JSON_MARKER', $url) . "\n";
        $stat{err_no_json_marker}++;
        next;
    }

    die $json unless $json =~ /\.json$/;

    my $cmd = qq{grep -A 1 'BEGIN REPORT JSON' $file | tail -n 1 | json_xs > '$json'};
    #print "$cmd\n";
    qx{$cmd};

    my $size = (stat $json)[7];
    #print "$name\n$size\n";

    if (!$size || $size < 100_000) {
        $stat{ $size ? 'err_small' : 'err_no_json' }++;

        print $weird join(' | ', $name, ($size ? 'FILE_TOO_SMALL' : 'NO_JSON_CONTENT'), $url) . "\n";

        if ($size) {
            #system "mv", $json, 'json_weird/' and die $!;
        }
        else {
            warn "$name\nCan't parse json in $file";
            unlink $json;
        }
    }
    else {
        $stat{ok}++;
    }
}

$Data::Dumper::Sortkeys = 1;
#delete $names{$_} for grep @{$names{$_}}>1, keys %names;
#warn Dumper \%names;
warn Dumper \%stat;
