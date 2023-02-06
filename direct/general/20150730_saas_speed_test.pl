#!/usr/bin/perl

use my_inc '..';

=head1 DEPLOY

# approved by pankovpv
# .migr
{
  type => 'script',
  when => 'after',
  time_estimate => "20 минут",
  comment => "Проверяем время ответа SaaS-ручки про мобильные приложения"
}

=cut

use warnings;
use strict;
use utf8;

use Yandex::HTTP qw/http_parallel_request/;

use Settings;
use ScriptHelper;

my @countries = qw/ru tr ua kz by en/;

my $apps = {
    gplay => [qw/
        AKnght.Studios.Kids123Lite AOIC.AOIC_kaz01 AW.FlashLights com.monefy.app.pro Affinity.IQ Air.Density Alekseyt.Lusher
        Alfasoft.Widgets.GermanyFlagAnalogClock Alfasoft.Widgets.ItalyFlagAnalogClock Alfasoft.Widgets.WhiteTigerAnalogClock
        Android.WirelessPassword Android.app App.Igor.Blondinenwitze AppClude.TravelEssentialUtility ArgosDONG.Mobile AvanteSales.AvanteSales
        BALLSOFSTEEL.pac BKE.blom.com BSH.SmartHouseSystem BabyRun.Rizlac2 BhagatSingh.ninjastrike Blasting.goodteam.cn
        Bluehertz.zaragoza.english Bridgepoint.Mobile.ExpenseReport.Android COM.HSB COM.MB4GLS CallTsubame.Tsubame Calutor.com
        Catala.Paquete.Agenda Change8ToPlus7.Change8ToPlus7 CheeseAndMouseH.app Chinese.full CigControlPro.By.KwezitPro Clalit.Clock
        Cn.sasj.hop.ring Com.GamesLab.PennyParlor Com.shaun.MobileToggle Courier_Connect.agents Cust.Menu CutebabyTW.CrashLogPro
        DTH.got.Healer.v1 DanS.games.simonSaid Dardiries.BDI.BDI Ddongddo.propro DragonY_ad.intersave DressUp_MyPet.lain Elegant.Blue.Keyboard
        Eng.Word.Game Eweb.Redots Excendia.Mobility.SmartPhone Face.Sorter Finkbeiner.Games.WC Fito.TicTacToe Floorballmatch.statistics.token
        GFR_HO.COM GPSInvaders.lite GY86BP8AU6.com.lesmercenaires.btp74_tablette Game.app GaryNg.VMC GeneratorRexPackFighter.generatorrexpackfighter
        GeoSpotX.SpotXmobile GetMe2Wellington.source Golf_Olympic.app Grocery.List GymScores.Scores HKExpo.kineticspace.net
        IncaSoft.AprendeBebe IpricoT.yerevanararathdlwp Itgmbh.Elvis.AndroidViewer Jinseo.Park.NandS JoyApp.JoyPsy KU.Campus.Life
        KX4646B8J2.ice.EazeeScreen.BlockBuster KiLi.Puzzle Krea.Comunicaciones KrishnaaurKansMKP.borderwar KtaiS.Spider KwachsTextConverter.ark
        LA.HAM LASroCd.game LaMeuseGPS.Motor Leeks.Design.leeks19plus.Widgets.AnalogClock LetFlash.com LiveWallpapers.cashinhandslivewallpaperlwpfree
        LiveWallpapers.gangstamoneylivewallpapergamelwp LiveWallpapers.lovegamelivewallpaperlwpfree LiveWallpapers.magicmoneytreewallpaperlwpfree
        LiveWallpapers.magicstarslivewallpaperlwpfree MHill.mbhRemote MMSL.BGGlucoDiary Magic.FinCal.namespace Manual.BluetoothConnecting
        Martintest.test MillerTech.santalwp Mortgage.CalculatorBrown Mortgage.CalculatorClow Mortgage.CalculatorEvered Mortgage.CalculatorHoagland
        NS.popkon.real NadavRL.Scheduled.MessagesFree NantesChateauUK.Motor com.fakedomain.fakeapp.one com.fakedomain.fakeapp.two com.fakedomain.fakeapp.three
    /],
    itunes => [qw/
        id653287635 id368002213 id828790992 id511459648 id467085833 id594843575 id877499600 id375584677 id923920146 id526357562
        id576442111 id550923627 id660660032 id907400995 id534082757 id397553707 id719525810 id631881629 id319170675 id435039432
        id948656560 id920851306 id768223890 id581641031 id902860881 id667728512 id651510680 id909124597 id840066184 id794957821
        id432531914 id714796093 id944109582 id918259711 id929329226 id911152486 id370339723 id585661281 id649362246 id857514427
        id929036047 id945469538 id695415794 id635148681 id751160885 id705126311 id858467955 id876635670 id790203750 id431946152
        id586634331 id586634331 id953927443 id286274367 id800844884 id789121732 id864837628 id398384707 id565841375 id623673775
        id506556769 id955785392 id778026240 id469308137 id436762566 id674996719 id372284136 id895324943 id693206028 id527445936
        id615921018 id734258668 id890412250 id569349270 id481896903 id440045374 id950608825 id608847384 id414664715 id769737983
        id896028023 id484550839 id399428058 id529040479 id601949690 id496386846 id597986893 id544889292 id925089729 id913292932
        id453640300 id818300899 id965654686 id533366852 id948618510 id623731879 id670753987 id900476260 id467200220 id326863049
        id000000000 id000000001 id000000002
    /],
};

