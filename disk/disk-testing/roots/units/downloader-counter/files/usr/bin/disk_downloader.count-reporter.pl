#!/usr/bin/perl -w

use strict;
use warnings;

use lib '/etc/nginx';
use YandexDisk::DownloadsCounter;

use LWP::UserAgent;
use POSIX qw/strftime/;
use Time::HiRes qw/usleep/;

sleep (int(rand(21)) + 10);

my $app = "disk_downloader.count-reporter";
my $url = 'http://mpfs.disk.yandex.net/service/kladun_download_counter_inc?hash=%s&bytes=%s&count=%s';

my $ua = LWP::UserAgent->new;
$ua->timeout(2);
$ua->agent($app);

my $access_log_tail = "/usr/bin/mymtail.sh /var/log/nginx/downloader/access.log $app |";
my $parsed_file = YandexDisk::parseFile($access_log_tail);

open COUNTER_LOG, '>>/var/log/downloads_counter.log';
while (my ($hash, $hash_info) = each %$parsed_file) {
    my ($sent, $req_time, $count) = @$hash_info;

    my $cur_url = sprintf("$url", $hash, $sent, $count);
    my $req = $ua->get($cur_url);

    my $msg = sprintf("url=%s return_code=%s time_delta=%s", $cur_url, $req->code, (time() - $req_time));
    my $timestamp = strftime "%m/%d/%Y %H:%M:%S", localtime;
    print COUNTER_LOG "$timestamp $msg\n";

    usleep(10000);
}
close COUNTER_LOG;
