package ru.yandex.market.delivery.transport_manager.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.ScheduleMetaData;
import ru.yandex.market.delivery.transport_manager.repository.mappers.YtScheduleMetadataMapper;

public class ScheduleMetaDataMapperTest extends AbstractContextualTest {

    @Autowired
    YtScheduleMetadataMapper scheduleMetadataMapper;

    private static final ScheduleMetaData XML_SCHEDULE_METADATA = new ScheduleMetaData(1593454806L, "233");

    @Test
    @DatabaseSetup("/repository/schedule/schedule_meta_data.xml")
    void testGetMetadata() {
        ScheduleMetaData current = scheduleMetadataMapper.getCurrent();
        assertThatModelEquals(XML_SCHEDULE_METADATA, current);
    }

    @Test
    @DatabaseSetup("/repository/schedule/schedule_meta_data.xml")
    void testUpdateMetadata() {
        ScheduleMetaData metadataUpdate = new ScheduleMetaData(1593454806L, "234");
        scheduleMetadataMapper.updateMetadata(metadataUpdate);
        ScheduleMetaData current = scheduleMetadataMapper.getCurrent();
        assertThatModelEquals(metadataUpdate, current);
    }

    @Test
    @DatabaseSetup("/repository/schedule/schedule_meta_data.xml")
    void testDeleteMetadata() {
        assertThatModelEquals(XML_SCHEDULE_METADATA, scheduleMetadataMapper.getCurrent());
        scheduleMetadataMapper.deleteMetadata();
        assertThatModelEquals(null, scheduleMetadataMapper.getCurrent());
    }

    @Test
    void testUpdateMetadataOnEmptyDb() {
        ScheduleMetaData metadataUpdate = new ScheduleMetaData(1593454806L, "234");
        scheduleMetadataMapper.updateMetadata(metadataUpdate);
        ScheduleMetaData current = scheduleMetadataMapper.getCurrent();
        assertThatModelEquals(metadataUpdate, current);
    }
}
