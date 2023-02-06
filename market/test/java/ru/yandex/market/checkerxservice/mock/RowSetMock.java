package ru.yandex.market.checkerxservice.mock;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

public class RowSetMock implements SqlRowSet {
    private int idx = -1;
    private List<Map<String, Object>> dataList;

    public RowSetMock(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
    }

    @Override
    public SqlRowSetMetaData getMetaData() {
        return null;
    }

    @Override
    public int findColumn(String columnLabel) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public byte getByte(String columnLabel) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Date getDate(String columnLabel) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public double getDouble(String columnLabel) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public float getFloat(String columnLabel) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public int getInt(int columnIndex) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public int getInt(String columnLabel) throws InvalidResultSetAccessException {
        return (Integer) dataList.get(idx).get(columnLabel);
    }

    @Override
    public long getLong(int columnIndex) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public long getLong(String columnLabel) throws InvalidResultSetAccessException {
        return (Long) dataList.get(idx).get(columnLabel);
    }

    @Override
    public String getNString(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public String getNString(String columnLabel) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public short getShort(int columnIndex) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public short getShort(String columnLabel) throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public String getString(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public String getString(String columnLabel) throws InvalidResultSetAccessException {
        return (String) dataList.get(idx).get(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Time getTime(String columnLabel) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
        return null;
    }

    @Override
    public boolean absolute(int row) throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public void afterLast() throws InvalidResultSetAccessException {

    }

    @Override
    public void beforeFirst() throws InvalidResultSetAccessException {
        this.idx = -1;
    }

    @Override
    public boolean first() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public int getRow() throws InvalidResultSetAccessException {
        return 0;
    }

    @Override
    public boolean isAfterLast() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean isBeforeFirst() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean isFirst() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean isLast() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean last() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean next() throws InvalidResultSetAccessException {
        this.idx++;
        return this.idx < dataList.size();
    }

    @Override
    public boolean previous() throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean relative(int rows) throws InvalidResultSetAccessException {
        return false;
    }

    @Override
    public boolean wasNull() throws InvalidResultSetAccessException {
        return false;
    }
}
