package lt.grimsa.hibernate.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.id.IdTableInfo;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.TableBasedDeleteHandlerImpl;
import org.hibernate.hql.spi.id.TableBasedUpdateHandlerImpl;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.SelectValues;
import org.hibernate.type.StringType;

/**
 * A strategy resembling {@link GlobalTemporaryTableBulkIdStrategy} modified to use a single "global temporary table" created beforehand (e.g.
 * HT_TEMP_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100)))
 * <p>
 * Can be useful in environments where DDL statements cannot be executed from application and managing a large number of ID tables is not practical.
 * <p>
 * <b>Note:</b> multicolumn IDs or inconsistent ID types were not tested and will likely NOT work.
 */
public class SingleGlobalTemporaryTableBulkIdStrategy implements MultiTableBulkIdStrategy {

    /**
     * Fully qualified name of the table to use
     */
    public static final String TABLE = "hibernate.hql.bulk_id_strategy.single_global_temporary.table";

    /**
     * Column to be used as entity discriminator
     */
    public static final String DISCRIMINATOR_COLUMN = "hibernate.hql.bulk_id_strategy.single_global_temporary.discriminator_column";

    /**
     * Whether ID rows should be deleted after update/delete is processed. Defaults to {@code false}
     */
    public static final String CLEAN_ROWS = "hibernate.hql.bulk_id_strategy.single_global_temporary.clean_rows";

    private String fullyQualifiedTableName;
    private String discriminatorColumn;
    private boolean cleanRows;

    @Override
    public void prepare(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess, MetadataImplementor metadata, SessionFactoryOptions sessionFactoryOptions) {
        ConfigurationService configService = sessionFactoryOptions.getServiceRegistry().getService(ConfigurationService.class);
        this.fullyQualifiedTableName = Objects.requireNonNull(configService.getSetting(TABLE, String.class, null), "Property " + TABLE + " must be set.");
        this.discriminatorColumn = Objects.requireNonNull(configService.getSetting(DISCRIMINATOR_COLUMN, String.class, null), "Property " + DISCRIMINATOR_COLUMN + " must be set.");
        this.cleanRows = configService.getSetting(CLEAN_ROWS, StandardConverters.BOOLEAN, false);
    }

    @Override
    public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
    }

    @Override
    public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
        final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();
        final Queryable targetedPersister = updateStatement.getFromClause().getFromElement().getQueryable();
        final String discriminator = generateDiscriminatorValue(targetedPersister);

        return new TableBasedUpdateHandlerImpl(factory, walker, this::getTableName) {

            @Override
            protected String generateIdSubselect(Queryable persister, IdTableInfo idTableInfo) {
                return super.generateIdSubselect(persister, idTableInfo) + " where " + discriminatorColumn + "='" + discriminator + '\'';
            }

            @Override
            protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
                selectClause.addColumn(null, '\'' + discriminator + '\'', discriminatorColumn);
            }

            @Override
            protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
                if (cleanRows) {
                    cleanUpRows(session, targetedPersister);
                }
            }
        };
    }

    @Override
    public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
        final DeleteStatement deleteStatement = (DeleteStatement) walker.getAST();
        final Queryable targetedPersister = deleteStatement.getFromClause().getFromElement().getQueryable();
        final String discriminator = generateDiscriminatorValue(targetedPersister);

        return new TableBasedDeleteHandlerImpl(factory, walker, this::getTableName) {

            @Override
            protected String generateIdSubselect(Queryable persister, IdTableInfo idTableInfo) {
                return super.generateIdSubselect(persister, idTableInfo) + " where " + discriminatorColumn + "='" + discriminator + '\'';
            }

            @Override
            protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
                selectClause.addColumn(null, '\'' + generateDiscriminatorValue(targetedPersister) + '\'', discriminatorColumn);
            }

            @Override
            protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
                if (cleanRows) {
                    cleanUpRows(session, persister);
                }
            }
        };
    }

    private void cleanUpRows(SharedSessionContractImplementor session, Queryable persister) {
        final String sql = "delete from " + fullyQualifiedTableName + " where " + discriminatorColumn + "=?";
        PreparedStatement ps = null;
        try {
            ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement(sql, false);
            ps.setString(1, generateDiscriminatorValue(persister));
            StringType.INSTANCE.set(ps, generateDiscriminatorValue(persister), 1, session);
            session.getJdbcCoordinator().getResultSetReturn().executeUpdate(ps);
        } catch (SQLException e) {
            throw session.getJdbcServices().getSqlExceptionHelper().convert(e, "Unable to clean up id table [" + fullyQualifiedTableName + "]", sql);
        } finally {
            if (ps != null) {
                session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release(ps);
            }
        }
    }

    protected String generateDiscriminatorValue(Queryable persister) {
        return persister.getEntityName();
    }

    private String getTableName() {
        return fullyQualifiedTableName;
    }
}
