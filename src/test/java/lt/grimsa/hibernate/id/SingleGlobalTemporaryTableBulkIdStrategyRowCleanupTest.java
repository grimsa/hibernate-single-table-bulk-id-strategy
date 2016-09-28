package lt.grimsa.hibernate.id;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import model.TestEntities.Animal;
import model.TestEntities.Dog;
import model.TestEntities.Human;
import model.TestEntities.Mammal;
import model.TestEntities.Reptile;

public class SingleGlobalTemporaryTableBulkIdStrategyRowCleanupTest extends BaseCoreFunctionalTestCase {

    private Logger sqlLogger = Logger.getLogger("org.hibernate.SQL");

    private MessageCapturingAppender sqlAppender;
    private boolean ddlExecuted;

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty(AvailableSettings.HQL_BULK_ID_STRATEGY, SingleGlobalTemporaryTableBulkIdStrategy.class.getName());
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.TABLE, "HT_TEMP_IDS");
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.DISCRIMINATOR_COLUMN, "ENTITY_NAME");
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.CLEAN_ROWS, "true");
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Animal.class, Mammal.class, Reptile.class, Human.class, Dog.class };
    }

    @Override
    protected void prepareTest() throws Exception {
        sqlAppender = new MessageCapturingAppender();
        openSession();
        if (!ddlExecuted) {
            doInTransaction(() -> session.createNativeQuery("create global temporary table HT_TEMP_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100))").executeUpdate());
            ddlExecuted = true;
        }
        sqlLogger.addAppender(sqlAppender);
    }

    @Override
    protected void cleanupTest() throws Exception {
        sqlLogger.removeAppender(sqlAppender);
    }

    @Test
    public void testDeleteCleansUpRows() {
        // given
        doInTransaction(() -> {
            session.save(new Human());
            session.flush();

            // when
            doWithLogging(() -> session.createQuery("delete from Mammal").executeUpdate());
        });

        // then: row delete statement was executed
        assertTrue(sqlAppender.log.contains("delete from HT_TEMP_IDS where ENTITY_NAME=?"));
    }

    @Test
    public void testUpdateCleansUpRows() {
        // given
        doInTransaction(() -> {
            session.save(new Human());
            session.flush();

            // when
            doWithLogging(() -> session.createQuery("update Human h set h.mammalField = 'someCoolValue'").executeUpdate());
        });

        // then: row delete statement was executed
        assertTrue(sqlAppender.log.contains("delete from HT_TEMP_IDS where ENTITY_NAME=?"));
    }

    private void doInTransaction(Runnable runnable) {
        Transaction transaction = session.beginTransaction();
        runnable.run();
        transaction.commit();
    }

    private void doWithLogging(Runnable runnable) {
        sqlLogger.setLevel(Level.DEBUG);
        runnable.run();
        sqlLogger.setLevel(Level.OFF);
    }

    private static class MessageCapturingAppender extends AppenderSkeleton {
        private final List<String> log = new ArrayList<>();

        @Override
        protected void append(LoggingEvent event) {
            log.add(event.getRenderedMessage());
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
        }
    }
}