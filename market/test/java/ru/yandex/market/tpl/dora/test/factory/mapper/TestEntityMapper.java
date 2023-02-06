package ru.yandex.market.tpl.dora.test.factory.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import ru.yandex.market.tpl.dora.domain.course.record.CourseCreationRecord;
import ru.yandex.market.tpl.dora.test.factory.TestCourseFactory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class TestEntityMapper {

    public static final TestEntityMapper TEST_MAPPER = Mappers.getMapper(TestEntityMapper.class);

    public abstract CourseCreationRecord toCourseCreationRecord(TestCourseFactory.CourseTestParams params);
}
