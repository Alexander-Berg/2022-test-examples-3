package ru.yandex.market.books.diff.isbnchain;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import junit.framework.TestCase;

import java.util.List;

/**
 * todo описать предназначение
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class ChainsIndexTest extends TestCase {
    public void testBuildChainsIndex() {
        IsbnsChain chain1 = new IsbnsChain(100L, 200L, 300L, 400L);
        IsbnsChain chain2 = new IsbnsChain(400L, 500L, 600L, 700L, 800L);

        IsbnsChain chain3 = new IsbnsChain(101L, 201L, 301L, 401L, 501L, 601L);

        IsbnsChain chain4 = new IsbnsChain(2001L, 2002L, 2003L, 2004L);
        IsbnsChain chain5 = new IsbnsChain(2004L, 2005L, 2006L, 2007L);
        IsbnsChain chain6 = new IsbnsChain(2007L, 2008L, 2009L, 2010L);
        IsbnsChain chain7 = new IsbnsChain(2010L, 2011L, 2012L, 2013L);
        IsbnsChain chain8 = new IsbnsChain(2013L, 2014L, 2015L, 2016L);

        ChainsIndex chainsIndex = ChainsIndex.buildChainsIndex(
            Iterators.forArray(chain1, chain2, chain3),
            Iterators.forArray(chain4, chain5),
            Iterators.forArray(chain6, chain7),
            Iterators.forArray(chain8)
        );

//		Collection<Long> linkedWith50 = chainsIndex.getLinkedIsbns(50L);
//		assertNull(linkedWith50);
//
//		Collection<Long> linkedWith100 = chainsIndex.getLinkedIsbns(100L);
//		assertNotNull(linkedWith100);
//		assertEquals(8, linkedWith100.size());
//
//		Collection<Long> linkedWith400 = chainsIndex.getLinkedIsbns(400L);
//		assertNotNull(linkedWith400);
//		assertEquals(8, linkedWith400.size());
//
//		Collection<Long> linkedWith600 = chainsIndex.getLinkedIsbns(600L);
//		assertNotNull(linkedWith600);
//		assertEquals(8, linkedWith600.size());
//
//		Collection<Long> linkedWith101 = chainsIndex.getLinkedIsbns(101L);
//		assertNotNull(linkedWith101);
//		assertEquals(6, linkedWith101.size());
//
//		Collection<Long> linkedWith2001 = chainsIndex.getLinkedIsbns(2001L);
//		assertNotNull(linkedWith2001);
//		assertEquals(16, linkedWith2001.size());
//
//		Collection<Long> linkedWith2002 = chainsIndex.getLinkedIsbns(2002L);
//		assertNotNull(linkedWith2002);
//		assertEquals(16, linkedWith2002.size());
//		assertEquals(linkedWith2001, linkedWith2002);
//
//		Collection<Long> linkedWith2012 = chainsIndex.getLinkedIsbns(2012L);
//		assertNotNull(linkedWith2012);
//		assertEquals(16, linkedWith2012.size());
//		assertEquals(linkedWith2002, linkedWith2012);

        List<long[]> isbnsArrays = chainsIndex.getIsbnsArrays();
        assertEquals(3, isbnsArrays.size());
        LongSet allIsbnsSet = new LongLinkedOpenHashSet();
        for (long[] isbnsArray : isbnsArrays) {
            assertTrue(6 == isbnsArray.length || 8 == isbnsArray.length || 16 == isbnsArray.length);
            for (long isbn : isbnsArray) {
                assertTrue(allIsbnsSet.add(isbn));
            }
        }
        assertEquals(30, allIsbnsSet.size());
    }
}
