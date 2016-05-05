package ru.slicer;

import javax.annotation.Resource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.h2.tools.RunScript;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

@WebListener
public class ContextListener implements ServletContextListener {
    //Конфигурировать свой датасорс нам пришлось бы в конфиге контейнера,
    //и чтобы можно было обойтись без этого, для целей тестирования обойдемся
    //существующим в wildfly/jboss изначально in-memory datasource.
    @Resource(lookup = Datasources.MAIN)
    private DataSource securityDataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            try (final Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().
                    getResourceAsStream("init.sql"));
                 final Connection connection = securityDataSource.getConnection())
            {
                RunScript.execute(connection, reader);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
