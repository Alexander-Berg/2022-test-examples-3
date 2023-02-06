#! /usr/bin/perl
use ExtUtils::testlib;

use blackbox2;
use Data::Dumper;

for ($count=1; $count<2; $count++) {

my $options = blackbox2::Options->new();
$options->add("a", "b");

my $opt = blackbox2::Option->new("custom_opt","yes");
$options->add_opt($opt);

$options->add_opt( blackbox2::Option->Regname() );
$options->add_opt( blackbox2::Option->GetYandexEmails() );
$options->add_opt( blackbox2::Option->GetSocialAliases() );

my $testmail = blackbox2::OptTestEmail->new("ya\@ya.ru");
$testmail->format($options);

my $result = blackbox2::InfoRequest_Uid("1212", "127.0.0.1", $options);

my $fields = blackbox2::DBFields->new();
$fields->add("asdf");
$fields->add("qwert");

$options->add_fields($fields);

my $oo= $fields->getOptions();
$oo->add("test1","test2");

my $result2 = blackbox2::LoginRequest(blackbox2::LoginSid->new("vasya", "2"), "qwerty123", "fotki", "127.0.0.1", $options);

my $result3 = blackbox2::SessionIDRequest("1234.1234.1234.1234", "yandex.ru", "127.0.0.1", $oo);

my $result4 = blackbox2::InfoRequest_Bulk(["123","456","789"], "8.8.8.8", $options);

print $result, "\n";
print Dumper($result2), "\n";
print $result3, "\n";
print $result4, "\n";

my $req_body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>
    <doc>
    <status id=\"0\">VALID</status>
    <error>OK</error>
    <uid hosted=\"0\" domid=\"\" domain=\"\">20903734</uid>
    <karma confirmed=\"0\">75</karma>
    <aliases>
      <alias type=\"4\">вася\@xn--h1adkfax.xn--p1ai</alias>
      <alias type=\"5\">pdd-login</alias>
      <alias type=\"6\">uid-5djew2ti</alias>
    </aliases>
    </doc>";

my $log_body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>
    <doc>
    <status id=\"0\">VALID</status>
    <error>some message</error>
    <uid hosted=\"1\" domid=\"51\" domain=\"xn--d1abbgf6aiiy.xn--p1ai\">11806301</uid>
    <karma confirmed=\"0\">0</karma>
    <regname>junit-test</regname>
    <dbfield id=\"account_info.fio.uid\">\@В &lt;html&gt; &amp;Р’Р°СЃСЏ</dbfield>
    <dbfield id=\"subscription.login.2\">junit-test2</dbfield>
    <address-list>
      <address validated=\"1\" default=\"0\" rpop=\"0\" native=\"1\" born-date=\"2010-10-04 00:00:00\">junit-test\@YA.ru</address>
      <address validated=\"1\" default=\"0\" rpop=\"0\" native=\"1\" born-date=\"2010-10-04 00:00:00\">junit-test\@narod.ru</address>
      <address validated=\"1\" default=\"1\" rpop=\"0\" native=\"1\" born-date=\"2010-10-04 00:00:00\">junit-test\@yandex.by</address>
      <address validated=\"0\" default=\"0\" rpop=\"0\" native=\"0\" born-date=\"2010-11-08 14:14:05\">$%\{\}\@yandex.ru</address>
    </address-list>
    </doc>";

my $bulk_body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>
    <doc>
    <error>OK</error>
    <user id=\"12345\">
        <uid hosted=\"0\" domid=\"\" domain=\"\">12345</uid>
        <karma confirmed=\"0\">75</karma>
    </user>
    <user id=\"54321\">
        <uid hosted=\"1\" domid=\"\" domain=\"\">54321</uid>
        <karma confirmed=\"0\">25</karma>
    </user>
    </doc>";

my $response = blackbox2::Response->new($req_body);
my $lresp = blackbox2::LoginResp->new($log_body);
my $bresp = blackbox2::BulkResponse->new($bulk_body);

my $karma_info = blackbox2::KarmaInfo->new($response);

$fields->readResponse($lresp);

my $iter = blackbox2::DBFieldsIterator->new($fields);
my $elem = $iter->getNext();
print "Fields:\n";
while ( $elem ) {
  print $elem->{key} , "=" , $elem->{val} , "\n";
  $elem = $iter->getNext();
};

print $response->message() , "\n";
print $lresp->message(), "\n";

print $karma_info->karma(), "\n";

print "Email list information:\n";

my $maillist = blackbox2::EmailList->new($lresp);

my $def = $maillist->getDefaultItem();
print "default addr=", $def->address(), ", date=", $def->date(), ", native=", $def->native(),"\n";

my $l = $maillist->getEmailItems();

foreach ( @$l ) {
  print "addr=", $_->address(), ", date=", $_->date(), ", native=", $_->native(),"\n";
}

print "Alias list:\n";

my $lst = blackbox2::AliasList->new($response);

my $l2 = $lst->getAliases();

foreach ( @$l2 ) {
  print "type =", $_->type(), ", alias=", $_->alias(), "\n"
}

print "Bulk response info: message=", $bresp->message() , " count = ", $bresp->count() , "\n";
for ($bl=0; $bl < $bresp->count(); $bl++) {
    print "bulk response #" , $bl , " id=" , $bresp->id($bl), "\n";
    my $u = blackbox2::Uid->new( $bresp->user($bl) );
    print "uid = ", $u->uid() , " hosted=", $u->hosted , "\n";
    my $k = blackbox2::KarmaInfo->new( $bresp->user($bl) );
    print "karma=" , $k->karma(), "\n";

}

}