my $req_timeout = 3;    # таймаут ответа ручки, сек.
my $iterations = 5;     # сколько раз повторяем все запросы

$log->out('START');

foreach my $iteration (1..$iterations) {
    $log->out("Iteration $iteration");
    my $id = 0;
    my $reqs;
    foreach my $store (keys %$apps) {
        my $params = {
            store => $store,
            content_type => 'app',
        };
        foreach my $app (@{$apps->{$store}}) {
            $params->{app_id} = $app;
            foreach my $country (@countries) {
                $params->{country} = $country;
                my $url = Yandex::HTTP::make_url($Settings::MOBILE_APPLICATION_URL, $params);
                $reqs->{$id++} = { url => $url };
            }
        }
    }

    my $resp = Yandex::HTTP::http_parallel_request(
            GET => $reqs,
            max_req => 10,
            timeout => $req_timeout,
            soft_timeout => 0.5,
            num_attempts => 2,
        );
    my $total_requests = scalar keys $resp; # всего запросов
    my $is_success = 0;                     # получили успешный ответ
    my $min_time = $resp->{0}->{elapsed};   # самый быстрый ответ
    my $max_time = $resp->{0}->{elapsed};   # самый медленный ответ


    my $bad_reqs;       # запросы, которые не выполнились успешно
    my $sum_time = 0;   # сумма для подсчета средного
    my @responses_time; # массив для подсчета медианы

    my $timeouted_requests; # хеш с запросами, которые превысили таймаут

    # гистограмма распределения времен ответов ручки от 0 до 3 сек, с шагом 0.1 сек
    my $histogram = {};
    for (my $i = 0; $i < $req_timeout; $i += 0.1) {
        $histogram->{$i} = 0;
    }

    foreach my $key (keys %$resp) {
        # запросы, которые превысили таймаут
        if ($resp->{$key}->{elapsed} > $req_timeout) {
            $timeouted_requests->{$reqs->{$key}->{url}} = $resp->{$key}->{elapsed};
        }
        # заносим значения в гистограмму
        foreach my $timeout (sort {$b <=> $a} keys %$histogram) {
            if ($resp->{$key}->{elapsed} > $timeout) {
                $histogram->{$timeout}++;
                last;
            }
        }
        # ищем максимальное и минимальное время ответа
        $min_time = $resp->{$key}->{elapsed} if ($min_time > $resp->{$key}->{elapsed});
        $max_time = $resp->{$key}->{elapsed} if ($max_time < $resp->{$key}->{elapsed});
        # сумма для подсчета среднего
        $sum_time += $resp->{$key}->{elapsed};
        # для вычисления медианы
        push @responses_time, $resp->{$key}->{elapsed};
        # счетчик успешных ответов
        if ($resp->{$key}->{is_success}) {
            $is_success++;
        } else {
            push @$bad_reqs, $reqs->{$key}->{url};
        }
    }

    my $mean_time = $sum_time/$total_requests;
    my $median_time = median(\@responses_time);

    foreach my $timeout (sort {$a <=> $b} keys %$histogram) {
        $log->out("$timeout\t$histogram->{$timeout}");
    }
    $log->out("Min time:\t$min_time");
    $log->out("Max time:\t$max_time");
    $log->out("Mean time:\t$mean_time");
    $log->out("Median time:\t$median_time");
    $log->out("Total requests:\t$total_requests");
    $log->out("Successful responses:\t$is_success");
    $log->out("Timeouted requests:");
    foreach my $url (keys %$timeouted_requests) {
        $log->out("$timeouted_requests->{$url}\t$url");
    }
    $log->out("Bad requests:");
    foreach my $url (@$bad_reqs) {
        $log->out($url);
    }
}

$log->out('FINISH');


sub median
{
    my $data = shift;

    my @vals = sort {$a <=> $b} @$data;
    my $len = @vals;
    if($len%2) {
        return $vals[int($len/2)];
    } else {
        return ($vals[int($len/2)-1] + $vals[int($len/2)])/2;
    }
}
