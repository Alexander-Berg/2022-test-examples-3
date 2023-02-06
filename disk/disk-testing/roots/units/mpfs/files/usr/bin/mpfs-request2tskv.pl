#!/usr/bin/perl


while(my $line = <STDIN>) {
    chomp $line;
        if($line =~ /^(\d+);(\d+);(\d+);(.*)\ \[(\d+)\]\ ([a-zA-z0-9_-]+)?\ ([0-9a-zA-Z\_-]+)?\ ?(.*)\ ([0-9\.]+)$/) {
                print "$1;$2;$3;tskv\ttskv_format=mpfs-request-log\ttimestamp=$4\tpid=$5\trequest_id=$6\tid=$7\trequest=$8\trequest_time=$9\n";
                }
}
