#!/usr/bin/perl
#2014-05-15 15:16:56,058 [18881] 18881_13340 mailer email delivered to arman-karman@yandex.ru, template welcome
#2014-05-15 15:18:35,434 [28435] 28435_10758 mailer email delivered to kimka-1@yandex.ua, template welcome

while(my $line = <STDIN>) {
    chomp $line;
        if($line =~ /^(\d+);(\d+);(\d+);(.*)\ \[\d+\]\ (?:[a-zA-z0-9_-]+)? ([0-9\_]+)\ mailer\ email\ delivered\ to\ (.*)\,\ template\ ([a-z0-9A-Z\/]+).*/) {
                print "$1;$2;$3;tskv\ttskv_format=mpfs-fcgi-log\ttimestamp=$4\ttimezone=+0400\tid=$5\tuser=$6\ttemplate=$7\n";
                }
}
