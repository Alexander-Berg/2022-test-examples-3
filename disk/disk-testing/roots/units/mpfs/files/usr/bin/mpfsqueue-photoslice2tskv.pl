#!/usr/bin/perl

=item example
:
2013-12-02 12:30:16,500 [30983] 529c4517dd3cb32b42dd040e space 224751850 enlarged space 8589934592 => 10737418240 (reason: promo_shared, added 2147483648)
2013-12-02 12:13:34,062 [30985] 529c412ddd3cb36660dd047d mailer email delivered to khamidullin90renat@yandex.ru, template sharedFolder/accepted

=cut

while (my $line = <STDIN> ) {
  chomp $line;
  if ($line =~ m/(\d+);(\d+);(\d+);(.*)\ \[(\d+)\]\ ([a-zA-z0-9_-]+)?\ ([0-9a-zA-Z\_-]+)?\ ?(.*)\ ([0-9\.]+)$/){
    print "$1;$2;$3;tskv\ttskv_format=mpfs-queue-photoslice-log\ttimestamp=$4\tpid=$5\trequest_id=$6\tid=$7\tjob=$8\tjobtime=$9\n";
  } #elsif ($line =~ m/(\d+);(\d+);(\d+);(.*)\ \[(\d+)\]\ ([a-zA-z0-9_-]+)?\ ([0-9a-zA-Z\_-]+)?\ ?(.*)\ ([0-9\.]+)?$/) {
    #print "$1;$2;$3;tskv\ttskv_format=mpfs-queue-log\ttimestamp=$4\tms=$5\tmailto=$6\ttemplate=$7\n";
    #}
}
