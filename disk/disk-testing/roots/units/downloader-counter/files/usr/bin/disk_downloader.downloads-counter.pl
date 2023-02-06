#!/usr/bin/perl

our $hostname=`hostname -f`;
chomp $hostname;

my ($non200, $delta, $unknown, $total) = (0, 0, 0, 1);

my $logid = 'downloads-counter.default';

if (scalar @ARGV > 1) {
    print "USAGE: $0 [LOGID]\n";
    exit 1;
} elsif (scalar @ARGV == 1) {
    $logid = shift @ARGV;
}

open (LOGFILE, "/usr/bin/mymtail.sh /var/log/downloads_counter.log $logid |");
while (my $line = <LOGFILE>) {
	if($line =~ m/return_code=([0-9]{3}) time_delta=([0-9]+)$/i) {
		$total++;
		my ($code, $time) = ($1, $2);

		if($code != 200) {
			$non200++;
		}

		$delta += $time;
	} else {
		$unknown++;
	}
}
close LOGFILE;


print ("downloads_counter_non200 $non200\n");
print ("downloads_counter_delta ", ($delta / $total), "\n");
print ("downloads_counter_unknown $unknown\n");
