package lt.grimsa.hibernate.id;

import model.TestEntities.Human;
import java.io.Serializable;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

public class SingleGlobalTemporaryTableBulkIdStrategyRowCleanupTest extends AbstractSingleGlobalTemporaryTableBulkIdStrategyTest {

    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);
        configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.CLEAN_ROWS, "true");
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
        verify(sqlLog -> sqlLog.contains(("delete from HT_TEMP_IDS where ENTITY_NAME=?")));
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
        });

        // then: row delete statement was executed
        verify(sqlLog -> sqlLog.contains(("delete from HT_TEMP_IDS where ENTITY_NAME=?")));
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
        verify(sqlLog -> sqlLog.contains(("delete from HT_TEMP_IDS where ENTITY_NAME=?")));
    }
}