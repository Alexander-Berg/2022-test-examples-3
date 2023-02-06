#!/usr/bin/perl

use HTML::Entities;

while(my $line = <STDIN>) {
    chomp $line;
        #if($line =~ /^(\d+);(\d+);(\d+);(.*)\ \[(\d+)\]\ ([a-zA-Z0-9_-]+)?\ ([0-9\_]+)\ [a-z\_]+\ ([A-Z]+)\ (.*)\ ([A-Z0-9\/\.]+)\ (\d+)\ ([0-9\.]+).*/) {
        if($line =~ /^(\d+);(\d+);(\d+);([0-9\/\ \:]+)\ \[([a-zA-Z]+)\]\ ([0-9\#]+)\:\ \*([0-9]+)\ (.*)$/) {
		$a1 = $1 ; $a2 = $2 ; $a3=$3;
		$date = $4;
		$level = $5;
		$pid = $6;
		$request_id = $7;
		$error_string = $8;
		$date =~ tr/\//\-/;
		$error_string =~ s/"/\\"/g;
                print "$a1;$a2;$a3;tskv\ttskv_format=ydisk-error-log-nginx-mpfs\ttimestamp=$date\ttimezone=+0300\tlevel=$level\tpid=$pid\trequest_id=$request_id\terror_string=$error_string\n";
                }
}
