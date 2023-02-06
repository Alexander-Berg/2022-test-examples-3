#!/usr/bin/perl

my $app="yadrop-mpfs";

my $yadroppushererror=0;

open (MAILLOG, "/usr/bin/mymtail.sh /var/log/yadrop/mpfs.log $app |");
while ($line=<MAILLOG>)
{
	if($line =~ m/url=http:\/\/pusher.yandex.net.* code=(\d+)/) {
        if($1 != 200) {
            $yadroppushererror++;
        }
	}
}
close MAILLOG;

print("yadrop.pusher.error $yadroppushererror\n");
