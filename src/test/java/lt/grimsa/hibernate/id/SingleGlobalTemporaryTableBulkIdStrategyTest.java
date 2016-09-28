package lt.grimsa.hibernate.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

public class SingleGlobalTemporaryTableBulkIdStrategyTest extends BaseCoreFunctionalTestCase {

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
    public void testDelete() {
        // given
        Human human = new Human();
        doInTransaction(() -> {
            session.save(human);
            Reptile reptile = new Reptile();
            session.save(reptile);
            session.flush();

            // when
            doWithLogging(() -> session.createQuery("delete from Mammal").executeUpdate());
            session.clear();
        });

        // then: entity was deleted
        assertNull(session.find(Human.class, human.id));

        // then: expected SQL was generated
        assertEquals(
                "insert into HT_TEMP_IDS select testentiti0_.id as id, 'model.TestEntities$Mammal' as ENTITY_NAME from Mammal testentiti0_ inner join Animal testentiti0_1_ on testentiti0_.id=testentiti0_1_.id",
                sqlAppender.log.get(0));
        assertTrue(sqlAppender.log.contains("delete from Dog where (id) IN (select id from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertTrue(sqlAppender.log.contains("delete from Human where (id) IN (select id from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertTrue(sqlAppender.log.contains("delete from Mammal where (id) IN (select id from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertTrue(sqlAppender.log.contains("delete from Animal where (id) IN (select id from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertEquals(5, sqlAppender.log.size());
    }

    @Test
    public void testUpdate() {
        // given
        Human human = new Human();
        Reptile reptile = new Reptile();
        doInTransaction(() -> {
            session.save(human);
            session.save(reptile);
            session.flush();

            // when
            doWithLogging(() -> session.createQuery("update Human h set h.mammalField = 'someCoolValue'").executeUpdate());
            session.clear();
        });

        // then: update was performed
        assertEquals("someCoolValue", session.find(Human.class, human.id).mammalField);

        // then: expected SQL was generated
        assertEquals(
                "insert into HT_TEMP_IDS select testentiti0_.id as id, 'model.TestEntities$Human' as ENTITY_NAME from Human testentiti0_ inner join Mammal testentiti0_1_ on testentiti0_.id=testentiti0_1_.id inner join Animal testentiti0_2_ on testentiti0_.id=testentiti0_2_.id",
                sqlAppender.log.get(0));
        assertTrue(sqlAppender.log.contains("update Mammal set mammalField='someCoolValue' where (id) IN (select id from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        assertEquals(2, sqlAppender.log.size());
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