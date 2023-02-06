package ru.yandex.direct.logicprocessor.processors.bsexport.bids

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType.DYNAMIC
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType.KEYWORD
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType.PERFORMANCE
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType.RELEVANCE_MATCH
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BidObjectType.RETARGETING
import ru.yandex.direct.ess.logicobjects.bsexport.bids.BsExportBidsObject
import ru.yandex.direct.logicprocessor.configuration.EssLogicProcessorConfiguration


object BiddableShowConditionsServiceManualRun {
    @JvmStatic
    fun main(args: Array<String>) {
        AnnotationConfigApplicationContext(EssLogicProcessorConfiguration::class.java).use { ctx ->
            val service = ctx.getBean(BsExportBiddableShowConditionsService::class.java)
            service.updateResources(20, testCases())
        }
    }

    fun logicObject(type: BidObjectType, cid: Long?, pid: Long, id: Long, isDeleted: Boolean) =
        BsExportBidsObject(cid, pid, id, type, isDeleted)

    fun testCases() = listOf(
        logicObject(KEYWORD, 40865710, 3694969246, 15795317480, false),
        logicObject(KEYWORD, 40865710, 3694969259, 15795317493, false),
        logicObject(KEYWORD, 40865710, 3694969272, 15795317506, false),
        logicObject(KEYWORD, 40865726, 3694970614, 15795341938, false),
        logicObject(KEYWORD, 40865726, 3694970675, 15795341999, false),
        logicObject(KEYWORD, 40864330, 3694974929, 15795376383, false),
        logicObject(KEYWORD, 40866182, 3694987339, 15795474547, false),
        logicObject(KEYWORD, 40866182, 3694987372, 15795474582, false),
        logicObject(KEYWORD, 40866182, 3694987381, 15795474593, false),
        logicObject(KEYWORD, 40866182, 3694987240, 15795474619, true),
        logicObject(DYNAMIC, null, 3710565066, 804162, true),
        logicObject(DYNAMIC, null, 3710594486, 804199, false),
        logicObject(DYNAMIC, null, 3710611245, 804242, false),
        logicObject(DYNAMIC, null, 3710624927, 804322, false),
        logicObject(DYNAMIC, null, 3710625046, 804333, true),
        logicObject(DYNAMIC, null, 3710649518, 804364, false),
        logicObject(DYNAMIC, null, 3710695442, 804439, true),
        logicObject(DYNAMIC, null, 3715504894, 807606, true),
        logicObject(DYNAMIC, null, 3717210541, 882054, true),
        logicObject(DYNAMIC, null, 3721163009, 811957, false),
        logicObject(PERFORMANCE, null, 3696320243, 424666, false),
        logicObject(PERFORMANCE, null, 3696320243, 424667, true),
        logicObject(PERFORMANCE, null, 3696582777, 420944, false),
        logicObject(PERFORMANCE, null, 3725281090, 433575, false),
        logicObject(PERFORMANCE, null, 3743531594, 438072, false),
        logicObject(PERFORMANCE, null, 3753405129, 441250, false),
        logicObject(PERFORMANCE, null, 3757975905, 442735, false),
        logicObject(PERFORMANCE, null, 3761717289, 447614, true),
        logicObject(PERFORMANCE, null, 3766526341, 445459, false),
        logicObject(PERFORMANCE, null, 3766530925, 457072, true),
        logicObject(RELEVANCE_MATCH, 40878707, 3695764175, 15800601749, false),
        logicObject(RELEVANCE_MATCH, 40886164, 3696210008, 15803712356, false),
        logicObject(RELEVANCE_MATCH, 40896696, 3696776052, 15807916621, true),
        logicObject(RELEVANCE_MATCH, 40921462, 3698240024, 15816773316, false),
        logicObject(RELEVANCE_MATCH, 40951737, 3700138111, 15827354853, false),
        logicObject(RELEVANCE_MATCH, 40966141, 3700848047, 15832632675, true),
        logicObject(RELEVANCE_MATCH, 40971345, 3701305902, 15835759417, true),
        logicObject(RELEVANCE_MATCH, 40986133, 3701975175, 15839621746, false),
        logicObject(RELEVANCE_MATCH, 40998169, 3702537371, 15843854023, false),
        logicObject(RELEVANCE_MATCH, 41023939, 3703801186, 15853929510, true),
        logicObject(RETARGETING, 40935146, 3699315030, 12765081, false),
        logicObject(RETARGETING, 40981654, 3701626965, 12779194, false),
        logicObject(RETARGETING, 40981645, 3701625439, 12779244, false),
        logicObject(RETARGETING, 40981615, 3701622767, 12779296, false),
        logicObject(RETARGETING, 40981603, 3701621835, 12779322, false),
        logicObject(RETARGETING, 40977669, 3704660158, 12795926, true),
        logicObject(RETARGETING, 41122791, 3707514421, 12808856, false),
        logicObject(RETARGETING, 41125907, 3707676112, 12809375, true),
        logicObject(RETARGETING, 41129669, 3707917495, 12810207, false),
        logicObject(RETARGETING, 41129669, 3707917495, 12810209, false),
    )
}

