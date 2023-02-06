#!/usr/bin/perl 

#2013-10-24 08:20:20,740 [2554] 5268a003cb00cf09b8990c3f common U:133710406 A:buy_new P:10gb_1y SB:1 S:cancelled M:300 C:RUB SU:3138731082 SL:9126805504
#2013-10-24 10:48:28,642 [2553] 5268c2bbcb00cf10fb990b79 common U:230927933 A:buy_new P:10gb_1y SB:1 S:cancelled M:10 C:USD SU:9692988 SL:10737418240
#2013-10-24 11:16:17,428 [2553] 5268c93fcb00cf2241990efe common U:203143448 A:buy_new P:100gb_1m SB:1 S:cancelled M:150 C:RUB SU:47712084932 SL:118111600640
#2013-10-24 13:09:02,802 [2553] 5268e3adcb00cf50ec1523be common U:3313793 A:delete P:10gb_1m SB:0 S:None M:30 C:RUB SU:None SL:None
#2013-10-24 13:50:40,411 [2553] 5268ed6fcb00cf2209990856 common U:62598939 A:buy_new P:10gb_1y SB:0 S:cancelled M:300 C:RUB SU:2212254349 SL:42949672960
#2013-10-24 13:59:08,238 [14532] 14532_11613 common U:230315558 A:order_new P:10gb_1y SB:0 S:None M:300 C:RUB SU:None SL:None
#2013-10-24 14:34:17,805 [2553] 5268f7a8cb00cf631a990b48 common U:204739437 A:buy_new P:10gb_1m SB:0 S:cancelled M:1 C:USD SU:9125150831 SL:9126805504
#2013-10-24 14:44:54,350 [3059] 3059_10946 common U:14556466 A:order_new P:10gb_1m SB:0 S:None M:30 C:RUB SU:None SL:None
#2013-10-24 14:47:50,115 [4339] 4339_12917 common U:223403570 A:order_new P:1tb_1m SB:1 S:None M:900 C:RUB SU:None SL:None

$|=1;

while(my $line = <STDIN>) {
    chomp $line;
    if($line =~ /^(\d+);(\d+);(\d+);(.*)\,(\d+)\ .*\ ([a-z]+)\ U\:(\d+)\ A\:(.*)\ P\:([a-z0-9A-Z\_]+)\ SB\:(0|1)\ S\:([a-zA-Z]+)\ M\:(\d+)\ C\:([a-zA-Z]+)\ SU\:([a-zA-Z0-9]+)\ SL\:([a-zA-Z0-9]+).*/) {
        print "$1;$2;$3;tskv\ttskv_format=mpfs-billing-log\ttimestamp=$4\tms=$5\ttype=$6\tuid=$7\taction=$8\tproduct=$9\tsbflag=$10\tstatus=$11\tmoney=$12\tcurrency=$13\tSU=$14\tSL=$15\n";
    }
}
