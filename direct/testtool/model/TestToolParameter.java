package ru.yandex.direct.internaltools.tools.testtool.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ru.yandex.direct.internaltools.core.annotations.input.CheckBox;
import ru.yandex.direct.internaltools.core.annotations.input.Date;
import ru.yandex.direct.internaltools.core.annotations.input.DateTime;
import ru.yandex.direct.internaltools.core.annotations.input.File;
import ru.yandex.direct.internaltools.core.annotations.input.Group;
import ru.yandex.direct.internaltools.core.annotations.input.Hidden;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.MultipleSelect;
import ru.yandex.direct.internaltools.core.annotations.input.Number;
import ru.yandex.direct.internaltools.core.annotations.input.NumericId;
import ru.yandex.direct.internaltools.core.annotations.input.Select;
import ru.yandex.direct.internaltools.core.annotations.input.ShardSelect;
import ru.yandex.direct.internaltools.core.annotations.input.Text;
import ru.yandex.direct.internaltools.core.annotations.input.TextArea;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class TestToolParameter extends InternalToolParameter {
    @Input(label = "Спрятанное значение")
    @Hidden(defaultValue = "1.6")
    private BigDecimal hiddenValue;

    @Input(label = "Галочка")
    @CheckBox(checked = false)
    private Boolean cb;

    @Input(label = "Галочка со значением")
    @CheckBox(checked = true)
    private boolean cbt;

    @Input(label = "Число")
    @Number(defaultValue = 15)
    private Long number;

    @Input(label = "Число с ограничениями", required = false)
    @Number(defaultValue = -100, maxValue = 0, minValue = -200)
    private Long numberLimits;

    @Input(label = "Идентификатор", required = false)
    @NumericId()
    private Long numberId;

    @Input(label = "Шард")
    @ShardSelect
    private int shard;

    @Input(label = "Текст", required = false)
    private String text;

    @Input(label = "Текст c ограничениями", description = "Поле размером в 15 и с максимальной длиной текста 20 символов")
    @Text(valueMaxLen = 20, fieldLen = 15, defaultValue = "Умолчание")
    private String textWithLen;

    @Input(label = "Текст", required = false)
    @TextArea(columns = 80, rows = 4, defaultValue = "Текст\nТекст после переноса строки")
    private String textArea;

    @Input(label = "Выбор текста")
    @Select(choices = {"Раз", "Два"})
    private String textSelect;

    @Input(label = "Выбор енама", required = false)
    private TestEnum testEnum;

    @MultipleSelect(choices = {"Раз", "Два", "Три"}, defaultValues = {"Раз", "Три"})
    @Input(label = "Выбор нескольких элементов")
    private Set<String> stringMultipleSelect;

    @MultipleSelect
    @Input(label = "Выбор нескольких элементов енама")
    private Set<TestEnum> enumMultipleSelect;

    @MultipleSelect(choices = {"10", "200"})
    @Input(label = "MultiLongList Приоритет переотправки:", required = false)
    private Set<Long> longMultipleSelect;

    @Group(name = "Даты", priority = 15)
    @Input(label = "Дата")
    private LocalDate date;

    @Group(name = "Даты", priority = 15)
    @Input(label = "Дата с заданным значением")
    @Date(defaultValue = "2017-08-01")
    private LocalDate dateFixed;

    @Group(name = "Даты", priority = 15)
    @Input(label = "Дата с текущим значением")
    @Date(today = true)
    private LocalDate dateToday;

    @Group(name = "Время", priority = 20)
    @Input(label = "Время")
    private LocalDateTime dateTime;

    @Group(name = "Время", priority = 20)
    @Input(label = "Время с заданным значением")
    @DateTime(defaultValue = "2017-08-01T04:15:22")
    private LocalDateTime dateTimeFixed;

    @Group(name = "Время", priority = 20)
    @Input(label = "Время с текущим значением")
    @DateTime(now = true)
    private LocalDateTime dateTimeNow;

    @Input(label = "Первый файл")
    @File
    private byte[] file;

    @Input(label = "Необязательный файл", required = false)
    @File
    private byte[] fileNotRequired;

    public BigDecimal getHiddenValue() {
        return hiddenValue;
    }

    public void setHiddenValue(BigDecimal hiddenValue) {
        this.hiddenValue = hiddenValue;
    }

    public Boolean getCb() {
        return cb;
    }

    public void setCb(Boolean cb) {
        this.cb = cb;
    }

    public boolean isCbt() {
        return cbt;
    }

    public void setCbt(boolean cbt) {
        this.cbt = cbt;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public int getShard() {
        return shard;
    }

    public void setShard(int shard) {
        this.shard = shard;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTextWithLen() {
        return textWithLen;
    }

    public void setTextWithLen(String textWithLen) {
        this.textWithLen = textWithLen;
    }

    public String getTextArea() {
        return textArea;
    }

    public void setTextArea(String textArea) {
        this.textArea = textArea;
    }

    public String getTextSelect() {
        return textSelect;
    }

    public void setTextSelect(String textSelect) {
        this.textSelect = textSelect;
    }

    public Set<String> getStringMultipleSelect() {
        return stringMultipleSelect;
    }

    public void setStringMultipleSelect(Set<String> stringMultipleSelect) {
        this.stringMultipleSelect = stringMultipleSelect;
    }

    public Set<TestEnum> getEnumMultipleSelect() {
        return enumMultipleSelect;
    }

    public void setEnumMultipleSelect(Set<TestEnum> enumMultipleSelect) {
        this.enumMultipleSelect = enumMultipleSelect;
    }

    public Set<Long> getLongMultipleSelect() {
        return longMultipleSelect;
    }

    public void setLongMultipleSelect(Set<Long> longMultipleSelect) {
        this.longMultipleSelect = longMultipleSelect;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDateFixed() {
        return dateFixed;
    }

    public void setDateFixed(LocalDate dateFixed) {
        this.dateFixed = dateFixed;
    }

    public LocalDate getDateToday() {
        return dateToday;
    }

    public void setDateToday(LocalDate dateToday) {
        this.dateToday = dateToday;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public LocalDateTime getDateTimeFixed() {
        return dateTimeFixed;
    }

    public void setDateTimeFixed(LocalDateTime dateTimeFixed) {
        this.dateTimeFixed = dateTimeFixed;
    }

    public LocalDateTime getDateTimeNow() {
        return dateTimeNow;
    }

    public void setDateTimeNow(LocalDateTime dateTimeNow) {
        this.dateTimeNow = dateTimeNow;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public byte[] getFileNotRequired() {
        return fileNotRequired;
    }

    public TestEnum getTestEnum() {
        return testEnum;
    }

    public void setTestEnum(TestEnum testEnum) {
        this.testEnum = testEnum;
    }

    public void setFileNotRequired(byte[] fileNotRequired) {
        this.fileNotRequired = fileNotRequired;
    }

    public Long getNumberLimits() {
        return numberLimits;
    }

    public void setNumberLimits(Long numberLimits) {
        this.numberLimits = numberLimits;
    }

    public Long getNumberId() {
        return numberId;
    }

    public void setNumberId(Long numberId) {
        this.numberId = numberId;
    }
}
