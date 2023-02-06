#!/usr/bin/perl

use strict;
use warnings;

our $hostname=`hostname -f`;
chomp $hostname;

my $successauth = 0;
my $failedauth = 0;
my $errorauth = 0;
my $requests = 0;
my $unknown = 0;

my $logid = 'authorizer.default';

if (scalar @ARGV > 1) {
    print "USAGE: $0 [LOGID]\n";
    exit 1;
} elsif (scalar @ARGV == 1) {
    $logid = shift @ARGV;
}


open (LOGFILE, "/usr/bin/mymtail.sh /var/log/fastcgi-blackbox-authorizer.log $logid |");
while (my $line = <LOGFILE>) {
    if($line =~ m/status=402/) {
        $successauth++;
    }
    elsif($line =~ m/status=403 cache=no/) {
        $failedauth++;
    }
    elsif($line =~ m/status=500/){
	$errorauth++;        
    }
    elsif($line =~ m/blackbox request/) {
        $requests++;
    }
#    else {
#        $unknown++;
#    }
}

close LOGFILE;

print ("authorizer_sucess $successauth\n");
print ("authorizer_failed $failedauth\n");
print ("authorizer_error $errorauth\n");
print ("authorizer_requests $requests\n");
#print ("authorizer_unknown $unknown\n");


