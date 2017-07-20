package lt.grimsa.hibernate.id;

import static org.junit.Assert.assertTrue;

import org.hibernate.cfg.Configuration;
import org.junit.Test;

import model.TestEntities.Human;

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
}