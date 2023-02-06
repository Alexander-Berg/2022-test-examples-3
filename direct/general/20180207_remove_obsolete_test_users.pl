#!/usr/bin/perl

=head1 DEPLOY

# .migr
{
  tasks => [
    {
      type => 'script',
      when => 'after',
      time_estimate => "30 sec",
      comment => "перезапускать можно"
    }
  ],
  approved_by => 'lena-san'
}

=cut

use Direct::Modern;

use my_inc '..';

use ScriptHelper;
use Settings;

use TestUsers;

my $lena_san_superreader_uid = 84407126;
my $to_delete = [
    {login => "afanasjeff-super", uid => 238116680},
    {login => "alexteut-super", uid => 415657731},
    {login => "aluck-super", uid => 239102574},
    {login => "brostovskiy-media", uid => 218286438},
    {login => "brostovskiy-placer", uid => 218286037},
    {login => "brostovskiy-super", uid => 218285871},
    {login => "brostovskiy-supp", uid => 218286238},
    {login => "coffeeich-super", uid => 242248547},
    {login => "dmitriy-ch-super", uid => 528892449},
    {login => "dmmaklygin-super", uid => 200593132},
    {login => "dyadyajora-super", uid => 428305238},
    {login => "eemelin-super", uid => 237684049},
    {login => "eugengantz-super", uid => 389984878},
    {login => "fi11-super", uid => 230007533},
    {login => "gizmo-av-manager", uid => 223385476},
    {login => "johnson-direct-super", uid => 431890786},
    {login => "kappa24-super", uid => 328759976},
    {login => "kislovm-super2", uid => 187148268},
    {login => "koroandr-super", uid => 235300504},
    {login => "kovbasa-super", uid => 344089501},
    {login => "kozunov-super", uid => 21964295},
    {login => "max581-med-1", uid => 14231829},
    {login => "max581-sup", uid => 15152925},
    {login => "max581-vesh", uid => 12503569},
    {login => "msa-media", uid => 13482041},
    {login => "msa-placer", uid => 11622087},
    {login => "msa-super", uid => 10999940},
    {login => "nik-isaev-super", uid => 226317645},
    {login => "nvsmirnova-super", uid => 234900977},
    {login => "onotole-super", uid => 393229785},
    {login => "oregano-media", uid => 91924631},
    {login => "oregano-placer", uid => 91924846},
    {login => "oregano-super", uid => 91817157},
    {login => "oregano-supp", uid => 91924703},
    {login => "orlenko-super", uid => 238506891},
    {login => "samedy-super", uid => 348691387},
    {login => "secondfry-super", uid => 474852129},
    {login => "shchepetkov-super", uid => 586714557},
    {login => "skirsanov-super", uid => 524365683},
    {login => "slonoed-super", uid => 228742637},
    {login => "smaksimov-super", uid => 193479013},
    {login => "span4ik-super", uid => 199675254},
    {login => "vberesnev-super", uid => 248923523},
    {login => "yndx-afanasjeff-manager", uid => 137012150},
    {login => "yndx-afanasjeff-media", uid => 137012429},
    {login => "yndx-afanasjeff-placer", uid => 137012297},
    {login => "yndx-afanasjeff-super", uid => 136338403},
    {login => "yndx-an4is-media", uid => 300108165},
    {login => "yndx-an4is-placer", uid => 300108400},
    {login => "yndx-an4is-super", uid => 300098234},
    {login => "yndx-an4is-support", uid => 300108564},
    {login => "yndx-aspichueva-super", uid => 401245384},
    {login => "yndx-astarove-media", uid => 292851827},
    {login => "yndx-astarove-placer", uid => 292851194},
    {login => "yndx-astarove-super", uid => 292850935},
    {login => "yndx-astarove-support", uid => 292851575},
    {login => "yndx-barbaros", uid => 293160509},
    {login => "yndx-barbaros-manager", uid => 293177340},
    {login => "yndx-binarin-super", uid => 277397027},
    {login => "yndx-ddos-super", uid => 310546063},
    {login => "yndx-f1nal-media", uid => 245971813},
    {login => "yndx-f1nal-placer", uid => 245971529},
    {login => "yndx-f1nal-super", uid => 245971361},
    {login => "yndx-f1nal-support", uid => 245971703},
    {login => "yndx-gennadiy-manager", uid => 254371878},
    {login => "yndx-gennadiy-super", uid => 254371771},
    {login => "yndx-ioooosh-support-beta", uid => 405465503},
    {login => "yndx-johnyh-super", uid => 165921084},
    {login => "yndx-kabzon-super", uid => 223661153},
    {login => "yndx-kotoch-direct-support", uid => 245357630},
    {login => "yndx-kotoch-media", uid => 217966138},
    {login => "yndx-kotoch-placer", uid => 217966498},
    {login => "yndx-kotoch-super", uid => 217965476},
    {login => "yndx-n-boy-super", uid => 191935866},
    {login => "yndx-nefri-media", uid => 291332911},
    {login => "yndx-nefri-placer", uid => 291337283},
    {login => "yndx-nefri-super", uid => 291330846},
    {login => "yndx-nefri-support", uid => 291334029},
    {login => "yndx-nik-isaev-super", uid => 280878446},
    {login => "yndx-pankovpv-super", uid => 121635306},
    {login => "yndx-snaury-super", uid => 250575967},
    {login => "yndx-waveserj-mediaplan", uid => 315743560},
    {login => "yndx-waveserj-super", uid => 314492777},
    {login => "yndx-waveserj-superreader", uid => 314055665},
    {login => "yndx-waveserj-supp", uid => 315743099},
    {login => "yndx-waveserj-vesh", uid => 315742707},
    {login => "yndx-zigzag-super", uid => 367980757},
];

$log->out('START');

for my $u (@$to_delete){
    $log->out("goung to remove user '$u->{login}' (uid $u->{uid})");
    TestUsers::remove( uid => $u->{uid}, UID => $lena_san_superreader_uid );
}

$log->out('FINISH');

