#!/usr/bin/perl


while(my $line = <STDIN>) {
    chomp $line;
        if($line =~ /^(\d+);(\d+);(\d+);\[(.*)\ \+.*\]\ ([a-z\.]+)\ ([0-9a-z\.\:]+)\ \"(.*)\ (.*)\ (.*)\"\ ([0-9]+)\ .*\]\ ([0-9]+)\ ([0-9]+)\ ([0-9\.]+).*/) {
                print "$1;$2;$3;tskv\ttskv_format=mpfs-access-log\ttimestamp=$4\thost=$5\tip=$6\tmethod=$7\trequest=$8\tprotocol=$9\tstatus=$10\trequest_length=$11\tbytes_sent=$12\trequest_time=$13\n";
                }
}
