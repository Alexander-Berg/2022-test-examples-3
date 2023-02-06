package ru.yandex.market.books.dao;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.utils.jdbc.BulkInserter;
import ru.yandex.utils.jdbc.InsertRowMapper;
import ru.yandex.utils.jdbc.MySqlBulkInserter;
//import ru.yandex.utils.spring.SpringContextTestCase;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("checkstyle:magicnumber")
public class IsbnTest /*extends SpringContextTestCase*/ {
    private static final Logger log = Logger.getLogger(IsbnTest.class);

    private DataSource dataSource;
    private JdbcTemplate template;

    private static class Offer {
        String isbn;
        String publisher;
        String year;
        int modelId;
    }

/*    @SuppressWarnings("unchecked")
   public void testIsbn() {
       final List<Offer> isbns = new ArrayList<Offer>();
       ChunkLoader<Offer> loader = new MySqlChunkLoader<Offer>();
       loader.init("isbn, publisher, year, id", 10000, new RowMapper() {
           public Object mapRow(ResultSet rs, int arg1) throws SQLException {
               Offer offer = new Offer();
               offer.isbn = rs.getString(1);
               offer.publisher = StringEncodingUtils.getWindows(rs.getBytes(2));
               offer.year = rs.getString(3);
               offer.modelId = rs.getInt(4);
               return offer;
           }

       }, dataSource, "book_dump3");
       while(loader.hasNext()) {
           for(Offer o : loader.nextChunk()) {
               if(o.isbn != null) {
                   StringTokenizer st = new StringTokenizer(o.isbn);
                   StringTokenizer st2 = new StringTokenizer(o.publisher);
                   StringTokenizer st3 = new StringTokenizer(o.year);
                   while(st.hasMoreTokens()) {
                       Offer no = new Offer();
                       isbns.add(st.nextToken());
                   }
               }
           }
       }
       saveIsbns(isbns, "our_isbn");
   }*/

    private void saveIsbnsAndMore(final List<Offer> isbns, final String tableName) {
        template.execute(new ConnectionCallback() {
            public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
                BulkInserter inserter = new MySqlBulkInserter();
                inserter.insert("insert ignore into " + tableName + "(isbn, publisher, year, model_id)", isbns,
                        4, new InsertRowMapper<Offer>() {
                    public void mapRow(int index, Offer element, PreparedStatement statement) throws SQLException {
                        statement.setObject(index + 1, element);
                        statement.setObject(index + 2, element);
                        statement.setObject(index + 3, element);
                        statement.setObject(index + 4, element);
                    }
                }, connection);
                return null;
            }
        });
    }

    private void saveIsbns(final List<String> isbns, final String tableName) {
        template.execute(new ConnectionCallback() {
            public Object doInConnection(Connection connection) throws SQLException, DataAccessException {
                BulkInserter inserter = new MySqlBulkInserter();
                inserter.insert("insert ignore into " + tableName + "(isbn)", isbns, 1, new InsertRowMapper<String>() {
                    public void mapRow(int index, String element, PreparedStatement statement) throws SQLException {
                        statement.setString(index + 1, element);
                    }
                }, connection);
                return null;
            }
        });
    }

    /*    public void testRkp() throws Exception {
            List<String> isbns = new ArrayList<String>();
            File folder = new File("./");
            for(String f : folder.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith(".csv");
                }

            })) {
                isbns.addAll(readIsbns(f));
            }
            saveIsbns(isbns, "rkp_isbn");
        }
    */
    private List<String> readIsbns(String file) throws FileNotFoundException, IOException {
        List<String> isbns = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String s;
        while ((s = reader.readLine()) != null) {
            String[] ss = s.split("[\\s;]+");
            boolean d = false;
            for (int i = 0; i < ss.length; i++) {
                ss[i] = ss[i].replace("-", "").toLowerCase().replace('�', 'x');
                if (ss[i] != null && checkIsbn(ss[i])) {
                    isbns.add(ss[i].toUpperCase());
                    d = true;
                }
            }
            if (!d) {
                log.debug(s);
            }
        }
        return isbns;
    }

    //@Override
    protected String[] getConfigLocations() {
        return new String[]{"fake-monitoring.xml"};
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        template = new JdbcTemplate(dataSource);
    }


    public static boolean checkIsbn(String isbn) {
        return checkIsbn(isbn, false);
    }

    private static boolean checkIsbn(String isbn, boolean xSet) {
        if (isbn.length() == 10 || (xSet && isbn.length() == 9)) {
            int x = 0;
            for (int i = 0; i < 9; i++) {
                x += (10 - i) * Character.digit(isbn.charAt(i), 10);
            }
            x %= 11;
            return xSet || isbn.charAt(9) == 'x' ? 11 - x == 10 : Character.digit(isbn.charAt(9), 10) == (11 - x) % 11;
        } else if (isbn.length() == 13) {
            int x = 0;
            for (int i = 0; i < 12; i++) {
                x += (i % 2 == 0 ? 1 : 3) * Character.digit(isbn.charAt(i), 10);
            }
            x %= 10;
            return Character.digit(isbn.charAt(12), 10) == (10 - x) % 10;
        }
        return false;
    }

    public static boolean checkIsbn(String isbn, String last) {
        if (isbn.length() == 9) {
            int x = 0;
            for (int i = 0; i < 9; i++) {
                x += (10 - i) * Character.digit(isbn.charAt(i), 10);
            }
            x %= 11;
            return last.charAt(0) == 'x' ? 11 - x == 10 : false;
        }
        return false;
    }

}
