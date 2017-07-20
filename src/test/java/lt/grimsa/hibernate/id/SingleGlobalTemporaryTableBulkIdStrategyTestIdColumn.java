package lt.grimsa.hibernate.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.cfg.Configuration;
import org.junit.Test;

import model.TestEntities.Human;
import model.TestEntities.Reptile;

public class SingleGlobalTemporaryTableBulkIdStrategyTestIdColumn extends AbstractSingleGlobalTemporaryTableBulkIdStrategyTest {

    private static final String CUSTOM_ID_COLUMN_NAME = "MY_ID";

    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.ID_COLUMN, CUSTOM_ID_COLUMN_NAME);
    }

    @Override
    protected String getIdColumnName() {
        return CUSTOM_ID_COLUMN_NAME;
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
        assertTrue(sqlAppender.log.contains("delete from Dog where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertTrue(sqlAppender.log.contains("delete from Human where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertTrue(sqlAppender.log.contains("delete from Mammal where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        assertTrue(sqlAppender.log.contains("delete from Animal where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
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
        assertTrue(sqlAppender.log.contains("update Mammal set mammalField='someCoolValue' where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        assertEquals(2, sqlAppender.log.size());
    }
}