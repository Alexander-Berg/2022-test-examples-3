package ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters;

import org.dozer.DozerConverter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 03.04.15
 */
public class MobileBidAdjustmentConverter extends DozerConverter<Integer, Integer> {

    public MobileBidAdjustmentConverter() {
        super(Integer.class, Integer.class);
    }

    @Override
    public Integer convertTo(Integer source, Integer destination) {
        if (source == null) {
            return 100;
        } else {
            return source + 100;
        }
    }

    @Override
    public Integer convertFrom(Integer source, Integer destination) {
        if (source == 100) {
            return null;
        } else {
            return source - 100;
        }

    }
}
