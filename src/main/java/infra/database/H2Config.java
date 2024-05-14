package infra.database;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class H2Config {
    public static DataSource getDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"); // Cria um banco de dados na memória
        dataSource.setUser("sa"); // usuário padrão
        dataSource.setPassword(""); // senha padrão
        return dataSource;
    }
}
