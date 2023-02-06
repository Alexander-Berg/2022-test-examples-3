package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.market.logistics.front.library.annotation.Editable;
import ru.yandex.market.logistics.front.library.annotation.FieldOrder;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.Type;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailField;
import ru.yandex.market.logistics.front.library.dto.detail.DetailItem;
import ru.yandex.market.logistics.front.library.dto.detail.DetailMeta;
import ru.yandex.market.logistics.front.library.dto.grid.GridColumn;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.logistics.front.library.dto.grid.GridMeta;

@FieldOrder({"name", "active", "length", "list"})
public class MockDto {
    private Long id;
    private String name;
    @Editable
    private boolean active;
    private int length;
    private List<Long> list;
    private String title = this.getClass().getSimpleName();

    public MockDto() {
    }

    public MockDto(Long id, String name, boolean active, int length, List<Long> list) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.length = length;
        this.list = list;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public int getLength() {
        return length;
    }

    public List<Long> getList() {
        return list;
    }

    public String getTitle() {
        return title;
    }

    public static GridData getGridData(List<MockDto> mockDtoList, Long totalCount) {
        List<GridColumn> gridColumns = Arrays.asList(
            GridColumn.builder().name("name").title("name").type(Type.STRING).build(),
            GridColumn.builder().name("active").title("active").type(Type.BOOLEAN).build(),
            GridColumn.builder().name("length").title("length").type(Type.NUMBER).build(),
            GridColumn.builder().name("list").title("list").type(Type.NUMBER_ARRAY).build()
        );

        GridMeta gridMeta = new GridMeta(gridColumns, Mode.VIEW);

        List<GridItem> gridItems = mockDtoList.stream()
            .map(mockDto -> new GridItem(mockDto.getId(), getValueMap(mockDto)))
            .collect(Collectors.toList());

        return new GridData(gridMeta, totalCount, gridItems);
    }

    public static DetailData getEmptyDetailData() {
        DetailMeta detailMeta = DetailMeta.builder().fields(getDetailFieldList()).mode(Mode.CREATE).build();

        DetailItem detailItem = new DetailItem(
            null,
            getEmptyValueMap(),
            MockDto.class.getSimpleName()
        );

        return new DetailData(detailMeta, detailItem);
    }

    public static DetailData getDetailData(MockDto mockDto) {
        DetailMeta detailMeta = DetailMeta.builder().fields(getDetailFieldList()).mode(Mode.VIEW).build();

        DetailItem detailItem = new DetailItem(
            mockDto.getId(),
            getValueMap(mockDto),
            mockDto.getTitle()
        );

        return new DetailData(detailMeta, detailItem);
    }

    private static List<DetailField> getDetailFieldList() {
        return Arrays.asList(
            DetailField.builder().name("name").title("name").type(Type.STRING).build(),
            DetailField.builder().name("active").title("active").type(Type.BOOLEAN).editable(true).build(),
            DetailField.builder().name("length").title("length").type(Type.NUMBER).build(),
            DetailField.builder().name("list").title("list").type(Type.LIST).build()
        );
    }

    private static Map<String, Object> getValueMap(MockDto mockDto) {
        Map<String, Object> map = new HashMap<>();

        map.put("name", mockDto.getName());
        map.put("active", mockDto.isActive());
        map.put("length", mockDto.getLength());
        map.put("list", mockDto.getList());

        return map;
    }

    private static Map<String, Object> getEmptyValueMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("active", false);
        map.put("length", 0);

        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MockDto mockDto = (MockDto) o;
        return active == mockDto.active &&
            length == mockDto.length &&
            Objects.equals(id, mockDto.id) &&
            Objects.equals(name, mockDto.name) &&
            Objects.equals(list, mockDto.list) &&
            Objects.equals(title, mockDto.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, active, length, list, title);
    }

    @Override
    public String toString() {
        return "MockDto{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", active=" + active +
            ", length=" + length +
            ", list=" + list +
            ", title='" + title + '\'' +
            '}';
    }
}
