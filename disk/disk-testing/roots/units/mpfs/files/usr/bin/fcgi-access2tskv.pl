#!/usr/bin/perl


while(my $line = <STDIN>) {
    chomp $line;
        if($line =~ /^(\d+);(\d+);(\d+);(.*)\ \[(\d+)\]\ ([a-zA-Z0-9_-]+)?\ ([0-9\_]+)\ [a-z\_]+\ ([A-Z]+)\ (.*)\ ([A-Z0-9\/\.]+)\ (\d+)\ ([0-9\.]+).*/) {
                print "$1;$2;$3;tskv\ttskv_format=mpfs-fcgi-access-log\ttimestamp=$4\tpid=$5\trequest_id=$6\tid=$7\tmethod=$8\trequest=$9\tprotocol=$10\tstatus=$11\trequest_time=$12\n";
                }
}
