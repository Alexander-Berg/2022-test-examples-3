#!/usr/bin/perl
#2014-06-26 16:36:50,021 [13595] 53a8318e9bfd6422eb0f5ef4 client POST "http://index.disk.yandex.net/?id=617b326ad557b8408995f877335f543028a56c67086bf9ec4803a4e0ee39f4df&{"action":"modify","docs":[{"mimetype":{"value":"text/plain"},"name":{"value":"ivi для детей"},"body_text":"not_logged","timestamp":{"value":1403531662},"stid":{"value":"16845.yadisk:256916691.239260273198978218556192574953"},"mediatype":{"value":3},"parsed":{"value":true},"visible":{"value":1},"version":{"value":1403531662659584},"meta":{"value":"Content-Type:text/plain; charset=ISO-8859-1"},"key":{"value":"/disk/ivi для детей"},"mtime":{"value":1403531662},"built_date":{"value":"2014-06-24 15:07:19"},"size":{"value":58},"type":{"value":"file"},"id":{"value":"617b326ad557b8408995f877335f543028a56c67086bf9ec4803a4e0ee39f4df"},"md5":{"value":"089b932c6b447b3d12af1e375d63d0d2"}}],"prefix":256916691}" 200 780 0 0.012

while(my $line = <STDIN>) {
    chomp $line;
        if($line =~ /^(\d+);(\d+);(\d+);(.*)\,(\d+)\ \[\d+\]\ .*\?id\=([a-zA-Z0-9]+)\&\{\"action\"\:\"([a-z]+)\".*\[\{\"mimetype\"\:\{\"value\"\:\"([a-zA-Z\/]+)\".*\"mtime\"\:\{\"value\"\:([0-9]+)\}.*\}\"\ (\d+)\ \d+\ \d+\ ([0-9\.]+)$/) {
                print "$1;$2;$3;tskv\ttskv_format=mpfs-search-log\ttimestamp=$4\tms=$5\ttimezone=+0400\tid=$6\taction=$7\tmimetype=$8\tmtime=$9\tstatus=$10\tresponse_time=$11\n";
                }
}
