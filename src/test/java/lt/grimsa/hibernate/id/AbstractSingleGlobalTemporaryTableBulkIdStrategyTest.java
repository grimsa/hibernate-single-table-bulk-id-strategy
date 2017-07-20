package lt.grimsa.hibernate.id;

import model.TestEntities.*;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertTrue;

public abstract class AbstractSingleGlobalTemporaryTableBulkIdStrategyTest extends BaseCoreFunctionalTestCase {
    private Logger sqlLogger = Logger.getLogger("org.hibernate.SQL");
    private MessageCapturingAppender sqlAppender;
    private boolean ddlExecuted;

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty(AvailableSettings.HQL_BULK_ID_STRATEGY, SingleGlobalTemporaryTableBulkIdStrategy.class.getName());
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.TABLE, "HT_TEMP_IDS");
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.DISCRIMINATOR_COLUMN, "ENTITY_NAME");
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Animal.class, Mammal.class, Reptile.class, Human.class, Dog.class};
    }

    @Override
    protected void prepareTest() throws Exception {
        sqlAppender = new MessageCapturingAppender();
        openSession();
        if (!ddlExecuted) {
            doInTransaction(() -> session.createNativeQuery("create global temporary table HT_TEMP_IDS ("
                    + getIdColumnName() + " CHAR(36), "
                    + getEntityColumnName() + " VARCHAR(100))")
                    .executeUpdate());
            ddlExecuted = true;
        }
        sqlLogger.addAppender(sqlAppender);
    }

    protected String getIdColumnName() {
        return "ID";
    }

    protected String getEntityColumnName() {
        return "ENTITY_NAME";
    }

    @Override
    protected void cleanupTest() throws Exception {
        sqlLogger.removeAppender(sqlAppender);
    }

    protected void doInTransaction(Runnable runnable) {
        Transaction transaction = session.beginTransaction();
        runnable.run();
        transaction.commit();
    }

    protected void doWithLogging(Runnable runnable) {
        sqlLogger.setLevel(Level.DEBUG);
        runnable.run();
        sqlLogger.setLevel(Level.OFF);
    }

    protected void verify(Predicate<List<String>> sqlLogConsumingPredicate) {
        assertTrue(sqlLogConsumingPredicate.test(sqlAppender.log));
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