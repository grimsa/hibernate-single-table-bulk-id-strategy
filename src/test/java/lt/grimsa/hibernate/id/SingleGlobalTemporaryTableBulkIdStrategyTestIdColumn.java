package lt.grimsa.hibernate.id;

import model.TestEntities.Human;
import model.TestEntities.Reptile;

import java.io.Serializable;

import org.hibernate.cfg.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        verify(sqlLog -> sqlLog.get(0).equals("insert into HT_TEMP_IDS select testentiti0_.id as id, 'model.TestEntities$Mammal' as ENTITY_NAME from Mammal testentiti0_ inner join Animal testentiti0_1_ on testentiti0_.id=testentiti0_1_.id"));
        verify(sqlLog -> sqlLog.contains("delete from Dog where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        verify(sqlLog -> sqlLog.contains("delete from Human where (human_id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        verify(sqlLog -> sqlLog.contains("delete from Mammal where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        verify(sqlLog -> sqlLog.contains("delete from Animal where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Mammal')"));
        verify(sqlLog -> sqlLog.size() == 5);
    }

    @Test
    public void testDeleteCleansUpRows_ManyToMany() {
        // given
        Human human = new Human();
        doInTransaction(() -> {
            Serializable id = session.save(human);
            session.flush();

            // when
            doWithLogging(() -> session.createQuery("delete from Human where id = :id").setParameter("id", id).executeUpdate());
            session.clear();
        });

        // then: entity was deleted
        assertNull(session.find(Human.class, human.id));

        // then: expected SQL was generated
        verify(sqlLog -> sqlLog.get(0).equals("insert into HT_TEMP_IDS select testentiti0_.human_id as human_id, 'model.TestEntities$Human' as ENTITY_NAME from Human testentiti0_ inner join Mammal testentiti0_1_ on testentiti0_.human_id=testentiti0_1_.id inner join Animal testentiti0_2_ on testentiti0_.human_id=testentiti0_2_.id where testentiti0_.human_id=?"));
        verify(sqlLog -> sqlLog.contains("delete from Human_Dog where (Human_human_id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        verify(sqlLog -> sqlLog.contains("delete from Human where (human_id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        verify(sqlLog -> sqlLog.contains("delete from Mammal where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        verify(sqlLog -> sqlLog.contains("delete from Animal where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        verify(sqlLog -> sqlLog.size() == 5);
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
        verify(sqlLog -> sqlLog.get(0).equals("insert into HT_TEMP_IDS select testentiti0_.human_id as human_id, 'model.TestEntities$Human' as ENTITY_NAME from Human testentiti0_ inner join Mammal testentiti0_1_ on testentiti0_.human_id=testentiti0_1_.id inner join Animal testentiti0_2_ on testentiti0_.human_id=testentiti0_2_.id"));
        verify(sqlLog -> sqlLog.contains("update Mammal set mammalField='someCoolValue' where (id) IN (select MY_ID from HT_TEMP_IDS where ENTITY_NAME='model.TestEntities$Human')"));
        verify(sqlLog -> sqlLog.size() == 2);
    }
}