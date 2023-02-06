package ru.yandex.direct.manualtests.tasks.bsgoalsstat;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.grid.core.entity.campaign.repository.GridCampaignYtRepository;
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService;
import ru.yandex.direct.manualtests.app.TestTasksRunner;

/**
 * Получение статистики для составных целей компаний из YT БК
 */
@Component
public class BsGoalsStatReceiver implements Runnable {
    public static void main(String[] args) {
        TestTasksRunner.runTask(BsGoalsStatReceiver.class, BsGoalsStatConfiguration.class, args);
    }

    @Autowired
    private GridCampaignYtRepository gridCampaignYtRepository;

    @Autowired
    private GridCampaignService gridCampaignService;

    @Override
    public void run() {
        // https://direct.yandex.ru/web-api/grid/?operationName=GetMetrikaGoalsByCounter&user_login=lagency-255150-js3i&variables=%7B%0A%20%20%22input%22%3A%20%7B%0A%20%20%20%20%22campaignId%22%3A%20%2266152234%22%2C%0A%20%20%20%20%22campaignType%22%3A%20%22PERFORMANCE%22%2C%0A%20%20%20%20%22counterIds%22%3A%20%5B%0A%20%20%20%20%20%20%2269838900%22%0A%20%20%20%20%5D%0A%20%20%7D%2C%0A%20%20%22login%22%3A%20%22lagency-255150-js3i%22%0A%7D#query%20GetMetrikaGoalsByCounter(%24input%3A%20GdMetrikaGoalsByCounterInput!)%20%7B%0A%20%20reqId%3A%20getReqId%0A%20%20getMetrikaGoalsByCounter(input%3A%20%24input)%20%7B%0A%20%20%20%20goals%20%7B%0A%20%20%20%20%20%20id%0A%20%20%20%20%20%20name%0A%20%20%20%20%20%20type%0A%20%20%20%20%20%20domain%0A%20%20%20%20%20%20parentId%0A%20%20%20%20%20%20simpleName%0A%20%20%20%20%20%20counterId%0A%20%20%20%20%20%20metrikaGoalType%0A%20%20%20%20%20%20mobileAppId%0A%20%20%20%20%20%20mobileAppName%0A%20%20%20%20%20%20conversionVisitsCount%0A%20%20%20%20%20%20hasPrice%0A%20%20%20%20%7D%0A%20%20%20%20isMetrikaAvailable%0A%20%20%20%20top1GoalId%0A%20%20%7D%0A%7D%0A
        Set<Long> campaingsIds = Set.of(57232533L, 57232548L, 57232557L, 57232563L, 57232573L, 57232580L, 57232585L,
                57232589L, 57232596L, 57232606L, 57232617L, 57232622L, 57232626L, 57232631L, 57232634L, 57232637L,
                57232644L, 57232647L, 57232652L, 57232661L, 57232665L, 57232680L, 57232686L, 57232689L, 57232692L,
                57232696L, 57232706L, 57232713L, 57232718L, 57232719L, 57232724L, 57232727L, 57232741L, 57232753L,
                57232764L, 57232769L, 57232890L, 57234367L, 57234574L, 57234662L, 57234674L, 57234677L, 57234683L,
                57234753L, 57234797L, 57234922L, 57234958L, 57234987L, 57235138L, 57235255L, 57235314L, 57248915L,
                57370705L, 57428050L, 57485208L, 57520521L, 57523672L, 57543042L, 57543048L, 57543132L, 57544210L,
                57564809L, 57666141L, 57758196L, 57823561L, 57823567L, 57824491L, 57965850L, 57984676L, 57985403L,
                58027199L, 58030527L, 58030530L, 58145270L, 58310236L, 58334173L, 58451532L, 58451682L, 58505213L,
                58505214L, 58545689L, 58725387L, 58837176L, 58872197L, 58872208L, 58892983L, 58892985L, 58943606L,
                59093095L, 59253671L, 59276317L, 59391263L, 59631395L, 59729506L, 59748350L, 59753261L, 59785715L,
                59881684L, 59881687L, 59911406L, 59931213L, 59985426L, 60027274L, 60027292L, 60027300L, 60040479L,
                60132387L, 60454815L, 60794061L, 60820650L, 60973355L, 61033894L, 61053257L, 61129426L, 61189902L,
                61233950L, 61349395L, 61756628L, 61756810L, 61785722L, 61941689L, 61945934L, 62423906L, 62424241L,
                62553858L, 63307089L, 63384214L, 63384217L, 63387950L, 63435429L, 63452198L, 63519098L, 63642466L,
                63647981L, 63666204L, 63694013L, 63755488L, 63777249L, 63777251L, 63790769L, 63790774L, 63827980L,
                63831539L, 63832857L, 63834919L, 63834921L, 63836094L, 63876911L, 63876943L, 63877259L, 63877490L,
                63878445L, 63879941L, 63880084L, 63880138L, 63880163L, 63883475L, 63892679L, 63906746L, 63911563L,
                63920224L, 63920225L, 63920228L, 63937792L, 63993493L, 63994497L, 63994505L, 64012925L, 64022271L,
                64040336L, 64082484L, 64096127L, 64109962L, 64120268L, 64120440L, 64120442L, 64134458L, 64141276L,
                64141280L, 64171742L, 64173757L, 64200275L, 64223316L, 64223319L, 64231086L, 64231097L, 64248234L,
                64248244L, 64260556L, 64260559L, 64329439L, 64338431L, 64346028L, 64346039L, 64461264L, 64467406L,
                64518452L, 64545813L, 64545978L, 64629505L, 64629518L, 64917424L, 64917430L, 64917438L, 64917448L,
                64917456L, 64917467L, 64921286L, 64921297L, 64921306L, 64921317L, 64921325L, 64937254L, 64937678L,
                64938387L, 64939772L, 64950456L, 64958302L, 64958309L, 64960006L, 64960011L, 64967516L, 64967529L,
                65194592L, 65205723L, 65213196L, 65213199L, 65213248L, 65249103L, 65306325L, 65322324L, 65322342L,
                65333991L, 65360723L, 65490833L, 65507530L, 65507987L, 65507988L, 65709106L, 66152234L, 66843328L,
                67297705L, 67945020L, 68028251L, 68031864L, 68462474L, 68464811L, 68470421L, 68472209L, 68473762L,
                68480015L, 68592366L, 68703203L, 68730101L, 68939902L, 68945121L, 68945129L, 68945138L);
        Set<Long> goalsIds = Set.of(226384145L, 226374929L);
        var response1 = gridCampaignYtRepository.getCampaignGoalsConversionsCount(
                campaingsIds, LocalDate.now().minusDays(7), LocalDate.now(), goalsIds);

        var response2 = gridCampaignService.getCampaignStatsWithOptimization(
                campaingsIds, LocalDate.now().minusDays(7), LocalDate.now(), goalsIds);

        System.out.println(String.format("gridCampaignYtRepository.getCampaignGoalsConversionsCount = '%s'%n" +
                "gridCampaignService.getCampaignStatsWithOptimization = '%s'", response1, response2));
    }

}

