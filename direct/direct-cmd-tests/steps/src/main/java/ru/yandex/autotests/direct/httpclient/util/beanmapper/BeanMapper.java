package ru.yandex.autotests.direct.httpclient.util.beanmapper;

import org.apache.commons.beanutils.BeanUtils;
import org.dozer.DozerBeanMapper;
import org.dozer.loader.api.BeanMappingBuilder;

/**
 * Created by shmykov on 09.02.15.
 */
public class BeanMapper {

    public static <SRC, DST> DST map(SRC srcBean, Class<DST> dstClass) {
        DozerBeanMapper mapper = new DozerBeanMapper();
        return mapper.map(srcBean, dstClass);
    }

    public static <SRC, DST> DST map2(SRC srcBean, DST dstBean) {
        try {
            BeanUtils.copyProperties(dstBean, srcBean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dstBean;
    }

    public static <SRC> SRC cloneBean(SRC beanToClone) {
        try {
           return (SRC) BeanUtils.cloneBean(beanToClone);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <SRC, DST, M extends BeanMappingBuilder> DST map(SRC srcBean, Class<DST> dstClass, M mappingBuilder) {
        DozerBeanMapper mapper = new DozerBeanMapper();
        mapper.addMapping(mappingBuilder);
        return mapper.map(srcBean, dstClass);
    }

    public static <SRC, DST, M extends BeanMappingBuilder> void map(SRC srcBean, DST dstBean, M mappingBuilder) {
        DozerBeanMapper mapper = new DozerBeanMapper();
        mapper.addMapping(mappingBuilder);
        mapper.map(srcBean, dstBean, null);
    }
}