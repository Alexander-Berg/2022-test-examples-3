package ru.yandex.market.supercontroller.dao;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class CacheDaoTest extends TestCase {
	
	
	private String generateSql(final List<String> sessions) {
		StringBuilder sql = new StringBuilder("delete from supercontroller_cache where session_id in (");
		for(String sessionId : sessions) {
			sql.append('\'').append(sessionId).append("',");
		}
		sql.replace(sql.length()-1, sql.length(), ")");
		return sql.toString();
	}
	
	public void testSqlGeneration() {
		String[] first = new String[] {"2007"};
		String[] second = new String[] {"2007", "2008"};
		assertEquals("delete from supercontroller_cache where session_id in ('2007')", generateSql(Arrays.asList(first)));		
		assertEquals("delete from supercontroller_cache where session_id in ('2007','2008')", generateSql(Arrays.asList(second)));		
	}

}
